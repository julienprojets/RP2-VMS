package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Requests;
import pkg.vms.model.Vouchers;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VouchersController {

    @FXML private TableView<Vouchers> voucherTable;
    @FXML private TableColumn<Vouchers, String> codeColumn;
    @FXML private TableColumn<Vouchers, Double> priceColumn;
    @FXML private TableColumn<Vouchers, String> statusColumn;
    @FXML private TableColumn<Vouchers, Date> initDateColumn;
    @FXML private TableColumn<Vouchers, Date> expiryDateColumn;

    @FXML private Button addButton, editButton, deleteButton, generateButton;

    private ObservableList<Vouchers> voucherList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        codeColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getCode_voucher()));
        priceColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getPrice()));
        statusColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus_voucher()));
        initDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getInit_date()));
        expiryDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue().getExpiry_date()));

        voucherTable.setItems(voucherList);

        loadVouchers();
    }

    /** Load vouchers from the database */
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

    /** Generate vouchers for a request */
    public static List<Vouchers> generateVouchersForRequest(Requests request) {
        List<Vouchers> vouchers = new ArrayList<>();

        for (int i = 0; i < request.getNum_voucher(); i++) {
            String code = "VCHR-" + request.getRef_request() + "-" + (i + 1);
            Vouchers v = new Vouchers(
                    request.getRef_client(),
                    code,
                    request.getVal_voucher(), // or a default value
                    false
            );
            Date now = new Date();
            v.setInit_date(now);
            long expiryMillis = now.getTime() + (request.getDuration_voucher() * 24L * 60L * 60L * 1000L);
            v.setExpiry_date(new Date(expiryMillis));

            // insert into database
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

    @FXML
    private void handleAddVoucher() {
        // Simple dialog to add voucher manually
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Voucher");
        dialog.setHeaderText("Enter voucher code:");
        dialog.showAndWait().ifPresent(code -> {
            Vouchers v = new Vouchers(0, code, 0, false); // client 0 for manual
            v.setStatus_voucher("Active");
            voucherList.add(v);
            // TODO: insert into DB if needed
        });
    }

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
}
