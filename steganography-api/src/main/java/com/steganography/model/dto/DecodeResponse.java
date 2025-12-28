package com.steganography.model.dto;

public record DecodeResponse(
        String message,
        int messageLength,
        int bitsPerChannelUsed
) {}
