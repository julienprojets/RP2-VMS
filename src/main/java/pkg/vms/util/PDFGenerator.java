package pkg.vms.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * PDF generation utility for vouchers
 */
public class PDFGenerator {
    
    /**
     * Generate a single voucher PDF with QR code
     */
    public static String generateVoucherPDF(String voucherCode, String clientName, double value, 
                                             String expiryDate, String outputDir) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font helveticaOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
                
                // Title
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 24);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("VOUCHER");
                contentStream.endText();
                
                // Voucher Code
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 18);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Code: " + voucherCode);
                contentStream.endText();
                
                // Client Name
                contentStream.beginText();
                contentStream.setFont(helvetica, 14);
                contentStream.newLineAtOffset(50, 670);
                contentStream.showText("Client: " + clientName);
                contentStream.endText();
                
                // Value
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 20);
                contentStream.newLineAtOffset(50, 640);
                contentStream.showText("Value: Rs " + String.format("%.2f", value));
                contentStream.endText();
                
                // Expiry Date
                contentStream.beginText();
                contentStream.setFont(helvetica, 12);
                contentStream.newLineAtOffset(50, 610);
                contentStream.showText("Valid until: " + expiryDate);
                contentStream.endText();
                
                // QR Code - Contains URL for mobile redemption
                try {
                    // Get local IP for QR code URL
                    String localIP = getLocalIP();
                    String redemptionURL = "http://" + localIP + ":8080?code=" + voucherCode;
                    
                    byte[] qrBytes = QRCodeGenerator.generateQRCodeBytes(redemptionURL, 150);
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrBytes, "qr");
                    contentStream.drawImage(pdImage, 400, 600, 150, 150);
                } catch (Exception e) {
                    System.err.println("Error generating QR code: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Terms and Conditions
                contentStream.beginText();
                contentStream.setFont(helveticaOblique, 10);
                contentStream.newLineAtOffset(50, 500);
                contentStream.showText("Terms: Single use only. Cannot be partially redeemed. Valid at participating stores.");
                contentStream.endText();
                
                // Signature line
                contentStream.beginText();
                contentStream.setFont(helvetica, 12);
                contentStream.newLineAtOffset(50, 200);
                contentStream.showText("Authorized Signature: _________________________");
                contentStream.endText();
            }
            
            // Save PDF
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            String fileName = "Voucher_" + voucherCode + ".pdf";
            File pdfFile = new File(outputDir, fileName);
            document.save(pdfFile);
            
            return pdfFile.getAbsolutePath();
        }
    }
    
    /**
     * Generate summary PDF for admin
     */
    public static String generateSummaryPDF(String requestReference, String clientName, 
                                            int voucherCount, double totalValue, String outputDir) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font helveticaBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                
                // Title
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 20);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Voucher Request Summary");
                contentStream.endText();
                
                // Request Reference
                contentStream.beginText();
                contentStream.setFont(helvetica, 14);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Request Reference: " + requestReference);
                contentStream.endText();
                
                // Client Name
                contentStream.beginText();
                contentStream.setFont(helvetica, 14);
                contentStream.newLineAtOffset(50, 670);
                contentStream.showText("Client: " + clientName);
                contentStream.endText();
                
                // Voucher Count
                contentStream.beginText();
                contentStream.setFont(helvetica, 14);
                contentStream.newLineAtOffset(50, 640);
                contentStream.showText("Number of Vouchers: " + voucherCount);
                contentStream.endText();
                
                // Total Value
                contentStream.beginText();
                contentStream.setFont(helveticaBold, 16);
                contentStream.newLineAtOffset(50, 610);
                contentStream.showText("Total Value: Rs " + String.format("%.2f", totalValue));
                contentStream.endText();
                
                // Date
                contentStream.beginText();
                contentStream.setFont(helvetica, 12);
                contentStream.newLineAtOffset(50, 580);
                contentStream.showText("Generated on: " + java.time.LocalDate.now());
                contentStream.endText();
            }
            
            // Save PDF
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            String fileName = "Summary_" + requestReference + ".pdf";
            File pdfFile = new File(outputDir, fileName);
            document.save(pdfFile);
            
            return pdfFile.getAbsolutePath();
        }
    }
    
    /**
     * Get local IP address for QR code URL
     */
    private static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}

