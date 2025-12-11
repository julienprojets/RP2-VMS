package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Vouchers;
import pkg.vms.util.DatabaseSchema;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

/**
 * Controller for Reports & Analytics page
 */
public class ReportsController implements Initializable {

    @FXML private TableView<Vouchers> reportsTable;
    @FXML private TableColumn<Vouchers, String> voucherCodeColumn;
    @FXML private TableColumn<Vouchers, String> requestRefColumn;
    @FXML private TableColumn<Vouchers, String> clientColumn;
    @FXML private TableColumn<Vouchers, Double> valueColumn;
    @FXML private TableColumn<Vouchers, String> statusColumn;
    @FXML private TableColumn<Vouchers, java.util.Date> initDateColumn;
    @FXML private TableColumn<Vouchers, java.util.Date> expiryDateColumn;
    @FXML private TableColumn<Vouchers, java.util.Date> dateRedeemedColumn;
    @FXML private TableColumn<Vouchers, String> redeemedByColumn;
    @FXML private TableColumn<Vouchers, String> redeemedBranchColumn;
    
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private TextField searchField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button exportExcelButton;
    
    @FXML private Label totalVouchersLabel;
    @FXML private Label redeemedLabel;
    @FXML private Label activeLabel;
    @FXML private Label expiredLabel;
    @FXML private Label totalValueLabel;
    
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> redemptionBarChart;
    @FXML private CategoryAxis dateAxis;
    @FXML private NumberAxis countAxis;
    
    private ObservableList<Vouchers> allVouchers = FXCollections.observableArrayList();
    private FilteredList<Vouchers> filteredVouchers;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup table columns first
        setupTableColumns();
        
        // Setup filter combo
        filterStatusCombo.getItems().addAll("All", "Redeemed", "Active", "Expired", "Available", "Reserved");
        filterStatusCombo.setValue("All");
        
        // Load data in background thread to prevent UI freezing
        new Thread(() -> {
            // Ensure schema is up to date (only once, not blocking)
            try {
                DatabaseSchema.ensureSchemaExists();
            } catch (Exception e) {
                System.err.println("Error ensuring schema: " + e.getMessage());
            }
            
            // Load all vouchers for reporting (including redeemed)
            loadAllVouchers();
            
            // Update statistics and charts on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                updateStatistics();
                updateCharts();
            });
        }).start();
    }
    
    // Cache for additional voucher data
    private Map<String, String> clientNameCache = new HashMap<>();
    private Map<String, String> requestRefCache = new HashMap<>();
    private Map<String, String> redeemedByCache = new HashMap<>();
    private Map<String, String> redeemedBranchCache = new HashMap<>();
    
    private void setupTableColumns() {
        voucherCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code_voucher"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status_voucher"));
        valueColumn.setCellValueFactory(cellData -> {
            Vouchers v = cellData.getValue();
            javafx.beans.property.SimpleDoubleProperty prop = new javafx.beans.property.SimpleDoubleProperty(v.getPrice());
            return prop.asObject();
        });
        initDateColumn.setCellValueFactory(new PropertyValueFactory<>("init_date"));
        expiryDateColumn.setCellValueFactory(new PropertyValueFactory<>("expiry_date"));
        dateRedeemedColumn.setCellValueFactory(new PropertyValueFactory<>("date_redeemed"));
        
        // Use cached data instead of querying database for each row
        clientColumn.setCellValueFactory(cellData -> {
            Vouchers v = cellData.getValue();
            String key = String.valueOf(v.getRef_client());
            String clientName = clientNameCache.getOrDefault(key, "Unknown");
            return new javafx.beans.property.SimpleStringProperty(clientName);
        });
        
        requestRefColumn.setCellValueFactory(cellData -> {
            Vouchers v = cellData.getValue();
            String requestRef = requestRefCache.getOrDefault(v.getCode_voucher(), "");
            return new javafx.beans.property.SimpleStringProperty(requestRef);
        });
        
        redeemedByColumn.setCellValueFactory(cellData -> {
            Vouchers v = cellData.getValue();
            String redeemedBy = redeemedByCache.getOrDefault(v.getCode_voucher(), "");
            return new javafx.beans.property.SimpleStringProperty(redeemedBy);
        });
        
        redeemedBranchColumn.setCellValueFactory(cellData -> {
            Vouchers v = cellData.getValue();
            String branch = redeemedBranchCache.getOrDefault(v.getCode_voucher(), "");
            return new javafx.beans.property.SimpleStringProperty(branch);
        });
    }
    
    private String getClientName(int clientId) {
        try (Connection conn = DBconnection.getConnection()) {
            // Check which column exists
            String columnName = "nom_client"; // Default to nom_client
            try (Statement checkStmt = conn.createStatement();
                 ResultSet colRs = checkStmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'clients' AND column_name IN ('nom_client', 'name_client') " +
                    "ORDER BY CASE WHEN column_name = 'nom_client' THEN 1 ELSE 2 END LIMIT 1")) {
                if (colRs.next()) {
                    columnName = colRs.getString("column_name");
                }
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(
                 "SELECT " + columnName + " FROM clients WHERE ref_client = ?")) {
                stmt.setInt(1, clientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(columnName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }
    
    private String getRequestReference(String voucherCode) {
        try (Connection conn = DBconnection.getConnection()) {
            String voucherCodeColumn = getVoucherCodeColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(
                 "SELECT request_reference FROM vouchers WHERE " + voucherCodeColumn + " = ?")) {
                stmt.setString(1, voucherCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String ref = rs.getString("request_reference");
                        return ref != null ? ref : "";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private String getRedeemedBy(String voucherCode) {
        try (Connection conn = DBconnection.getConnection()) {
            String voucherCodeColumn = getVoucherCodeColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(
                 "SELECT redeemed_by FROM vouchers WHERE " + voucherCodeColumn + " = ?")) {
                stmt.setString(1, voucherCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("redeemed_by");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private String getRedeemedBranch(String voucherCode) {
        try (Connection conn = DBconnection.getConnection()) {
            String voucherCodeColumn = getVoucherCodeColumn(conn);
            try (PreparedStatement stmt = conn.prepareStatement(
                 "SELECT redeemed_branch FROM vouchers WHERE " + voucherCodeColumn + " = ?")) {
                stmt.setString(1, voucherCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("redeemed_branch");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    private String getVoucherCodeColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT column_name FROM information_schema.columns " +
                 "WHERE table_name = 'vouchers' AND column_name IN ('code_voucher', 'ref_voucher') " +
                 "ORDER BY CASE WHEN column_name = 'code_voucher' THEN 1 ELSE 2 END LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("column_name");
            }
        }
        return "code_voucher"; // Default
    }
    
    private void loadAllVouchers() {
        allVouchers.clear();
        // Clear caches
        clientNameCache.clear();
        requestRefCache.clear();
        redeemedByCache.clear();
        redeemedBranchCache.clear();
        
        Connection conn = null;
        try {
            conn = DBconnection.getConnection();
            if (conn == null) {
                javafx.application.Platform.runLater(() -> {
                    showError("Unable to connect to database");
                });
                return;
            }
            
            String voucherCodeColumn = getVoucherCodeColumn(conn);
            
            // Check which client name column exists
            String clientNameColumn = "nom_client"; // Default
            try (Statement checkStmt = conn.createStatement();
                 ResultSet colRs = checkStmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'clients' AND column_name IN ('nom_client', 'name_client') " +
                    "ORDER BY CASE WHEN column_name = 'nom_client' THEN 1 ELSE 2 END LIMIT 1")) {
                if (colRs.next()) {
                    clientNameColumn = colRs.getString("column_name");
                }
            }
            
            // Load all vouchers with JOIN to get client names and other data in one query
            String sql = "SELECT v." + voucherCodeColumn + ", v.status_voucher, v.init_date, v.expiry_date, " +
                        "v.ref_client, v.val_voucher, v.redeemed, v.date_redeemed, v.request_reference, " +
                        "v.redeemed_by, v.redeemed_branch, c." + clientNameColumn + " as client_name " +
                        "FROM vouchers v " +
                        "LEFT JOIN clients c ON v.ref_client = c.ref_client " +
                        "ORDER BY v.date_redeemed DESC NULLS LAST, v.init_date DESC";
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    String code = rs.getString(1);
                    String status = rs.getString("status_voucher");
                    java.sql.Date initDateSql = rs.getDate("init_date");
                    java.sql.Date expiryDateSql = rs.getDate("expiry_date");
                    int refClient = rs.getInt("ref_client");
                    double valVoucher = rs.getDouble("val_voucher");
                    boolean redeemed = rs.getBoolean("redeemed");
                    Timestamp dateRedeemed = rs.getTimestamp("date_redeemed");
                    String requestRef = rs.getString("request_reference");
                    String redeemedBy = rs.getString("redeemed_by");
                    String redeemedBranch = rs.getString("redeemed_branch");
                    String clientName = rs.getString("client_name");
                    
                    // Cache the data
                    clientNameCache.put(String.valueOf(refClient), clientName != null ? clientName : "Unknown");
                    if (requestRef != null) {
                        requestRefCache.put(code, requestRef);
                    }
                    if (redeemedBy != null) {
                        redeemedByCache.put(code, redeemedBy);
                    }
                    if (redeemedBranch != null) {
                        redeemedBranchCache.put(code, redeemedBranch);
                    }
                    
                    Vouchers voucher = new Vouchers(refClient, code, valVoucher, redeemed);
                    voucher.setStatus_voucher(status);
                    if (initDateSql != null) {
                        voucher.setInit_date(new java.util.Date(initDateSql.getTime()));
                    }
                    if (expiryDateSql != null) {
                        voucher.setExpiry_date(new java.util.Date(expiryDateSql.getTime()));
                    }
                    if (dateRedeemed != null) {
                        voucher.setDate_redeemed(new java.util.Date(dateRedeemed.getTime()));
                    }
                    allVouchers.add(voucher);
                }
            }
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                filteredVouchers = new FilteredList<>(allVouchers, p -> true);
                reportsTable.setItems(filteredVouchers);
            });
            
        } catch (SQLException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                showError("Error loading vouchers: " + e.getMessage());
            });
        }
        // Don't close connection - let DBconnection manage it
    }
    
    @FXML
    private void handleFilterChange() {
        applyFilters();
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }
    
    @FXML
    private void handleDateFilter() {
        applyFilters();
    }
    
    private void applyFilters() {
        String selectedStatus = filterStatusCombo.getSelectionModel().getSelectedItem();
        String searchText = searchField.getText().toLowerCase();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        filteredVouchers.setPredicate(v -> {
            // Status filter
            boolean matchesStatus = true;
            if (selectedStatus != null && !"All".equals(selectedStatus)) {
                if ("Redeemed".equals(selectedStatus)) {
                    matchesStatus = v.isRedeemed();
                } else if ("Expired".equals(selectedStatus)) {
                    if (v.getExpiry_date() != null) {
                        matchesStatus = v.getExpiry_date().before(new java.util.Date());
                    } else {
                        matchesStatus = false;
                    }
                } else {
                    matchesStatus = selectedStatus.equals(v.getStatus_voucher());
                }
            }
            
            // Search filter
            boolean matchesSearch = true;
            if (!searchText.isEmpty()) {
                String clientKey = String.valueOf(v.getRef_client());
                String clientName = clientNameCache.getOrDefault(clientKey, "");
                String requestRef = requestRefCache.getOrDefault(v.getCode_voucher(), "");
                matchesSearch = v.getCode_voucher().toLowerCase().contains(searchText) ||
                              clientName.toLowerCase().contains(searchText) ||
                              requestRef.toLowerCase().contains(searchText);
            }
            
            // Date filter
            boolean matchesDate = true;
            if (startDate != null || endDate != null) {
                if (v.getDate_redeemed() != null) {
                    LocalDate redeemedDate = new java.sql.Date(v.getDate_redeemed().getTime()).toLocalDate();
                    if (startDate != null && redeemedDate.isBefore(startDate)) {
                        matchesDate = false;
                    }
                    if (endDate != null && redeemedDate.isAfter(endDate)) {
                        matchesDate = false;
                    }
                } else {
                    matchesDate = false; // Only show vouchers with redemption dates if date filter is set
                }
            }
            
            return matchesStatus && matchesSearch && matchesDate;
        });
    }
    
    private void updateStatistics() {
        int total = allVouchers.size();
        int redeemed = 0;
        int active = 0;
        int expired = 0;
        double totalValue = 0.0;
        
        for (Vouchers v : allVouchers) {
            totalValue += v.getPrice();
            if (v.isRedeemed()) {
                redeemed++;
            }
            if ("Active".equals(v.getStatus_voucher())) {
                active++;
            }
            if (v.getExpiry_date() != null && v.getExpiry_date().before(new java.util.Date())) {
                expired++;
            }
        }
        
        totalVouchersLabel.setText(String.valueOf(total));
        redeemedLabel.setText(String.valueOf(redeemed));
        activeLabel.setText(String.valueOf(active));
        expiredLabel.setText(String.valueOf(expired));
        totalValueLabel.setText(String.format("Rs %.2f", totalValue));
    }
    
    private void updateCharts() {
        // Update Pie Chart - Status Distribution
        Map<String, Integer> statusCount = new HashMap<>();
        for (Vouchers v : allVouchers) {
            String status = v.getStatus_voucher();
            if (status == null) status = "Unknown";
            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);
        }
        
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        statusPieChart.setData(pieChartData);
        statusPieChart.setTitle("Voucher Status Distribution");
        
        // Update Bar Chart - Redemptions Over Time
        Map<String, Integer> redemptionByDate = new HashMap<>();
        for (Vouchers v : allVouchers) {
            if (v.isRedeemed() && v.getDate_redeemed() != null) {
                String dateStr = new java.sql.Date(v.getDate_redeemed().getTime()).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMM dd"));
                redemptionByDate.put(dateStr, redemptionByDate.getOrDefault(dateStr, 0) + 1);
            }
        }
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Redemptions");
        
        // Sort dates
        List<String> sortedDates = new ArrayList<>(redemptionByDate.keySet());
        sortedDates.sort((d1, d2) -> {
            try {
                LocalDate date1 = LocalDate.parse(d1, DateTimeFormatter.ofPattern("MMM dd"));
                LocalDate date2 = LocalDate.parse(d2, DateTimeFormatter.ofPattern("MMM dd"));
                return date1.compareTo(date2);
            } catch (Exception e) {
                return d1.compareTo(d2);
            }
        });
        
        for (String date : sortedDates) {
            series.getData().add(new XYChart.Data<>(date, redemptionByDate.get(date)));
        }
        
        redemptionBarChart.getData().clear();
        if (!series.getData().isEmpty()) {
            redemptionBarChart.getData().add(series);
        }
        redemptionBarChart.setTitle("Redemptions Over Time");
    }
    
    @FXML
    private void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Reports to Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.append("Voucher Code,Request Reference,Client,Value (Rs),Status,Init Date,Expiry Date,Date Redeemed,Redeemed By,Branch\n");

                // Write data
                for (Vouchers v : filteredVouchers) {
                    String clientKey = String.valueOf(v.getRef_client());
                    writer.append(String.format("\"%s\",\"%s\",\"%s\",%.2f,\"%s\",%s,%s,%s,\"%s\",\"%s\"\n",
                        escapeCSV(v.getCode_voucher()),
                        escapeCSV(requestRefCache.getOrDefault(v.getCode_voucher(), "")),
                        escapeCSV(clientNameCache.getOrDefault(clientKey, "Unknown")),
                        v.getPrice(),
                        escapeCSV(v.getStatus_voucher()),
                        v.getInit_date() != null ? v.getInit_date().toString() : "",
                        v.getExpiry_date() != null ? v.getExpiry_date().toString() : "",
                        v.getDate_redeemed() != null ? v.getDate_redeemed().toString() : "",
                        escapeCSV(redeemedByCache.getOrDefault(v.getCode_voucher(), "")),
                        escapeCSV(redeemedBranchCache.getOrDefault(v.getCode_voucher(), ""))
                    ));
                }

                showSuccess("Exported " + filteredVouchers.size() + " vouchers to " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showError("Error exporting to Excel: " + e.getMessage());
            }
        }
    }
    
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

