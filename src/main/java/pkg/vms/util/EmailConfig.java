package pkg.vms.util;

import java.io.*;
import java.util.Properties;

/**
 * Email configuration manager
 * Stores SMTP settings in a properties file
 */
public class EmailConfig {
    
    private static final String CONFIG_FILE = "email_config.properties";
    private static Properties props = new Properties();
    
    static {
        loadConfig();
    }
    
    /**
     * Load configuration from file
     */
    private static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading email config: " + e.getMessage());
            }
        } else {
            // Set default Gmail settings
            props.setProperty("smtp.host", "smtp.gmail.com");
            props.setProperty("smtp.port", "587");
            props.setProperty("smtp.user", "");
            props.setProperty("smtp.password", "");
            saveConfig();
        }
    }
    
    /**
     * Save configuration to file
     */
    public static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Email Configuration");
        } catch (IOException e) {
            System.err.println("Error saving email config: " + e.getMessage());
        }
    }
    
    /**
     * Get SMTP host
     */
    public static String getSmtpHost() {
        return props.getProperty("smtp.host", "smtp.gmail.com");
    }
    
    /**
     * Get SMTP port
     */
    public static String getSmtpPort() {
        return props.getProperty("smtp.port", "587");
    }
    
    /**
     * Get SMTP user (email)
     */
    public static String getSmtpUser() {
        String user = props.getProperty("smtp.user", "");
        // Also check environment variable as fallback
        if (user.isEmpty()) {
            user = System.getenv("SMTP_USER");
        }
        return user != null ? user : "";
    }
    
    /**
     * Get SMTP password
     */
    public static String getSmtpPassword() {
        String password = props.getProperty("smtp.password", "");
        // Also check environment variable as fallback
        if (password.isEmpty()) {
            password = System.getenv("SMTP_PASSWORD");
        }
        return password != null ? password : "";
    }
    
    /**
     * Set SMTP user
     */
    public static void setSmtpUser(String user) {
        props.setProperty("smtp.user", user);
        saveConfig();
    }
    
    /**
     * Set SMTP password
     */
    public static void setSmtpPassword(String password) {
        props.setProperty("smtp.password", password);
        saveConfig();
    }
    
    /**
     * Set SMTP host
     */
    public static void setSmtpHost(String host) {
        props.setProperty("smtp.host", host);
        saveConfig();
    }
    
    /**
     * Set SMTP port
     */
    public static void setSmtpPort(String port) {
        props.setProperty("smtp.port", port);
        saveConfig();
    }
    
    /**
     * Check if email is configured
     */
    public static boolean isConfigured() {
        String user = getSmtpUser();
        String password = getSmtpPassword();
        return user != null && !user.isEmpty() && password != null && !password.isEmpty();
    }
}

