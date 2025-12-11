package pkg.vms.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * Email service for sending notifications
 */
public class EmailService {
    
    private static String getSmtpHost() {
        return EmailConfig.getSmtpHost();
    }
    
    private static String getSmtpPort() {
        return EmailConfig.getSmtpPort();
    }
    
    private static String getSmtpUser() {
        return EmailConfig.getSmtpUser();
    }
    
    private static String getSmtpPassword() {
        return EmailConfig.getSmtpPassword();
    }
    
    /**
     * Send email notification
     */
    public static boolean sendEmail(String to, String cc, String subject, String body, String[] attachments) {
        if (to == null || to.trim().isEmpty()) {
            System.err.println("Email recipient is empty. Cannot send email.");
            return false;
        }
        
        String SMTP_USER = getSmtpUser();
        String SMTP_PASSWORD = getSmtpPassword();
        
        if (SMTP_USER == null || SMTP_USER.isEmpty() || SMTP_PASSWORD == null || SMTP_PASSWORD.isEmpty()) {
            System.out.println("========================================");
            System.out.println("EMAIL CONFIGURATION NOT SET");
            System.out.println("========================================");
            System.out.println("To enable email sending:");
            System.out.println("  1. Go to Settings/Configuration in the application");
            System.out.println("  2. Or configure email settings in the Email Configuration dialog");
            System.out.println("");
            System.out.println("Email Details:");
            System.out.println("  To: " + to);
            System.out.println("  Subject: " + subject);
            System.out.println("  Body: " + (body.length() > 100 ? body.substring(0, 100) + "..." : body));
            System.out.println("========================================");
            
            // Log the email details but don't block the workflow
            // The system continues to work normally even without email configuration
            System.out.println("Note: Email not sent (SMTP not configured). Workflow continues normally.");
            // Return true so the workflow continues without blocking
            return true;
        }
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", getSmtpHost());
            props.put("mail.smtp.port", getSmtpPort());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            
            if (cc != null && !cc.isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            }
            
            message.setSubject(subject);
            message.setText(body);
            
            // Add attachments if provided
            if (attachments != null && attachments.length > 0) {
                Multipart multipart = new MimeMultipart();
                
                // Add text body
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(body);
                multipart.addBodyPart(textPart);
                
                // Add attachment files
                for (String attachmentPath : attachments) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachmentPath);
                    multipart.addBodyPart(attachmentPart);
                }
                
                message.setContent(multipart);
            }
            
            Transport.send(message);
            System.out.println("========================================");
            System.out.println("EMAIL SENT SUCCESSFULLY");
            System.out.println("========================================");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("========================================");
            
            // Show success message if JavaFX is available
            try {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Email Sent");
                    alert.setHeaderText("Email sent successfully");
                    alert.setContentText("Email sent to: " + to + "\nSubject: " + subject);
                    alert.showAndWait();
                });
            } catch (Exception ex) {
                // JavaFX not available, just log
            }
            
            return true;
            
        } catch (javax.mail.AuthenticationFailedException e) {
            System.err.println("========================================");
            System.err.println("EMAIL AUTHENTICATION FAILED");
            System.err.println("========================================");
            System.err.println("Please check your SMTP_USER and SMTP_PASSWORD.");
            System.err.println("For Gmail, you may need to use an App Password.");
            System.err.println("Error: " + e.getMessage());
            System.err.println("========================================");
            
            try {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Email Error");
                    alert.setHeaderText("Email authentication failed");
                    alert.setContentText("Please check your email configuration.\n" +
                        "For Gmail, use an App Password instead of your regular password.");
                    alert.showAndWait();
                });
            } catch (Exception ex) {
                // JavaFX not available, just log
            }
            return false;
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("ERROR SENDING EMAIL");
            System.err.println("========================================");
            System.err.println("To: " + to);
            System.err.println("Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            
            try {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Email Error");
                    alert.setHeaderText("Failed to send email");
                    alert.setContentText("Error: " + e.getMessage());
                    alert.showAndWait();
                });
            } catch (Exception ex) {
                // JavaFX not available, just log
            }
            return false;
        }
    }
    
    /**
     * Send payment notification to approver
     */
    public static boolean notifyApprover(String approverEmail, String requestReference, String clientName, double amount) {
        String subject = "Payment Received - Voucher Request " + requestReference;
        String body = String.format(
            "Dear Approver,\n\n" +
            "A payment has been received for voucher request %s.\n\n" +
            "Client: %s\n" +
            "Amount: Rs %.2f\n\n" +
            "Please review and approve the request in the system.\n\n" +
            "Thank you,\n" +
            "VMS System",
            requestReference, clientName, amount
        );
        
        return sendEmail(approverEmail, null, subject, body, null);
    }
    
    /**
     * Send vouchers to client
     */
    public static boolean sendVouchersToClient(String clientEmail, String ccEmails, String requestReference, 
                                                String clientName, int voucherCount, String[] pdfPaths) {
        String subject = "Your Vouchers - Request " + requestReference;
        String body = String.format(
            "Dear %s,\n\n" +
            "Your voucher request %s has been approved and processed.\n\n" +
            "Number of vouchers: %d\n\n" +
            "Please find attached the individual voucher PDFs.\n\n" +
            "Each voucher contains a unique QR code for redemption.\n\n" +
            "Thank you for your business!\n\n" +
            "VMS Team",
            clientName, requestReference, voucherCount
        );
        
        return sendEmail(clientEmail, ccEmails, subject, body, pdfPaths);
    }
}

