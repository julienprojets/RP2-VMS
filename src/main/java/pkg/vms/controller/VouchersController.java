package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Requests;
import pkg.vms.model.Vouchers;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VouchersController {

    /* ============================
            TABLE + COLUMNS
       ============================ */
    @FXML private TableView<Vouchers> voucherTable;
    @FXML private TableColumn<Vouchers, String> codeColumn;
    @FXML private TableColumn<Vouchers, Double> priceColumn;
    @FXML private TableColumn<Vouchers, String> statusColumn;
    @FXML private TableColumn<Vouchers, Date> initDateColumn;
    @FXML private TableColumn<Vouchers, Date> expiryDateColumn;

    /* ============================
            TOP BUTTONS
       ============================ */
    @FXML private Button addButton, editButton, deleteButton, generateButton;

    /* ============================
            SEARCH BAR
       ============================ */
    @FXML private TextField searchHeaderField;

    /* ============================
            ADD / EDIT FORM
       ============================ */
    @FXML private VBox addForm;

    @FXML private TextField formCode;
    @FXML private TextField formPrice;
    @FXML private TextField formStatus;
    @FXML private DatePicker formInitDate;
    @FXML private DatePicker formExpiryDate;
    @FXML private Label formError;

    private boolean editMode = false;
    private Vouchers editingVoucher = null;

    /* ============================
            DATA LIST
       ============================ */
    private ObservableList<Vouchers> voucherList = FXCollections.observableArrayList();


    /* ============================
            INITIALIZE
       ============================ */
    @FXML
    public void initialize() {
        codeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCode_voucher()));
        priceColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getPrice()));
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus_voucher()));
        initDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getInit_date()));
        expiryDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getExpiry_date()));

        voucherTable.setItems(voucherList);

        addForm.setVisible(false);
        addForm.setManaged(false);

        loadVouchers();
    }


    /* ============================
            LOAD VOUCHERS
       ============================ */
    private void loadVouchers() {
        voucherList.clear();
        String sql = "SELECT * FROM vouchers";

        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Vouchers v = new Vouchers(
                        rs.getInt("ref_client"),
                        rs.getString("ref_voucher"),
                        rs.getDouble("val_voucher"),
                        rs.getBoolean("redeemed")
                );
                v.setInit_date(rs.getDate("init_date"));
                v.setExpiry_date(rs.getDate("expiry_date"));
                v.setDate_redeemed(rs.getDate("date_redeemed"));
                v.setBearer(rs.getString("bearer"));
                v.setStatus_voucher(rs.getString("status_voucher"));

                voucherList.add(v);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        formCode.clear();
        formPrice.clear();
        formStatus.clear();
        formInitDate.setValue(null);
        formExpiryDate.setValue(null);
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
        if (selected == null) return;

        editMode = true;
        editingVoucher = selected;

        formCode.setText(selected.getCode_voucher());
        formPrice.setText(String.valueOf(selected.getPrice()));
        formStatus.setText(selected.getStatus_voucher());

        if (selected.getInit_date() != null)
            formInitDate.setValue(new java.sql.Date(selected.getInit_date().getTime()).toLocalDate());

        if (selected.getExpiry_date() != null)
            formExpiryDate.setValue(new java.sql.Date(selected.getExpiry_date().getTime()).toLocalDate());

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
        if (selected == null) return;

        voucherList.remove(selected);

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM vouchers WHERE ref_voucher=?")) {

            ps.setString(1, selected.getCode_voucher());
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /* ============================
            SAVE FORM (ADD / EDIT)
       ============================ */
    @FXML
    private void handleFormSave() {
        try {
            String code = formCode.getText().trim();
            double price = Double.parseDouble(formPrice.getText().trim());
            String status = formStatus.getText().trim();

            java.sql.Date init = formInitDate.getValue() != null
                    ? java.sql.Date.valueOf(formInitDate.getValue())
                    : null;

            java.sql.Date exp = formExpiryDate.getValue() != null
                    ? java.sql.Date.valueOf(formExpiryDate.getValue())
                    : null;

            if (code.isEmpty() || status.isEmpty()) {
                formError.setText("All fields are required.");
                return;
            }

            if (editMode) {
                // update existing voucher
                editingVoucher.setCode_voucher(code);
                editingVoucher.setPrice(price);
                editingVoucher.setStatus_voucher(status);
                editingVoucher.setInit_date(init);
                editingVoucher.setExpiry_date(exp);

            } else {
                // create new voucher object
                Vouchers v = new Vouchers(0, code, price, false);
                v.setStatus_voucher(status);
                v.setInit_date(init);
                v.setExpiry_date(exp);

                voucherList.add(v);
            }

            addForm.setVisible(false);
            addForm.setManaged(false);

            voucherTable.refresh();

        } catch (Exception e) {
            formError.setText("Invalid input. Check values again.");
        }
    }


    /* ============================
            CANCEL FORM
       ============================ */
    @FXML
    private void handleCancel() {
        addForm.setVisible(false);
        addForm.setManaged(false);
    }


    /* ============================
            GENERATE VOUCHERS (dialog)
       ============================ */
    @FXML
    private void handleGenerate() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Generate");
        alert.setHeaderText("Voucher generation feature is called.");
        alert.setContentText("You can connect this to Requests later.");
        alert.show();
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
