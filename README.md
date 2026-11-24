# Java_PBL-Steganography

# Image LSB Steganography in Java

A lightweight, self-contained Java command-line tool that hides secret text messages inside standard image files using **Least Significant Bit (LSB) Steganography** and extracts them back out without any visible distortion.

---

## 1. The Plain-English Explanation (Layman's Guide)

### What does this project do?
Imagine you want to send a secret note to a friend, and you don't want anyone else to even suspect that a secret message is being sent. Instead of sending a locked box (which immediately tells everyone *"there is something valuable inside!"*), you slip the note inside a regular birthday card. To anyone passing by, it just looks like a normal birthday card.

In the digital world:
* **Cryptography** is the locked box—it scrambles a message into unreadable gibberish (`#9f$kL2...`), which hides the *meaning* of the message, though everyone can still see that a secret message exists.
* **Steganography** is the birthday card—it hides the *very existence* of the message inside an everyday file like a photograph.

### How can text hide inside a picture without changing how the picture looks?
Every digital image on your screen is made up of millions of tiny colored dots called **pixels**. Each pixel's color is created by mixing three primary light colors: **Red**, **Green**, and **Blue** (RGB). Each of those three colors is stored as a number from `0` (completely dark) to `255` (full brightness).

For example, a shade of sky blue might have a Blue value of `200`. If we change that number by just `1`—making it `201`—the shade of blue changes by less than 0.4%. The human eye simply cannot tell the difference between `200` and `201`.

This program uses that tiny `1`-point wiggle room in every pixel's **Blue** value to store binary `0`s and `1`s—effectively spelling out your secret text message inside the pixels of the image.

### Why does it save the result as a `.png` file?
* **JPEG (`.jpg`)** files shrink their file size by aggressively averaging and discarding tiny color details. If you hide bits inside a `.jpg`, JPEG compression wipes them out.
* **PNG (`.png`)** files use *lossless* compression—every single pixel number is preserved down to the exact digit, keeping your hidden message completely intact.

---

## 2. Project Architecture & File Overview

The project consists of two standalone Java source files with zero external third-party libraries (built strictly on standard `java.awt`, `javax.imageio`, `java.io`, `java.nio`, and `java.util`):

| File | Role | Input | Output |
| :--- | :--- | :--- | :--- |
| **`EncodeImage.java`** | Embeds a UTF-8 text message into the blue channel LSBs of an image. | Image file path (CLI argument) + secret message (`System.in`) | Lossless `.png` image containing the hidden header and payload |
| **`DecodeImage.java`** | Extracts the length header and reconstructs the hidden UTF-8 string. | Encoded `.png` file path (CLI argument) | Prints the extracted plaintext message to `System.out` |

---

## 3. Deep Technical Specification

### 3.1 Pixel Bit Layout (`BufferedImage.TYPE_INT_RGB`)
When `EncodeImage` reads a source image via `ImageIO.read()`, it draws the image onto a normalized canvas of type `BufferedImage.TYPE_INT_RGB`. In this format, each pixel is represented as a 32-bit signed integer (`int`) packed in `0x00RRGGBB` layout:

```text
Bits 31..24 : Unused / Alpha padding (0x00)
Bits 23..16 : Red channel   (8 bits, 0–255)
Bits 15..8  : Green channel (8 bits, 0–255)
Bits  7..0  : Blue channel  (8 bits, 0–255)  <-- Bit 0 (LSB) is modified
```

- **Bit Insertion (Encoding):** To write a single bit $b \in \{0, 1\}$ into the least significant bit of the blue channel without disturbing the upper 31 bits:

$$\text{updatedRGB} = (\text{rgb} \ \& \ \text{0xFFFFFFFE}) \ | \ (b \ \& \ 1)$$

The bitmask `0xFFFFFFFE` (`11111111_11111111_11111111_11111110` in binary) clears bit `0` to `0`, and bitwise OR (`|`) sets it to `b`.

- **Bit Extraction (Decoding):**

$$\text{extractedBit} = \text{rgb} \ \& \ 1$$

### 3.2 Linear Pixel Addressing
Pixels are traversed sequentially in row-major order (left-to-right, top-to-bottom). A linear pixel index $i \ge 0$ maps to 2D image coordinates $(x, y)$ as:
$$x = i \bmod \text{width}, \qquad y = \left\lfloor \frac{i}{\text{width}} \right\rfloor$$

### 3.3 Binary Payload Framing Protocol
Because an image has no inherent end-of-message marker, `EncodeImage` frames the payload with a fixed **32-bit big-endian length prefix** followed by the raw UTF-8 payload bytes:

```text
+-----------------------------------+------------------------------------------+
|     32-Bit Length Header (4B)     |         UTF-8 Message Payload            |
|        Pixels 0 .. 31             |     Pixels 32 .. (32 + 8*N - 1)          |
+-----------------------------------+------------------------------------------+
```

1. **Header (`Pixels 0..31`):**
   The byte length $N = \text{msgBytes.length}$ (a 32-bit Java `int`) is serialized most-significant-bit first (`bit 31` down to `bit 0`) using unsigned right-shift (`>>>`).
2. **Payload (`Pixels 32 .. 32 + 8N - 1`):**
   Each byte of the UTF-8 encoded string is serialized from bit `7` (MSB) down to bit `0` (LSB), consuming 8 pixels per byte.
3. **Capacity Constraint:**
   Because 1 pixel stores 1 bit of information, a cover image with dimensions $W \times H$ can store at most:
   $$\text{Max Payload Bytes} = \left\lfloor \frac{(W \times H) - 32}{8} \right\rfloor$$
   For example, a standard $1920 \times 1080$ image contains $2,073,600$ pixels, providing capacity for **$259,196$ bytes** (~253 KB) of UTF-8 text.

### 3.4 Decoder Validation Checks
Before allocating memory for the output byte array, `DecodeImage` validates the extracted 32-bit `length` header against two bounds:
* `length <= 0` (rejects blank/unencoded images whose initial bits decode to zero or a negative signed integer).
* `32L + (length * 8L) > totalPixels` (prevents `OutOfMemoryError` or out-of-bounds pixel reads when random noise in an unencoded image produces a large integer).

---

## 4. Prerequisites & Setup

* **Java Development Kit (JDK):** JDK 11 or newer (`javac` and `java` available in your system `PATH`).
* **Operating System:** Windows, macOS, or Linux.
* **Cover Image:** Any standard image readable by `javax.imageio.ImageIO` (`.jpg`, `.jpeg`, `.png`, `.bmp`).

---

## 5. How to Compile and Run

Open a terminal inside the directory containing `EncodeImage.java` and `DecodeImage.java`.

### Step 1: Compile Both Source Files
```powershell
javac EncodeImage.java DecodeImage.java
```

### Step 2: Encode a Message into an Image
Pass the path to your target image as a command-line argument. The program will prompt you to type your secret message:
```powershell
java EncodeImage path\to\cover1.jpg
```
**Example Terminal Session:**
```text
Enter message to encode: Meet at Lab 3 at 4:00 PM
Updated image in place: path\to\cover1.png
```
*(Note: If the input file was a `.jpg`, the program writes the encoded output as a `.png` file with the same base filename to guarantee lossless storage.)*

### Step 3: Decode the Message from the PNG Image
Pass the encoded `.png` image path to `DecodeImage`:
```powershell
java DecodeImage path\to\cover1.png
```
**Example Output:**
```text
Meet at Lab 3 at 4:00 PM
```

---

## 6. Error Handling & Edge Cases

| Message / Error | Cause | Resolution |
| :--- | :--- | :--- |
| `Error: File not found` | The file path passed to `EncodeImage` does not exist. | Verify the directory path and file extension (`.jpg` vs `.jpeg` vs `.png`). |
| `Error: Message cannot be empty.` | `Enter` was pressed with no characters typed at the prompt. | Enter at least one character to encode. |
| `Error: Image too small for message` | Total required bits (`32 + 8 * bytes`) exceed `width * height`. | Use a higher-resolution cover image or shorten the text message. |
| `Error: No valid hidden message found in image.` | `DecodeImage` was run on an unencoded image, a `.jpg` file, or an image re-saved with lossy compression. | Ensure you pass the exact `.png` output generated by `EncodeImage`. |

---

## 7. Security Limitations & Potential Improvements

This implementation is designed for educational clarity to demonstrate core steganographic principles. In real-world security applications, it has well-known limitations that can be addressed in future iterations:

1. **No Cryptographic Confidentiality:** The bits are stored in plaintext UTF-8. Adding **AES-GCM encryption** with a password-derived key (**PBKDF2**) ensures that even if hidden bits are extracted, they cannot be read without the passphrase.
2. **Sequential Pixel Placement:** Writing sequentially starting at pixel `(0, 0)` creates a detectable statistical anomaly at the top edge of the image (vulnerable to Chi-Square or RS steganalysis). Scattering bit positions across the image using a **cryptographically seeded Fisher-Yates shuffle** distributes changes uniformly.
3. **Single-Channel Embedding:** Utilizing the LSBs of all three channels (Red, Green, and Blue) triples storage capacity from 1 bit per pixel to 3 bits per pixel.
