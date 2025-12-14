package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Branch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BranchController {

    @FXML private TableView<Branch> branchTable;
    @FXML private TableColumn<Branch, Integer> idColumn;
    @FXML private TableColumn<Branch, String> companyColumn;
    @FXML private TableColumn<Branch, String> industryColumn;
    @FXML private TableColumn<Branch, String> locationColumn;
    @FXML private TableColumn<Branch, String> responsibleColumn;

    @FXML private TextField searchHeaderField;
    @FXML private ComboBox<String> companyFilterCombo;
    @FXML private Button addCompanyButton;

    // Add Branch Form
    @FXML private ScrollPane addFormScrollPane;
    @FXML private VBox addForm;
    @FXML private TextField addLocation;
    @FXML private TextField addPhone;
    @FXML private TextField addUserEmail;
    @FXML private ComboBox<String> addCompanyCombo;
    @FXML private ComboBox<String> addResponsibleCombo;
    @FXML private Label addError;
    @FXML private Label formTitle;
    @FXML private Button formSaveButton;

    // Add Company Form
    @FXML private VBox addCompanyForm;
    @FXML private TextField addCompanyName;
    @FXML private TextField addCompanyIndustry;
    @FXML private Label companyError;

    // Delete Company Form
    @FXML private VBox deleteCompanyForm;
    @FXML private Label deleteCompanyInfo;
    @FXML private Label deleteCompanyBranches;
    @FXML private Label deleteCompanyError;

    // Branch Details Panel
    @FXML private VBox detailsPane;
    @FXML private Label detailsTitle;
    @FXML private Label detId, detCompany, detIndustry, detLocation, detPhone, detResponsible;

    private ObservableList<Branch> branchList = FXCollections.observableArrayList();
    private FilteredList<Branch> filteredBranches;
    private boolean isEditMode = false;
    private Branch selectedBranch = null;
    private Map<String, String> companyIndustryMap = new HashMap<>(); // Maps company name to industry

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> cell.getValue().branchIdProperty().asObject());
        companyColumn.setCellValueFactory(cell -> cell.getValue().companyProperty());
        industryColumn.setCellValueFactory(cell -> cell.getValue().industryProperty());
        locationColumn.setCellValueFactory(cell -> cell.getValue().locationProperty());
        responsibleColumn.setCellValueFactory(cell -> cell.getValue().responsibleUserProperty());

        // Setup filtered list
        filteredBranches = new FilteredList<>(branchList, p -> true);
        branchTable.setItems(filteredBranches);

        // Load data in background thread to prevent UI freezing
        new Thread(() -> {
            // Ensure company, phone, and industry columns exist in database
            ensureCompanyColumnExists();
            ensurePhoneColumnExists();
            ensureIndustryColumnExists();
            
            loadBranches();
            loadCompanies();
            loadUsers();
        }).start();

        // Add Enter key support for search field
        if (searchHeaderField != null) {
            searchHeaderField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleSearch();
                }
            });
        }

        // Setup phone number validation (numbers only)
        setupPhoneNumberValidation();
    }

    private void setupPhoneNumberValidation() {
        if (addPhone != null) {
            addPhone.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.matches("\\d*")) {
                    addPhone.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });
        }
    }

    private void ensurePhoneColumnExists() {
        try (Connection conn = DBconnection.getConnection()) {
            // Check if phone column exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "branch", "phone");
            
            if (!columns.next()) {
                // Column doesn't exist, add it
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE branch ADD COLUMN phone VARCHAR(20)");
                }
            }
        } catch (SQLException e) {
            // Column might already exist or other error - continue
            System.out.println("Note: Phone column check: " + e.getMessage());
        }
    }

    private void ensureIndustryColumnExists() {
        try (Connection conn = DBconnection.getConnection()) {
            // Check if industry column exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "branch", "industry");
            
            if (!columns.next()) {
                // Column doesn't exist, add it
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE branch ADD COLUMN industry VARCHAR(255)");
                }
            }
        } catch (SQLException e) {
            // Column might already exist or other error - continue
            System.out.println("Note: Industry column check: " + e.getMessage());
        }
    }

    private void ensureCompanyColumnExists() {
        try (Connection conn = DBconnection.getConnection()) {
            // Check if company column exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "branch", "company");
            
            if (!columns.next()) {
                // Column doesn't exist, add it
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE branch ADD COLUMN company VARCHAR(255)");
                }
            }
        } catch (SQLException e) {
            // Column might already exist or other error - continue
            System.out.println("Note: Company column check: " + e.getMessage());
        }
    }

    private void loadBranches() {
        branchList.clear();
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM branch")) {

            while (rs.next()) {
                String location = rs.getString("location");
                String company = null;
                String phone = null;
                String industry = null;
                
                // Try to get company column
                try {
                    company = rs.getString("company");
                } catch (SQLException e) {
                    // Column doesn't exist yet
                }
                
                // Try to get phone column
                try {
                    phone = rs.getString("phone");
                } catch (SQLException e) {
                    // Column doesn't exist yet
                }
                
                // Try to get industry column
                try {
                    industry = rs.getString("industry");
                } catch (SQLException e) {
                    // Column doesn't exist yet
                }
                
                if (company == null || company.isEmpty()) {
                    company = "Default";
                }
                if (phone == null) {
                    phone = "";
                }
                if (industry == null) {
                    industry = "";
                }

                Branch branch = new Branch(
                        rs.getInt("branch_id"),
                        location != null ? location : "",
                        rs.getString("responsible_user") != null ? rs.getString("responsible_user") : "",
                        company,
                        phone,
                        industry
                );
                branchList.add(branch);
            }

            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                filteredBranches = new FilteredList<>(branchList, p -> true);
                branchTable.setItems(filteredBranches);
                // Refresh company filter
                loadCompanies();
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadCompanies() {
        Set<String> companies = new HashSet<>();
        companies.add("All Companies");
        
        for (Branch branch : branchList) {
            if (branch.getCompany() != null && !branch.getCompany().isEmpty()) {
                companies.add(branch.getCompany());
            }
        }

        companyFilterCombo.getItems().clear();
        companyFilterCombo.getItems().addAll(companies);
        companyFilterCombo.setValue("All Companies");

        // Also update add branch form company combo
        addCompanyCombo.getItems().clear();
        for (String company : companies) {
            if (!company.equals("All Companies")) {
                addCompanyCombo.getItems().add(company);
            }
        }
    }

    private void loadUsers() {
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM users")) {

            javafx.collections.ObservableList<String> usernames = javafx.collections.FXCollections.observableArrayList();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                addResponsibleCombo.getItems().clear();
                addResponsibleCombo.getItems().addAll(usernames);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchHeaderField.getText().toLowerCase();
        String selectedCompany = companyFilterCombo.getValue();

        filteredBranches.setPredicate(branch -> {
            boolean matchesSearch = searchText.isEmpty() ||
                    String.valueOf(branch.getBranchId()).contains(searchText) ||
                    (branch.getLocation() != null && branch.getLocation().toLowerCase().contains(searchText)) ||
                    (branch.getCompany() != null && branch.getCompany().toLowerCase().contains(searchText)) ||
                    (branch.getIndustry() != null && branch.getIndustry().toLowerCase().contains(searchText)) ||
                    (branch.getResponsibleUser() != null && branch.getResponsibleUser().toLowerCase().contains(searchText));

            boolean matchesCompany = selectedCompany == null || 
                    selectedCompany.equals("All Companies") ||
                    (branch.getCompany() != null && branch.getCompany().equals(selectedCompany));

            return matchesSearch && matchesCompany;
        });
    }

    @FXML
    private void handleCompanyFilter() {
        handleSearch(); // Re-apply search with new filter
    }

    @FXML
    private void handleAddCompany() {
        // Hide other forms
        if (addForm != null) {
            addForm.setVisible(false);
            addForm.setManaged(false);
        }
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
        deleteCompanyForm.setVisible(false);
        deleteCompanyForm.setManaged(false);

        // Show add company form
        addCompanyForm.setVisible(true);
        addCompanyForm.setManaged(true);
        addCompanyName.clear();
        addCompanyIndustry.clear();
        companyError.setText("");
    }

    @FXML
    private void handleCompanySave() {
        String companyName = addCompanyName.getText().trim();
        String industry = addCompanyIndustry.getText().trim();
        
        if (companyName.isEmpty()) {
            companyError.setText("Company name cannot be empty.");
            return;
        }

        // Check if company already exists
        if (companyFilterCombo.getItems().contains(companyName)) {
            companyError.setText("Company already exists.");
            return;
        }

        // Store company-industry mapping
        if (!industry.isEmpty()) {
            companyIndustryMap.put(companyName, industry);
        }
        
        // Company is stored in the branch table, so we'll add it when creating a branch
        // For now, just add it to the combo box
        companyFilterCombo.getItems().add(companyName);
        addCompanyCombo.getItems().add(companyName);
        
        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Success");
        successAlert.setHeaderText(null);
        successAlert.setContentText("Company added successfully. You can now create branches for this company.");
        successAlert.showAndWait();

        handleCompanyCancel();
    }

    @FXML
    private void handleCompanyCancel() {
        addCompanyForm.setVisible(false);
        addCompanyForm.setManaged(false);
    }

    @FXML
    private void handleDeleteCompany() {
        String selectedCompany = companyFilterCombo.getValue();
        
        if (selectedCompany == null || selectedCompany.isEmpty() || selectedCompany.equals("All Companies")) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a company from the filter dropdown to delete.");
            alert.showAndWait();
            return;
        }

        // Hide other forms
        addForm.setVisible(false);
        addForm.setManaged(false);
        addCompanyForm.setVisible(false);
        addCompanyForm.setManaged(false);
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);

        // Count branches for this company
        int branchCount = 0;
        StringBuilder branchList = new StringBuilder();
        for (Branch branch : this.branchList) {
            if (branch.getCompany() != null && branch.getCompany().equals(selectedCompany)) {
                branchCount++;
                if (branchList.length() > 0) branchList.append(", ");
                branchList.append(branch.getLocation());
            }
        }

        // Show delete company form
        deleteCompanyInfo.setText("You are about to delete company: " + selectedCompany);
        
        if (branchCount > 0) {
            deleteCompanyBranches.setText("WARNING: This will also delete " + branchCount + " associated branch(es):\n" + branchList.toString());
        } else {
            deleteCompanyBranches.setText("No associated branches found.");
        }
        
        deleteCompanyError.setText("");

        deleteCompanyForm.setVisible(true);
        deleteCompanyForm.setManaged(true);
    }

    @FXML
    private void handleConfirmDeleteCompany() {
        String selectedCompany = companyFilterCombo.getValue();
        
        if (selectedCompany == null || selectedCompany.isEmpty() || selectedCompany.equals("All Companies")) {
            deleteCompanyError.setText("No company selected.");
            return;
        }

        try (Connection conn = DBconnection.getConnection()) {
            // Delete all branches associated with this company first
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM branch WHERE company = ?")) {
                ps.setString(1, selectedCompany);
                int branchesDeleted = ps.executeUpdate();
                
                // Remove from combo boxes
                companyFilterCombo.getItems().remove(selectedCompany);
                addCompanyCombo.getItems().remove(selectedCompany);
                
                // Reset filter if deleted company was selected
                if (companyFilterCombo.getValue() == null || companyFilterCombo.getValue().equals(selectedCompany)) {
                    companyFilterCombo.setValue("All Companies");
                }
                
                // Reload branches
                loadBranches();
                handleSearch(); // Refresh filtered view
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Company '" + selectedCompany + "' and " + branchesDeleted + " associated branch(es) deleted successfully.");
                successAlert.showAndWait();
                
                handleCancelDeleteCompany();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            deleteCompanyError.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelDeleteCompany() {
        deleteCompanyForm.setVisible(false);
        deleteCompanyForm.setManaged(false);
        deleteCompanyError.setText("");
    }

    @FXML
    private void handleAddBranch() {
        // Hide other forms
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
        addCompanyForm.setVisible(false);
        addCompanyForm.setManaged(false);
        deleteCompanyForm.setVisible(false);
        deleteCompanyForm.setManaged(false);

        isEditMode = false;
        selectedBranch = null;

        formTitle.setText("Add New Branch");
        formSaveButton.setText("Save");

        addLocation.clear();
        addPhone.clear();
        addCompanyCombo.getSelectionModel().clearSelection();
        addCompanyCombo.setValue(null);
        addResponsibleCombo.getSelectionModel().clearSelection();
        addResponsibleCombo.setValue(null);
        addUserEmail.clear();
        addError.setText("");

        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    @FXML
    private void handleUserSelection() {
        String selectedUsername = addResponsibleCombo.getValue();
        if (selectedUsername != null && !selectedUsername.isEmpty()) {
            // Fetch user email from database
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT email_user FROM users WHERE username = ?")) {
                ps.setString(1, selectedUsername);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String email = rs.getString("email_user");
                    if (addUserEmail != null) {
                        addUserEmail.setText(email != null ? email : "");
                    }
                } else {
                    if (addUserEmail != null) {
                        addUserEmail.setText("");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                if (addUserEmail != null) {
                    addUserEmail.setText("");
                }
            }
        } else {
            if (addUserEmail != null) {
                addUserEmail.clear();
            }
        }
    }

    @FXML
    private void handleAddCancel() {
        addForm.setVisible(false);
        addForm.setManaged(false);
        isEditMode = false;
        selectedBranch = null;
    }

    @FXML
    private void handleFormSave() {
        String location = addLocation.getText().trim();
        String phone = addPhone.getText().trim();
        String company = addCompanyCombo.getValue();
        String responsible = addResponsibleCombo.getValue();
        String industry = companyIndustryMap.getOrDefault(company, "");

        if (location.isEmpty()) {
            addError.setText("Location cannot be empty.");
            return;
        }

        if (company == null || company.isEmpty()) {
            addError.setText("Please select a company.");
            return;
        }

        try (Connection conn = DBconnection.getConnection()) {
            if (isEditMode && selectedBranch != null) {
                // UPDATE EXISTING BRANCH
                try {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE branch SET location=?, responsible_user=?, company=?, phone=?, industry=? WHERE branch_id=?");
                    ps.setString(1, location);
                    ps.setString(2, responsible != null ? responsible : "");
                    ps.setString(3, company);
                    ps.setString(4, phone);
                    ps.setString(5, industry);
                    ps.setInt(6, selectedBranch.getBranchId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    // If columns don't exist, try without them
                    try {
                        PreparedStatement ps = conn.prepareStatement(
                                "UPDATE branch SET location=?, responsible_user=?, company=? WHERE branch_id=?");
                        ps.setString(1, location);
                        ps.setString(2, responsible != null ? responsible : "");
                        ps.setString(3, company);
                        ps.setInt(4, selectedBranch.getBranchId());
                        ps.executeUpdate();
                    } catch (SQLException e2) {
                        PreparedStatement ps = conn.prepareStatement(
                                "UPDATE branch SET location=?, responsible_user=? WHERE branch_id=?");
                        ps.setString(1, location);
                        ps.setString(2, responsible != null ? responsible : "");
                        ps.setInt(3, selectedBranch.getBranchId());
                        ps.executeUpdate();
                    }
                }

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Branch updated successfully.");
                successAlert.showAndWait();
            } else {
                // INSERT NEW BRANCH
                try {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO branch(location, responsible_user, company, phone, industry) VALUES(?, ?, ?, ?, ?)");
                    ps.setString(1, location);
                    ps.setString(2, responsible != null ? responsible : "");
                    ps.setString(3, company);
                    ps.setString(4, phone);
                    ps.setString(5, industry);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    // If columns don't exist, try without them
                    try {
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO branch(location, responsible_user, company) VALUES(?, ?, ?)");
                        ps.setString(1, location);
                        ps.setString(2, responsible != null ? responsible : "");
                        ps.setString(3, company);
                        ps.executeUpdate();
                    } catch (SQLException e2) {
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO branch(location, responsible_user) VALUES(?, ?)");
                        ps.setString(1, location);
                        ps.setString(2, responsible != null ? responsible : "");
                        ps.executeUpdate();
                    }
                }

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Branch created successfully.");
                successAlert.showAndWait();
            }

            loadBranches();
            handleAddCancel();
            handleSearch(); // Refresh filtered view

        } catch (SQLException e) {
            e.printStackTrace();
            addError.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewDetails() {
        // Toggle functionality
        if (detailsPane.isVisible()) {
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);
            return;
        }

        Branch b = branchTable.getSelectionModel().getSelectedItem();
        if (b == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a branch from the table to view details.");
            alert.showAndWait();
            return;
        }

        // Hide other forms
        if (addForm != null) {
            addForm.setVisible(false);
            addForm.setManaged(false);
        }
        addCompanyForm.setVisible(false);
        addCompanyForm.setManaged(false);
        deleteCompanyForm.setVisible(false);
        deleteCompanyForm.setManaged(false);

        detailsPane.setVisible(true);
        detailsPane.setManaged(true);

        detailsTitle.setText("Branch Details (Selected: " + b.getBranchId() + " - " + b.getLocation() + ")");

        detId.setText(String.valueOf(b.getBranchId()));
        detCompany.setText(b.getCompany() != null ? b.getCompany() : "");
        detIndustry.setText(b.getIndustry() != null ? b.getIndustry() : "");
        detLocation.setText(b.getLocation() != null ? b.getLocation() : "");
        detPhone.setText(b.getPhone() != null ? b.getPhone() : "");
        detResponsible.setText(b.getResponsibleUser() != null ? b.getResponsibleUser() : "");
    }

    @FXML
    private void handleEdit() {
        // Toggle functionality
        if (addForm != null && addForm.isVisible() && isEditMode) {
            handleAddCancel();
            return;
        }

        selectedBranch = branchTable.getSelectionModel().getSelectedItem();
        if (selectedBranch == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a branch from the table to edit.");
            alert.showAndWait();
            return;
        }

        // Hide other forms
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
        addCompanyForm.setVisible(false);
        addCompanyForm.setManaged(false);
        deleteCompanyForm.setVisible(false);
        deleteCompanyForm.setManaged(false);

        isEditMode = true;
        formTitle.setText("Edit Branch");
        formSaveButton.setText("Update");

        addLocation.setText(selectedBranch.getLocation());
        addPhone.setText(selectedBranch.getPhone() != null ? selectedBranch.getPhone() : "");
        
        // Set company value - ensure it's in the list first
        String companyValue = selectedBranch.getCompany();
        if (companyValue != null && !companyValue.isEmpty()) {
            if (!addCompanyCombo.getItems().contains(companyValue)) {
                addCompanyCombo.getItems().add(companyValue);
            }
            addCompanyCombo.getSelectionModel().select(companyValue);
            addCompanyCombo.setValue(companyValue);
        } else {
            addCompanyCombo.getSelectionModel().clearSelection();
            addCompanyCombo.setValue(null);
        }
        
        // Set responsible user value - ensure it's in the list first
        String userValue = selectedBranch.getResponsibleUser();
        if (userValue != null && !userValue.isEmpty()) {
            if (!addResponsibleCombo.getItems().contains(userValue)) {
                addResponsibleCombo.getItems().add(userValue);
            }
            addResponsibleCombo.getSelectionModel().select(userValue);
            addResponsibleCombo.setValue(userValue);
            // Auto-populate email
            handleUserSelection();
        } else {
            addResponsibleCombo.getSelectionModel().clearSelection();
            addResponsibleCombo.setValue(null);
            addUserEmail.clear();
        }
        
        addError.setText("");

        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    @FXML
    private void handleDelete() {
        Branch selected = branchTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a branch from the table to delete.");
            alert.showAndWait();
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Branch");
        confirmAlert.setContentText("Are you sure you want to delete branch '" + selected.getLocation() + "'?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM branch WHERE branch_id=?")) {
                ps.setInt(1, selected.getBranchId());
                ps.executeUpdate();
                loadBranches();
                handleSearch(); // Refresh filtered view
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Branch deleted successfully.");
                successAlert.showAndWait();
            } catch (SQLException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText(null);
                errorAlert.setContentText("Failed to delete branch: " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    @FXML
    private void handleExportExcel() {
        try {
            Stage stage = (Stage) branchTable.getScene().getWindow();
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Branches to Excel");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            fileChooser.setInitialFileName("branches_export.csv");
            
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    // Write header
                    writer.append("ID,Company,Industry,Location,Responsible User\n");
                    
                    // Write data from filtered list
                    for (Branch branch : filteredBranches) {
                        writer.append(String.valueOf(branch.getBranchId())).append(",");
                        writer.append(escapeCSV(branch.getCompany() != null ? branch.getCompany() : "")).append(",");
                        writer.append(escapeCSV(branch.getIndustry() != null ? branch.getIndustry() : "")).append(",");
                        writer.append(escapeCSV(branch.getLocation() != null ? branch.getLocation() : "")).append(",");
                        writer.append(escapeCSV(branch.getResponsibleUser() != null ? branch.getResponsibleUser() : "")).append("\n");
                    }
                    
                    writer.flush();
                    
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Export Successful");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Branches exported successfully to:\n" + file.getAbsolutePath());
                    successAlert.showAndWait();
                } catch (IOException e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Export Error");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Failed to export branches: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Export Error");
            errorAlert.setHeaderText(null);
            errorAlert.setContentText("Failed to export branches: " + e.getMessage());
            errorAlert.showAndWait();
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
}
