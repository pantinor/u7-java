package ultima7;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * Utility to take an image and transform a given color(s) to be transparent
 * instead of opaque.
 *
 * @author Paul
 *
 */
public class ImageTransparency {

    public static int MARKER_RED;
    public static int MARKER_GREEN;
    public static int MARKER_BLUE;

    public static void convert(String inputFileName, String outputFileName) {
        try {

            BufferedImage source = ImageIO.read(new File(inputFileName));
            int rgb = source.getRGB(0, 0);
            convert(inputFileName, outputFileName, rgb);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convert(String inputFileName, String outputFileName, Color[] transColors) {
        try {

            BufferedImage source = ImageIO.read(new File(inputFileName));

            for (Color color : transColors) {

                int rgb = color.getRGB();

                MARKER_RED = (rgb >> 16) & 0xFF;
                MARKER_GREEN = (rgb >> 8) & 0xFF;
                MARKER_BLUE = rgb & 0xFF;

                source = makeColorTransparent(source, color);
            }

            ImageIO.write(source, "PNG", new File(outputFileName));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convert(String inputFileName, String outputFileName, int rgb) {
        try {

            System.out.println("Copying file " + inputFileName + " to " + outputFileName);

            MARKER_RED = (rgb >> 16) & 0xFF;
            MARKER_GREEN = (rgb >> 8) & 0xFF;
            MARKER_BLUE = rgb & 0xFF;

            BufferedImage source = ImageIO.read(new File(inputFileName));
            BufferedImage imageWithTransparency = makeColorTransparent(source, new Color(rgb));
            //BufferedImage transparentImage = imageToBufferedImage(imageWithTransparency);
            ImageIO.write(imageWithTransparency, "PNG", new File(outputFileName));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static BufferedImage convert(BufferedImage source, String outputFileName, int rgb) throws Exception {

        System.out.println("converting to " + outputFileName);

        MARKER_RED = (rgb >> 16) & 0xFF;
        MARKER_GREEN = (rgb >> 8) & 0xFF;
        MARKER_BLUE = rgb & 0xFF;

        BufferedImage imageWithTransparency = makeColorTransparent(source, new Color(rgb));

        return imageWithTransparency;

    }

    public static BufferedImage makeColorTransparent(BufferedImage im, final Color color) {

        final ImageFilter filter = new RGBImageFilter() {
            public int markerRGB = color.getRGB();

            public final int filterRGB(int x, int y, int rgb) {

                int alpha = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                if (red == MARKER_RED && green == MARKER_GREEN && blue == MARKER_BLUE) {
                    // Mark the alpha bits as zero - transparent
                    rgb = 0x00FFFFFF & rgb;
                }

                alpha = (rgb >> 24) & 0xff;
                red = (rgb >> 16) & 0xFF;
                green = (rgb >> 8) & 0xFF;
                blue = rgb & 0xFF;

                return rgb;
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        Image i = Toolkit.getDefaultToolkit().createImage(ip);

        return imageToBufferedImage(i);

    }

    public static BufferedImage imageToBufferedImage(Image image) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return bufferedImage;

    }

    public static BufferedImage cropTransparent(double[] alpha, BufferedImage t) {
        // Find the bounding box
        WritableRaster r = t.getRaster();
        int minx = -1;
        int miny = -1;
        int maxx = r.getWidth();
        int maxy = r.getHeight();
        double[] pv = new double[4];
        int x0 = 0;
        int x1 = r.getWidth();
        int y0 = 0;
        int y1 = r.getHeight();

        // min y
        boolean contentFound = false;
        for (int y = y0; y < y1 && !contentFound; y++) {
            for (int x = x0; x < x1; x++) {
                r.getPixel(x, y, pv);
                if (!Arrays.equals(pv, alpha)) {
                    contentFound = true;
                    miny = y;
                    break;
                }
            }
        }

        // max y
        contentFound = false;
        for (int y = y1 - 1; y > 0 && !contentFound; y--) {
            for (int x = x0; x < x1; x++) {
                r.getPixel(x, y, pv);
                if (!Arrays.equals(pv, alpha)) {
                    contentFound = true;
                    maxy = y + 1;
                    break;
                }
            }
        }

        // min x
        contentFound = false;
        for (int x = x0; x < x1 && !contentFound; x++) {
            for (int y = y0; y < y1; y++) {
                r.getPixel(x, y, pv);
                if (!Arrays.equals(pv, alpha)) {
                    contentFound = true;
                    minx = x;
                    break;
                }
            }
        }

        // max x
        contentFound = false;
        for (int x = x1 - 1; x > x0 && !contentFound; x--) {
            for (int y = y0; y < y1; y++) {
                r.getPixel(x, y, pv);
                if (!Arrays.equals(pv, alpha)) {
                    contentFound = true;
                    maxx = x + 1;
                    break;
                }
            }
        }
        
        if (minx == -1 && miny == -1) {
            return t.getSubimage(0, 0, 1, 1);
        }

        minx = minx < x0 ? x0 : minx;
        miny = miny < y0 ? y0 : miny;
        maxx = maxx > x1 ? x1 : maxx;
        maxy = maxy > y1 ? y1 : maxy;

        int nw = maxx - minx;
        int nh = maxy - miny;

        return t.getSubimage(minx, miny, nw, nh);
    }

    public static BufferedImage makeOutline(BufferedImage originalImage) {

        BufferedImage clone = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        WritableRaster original = originalImage.getRaster();
        WritableRaster canvas = clone.getRaster();

        double[] black = new double[]{0, 0, 0, 255};
        double[] alpha = new double[]{0, 0, 0, 0};

        double[] pv = new double[4];
        double[] dv = new double[4];
        int x0 = 0;
        int x1 = originalImage.getWidth();
        int y0 = 0;
        int y1 = originalImage.getHeight();

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                original.getPixel(x, y, pv);
                if (!Arrays.equals(pv, alpha)) {
                    checkAdjacents(original, canvas, x, y, x1, y1);
                }
            }
        }

        return clone;
    }

    private static void checkAdjacents(WritableRaster original, WritableRaster r, int x, int y, int maxx, int maxy) {

        double[] black = new double[]{0, 0, 0, 255};
        double[] alpha = new double[]{0, 0, 0, 0};
        double[] dv = new double[4];

        if (x < maxx - 1) {
            original.getPixel(x + 1, y, dv);
            if (Arrays.equals(dv, alpha)) {
                r.setPixel(x + 1, y, black);
            }
        }
        if (x > 0) {
            original.getPixel(x - 1, y, dv);
            if (Arrays.equals(dv, alpha)) {
                r.setPixel(x - 1, y, black);
            }
        }
        if (y < maxy - 1) {
            original.getPixel(x, y + 1, dv);
            if (Arrays.equals(dv, alpha)) {
                r.setPixel(x, y + 1, black);
            }
        }
        if (y > 0) {
            original.getPixel(x, y - 1, dv);
            if (Arrays.equals(dv, alpha)) {
                r.setPixel(x, y - 1, black);
            }
        }

    }

}
