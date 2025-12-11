package pkg.vms.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * QR Code generation utility
 */
public class QRCodeGenerator {
    
    private static final int DEFAULT_SIZE = 200;
    
    /**
     * Generate QR code as BufferedImage
     */
    public static BufferedImage generateQRCodeImage(String data, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints);
        
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        
        return image;
    }
    
    /**
     * Generate QR code as JavaFX Image
     */
    public static Image generateQRCodeFX(String data, int size) {
        try {
            BufferedImage bufferedImage = generateQRCodeImage(data, size);
            return convertToFXImage(bufferedImage);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Convert BufferedImage to JavaFX Image without Swing interop
     */
    private static Image convertToFXImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                javafx.scene.paint.Color color = javafx.scene.paint.Color.rgb(r, g, b, a / 255.0);
                pixelWriter.setColor(x, y, color);
            }
        }
        
        return writableImage;
    }
    
    /**
     * Generate QR code as byte array (for PDF embedding)
     */
    public static byte[] generateQRCodeBytes(String data, int size) throws WriterException, IOException {
        BufferedImage image = generateQRCodeImage(data, size);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
    
    /**
     * Generate QR code with default size
     */
    public static BufferedImage generateQRCodeImage(String data) throws WriterException {
        return generateQRCodeImage(data, DEFAULT_SIZE);
    }
}

