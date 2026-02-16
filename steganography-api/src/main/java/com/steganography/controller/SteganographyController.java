package com.steganography.controller;

import com.steganography.model.dto.AnalysisResponse;
import com.steganography.model.dto.CapacityResponse;
import com.steganography.model.dto.DecodeResponse;
import com.steganography.service.SteganographyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class SteganographyController {

    private final SteganographyService steganographyService;

    public SteganographyController(SteganographyService steganographyService) {
        this.steganographyService = steganographyService;
    }

    /**
     * Encode a message into an image.
     *
     * @param image          image
     * @param message        message
     * @param bitsPerChannel number of LSBs to use per channel (1-8)
     * @return      image
     */
    @PostMapping("/encode")
    public ResponseEntity<byte[]> encode(
            @RequestParam("image") MultipartFile image,
            @RequestParam("message") String message,
            @RequestParam(value = "bitsPerChannel", defaultValue = "1") int bitsPerChannel) {

        byte[] stegoImage = steganographyService.encode(image, message, bitsPerChannel);

        String filename = generateFilename(image.getOriginalFilename(), bitsPerChannel);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(stegoImage.length)
                .body(stegoImage);
    }

    /**
     * Decode a hidden message from an image.
     *
     * @param image          image
     * @param bitsPerChannel number of LSBs that were used (1-8)
     * @return extracted message
     */
    @PostMapping("/decode")
    public ResponseEntity<DecodeResponse> decode(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "bitsPerChannel", defaultValue = "1") int bitsPerChannel) {

        DecodeResponse response = steganographyService.decode(image, bitsPerChannel);
        return ResponseEntity.ok(response);
    }

    /**
     * Analyze quality metrics between original and stego images.
     *
     * @param original original image
     * @param stego    image with hidden message
     * @return PSNR, MSE, and quality assessment
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestParam("original") MultipartFile original,
            @RequestParam("stego") MultipartFile stego) {

        AnalysisResponse response = steganographyService.analyze(original, stego);
        return ResponseEntity.ok(response);
    }

    /**
     * Calculate the capacity of an image for different bits per channel settings.
     *
     * @param width          image width
     * @param height         image height
     * @param bitsPerChannel number of LSBs to use (1-8)
     * @return      capacity information
     */
    @GetMapping("/capacity")
    public ResponseEntity<CapacityResponse> capacity(
            @RequestParam("width") int width,
            @RequestParam("height") int height,
            @RequestParam(value = "bitsPerChannel", defaultValue = "1") int bitsPerChannel) {

        CapacityResponse response = steganographyService.calculateCapacity(width, height, bitsPerChannel);
        return ResponseEntity.ok(response);
    }

    private String generateFilename(String originalFilename, int bitsPerChannel) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "stego_image_" + bitsPerChannel + "bpc.png";
        }

        String baseName = originalFilename.replaceFirst("[.][^.]+$", "");
        return baseName + "_stego_" + bitsPerChannel + "bpc.png";
    }
}
