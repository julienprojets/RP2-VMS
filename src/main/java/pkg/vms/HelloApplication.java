package pkg.vms;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Declare the FXMLLoader and load the FXML resource
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pkg/vms/fxml/vouchers.fxml"));

        // Load the FXML and get the root node
        Parent root = loader.load();

        // Create a new scene with the loaded root
        Scene scene = new Scene(root);

        // Set the scene for the stage
        stage.setScene(scene);

        // Set the stage properties
        stage.setTitle("VMS - Voucher Management System");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
