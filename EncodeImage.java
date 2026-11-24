import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class EncodeImage {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java EncodeImage <image-file>");
            return;
        }

        File targetFile = new File(args[0]);
        if (!targetFile.exists()) {
            System.err.println("Error: File not found -> " + targetFile.getPath());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter message to encode: ");
        String message = scanner.nextLine();

        if (message.isEmpty()) {
            System.err.println("Error: Message cannot be empty.");
            return;
        }

        try {
            BufferedImage source = ImageIO.read(targetFile);
            if (source == null) {
                System.err.println("Error: Could not read image '" + targetFile.getPath() + "'.");
                return;
            }

            byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
            long totalBitsNeeded = 32L + (msgBytes.length * 8L);
            long totalPixels = (long) source.getWidth() * source.getHeight();

            if (totalBitsNeeded > totalPixels) {
                System.err.println("Error: Image too small for message (" + totalBitsNeeded + " bits needed, " + totalPixels + " pixels available).");
                return;
            }

            BufferedImage stego = new BufferedImage(
                    source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            stego.getGraphics().drawImage(source, 0, 0, null);

            // Store 32-bit message byte length in pixels 0..31
            for (int i = 0; i < 32; i++) {
                int bit = (msgBytes.length >>> (31 - i)) & 1;
                setBlueBit(stego, i, bit);
            }

            // Store message bits starting at pixel 32
            int pixelIndex = 32;
            for (byte b : msgBytes) {
                for (int bitPos = 7; bitPos >= 0; bitPos--) {
                    int bit = (b >>> bitPos) & 1;
                    setBlueBit(stego, pixelIndex++, bit);
                }
            }

            // Overwrite as PNG so lossless compression preserves all LSB changes
            File outFile = targetFile.getName().toLowerCase().endsWith(".png")
                    ? targetFile
                    : new File(targetFile.getParentFile(),
                            targetFile.getName().replaceFirst("\\.[^.]+$", "") + ".png");

            ImageIO.write(stego, "png", outFile);
            System.out.println("Updated image in place: " + outFile.getPath());
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    private static void setBlueBit(BufferedImage img, int index, int bit) {
        int x = index % img.getWidth();
        int y = index / img.getWidth();
        int rgb = img.getRGB(x, y);
        int updated = (rgb & 0xFFFFFFFE) | (bit & 1);
        img.setRGB(x, y, updated);
    }
}