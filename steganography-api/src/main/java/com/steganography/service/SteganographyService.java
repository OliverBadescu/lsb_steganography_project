package com.steganography.service;

import com.steganography.core.LsbDecoder;
import com.steganography.core.LsbEncoder;
import com.steganography.exception.SteganographyException;
import com.steganography.model.dto.AnalysisResponse;
import com.steganography.model.dto.CapacityResponse;
import com.steganography.model.dto.DecodeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class SteganographyService {

    private final LsbEncoder encoder;
    private final LsbDecoder decoder;
    private final ImageMetricsService metricsService;

    public SteganographyService(ImageMetricsService metricsService) {
        this.encoder = new LsbEncoder();
        this.decoder = new LsbDecoder();
        this.metricsService = metricsService;
    }

    /**
     * Encodes a message into an image.
     *
     * @param imageFile      the cover image file
     * @param message        the message to hide
     * @param bitsPerChannel number of LSBs to use (1-8)
     * @return stego image as byte array (PNG format)
     */
    public byte[] encode(MultipartFile imageFile, String message, int bitsPerChannel) {
        try {
            BufferedImage coverImage = ImageIO.read(imageFile.getInputStream());
            if (coverImage == null) {
                throw new SteganographyException("Failed to read. Ensure it's a valid PNG image.");
            }

            BufferedImage stegoImage = encoder.encode(coverImage, message, bitsPerChannel);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(stegoImage, "PNG", outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new SteganographyException("Failed to process image: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a hidden message from a stego image.
     *
     * @param imageFile      the stego image file
     * @param bitsPerChannel number of LSBs that were used (1-8)
     * @return decoded message response
     */
    public DecodeResponse decode(MultipartFile imageFile, int bitsPerChannel) {
        try {
            BufferedImage stegoImage = ImageIO.read(imageFile.getInputStream());
            if (stegoImage == null) {
                throw new SteganographyException("Failed to read. Ensure it's a valid PNG image.");
            }

            String message = decoder.decode(stegoImage, bitsPerChannel);

            return new DecodeResponse(message, message.length(), bitsPerChannel);

        } catch (IOException e) {
            throw new SteganographyException("Failed to process image: " + e.getMessage(), e);
        }
    }

    /**
     * Analyzes two images and returns quality metrics.
     *
     * @param originalFile original cover image
     * @param stegoFile    stego image with hidden message
     * @return analysis with PSNR, MSE metrics
     */
    public AnalysisResponse analyze(MultipartFile originalFile, MultipartFile stegoFile) {
        try {
            BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());
            BufferedImage stegoImage = ImageIO.read(stegoFile.getInputStream());

            if (originalImage == null || stegoImage == null) {
                throw new SteganographyException("Failed to read one or both image files.");
            }

            double mse = metricsService.calculateMSE(originalImage, stegoImage);
            double psnr = metricsService.calculatePSNR(originalImage, stegoImage);
            String quality = metricsService.getQualityAssessment(psnr);

            return new AnalysisResponse(mse, psnr, quality, originalImage.getWidth(), originalImage.getHeight());

        } catch (IOException e) {
            throw new SteganographyException("Failed to process images: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the capacity of an image for different bits per channel settings.
     *
     * @param width          image width
     * @param height         image height
     * @param bitsPerChannel bits per channel (1-8)
     * @return capacity information
     */
    public CapacityResponse calculateCapacity(int width, int height, int bitsPerChannel) {
        if (bitsPerChannel < 1 || bitsPerChannel > 8) {
            throw new IllegalArgumentException("Bits per channel must be between 1 and 8");
        }

        long totalBits = (long) width * height * 3 * bitsPerChannel;
        long headerBits = 32;
        long availableBits = totalBits - headerBits;
        long capacityBytes = availableBits / 8;

        return new CapacityResponse(width, height, bitsPerChannel, capacityBytes, capacityBytes / 1024.0, (long) width * height);
    }
}
