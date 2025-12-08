package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Requests;
import pkg.vms.model.Vouchers;

import java.io.File;
import java.sql.*;
import java.util.Date;

public class RequestsController {

    @FXML private TableView<Requests> requestsTable;
    @FXML private TableColumn<Requests, Integer> idColumn;
    @FXML private TableColumn<Requests, String> statusColumn;
    @FXML private TableColumn<Requests, Integer> clientIdColumn;

    @FXML private Button addButton, editButton, deleteButton, uploadProofButton, approveButton;

    private ObservableList<Requests> requestList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> cell.getValue().refRequestProperty().asObject());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        clientIdColumn.setCellValueFactory(cell -> cell.getValue().refClientProperty().asObject());

        requestsTable.setItems(requestList);
        loadRequests();
    }

    private void loadRequests() {
        requestList.clear();
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM requests")) {

            while (rs.next()) {
                Requests r = new Requests(
                        rs.getInt("ref_request"),
                        new java.util.Date(rs.getDate("creation_date").getTime()), // convert sql.Date -> util.Date
                        rs.getInt("num_voucher"),
                        rs.getString("status"),
                        rs.getString("payment"),
                        rs.getDate("date_payment") != null ? new java.util.Date(rs.getDate("date_payment").getTime()) : null,
                        rs.getInt("ref_payment"),
                        rs.getDate("date_approval") != null ? new java.util.Date(rs.getDate("date_approval").getTime()) : null,
                        rs.getInt("duration_voucher"),
                        rs.getInt("ref_client"),
                        rs.getString("processed_by"),
                        rs.getString("approved_by"),
                        rs.getString("validated_by")
                );
                requestList.add(r);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddRequest() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Request");
        dialog.setHeaderText("Enter number of vouchers:");
        dialog.showAndWait().ifPresent(input -> {
            try {
                int numVouchers = Integer.parseInt(input);
                int clientId = 1; // TODO: replace with selected client or dynamic choice
                try (Connection conn = DBconnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO requests(status, ref_client, num_voucher, creation_date) VALUES(?, ?, ?, ?)")) {

                    ps.setString(1, "initiated");
                    ps.setInt(2, clientId);
                    ps.setInt(3, numVouchers);
                    ps.setDate(4, new java.sql.Date(new Date().getTime()));
                    ps.executeUpdate();
                    loadRequests();
                }
            } catch (NumberFormatException | SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleEditRequest() {
        Requests selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getStatus());
        dialog.setTitle("Edit Request Status");
        dialog.setHeaderText("Update status:");
        dialog.showAndWait().ifPresent(status -> {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE requests SET status=? WHERE ref_request=?")) {
                ps.setString(1, status);
                ps.setInt(2, selected.getRef_request());
                ps.executeUpdate();
                loadRequests();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleDeleteRequest() {
        Requests selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM requests WHERE ref_request=?")) {
            ps.setInt(1, selected.getRef_request());
            ps.executeUpdate();
            loadRequests();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUploadProof() {
        Requests selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select proof of payment");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE requests SET proof_file=? WHERE ref_request=?")) {
                ps.setString(1, file.getAbsolutePath());
                ps.setInt(2, selected.getRef_request());
                ps.executeUpdate();
                loadRequests();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleApproveRequest() {
        Requests selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE requests SET status=? WHERE ref_request=?")) {
            ps.setString(1, "approved");
            ps.setInt(2, selected.getRef_request());
            ps.executeUpdate();

            // After approval, generate vouchers
            VouchersController.generateVouchersForRequest(selected);

            loadRequests();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
