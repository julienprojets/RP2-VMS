package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Branch;

import java.sql.*;

public class BranchController {

    @FXML private TableView<Branch> branchTable;
    @FXML private TableColumn<Branch, Integer> idColumn;
    @FXML private TableColumn<Branch, String> locationColumn;
    @FXML private TableColumn<Branch, String> responsibleColumn;

    @FXML private Button addButton, editButton, deleteButton;

    private ObservableList<Branch> branchList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> cell.getValue().branchIdProperty().asObject());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        responsibleColumn.setCellValueFactory(cell -> cell.getValue().responsibleUserProperty());

        branchTable.setItems(branchList);
        loadBranches();
    }

    private void loadBranches() {
        branchList.clear();
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM branch")) {

            while (rs.next()) {
                Branch branch = new Branch(
                        rs.getInt("branch_id"),
                        rs.getString("location"),
                        rs.getString("responsible_user")
                );
                branchList.add(branch);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddBranch() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Branch");
        dialog.setHeaderText("Enter location:");
        dialog.showAndWait().ifPresent(location -> {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO branch(location, responsible_user) VALUES(?, ?)")) {
                ps.setString(1, location);
                ps.setString(2, "ResponsibleUser");
                ps.executeUpdate();
                loadBranches();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleEditBranch() {
        Branch selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.getLocation());
        dialog.setTitle("Edit Branch Location");
        dialog.setHeaderText("Enter new location:");
        dialog.showAndWait().ifPresent(location -> {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE branch SET location=? WHERE branch_id=?")) {
                ps.setString(1, location);
                ps.setInt(2, selected.getBranchId());
                ps.executeUpdate();
                loadBranches();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleDeleteBranch() {
        Branch selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM branch WHERE branch_id=?")) {
            ps.setInt(1, selected.getBranchId());
            ps.executeUpdate();
            loadBranches();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
