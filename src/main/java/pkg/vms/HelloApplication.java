package pkg.vms;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class HelloApplication extends Application {

    static {
        bootLog("HelloApplication class loaded " + Instant.now());
    }

    private static void bootLog(String line) {
        String localApp = System.getenv("LOCALAPPDATA");
        if (localApp == null) {
            localApp = System.getProperty("user.home");
        }
        Path[] paths = new Path[] {
                Path.of(System.getProperty("user.home"), "vms-packaged-startup.log"),
                Path.of(localApp, "vms-debug.log"),
                Path.of(System.getProperty("user.dir"), "vms-debug-here.log")
        };
        for (Path p : paths) {
            try {
                Files.writeString(p, line + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ignored) {
                // try next path
            }
        }
    }

    private static Path startupLogPath() {
        return Path.of(System.getProperty("user.home"), "vms-packaged-startup.log");
    }

    private static void appendLog(String line) {
        bootLog(line);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Declare the FXMLLoader and load the FXML resource
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pkg/vms/fxml/loginpage.fxml"));

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
        bootLog("main() entered " + Instant.now());
        appendLog("main() " + Instant.now());
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            appendLog("Uncaught on thread " + t.getName() + ":" + System.lineSeparator() + sw);
        });
        try {
            launch(args);
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            appendLog("launch() failed:" + System.lineSeparator() + sw);
            throw t;
        }
    }
}
