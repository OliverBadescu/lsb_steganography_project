# LSB Steganography API

A Spring Boot REST API implementing **Least Significant Bit (LSB) steganography** for hiding secret messages within PNG images. 

## Table of Contents

- [Overview](#overview)
- [How Steganography Works](#how-steganography-works)
- [Algorithm Deep Dive](#algorithm-deep-dive)
  - [Bit Manipulation](#bit-manipulation)
  - [LSB Encoding Process](#lsb-encoding-process)
  - [LSB Decoding Process](#lsb-decoding-process)
  - [Variable Bits Per Channel](#variable-bits-per-channel)
- [Image Quality Metrics](#image-quality-metrics)
  - [Mean Square Error (MSE)](#mean-square-error-mse)
  - [Peak Signal-to-Noise Ratio (PSNR)](#peak-signal-to-noise-ratio-psnr)
- [Project Architecture](#project-architecture)
- [File Structure](#file-structure)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Examples](#examples)

---

## Overview

**Steganography** is the practice of hiding secret information within an ordinary, non-secret medium (the "cover") in such a way that the presence of the hidden information is not detectable. Unlike encryption, which makes data unreadable, steganography conceals the very existence of the secret data.

This API implements **spatial domain steganography** using the **Least Significant Bit (LSB)** technique on PNG images. The LSB method is one of the simplest and most widely used steganographic techniques, offering a good balance between payload capacity and imperceptibility.

### Key Features

- **Encode** secret messages into PNG images
- **Decode** hidden messages from stego images
- **Configurable bit depth** (1-8 bits per channel) for capacity vs. quality trade-offs
- **Image quality analysis** using PSNR and MSE metrics
- **Capacity calculation** for planning message embedding
- **RESTful API** for easy integration

---

## How Steganography Works

### The Concept of LSB Steganography

Digital images are composed of pixels, where each pixel contains color information. In a 24-bit RGB image, each pixel has three color channels (Red, Green, Blue), each represented by 8 bits (values 0-255).

```
Pixel RGB Value: (147, 89, 204)

Binary Representation:
  Red:   10010011  (147)
  Green: 01011001  (89)
  Blue:  11001100  (204)
              ↑
              LSB (Least Significant Bit)
```

The **Least Significant Bit** is the rightmost bit in a binary number. Changing this bit only alters the value by ±1, which is imperceptible to the human eye.

```
Original:  10010011 (147)  →  Modified: 10010010 (146)
                                        Change: -1 (invisible)
```

By systematically replacing the LSBs of pixel color values with message bits, we can embed hidden data without visibly altering the image.

---

## Algorithm Deep Dive

### Bit Manipulation

The foundation of LSB steganography lies in bit-level operations. The `BitManipulator` class (`src/main/java/com/steganography/core/BitManipulator.java`) provides the essential building blocks:

#### Extracting a Single Bit

```java
public static int getBit(int value, int position) {
    return (value >> position) & 1;
}
```

**How it works:**
1. Right-shift the value by `position` places to move the target bit to position 0
2. AND with `1` to isolate that single bit

**Example:**
```
value = 147 = 10010011
position = 4

Step 1: 10010011 >> 4 = 00001001
Step 2: 00001001 & 00000001 = 00000001 = 1
```

#### Replacing LSBs

```java
public static int replaceLSBs(int original, int dataBits, int bitsToReplace) {
    int mask = 0xFF << bitsToReplace;        // Create mask to clear LSBs
    int cleared = original & mask;           // Clear the LSBs
    int dataToEmbed = dataBits & ((1 << bitsToReplace) - 1);  // Extract bits to embed
    return cleared | dataToEmbed;            // Combine
}
```

**Step-by-step example** (replacing 2 LSBs):
```
original = 147 = 10010011
dataBits = 2 = 10
bitsToReplace = 2

Step 1: mask = 0xFF << 2 = 11111100
Step 2: cleared = 10010011 & 11111100 = 10010000
Step 3: dataToEmbed = 10 & 00000011 = 00000010
Step 4: result = 10010000 | 00000010 = 10010010 = 146
```

#### String to Bits Conversion

```java
public static int[] stringToBits(String message) {
    byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
    int[] bits = new int[bytes.length * 8];

    for (int i = 0; i < bytes.length; i++) {
        for (int j = 0; j < 8; j++) {
            bits[i * 8 + j] = getBit(bytes[i] & 0xFF, 7 - j);
        }
    }
    return bits;
}
```

**Example:** Converting "Hi" to bits:
```
'H' = 72 = 01001000
'i' = 105 = 01101001

Result: [0,1,0,0,1,0,0,0, 0,1,1,0,1,0,0,1]
```

---

### LSB Encoding Process

The `LsbEncoder` class (`src/main/java/com/steganography/core/LsbEncoder.java`) implements the message embedding algorithm:

#### Message Format

```
┌─────────────────────────────────────────────────────────┐
│  32-bit Header  │        Message Payload                │
│  (msg length)   │        (UTF-8 encoded)                │
├─────────────────┼───────────────────────────────────────┤
│   4 bytes       │        N bytes                        │
└─────────────────────────────────────────────────────────┘
```

The 32-bit header stores the message length (in bytes), allowing the decoder to know exactly how many bytes to extract.

#### Encoding Algorithm

```java
public BufferedImage encode(BufferedImage image, String message, int bitsPerChannel) {
    // 1. Validate capacity
    byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
    long requiredBits = HEADER_SIZE_BITS + (messageBytes.length * 8);
    long availableBits = calculateCapacityBits(image, bitsPerChannel);

    if (requiredBits > availableBits) {
        throw new InsufficientCapacityException(...);
    }

    // 2. Prepare bit arrays
    int[] headerBits = BitManipulator.intToBits(messageLength, 32);
    int[] messageBits = BitManipulator.stringToBits(message);
    int[] allBits = concatenate(headerBits, messageBits);

    // 3. Embed bits into image
    embedBits(stegoImage, allBits, bitsPerChannel);

    return stegoImage;
}
```

#### Bit Embedding Process

```java
private void embedBits(BufferedImage image, int[] bits, int bitsPerChannel) {
    int bitIndex = 0;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // Extract pixel components
            int pixel = image.getRGB(x, y);
            int alpha = (pixel >> 24) & 0xFF;
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;

            int[] channels = {red, green, blue};

            // Embed bits into each channel
            for (int c = 0; c < 3; c++) {
                // Collect bitsPerChannel bits from message
                int bitsToEmbed = 0;
                for (int b = 0; b < bitsPerChannel; b++) {
                    bitsToEmbed = (bitsToEmbed << 1) | bits[bitIndex++];
                }

                // Replace LSBs in channel
                channels[c] = BitManipulator.replaceLSBs(
                    channels[c], bitsToEmbed, bitsPerChannel
                );
            }

            // Reconstruct and set pixel
            int newPixel = (alpha << 24) | (channels[0] << 16)
                         | (channels[1] << 8) | channels[2];
            image.setRGB(x, y, newPixel);
        }
    }
}
```

**Visual representation of encoding:**

```
Message: "A" = 01000001

Image pixels (simplified, 1 bit per channel):
Before:                          After:
Pixel 0: R=147 G=89  B=204      R=146 G=89  B=204
         ↓     ↓     ↓              ↓     ↓     ↓
         0     1     0          embed 0,1,0

Pixel 1: R=100 G=150 B=75       R=100 G=150 B=74
         ↓     ↓     ↓              ↓     ↓     ↓
         0     0     0          embed 0,0,0

Pixel 2: R=200 G=50  B=125      R=200 G=51  B=125
         ↓     ↓     ↓              ↓     ↓     ↓
         0     1     0          embed 0,1,remaining
```

---

### LSB Decoding Process

The `LsbDecoder` class (`src/main/java/com/steganography/core/LsbDecoder.java`) reverses the encoding process:

#### Decoding Algorithm

```java
public String decode(BufferedImage image, int bitsPerChannel) {
    // 1. Extract 32-bit header to get message length
    int[] headerBits = extractBits(image, 32, 0, bitsPerChannel);
    int messageLength = BitManipulator.bitsToInt(headerBits);

    // 2. Validate message length
    if (messageLength < 0 || messageLength > maxCapacity) {
        throw new SteganographyException("Invalid or no hidden message");
    }

    // 3. Extract message bits (skip header)
    int[] messageBits = extractBits(
        image,
        messageLength * 8,  // Number of bits to extract
        32,                 // Start after header
        bitsPerChannel
    );

    // 4. Convert bits back to string
    return BitManipulator.bitsToString(messageBits);
}
```

#### Bit Extraction Process

```java
private int[] extractBits(BufferedImage image, int bitCount,
                          int startBit, int bitsPerChannel) {
    int[] bits = new int[bitCount];
    int totalBitsPerPixel = 3 * bitsPerChannel;

    // Calculate starting position
    int startPixel = startBit / totalBitsPerPixel;
    int startBitInPixel = startBit % totalBitsPerPixel;

    // Navigate to starting pixel
    int pixelX = startPixel % width;
    int pixelY = startPixel / width;

    while (bitIndex < bitCount) {
        int pixel = image.getRGB(pixelX, pixelY);
        int[] channels = {
            (pixel >> 16) & 0xFF,  // Red
            (pixel >> 8) & 0xFF,   // Green
            pixel & 0xFF           // Blue
        };

        // Extract bits from each channel
        for each channel {
            int extractedLSBs = BitManipulator.extractLSBs(
                channelValue, bitsPerChannel
            );

            // Extract individual bits from MSB to LSB order
            for (int bitInChannel = 0; bitInChannel < bitsPerChannel; bitInChannel++) {
                int bitPosition = bitsPerChannel - 1 - bitInChannel;
                bits[bitIndex++] = BitManipulator.getBit(extractedLSBs, bitPosition);
            }
        }

        // Move to next pixel
        pixelX++;
        if (pixelX >= width) {
            pixelX = 0;
            pixelY++;
        }
    }

    return bits;
}
```

---

### Variable Bits Per Channel

This implementation supports **1-8 bits per channel**, allowing users to trade off between:

| Bits/Channel | Capacity | Quality Impact | Use Case |
|--------------|----------|----------------|----------|
| 1 | Low | Minimal (±1 per channel) | Maximum stealth |
| 2 | 2x | Slight (±3 per channel) | Good balance |
| 3-4 | 4-8x | Moderate | Large payloads |
| 5-8 | 8-16x | Significant | Maximum capacity |

**Capacity Formula:**
```
Capacity (bytes) = (Width × Height × 3 × BitsPerChannel - 32) / 8

Example: 1920×1080 image with 1 bit/channel
Capacity = (1920 × 1080 × 3 × 1 - 32) / 8 = 777,596 bytes ≈ 759 KB
```

**Quality impact visualization:**

```
Original pixel: R=147 (10010011)

1 bit/channel:  R can be 146-147  (change: ±1)
2 bits/channel: R can be 144-147  (change: ±3)
3 bits/channel: R can be 144-151  (change: ±7)
4 bits/channel: R can be 144-159  (change: ±15)
...
8 bits/channel: R can be 0-255    (complete replacement)
```

---

## Image Quality Metrics

The `ImageMetricsService` class (`src/main/java/com/steganography/service/ImageMetricsService.java`) implements standard image quality assessment metrics.

### Mean Square Error (MSE)

MSE measures the average squared difference between original and modified pixel values:

```
         1        H   W   3
MSE = ─────── × Σ   Σ   Σ  (Original[y,x,c] - Modified[y,x,c])²
      W×H×3    y=0 x=0 c=0
```

**Implementation:**

```java
public double calculateMSE(BufferedImage original, BufferedImage modified) {
    long sumSquaredError = 0;
    int totalPixels = width * height;

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // Extract RGB from both images
            int origRed = (origPixel >> 16) & 0xFF;
            int modRed = (modPixel >> 16) & 0xFF;
            // ... same for green and blue

            // Accumulate squared differences
            sumSquaredError += Math.pow(origRed - modRed, 2);
            sumSquaredError += Math.pow(origGreen - modGreen, 2);
            sumSquaredError += Math.pow(origBlue - modBlue, 2);
        }
    }

    return (double) sumSquaredError / (totalPixels * 3);
}
```

**Interpretation:**
- MSE = 0: Images are identical
- Lower MSE = Less distortion = Better quality

### Peak Signal-to-Noise Ratio (PSNR)

PSNR expresses the ratio between the maximum possible signal power and corrupting noise power in decibels:

```
                    MAX²
PSNR = 10 × log₁₀ ─────
                    MSE

Where MAX = 255 (maximum pixel value for 8-bit images)
```

**Implementation:**

```java
public double calculatePSNR(BufferedImage original, BufferedImage modified) {
    double mse = calculateMSE(original, modified);

    if (mse == 0) {
        return Double.POSITIVE_INFINITY;  // Identical images
    }

    double maxPixelValue = 255.0;
    return 10 * Math.log10((maxPixelValue * maxPixelValue) / mse);
}
```

**Quality Assessment Scale:**

| PSNR (dB) | Quality | Description |
|-----------|---------|-------------|
| ∞ | PERFECT | Images are identical |
| ≥ 50 | EXCELLENT | No perceptible difference |
| 40-50 | VERY GOOD | Differences imperceptible to most |
| 30-40 | GOOD | Minor visible differences |
| 20-30 | FAIR | Noticeable differences |
| < 20 | POOR | Significant distortion |

**Typical PSNR values for LSB steganography:**
- 1 bit/channel: ~51-57 dB (Excellent)
- 2 bits/channel: ~44-51 dB (Very Good)
- 4 bits/channel: ~38-44 dB (Good)

---

## Project Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        REST API Layer                          │
│                   SteganographyController                       │
│         /encode  /decode  /analyze  /capacity                   │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                       Service Layer                             │
│  ┌─────────────────────┐    ┌─────────────────────────────┐    │
│  │ SteganographyService│    │   ImageMetricsService       │    │
│  │   - encode()        │    │   - calculateMSE()          │    │
│  │   - decode()        │    │   - calculatePSNR()         │    │
│  │   - analyze()       │    │   - getQualityAssessment()  │    │
│  │   - calculateCap()  │    │                             │    │
│  └──────────┬──────────┘    └─────────────────────────────┘    │
└─────────────┼───────────────────────────────────────────────────┘
              │
┌─────────────▼───────────────────────────────────────────────────┐
│                         Core Layer                              │
│  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐   │
│  │  LsbEncoder    │  │   LsbDecoder   │  │  BitManipulator │   │
│  │  - encode()    │  │   - decode()   │  │  - getBit()     │   │
│  │  - embedBits() │  │   - extract()  │  │  - replaceLSBs()│   │
│  │  - capacity()  │  │                │  │  - stringToBits │   │
│  └────────────────┘  └────────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---


### Detailed File Descriptions

| File | Purpose |
|------|---------|
| `BitManipulator.java` | Provides fundamental bit-level operations: extracting bits, replacing LSBs, converting between strings/integers and bit arrays. The foundation of all steganographic operations. |
| `LsbEncoder.java` | Implements the encoding algorithm. Prepends a 32-bit header (message length), converts the message to bits, and embeds them into image pixels using configurable bits per channel. |
| `LsbDecoder.java` | Reverses the encoding process. Extracts the header to determine message length, then extracts and reconstructs the hidden message from pixel LSBs. |
| `SteganographyService.java` | Service layer that orchestrates encoding/decoding operations, handles image I/O, and coordinates with the metrics service for quality analysis. |
| `ImageMetricsService.java` | Calculates MSE and PSNR metrics to quantify image quality degradation after steganographic embedding. |
| `SteganographyController.java` | REST controller exposing four endpoints: encode, decode, analyze, and capacity calculation. |
| `GlobalExceptionHandler.java` | Catches and formats exceptions into consistent JSON error responses with appropriate HTTP status codes. |
| `WebConfig.java` | Configures CORS to allow frontend applications (localhost:3000, localhost:5173) to access the API. |

---

## API Reference

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints

#### 1. Encode Message

Hides a secret message within an image.

```http
POST /encode
Content-Type: multipart/form-data
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| image | file | Yes | PNG image file (cover image) |
| message | string | Yes | Secret message to hide |
| bitsPerChannel | int | No | LSBs to use per channel (1-8, default: 1) |

**Response:** PNG image file with embedded message

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/encode \
  -F "image=@cover.png" \
  -F "message=Secret message here" \
  -F "bitsPerChannel=1" \
  --output stego.png
```

---

#### 2. Decode Message

Extracts a hidden message from a stego image.

```http
POST /decode
Content-Type: multipart/form-data
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| image | file | Yes | PNG image with hidden message |
| bitsPerChannel | int | No | LSBs that were used (1-8, default: 1) |

**Response:**
```json
{
  "message": "Secret message here",
  "messageLength": 19,
  "bitsPerChannelUsed": 1
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/decode \
  -F "image=@stego.png" \
  -F "bitsPerChannel=1"
```

---

#### 3. Analyze Quality

Compares original and stego images to measure quality degradation.

```http
POST /analyze
Content-Type: multipart/form-data
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| original | file | Yes | Original cover image |
| stego | file | Yes | Image with hidden message |

**Response:**
```json
{
  "mse": 0.1523,
  "psnr": 56.34,
  "qualityAssessment": "EXCELLENT",
  "originalWidth": 1920,
  "originalHeight": 1080
}
```

---

#### 4. Calculate Capacity

Calculates how much data can be hidden in an image of given dimensions.

```http
GET /capacity
```

**Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| width | int | Yes | Image width in pixels |
| height | int | Yes | Image height in pixels |
| bitsPerChannel | int | No | LSBs to use (1-8, default: 1) |

**Response:**
```json
{
  "width": 1920,
  "height": 1080,
  "bitsPerChannel": 1,
  "capacityBytes": 777596,
  "capacityKilobytes": 759.37,
  "totalPixels": 2073600
}
```

**Example:**
```bash
curl "http://localhost:8080/api/v1/capacity?width=1920&height=1080&bitsPerChannel=2"
```

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd steganography-api

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

---

## Technical Considerations

### Why PNG Format?

This API uses PNG (Portable Network Graphics) because:
1. **Lossless compression** - JPEG and other lossy formats destroy LSB data
2. **Wide support** - Supported by all browsers and image tools
3. **Alpha channel support** - Though not used for embedding, preserved in output

### Security Notes

- LSB steganography provides **obscurity, not encryption**
- For sensitive data, encrypt your message before embedding
- The hidden message can be extracted by anyone who knows to look for it
- Consider combining with AES encryption for true security

### Limitations

- Only supports PNG images
- Alpha channel is not used for data embedding
- No built-in encryption (encrypt before embedding if needed)
- Message length limited by image capacity

---

## Inspirations & References

The following open-source projects served as inspiration for the algorithms used in this application:

- [Image-Steganography-using-LSB](https://github.com/MonikaJov/Image-Steganography-using-LSB) — LSB steganography implementation with encoding and decoding
- [Image-Steganography-using-LSB (Yoga-Priya)](https://github.com/Yoga-Priya/Image-Steganography-using-LSB) — Python-based LSB steganography approach
- [Steganography_LSB-DCT-and-compare](https://github.com/Mahmoud-raafat-ac/Steganography_LSB-DCT-and-compare) — Comparison between LSB and DCT steganography methods
- [Image-Stegano LSBEncoding](https://github.com/varunon9/Image-Stegano/blob/master/src/steganography/LSBEncoding.java) — Java LSB encoding implementation
- [Steganography topic on GitHub](https://github.com/topics/steganography?l=java) — Collection of Java steganography projects

---

## License

This project is provided for educational purposes.