package com.steganography.core;

import com.steganography.exception.InsufficientCapacityException;

import java.awt.image.BufferedImage;

public class LsbEncoder {

    private static final int HEADER_SIZE_BITS = 32;

    /**
     * Encodes a message into an image using LSB steganography.
     *
     * @param image          the cover image
     * @param message        the message to hide
     * @param bitsPerChannel number of LSBs to use per color channel (1-8)
     * @return new image with embedded message
     */
    public BufferedImage encode(BufferedImage image, String message, int bitsPerChannel) {
        validateBitsPerChannel(bitsPerChannel);

        byte[] messageBytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int messageLength = messageBytes.length;

        long requiredBits = HEADER_SIZE_BITS + ((long) messageLength * 8);
        long availableBits = calculateCapacityBits(image, bitsPerChannel);

        if (requiredBits > availableBits) {
            throw new InsufficientCapacityException(
                    String.format("Message requires %d bits but image can only hold %d bits with %d bits per channel",
                            requiredBits, availableBits, bitsPerChannel));
        }

        BufferedImage stegoImage = copyImage(image);

        int[] headerBits = BitManipulator.intToBits(messageLength, HEADER_SIZE_BITS);
        int[] messageBits = BitManipulator.stringToBits(message);

        int[] allBits = new int[headerBits.length + messageBits.length];
        System.arraycopy(headerBits, 0, allBits, 0, headerBits.length);
        System.arraycopy(messageBits, 0, allBits, headerBits.length, messageBits.length);

        embedBits(stegoImage, allBits, bitsPerChannel);

        return stegoImage;
    }

    private void embedBits(BufferedImage image, int[] bits, int bitsPerChannel) {
        int width = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= bits.length) {
                    break outer;
                }

                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                int[] channels = {red, green, blue};

                for (int c = 0; c < 3 && bitIndex < bits.length; c++) {
                    int bitsToEmbed = 0;
                    int bitsEmbedded = 0;

                    for (int b = 0; b < bitsPerChannel && bitIndex < bits.length; b++) {
                        bitsToEmbed = (bitsToEmbed << 1) | bits[bitIndex];
                        bitIndex++;
                        bitsEmbedded++;
                    }

                    if (bitsEmbedded < bitsPerChannel) {
                        bitsToEmbed <<= (bitsPerChannel - bitsEmbedded);
                    }

                    channels[c] = BitManipulator.replaceLSBs(channels[c], bitsToEmbed, bitsPerChannel);
                }

                int newPixel = (alpha << 24) | (channels[0] << 16) | (channels[1] << 8) | channels[2];
                image.setRGB(x, y, newPixel);
            }
        }
    }

    public long calculateCapacityBits(BufferedImage image, int bitsPerChannel) {
        return (long) image.getWidth() * image.getHeight() * 3 * bitsPerChannel;
    }


    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }

    private void validateBitsPerChannel(int bitsPerChannel) {
        if (bitsPerChannel < 1 || bitsPerChannel > 8) {
            throw new IllegalArgumentException("Bits per channel must be between 1 and 8");
        }
    }
}
