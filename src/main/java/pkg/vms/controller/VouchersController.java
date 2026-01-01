package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Requests;
import pkg.vms.model.Vouchers;
import pkg.vms.util.DatabaseSchema;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class VouchersController implements Initializable {

    /* ============================
            TABLE + COLUMNS
       ============================ */
    @FXML private TableView<Vouchers> voucherTable;
    @FXML private TableColumn<Vouchers, String> codeColumn;
    @FXML private TableColumn<Vouchers, String> statusColumn;
    @FXML private TableColumn<Vouchers, Date> initDateColumn;
    @FXML private TableColumn<Vouchers, Date> expiryDateColumn;

    /* ============================
            TOP BUTTONS
       ============================ */
    @FXML private Button addButton, editButton, deleteButton;

    /* ============================
            SEARCH BAR
       ============================ */
    @FXML private TextField searchHeaderField;

    /* ============================
            ADD / EDIT FORM
       ============================ */
    @FXML private VBox addForm;

    @FXML private TextField formCode;
    @FXML private TextField formQuantity;
    @FXML private ComboBox<String> formStatus;
    @FXML private DatePicker formInitDate;
    @FXML private DatePicker formExpiryDate;
    @FXML private Label formError;
    @FXML private Label formTitle;

    private boolean editMode = false;
    private Vouchers editingVoucher = null;

    /* ============================
            DATA LIST
       ============================ */
    private ObservableList<Vouchers> voucherList = FXCollections.observableArrayList();


    /* ============================
            INITIALIZE
       ============================ */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup table columns
        codeColumn.setCellValueFactory(cell -> {
            String code = cell.getValue().getCode_voucher();
            if (code == null || code.isEmpty()) {
                // Try ref_voucher if code_voucher is empty
                try {
                    Connection conn = DBconnection.getConnection();
                    String voucherCodeColumn = getVoucherCodeColumn(conn);
                    if ("ref_voucher".equals(voucherCodeColumn)) {
                        // Load from database
                        String sql = "SELECT ref_voucher FROM vouchers WHERE ref_voucher = ? OR code_voucher = ?";
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, cell.getValue().getCode_voucher());
                            ps.setString(2, cell.getValue().getCode_voucher());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    code = rs.getString(1);
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    // Ignore
                }
            }
            return new javafx.beans.property.SimpleStringProperty(code != null ? code : "");
        });
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
            cell.getValue().getStatus_voucher() != null ? cell.getValue().getStatus_voucher() : "Available"));
        initDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getInit_date()));
        expiryDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getExpiry_date()));

        voucherTable.setItems(voucherList);

        addForm.setVisible(false);
        addForm.setManaged(false);

        // Setup status ComboBox - only Active and Reserved for new vouchers
        formStatus.getItems().addAll("Active", "Reserved");
        formStatus.setValue("Active");

        // Load data in background thread to prevent UI freezing
        new Thread(() -> {
            // Ensure database schema is up to date (only once, not blocking)
            try {
                DatabaseSchema.ensureSchemaExists();
            } catch (Exception e) {
                System.err.println("Error ensuring schema: " + e.getMessage());
            }
            loadVouchers();
        }).start();
    }
    
    private String getVoucherCodeColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name IN ('ref_voucher', 'code_voucher') " +
                "ORDER BY CASE WHEN column_name = 'code_voucher' THEN 1 ELSE 2 END")) {
            if (rs.next()) {
                return rs.getString("column_name");
            }
        }
        return "ref_voucher"; // Default
    }


    /* ============================
            LOAD VOUCHERS
       ============================ */
    private void loadVouchers() {
        voucherList.clear();
        Connection conn = null;
        try {
            conn = DBconnection.getConnection();
            String voucherCodeColumn = getVoucherCodeColumn(conn);
            
            // Check if assigned_to_request column exists
            boolean hasAssignedColumn = false;
            try (Statement checkStmt = conn.createStatement();
                 ResultSet colRs = checkStmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'vouchers' AND column_name = 'assigned_to_request'")) {
                hasAssignedColumn = colRs.next();
            }
            
            // Check if request_reference column exists (vouchers assigned to requests will have this)
            boolean hasRequestRefColumn = false;
            try (Statement checkStmt = conn.createStatement();
                 ResultSet colRs = checkStmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'vouchers' AND column_name = 'request_reference'")) {
                hasRequestRefColumn = colRs.next();
            }
            
            String sql;
            if (hasAssignedColumn && hasRequestRefColumn) {
                // Filter out vouchers that are assigned to requests AND redeemed vouchers
                sql = "SELECT " + voucherCodeColumn + ", status_voucher, init_date, expiry_date, " +
                      "ref_client, val_voucher, redeemed FROM vouchers " +
                      "WHERE (assigned_to_request = FALSE OR assigned_to_request IS NULL) " +
                      "AND (request_reference IS NULL OR request_reference = '') " +
                      "AND (redeemed = FALSE OR redeemed IS NULL) " +
                      "ORDER BY init_date DESC";
            } else if (hasAssignedColumn) {
                sql = "SELECT " + voucherCodeColumn + ", status_voucher, init_date, expiry_date, " +
                      "ref_client, val_voucher, redeemed FROM vouchers " +
                      "WHERE (assigned_to_request = FALSE OR assigned_to_request IS NULL) " +
                      "AND (redeemed = FALSE OR redeemed IS NULL) " +
                      "ORDER BY init_date DESC";
            } else if (hasRequestRefColumn) {
                // Filter by request_reference if assigned_to_request doesn't exist
                sql = "SELECT " + voucherCodeColumn + ", status_voucher, init_date, expiry_date, " +
                      "ref_client, val_voucher, redeemed FROM vouchers " +
                      "WHERE (request_reference IS NULL OR request_reference = '') " +
                      "AND (redeemed = FALSE OR redeemed IS NULL) " +
                      "ORDER BY init_date DESC";
            } else {
                // Neither column exists, filter out redeemed vouchers
                sql = "SELECT " + voucherCodeColumn + ", status_voucher, init_date, expiry_date, " +
                      "ref_client, val_voucher, redeemed FROM vouchers " +
                      "WHERE (redeemed = FALSE OR redeemed IS NULL) " +
                      "ORDER BY init_date DESC";
            }

            try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                    String code = rs.getString(1);
                    String status = rs.getString("status_voucher");
                    if (status == null) status = "Available";
                    
                Vouchers v = new Vouchers(
                        rs.getInt("ref_client"),
                            code,
                            0.0, // Price not stored here, determined by request
                        rs.getBoolean("redeemed")
                );
                v.setInit_date(rs.getDate("init_date"));
                v.setExpiry_date(rs.getDate("expiry_date"));
                    v.setStatus_voucher(status);

                voucherList.add(v);
                }
            }
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                voucherTable.setItems(voucherList);
            });
        } catch (SQLException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> {
                showError("Error loading vouchers: " + e.getMessage());
            });
        } finally {
            // Don't close connection - let DBconnection manage it
        }
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


    /* ============================
            SEARCH
       ============================ */
    @FXML
    private void handleSearch() {
        String keyword = searchHeaderField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            loadVouchers();
            return;
        }

        ObservableList<Vouchers> filtered = FXCollections.observableArrayList();

        for (Vouchers v : voucherList) {
            if (v.getCode_voucher().toLowerCase().contains(keyword)) {
                filtered.add(v);
            }
        }

        voucherTable.setItems(filtered);
    }


    /* ============================
            ADD VOUCHER (opens form)
       ============================ */
    @FXML
    private void handleAddVoucher() {
        editMode = false;
        editingVoucher = null;
        formTitle.setText("Add New Voucher(s)");

        formCode.clear();
        formQuantity.setText("1");
        formStatus.setValue("Active");
        formInitDate.setValue(LocalDate.now());
        formExpiryDate.setValue(LocalDate.now().plusDays(365));
        formError.setText("");

        addForm.setVisible(true);
        addForm.setManaged(true);
    }


    /* ============================
            EDIT VOUCHER (opens form)
       ============================ */
    @FXML
    private void handleEditVoucher() {
        Vouchers selected = voucherTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a voucher to edit.");
            return;
        }

        editMode = true;
        editingVoucher = selected;
        formTitle.setText("Edit Voucher");

        formCode.setText(selected.getCode_voucher());
        formQuantity.setText("1");
        // For editing, show existing status if it's Active or Reserved, otherwise default to Active
        String currentStatus = selected.getStatus_voucher();
        if (currentStatus != null && (currentStatus.equals("Active") || currentStatus.equals("Reserved"))) {
            formStatus.setValue(currentStatus);
        } else {
            formStatus.setValue("Active");
        }

        if (selected.getInit_date() != null)
            formInitDate.setValue(new java.sql.Date(selected.getInit_date().getTime()).toLocalDate());
        else
            formInitDate.setValue(LocalDate.now());

        if (selected.getExpiry_date() != null)
            formExpiryDate.setValue(new java.sql.Date(selected.getExpiry_date().getTime()).toLocalDate());
        else
            formExpiryDate.setValue(LocalDate.now().plusDays(365));

        formError.setText("");

        addForm.setVisible(true);
        addForm.setManaged(true);
    }


    /* ============================
            DELETE VOUCHER
       ============================ */
    @FXML
    private void handleDeleteVoucher() {
        Vouchers selected = voucherTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a voucher to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Voucher");
        confirm.setHeaderText("Delete Voucher");
        confirm.setContentText("Are you sure you want to delete voucher: " + selected.getCode_voucher() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBconnection.getConnection()) {
                    String voucherCodeColumn = getVoucherCodeColumn(conn);
                    String sql = "DELETE FROM vouchers WHERE " + voucherCodeColumn + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, selected.getCode_voucher());
            ps.executeUpdate();
                    }

                    voucherList.remove(selected);
                    showSuccess("Voucher deleted successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
                    showError("Error deleting voucher: " + e.getMessage());
                }
        }
        });
    }


    /* ============================
            SAVE FORM (ADD / EDIT)
       ============================ */
    @FXML
    private void handleFormSave() {
        try {
            String status = formStatus.getValue();
            if (status == null || status.isEmpty()) {
                formError.setText("Please select a status.");
                return;
            }

            java.sql.Date init = formInitDate.getValue() != null
                    ? java.sql.Date.valueOf(formInitDate.getValue())
                    : java.sql.Date.valueOf(LocalDate.now());

            java.sql.Date exp = formExpiryDate.getValue() != null
                    ? java.sql.Date.valueOf(formExpiryDate.getValue())
                    : java.sql.Date.valueOf(LocalDate.now().plusDays(365));

            if (editMode) {
                // Update existing voucher
                String code = formCode.getText().trim();
                if (code.isEmpty()) {
                    formError.setText("Voucher code is required for editing.");
                return;
            }

                try (Connection conn = DBconnection.getConnection()) {
                    String voucherCodeColumn = getVoucherCodeColumn(conn);
                    
                    // Check if code is unique (if changed)
                    if (!code.equals(editingVoucher.getCode_voucher())) {
                        String checkSql = "SELECT COUNT(*) FROM vouchers WHERE " + voucherCodeColumn + " = ?";
                        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                            checkPs.setString(1, code);
                            try (ResultSet rs = checkPs.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    formError.setText("Voucher code already exists. Please use a unique code.");
                                    return;
                                }
                            }
                        }
                    }

                    String updateSql = "UPDATE vouchers SET " + voucherCodeColumn + " = ?, status_voucher = ?, " +
                                     "init_date = ?, expiry_date = ? WHERE " + voucherCodeColumn + " = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, code);
                        ps.setString(2, status);
                        ps.setDate(3, init);
                        ps.setDate(4, exp);
                        ps.setString(5, editingVoucher.getCode_voucher());
                        ps.executeUpdate();
                    }

                    showSuccess("Voucher updated successfully.");
                    loadVouchers();
                    handleCancel();
                } catch (SQLException e) {
                    e.printStackTrace();
                    formError.setText("Database error: " + e.getMessage());
                }

            } else {
                // Create new voucher(s) - batch creation
                int quantity = 1;
                try {
                    String qtyText = formQuantity.getText().trim();
                    if (!qtyText.isEmpty()) {
                        quantity = Integer.parseInt(qtyText);
                        if (quantity <= 0) {
                            formError.setText("Quantity must be greater than 0.");
                            return;
                        }
                        if (quantity > 1000) {
                            formError.setText("Maximum 1000 vouchers can be created at once.");
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    formError.setText("Invalid quantity. Please enter a number.");
                    return;
                }

                String baseCode = formCode.getText().trim();
                List<String> createdCodes = new ArrayList<>();

                try (Connection conn = DBconnection.getConnection()) {
                    String voucherCodeColumn = getVoucherCodeColumn(conn);
                    
                    for (int i = 0; i < quantity; i++) {
                        String voucherCode;
                        if (!baseCode.isEmpty() && quantity == 1) {
                            // Use provided code for single voucher
                            voucherCode = baseCode;
                        } else {
                            // Generate unique code
                            voucherCode = generateUniqueVoucherCode(conn, voucherCodeColumn);
                        }

                        // Check uniqueness
                        String checkSql = "SELECT COUNT(*) FROM vouchers WHERE " + voucherCodeColumn + " = ?";
                        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                            checkPs.setString(1, voucherCode);
                            try (ResultSet rs = checkPs.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    // Code exists, generate new one
                                    voucherCode = generateUniqueVoucherCode(conn, voucherCodeColumn);
                                }
                            }
                        }

                        // Check if assigned_to_request column exists
                        boolean hasAssignedColumn = false;
                        try (Statement checkStmt = conn.createStatement();
                             ResultSet colRs = checkStmt.executeQuery(
                                "SELECT column_name FROM information_schema.columns " +
                                "WHERE table_name = 'vouchers' AND column_name = 'assigned_to_request'")) {
                            hasAssignedColumn = colRs.next();
            }

                        // Insert voucher
                        String insertSql;
                        if (hasAssignedColumn) {
                            insertSql = "INSERT INTO vouchers(" + voucherCodeColumn + ", status_voucher, " +
                                       "init_date, expiry_date, redeemed, assigned_to_request, val_voucher) " +
                                       "VALUES(?, ?, ?, ?, FALSE, FALSE, 0.0)";
                        } else {
                            insertSql = "INSERT INTO vouchers(" + voucherCodeColumn + ", status_voucher, " +
                                       "init_date, expiry_date, redeemed, val_voucher) " +
                                       "VALUES(?, ?, ?, ?, FALSE, 0.0)";
                        }
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setString(1, voucherCode);
                            ps.setString(2, status);
                            ps.setDate(3, init);
                            ps.setDate(4, exp);
                            ps.executeUpdate();
                        }

                        createdCodes.add(voucherCode);
                    }

                    showSuccess("Successfully created " + createdCodes.size() + " voucher(s).");
                    loadVouchers();
                    handleCancel();
                } catch (SQLException e) {
                    e.printStackTrace();
                    formError.setText("Database error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            formError.setText("Error: " + e.getMessage());
        }
    }
    
    private String generateUniqueVoucherCode(Connection conn, String voucherCodeColumn) throws SQLException {
        String prefix = "VCHR";
        int attempt = 0;
        String voucherCode;
        
        // Get the next sequence number
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(" + voucherCodeColumn + " FROM 'VCHR(\\d+)') AS INTEGER)), 0) + 1 " +
                    "FROM vouchers WHERE " + voucherCodeColumn + " ~ '^VCHR\\d+$'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int nextNum = 1;
            if (rs.next()) {
                nextNum = rs.getInt(1);
            }
            voucherCode = prefix + String.format("%06d", nextNum);
        }
        
        // Ensure uniqueness
        while (attempt < 100) {
            String checkSql = "SELECT COUNT(*) FROM vouchers WHERE " + voucherCodeColumn + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, voucherCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        // Code is unique
                        break;
                    } else {
                        // Code exists, try with suffix
                        attempt++;
                        voucherCode = prefix + String.format("%06d", Integer.parseInt(voucherCode.substring(4)) + attempt);
        }
                }
            }
        }
        
        return voucherCode;
    }


    /* ============================
            CANCEL FORM
       ============================ */
    @FXML
    private void handleCancel() {
        addForm.setVisible(false);
        addForm.setManaged(false);
        editMode = false;
        editingVoucher = null;
    }


    /* ============================
            GENERATION FOR REQUESTS
       ============================ */
    public static List<Vouchers> generateVouchersForRequest(Requests request) {
        List<Vouchers> vouchers = new ArrayList<>();

        for (int i = 0; i < request.getNum_voucher(); i++) {

            String code = "VCHR-" + request.getRef_request() + "-" + (i + 1);

            Vouchers v = new Vouchers(
                    request.getRef_client(),
                    code,
                    request.getVal_voucher(),
                    false
            );

            Date now = new Date();
            v.setInit_date(now);

            long expiryMillis =
                    now.getTime() + (request.getDuration_voucher() * 24L * 60L * 60L * 1000L);

            v.setExpiry_date(new Date(expiryMillis));

            // Insert in DB
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO vouchers(ref_client, ref_voucher, val_voucher, init_date, expiry_date, status_voucher, redeemed) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?)")) {

                ps.setInt(1, v.getRef_client());
                ps.setString(2, v.getCode_voucher());
                ps.setDouble(3, v.getPrice());
                ps.setDate(4, new java.sql.Date(v.getInit_date().getTime()));
                ps.setDate(5, new java.sql.Date(v.getExpiry_date().getTime()));
                ps.setString(6, "Active");
                ps.setBoolean(7, false);
                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }

            vouchers.add(v);
        }

        return vouchers;
    }
}
