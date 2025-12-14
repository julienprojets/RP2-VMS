package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import pkg.vms.DAO.ClientsDAO;
import pkg.vms.model.Clients;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

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

        // Load data in background thread to prevent UI freezing
        new Thread(() -> {
            loadClients();
        }).start();
        
        // Add Enter key support for search field
        if (searchHeaderField != null) {
            searchHeaderField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleSearch();
                }
            });
        }
        
        // Restrict phone field to numbers only
        setupPhoneNumberValidation();
    }
    
    private void setupPhoneNumberValidation() {
        if (addPhone != null) {
            // Create a TextFormatter that only allows digits
            Pattern pattern = Pattern.compile("\\d*");
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String newText = change.getControlNewText();
                if (pattern.matcher(newText).matches()) {
                    return change;
                } else {
                    return null; // Reject the change
                }
            };
            
            TextFormatter<String> formatter = new TextFormatter<>(filter);
            addPhone.setTextFormatter(formatter);
            
            // Also add a listener to filter out non-numeric characters on paste
            addPhone.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.matches("\\d*")) {
                    addPhone.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });
        }
    }

    private void loadClients() {
        try {
            data.clear();
            data.addAll(dao.getAll());
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                clientsTable.setItems(data);
            });
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
        // Hide details pane
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
        
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


    // Edit Client Form - Toggle functionality
    @FXML
    private void handleEdit() {
        // If form is already visible, toggle it off
        if (addForm.isVisible() && isEditMode) {
            handleAddCancel();
            return;
        }
        
        selectedClient = clientsTable.getSelectionModel().getSelectedItem();

        if (selectedClient == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a client from the table to edit.");
            alert.showAndWait();
            return;
        }

        // Hide details pane
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);

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

    // Figma View Details - Toggle functionality
    @FXML
    private void handleViewDetails() {
        Clients c = clientsTable.getSelectionModel().getSelectedItem();
        
        // If details pane is already visible, toggle it off
        if (detailsPane.isVisible()) {
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);
            return;
        }
        
        // If no client selected, show warning
        if (c == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a client from the table to view details.");
            alert.showAndWait();
            return;
        }

        try {
            // Hide add form if visible
            addForm.setVisible(false);
            addForm.setManaged(false);

            // Show details pane
            detailsPane.setVisible(true);
            detailsPane.setManaged(true);

            // Set title
            detailsTitle.setText("Client Details (Selected: "
                    + c.getRef_client() + " - " + c.getNom_client() + ")");

            // Set client information with null safety
            detId.setText(String.valueOf(c.getRef_client()));
            detName.setText(c.getNom_client() != null ? c.getNom_client() : "");
            detEmail.setText(c.getEmail_client() != null ? c.getEmail_client() : "");
            detAddress.setText(c.getAddress_client() != null ? c.getAddress_client() : "");
            detPhone.setText(c.getPhone_client() != null ? c.getPhone_client() : "");

            // Set requests and vouchers count safely
            try {
                int requestsCount = c.getRequests() != null ? c.getRequests().size() : 0;
                detRequests.setText(String.valueOf(requestsCount));
            } catch (Exception e) {
                detRequests.setText("0");
            }

            try {
                int vouchersCount = c.getVouchers() != null ? c.getVouchers().size() : 0;
                detVouchers.setText(String.valueOf(vouchersCount));
            } catch (Exception e) {
                detVouchers.setText("0");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Failed to load client details: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }



    @FXML
    private void handleDelete() {
        Clients selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a client from the table to delete.");
            alert.showAndWait();
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Client");
        confirmAlert.setContentText("Are you sure you want to delete client '" + selected.getNom_client() + "'?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                dao.delete(selected.getRef_client());
                loadClients();
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Client deleted successfully.");
                successAlert.showAndWait();
            } catch (SQLException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Failed to delete client: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    @FXML
    private void handleExportExcel() {
        try {
            // Get the stage from any control
            Stage stage = (Stage) clientsTable.getScene().getWindow();
            
            // Create file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Clients to Excel");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            fileChooser.setInitialFileName("clients_export.csv");
            
            // Show save dialog
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                // Write CSV file
                try (FileWriter writer = new FileWriter(file)) {
                    // Write header
                    writer.append("ID,Name,Email,Phone,Address\n");
                    
                    // Write data
                    for (Clients client : data) {
                        writer.append(String.valueOf(client.getRef_client())).append(",");
                        writer.append(escapeCSV(client.getNom_client())).append(",");
                        writer.append(escapeCSV(client.getEmail_client())).append(",");
                        writer.append(escapeCSV(client.getPhone_client())).append(",");
                        writer.append(escapeCSV(client.getAddress_client())).append("\n");
                    }
                    
                    writer.flush();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Export Successful");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Clients exported successfully to:\n" + file.getAbsolutePath());
                    successAlert.showAndWait();
                } catch (IOException e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Export Error");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Failed to export clients: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Export Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Failed to export clients: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }
    
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
