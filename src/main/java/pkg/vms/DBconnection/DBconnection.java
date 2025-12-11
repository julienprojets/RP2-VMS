package pkg.vms.DBconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection {

    private static final String URL = "jdbc:postgresql://postgresql-julienjeanpierre.alwaysdata.net:5432/julienjeanpierre_vms_db";
    private static final String USER = "julienjeanpierre";
    private static final String PASS = "OR2Uw33S'p8`alHd";
    private static Connection conn;
    
    // Thread-local connection for multi-threaded environments (like HTTP server)
    private static final ThreadLocal<Connection> threadLocalConn = new ThreadLocal<>();

    // Open a connection - always returns a fresh, valid connection
    // For HTTP server threads, use thread-local connections to avoid conflicts
    public static Connection getConnection() {
        // Check if we're in a thread that needs isolation (like HTTP server)
        // For now, always create a fresh connection to avoid sharing issues
        Connection localConn = null;
        try {
            // Always create a completely new connection
            localConn = DriverManager.getConnection(URL, USER, PASS);
            
            // Verify it's actually valid
            if (!localConn.isValid(2)) {
                localConn.close();
                throw new SQLException("New connection is not valid");
            }
            
            // Set auto-commit to true by default (can be changed by caller)
            localConn.setAutoCommit(true);
            
            System.out.println("Connected to PostgreSQL database (thread: " + Thread.currentThread().getName() + ").");
            return localConn;
            
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            if (localConn != null) {
                try {
                    localConn.close();
                } catch (SQLException e2) {
                    // Ignore
                }
            }
            return null;
        }
    }

}
