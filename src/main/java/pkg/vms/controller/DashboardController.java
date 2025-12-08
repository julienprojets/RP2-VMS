package pkg.vms.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pkg.vms.controller.layout.SidebarController;

import java.io.IOException;

public class DashboardController {

    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Button logoutButton;

    @FXML private ImageView iconRequests;
    @FXML private ImageView iconClients;
    @FXML private ImageView iconVouchers;
    @FXML private ImageView iconBranches;
    @FXML private ImageView iconUsers;
    @FXML private ImageView iconReports;

    @FXML private VBox vboxRequests;
    @FXML private VBox vboxClients;
    @FXML private VBox vboxVouchers;
    @FXML private VBox vboxBranches;
    @FXML private VBox vboxUsers;
    @FXML private VBox vboxReports;

    @FXML private BorderPane rootPane;  // The main BorderPane from Dashboard.fxml

    private String currentUserRole;
    private SidebarController sidebarController;

    @FXML
    public void initialize() {

        // Load Sidebar and connect navigation system
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/pkg/vms/fxml/layout/Sidebar.fxml")
            );

            Parent sidebar = loader.load();
            sidebarController = loader.getController();

            // Connect sidebar navigation to dashboard navigation
            sidebarController.setNavigationHandler(this::navigateTo);

            // Insert sidebar inside the left of the dashboard
            rootPane.setLeft(sidebar);

            // Apply role-based access control from UserSession if available
            String role = UserSession.getInstance().getRole();
            if (role != null) {
                currentUserRole = role;
                String username = UserSession.getInstance().getUsername();
                if (username != null && usernameLabel != null) {
                    usernameLabel.setText("Logged in as: " + username);
                }
                if (roleLabel != null) {
                    roleLabel.setText("Role: " + role);
                }
                configureRoleBasedAccess(role);
            } else if (currentUserRole != null) {
                // Fallback to currentUserRole if UserSession not set yet
                configureRoleBasedAccess(currentUserRole);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CLICK HANDLERS FOR DASHBOARD ICONS
    @FXML
    private void handleRequestsClick() {
        if (!iconRequests.isDisable()) {
            navigateTo("requests");
        }
    }

    @FXML
    private void handleClientsClick() {
        if (!iconClients.isDisable()) {
            navigateTo("clients");
        }
    }

    @FXML
    private void handleVouchersClick() {
        if (!iconVouchers.isDisable()) {
            navigateTo("vouchers");
        }
    }

    @FXML
    private void handleBranchesClick() {
        if (!iconBranches.isDisable()) {
            navigateTo("branches");
        }
    }

    @FXML
    private void handleUsersClick() {
        if (!iconUsers.isDisable()) {
            navigateTo("users");
        }
    }

    @FXML
    private void handleReportsClick() {
        if (!iconReports.isDisable()) {
            navigateTo("reports");
        }
    }

    /**
     * Central navigation for dashboard (sidebar + dashboard icons)
     */
    private void navigateTo(String target) {
        try {
            Parent view = null;

            switch (target) {

                case "users":
                    view = FXMLLoader.load(
                            getClass().getResource("/pkg/vms/fxml/users.fxml")
                    );
                    break;

                case "clients":
                    view = FXMLLoader.load(
                            getClass().getResource("/pkg/vms/fxml/clients.fxml")
                    );
                    break;

                case "vouchers":
                    view = FXMLLoader.load(
                            getClass().getResource("/pkg/vms/fxml/vouchers.fxml")
                    );
                    break;

                case "requests":
                    view = FXMLLoader.load(
                            getClass().getResource("/pkg/vms/fxml/requests.fxml")
                    );
                    break;

                case "branches":
                    view = FXMLLoader.load(
                            getClass().getResource("/pkg/vms/fxml/branches.fxml")
                    );
                    break;

                case "reports":
                    // TODO: Load reports view when available
                    System.out.println("Reports view not yet implemented");
                    break;

                case "dashboard":
                    view = FXMLLoader.load(
                            getClass().getResource("/pkg/vms/fxml/Dashboard.fxml")
                    );
                    break;
            }

            if (view != null) {
                rootPane.setCenter(view);   // Swap the center content
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUserInfo(String username, String role) {
        usernameLabel.setText("Logged in as: " + username);
        roleLabel.setText("Role: " + role);
        currentUserRole = role;
        
        // Configure role-based access control
        configureRoleBasedAccess(role);
    }

    /**
     * Configures access control for dashboard buttons and sidebar buttons based on user role
     * Superuser - access all buttons (all enabled)
     * Admin & Accountant - only Requests and Clients enabled (others disabled/greyed)
     * Approver - only Vouchers and Reports enabled (others disabled/greyed)
     */
    private void configureRoleBasedAccess(String role) {
        if (role == null) {
            return;
        }

        String roleLower = role.toLowerCase().trim();

        // Configure dashboard icons - show all but disable based on role
        if (roleLower.equals("superuser")) {
            // Superuser - enable all
            setDashboardButtonEnabled(vboxRequests, iconRequests, true);
            setDashboardButtonEnabled(vboxClients, iconClients, true);
            setDashboardButtonEnabled(vboxVouchers, iconVouchers, true);
            setDashboardButtonEnabled(vboxBranches, iconBranches, true);
            setDashboardButtonEnabled(vboxUsers, iconUsers, true);
            setDashboardButtonEnabled(vboxReports, iconReports, true);
        } else if (roleLower.equals("admin") || roleLower.equals("accountant")) {
            // Admin & Accountant - only Requests and Clients enabled
            setDashboardButtonEnabled(vboxRequests, iconRequests, true);
            setDashboardButtonEnabled(vboxClients, iconClients, true);
            setDashboardButtonEnabled(vboxVouchers, iconVouchers, false);
            setDashboardButtonEnabled(vboxBranches, iconBranches, false);
            setDashboardButtonEnabled(vboxUsers, iconUsers, false);
            setDashboardButtonEnabled(vboxReports, iconReports, false);
        } else if (roleLower.equals("approver")) {
            // Approver - only Vouchers and Reports enabled
            setDashboardButtonEnabled(vboxRequests, iconRequests, false);
            setDashboardButtonEnabled(vboxClients, iconClients, false);
            setDashboardButtonEnabled(vboxVouchers, iconVouchers, true);
            setDashboardButtonEnabled(vboxBranches, iconBranches, false);
            setDashboardButtonEnabled(vboxUsers, iconUsers, false);
            setDashboardButtonEnabled(vboxReports, iconReports, true);
        } else {
            // Unknown role - disable all
            setDashboardButtonEnabled(vboxRequests, iconRequests, false);
            setDashboardButtonEnabled(vboxClients, iconClients, false);
            setDashboardButtonEnabled(vboxVouchers, iconVouchers, false);
            setDashboardButtonEnabled(vboxBranches, iconBranches, false);
            setDashboardButtonEnabled(vboxUsers, iconUsers, false);
            setDashboardButtonEnabled(vboxReports, iconReports, false);
        }

        // Configure sidebar buttons
        if (sidebarController != null) {
            sidebarController.configureRoleBasedAccess(role);
        }
    }

    /**
     * Enables or disables a dashboard button with appropriate styling
     */
    private void setDashboardButtonEnabled(VBox vbox, ImageView icon, boolean enabled) {
        vbox.setVisible(true);
        vbox.setManaged(true);
        
        // Find and manage lock icon label
        Label lockLabel = null;
        for (javafx.scene.Node node : vbox.getChildren()) {
            if (node instanceof Label && node.getStyleClass().contains("lock-icon")) {
                lockLabel = (Label) node;
                break;
            }
        }
        
        if (enabled) {
            // Enable button
            icon.setDisable(false);
            icon.setOpacity(1.0);
            icon.setStyle("-fx-cursor: hand;");
            vbox.getStyleClass().remove("disabled-button");
            // Remove lock icon if present
            if (lockLabel != null) {
                vbox.getChildren().remove(lockLabel);
            }
        } else {
            // Disable button
            icon.setDisable(true);
            icon.setOpacity(0.4);
            icon.setStyle("-fx-cursor: default;");
            vbox.getStyleClass().add("disabled-button");
            // Add lock icon if not already present
            if (lockLabel == null) {
                lockLabel = new Label("🔒");
                lockLabel.getStyleClass().add("lock-icon");
                lockLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px;");
                vbox.getChildren().add(lockLabel);
            }
        }
    }

    /**
     * Handles logout action - clears session and returns to login page
     */
    @FXML
    private void handleLogout() {
        try {
            // Clear user session
            UserSession.getInstance().clear();
            
            // Load login page
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/pkg/vms/fxml/loginpage.fxml")
            );
            Parent loginRoot = loader.load();
            
            // Get current stage and switch to login scene
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
