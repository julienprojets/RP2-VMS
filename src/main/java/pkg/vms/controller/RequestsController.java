package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.controller.UserSession;
import pkg.vms.model.Clients;
import pkg.vms.model.Requests;
import pkg.vms.model.VoucherRequest;
import pkg.vms.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Comprehensive Voucher Request Management Controller
 * Handles: Creation, Payment, Approval, Generation, Dispatch, Reporting
 */
public class RequestsController implements Initializable {

    // Table and columns
    @FXML private TableView<VoucherRequest> requestsTable;
    @FXML private TableColumn<VoucherRequest, String> referenceColumn;
    @FXML private TableColumn<VoucherRequest, String> clientColumn;
    @FXML private TableColumn<VoucherRequest, Integer> quantityColumn;
    @FXML private TableColumn<VoucherRequest, Double> unitValueColumn;
    @FXML private TableColumn<VoucherRequest, Double> totalValueColumn;
    @FXML private TableColumn<VoucherRequest, String> statusColumn;
    @FXML private TableColumn<VoucherRequest, String> paymentStatusColumn;

    // Search and filter
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> paymentFilter;

    // Buttons
    @FXML private Button addRequestButton;
    @FXML private Button editRequestButton;
    @FXML private Button deleteRequestButton;
    @FXML private Button updatePaymentButton;
    @FXML private Button approveButton;
    @FXML private Button generateVouchersButton;
    @FXML private Button exportExcelButton;

    // Add/Edit Form
    @FXML private VBox addForm;
    @FXML private Label formTitle;
    @FXML private ComboBox<Clients> clientCombo;
    @FXML private TextField numVouchersField;
    @FXML private TextField unitValueField;
    @FXML private Label totalValueLabel;
    @FXML private Label formError;
    @FXML private Button formSaveButton;
    @FXML private Button formCancelButton;

    // Payment Form
    @FXML private VBox paymentForm;
    @FXML private Label paymentRequestRef;
    @FXML private ComboBox<String> paymentStatusCombo;
    @FXML private TextField approverEmailField;
    @FXML private Label paymentError;
    @FXML private Button paymentSaveButton;
    @FXML private Button paymentCancelButton;

    private ObservableList<VoucherRequest> requestList = FXCollections.observableArrayList();
    private ObservableList<Clients> clientList = FXCollections.observableArrayList();
    private FilteredList<VoucherRequest> filteredRequests;
    private VoucherRequest editingRequest = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize database schema
        DatabaseSchema.ensureSchemaExists();
        
        // Setup table columns
        referenceColumn.setCellValueFactory(new PropertyValueFactory<>("requestReference"));
        clientColumn.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("numVouchers"));
        unitValueColumn.setCellValueFactory(new PropertyValueFactory<>("unitValue"));
        totalValueColumn.setCellValueFactory(new PropertyValueFactory<>("totalValue"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        // Setup filters
        filteredRequests = new FilteredList<>(requestList);
        requestsTable.setItems(filteredRequests);

        statusFilter.getItems().addAll("All", "initiated", "approved", "rejected", "completed");
        statusFilter.setValue("All");
        paymentFilter.getItems().addAll("All", "unpaid", "paid");
        paymentFilter.setValue("All");

        paymentStatusCombo.getItems().addAll("unpaid", "paid");

        // Load data
        loadClients();
        loadRequests();

        // Setup client ComboBox to display client names properly
        setupClientComboBox();

        // Hide forms initially
        addForm.setVisible(false);
        addForm.setManaged(false);
        paymentForm.setVisible(false);
        paymentForm.setManaged(false);

        // Setup listeners
        numVouchersField.textProperty().addListener((obs, old, val) -> calculateTotal());
        unitValueField.textProperty().addListener((obs, old, val) -> calculateTotal());

        statusFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        paymentFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void loadClients() {
        clientList.clear();
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clients ORDER BY nom_client")) {
            while (rs.next()) {
                Clients client = new Clients(
                    rs.getInt("ref_client"),
                    rs.getString("nom_client"),
                    rs.getString("email_client"),
                    rs.getString("address_client"),
                    rs.getString("phone_client")
                );
                clientList.add(client);
            }
            clientCombo.setItems(clientList);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading clients: " + e.getMessage());
        }
    }
    
    private void setupClientComboBox() {
        // Setup client ComboBox cell factory to display client names
        clientCombo.setCellFactory(param -> new javafx.scene.control.ListCell<Clients>() {
            @Override
            protected void updateItem(Clients client, boolean empty) {
                super.updateItem(client, empty);
                if (empty || client == null) {
                    setText(null);
                } else {
                    setText(client.getNom_client());
                }
            }
        });
        
        // Setup client ComboBox button cell to display selected client name
        clientCombo.setButtonCell(new javafx.scene.control.ListCell<Clients>() {
            @Override
            protected void updateItem(Clients client, boolean empty) {
                super.updateItem(client, empty);
                if (empty || client == null) {
                    setText(null);
                } else {
                    setText(client.getNom_client());
                }
            }
        });
    }

    private void loadRequests() {
        requestList.clear();
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM voucher_requests WHERE status != 'Redeemed' OR status IS NULL ORDER BY request_id DESC")) {
            while (rs.next()) {
                VoucherRequest req = new VoucherRequest();
                req.setRequestId(rs.getInt("request_id"));
                req.setRequestReference(rs.getString("request_reference"));
                req.setRefClient(rs.getInt("ref_client"));
                req.setClientName(rs.getString("client_name"));
                req.setNumVouchers(rs.getInt("num_vouchers"));
                req.setUnitValue(rs.getDouble("unit_value"));
                req.setTotalValue(rs.getDouble("total_value"));
                req.setStatus(rs.getString("status"));
                req.setPaymentStatus(rs.getString("payment_status"));
                if (rs.getTimestamp("payment_date") != null) {
                    req.setPaymentDate(rs.getTimestamp("payment_date"));
                }
                if (rs.getTimestamp("approval_date") != null) {
                    req.setApprovalDate(rs.getTimestamp("approval_date"));
                }
                req.setApprovedBy(rs.getString("approved_by"));
                req.setProcessedBy(rs.getString("processed_by"));
                requestList.add(req);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading requests: " + e.getMessage());
        }
    }

    private void applyFilters() {
        filteredRequests.setPredicate(request -> {
            String statusFilterValue = statusFilter.getValue();
            String paymentFilterValue = paymentFilter.getValue();
            String searchText = searchField.getText().toLowerCase();

            if (statusFilterValue != null && !statusFilterValue.equals("All") && 
                !request.getStatus().equals(statusFilterValue)) {
                return false;
            }

            if (paymentFilterValue != null && !paymentFilterValue.equals("All") && 
                !request.getPaymentStatus().equals(paymentFilterValue)) {
                return false;
            }

            if (searchText != null && !searchText.isEmpty()) {
                return request.getRequestReference().toLowerCase().contains(searchText) ||
                       request.getClientName().toLowerCase().contains(searchText);
            }

            return true;
        });
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleAddRequest() {
        isEditMode = false;
        editingRequest = null;
        formTitle.setText("Add New Voucher Request");
        formSaveButton.setText("Create Request");
        
        clientCombo.getSelectionModel().clearSelection();
        numVouchersField.clear();
        unitValueField.clear();
        totalValueLabel.setText("Total: Rs 0.00");
        formError.setText("");

        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    @FXML
    private void handleEditRequest() {
        VoucherRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a request to edit.");
            return;
        }

        isEditMode = true;
        editingRequest = selected;
        formTitle.setText("Edit Voucher Request");
        formSaveButton.setText("Update Request");

        // Find and select client
        for (Clients client : clientList) {
            if (client.getRef_client() == selected.getRefClient()) {
                clientCombo.setValue(client);
                break;
            }
        }

        numVouchersField.setText(String.valueOf(selected.getNumVouchers()));
        unitValueField.setText(String.valueOf(selected.getUnitValue()));
        totalValueLabel.setText("Total: Rs " + String.format("%.2f", selected.getTotalValue()));
        formError.setText("");

        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    @FXML
    private void handleFormSave() {
        if (clientCombo.getValue() == null) {
            formError.setText("Please select a client.");
            return;
        }

        int numVouchers;
        double unitValue;
        try {
            numVouchers = Integer.parseInt(numVouchersField.getText().trim());
            unitValue = Double.parseDouble(unitValueField.getText().trim());
        } catch (NumberFormatException e) {
            formError.setText("Please enter valid numbers for quantity and unit value.");
            return;
        }

        if (numVouchers <= 0 || unitValue <= 0) {
            formError.setText("Quantity and unit value must be greater than 0.");
            return;
        }

        double totalValue = numVouchers * unitValue;
        Clients selectedClient = clientCombo.getValue();
        String userName = UserSession.getInstance().getUsername();

        try (Connection conn = DBconnection.getConnection()) {
            if (isEditMode && editingRequest != null) {
                // Update existing request
                String sql = "UPDATE voucher_requests SET num_vouchers=?, unit_value=?, total_value=?, " +
                           "updated_at=CURRENT_TIMESTAMP WHERE request_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, numVouchers);
                    ps.setDouble(2, unitValue);
                    ps.setDouble(3, totalValue);
                    ps.setInt(4, editingRequest.getRequestId());
                    ps.executeUpdate();
                }

                AuditLogger.logAction("UPDATE", "VOUCHER_REQUEST", editingRequest.getRequestReference(),
                    userName, "Updated voucher request", null, 
                    numVouchers + " vouchers @ Rs " + unitValue, "Request update");

                showSuccess("Request updated successfully.");
            } else {
                // Create new request
                // First, get vouchers from the pool to use the first voucher code as request reference
                String requestReference = null;
                int requestId = 0;
                
                // Get available vouchers from the pool (oldest first)
                List<String> voucherCodes = getAvailableVoucherCodes(conn, selectedClient.getRef_client(), numVouchers, unitValue);
                
                if (voucherCodes.size() < numVouchers) {
                    formError.setText("Not enough vouchers available in the pool. Found " + voucherCodes.size() + 
                                    " vouchers, but " + numVouchers + " are required. Please generate more vouchers first.");
                    return;
                }
                
                // Take only the required number
                if (voucherCodes.size() > numVouchers) {
                    voucherCodes = voucherCodes.subList(0, numVouchers);
                }
                
                // Use the first voucher code as the request reference
                requestReference = voucherCodes.get(0);
                
                // Create the request with the voucher code as reference
                String sql = "INSERT INTO voucher_requests(request_reference, ref_client, client_name, " +
                           "num_vouchers, unit_value, total_value, status, payment_status, processed_by, " +
                           "created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, 'initiated', 'unpaid', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, requestReference);
                    ps.setInt(2, selectedClient.getRef_client());
                    ps.setString(3, selectedClient.getNom_client());
                    ps.setInt(4, numVouchers);
                    ps.setDouble(5, unitValue);
                    ps.setDouble(6, totalValue);
                    ps.setString(7, userName);
                    ps.executeUpdate();
                }

                // Get the request ID
                try (PreparedStatement getIdPs = conn.prepareStatement(
                    "SELECT request_id FROM voucher_requests WHERE request_reference = ?")) {
                    getIdPs.setString(1, requestReference);
                    try (ResultSet rs = getIdPs.executeQuery()) {
                        if (rs.next()) {
                            requestId = rs.getInt(1);
                        } else {
                            throw new SQLException("Could not retrieve request ID");
                        }
                    }
                }
                
                // Create invoice
                createInvoice(conn, requestReference, requestId, 
                            selectedClient.getRef_client(), selectedClient.getNom_client(), totalValue);

                // Assign vouchers from vouchers table (using the request reference which is the first voucher code)
                // This will mark them as assigned and remove them from the pool
                int assignedCount = assignVouchersToRequest(conn, requestId, requestReference, 
                                          selectedClient.getRef_client(), numVouchers, unitValue, voucherCodes);

                AuditLogger.logRequestCreation(requestReference, userName, 
                    selectedClient.getNom_client(), numVouchers);
                
                if (assignedCount < numVouchers) {
                    showSuccess("Request created successfully. Reference: " + requestReference + 
                              "\nNote: " + assignedCount + " of " + numVouchers + 
                              " vouchers were assigned from available pool. Remaining will be generated on approval.");
                } else {
                    showSuccess("Request created successfully. Reference: " + requestReference + 
                              "\nAll " + assignedCount + " vouchers were assigned from available pool.");
                }
            }

            loadRequests();
            handleFormCancel();
        } catch (SQLException e) {
            e.printStackTrace();
            formError.setText("Database error: " + e.getMessage());
        }
    }

    private String generateRequestReference(Connection conn) throws SQLException {
        // Get the next sequence number - handle both formats: VR0001 and VR0001-200
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(request_reference FROM 'VR(\\d+)') AS INTEGER)), 0) + 1 " +
                    "FROM voucher_requests WHERE request_reference ~ '^VR\\d+'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int nextNum = 1;
            if (rs.next()) {
                nextNum = rs.getInt(1);
            }
            
            // Ensure uniqueness by checking if the reference already exists
            String candidateRef = String.format("VR%04d", nextNum);
            int attempts = 0;
            while (attempts < 1000) {
                String checkSql = "SELECT COUNT(*) FROM voucher_requests WHERE request_reference = ?";
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setString(1, candidateRef);
                    try (ResultSet checkRs = checkPs.executeQuery()) {
                        if (checkRs.next() && checkRs.getInt(1) == 0) {
                            // Reference is unique
                            return candidateRef;
                        } else {
                            // Reference exists, try next number
                            nextNum++;
                            candidateRef = String.format("VR%04d", nextNum);
                            attempts++;
                        }
                    }
                }
            }
            // Fallback: use timestamp if we can't find a unique number
            return "VR" + System.currentTimeMillis();
        }
    }

    private void createInvoice(Connection conn, String requestReference, int requestId, 
                              int clientId, String clientName, double totalAmount) throws SQLException {
        String invoiceNumber = "INV-" + requestReference;
        String sql = "INSERT INTO invoices(invoice_number, request_id, request_reference, ref_client, " +
                   "client_name, total_amount, status, created_at) " +
                   "VALUES(?, ?, ?, ?, ?, ?, 'pending', CURRENT_TIMESTAMP)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            ps.setInt(2, requestId);
            ps.setString(3, requestReference);
            ps.setInt(4, clientId);
            ps.setString(5, clientName);
            ps.setDouble(6, totalAmount);
            ps.executeUpdate();
        }
    }


    @FXML
    private void handleEmailConfig() {
        pkg.vms.util.EmailConfigDialog.showConfigDialog();
    }

    @FXML
    private void handleFormCancel() {
        addForm.setVisible(false);
        addForm.setManaged(false);
        isEditMode = false;
        editingRequest = null;
    }

    private void calculateTotal() {
        try {
            int num = Integer.parseInt(numVouchersField.getText().trim());
            double unit = Double.parseDouble(unitValueField.getText().trim());
            double total = num * unit;
            totalValueLabel.setText("Total: Rs " + String.format("%.2f", total));
        } catch (NumberFormatException e) {
            totalValueLabel.setText("Total: Rs 0.00");
        }
    }

    @FXML
    private void handleUpdatePayment() {
        VoucherRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a request to update payment.");
            return;
        }

        paymentRequestRef.setText("Request: " + selected.getRequestReference());
        paymentStatusCombo.setValue(selected.getPaymentStatus());
        approverEmailField.clear();
        paymentError.setText("");

        paymentForm.setVisible(true);
        paymentForm.setManaged(true);
    }

    @FXML
    private void handlePaymentSave() {
        VoucherRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            paymentError.setText("No request selected.");
            return;
        }

        String newPaymentStatus = paymentStatusCombo.getValue();
        String approverEmail = approverEmailField.getText().trim();
        String userName = UserSession.getInstance().getUsername();

        if (newPaymentStatus == null) {
            paymentError.setText("Please select payment status.");
            return;
        }

        if (newPaymentStatus.equals("paid") && approverEmail.isEmpty()) {
            paymentError.setText("Please enter approver email for paid requests.");
            return;
        }

        try (Connection conn = DBconnection.getConnection()) {
            String sql = "UPDATE voucher_requests SET payment_status=?, payment_date=?, " +
                        "updated_at=CURRENT_TIMESTAMP WHERE request_id=?";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newPaymentStatus);
                if (newPaymentStatus.equals("paid")) {
                    ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                } else {
                    ps.setNull(2, Types.TIMESTAMP);
                }
                ps.setInt(3, selected.getRequestId());
                ps.executeUpdate();
            }

            // Update invoice status
            if (newPaymentStatus.equals("paid")) {
                String invoiceSql = "UPDATE invoices SET status='paid' WHERE request_reference=?";
                try (PreparedStatement ps = conn.prepareStatement(invoiceSql)) {
                    ps.setString(1, selected.getRequestReference());
                    ps.executeUpdate();
                }

                // Get client email
                String clientEmail = getClientEmail(selected.getRefClient());
                
                // Send email notification to approver
                if (approverEmail != null && !approverEmail.isEmpty()) {
                    EmailService.notifyApprover(approverEmail, selected.getRequestReference(), 
                        selected.getClientName(), selected.getTotalValue());
                }
                
                // Send email notification to client about payment received
                if (clientEmail != null && !clientEmail.isEmpty()) {
                    String subject = "Payment Received - Voucher Request " + selected.getRequestReference();
                    String body = String.format(
                        "Dear %s,\n\n" +
                        "We have received your payment for voucher request %s.\n\n" +
                        "Amount: Rs %.2f\n\n" +
                        "Your request is now being reviewed for approval.\n\n" +
                        "Thank you for your payment!\n\n" +
                        "VMS Team",
                        selected.getClientName(), selected.getRequestReference(), selected.getTotalValue()
                    );
                    EmailService.sendEmail(clientEmail, null, subject, body, null);
                }
            }

            AuditLogger.logPaymentUpdate(selected.getRequestReference(), userName, 
                selected.getPaymentStatus(), newPaymentStatus);

            showSuccess("Payment status updated successfully.");
            loadRequests();
            handlePaymentCancel();
        } catch (SQLException e) {
            e.printStackTrace();
            paymentError.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handlePaymentCancel() {
        paymentForm.setVisible(false);
        paymentForm.setManaged(false);
    }

    @FXML
    private void handleApprove() {
        VoucherRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a request to approve.");
            return;
        }

        if (!selected.getPaymentStatus().equals("paid")) {
            showError("Request must be marked as paid before approval.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Approve Request");
        confirm.setHeaderText("Approve Voucher Request");
        confirm.setContentText("Are you sure you want to approve request " + selected.getRequestReference() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBconnection.getConnection()) {
                    // Update vouchers status from Reserved to Active
                    String updateVouchersSql = "UPDATE vouchers SET status_voucher='Active' " +
                                            "WHERE request_reference = ? AND assigned_to_request = TRUE";
                    try (PreparedStatement ps = conn.prepareStatement(updateVouchersSql)) {
                        ps.setString(1, selected.getRequestReference());
                        ps.executeUpdate();
                    }
                    
                    String sql = "UPDATE voucher_requests SET status='approved', approved_by=?, " +
                               "approval_date=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE request_id=?";
                    
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, UserSession.getInstance().getUsername());
                        ps.setInt(2, selected.getRequestId());
                        ps.executeUpdate();
                    }
                    
                    // Vouchers remain assigned and become Active (not returned to pool on approval)
                    // They stay with the request and are not available in the vouchers pool

                    AuditLogger.logApproval(selected.getRequestReference(), 
                        UserSession.getInstance().getUsername());

                    // Send email notification to client about approval
                    String clientEmail = getClientEmail(selected.getRefClient());
                    if (clientEmail != null && !clientEmail.isEmpty()) {
                        String subject = "Voucher Request Approved - " + selected.getRequestReference();
                        String body = String.format(
                            "Dear %s,\n\n" +
                            "Your voucher request %s has been approved!\n\n" +
                            "Number of vouchers: %d\n" +
                            "Unit value: Rs %.2f\n" +
                            "Total value: Rs %.2f\n\n" +
                            "The vouchers will be generated and sent to you shortly.\n\n" +
                            "Thank you for your business!\n\n" +
                            "VMS Team",
                            selected.getClientName(), selected.getRequestReference(),
                            selected.getNumVouchers(), selected.getUnitValue(), selected.getTotalValue()
                        );
                        EmailService.sendEmail(clientEmail, null, subject, body, null);
                    }

                    showSuccess("Request approved successfully.");
                    loadRequests();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error approving request: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleGenerateVouchers() {
        VoucherRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select an approved request to generate vouchers.");
            return;
        }

        if (!selected.getStatus().equals("approved")) {
            showError("Only approved requests can generate vouchers.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Generate Vouchers");
        confirm.setHeaderText("Generate Vouchers");
        confirm.setContentText("Generate " + selected.getNumVouchers() + 
            " vouchers for request " + selected.getRequestReference() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                generateAndDispatchVouchers(selected);
            }
        });
    }

    private void generateAndDispatchVouchers(VoucherRequest request) {
        try {
            // Ensure database schema is up to date before generating vouchers
            DatabaseSchema.ensureSchemaExists();
            
            String outputDir = System.getProperty("user.dir") + "/vouchers/" + request.getRequestReference();
            List<String> pdfPaths = new ArrayList<>();
            String clientEmail = getClientEmail(request.getRefClient());
            String clientName = request.getClientName();

            // Check if vouchers are already assigned from vouchers table
            try (Connection conn = DBconnection.getConnection()) {
                // Get the voucher code column name
                String voucherCodeColumn = getVoucherCodeColumn(conn);
                
                // Check if vouchers are already linked to this request
                String checkSql = "SELECT COUNT(*) as cnt FROM vouchers WHERE request_reference = ? AND assigned_to_request = TRUE";
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setString(1, request.getRequestReference());
                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next() && rs.getInt("cnt") > 0) {
                            // Vouchers already assigned, use them
                            String selectSql = "SELECT " + voucherCodeColumn + 
                                             " FROM vouchers WHERE request_reference = ? AND assigned_to_request = TRUE";
                            try (PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
                                selectPs.setString(1, request.getRequestReference());
                                try (ResultSet voucherRs = selectPs.executeQuery()) {
                                    while (voucherRs.next()) {
                                        String voucherCode = voucherRs.getString(1);
                                        String expiryDate = LocalDate.now().plusDays(365).format(DateTimeFormatter.ISO_DATE);
                                        
                                        String pdfPath = PDFGenerator.generateVoucherPDF(
                                            voucherCode, clientName, request.getUnitValue(), expiryDate, outputDir
                                        );
                                        pdfPaths.add(pdfPath);
                                        
                                        // Update voucher status to Active
                                        String updateSql = "UPDATE vouchers SET status_voucher='Active', " +
                                                          "assigned_to_request=TRUE WHERE " + voucherCodeColumn + " = ?";
                                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                                            updatePs.setString(1, voucherCode);
                                            updatePs.executeUpdate();
                                        }
                                    }
                                }
                            }
                            
                            // Skip generation, vouchers already exist
                            if (!pdfPaths.isEmpty()) {
                                // Generate summary and send emails
                                String summaryPath = PDFGenerator.generateSummaryPDF(
                                    request.getRequestReference(), clientName, 
                                    pdfPaths.size(), request.getTotalValue(), outputDir
                                );
                                
                                String[] allPdfs = pdfPaths.toArray(new String[0]);
                                EmailService.sendVouchersToClient(clientEmail, null, request.getRequestReference(), 
                                    clientName, pdfPaths.size(), allPdfs);
                                
                                // Update request status
                                updateRequestAfterVoucherGeneration(conn, request, pdfPaths.size());
                                
                                showSuccess("Dispatched " + pdfPaths.size() + " vouchers successfully.");
                                loadRequests();
                                return;
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.out.println("Note: Could not check for existing vouchers: " + e.getMessage());
            }

            // Generate new vouchers with unique codes
            Connection voucherConn = DBconnection.getConnection();
            for (int i = 1; i <= request.getNumVouchers(); i++) {
                String voucherCode = generateUniqueVoucherCode(request.getRequestReference(), i, voucherConn);
                String expiryDate = LocalDate.now().plusDays(365).format(DateTimeFormatter.ISO_DATE);
                
                String pdfPath = PDFGenerator.generateVoucherPDF(
                    voucherCode, clientName, request.getUnitValue(), expiryDate, outputDir
                );
                pdfPaths.add(pdfPath);

                // Save voucher to database
                saveVoucherToDatabase(request, voucherCode, pdfPath);
            }

            // Generate summary PDF
            String summaryPath = PDFGenerator.generateSummaryPDF(
                request.getRequestReference(), clientName, 
                request.getNumVouchers(), request.getTotalValue(), outputDir
            );

            // Send emails
            String[] allPdfs = pdfPaths.toArray(new String[0]);
            EmailService.sendVouchersToClient(clientEmail, null, request.getRequestReference(), 
                clientName, request.getNumVouchers(), allPdfs);

            // Update request status
            updateRequestAfterVoucherGeneration(voucherConn, request, pdfPaths.size());

            AuditLogger.logVoucherGeneration(request.getRequestReference(), 
                UserSession.getInstance().getUsername(), request.getNumVouchers());
            AuditLogger.logVoucherDispatch(request.getRequestReference(), 
                UserSession.getInstance().getUsername(), clientEmail);

            showSuccess("Generated and dispatched " + request.getNumVouchers() + " vouchers successfully.");
            loadRequests();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error generating vouchers: " + e.getMessage());
        }
    }

    private void saveVoucherToDatabase(VoucherRequest request, String voucherCode, String pdfPath) 
            throws SQLException {
        // Get connection - DBconnection will handle reconnection if needed
        Connection conn = DBconnection.getConnection();
        if (conn == null) {
            throw new SQLException("Unable to get database connection");
        }
        
        // Verify connection is still valid
        try {
            if (conn.isClosed() || !conn.isValid(2)) {
                // Connection is closed or invalid, get a new one
                conn = DBconnection.getConnection();
            }
        } catch (SQLException e) {
            // Try to get a fresh connection
            conn = DBconnection.getConnection();
        }
        
        // Ensure all required columns exist before inserting
        try (Statement stmt = conn.createStatement()) {
            // Check and add ref_client column
            try {
                stmt.executeQuery("SELECT ref_client FROM vouchers LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist") || e.getMessage().contains("column")) {
                    try {
                        stmt.executeUpdate("ALTER TABLE vouchers ADD COLUMN ref_client INTEGER");
                        System.out.println("Added ref_client column to vouchers table");
                    } catch (SQLException addE) {
                        System.out.println("Note: Could not add ref_client column: " + addE.getMessage());
                    }
                }
            }
            
            // Check and add redeemed column (CRITICAL - this is causing the error)
            try {
                stmt.executeQuery("SELECT redeemed FROM vouchers LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist") || e.getMessage().contains("column")) {
                    try {
                        stmt.executeUpdate("ALTER TABLE vouchers ADD COLUMN redeemed BOOLEAN DEFAULT FALSE");
                        System.out.println("Added redeemed column to vouchers table");
                    } catch (SQLException addE) {
                        System.out.println("Note: Could not add redeemed column: " + addE.getMessage());
                    }
                }
            }
            
            // Check and add other required columns
            String[] requiredColumns = {
                "ref_voucher VARCHAR(100)",
                "val_voucher DECIMAL(10, 2)",
                "init_date DATE",
                "expiry_date DATE",
                "status_voucher VARCHAR(50)"
            };
            
            for (String colDef : requiredColumns) {
                String colName = colDef.split(" ")[0];
                try {
                    stmt.executeQuery("SELECT " + colName + " FROM vouchers LIMIT 1");
                } catch (SQLException e) {
                    if (e.getMessage().contains("does not exist") || e.getMessage().contains("column")) {
                        try {
                            stmt.executeUpdate("ALTER TABLE vouchers ADD COLUMN " + colDef);
                            System.out.println("Added " + colName + " column to vouchers table");
                        } catch (SQLException addE) {
                            // Column might already exist, ignore
                        }
                    }
                }
            }
        }
        
        // Ensure code_voucher column exists (needed as fallback or alternative)
        try (Statement stmt = conn.createStatement()) {
            try {
                stmt.executeQuery("SELECT code_voucher FROM vouchers LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist") || e.getMessage().contains("column")) {
                    try {
                        stmt.executeUpdate("ALTER TABLE vouchers ADD COLUMN code_voucher VARCHAR(100)");
                        System.out.println("Added code_voucher column to vouchers table");
                    } catch (SQLException addE) {
                        System.out.println("Note: Could not add code_voucher column: " + addE.getMessage());
                    }
                }
            }
        }
        
        // Check if ref_voucher is INTEGER or VARCHAR and handle accordingly
        String refVoucherType = "VARCHAR";
        boolean refVoucherExists = false;
        try (Statement checkStmt = conn.createStatement()) {
            try (ResultSet rs = checkStmt.executeQuery(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name = 'ref_voucher'")) {
                if (rs.next()) {
                    refVoucherExists = true;
                    String dataType = rs.getString("data_type");
                    if ("integer".equalsIgnoreCase(dataType) || "int4".equalsIgnoreCase(dataType)) {
                        refVoucherType = "INTEGER";
                    }
                }
            }
        } catch (SQLException e) {
            // Assume VARCHAR if we can't check
            System.out.println("Could not check ref_voucher type, assuming VARCHAR: " + e.getMessage());
        }
        
        // Determine which column to use for voucher code
        String voucherCodeColumn = "ref_voucher";
        if ("INTEGER".equals(refVoucherType) && refVoucherExists) {
            // ref_voucher is INTEGER, use code_voucher instead
            voucherCodeColumn = "code_voucher";
        }
        
        // Check if ref_request column exists and if it's required
        boolean refRequestExists = false;
        boolean refRequestRequired = false;
        boolean refRequestHasValidFK = false;
        try (Statement checkStmt = conn.createStatement()) {
            try (ResultSet rs = checkStmt.executeQuery(
                "SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name = 'ref_request'")) {
                if (rs.next()) {
                    refRequestExists = true;
                    String isNullable = rs.getString("is_nullable");
                    refRequestRequired = "NO".equalsIgnoreCase(isNullable);
                    
                    // Check if foreign key constraint exists and points to correct table
                    try (ResultSet fkRs = checkStmt.executeQuery(
                        "SELECT COUNT(*) as cnt FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name " +
                        "WHERE tc.table_name = 'vouchers' AND kcu.column_name = 'ref_request' " +
                        "AND tc.constraint_type = 'FOREIGN KEY'")) {
                        if (fkRs.next() && fkRs.getInt("cnt") > 0) {
                            // Check if it references voucher_requests
                            try (ResultSet refRs = checkStmt.executeQuery(
                                "SELECT kcu2.table_name FROM information_schema.table_constraints tc " +
                                "JOIN information_schema.key_column_usage kcu1 ON tc.constraint_name = kcu1.constraint_name " +
                                "JOIN information_schema.key_column_usage kcu2 ON tc.constraint_name = kcu2.constraint_name " +
                                "WHERE tc.table_name = 'vouchers' AND kcu1.column_name = 'ref_request' " +
                                "AND tc.constraint_type = 'FOREIGN KEY' AND kcu1.ordinal_position = kcu2.ordinal_position")) {
                                if (refRs.next()) {
                                    String refTable = refRs.getString("table_name");
                                    refRequestHasValidFK = "voucher_requests".equals(refTable);
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Column might not exist, that's ok
            System.out.println("Note: Could not check ref_request: " + e.getMessage());
        }
        
        // Build the INSERT statement with all required columns
        // Only include ref_request if it's required AND has a valid foreign key OR no foreign key
        boolean includeRefRequest = refRequestExists && refRequestRequired && 
                                    (!refRequestHasValidFK || refRequestHasValidFK);
        
        // If ref_request has invalid FK, don't include it (let it be NULL)
        if (refRequestExists && refRequestHasValidFK == false && refRequestRequired) {
            // Try to make it nullable first
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE vouchers ALTER COLUMN ref_request DROP NOT NULL");
                includeRefRequest = false; // Don't include it, let it be NULL
            } catch (SQLException e) {
                // If we can't make it nullable, we'll have to include it and hope the FK is fixed
                System.out.println("Note: Could not make ref_request nullable, will try to include it: " + e.getMessage());
            }
        }
        
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO vouchers(ref_client, ");
        sqlBuilder.append(voucherCodeColumn).append(", val_voucher, ");
        sqlBuilder.append("init_date, expiry_date, status_voucher, redeemed");
        
        // Add ref_request only if safe to do so
        if (includeRefRequest && refRequestHasValidFK) {
            sqlBuilder.append(", ref_request");
        }
        
        // Add optional columns
        sqlBuilder.append(", request_reference, pdf_path");
        
        sqlBuilder.append(") VALUES(?, ?, ?, CURRENT_DATE, CURRENT_DATE + INTERVAL '365 days', 'Active', FALSE");
        
        if (includeRefRequest && refRequestHasValidFK) {
            sqlBuilder.append(", ?"); // For ref_request
        }
        sqlBuilder.append(", ?, ?"); // For request_reference and pdf_path
        
        sqlBuilder.append(")");
        
        String sql = sqlBuilder.toString();
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, request.getRefClient());
            ps.setString(idx++, voucherCode);
            ps.setDouble(idx++, request.getUnitValue());
            
            if (includeRefRequest && refRequestHasValidFK) {
                ps.setInt(idx++, request.getRequestId());
            }
            
            ps.setString(idx++, request.getRequestReference());
            ps.setString(idx++, pdfPath);
            
            ps.executeUpdate();
        } catch (SQLException e) {
            // If foreign key error, try without ref_request
            if (e.getMessage().contains("foreign key constraint") || 
                e.getMessage().contains("violates foreign key")) {
                // Remove ref_request from insert
                String sqlWithoutFK = "INSERT INTO vouchers(ref_client, " + voucherCodeColumn + 
                    ", val_voucher, init_date, expiry_date, status_voucher, redeemed, request_reference, pdf_path) " +
                    "VALUES(?, ?, ?, CURRENT_DATE, CURRENT_DATE + INTERVAL '365 days', 'Active', FALSE, ?, ?)";
                
                try (PreparedStatement ps = conn.prepareStatement(sqlWithoutFK)) {
                    ps.setInt(1, request.getRefClient());
                    ps.setString(2, voucherCode);
                    ps.setDouble(3, request.getUnitValue());
                    ps.setString(4, request.getRequestReference());
                    ps.setString(5, pdfPath);
                    ps.executeUpdate();
                }
            } else if (e.getMessage().contains("does not exist") || e.getMessage().contains("column") || 
                       e.getMessage().contains("null value") || e.getMessage().contains("constraint")) {
                // Fallback: try minimal insert without optional columns
                String fallbackSql = "INSERT INTO vouchers(ref_client, " + voucherCodeColumn + 
                    ", val_voucher, init_date, expiry_date, status_voucher, redeemed) " +
                    "VALUES(?, ?, ?, CURRENT_DATE, CURRENT_DATE + INTERVAL '365 days', 'Active', FALSE)";
                
                try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                    ps.setInt(1, request.getRefClient());
                    ps.setString(2, voucherCode);
                    ps.setDouble(3, request.getUnitValue());
                    ps.executeUpdate();
                }
            } else {
                throw e; // Re-throw if it's a different error
            }
        }
    }

    private List<String> getAvailableVoucherCodes(Connection conn, int clientId, int numVouchers, double unitValue) throws SQLException {
        // Get available voucher codes from the pool - vouchers that are NOT assigned to any request
        String voucherCodeColumn = getVoucherCodeColumn(conn);
        
        List<String> availableVoucherCodes = new ArrayList<>();
        
        // Check if assigned_to_request and request_reference columns exist
        boolean hasAssignedColumn = false;
        boolean hasRequestRefColumn = false;
        try (Statement checkStmt = conn.createStatement();
             ResultSet colRs = checkStmt.executeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name IN ('assigned_to_request', 'request_reference')")) {
            while (colRs.next()) {
                String colName = colRs.getString("column_name");
                if ("assigned_to_request".equals(colName)) hasAssignedColumn = true;
                if ("request_reference".equals(colName)) hasRequestRefColumn = true;
            }
        }
        
        // Simple query: Get vouchers that are NOT assigned to any request
        // Don't filter by status - if it's in vouchers management and not assigned, it's available
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(redeemed = FALSE OR redeemed IS NULL) ");
        
        if (hasAssignedColumn) {
            whereClause.append("AND (assigned_to_request = FALSE OR assigned_to_request IS NULL) ");
        }
        if (hasRequestRefColumn) {
            whereClause.append("AND (request_reference IS NULL OR request_reference = '') ");
        }
        
        // Get vouchers from the pool - order by oldest first
        String selectSql = "SELECT " + voucherCodeColumn + " FROM vouchers " +
                          "WHERE " + whereClause.toString() +
                          "ORDER BY init_date ASC NULLS FIRST, " + voucherCodeColumn + " ASC LIMIT ?";
        
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, numVouchers);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString(1);
                    if (code != null && !code.isEmpty()) {
                        availableVoucherCodes.add(code);
                    }
                }
            }
        }
        
        // Debug: Log how many vouchers were found
        System.out.println("Found " + availableVoucherCodes.size() + " available vouchers out of " + numVouchers + " requested");
        if (availableVoucherCodes.size() < numVouchers) {
            // Log total count for debugging
            try (Statement stmt = conn.createStatement();
                 ResultSet countRs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM vouchers WHERE " + whereClause.toString())) {
                if (countRs.next()) {
                    int totalAvailable = countRs.getInt(1);
                    System.out.println("Total available vouchers in pool: " + totalAvailable);
                }
            }
        }
        
        return availableVoucherCodes;
    }
    
    private int assignVouchersToRequest(Connection conn, int requestId, String requestReference, 
                                       int clientId, int numVouchers, double unitValue, List<String> voucherCodes) throws SQLException {
        // Assign the provided voucher codes to the request
        // The request reference is already set to the first voucher code
        String voucherCodeColumn = getVoucherCodeColumn(conn);
        
        // Assign found vouchers to request
        int assignedCount = 0;
        if (!voucherCodes.isEmpty()) {
            String updateSql = "UPDATE vouchers SET request_id = ?, request_reference = ?, " +
                             "status_voucher = 'Reserved', assigned_to_request = TRUE, " +
                             "val_voucher = ?, ref_client = ? " +
                             "WHERE " + voucherCodeColumn + " = ?";
            
            for (String voucherCode : voucherCodes) {
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, requestId);
                    ps.setString(2, requestReference);
                    ps.setDouble(3, unitValue); // Set the unit value
                    ps.setInt(4, clientId); // Set the client reference
                    ps.setString(5, voucherCode);
                    ps.executeUpdate();
                    assignedCount++;
                }
            }
        }
        
        return assignedCount;
    }
    
    private String getVoucherCodeColumn(Connection conn) throws SQLException {
        // Check which column exists for voucher code
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
    
    private String generateUniqueVoucherCode(String requestReference, int index, Connection conn) throws SQLException {
        String baseCode = requestReference + "-" + index;
        String voucherCode = baseCode;
        int attempt = 0;
        
        String voucherCodeColumn = getVoucherCodeColumn(conn);
        String checkSql = "SELECT COUNT(*) FROM vouchers WHERE " + voucherCodeColumn + " = ?";
        
        // Ensure uniqueness
        while (attempt < 100) {
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, voucherCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        // Code is unique
                        break;
                    } else {
                        // Code exists, try with suffix
                        attempt++;
                        voucherCode = baseCode + "-" + attempt;
                    }
                }
            }
        }
        
        return voucherCode;
    }
    
    private void releaseVouchersFromRequest(Connection conn, String requestReference) throws SQLException {
        // Return vouchers to available pool if request is rejected/not approved/deleted
        // For vouchers generated from vouchers page (val_voucher was 0.0 when assigned), reset to 0.0
        // This ensures they appear back in the vouchers management page
        String voucherCodeColumn = getVoucherCodeColumn(conn);
        
        // Simple update: clear request assignment and reset to Available status
        // Reset val_voucher to 0.0 for vouchers that were assigned (they were from the pool)
        String updateSql = "UPDATE vouchers SET request_id = NULL, request_reference = NULL, " +
                         "status_voucher = 'Available', assigned_to_request = FALSE, " +
                         "val_voucher = 0.0, ref_client = NULL " +
                         "WHERE request_reference = ? AND assigned_to_request = TRUE";
        
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, requestReference);
            int updated = ps.executeUpdate();
            System.out.println("Returned " + updated + " vouchers to pool for request " + requestReference);
        }
    }
    
    private void updateRequestAfterVoucherGeneration(Connection conn, VoucherRequest request, int voucherCount) throws SQLException {
        // First ensure the columns exist
        try (Statement stmt = conn.createStatement()) {
            try {
                stmt.executeQuery("SELECT vouchers_generated FROM voucher_requests LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist") || e.getMessage().contains("column")) {
                    try {
                        stmt.executeUpdate("ALTER TABLE voucher_requests ADD COLUMN vouchers_generated BOOLEAN DEFAULT FALSE");
                        System.out.println("Added vouchers_generated column to voucher_requests table");
                    } catch (SQLException addE) {
                        System.out.println("Note: Could not add vouchers_generated column: " + addE.getMessage());
                    }
                }
            }
            
            try {
                stmt.executeQuery("SELECT vouchers_sent FROM voucher_requests LIMIT 1");
            } catch (SQLException e) {
                if (e.getMessage().contains("does not exist") || e.getMessage().contains("column")) {
                    try {
                        stmt.executeUpdate("ALTER TABLE voucher_requests ADD COLUMN vouchers_sent BOOLEAN DEFAULT FALSE");
                        System.out.println("Added vouchers_sent column to voucher_requests table");
                    } catch (SQLException addE) {
                        System.out.println("Note: Could not add vouchers_sent column: " + addE.getMessage());
                    }
                }
            }
        }
        
        // Check which columns exist and build update query accordingly
        boolean hasVouchersGenerated = false;
        boolean hasVouchersSent = false;
        
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'voucher_requests' AND column_name IN ('vouchers_generated', 'vouchers_sent')")) {
            while (rs.next()) {
                String colName = rs.getString("column_name");
                if ("vouchers_generated".equals(colName)) {
                    hasVouchersGenerated = true;
                } else if ("vouchers_sent".equals(colName)) {
                    hasVouchersSent = true;
                }
            }
        }
        
        // Build update query based on available columns
        String updateSql = "UPDATE voucher_requests SET status='completed', updated_at=CURRENT_TIMESTAMP";
        if (hasVouchersGenerated) {
            updateSql += ", vouchers_generated=TRUE";
        }
        if (hasVouchersSent) {
            updateSql += ", vouchers_sent=TRUE";
        }
        updateSql += " WHERE request_id=?";
        
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, request.getRequestId());
            ps.executeUpdate();
        }
    }
    
    private String getClientEmail(int clientId) {
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT email_client FROM clients WHERE ref_client=?")) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email_client");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    @FXML
    private void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write header
                writer.append("Request Reference,Client Name,Quantity,Unit Value,Total Value,Status,Payment Status,Payment Date,Approval Date\n");

                // Write data
                for (VoucherRequest req : filteredRequests) {
                    writer.append(String.format("\"%s\",\"%s\",%d,%.2f,%.2f,\"%s\",\"%s\",%s,%s\n",
                        req.getRequestReference(),
                        req.getClientName(),
                        req.getNumVouchers(),
                        req.getUnitValue(),
                        req.getTotalValue(),
                        req.getStatus(),
                        req.getPaymentStatus(),
                        req.getPaymentDate() != null ? req.getPaymentDate().toString() : "",
                        req.getApprovalDate() != null ? req.getApprovalDate().toString() : ""
                    ));
                }

                showSuccess("Exported " + filteredRequests.size() + " requests to " + file.getName());
            } catch (IOException e) {
                e.printStackTrace();
                showError("Error exporting to Excel: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteRequest() {
        VoucherRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a request to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Request");
        confirm.setHeaderText("Delete Voucher Request");
        confirm.setContentText("Are you sure you want to delete request " + 
            selected.getRequestReference() + "? This will return assigned vouchers to the pool.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBconnection.getConnection()) {
                    // First, return vouchers to the pool
                    releaseVouchersFromRequest(conn, selected.getRequestReference());
                    
                    // Then delete the request
                    try (PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM voucher_requests WHERE request_id=?")) {
                        ps.setInt(1, selected.getRequestId());
                        ps.executeUpdate();
                    }

                    AuditLogger.logAction("DELETE", "VOUCHER_REQUEST", selected.getRequestReference(),
                        UserSession.getInstance().getUsername(), "Deleted voucher request", null, null, "Request deletion");

                    showSuccess("Request deleted successfully. Vouchers have been returned to the pool.");
                    loadRequests();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showError("Error deleting request: " + e.getMessage());
                }
            }
        });
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
