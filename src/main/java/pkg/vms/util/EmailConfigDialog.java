package pkg.vms.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * Dialog for configuring email settings
 */
public class EmailConfigDialog {
    
    public static void showConfigDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Email Configuration");
        dialog.setHeaderText("Configure SMTP Settings for Email Notifications");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField hostField = new TextField(EmailConfig.getSmtpHost());
        hostField.setPromptText("smtp.gmail.com");
        TextField portField = new TextField(EmailConfig.getSmtpPort());
        portField.setPromptText("587");
        TextField userField = new TextField(EmailConfig.getSmtpUser());
        userField.setPromptText("your-email@gmail.com");
        PasswordField passwordField = new PasswordField();
        passwordField.setText(EmailConfig.getSmtpPassword());
        passwordField.setPromptText("Your password or App Password");
        
        grid.add(new Label("SMTP Host:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("SMTP Port:"), 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("Email (User):"), 0, 2);
        grid.add(userField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        
        Label infoLabel = new Label("For Gmail: Use App Password instead of regular password.\n" +
            "Enable 2-Step Verification and generate App Password at:\n" +
            "https://myaccount.google.com/apppasswords");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        grid.add(infoLabel, 0, 4, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType testButtonType = new ButtonType("Test Connection", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, testButtonType, ButtonType.CANCEL);
        
        Button testButton = (Button) dialog.getDialogPane().lookupButton(testButtonType);
        if (testButton != null) {
            testButton.setOnAction(e -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String user = userField.getText().trim();
            String password = passwordField.getText();
            
            if (user.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please enter email and password to test.");
                return;
            }
            
            // Temporarily set config for testing
            EmailConfig.setSmtpHost(host);
            EmailConfig.setSmtpPort(port);
            EmailConfig.setSmtpUser(user);
            EmailConfig.setSmtpPassword(password);
            
            // Test email
            boolean success = EmailService.sendEmail(user, null, "Test Email from VMS", 
                "This is a test email. If you receive this, your email configuration is working correctly!", null);
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "Test email sent successfully! Check your inbox.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Test Failed", 
                    "Could not send test email. Please check your settings.");
            }
            });
        }
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String host = hostField.getText().trim();
                String port = portField.getText().trim();
                String user = userField.getText().trim();
                String password = passwordField.getText();
                
                if (host.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "SMTP Host is required.");
                    return null;
                }
                if (port.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "SMTP Port is required.");
                    return null;
                }
                if (user.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Email address is required.");
                    return null;
                }
                if (password.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Password is required.");
                    return null;
                }
                
                EmailConfig.setSmtpHost(host);
                EmailConfig.setSmtpPort(port);
                EmailConfig.setSmtpUser(user);
                EmailConfig.setSmtpPassword(password);
                
                showAlert(Alert.AlertType.INFORMATION, "Saved", 
                    "Email configuration saved successfully!");
            }
            return null;
        });
        
        dialog.showAndWait();
    }
    
    private static void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

