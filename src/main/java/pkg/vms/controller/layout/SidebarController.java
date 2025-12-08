package pkg.vms.controller.layout;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class SidebarController implements Initializable {

    @FXML
    private VBox root;

    @FXML private Button requestsButton;
    @FXML private Button clientsButton;
    @FXML private Button vouchersButton;
    @FXML private Button branchesButton;
    @FXML private Button usersButton;
    @FXML private Button reportsButton;

    /**
     * Callback used by the main controller (DashboardController)
     * so it can load the matching view when a sidebar item is clicked.
     */
    private Consumer<String> navigationHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // No default highlight since Dashboard is now just a label
    }

    // Sidebar button handlers =========================

    @FXML
    private void onRequests(ActionEvent e) {
        navigate("requests");
    }

    @FXML
    private void onClients(ActionEvent e) {
        navigate("clients");
    }

    @FXML
    private void onVouchers(ActionEvent e) {
        navigate("vouchers");
    }

    @FXML
    private void onBranches(ActionEvent e) {
        navigate("branches");
    }

    @FXML
    private void onUsers(ActionEvent e) {
        navigate("users");
    }

    @FXML
    private void onReports(ActionEvent e) {
        navigate("reports");
    }

    // ================================================

    /**
     * Allows DashboardController to receive navigation events.
     */
    public void setNavigationHandler(Consumer<String> handler) {
        this.navigationHandler = handler;
    }

    /**
     * Called internally when a sidebar button is pressed.
     */
    private void navigate(String target) {
        // Check if the button is disabled before navigating
        Button targetButton = getButtonForTarget(target);
        if (targetButton != null && targetButton.isDisable()) {
            return; // Don't navigate if button is disabled
        }
        
        if (navigationHandler != null) {
            navigationHandler.accept(target);   // send event to DashboardController
        } else {
            System.out.println("[Sidebar] navigate: " + target);
        }
        setActive(target);
    }

    /**
     * Gets the button for a given target name
     */
    private Button getButtonForTarget(String target) {
        switch (target) {
            case "dashboard": return null; // Dashboard is now a label, not a button
            case "requests": return requestsButton;
            case "clients": return clientsButton;
            case "vouchers": return vouchersButton;
            case "branches": return branchesButton;
            case "users": return usersButton;
            case "reports": return reportsButton;
            default: return null;
        }
    }

    /**
     * Highlights whichever button is active.
     */
    public void setActive(String name) {

        // remove from all
        requestsButton.getStyleClass().remove("active");
        clientsButton.getStyleClass().remove("active");
        vouchersButton.getStyleClass().remove("active");
        branchesButton.getStyleClass().remove("active");
        usersButton.getStyleClass().remove("active");
        reportsButton.getStyleClass().remove("active");

        // add to one (dashboard is now a label, so skip it)
        switch (name) {
            case "dashboard": break; // Dashboard is a label, not a button
            case "requests":  requestsButton.getStyleClass().add("active"); break;
            case "clients":   clientsButton.getStyleClass().add("active"); break;
            case "vouchers":  vouchersButton.getStyleClass().add("active"); break;
            case "branches":  branchesButton.getStyleClass().add("active"); break;
            case "users":     usersButton.getStyleClass().add("active"); break;
            case "reports":   reportsButton.getStyleClass().add("active"); break;
        }
    }

    public VBox getRoot() {
        return root;
    }

    /**
     * Configures access control for sidebar buttons based on user role
     * Superuser - access all buttons (all enabled)
     * Admin & Accountant - only Requests and Clients enabled (others disabled/greyed)
     * Approver - only Vouchers and Reports enabled (others disabled/greyed)
     */
    public void configureRoleBasedAccess(String role) {
        if (role == null) {
            return;
        }

        String roleLower = role.toLowerCase().trim();

        if (roleLower.equals("superuser")) {
            // Superuser - enable all
            setButtonEnabled(requestsButton, true);
            setButtonEnabled(clientsButton, true);
            setButtonEnabled(vouchersButton, true);
            setButtonEnabled(branchesButton, true);
            setButtonEnabled(usersButton, true);
            setButtonEnabled(reportsButton, true);
        } else if (roleLower.equals("admin") || roleLower.equals("accountant")) {
            // Admin & Accountant - only Requests and Clients enabled
            setButtonEnabled(requestsButton, true);
            setButtonEnabled(clientsButton, true);
            setButtonEnabled(vouchersButton, false);
            setButtonEnabled(branchesButton, false);
            setButtonEnabled(usersButton, false);
            setButtonEnabled(reportsButton, false);
        } else if (roleLower.equals("approver")) {
            // Approver - only Vouchers and Reports enabled
            setButtonEnabled(requestsButton, false);
            setButtonEnabled(clientsButton, false);
            setButtonEnabled(vouchersButton, true);
            setButtonEnabled(branchesButton, false);
            setButtonEnabled(usersButton, false);
            setButtonEnabled(reportsButton, true);
        } else {
            // Unknown role - disable all
            setButtonEnabled(requestsButton, false);
            setButtonEnabled(clientsButton, false);
            setButtonEnabled(vouchersButton, false);
            setButtonEnabled(branchesButton, false);
            setButtonEnabled(usersButton, false);
            setButtonEnabled(reportsButton, false);
        }
    }

    /**
     * Enables or disables a sidebar button with appropriate styling
     */
    private void setButtonEnabled(Button button, boolean enabled) {
        button.setVisible(true);
        button.setManaged(true);
        button.setDisable(!enabled);
        
        if (enabled) {
            button.getStyleClass().remove("disabled-sidebar-button");
            // Remove lock icon from text if present
            String text = button.getText();
            if (text.contains("🔒")) {
                button.setText(text.replace(" 🔒", ""));
            }
        } else {
            button.getStyleClass().add("disabled-sidebar-button");
            // Add lock icon to text if not present
            String text = button.getText();
            if (!text.contains("🔒")) {
                button.setText(text + " 🔒");
            }
        }
    }
}
