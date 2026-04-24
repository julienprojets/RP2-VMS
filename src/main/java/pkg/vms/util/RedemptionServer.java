package pkg.vms.util;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import pkg.vms.DBconnection.DBconnection;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple HTTP server for voucher redemption via QR code scanning
 * Accessible from mobile devices
 */
public class RedemptionServer {
    
    private static HttpServer server;
    private static int port = RedemptionConfig.getLocalServerPort();
    private static boolean isRunning = false;
    
    /**
     * Start the redemption server
     */
    public static void startServer() {
        if (!RedemptionConfig.isLocalServerEnabled()) {
            System.out.println("Redemption local server is disabled by configuration.");
            return;
        }

        if (isRunning) {
            System.out.println("Redemption server is already running on port " + port);
            return;
        }
        
        // Ensures schema migrations (e.g. drop FK on redeemed_by for free-text mobile redemption) run before redeem
        DatabaseSchema.ensureSchemaExists();
        
        try {
            // Bind to 0.0.0.0 to make it accessible from other devices on the network
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            
            // Main redemption page
            server.createContext("/", new RedemptionPageHandler());
            
            // API endpoint for redemption (legacy + versioned alias)
            server.createContext("/redeem", new RedemptionAPIHandler());
            server.createContext("/api/redeem", new RedemptionAPIHandler());
            
            // API endpoint to check voucher status (legacy + versioned alias)
            server.createContext("/check", new CheckVoucherHandler());
            server.createContext("/api/check", new CheckVoucherHandler());
            
            server.setExecutor(null); // Use default executor
            server.start();
            isRunning = true;
            
            String localIP = getLocalIP();
            System.out.println("========================================");
            System.out.println("Redemption Server Started!");
            System.out.println("Server is accessible from any device on your network");
            System.out.println("Access from phone:");
            System.out.println("  http://" + localIP + ":" + port);
            System.out.println("  or");
            System.out.println("  http://localhost:" + port);
            System.out.println("");
            System.out.println("IMPORTANT: Make sure your phone and computer are on the same Wi-Fi network");
            System.out.println("If still not accessible, check Windows Firewall settings for port " + port);
            System.out.println("========================================");
            
        } catch (IOException e) {
            System.err.println("Failed to start redemption server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stop the redemption server
     */
    public static void stopServer() {
        if (server != null && isRunning) {
            server.stop(0);
            isRunning = false;
            System.out.println("Redemption server stopped.");
        }
    }
    
    /**
     * Check if server is running
     */
    public static boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get the server port
     */
    public static int getPort() {
        return port;
    }
    
    /**
     * Set the server port (must be called before startServer)
     */
    public static void setPort(int newPort) {
        if (!isRunning) {
            port = newPort;
        }
    }
    
    /**
     * Get local IP address (prefer non-loopback address)
     */
    private static String getLocalIP() {
        try {
            // Try to get a non-loopback IP address
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    // Prefer IPv4 addresses
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        // Skip localhost and link-local addresses
                        if (!ip.equals("127.0.0.1") && !ip.startsWith("169.254")) {
                            return ip;
                        }
                    }
                }
            }
            // Fallback to localhost
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    /**
     * Handler for the main redemption page
     */
    static class RedemptionPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String html = RedemptionPageLoader.loadPageHtml(RedemptionConfig.getApiBaseUrl());
                sendResponse(exchange, 200, html, "text/html");
            } else {
                sendResponse(exchange, 405, "Method not allowed", "text/plain");
            }
        }
    }
    
    /**
     * Handler for voucher redemption API
     */
    static class RedemptionAPIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 204, "", "text/plain");
                    return;
                }

                if ("POST".equals(exchange.getRequestMethod())) {
                    // Read request body
                    String requestBody = readRequestBody(exchange);
                    System.out.println("Received redemption request: " + requestBody);
                    
                    // Parse JSON (simple parsing)
                    String voucherCode = extractValue(requestBody, "voucherCode");
                    String branch = extractValue(requestBody, "branch");
                    String redeemedBy = extractValue(requestBody, "redeemedBy");
                    
                    System.out.println("Parsed - VoucherCode: " + voucherCode + ", Branch: " + branch + ", RedeemedBy: " + redeemedBy);
                    
                    if (voucherCode == null || voucherCode.isEmpty()) {
                        System.err.println("Error: Voucher code is missing or empty");
                        sendJSONResponse(exchange, 400, "{\"success\":false,\"message\":\"Voucher code is required\"}");
                        return;
                    }
                    
                    // Process redemption
                    System.out.println("Processing redemption for voucher: " + voucherCode);
                    RedemptionResult result = processRedemption(voucherCode, branch, redeemedBy);
                    System.out.println("Redemption result - Success: " + result.success + ", Message: " + result.message);
                    
                    if (result.success) {
                        sendJSONResponse(exchange, 200, 
                            String.format("{\"success\":true,\"message\":\"%s\"}", escapeJSON(result.message)));
                    } else {
                        System.err.println("Redemption failed: " + result.message);
                        sendJSONResponse(exchange, 400, 
                            String.format("{\"success\":false,\"message\":\"%s\"}", escapeJSON(result.message)));
                    }
                } else {
                    sendResponse(exchange, 405, "Method not allowed", "text/plain");
                }
            } catch (Exception e) {
                System.err.println("Error in RedemptionAPIHandler: " + e.getMessage());
                e.printStackTrace();
                try {
                    sendJSONResponse(exchange, 500, 
                        String.format("{\"success\":false,\"message\":\"Internal server error: %s\"}", escapeJSON(e.getMessage())));
                } catch (IOException ioEx) {
                    System.err.println("Failed to send error response: " + ioEx.getMessage());
                }
            }
        }
    }
    
    /**
     * Handler for checking voucher status
     */
    static class CheckVoucherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "", "text/plain");
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String voucherCode = extractQueryParam(query, "code");
                
                if (voucherCode == null || voucherCode.isEmpty()) {
                    sendJSONResponse(exchange, 400, "{\"success\":false,\"message\":\"Voucher code is required\"}");
                    return;
                }
                
                VoucherStatus status = checkVoucherStatus(voucherCode);
                sendJSONResponse(exchange, 200, status.toJSON());
            } else {
                sendResponse(exchange, 405, "Method not allowed", "text/plain");
            }
        }
    }
    
    /**
     * Get a valid database connection, retrying if needed
     */
    private static Connection getValidConnection() throws SQLException {
        // Always get a fresh connection - don't rely on static connection
        Connection c = null;
        int retries = 3;
        
        while (retries > 0) {
            try {
                c = DBconnection.getConnection();
                if (c == null) {
                    retries--;
                    if (retries > 0) {
                        Thread.sleep(100); // Wait 100ms before retry
                        continue;
                    }
                    throw new SQLException("Connection is null after retries");
                }
                
                // Check if connection is closed
                try {
                    if (c.isClosed()) {
                        retries--;
                        if (retries > 0) {
                            Thread.sleep(100);
                            continue;
                        }
                        throw new SQLException("Connection is closed");
                    }
                } catch (SQLException e) {
                    // isClosed() might throw if connection is bad
                    retries--;
                    if (retries > 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    throw new SQLException("Connection check failed: " + e.getMessage());
                }
                
                // Test connection with a simple query
                try {
                    if (!c.isValid(2)) {
                        retries--;
                        if (retries > 0) {
                            Thread.sleep(100);
                            continue;
                        }
                        throw new SQLException("Connection is invalid");
                    }
                } catch (SQLException e) {
                    // isValid might throw if connection is bad
                    retries--;
                    if (retries > 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    throw new SQLException("Connection validation failed: " + e.getMessage());
                }
                
                // Connection is valid
                return c;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Connection retry interrupted");
            } catch (SQLException e) {
                if (retries <= 1) {
                    throw e; // Re-throw if out of retries
                }
                retries--;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Connection retry interrupted");
                }
            }
        }
        
        throw new SQLException("Could not establish valid connection after retries");
    }
    
    /**
     * Process voucher redemption - simplified with automatic connection management
     */
    private static void logRedemptionAudit(
            String voucherCode,
            String branch,
            String redeemedBy,
            String status,
            String message
    ) {
        String safeBranch = branch != null && !branch.trim().isEmpty() ? branch : "Unknown Company";
        String safeRedeemedBy = redeemedBy != null && !redeemedBy.trim().isEmpty() ? redeemedBy : "Mobile User";
        String safeStatus = status != null ? status : "FAILED";

        try (Connection auditConn = DBconnection.getConnection()) {
            if (auditConn == null || auditConn.isClosed()) {
                return;
            }

            String sql = "INSERT INTO redemption_audit(status, branch, voucher_code, redeemed_by, message) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = auditConn.prepareStatement(sql)) {
                ps.setString(1, safeStatus);
                ps.setString(2, safeBranch);
                ps.setString(3, voucherCode);
                ps.setString(4, safeRedeemedBy);
                ps.setString(5, message);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Warning: could not log redemption audit: " + e.getMessage());
        }
    }

    private static RedemptionResult processRedemption(String voucherCode, String branch, String redeemedBy) {
        // Try up to 3 times with fresh connections
        for (int attempt = 1; attempt <= 3; attempt++) {
            Connection conn = null;
            try {
                // Get a fresh connection for this attempt
                conn = DBconnection.getConnection();
                if (conn == null) {
                    if (attempt < 3) {
                        System.err.println("Connection is null, retrying attempt " + (attempt + 1));
                        Thread.sleep(200);
                        continue;
                    }
                    return new RedemptionResult(false, "Database connection failed. Please try again.");
                }
                
                // Verify connection
                try {
                    if (conn.isClosed() || !conn.isValid(2)) {
                        if (attempt < 3) {
                            System.err.println("Connection invalid, retrying attempt " + (attempt + 1));
                            Thread.sleep(200);
                            continue;
                        }
                        return new RedemptionResult(false, "Database connection invalid. Please try again.");
                    }
                } catch (SQLException e) {
                    if (attempt < 3) {
                        System.err.println("Connection check failed, retrying attempt " + (attempt + 1) + ": " + e.getMessage());
                        Thread.sleep(200);
                        continue;
                    }
                    return new RedemptionResult(false, "Database connection error. Please try again.");
                }
                
                // Verify connection is still valid after getting it
                try {
                    if (conn.isClosed() || !conn.isValid(2)) {
                        throw new SQLException("Connection became invalid immediately after getting it");
                    }
                } catch (SQLException e) {
                    if (attempt < 3) {
                        System.err.println("Connection invalid after getting it, retrying attempt " + (attempt + 1));
                        Thread.sleep(200);
                        continue;
                    }
                    return new RedemptionResult(false, "Database connection error. Please try again.");
                }
                
                // Set up transaction FIRST, before any other operations
                boolean originalAutoCommit = true;
                try {
                    originalAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);
                } catch (SQLException e) {
                    if (attempt < 3) {
                        System.err.println("Failed to set autoCommit, retrying attempt " + (attempt + 1) + ": " + e.getMessage());
                        Thread.sleep(200);
                        continue;
                    }
                    return new RedemptionResult(false, "Database transaction error. Please try again.");
                }
                
                // Verify connection is still valid after setting autoCommit
                try {
                    if (conn.isClosed() || !conn.isValid(2)) {
                        throw new SQLException("Connection became invalid after setting autoCommit");
                    }
                } catch (SQLException e) {
                    if (attempt < 3) {
                        System.err.println("Connection invalid after setting autoCommit, retrying attempt " + (attempt + 1));
                        Thread.sleep(200);
                        continue;
                    }
                    return new RedemptionResult(false, "Database connection error. Please try again.");
                }
                
                try {
                    // Fix redeemed_branch column if needed (non-critical, but verify connection first)
                    try {
                        if (!conn.isClosed() && conn.isValid(2)) {
                            fixRedeemedBranchColumn(conn);
                        }
                    } catch (Exception e) {
                        // Non-critical, continue
                        System.err.println("Warning: Could not fix redeemed_branch column: " + e.getMessage());
                    }
                    
                    // Verify connection again before getting column name
                    if (conn.isClosed() || !conn.isValid(2)) {
                        throw new SQLException("Connection became invalid before getVoucherCodeColumn");
                    }
                    
                    // Get voucher code column name
                    String voucherCodeColumn = getVoucherCodeColumn(conn);
                
                    // Verify connection again before query
                    if (conn.isClosed() || !conn.isValid(2)) {
                        throw new SQLException("Connection became invalid before query");
                    }
                
                    // Check if voucher exists and is valid
                    String checkSql = "SELECT " + voucherCodeColumn + ", status_voucher, redeemed, expiry_date, " +
                                    "request_id, request_reference FROM vouchers WHERE " + voucherCodeColumn + " = ?";
                
                    VoucherInfo voucherInfo = null;
                    try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setString(1, voucherCode);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            logRedemptionAudit(
                                    voucherCode,
                                    branch,
                                    redeemedBy,
                                    "FAILED",
                                    "Voucher code not found."
                            );
                            return new RedemptionResult(false, "Voucher code not found.");
                        }
                        
                        voucherInfo = new VoucherInfo();
                        voucherInfo.status = rs.getString("status_voucher");
                        voucherInfo.redeemed = rs.getBoolean("redeemed");
                        voucherInfo.expiryDate = rs.getDate("expiry_date");
                        // Get request_id - handle both integer and null
                        int requestId = rs.getInt("request_id");
                        voucherInfo.requestId = rs.wasNull() ? null : requestId;
                    }
                }
                
                // Validate voucher
                if (voucherInfo.redeemed) {
                    logRedemptionAudit(
                            voucherCode,
                            branch,
                            redeemedBy,
                            "FAILED",
                            "Voucher has already been redeemed."
                    );
                    return new RedemptionResult(false, "Voucher has already been redeemed.");
                }
                
                if (voucherInfo.expiryDate != null && voucherInfo.expiryDate.before(new java.sql.Date(System.currentTimeMillis()))) {
                    logRedemptionAudit(
                            voucherCode,
                            branch,
                            redeemedBy,
                            "FAILED",
                            "Voucher has expired."
                    );
                    return new RedemptionResult(false, "Voucher has expired.");
                }
                
                if (!"Active".equals(voucherInfo.status)) {
                    logRedemptionAudit(
                            voucherCode,
                            branch,
                            redeemedBy,
                            "FAILED",
                            "Voucher is not active. Status: " + voucherInfo.status
                    );
                    return new RedemptionResult(false, "Voucher is not active. Status: " + voucherInfo.status);
                }
                
                    // Verify connection before update
                    if (conn.isClosed() || !conn.isValid(2)) {
                        throw new SQLException("Connection became invalid before update");
                    }
                
                    // Update voucher as redeemed
                    String updateSql = "UPDATE vouchers SET redeemed = TRUE, " +
                                     "status_voucher = 'Redeemed', " +
                                     "date_redeemed = CURRENT_TIMESTAMP, " +
                                     "redeemed_by = ?, " +
                                     "redeemed_branch = ? " +
                                     "WHERE " + voucherCodeColumn + " = ?";
                
                    int updated = 0;
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, redeemedBy != null ? redeemedBy : "Mobile User");
                        updateStmt.setString(2, branch != null ? branch : "Unknown Company");
                        updateStmt.setString(3, voucherCode);
                        updated = updateStmt.executeUpdate();
                    }
                
                    if (updated == 0) {
                        throw new SQLException("Failed to update voucher - no rows affected.");
                    }
                
                // Also check by request_reference if request_id is not available
                String requestReference = null;
                try (PreparedStatement refStmt = conn.prepareStatement(
                    "SELECT request_reference FROM vouchers WHERE " + voucherCodeColumn + " = ?")) {
                    refStmt.setString(1, voucherCode);
                    try (ResultSet refRs = refStmt.executeQuery()) {
                        if (refRs.next()) {
                            requestReference = refRs.getString("request_reference");
                        }
                    }
                }
                
                // Check if all vouchers in the request are redeemed
                if (voucherInfo.requestId != null && voucherInfo.requestId > 0) {
                    String countSql = "SELECT COUNT(*) as total, " +
                                    "SUM(CASE WHEN redeemed = TRUE THEN 1 ELSE 0 END) as redeemed_count " +
                                    "FROM vouchers WHERE request_id = ?";
                    
                    try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                        countStmt.setInt(1, voucherInfo.requestId);
                        try (ResultSet rs = countStmt.executeQuery()) {
                            if (rs.next()) {
                                int total = rs.getInt("total");
                                int redeemedCount = rs.getInt("redeemed_count");
                                
                                if (total > 0 && total == redeemedCount) {
                                    // All vouchers redeemed, update request status
                                    String updateRequestSql = "UPDATE voucher_requests SET status = 'Redeemed' " +
                                                            "WHERE request_id = ?";
                                    try (PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql)) {
                                        updateRequestStmt.setInt(1, voucherInfo.requestId);
                                        updateRequestStmt.executeUpdate();
                                        System.out.println("Updated request " + voucherInfo.requestId + " status to Redeemed");
                                    }
                                }
                            }
                        }
                    }
                } else if (requestReference != null && !requestReference.isEmpty()) {
                    // Fallback: check by request_reference
                    String countSql = "SELECT COUNT(*) as total, " +
                                    "SUM(CASE WHEN redeemed = TRUE THEN 1 ELSE 0 END) as redeemed_count " +
                                    "FROM vouchers WHERE request_reference = ?";
                    
                    try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                        countStmt.setString(1, requestReference);
                        try (ResultSet rs = countStmt.executeQuery()) {
                            if (rs.next()) {
                                int total = rs.getInt("total");
                                int redeemedCount = rs.getInt("redeemed_count");
                                
                                if (total > 0 && total == redeemedCount) {
                                    // All vouchers redeemed, update request status
                                    String updateRequestSql = "UPDATE voucher_requests SET status = 'Redeemed' " +
                                                            "WHERE request_reference = ?";
                                    try (PreparedStatement updateRequestStmt = conn.prepareStatement(updateRequestSql)) {
                                        updateRequestStmt.setString(1, requestReference);
                                        updateRequestStmt.executeUpdate();
                                        System.out.println("Updated request " + requestReference + " status to Redeemed");
                                    }
                                }
                            }
                        }
                    }
                }
                
                    // Log redemption
                    AuditLogger.logRedemption(voucherCode, redeemedBy != null ? redeemedBy : "Mobile User", 
                                            branch != null ? branch : "Unknown Company");

                    // Also record redemption attempt (SUCCESS) for DB audit table
                    logRedemptionAudit(
                            voucherCode,
                            branch,
                            redeemedBy,
                            "SUCCESS",
                            "Voucher redeemed successfully!"
                    );
                    
                    // Commit transaction
                    conn.commit();
                    
                    // Restore auto-commit
                    try {
                        conn.setAutoCommit(originalAutoCommit);
                    } catch (SQLException e) {
                        // Ignore
                    }
                    
                    System.out.println("Voucher " + voucherCode + " successfully redeemed. Status: Redeemed, Request ID: " + 
                                     (voucherInfo.requestId != null ? voucherInfo.requestId : "N/A") + 
                                     ", Request Reference: " + (requestReference != null ? requestReference : "N/A"));
                    return new RedemptionResult(true, "Voucher redeemed successfully!");
                    
                } catch (SQLException e) {
                    // Rollback on error
                    try {
                        if (conn != null && !conn.isClosed()) {
                            conn.rollback();
                        }
                    } catch (SQLException rollbackEx) {
                        // Ignore
                    }
                    
                    // Check if it's a connection error
                    String errorMsg = e.getMessage();
                    boolean isConnectionError = errorMsg != null && 
                        (errorMsg.contains("closed") || errorMsg.contains("Connection") || 
                         errorMsg.contains("connection") || errorMsg.contains("This connection"));
                    
                    if (isConnectionError && attempt < 3) {
                        System.err.println("Connection error on attempt " + attempt + ", retrying: " + errorMsg);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            logRedemptionAudit(
                                    voucherCode,
                                    branch,
                                    redeemedBy,
                                    "FAILED",
                                    "Operation interrupted"
                            );
                            return new RedemptionResult(false, "Operation interrupted");
                        }
                        continue; // Retry with fresh connection
                    }
                    
                    // Not a connection error or out of retries
                    if (attempt >= 3) {
                        logRedemptionAudit(
                                voucherCode,
                                branch,
                                redeemedBy,
                                "FAILED",
                                "Error processing redemption: " + errorMsg
                        );
                        return new RedemptionResult(false, "Error processing redemption: " + errorMsg);
                    }
                    throw e; // Will be caught by outer catch
                    
                } finally {
                    // Restore auto-commit
                    if (conn != null) {
                        try {
                            if (!conn.isClosed()) {
                                conn.setAutoCommit(originalAutoCommit);
                            }
                        } catch (SQLException e) {
                            // Ignore
                        }
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logRedemptionAudit(
                        voucherCode,
                        branch,
                        redeemedBy,
                        "FAILED",
                        "Operation interrupted"
                );
                return new RedemptionResult(false, "Operation interrupted");
            } catch (Exception e) {
                if (attempt >= 3) {
                    System.err.println("Error in processRedemption after " + attempt + " attempts: " + e.getMessage());
                    e.printStackTrace();
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && (errorMsg.contains("closed") || errorMsg.contains("Connection"))) {
                        errorMsg = "Database connection error. Please try again.";
                    }
                    logRedemptionAudit(
                            voucherCode,
                            branch,
                            redeemedBy,
                            "FAILED",
                            "Error processing redemption: " + errorMsg
                    );
                    return new RedemptionResult(false, "Error processing redemption: " + errorMsg);
                }
                // Will retry on next iteration
            }
        }
        
        logRedemptionAudit(
                voucherCode,
                branch,
                redeemedBy,
                "FAILED",
                "Failed to process redemption after 3 attempts"
        );
        return new RedemptionResult(false, "Failed to process redemption after 3 attempts");
    }
    
    /**
     * Check voucher status
     */
    private static VoucherStatus checkVoucherStatus(String voucherCode) {
        Connection conn = null;
        try {
            conn = DBconnection.getConnection();
            if (conn == null || conn.isClosed()) {
                return new VoucherStatus(false, "Database connection failed", null, false, null);
            }
            
            String voucherCodeColumn = getVoucherCodeColumn(conn);
            
            String sql = "SELECT " + voucherCodeColumn + ", status_voucher, redeemed, expiry_date, " +
                        "val_voucher, ref_client FROM vouchers WHERE " + voucherCodeColumn + " = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, voucherCode);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return new VoucherStatus(false, "Voucher not found", null, false, null);
                    }
                    
                    String status = rs.getString("status_voucher");
                    boolean redeemed = rs.getBoolean("redeemed");
                    Date expiryDate = rs.getDate("expiry_date");
                    double value = rs.getDouble("val_voucher");
                    
                    String message = "Voucher is valid and ready for redemption.";
                    if (redeemed) {
                        message = "Voucher has already been redeemed.";
                    } else if (expiryDate != null && expiryDate.before(new java.sql.Date(System.currentTimeMillis()))) {
                        message = "Voucher has expired.";
                    } else if (!"Active".equals(status)) {
                        message = "Voucher is not active. Status: " + status;
                    }
                    
                    return new VoucherStatus(true, message, status, redeemed, value);
                }
            }
        } catch (SQLException e) {
            return new VoucherStatus(false, "Error checking voucher: " + e.getMessage(), null, false, null);
        }
        // Don't close connection - it's a shared static connection
    }
    
    /**
     * Fix redeemed_branch column type if it's INTEGER (should be VARCHAR)
     */
    private static void fixRedeemedBranchColumn(Connection conn) throws SQLException {
        // Verify connection is valid before use
        if (conn == null || conn.isClosed() || !conn.isValid(2)) {
            throw new SQLException("Connection is invalid in fixRedeemedBranchColumn");
        }
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
            return;
        }
        
        if (dataType != null && ("integer".equalsIgnoreCase(dataType) || "int4".equalsIgnoreCase(dataType))) {
            // Find and drop all foreign key constraints
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
                        System.out.println("Dropped FK constraint: " + constraintName);
                    } catch (SQLException e) {
                        // Ignore if constraint doesn't exist
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error finding FK constraints: " + e.getMessage());
            }
            
            // Alter column type
            try (Statement alterStmt = conn.createStatement()) {
                alterStmt.executeUpdate("ALTER TABLE vouchers ALTER COLUMN redeemed_branch TYPE VARCHAR(255) USING redeemed_branch::text");
                System.out.println("Fixed: Converted redeemed_branch from INTEGER to VARCHAR(255)");
            } catch (SQLException e) {
                // Try without USING clause
                try (Statement alterStmt2 = conn.createStatement()) {
                    alterStmt2.executeUpdate("ALTER TABLE vouchers ALTER COLUMN redeemed_branch TYPE VARCHAR(255)");
                    System.out.println("Fixed: Converted redeemed_branch from INTEGER to VARCHAR(255)");
                } catch (SQLException e2) {
                    System.err.println("Could not fix redeemed_branch column: " + e2.getMessage());
                }
            }
        }
    }
    
    private static String getVoucherCodeColumn(Connection conn) throws SQLException {
        // Verify connection is valid before use
        if (conn == null || conn.isClosed() || !conn.isValid(2)) {
            throw new SQLException("Connection is invalid in getVoucherCodeColumn");
        }
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT column_name FROM information_schema.columns " +
                 "WHERE table_name = 'vouchers' AND column_name IN ('code_voucher', 'ref_voucher') " +
                 "ORDER BY CASE WHEN column_name = 'code_voucher' THEN 1 ELSE 2 END LIMIT 1")) {
            if (rs.next()) {
                return rs.getString("column_name");
            }
        }
        return "code_voucher";
    }
    
    private static String extractValue(String json, String key) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            // Try to find the key in JSON
            int keyIndex = json.indexOf("\"" + key + "\"");
            if (keyIndex == -1) return null;
            
            // Find the colon after the key
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return null;
            
            // Skip whitespace after colon
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            
            // Check if value is a string (starts with quote)
            if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
                return null;
            }
            
            // Find the end of the string value, handling escaped quotes
            int valueEnd = valueStart + 1;
            boolean escaped = false;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    // Found the end of the string
                    return json.substring(valueStart + 1, valueEnd).replace("\\\"", "\"").replace("\\\\", "\\");
                }
                valueEnd++;
            }
        } catch (Exception e) {
            // Fallback to simple regex if parsing fails
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }
    
    private static String escapeJSON(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static String extractQueryParam(String query, String param) {
        if (query == null) return null;
        String[] params = query.split("&");
        for (String p : params) {
            String[] keyValue = p.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(param)) {
                try {
                    return java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return keyValue[1];
                }
            }
        }
        return null;
    }
    
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private static void sendJSONResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        sendResponse(exchange, statusCode, json, "application/json");
    }
    
    /**
     * Get the HTML page for mobile redemption
     */
    private static String getRedemptionPageHTML() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Voucher Redemption</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        background: linear-gradient(135deg, #003049 0%, #669bbc 50%, #dce6eb 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        padding: 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 20px;
                        padding: 30px;
                        max-width: 500px;
                        width: 100%;
                        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
                    }
                    h1 {
                        color: #003049;
                        text-align: center;
                        margin-bottom: 10px;
                        font-size: 28px;
                    }
                    .subtitle {
                        text-align: center;
                        color: #666;
                        margin-bottom: 30px;
                        font-size: 14px;
                    }
                    .form-group {
                        margin-bottom: 20px;
                    }
                    label {
                        display: block;
                        color: #003049;
                        font-weight: bold;
                        margin-bottom: 8px;
                        font-size: 14px;
                    }
                    input, select {
                        width: 100%;
                        padding: 12px;
                        border: 2px solid #669bbc;
                        border-radius: 8px;
                        font-size: 16px;
                        transition: border-color 0.3s;
                    }
                    input:focus, select:focus {
                        outline: none;
                        border-color: #003049;
                    }
                    .btn {
                        width: 100%;
                        padding: 14px;
                        background: #003049;
                        color: white;
                        border: none;
                        border-radius: 8px;
                        font-size: 16px;
                        font-weight: bold;
                        cursor: pointer;
                        transition: background 0.3s;
                        margin-top: 10px;
                    }
                    .btn:hover {
                        background: #004d73;
                    }
                    .btn:disabled {
                        background: #ccc;
                        cursor: not-allowed;
                    }
                    .btn-check {
                        background: #669bbc;
                    }
                    .btn-check:hover {
                        background: #5a8ba8;
                    }
                    .message {
                        padding: 12px;
                        border-radius: 8px;
                        margin-top: 15px;
                        text-align: center;
                        font-weight: bold;
                        display: none;
                    }
                    .message.success {
                        background: #d4edda;
                        color: #155724;
                        border: 1px solid #c3e6cb;
                        display: block;
                    }
                    .message.error {
                        background: #f8d7da;
                        color: #721c24;
                        border: 1px solid #f5c6cb;
                        display: block;
                    }
                    .voucher-info {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 8px;
                        margin-top: 15px;
                        display: none;
                    }
                    .voucher-info.show {
                        display: block;
                    }
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 8px;
                    }
                    .info-label {
                        font-weight: bold;
                        color: #003049;
                    }
                    .info-value {
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🎫 Voucher Redemption</h1>
                    <p class="subtitle">Scan QR code or enter voucher code manually</p>
                    
                    <form id="redemptionForm">
                        <div class="form-group">
                            <label for="voucherCode">Voucher Code *</label>
                            <input type="text" id="voucherCode" name="voucherCode" 
                                   placeholder="Enter voucher code manually or scan QR code" required autofocus>
                            <div style="margin-top: 10px; text-align: center;">
                                <button type="button" class="btn btn-check" onclick="toggleScanner()" 
                                        id="scanBtn" style="width: auto; padding: 12px 30px;">
                                    📷 Scan QR Code with Camera
                                </button>
                            </div>
                        </div>
                        
                        <!-- QR Code Scanner -->
                        <div id="scannerContainer" style="display: none; margin-bottom: 20px;">
                            <video id="scannerVideo" style="width: 100%; max-width: 400px; border-radius: 8px; background: #000;"></video>
                            <canvas id="scannerCanvas" style="display: none;"></canvas>
                            <div style="text-align: center; margin-top: 10px;">
                                <button type="button" class="btn btn-check" onclick="stopScanner()" style="background: #dc3545;">
                                    Stop Scanning
                                </button>
                            </div>
                        </div>
                        
                        <button type="button" class="btn btn-check" onclick="checkVoucher()">Check Voucher</button>
                        
                        <div id="voucherInfo" class="voucher-info">
                            <div class="info-row">
                                <span class="info-label">Status:</span>
                                <span class="info-value" id="voucherStatus">-</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Value:</span>
                                <span class="info-value" id="voucherValue">-</span>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label for="branch">Company *</label>
                            <input type="text" id="branch" name="branch" 
                                   placeholder="Enter company name" required>
                        </div>
                        
                        <div class="form-group">
                            <label for="redeemedBy">Redeemed By *</label>
                            <input type="text" id="redeemedBy" name="redeemedBy" 
                                   placeholder="Enter your name" required>
                        </div>
                        
                        <button type="submit" class="btn" id="redeemBtn">Redeem Voucher</button>
                    </form>
                    
                    <div id="message" class="message"></div>
                </div>
                
                <script src="https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.min.js"></script>
                <script>
                    const form = document.getElementById('redemptionForm');
                    const messageDiv = document.getElementById('message');
                    const voucherInfo = document.getElementById('voucherInfo');
                    let scannerStream = null;
                    let scanning = false;
                    
                    // Auto-fill voucher code from URL parameter (if scanned)
                    const urlParams = new URLSearchParams(window.location.search);
                    const code = urlParams.get('code');
                    if (code) {
                        document.getElementById('voucherCode').value = code;
                        checkVoucher();
                    }
                    
                    // QR Code Scanner Functions
                    async function toggleScanner() {
                        if (scanning) {
                            stopScanner();
                        } else {
                            startScanner();
                        }
                    }
                    
                    async function startScanner() {
                        const scannerContainer = document.getElementById('scannerContainer');
                        const scannerVideo = document.getElementById('scannerVideo');
                        const scannerCanvas = document.getElementById('scannerCanvas');
                        const scanBtn = document.getElementById('scanBtn');
                        
                        try {
                            // Request camera access
                            scannerStream = await navigator.mediaDevices.getUserMedia({
                                video: { 
                                    facingMode: 'environment', // Use back camera on mobile
                                    width: { ideal: 1280 },
                                    height: { ideal: 720 }
                                }
                            });
                            
                            scannerVideo.srcObject = scannerStream;
                            scannerVideo.setAttribute('playsinline', 'true'); // Important for iOS
                            await scannerVideo.play();
                            
                            scannerContainer.style.display = 'block';
                            scanBtn.textContent = '📷 Scanning...';
                            scanning = true;
                            
                            // Start scanning loop
                            scanQRCode();
                            
                        } catch (error) {
                            console.error('Error accessing camera:', error);
                            showMessage('Unable to access camera. Please allow camera permissions and try again.', 'error');
                            if (error.name === 'NotAllowedError') {
                                showMessage('Camera permission denied. Please allow camera access in your browser settings.', 'error');
                            } else if (error.name === 'NotFoundError') {
                                showMessage('No camera found. Please use a device with a camera.', 'error');
                            }
                        }
                    }
                    
                    function stopScanner() {
                        scanning = false;
                        if (scannerStream) {
                            scannerStream.getTracks().forEach(track => track.stop());
                            scannerStream = null;
                        }
                        const scannerContainer = document.getElementById('scannerContainer');
                        const scannerVideo = document.getElementById('scannerVideo');
                        const scanBtn = document.getElementById('scanBtn');
                        
                        scannerVideo.srcObject = null;
                        scannerContainer.style.display = 'none';
                        scanBtn.textContent = '📷 Scan';
                    }
                    
                    function scanQRCode() {
                        const scannerVideo = document.getElementById('scannerVideo');
                        const scannerCanvas = document.getElementById('scannerCanvas');
                        const context = scannerCanvas.getContext('2d');
                        
                        if (!scanning || !scannerVideo.readyState || scannerVideo.readyState !== scannerVideo.HAVE_ENOUGH_DATA) {
                            if (scanning) {
                                requestAnimationFrame(scanQRCode);
                            }
                            return;
                        }
                        
                        // Set canvas dimensions to match video
                        scannerCanvas.width = scannerVideo.videoWidth;
                        scannerCanvas.height = scannerVideo.videoHeight;
                        
                        // Draw video frame to canvas
                        context.drawImage(scannerVideo, 0, 0, scannerCanvas.width, scannerCanvas.height);
                        
                        // Get image data
                        const imageData = context.getImageData(0, 0, scannerCanvas.width, scannerCanvas.height);
                        
                        // Scan for QR code
                        if (typeof jsQR !== 'undefined') {
                            const code = jsQR(imageData.data, imageData.width, imageData.height);
                            
                            if (code) {
                                // QR code found!
                                const voucherCode = extractVoucherCodeFromURL(code.data);
                                if (voucherCode) {
                                    document.getElementById('voucherCode').value = voucherCode;
                                    stopScanner();
                                    checkVoucher();
                                    showMessage('QR code scanned successfully!', 'success');
                                } else {
                                    // Try using the raw data as voucher code
                                    document.getElementById('voucherCode').value = code.data;
                                    stopScanner();
                                    checkVoucher();
                                    showMessage('QR code scanned successfully!', 'success');
                                }
                                return;
                            }
                        } else {
                            // Fallback: Try to extract voucher code from URL if jsQR not loaded
                            console.warn('jsQR library not loaded, using fallback method');
                        }
                        
                        // Continue scanning
                        if (scanning) {
                            requestAnimationFrame(scanQRCode);
                        }
                    }
                    
                    function extractVoucherCodeFromURL(url) {
                        // Extract voucher code from URL like: http://192.168.1.1:8080?code=VCHR000001
                        // or https://domain.com/redeem?code=VCHR000001
                        try {
                            const urlObj = new URL(url);
                            const code = urlObj.searchParams.get('code');
                            if (code) {
                                return code;
                            }
                            // If no code parameter, check if URL ends with voucher code
                            const pathParts = urlObj.pathname.split('/');
                            const lastPart = pathParts[pathParts.length - 1];
                            if (lastPart && lastPart.match(/^[A-Z0-9-]+$/)) {
                                return lastPart;
                            }
                        } catch (e) {
                            // If URL parsing fails, try regex
                            const match = url.match(/[?&]code=([A-Z0-9-]+)/i);
                            if (match) {
                                return match[1];
                            }
                            // Try to find voucher code pattern in URL
                            const codeMatch = url.match(/(VCHR|VR)[0-9-]+/i);
                            if (codeMatch) {
                                return codeMatch[0];
                            }
                        }
                        return null;
                    }
                    
                    async function checkVoucher() {
                        const voucherCode = document.getElementById('voucherCode').value.trim();
                        if (!voucherCode) {
                            showMessage('Please enter a voucher code', 'error');
                            return;
                        }
                        
                        try {
                            const response = await fetch('/check?code=' + encodeURIComponent(voucherCode));
                            const data = await response.json();
                            
                            if (data.success) {
                                document.getElementById('voucherStatus').textContent = data.status || 'Active';
                                document.getElementById('voucherValue').textContent = data.value ? 'Rs ' + data.value.toFixed(2) : '-';
                                voucherInfo.classList.add('show');
                                
                                if (data.redeemed) {
                                    showMessage('This voucher has already been redeemed', 'error');
                                    document.getElementById('redeemBtn').disabled = true;
                                } else {
                                    showMessage('Voucher is valid and ready for redemption', 'success');
                                    document.getElementById('redeemBtn').disabled = false;
                                }
                            } else {
                                showMessage(data.message || 'Voucher not found', 'error');
                                voucherInfo.classList.remove('show');
                                document.getElementById('redeemBtn').disabled = true;
                            }
                        } catch (error) {
                            showMessage('Error checking voucher: ' + error.message, 'error');
                        }
                    }
                    
                    form.addEventListener('submit', async (e) => {
                        e.preventDefault();
                        
                        const voucherCode = document.getElementById('voucherCode').value.trim();
                        const branch = document.getElementById('branch').value.trim();
                        const redeemedBy = document.getElementById('redeemedBy').value.trim();
                        
                        if (!voucherCode || !branch || !redeemedBy) {
                            showMessage('Please fill in all fields', 'error');
                            return;
                        }
                        
                        document.getElementById('redeemBtn').disabled = true;
                        document.getElementById('redeemBtn').textContent = 'Processing...';
                        
                        try {
                            const response = await fetch('/redeem', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify({
                                    voucherCode: voucherCode,
                                    branch: branch,
                                    redeemedBy: redeemedBy
                                })
                            });
                            
                            const data = await response.json();
                            
                            if (data.success) {
                                showMessage(data.message, 'success');
                                form.reset();
                                voucherInfo.classList.remove('show');
                            } else {
                                showMessage(data.message, 'error');
                            }
                        } catch (error) {
                            showMessage('Error redeeming voucher: ' + error.message, 'error');
                        } finally {
                            document.getElementById('redeemBtn').disabled = false;
                            document.getElementById('redeemBtn').textContent = 'Redeem Voucher';
                        }
                    });
                    
                    function showMessage(text, type) {
                        messageDiv.textContent = text;
                        messageDiv.className = 'message ' + type;
                        setTimeout(() => {
                            messageDiv.className = 'message';
                        }, 5000);
                    }
                </script>
            </body>
            </html>
            """;
    }
    
    // Helper classes
    static class RedemptionResult {
        boolean success;
        String message;
        
        RedemptionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
    
    static class VoucherInfo {
        String status;
        boolean redeemed;
        Date expiryDate;
        Integer requestId;
    }
    
    static class VoucherStatus {
        boolean found;
        String message;
        String status;
        boolean redeemed;
        Double value;
        
        VoucherStatus(boolean found, String message, String status, boolean redeemed, Double value) {
            this.found = found;
            this.message = message;
            this.status = status;
            this.redeemed = redeemed;
            this.value = value;
        }
        
        String toJSON() {
            return String.format(
                "{\"success\":%s,\"message\":\"%s\",\"status\":%s,\"redeemed\":%s,\"value\":%s}",
                found,
                message != null ? escapeJSON(message) : "",
                status != null ? "\"" + escapeJSON(status) + "\"" : "null",
                redeemed,
                value != null ? value : "null"
            );
        }
    }
}

