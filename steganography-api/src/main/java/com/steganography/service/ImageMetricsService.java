package com.steganography.service;

import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class ImageMetricsService {

    /**
     * Calculates Mean Square Error between two images.
     * Lower MSE = less distortion = better quality.
     *
     * @param original original image
     * @param modified modified (stego) image
     * @return MSE value
     */
    public double calculateMSE(BufferedImage original, BufferedImage modified) {
        validateImageDimensions(original, modified);

        int width = original.getWidth();
        int height = original.getHeight();
        long sumSquaredError = 0;
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int origPixel = original.getRGB(x, y);
                int modPixel = modified.getRGB(x, y);

                int origRed = (origPixel >> 16) & 0xFF;
                int origGreen = (origPixel >> 8) & 0xFF;
                int origBlue = origPixel & 0xFF;

                int modRed = (modPixel >> 16) & 0xFF;
                int modGreen = (modPixel >> 8) & 0xFF;
                int modBlue = modPixel & 0xFF;

                sumSquaredError += Math.pow(origRed - modRed, 2);
                sumSquaredError += Math.pow(origGreen - modGreen, 2);
                sumSquaredError += Math.pow(origBlue - modBlue, 2);
            }
        }

        return (double) sumSquaredError / (totalPixels * 3);
    }

    /**
     * Calculates Peak Signal-to-Noise Ratio between two images.
     * Higher PSNR = better quality.
     * - PSNR > 40 dB: Excellent quality, differences imperceptible
     * - PSNR 30-40 dB: Good quality, minor differences
     * - PSNR 20-30 dB: Acceptable quality, visible differences
     * - PSNR < 20 dB: Poor quality, significant distortion
     *
     * @param original original image
     * @param modified modified (stego) image
     * @return PSNR value in decibels (dB)
     */
    public double calculatePSNR(BufferedImage original, BufferedImage modified) {
        double mse = calculateMSE(original, modified);

        if (mse == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double maxPixelValue = 255.0;
        return 10 * Math.log10((maxPixelValue * maxPixelValue) / mse);
    }

    /**
     * Gets quality assessment based on PSNR value.
     *
     * @param psnr PSNR value
     * @return quality description
     */
    public String getQualityAssessment(double psnr) {
        if (Double.isInfinite(psnr)) {
            return "PERFECT";
        } else if (psnr >= 50) {
            return "EXCELLENT";
        } else if (psnr >= 40) {
            return "VERY GOOD";
        } else if (psnr >= 30) {
            return "GOOD";
        } else if (psnr >= 20) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    private void validateImageDimensions(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException(
                    String.format("Image dimensions must match. Original: %dx%d, Modified: %dx%d",
                            img1.getWidth(), img1.getHeight(), img2.getWidth(), img2.getHeight()));
        }
    }
}
