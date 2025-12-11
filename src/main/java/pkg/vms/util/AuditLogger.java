package pkg.vms.util;

import pkg.vms.DBconnection.DBconnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

/**
 * Audit trail logging utility
 */
public class AuditLogger {
    
    /**
     * Log an action to the audit trail
     */
    public static void logAction(String actionType, String entityType, String entityId,
                                 String userName, String description, String oldValue, 
                                 String newValue, String context) {
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO audit_trail(action_type, entity_type, entity_id, user_name, " +
                 "action_description, old_value, new_value, timestamp, context) " +
                 "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            ps.setString(1, actionType);
            ps.setString(2, entityType);
            ps.setString(3, entityId);
            ps.setString(4, userName);
            ps.setString(5, description);
            ps.setString(6, oldValue);
            ps.setString(7, newValue);
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            ps.setString(9, context);
            
            ps.executeUpdate();
            
        } catch (Exception e) {
            System.err.println("Error logging audit trail: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Log voucher request creation
     */
    public static void logRequestCreation(String requestReference, String userName, String clientName, int voucherCount) {
        logAction("CREATE", "VOUCHER_REQUEST", requestReference, userName,
            "Created voucher request for " + clientName + " with " + voucherCount + " vouchers",
            null, requestReference, "Request creation");
    }
    
    /**
     * Log payment update
     */
    public static void logPaymentUpdate(String requestReference, String userName, String oldStatus, String newStatus) {
        logAction("UPDATE", "PAYMENT", requestReference, userName,
            "Payment status changed from " + oldStatus + " to " + newStatus,
            oldStatus, newStatus, "Payment update");
    }
    
    /**
     * Log approval
     */
    public static void logApproval(String requestReference, String userName) {
        logAction("APPROVE", "VOUCHER_REQUEST", requestReference, userName,
            "Voucher request approved", "pending", "approved", "Approval");
    }
    
    /**
     * Log voucher generation
     */
    public static void logVoucherGeneration(String requestReference, String userName, int voucherCount) {
        logAction("GENERATE", "VOUCHERS", requestReference, userName,
            "Generated " + voucherCount + " vouchers", null, String.valueOf(voucherCount), "Voucher generation");
    }
    
    /**
     * Log voucher dispatch
     */
    public static void logVoucherDispatch(String requestReference, String userName, String recipientEmail) {
        logAction("SEND", "VOUCHERS", requestReference, userName,
            "Vouchers sent to " + recipientEmail, null, recipientEmail, "Email dispatch");
    }
    
    /**
     * Log voucher redemption
     */
    public static void logRedemption(String voucherCode, String userName, String branchLocation) {
        logAction("REDEEM", "VOUCHER", voucherCode, userName,
            "Voucher redeemed at " + branchLocation, "active", "redeemed", "Redemption");
    }
}

