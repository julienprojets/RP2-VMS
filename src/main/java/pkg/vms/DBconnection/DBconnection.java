package pkg.vms.DBconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnection {

    private static final String URL = "jdbc:postgresql://postgresql-julienjeanpierre.alwaysdata.net:5432/julienjeanpierre_vms_db";
    private static final String USER = "julienjeanpierre";
    private static final String PASS = "OR2Uw33S'p8`alHd";
    private static Connection conn;

    // Open a connection
    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(URL, USER, PASS);
                System.out.println("Connected to PostgreSQL database.");
            }
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
        return conn;
    }

}
