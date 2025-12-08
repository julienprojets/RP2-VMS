package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import pkg.vms.DAO.ClientsDAO;
import pkg.vms.model.Clients;

import java.sql.SQLException;

public class ClientsController {

    @FXML private TableView<Clients> clientsTable;
    @FXML private TableColumn<Clients, Integer> colId;
    @FXML private TableColumn<Clients, String> colName;
    @FXML private TableColumn<Clients, String> colEmail;
    @FXML private TableColumn<Clients, String> colAddress;
    @FXML private TableColumn<Clients, String> colPhone;

    @FXML private TextField searchHeaderField;

    // Add Client Form
    @FXML private VBox addForm;
    @FXML private TextField addName;
    @FXML private TextField addEmail;
    @FXML private TextField addAddress;
    @FXML private TextField addPhone;
    @FXML private Label addError;

    // Switching Add/Edit Form
    @FXML private Label formTitle;
    @FXML private Button formSaveButton;

    // Figma View
    @FXML private VBox detailsPane;
    @FXML private Label detailsTitle;
    @FXML private Label detId, detName, detEmail, detAddress, detPhone, detRequests, detVouchers;

    // Switching between Add and Edit modes
    private boolean isEditMode = false;
    private Clients selectedClient = null;


    private final ClientsDAO dao = new ClientsDAO();
    private final ObservableList<Clients> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(cell -> cell.getValue().ref_clientProperty().asObject());
        colName.setCellValueFactory(cell -> cell.getValue().nom_clientProperty());
        colEmail.setCellValueFactory(cell -> cell.getValue().email_clientProperty());
        colAddress.setCellValueFactory(cell -> cell.getValue().address_clientProperty());
        colPhone.setCellValueFactory(cell -> cell.getValue().phone_clientProperty());

        loadClients();
        
        // Add Enter key support for search field
        if (searchHeaderField != null) {
            searchHeaderField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleSearch();
                }
            });
        }
    }

    private void loadClients() {
        try {
            data.clear();
            data.addAll(dao.getAll());
            clientsTable.setItems(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchHeaderField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            clientsTable.setItems(data);
            return;
        }
        ObservableList<Clients> filteredList = FXCollections.observableArrayList();
        for (Clients client : data) {
            if (String.valueOf(client.getRef_client()).contains(searchText)
                    || client.getNom_client().toLowerCase().contains(searchText)
                    || client.getEmail_client().toLowerCase().contains(searchText)
                    || client.getAddress_client().toLowerCase().contains(searchText)
                    || client.getPhone_client().toLowerCase().contains(searchText)) {
                filteredList.add(client);
            }
        }
        clientsTable.setItems(filteredList);
    }

// Add Client Form
    @FXML
    private void handleAdd() {
        System.out.println("Add button clicked!");
        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    // Cancel Add Client Form
    @FXML
    private void handleAddCancel() {
        addForm.setVisible(false);
        addForm.setManaged(false);

        addName.clear();
        addEmail.clear();
        addAddress.clear();
        addPhone.clear();
        addError.setText("");

        formTitle.setText("Add New Client");
        formSaveButton.setText("Save");

        isEditMode = false;
        selectedClient = null;
    }

    // Save Add/Edit Client Form
    @FXML
    private void handleFormSave() {

        String name = addName.getText().trim();
        String email = addEmail.getText().trim();
        String address = addAddress.getText().trim();
        String phone = addPhone.getText().trim();

        if (name.isEmpty()) {
            addError.setText("Name cannot be empty.");
            return;
        }

        try {
            if (isEditMode) {
                // --- UPDATE MODE ---
                selectedClient.setNom_client(name);
                selectedClient.setEmail_client(email);
                selectedClient.setAddress_client(address);
                selectedClient.setPhone_client(phone);

                dao.update(selectedClient);

            } else {
                // --- ADD MODE ---
                Clients c = new Clients(0, name, email, address, phone);
                dao.insert(c);
            }

            loadClients();          // refresh table
            handleAddCancel();      // hide form

        } catch (Exception e) {
            addError.setText("Error saving client.");
            e.printStackTrace();
        }
    }


    // Edit Client Form
    @FXML
    private void handleEdit() {
        selectedClient = clientsTable.getSelectionModel().getSelectedItem();

        if (selectedClient == null) return;

        isEditMode = true;

        formTitle.setText("Edit Client");
        formSaveButton.setText("Update");

        addName.setText(selectedClient.getNom_client());
        addEmail.setText(selectedClient.getEmail_client());
        addAddress.setText(selectedClient.getAddress_client());
        addPhone.setText(selectedClient.getPhone_client());

        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    // Figma View Details
    @FXML
    private void handleViewDetails() {
        Clients c = clientsTable.getSelectionModel().getSelectedItem();
        if (c == null) return;

        detailsPane.setVisible(true);
        detailsPane.setManaged(true);

        detailsTitle.setText("Client Details (Selected: "
                + c.getRef_client() + " - " + c.getNom_client() + ")");

        detId.setText(String.valueOf(c.getRef_client()));
        detName.setText(c.getNom_client());
        detEmail.setText(c.getEmail_client());
        detAddress.setText(c.getAddress_client());
        detPhone.setText(c.getPhone_client());

        detRequests.setText(String.valueOf(c.getRequests().size()));
        detVouchers.setText(String.valueOf(c.getVouchers().size()));
    }



    @FXML
    private void handleDelete() {
        Clients selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            dao.delete(selected.getRef_client());
            loadClients();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
