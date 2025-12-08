package pkg.vms.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import pkg.vms.DBconnection.DBconnection;
import pkg.vms.model.Users;
import java.sql.*;

public class UsersController {

    // TABLE & COLUMNS
    @FXML private TableView<Users> usersTable;
    @FXML private TableColumn<Users, String> usernameColumn;
    @FXML private TableColumn<Users, String> firstNameColumn;
    @FXML private TableColumn<Users, String> lastNameColumn;
    @FXML private TableColumn<Users, String> emailColumn;
    @FXML private TableColumn<Users, String> roleColumn;
    @FXML private TableColumn<Users, String> statusColumn;
    @FXML private TableColumn<Users, String> titreColumn;

    // ADD/EDIT FORM
    @FXML private VBox addForm;
    @FXML private Label formTitle;
    @FXML private TextField addUsername, addFirstName, addLastName, addEmail, addDdl, addTitre;
    @FXML private PasswordField addPassword;
    @FXML private ComboBox<String> addRoleCombo;
    @FXML private ComboBox<String> addStatusCombo;
    @FXML private Label formError;
    @FXML private TextField searchHeaderField;
    @FXML private Button deleteButton;
    @FXML private Button closeFormButton;
    @FXML private Button addUserButton;

    private ObservableList<Users> userList = FXCollections.observableArrayList();
    private Users editingUser = null;

    @FXML
    public void initialize() {
        // SETUP TABLE COLUMNS
        usernameColumn.setCellValueFactory(cell -> cell.getValue().usernameProperty());
        firstNameColumn.setCellValueFactory(cell -> cell.getValue().firstNameProperty());
        lastNameColumn.setCellValueFactory(cell -> cell.getValue().lastNameProperty());
        emailColumn.setCellValueFactory(cell -> cell.getValue().emailProperty());
        roleColumn.setCellValueFactory(cell -> cell.getValue().roleProperty());
        statusColumn.setCellValueFactory(cell -> cell.getValue().statusProperty());
        titreColumn.setCellValueFactory(cell -> cell.getValue().titreProperty());
        usersTable.setItems(userList);
        loadUsers();

        // SETUP COMBO BOXES
        if (addRoleCombo != null) {
            addRoleCombo.getItems().addAll("Superuser", "Admin", "Accountant", "Approver");
        }
        if (addStatusCombo != null) {
            addStatusCombo.getItems().addAll("Active", "Inactive", "Suspended");
        }

        // HIDE FORM INITIALLY
        addForm.setVisible(false);
        addForm.setManaged(false);
        
        // Configure delete button visibility based on role
        configureDeleteButtonAccess();
        
        // Configure add user button - only superuser can create accounts
        configureAddUserButtonAccess();
        
        // Add Enter key support for search field
        if (searchHeaderField != null) {
            searchHeaderField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleSearch();
                }
            });
        }
    }
    
    private void configureAddUserButtonAccess() {
        // Only superuser can create new accounts
        if (addUserButton != null) {
            String currentRole = UserSession.getInstance().getRole();
            if (currentRole != null && currentRole.toLowerCase().trim().equals("superuser")) {
                addUserButton.setVisible(true);
                addUserButton.setManaged(true);
            } else {
                addUserButton.setVisible(false);
                addUserButton.setManaged(false);
            }
        }
    }
    
    private void configureDeleteButtonAccess() {
        // Only superuser can delete users
        if (deleteButton != null) {
            String currentRole = UserSession.getInstance().getRole();
            if (currentRole != null && currentRole.toLowerCase().trim().equals("superuser")) {
                deleteButton.setVisible(true);
                deleteButton.setManaged(true);
            } else {
                deleteButton.setVisible(false);
                deleteButton.setManaged(false);
            }
        }
    }

    private void loadUsers() {
        userList.clear();
        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                Users user = new Users(
                        rs.getString("username"),
                        rs.getString("first_name_user"),
                        rs.getString("last_name_user"),
                        rs.getString("email_user"),
                        rs.getString("role"),
                        rs.getString("password"),
                        rs.getString("ddl"),
                        rs.getString("titre"),
                        rs.getString("status")
                );
                userList.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading users: " + e.getMessage());
        }
    }

    /** SEARCH USERS **/
    @FXML
    private void handleSearch() {
        String searchText = searchHeaderField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            usersTable.setItems(userList);
            return;
        }
        ObservableList<Users> filteredList = FXCollections.observableArrayList();
        for (Users user : userList) {
            if (user.getUsername().toLowerCase().contains(searchText)
                    || user.getFirstName().toLowerCase().contains(searchText)
                    || user.getLastName().toLowerCase().contains(searchText)
                    || user.getEmail().toLowerCase().contains(searchText)
                    || user.getRole().toLowerCase().contains(searchText)
                    || user.getStatus().toLowerCase().contains(searchText)) {
                filteredList.add(user);
            }
        }
        usersTable.setItems(filteredList);
    }

    /** SHOW ADD FORM **/
    @FXML
    private void handleAddUser() {
        // Check if current user is superuser
        String currentRole = UserSession.getInstance().getRole();
        if (currentRole == null || !currentRole.toLowerCase().trim().equals("superuser")) {
            showError("Only Superuser can create new user accounts.");
            return;
        }
        
        editingUser = null;
        formTitle.setText("Add New User");
        clearFormFields();
        formError.setText("");
        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    /** SHOW EDIT FORM **/
    @FXML
    private void handleEditUser() {
        Users selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a user to edit.");
            return;
        }
        editingUser = selected;
        formTitle.setText("Edit User");
        addUsername.setText(selected.getUsername());
        addFirstName.setText(selected.getFirstName());
        addLastName.setText(selected.getLastName());
        addEmail.setText(selected.getEmail());
        
        // Set combo box values
        if (addRoleCombo != null) {
            addRoleCombo.setValue(selected.getRole());
        }
        if (addStatusCombo != null) {
            addStatusCombo.setValue(selected.getStatus());
        }
        
        // Password field - don't show existing password for security
        if (addPassword != null) {
            addPassword.clear();
        }
        
        addDdl.setText(selected.getDdl());
        addTitre.setText(selected.getTitre());
        formError.setText("");
        addForm.setVisible(true);
        addForm.setManaged(true);
    }

    /** DELETE USER **/
    @FXML
    private void handleDeleteUser() {
        Users selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a user to delete.");
            return;
        }
        
        // Check if current user is superuser
        String currentRole = UserSession.getInstance().getRole();
        if (currentRole == null || !currentRole.toLowerCase().trim().equals("superuser")) {
            showError("Only Superuser can delete users.");
            return;
        }
        
        // Check if the user to be deleted has a role that can be deleted (accountant, admin, approver)
        String userRole = selected.getRole();
        if (userRole == null) {
            showError("Cannot delete user: role is not defined.");
            return;
        }
        
        String userRoleLower = userRole.toLowerCase().trim();
        if (!userRoleLower.equals("accountant") && 
            !userRoleLower.equals("admin") && 
            !userRoleLower.equals("approver")) {
            showError("Only users with roles: Accountant, Admin, or Approver can be deleted.");
            return;
        }
        
        // Confirm deletion
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete User");
        confirmAlert.setContentText("Are you sure you want to delete user '" + selected.getUsername() + "' with role '" + selected.getRole() + "'?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE username=?")) {
                ps.setString(1, selected.getUsername());
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("User deleted successfully.");
                    successAlert.showAndWait();
                    loadUsers();
                } else {
                    showError("Failed to delete user.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showError("Error deleting user: " + e.getMessage());
            }
        }
    }

    /** SAVE FORM **/
    @FXML
    private void handleFormSave() {
        // Check if current user is superuser (for creating new accounts)
        if (editingUser == null) {
            String currentRole = UserSession.getInstance().getRole();
            if (currentRole == null || !currentRole.toLowerCase().trim().equals("superuser")) {
                formError.setText("Only Superuser can create new user accounts.");
                return;
            }
        }
        
        String username = addUsername.getText().trim();
        String firstName = addFirstName.getText().trim();
        String lastName = addLastName.getText().trim();
        String email = addEmail.getText().trim();
        String role = addRoleCombo != null && addRoleCombo.getValue() != null ? 
                     addRoleCombo.getValue() : "";
        String status = addStatusCombo != null && addStatusCombo.getValue() != null ? 
                       addStatusCombo.getValue() : "";
        String password = addPassword != null ? addPassword.getText().trim() : "";
        String ddl = addDdl.getText().trim();
        String titre = addTitre.getText().trim();

        if (username.isEmpty()) {
            formError.setText("Username cannot be empty.");
            return;
        }
        
        // Validate email format - must contain @ symbol
        if (email.isEmpty()) {
            formError.setText("Email cannot be empty.");
            return;
        }
        
        if (!email.contains("@")) {
            formError.setText("Email must contain '@' symbol. Please enter a valid email address.");
            return;
        }
        
        if (role.isEmpty()) {
            formError.setText("Please select a role.");
            return;
        }
        
        if (status.isEmpty()) {
            formError.setText("Please select a status.");
            return;
        }
        
        // For new users, password is required
        if (editingUser == null && password.isEmpty()) {
            formError.setText("Password is required for new users.");
            return;
        }

        try (Connection conn = DBconnection.getConnection()) {
            if (editingUser == null) {
                // INSERT NEW USER
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username, first_name_user, last_name_user, email_user, role, password, ddl, titre, status) VALUES(?,?,?,?,?,?,?,?,?)");
                ps.setString(1, username);
                ps.setString(2, firstName);
                ps.setString(3, lastName);
                ps.setString(4, email);
                ps.setString(5, role);
                ps.setString(6, password);
                ps.setString(7, ddl);
                ps.setString(8, titre);
                ps.setString(9, status);
                ps.executeUpdate();
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("User created successfully.");
                successAlert.showAndWait();
            } else {
                // UPDATE EXISTING USER
                // Only update password if it's not empty
                if (password.isEmpty()) {
                    // Don't update password
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET username=?, first_name_user=?, last_name_user=?, email_user=?, role=?, ddl=?, titre=?, status=? WHERE username=?");
                    ps.setString(1, username);
                    ps.setString(2, firstName);
                    ps.setString(3, lastName);
                    ps.setString(4, email);
                    ps.setString(5, role);
                    ps.setString(6, ddl);
                    ps.setString(7, titre);
                    ps.setString(8, status);
                    ps.setString(9, editingUser.getUsername());
                    ps.executeUpdate();
                } else {
                    // Update password too
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET username=?, first_name_user=?, last_name_user=?, email_user=?, role=?, password=?, ddl=?, titre=?, status=? WHERE username=?");
                    ps.setString(1, username);
                    ps.setString(2, firstName);
                    ps.setString(3, lastName);
                    ps.setString(4, email);
                    ps.setString(5, role);
                    ps.setString(6, password);
                    ps.setString(7, ddl);
                    ps.setString(8, titre);
                    ps.setString(9, status);
                    ps.setString(10, editingUser.getUsername());
                    ps.executeUpdate();
                }
                
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText(null);
                successAlert.setContentText("User updated successfully.");
                successAlert.showAndWait();
            }
            loadUsers();
            handleAddCancel();
        } catch (SQLException e) {
            e.printStackTrace();
            formError.setText("Database error: " + e.getMessage());
        }
    }

    /** CANCEL ADD/EDIT **/
    @FXML
    private void handleAddCancel() {
        addForm.setVisible(false);
        addForm.setManaged(false);
    }

    private void clearFormFields() {
        addUsername.setText("");
        addFirstName.setText("");
        addLastName.setText("");
        addEmail.setText("");
        if (addRoleCombo != null) {
            addRoleCombo.setValue(null);
        }
        if (addStatusCombo != null) {
            addStatusCombo.setValue(null);
        }
        if (addPassword != null) {
            addPassword.clear();
        }
        addDdl.setText("");
        addTitre.setText("");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}