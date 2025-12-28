package com.steganography.core;

import com.steganography.exception.SteganographyException;

import java.awt.image.BufferedImage;

public class LsbDecoder {

    private static final int HEADER_SIZE_BITS = 32;

    /**
     * Decodes a hidden message from a stego image.
     *
     * @param image          the image containing hidden message
     * @param bitsPerChannel number of LSBs used per color channel (1-8)
     * @return the extracted message
     */
    public String decode(BufferedImage image, int bitsPerChannel) {
        validateBitsPerChannel(bitsPerChannel);

        int[] headerBits = extractBits(image, HEADER_SIZE_BITS, 0, bitsPerChannel);
        int messageLength = BitManipulator.bitsToInt(headerBits);

        if (messageLength < 0) {
            throw new SteganographyException("No hidden message found or invalid message length");
        }

        if (messageLength == 0) {
            return "";
        }

        long maxCapacity = calculateMaxMessageBytes(image, bitsPerChannel);
        if (messageLength > maxCapacity) {
            throw new SteganographyException(
                    String.format("Invalid message length %d exceeds maximum capacity %d. " +
                            "The image may not contain a hidden message or wrong bits per channel was specified.",
                            messageLength, maxCapacity));
        }

        int messageBitCount = messageLength * 8;
        int[] messageBits = extractBits(image, messageBitCount, HEADER_SIZE_BITS, bitsPerChannel);

        return BitManipulator.bitsToString(messageBits);
    }

    private int[] extractBits(BufferedImage image, int bitCount, int startBit, int bitsPerChannel) {
        int[] bits = new int[bitCount];
        int width = image.getWidth();
        int height = image.getHeight();

        int totalBitsPerPixel = 3 * bitsPerChannel;
        int startPixel = startBit / totalBitsPerPixel;
        int startBitInPixel = startBit % totalBitsPerPixel;

        int bitIndex = 0;
        int currentBitInPixel = startBitInPixel;

        int pixelX = startPixel % width;
        int pixelY = startPixel / width;

        while (bitIndex < bitCount && pixelY < height) {
            int pixel = image.getRGB(pixelX, pixelY);
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;

            int[] channels = {red, green, blue};

            while (currentBitInPixel < totalBitsPerPixel && bitIndex < bitCount) {
                int channelIndex = currentBitInPixel / bitsPerChannel;
                int bitInChannel = currentBitInPixel % bitsPerChannel;

                int channelValue = channels[channelIndex];
                int extractedBits = BitManipulator.extractLSBs(channelValue, bitsPerChannel);

                int bitPosition = bitsPerChannel - 1 - bitInChannel;
                bits[bitIndex] = BitManipulator.getBit(extractedBits, bitPosition);

                bitIndex++;
                currentBitInPixel++;
            }

            currentBitInPixel = 0;
            pixelX++;
            if (pixelX >= width) {
                pixelX = 0;
                pixelY++;
            }
        }

        return bits;
    }

    private long calculateMaxMessageBytes(BufferedImage image, int bitsPerChannel) {
        long totalBits = (long) image.getWidth() * image.getHeight() * 3 * bitsPerChannel;
        long availableBits = totalBits - HEADER_SIZE_BITS;
        return availableBits / 8;
    }

    private void validateBitsPerChannel(int bitsPerChannel) {
        if (bitsPerChannel < 1 || bitsPerChannel > 8) {
            throw new IllegalArgumentException("Bits per channel must be between 1 and 8");
        }
    }
}
