package pkg.vms.util;

import pkg.vms.DBconnection.DBconnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class to manage database schema updates
 */
public class DatabaseSchema {
    
    private static volatile boolean schemaChecked = false;
    private static final Object schemaCheckLock = new Object();
    
    public static void ensureSchemaExists() {
        // Only check schema once per application run to avoid repeated database queries
        if (schemaChecked) {
            return;
        }
        
        synchronized (schemaCheckLock) {
            // Double-check pattern
            if (schemaChecked) {
                return;
            }
            Connection conn = null;
            try {
                conn = DBconnection.getConnection();
                if (conn != null && !conn.isClosed()) {
                    ensureVoucherRequestsTable(conn);
                    ensureInvoicesTable(conn);
                    ensureVoucherStoresTable(conn);
                    // ensureRedemptionsTable(conn); // Removed - redemption functionality not used
                    ensureAuditTrailTable(conn);
                    ensureVouchersTable(conn); // Ensure vouchers table exists first
                    updateVouchersTable(conn); // Then update/add columns
                    updateRequestsTable(conn);
                    System.out.println("Database schema verified/updated successfully.");
                    schemaChecked = true;
                }
            } catch (Exception e) {
                System.err.println("Error updating database schema: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Don't close the connection - let DBconnection manage it
                // The static connection in DBconnection should remain open
            }
        }
    }
    
    /**
     * Reset schema check flag (useful for testing or when schema changes)
     */
    public static void resetSchemaCheck() {
        synchronized (schemaCheckLock) {
            schemaChecked = false;
        }
    }
    
    private static void ensureVoucherRequestsTable(Connection conn) throws Exception {
        if (!tableExists(conn, "voucher_requests")) {
            try (Statement stmt = conn.createStatement()) {
                String sql = """
                    CREATE TABLE voucher_requests (
                        request_id SERIAL PRIMARY KEY,
                        request_reference VARCHAR(50) UNIQUE NOT NULL,
                        ref_client INTEGER NOT NULL,
                        client_name VARCHAR(255) NOT NULL,
                        client_email VARCHAR(255),
                        num_vouchers INTEGER NOT NULL,
                        unit_value DECIMAL(10, 2) NOT NULL,
                        total_value DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) DEFAULT 'initiated',
                        payment_status VARCHAR(50) DEFAULT 'unpaid',
                        payment_date TIMESTAMP,
                        approved_by VARCHAR(100),
                        approval_date TIMESTAMP,
                        processed_by VARCHAR(100),
                        vouchers_generated BOOLEAN DEFAULT FALSE,
                        vouchers_sent BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (ref_client) REFERENCES clients(ref_client) ON DELETE CASCADE
                    )
                """;
                stmt.executeUpdate(sql);
                stmt.executeUpdate("CREATE INDEX idx_voucher_requests_ref ON voucher_requests(request_reference)");
                stmt.executeUpdate("CREATE INDEX idx_voucher_requests_client ON voucher_requests(ref_client)");
                System.out.println("Created voucher_requests table");
            }
        } else {
            // Table exists, add missing columns
            addColumnIfNotExists(conn, "voucher_requests", "client_email", "VARCHAR(255)");
            addColumnIfNotExists(conn, "voucher_requests", "vouchers_generated", "BOOLEAN DEFAULT FALSE");
            addColumnIfNotExists(conn, "voucher_requests", "vouchers_sent", "BOOLEAN DEFAULT FALSE");
        }
    }
    
    private static void ensureInvoicesTable(Connection conn) throws Exception {
        if (!tableExists(conn, "invoices")) {
            try (Statement stmt = conn.createStatement()) {
                String sql = """
                    CREATE TABLE invoices (
                        invoice_id SERIAL PRIMARY KEY,
                        invoice_number VARCHAR(50) UNIQUE NOT NULL,
                        request_id INTEGER NOT NULL,
                        request_reference VARCHAR(50) NOT NULL,
                        ref_client INTEGER NOT NULL,
                        client_name VARCHAR(255) NOT NULL,
                        total_amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(50) DEFAULT 'pending',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (request_id) REFERENCES voucher_requests(request_id) ON DELETE CASCADE,
                        FOREIGN KEY (ref_client) REFERENCES clients(ref_client) ON DELETE CASCADE
                    )
                """;
                stmt.executeUpdate(sql);
                stmt.executeUpdate("CREATE INDEX idx_invoices_request ON invoices(request_id)");
                System.out.println("Created invoices table");
            }
        }
    }
    
    private static void ensureVoucherStoresTable(Connection conn) throws Exception {
        if (!tableExists(conn, "voucher_stores")) {
            try (Statement stmt = conn.createStatement()) {
                // First ensure vouchers table has the right structure
                updateVouchersTable(conn);
                
                // Determine which column to use for voucher code reference
                String voucherCodeColumn = "code_voucher";
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns " +
                    "WHERE table_name = 'vouchers' AND column_name IN ('code_voucher', 'ref_voucher') " +
                    "ORDER BY CASE WHEN column_name = 'code_voucher' THEN 1 ELSE 2 END")) {
                    if (rs.next()) {
                        String colName = rs.getString("column_name");
                        // Check if ref_voucher is VARCHAR
                        try (ResultSet typeRs = stmt.executeQuery(
                            "SELECT data_type FROM information_schema.columns " +
                            "WHERE table_name = 'vouchers' AND column_name = '" + colName + "'")) {
                            if (typeRs.next()) {
                                String dataType = typeRs.getString("data_type");
                                if ("character varying".equalsIgnoreCase(dataType) || "varchar".equalsIgnoreCase(dataType)) {
                                    voucherCodeColumn = colName;
                                } else if ("code_voucher".equals(colName)) {
                                    voucherCodeColumn = "code_voucher";
                                }
                            }
                        }
                    }
                }
                
                String sql = """
                    CREATE TABLE voucher_stores (
                        voucher_store_id SERIAL PRIMARY KEY,
                        voucher_code VARCHAR(100) NOT NULL,
                        branch_id INTEGER,
                        company VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (branch_id) REFERENCES branch(branch_id) ON DELETE SET NULL
                    )
                """;
                stmt.executeUpdate(sql);
                
                // Add foreign key constraint only if the referenced column is VARCHAR
                try {
                    stmt.executeUpdate("ALTER TABLE voucher_stores ADD CONSTRAINT voucher_stores_voucher_code_fkey " +
                                     "FOREIGN KEY (voucher_code) REFERENCES vouchers(" + voucherCodeColumn + ") ON DELETE CASCADE");
                } catch (SQLException e) {
                    System.out.println("Note: Could not add foreign key constraint for voucher_code: " + e.getMessage());
                    // Continue without foreign key - it's not critical
                }
                
                stmt.executeUpdate("CREATE INDEX idx_voucher_stores_code ON voucher_stores(voucher_code)");
                stmt.executeUpdate("CREATE INDEX idx_voucher_stores_branch ON voucher_stores(branch_id)");
                System.out.println("Created voucher_stores table");
            }
        } else {
            // Table exists, check and fix foreign key if needed
            try (Statement stmt = conn.createStatement()) {
                // Check if foreign key exists and is problematic
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT constraint_name FROM information_schema.table_constraints " +
                    "WHERE table_name = 'voucher_stores' AND constraint_type = 'FOREIGN KEY' " +
                    "AND constraint_name LIKE '%voucher_code%'")) {
                    if (rs.next()) {
                        String constraintName = rs.getString("constraint_name");
                        // Try to drop and recreate with correct reference
                        try {
                            stmt.executeUpdate("ALTER TABLE voucher_stores DROP CONSTRAINT IF EXISTS " + constraintName);
                            System.out.println("Dropped problematic foreign key constraint: " + constraintName);
                        } catch (SQLException e) {
                            System.out.println("Note: Could not drop constraint: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
    
    private static void ensureAuditTrailTable(Connection conn) throws Exception {
        if (!tableExists(conn, "audit_trail")) {
            try (Statement stmt = conn.createStatement()) {
                String sql = """
                    CREATE TABLE audit_trail (
                        audit_id SERIAL PRIMARY KEY,
                        action_type VARCHAR(100) NOT NULL,
                        entity_type VARCHAR(100) NOT NULL,
                        entity_id VARCHAR(255),
                        user_name VARCHAR(100),
                        action_description TEXT,
                        old_value TEXT,
                        new_value TEXT,
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        ip_address VARCHAR(50),
                        context TEXT
                    )
                """;
                stmt.executeUpdate(sql);
                stmt.executeUpdate("CREATE INDEX idx_audit_type ON audit_trail(action_type)");
                stmt.executeUpdate("CREATE INDEX idx_audit_entity ON audit_trail(entity_type, entity_id)");
                stmt.executeUpdate("CREATE INDEX idx_audit_timestamp ON audit_trail(timestamp)");
                System.out.println("Created audit_trail table");
            }
        }
    }
    
    private static void ensureVouchersTable(Connection conn) throws Exception {
        if (!tableExists(conn, "vouchers")) {
            try (Statement stmt = conn.createStatement()) {
                String sql = """
                    CREATE TABLE vouchers (
                        ref_voucher VARCHAR(100) PRIMARY KEY,
                        ref_client INTEGER NOT NULL,
                        code_voucher VARCHAR(100) UNIQUE,
                        val_voucher DECIMAL(10, 2) NOT NULL,
                        price DECIMAL(10, 2),
                        init_date DATE NOT NULL,
                        expiry_date DATE NOT NULL,
                        status_voucher VARCHAR(50) DEFAULT 'Available',
                        redeemed BOOLEAN DEFAULT FALSE,
                        date_redeemed TIMESTAMP,
                        bearer VARCHAR(255),
                        ref_request INTEGER,
                        request_id INTEGER,
                        request_reference VARCHAR(50),
                        assigned_to_request BOOLEAN DEFAULT FALSE,
                        qr_code_data TEXT,
                        pdf_path VARCHAR(500),
                        email_sent BOOLEAN DEFAULT FALSE,
                        email_sent_date TIMESTAMP,
                        redeemed_by VARCHAR(100),
                        redeemed_branch VARCHAR(255),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (ref_client) REFERENCES clients(ref_client) ON DELETE CASCADE
                    )
                """;
                stmt.executeUpdate(sql);
                stmt.executeUpdate("CREATE INDEX idx_vouchers_client ON vouchers(ref_client)");
                stmt.executeUpdate("CREATE INDEX idx_vouchers_code ON vouchers(code_voucher)");
                stmt.executeUpdate("CREATE INDEX idx_vouchers_status ON vouchers(status_voucher)");
                stmt.executeUpdate("CREATE INDEX idx_vouchers_redeemed ON vouchers(redeemed)");
                System.out.println("Created vouchers table");
            }
        }
    }
    
    private static void updateVouchersTable(Connection conn) throws Exception {
        // First, ensure the table exists
        if (!tableExists(conn, "vouchers")) {
            ensureVouchersTable(conn);
            return; // Table was just created with all columns
        }
        
        // Table exists, check and fix column types
        // CRITICAL: Fix ref_voucher type if it's INTEGER (should be VARCHAR for voucher codes)
        try (Statement stmt = conn.createStatement()) {
            // Check the current type of ref_voucher
            try (ResultSet rs = stmt.executeQuery(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name = 'ref_voucher'")) {
                if (rs.next()) {
                    String dataType = rs.getString("data_type");
                    if ("integer".equalsIgnoreCase(dataType) || "int4".equalsIgnoreCase(dataType)) {
                        // Need to change from INTEGER to VARCHAR
                        System.out.println("Converting ref_voucher from INTEGER to VARCHAR...");
                        try {
                            // First, drop any constraints that might reference it
                            // Then alter the column type
                            stmt.executeUpdate("ALTER TABLE vouchers ALTER COLUMN ref_voucher TYPE VARCHAR(100) USING ref_voucher::text");
                            System.out.println("Successfully converted ref_voucher to VARCHAR(100)");
                        } catch (SQLException e) {
                            System.out.println("Note: Could not convert ref_voucher type: " + e.getMessage());
                            // If conversion fails, we'll use code_voucher instead
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Note: Could not check ref_voucher type: " + e.getMessage());
        }
        
        // Table exists, so add missing columns
        // Ensure ref_client column exists first (required for foreign key)
        addColumnIfNotExists(conn, "vouchers", "ref_client", "INTEGER");
        
        // Ensure all required columns exist (these are critical)
        addColumnIfNotExists(conn, "vouchers", "ref_voucher", "VARCHAR(100)");
        addColumnIfNotExists(conn, "vouchers", "val_voucher", "DECIMAL(10, 2)");
        addColumnIfNotExists(conn, "vouchers", "price", "DECIMAL(10, 2)"); // Alternative name
        addColumnIfNotExists(conn, "vouchers", "code_voucher", "VARCHAR(100)"); // Alternative name
        addColumnIfNotExists(conn, "vouchers", "init_date", "DATE");
        addColumnIfNotExists(conn, "vouchers", "expiry_date", "DATE");
        addColumnIfNotExists(conn, "vouchers", "status_voucher", "VARCHAR(50)");
        
        // CRITICAL: Ensure redeemed column exists (this is the one causing the error)
        addColumnIfNotExists(conn, "vouchers", "redeemed", "BOOLEAN DEFAULT FALSE");
        
        addColumnIfNotExists(conn, "vouchers", "date_redeemed", "TIMESTAMP");
        addColumnIfNotExists(conn, "vouchers", "bearer", "VARCHAR(255)");
        
        // Add optional columns if they don't exist
        addColumnIfNotExists(conn, "vouchers", "ref_request", "INTEGER"); // May be required by existing schema
        addColumnIfNotExists(conn, "vouchers", "request_id", "INTEGER");
        addColumnIfNotExists(conn, "vouchers", "request_reference", "VARCHAR(50)");
        addColumnIfNotExists(conn, "vouchers", "assigned_to_request", "BOOLEAN DEFAULT FALSE");
        addColumnIfNotExists(conn, "vouchers", "qr_code_data", "TEXT");
        addColumnIfNotExists(conn, "vouchers", "pdf_path", "VARCHAR(500)");
        addColumnIfNotExists(conn, "vouchers", "email_sent", "BOOLEAN DEFAULT FALSE");
        addColumnIfNotExists(conn, "vouchers", "email_sent_date", "TIMESTAMP");
        addColumnIfNotExists(conn, "vouchers", "redeemed_by", "VARCHAR(100)");
        
        // Check if redeemed_branch exists and what type it is
        String dataType = null;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name = 'redeemed_branch'")) {
            if (rs.next()) {
                dataType = rs.getString("data_type");
            }
        } catch (SQLException e) {
            System.err.println("Error checking redeemed_branch column: " + e.getMessage());
            e.printStackTrace();
        }
        
        // If column exists and is integer, convert it
        if (dataType != null && ("integer".equalsIgnoreCase(dataType) || "int4".equalsIgnoreCase(dataType))) {
            // First, find and drop all foreign key constraints on this column
            try (Statement fkStmt = conn.createStatement();
                 ResultSet fkRs = fkStmt.executeQuery(
                    "SELECT tc.constraint_name FROM information_schema.table_constraints tc " +
                    "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name " +
                    "WHERE tc.table_name = 'vouchers' AND kcu.column_name = 'redeemed_branch' " +
                    "AND tc.constraint_type = 'FOREIGN KEY'")) {
                while (fkRs.next()) {
                    String constraintName = fkRs.getString("constraint_name");
                    try (Statement dropStmt = conn.createStatement()) {
                        dropStmt.executeUpdate("ALTER TABLE vouchers DROP CONSTRAINT IF EXISTS " + constraintName);
                        System.out.println("Dropped foreign key constraint: " + constraintName);
                    } catch (SQLException e) {
                        System.err.println("Could not drop constraint " + constraintName + ": " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error finding foreign key constraints: " + e.getMessage());
            }
            
            // Now alter the column type
            try (Statement alterStmt = conn.createStatement()) {
                alterStmt.executeUpdate("ALTER TABLE vouchers ALTER COLUMN redeemed_branch TYPE VARCHAR(255) USING redeemed_branch::text");
                System.out.println("Altered redeemed_branch column from INTEGER to VARCHAR(255)");
            } catch (SQLException e) {
                // Try without USING clause (some PostgreSQL versions)
                try (Statement alterStmt2 = conn.createStatement()) {
                    alterStmt2.executeUpdate("ALTER TABLE vouchers ALTER COLUMN redeemed_branch TYPE VARCHAR(255)");
                    System.out.println("Altered redeemed_branch column from INTEGER to VARCHAR(255)");
                } catch (SQLException e2) {
                    System.err.println("Could not alter redeemed_branch column: " + e2.getMessage());
                    e2.printStackTrace();
                }
            }
        } else if (dataType == null) {
            // Column doesn't exist, add it as VARCHAR
            addColumnIfNotExists(conn, "vouchers", "redeemed_branch", "VARCHAR(255)");
        }
        
        // Fix ref_request foreign key constraint and make it nullable
        try (Statement stmt = conn.createStatement()) {
            // Check if ref_request column exists
            boolean refRequestExists = false;
            try (ResultSet rs = stmt.executeQuery(
                "SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'vouchers' AND column_name = 'ref_request'")) {
                if (rs.next()) {
                    refRequestExists = true;
                    String isNullable = rs.getString("is_nullable");
                    if ("NO".equalsIgnoreCase(isNullable)) {
                        // Make it nullable
                        try {
                            stmt.executeUpdate("ALTER TABLE vouchers ALTER COLUMN ref_request DROP NOT NULL");
                            System.out.println("Made ref_request column nullable");
                        } catch (SQLException e) {
                            System.out.println("Note: Could not make ref_request nullable: " + e.getMessage());
                        }
                    }
                }
            }
            
            // Check and fix foreign key constraints on ref_request
            if (refRequestExists) {
                try {
                    // Get all foreign key constraints on vouchers table
                    DatabaseMetaData metaData = conn.getMetaData();
                    try (ResultSet fkRs = metaData.getImportedKeys(null, null, "vouchers")) {
                        while (fkRs.next()) {
                            String fkName = fkRs.getString("FK_NAME");
                            String fkColumn = fkRs.getString("FKCOLUMN_NAME");
                            String pkTable = fkRs.getString("PKTABLE_NAME");
                            
                            // If ref_request has a foreign key to wrong table, drop it
                            if ("ref_request".equals(fkColumn) && !"voucher_requests".equals(pkTable)) {
                                try {
                                    stmt.executeUpdate("ALTER TABLE vouchers DROP CONSTRAINT IF EXISTS " + fkName);
                                    System.out.println("Dropped incorrect foreign key constraint: " + fkName);
                                } catch (SQLException e) {
                                    System.out.println("Note: Could not drop constraint " + fkName + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                    
                    // Add correct foreign key if voucher_requests table exists
                    if (tableExists(conn, "voucher_requests")) {
                        try {
                            stmt.executeUpdate("ALTER TABLE vouchers ADD CONSTRAINT fk_vouchers_request " +
                                             "FOREIGN KEY (ref_request) REFERENCES voucher_requests(request_id) ON DELETE SET NULL");
                            System.out.println("Added correct foreign key constraint for ref_request");
                        } catch (SQLException e) {
                            // Constraint might already exist or other issue, ignore
                            System.out.println("Note: Could not add foreign key constraint: " + e.getMessage());
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("Note: Could not check/fix foreign key constraints: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.out.println("Note: Could not check ref_request constraint: " + e.getMessage());
        }
        
        // Add foreign key constraint if it doesn't exist
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getImportedKeys(null, null, "vouchers")) {
                boolean hasForeignKey = false;
                while (rs.next()) {
                    if ("ref_client".equals(rs.getString("FKCOLUMN_NAME"))) {
                        hasForeignKey = true;
                        break;
                    }
                }
                if (!hasForeignKey && columnExists(conn, "vouchers", "ref_client")) {
                    try (Statement stmt = conn.createStatement()) {
                        // Try to add foreign key (may fail if constraint already exists, that's ok)
                        stmt.execute("ALTER TABLE vouchers ADD CONSTRAINT fk_vouchers_client " +
                                   "FOREIGN KEY (ref_client) REFERENCES clients(ref_client) ON DELETE CASCADE");
                    } catch (SQLException e) {
                        // Constraint might already exist, ignore
                        System.out.println("Note: Foreign key constraint may already exist: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Note: Could not check/add foreign key: " + e.getMessage());
        }
    }
    
    private static void updateRequestsTable(Connection conn) throws Exception {
        // Add columns if they don't exist
        addColumnIfNotExists(conn, "requests", "request_reference", "VARCHAR(50)");
        addColumnIfNotExists(conn, "requests", "client_name", "VARCHAR(255)");
        addColumnIfNotExists(conn, "requests", "unit_value", "DECIMAL(10, 2)");
        addColumnIfNotExists(conn, "requests", "total_value", "DECIMAL(10, 2)");
        addColumnIfNotExists(conn, "requests", "payment_status", "VARCHAR(50) DEFAULT 'unpaid'");
        addColumnIfNotExists(conn, "requests", "approver_email", "VARCHAR(255)");
        addColumnIfNotExists(conn, "requests", "invoice_id", "INTEGER");
        addColumnIfNotExists(conn, "requests", "vouchers_generated", "BOOLEAN DEFAULT FALSE");
        addColumnIfNotExists(conn, "requests", "vouchers_sent", "BOOLEAN DEFAULT FALSE");
    }
    
    private static boolean tableExists(Connection conn, String tableName) throws Exception {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
    
    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
    
    private static void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnType) throws Exception {
        if (!columnExists(conn, tableName, columnName)) {
            try (Statement stmt = conn.createStatement()) {
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
                stmt.executeUpdate(sql);
                System.out.println("Added column " + columnName + " to " + tableName);
            }
        }
    }
    
    private static void ensureColumnExists(Connection conn, String tableName, String columnName, String columnType) throws Exception {
        if (!columnExists(conn, tableName, columnName)) {
            addColumnIfNotExists(conn, tableName, columnName, columnType);
        }
    }
}

