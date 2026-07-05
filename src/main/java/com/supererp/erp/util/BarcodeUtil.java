package com.supererp.erp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility to generate QR codes and barcodes as Base64 PNG images.
 * Used for warehouse location labels and batch lot labels.
 */
@Component
@Slf4j
public class BarcodeUtil {

    /**
     * Generate a QR code as Base64 PNG string.
     *
     * @param content The text to encode
     * @param width   Width in pixels (default 200)
     * @param height  Height in pixels (default 200)
     * @return data: URI string  e.g. "data:image/png;base64,..."
     */
    public String generateQrCodeBase64(String content, int width, int height) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for content: {}", content, e);
            return "";
        }
    }

    /**
     * Generate a CODE_128 barcode as Base64 PNG string.
     *
     * @param content The text to encode
     * @param width   Width in pixels (default 300)
     * @param height  Height in pixels (default 80)
     * @return data: URI string
     */
    public String generateBarcodeBase64(String content, int width, int height) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.CODE_128, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            log.error("Failed to generate barcode for content: {}", content, e);
            return "";
        }
    }

    /** Convenience overloads with default sizes */
    public String generateQrCodeBase64(String content) {
        return generateQrCodeBase64(content, 200, 200);
    }

    public String generateBarcodeBase64(String content) {
        return generateBarcodeBase64(content, 300, 80);
    }
}
