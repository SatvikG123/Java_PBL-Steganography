# Java_PBL-Steganography
# Image LSB Steganography (Java)

A simple command-line Java program that hides a text message inside an image using **Least Significant Bit (LSB) steganography** on the blue color channel and extracts it back out.

## Project Files
- `EncodeImage.java` — Reads an image from the command line, prompts for a secret message, embeds the 32-bit byte length followed by the UTF-8 message bits into the blue-channel LSBs, and saves the result losslessly as a `.png`.
- `DecodeImage.java` — Reads a `.png` image from the command line, extracts the 32-bit length header, reads the hidden bytes, and prints the decoded message.

## How It Works
1. **Length Header (First 32 pixels):** Stores the 32-bit integer byte length of the UTF-8 message so the decoder knows how many bytes to read.
2. **Payload Bits (Remaining pixels):** Replaces the least significant bit (`& 0xFFFFFFFE`) of each pixel's blue channel with 1 bit of message data. Changing a channel value by at most `1` is visually imperceptible, and `.png` lossless compression keeps the modified bits intact.

## Compile and Run
Open a terminal in your folder and run:

```powershell
javac EncodeImage.java DecodeImage.java
java EncodeImage Image.png
java DecodeImage Image.png
```
