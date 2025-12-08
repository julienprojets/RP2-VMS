package pkg.vms.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import pkg.vms.DBconnection.DBconnection;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button cancelButton;

    @FXML
    private void initialize() {
        loginButton.setOnAction(e -> handleLogin());
        cancelButton.setOnAction(e -> handleCancel());
        
        // Allow Enter key to trigger login
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        
        // Request focus on username field when window opens
        usernameField.requestFocus();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Login Failed");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please enter both username and password.");
            alert.showAndWait();
            if (username.isEmpty()) {
                usernameField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
            return;
        }
        
        // Disable login button during authentication
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        try {
            Connection conn = DBconnection.getConnection();

            String checkUserQuery = "SELECT password, role FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkUserQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                if (storedPassword.equals(password)) {
                    // ========== LOGIN SUCCESS → LOAD DASHBOARD ==========
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/pkg/vms/fxml/Dashboard.fxml")
                        );
                        Parent root = loader.load();

                        // Store user session
                        String userRole = rs.getString("role");
                        UserSession.getInstance().setUser(username, userRole);
                        
                        // Pass username & role to Dashboard
                        DashboardController controller = loader.getController();
                        controller.setUserInfo(username, userRole);

                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.show();

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        showError("Navigation Error", "Failed to load dashboard. Please try again.");
                    }

                } else {
                    showError("Invalid Credentials", "Incorrect password. Please try again.");
                    passwordField.clear();
                    passwordField.requestFocus();
                }

            } else {
                showError("User Not Found", "No user with username '" + username + "' was found.");
                usernameField.requestFocus();
            }

            rs.close();
            checkStmt.close();

        } catch (SQLException e) {
            showError("Database Error", "Unable to connect to database. Please check your connection.");
            e.printStackTrace();
        } finally {
            // Re-enable login button
            loginButton.setDisable(false);
            loginButton.setText("Sign In");
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleCancel() {
        usernameField.clear();
        passwordField.clear();
    }
}
