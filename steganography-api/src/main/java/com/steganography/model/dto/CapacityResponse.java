package com.steganography.model.dto;

public record CapacityResponse(
        int width,
        int height,
        int bitsPerChannel,
        long capacityBytes,
        double capacityKilobytes,
        long totalPixels
) {}
