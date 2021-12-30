package ultima7;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public class ImageTransparency {

    public static BufferedImage makeColorTransparent(BufferedImage im, final int markerAlpha) {

        final ImageFilter filter = new RGBImageFilter() {

            @Override
            public final int filterRGB(int x, int y, int rgb) {
                if (rgb == markerAlpha) {
                    rgb = 0x00FFFFFF & rgb;
                }
                return rgb;
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        Image image = Toolkit.getDefaultToolkit().createImage(ip);

        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return bufferedImage;

    }

    public static BufferedImage cropTransparent(BufferedImage bi, int markerAlpha) {

        int minx = -1;
        int miny = -1;
        int maxx = bi.getWidth();
        int maxy = bi.getHeight();

        int x0 = 0;
        int x1 = bi.getWidth();
        int y0 = 0;
        int y1 = bi.getHeight();

        // min y
        boolean contentFound = false;
        for (int y = y0; y < y1 && !contentFound; y++) {
            for (int x = x0; x < x1; x++) {
                int rgb = bi.getRGB(x, y);
                if (rgb != markerAlpha) {
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
                int rgb = bi.getRGB(x, y);
                if (rgb != markerAlpha) {
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
                int rgb = bi.getRGB(x, y);
                if (rgb != markerAlpha) {
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
                int rgb = bi.getRGB(x, y);
                if (rgb != markerAlpha) {
                    contentFound = true;
                    maxx = x + 1;
                    break;
                }
            }
        }

        //if no content found then crop down to 8x8 red square
        if (minx == -1 && miny == -1) {
            BufferedImage ret = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x > 8; x++) {
                for (int y = 0; y < 8; y++) {
                    ret.setRGB(x, y, 0xFF0000FF);
                }
            }
            return ret;
            //return t.getSubimage(0, 0, 1, 1);
        }

        minx = minx < x0 ? x0 : minx;
        miny = miny < y0 ? y0 : miny;
        maxx = maxx > x1 ? x1 : maxx;
        maxy = maxy > y1 ? y1 : maxy;

        int nw = maxx - minx;
        int nh = maxy - miny;

        return bi.getSubimage(minx, miny, nw, nh);
    }

    private static final double[] BLACK = new double[]{0, 0, 0, 255};
    private static final double[] TRANSPARENT = new double[]{0, 0, 0, 0};

    public static BufferedImage makeOutline(BufferedImage originalImage) {

        BufferedImage clone = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        WritableRaster original = originalImage.getRaster();
        WritableRaster canvas = clone.getRaster();

        double[] pv = new double[4];

        int x0 = 0;
        int x1 = originalImage.getWidth();
        int y0 = 0;
        int y1 = originalImage.getHeight();

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                original.getPixel(x, y, pv);
                if (!Arrays.equals(pv, TRANSPARENT)) {
                    checkAdjacents(original, canvas, x, y, x1, y1);
                }
            }
        }

        return clone;
    }

    private static void checkAdjacents(WritableRaster original, WritableRaster r, int x, int y, int maxx, int maxy) {

        double[] dv = new double[4];

        if (x < maxx - 1) {
            original.getPixel(x + 1, y, dv);
            if (Arrays.equals(dv, TRANSPARENT)) {
                r.setPixel(x + 1, y, BLACK);
            }
        }
        if (x > 0) {
            original.getPixel(x - 1, y, dv);
            if (Arrays.equals(dv, TRANSPARENT)) {
                r.setPixel(x - 1, y, BLACK);
            }
        }
        if (y < maxy - 1) {
            original.getPixel(x, y + 1, dv);
            if (Arrays.equals(dv, TRANSPARENT)) {
                r.setPixel(x, y + 1, BLACK);
            }
        }
        if (y > 0) {
            original.getPixel(x, y - 1, dv);
            if (Arrays.equals(dv, TRANSPARENT)) {
                r.setPixel(x, y - 1, BLACK);
            }
        }

    }

    public static boolean checkAllAdjacentsAreTransparent(int[][] data, int y, int x, int transparent) {
        
        if (y + 1 < data.length && data[y + 1][x] != transparent) {
            return false;
        }
        if (y - 1 > 0 && data[y - 1][x] != transparent) {
            return false;
        }
        if (x + 1 < data[y].length && data[y][x + 1] != transparent) {
            return false;
        }
        if (x - 1 > 0 && data[y][x - 1] != transparent) {
            return false;
        }

        return true;

    }

}
