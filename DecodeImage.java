import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class DecodeImage {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java DecodeImage <encoded-image.png>");
            return;
        }

        File inputFile = new File(args[0]);

        try {
            BufferedImage img = ImageIO.read(inputFile);
            if (img == null) {
                System.err.println("Error: Could not read image '" + inputFile.getPath() + "'.");
                return;
            }

            // Read 32-bit byte length from pixels 0..31
            int length = 0;
            for (int i = 0; i < 32; i++) {
                length = (length << 1) | getBlueBit(img, i);
            }

            long totalPixels = (long) img.getWidth() * img.getHeight();
            if (length <= 0 || (32L + (length * 8L)) > totalPixels) {
                System.err.println("Error: No valid hidden message found in image.");
                return;
            }

            // Read 'length' bytes starting at pixel 32
            byte[] msgBytes = new byte[length];
            int pixelIndex = 32;
            for (int i = 0; i < length; i++) {
                int currentByte = 0;
                for (int bitPos = 7; bitPos >= 0; bitPos--) {
                    currentByte = (currentByte << 1) | getBlueBit(img, pixelIndex++);
                }
                msgBytes[i] = (byte) currentByte;
            }

            String decoded = new String(msgBytes, StandardCharsets.UTF_8);
            System.out.println(decoded);
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    private static int getBlueBit(BufferedImage img, int index) {
        int x = index % img.getWidth();
        int y = index / img.getWidth();
        return img.getRGB(x, y) & 1;
    }
}