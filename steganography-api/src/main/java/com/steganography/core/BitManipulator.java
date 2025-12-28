package com.steganography.core;

public class BitManipulator {

    /**
     * Extracts a specific bit from a byte.
     *
     * @param value    the byte value
     * @param position bit position (0 = LSB, 7 = MSB)
     * @return 0 or 1
     */
    public static int getBit(int value, int position) {
        return (value >> position) & 1;
    }


    /**
     * Replaces the k least significant bits of a value with bits from data.
     *
     * @param original      the original byte value (0-255)
     * @param dataBits      the bits to embed
     * @param bitsToReplace number of LSBs to replace (1-8)
     * @return modified byte value
     */
    public static int replaceLSBs(int original, int dataBits, int bitsToReplace) {
        int mask = 0xFF << bitsToReplace;
        int cleared = original & mask;
        int dataToEmbed = dataBits & ((1 << bitsToReplace) - 1);
        return cleared | dataToEmbed;
    }

    /**
     * Extracts the k least significant bits from a value.
     *
     * @param value         the byte value
     * @param bitsToExtract number of LSBs to extract (1-8)
     * @return extracted bits
     */
    public static int extractLSBs(int value, int bitsToExtract) {
        return value & ((1 << bitsToExtract) - 1);
    }

    /**
     * Converts a string to a bit array.
     *
     * @param message the message to convert
     * @return array of bits (0s and 1s)
     */
    public static int[] stringToBits(String message) {
        byte[] bytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] bits = new int[bytes.length * 8];

        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = getBit(bytes[i] & 0xFF, 7 - j);
            }
        }
        return bits;
    }

    /**
     * Converts a bit array back to a string.
     *
     * @param bits array of bits
     * @return reconstructed string
     */
    public static String bitsToString(int[] bits) {
        if (bits.length % 8 != 0) {
            throw new IllegalArgumentException("Bit array length must be a multiple of 8");
        }

        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            int byteValue = 0;
            for (int j = 0; j < 8; j++) {
                byteValue = (byteValue << 1) | bits[i * 8 + j];
            }
            bytes[i] = (byte) byteValue;
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Converts an integer to a fixed-size bit array.
     *
     * @param value   the integer value
     * @param numBits number of bits to use
     * @return bit array
     */
    public static int[] intToBits(int value, int numBits) {
        int[] bits = new int[numBits];
        for (int i = 0; i < numBits; i++) {
            bits[numBits - 1 - i] = getBit(value, i);
        }
        return bits;
    }

    /**
     * Converts a bit array to an integer.
     *
     * @param bits the bit array
     * @return integer value
     */
    public static int bitsToInt(int[] bits) {
        int value = 0;
        for (int bit : bits) {
            value = (value << 1) | bit;
        }
        return value;
    }
}
