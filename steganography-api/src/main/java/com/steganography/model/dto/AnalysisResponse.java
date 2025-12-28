package com.steganography.model.dto;

public record AnalysisResponse(
        double mse,
        double psnr,
        String qualityAssessment,
        int originalWidth,
        int originalHeight
) {}
