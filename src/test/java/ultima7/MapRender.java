package ultima7;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class MapRender {

    private static List<Palette> palettes = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        FileInputStream cis = new FileInputStream("c://Users//panti/Desktop//STATIC//U7CHUNKS");
        FileInputStream mis = new FileInputStream("c://Users//panti/Desktop//STATIC//U7MAP");
        FileInputStream sis = new FileInputStream("c://Users//panti/Desktop//STATIC//SHAPES.VGA");
        FileInputStream pis = new FileInputStream("c://Users//panti/Desktop//STATIC//PALETTES.FLX");

        ByteBuffer cbb = ByteBuffer.wrap(IOUtils.toByteArray(cis));
        ByteBuffer mbb = ByteBuffer.wrap(IOUtils.toByteArray(mis));
        ByteBuffer sbb = ByteBuffer.wrap(IOUtils.toByteArray(sis));
        ByteBuffer pbb = ByteBuffer.wrap(IOUtils.toByteArray(pis));

        cbb.order(ByteOrder.LITTLE_ENDIAN);
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        sbb.order(ByteOrder.LITTLE_ENDIAN);
        pbb.order(ByteOrder.LITTLE_ENDIAN);

        Region[][] regions = new Region[12][12];
        short[][][][] chunkIds = new short[12][12][16][16];

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        short id = mbb.getShort();
                        chunkIds[yy][xx][y][x] = id;
                    }
                }
            }
        }

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {

                Chunk[][] chunks = new Chunk[16][16];
                Region region = new Region(yy * 12 + xx, chunks);
                regions[yy][xx] = region;

                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {

                        short chunkId = chunkIds[yy][xx][y][x];

                        int offset = chunkId * 512;

                        Shape[][] shapes = new Shape[16][16];
                        chunks[y][x] = new Chunk(chunkId, shapes);

                        for (int tiley = 0; tiley < 16; tiley++) {
                            for (int tilex = 0; tilex < 16; tilex++) {

                                int id = 16 * tiley + tilex;
                                int shapeIndex = cbb.get(offset + 0) & 0xff + 256 * (cbb.get(offset + 1) & 3);
                                int frameIndex = (cbb.get(offset + 1) >> 2) & 0x1f;

                                offset += 2;

                                Shape s = new Shape(id, shapeIndex, frameIndex);
                                shapes[tiley][tilex] = s;
                            }
                        }
                    }
                }
            }
        }

        pbb.position(0x54);
        int numPals = pbb.getInt();

        pbb.position(0x80);
        for (int i = 0; i < numPals; i++) {
            int offset = pbb.getInt();
            int len = pbb.getInt();
            byte[] entry = new byte[len];
            palettes.add(new Palette(i, offset, len, entry));
        }

        for (Palette p : palettes) {
            pbb.position(p.offset);
            pbb.get(p.data);
            p.set();
        }

        sbb.position(0x54);
        int numRecords = sbb.getInt();

        List<Record> records = new ArrayList<>();

        sbb.position(0x80);
        for (int i = 0; i < numRecords; i++) {
            int offset = sbb.getInt();
            int len = sbb.getInt();
            byte[] entry = new byte[len];
            records.add(new Record(i, offset, len, entry));
        }

        for (Record rec : records) {
            sbb.position(rec.offset);
            sbb.get(rec.data);
            rec.set();
            System.out.println(rec);
        }

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {
                Region region = regions[yy][xx];
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        Chunk chunk = region.chunks[y][x];
                        for (int tiley = 0; tiley < 16; tiley++) {
                            for (int tilex = 0; tilex < 16; tilex++) {
                                Shape shape = chunk.shapes[tiley][tilex];
                                Record rec = records.get(shape.shapeIndex);
                                int fidx = shape.frameIndex >= rec.frames.length ? 0 : shape.frameIndex;
                                region.bi.getGraphics().drawImage(rec.frames[fidx].bi, x * tilex, y * tiley, null);
                            }
                        }
                    }
                }
            }
        }

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {
                Region region = regions[yy][xx];
                ImageIO.write(region.bi, "PNG", new File("region-" + yy + "-" + xx + ".png"));
            }
        }

    }

    private static class Record {

        int num;
        int offset;
        int len;
        byte[] data;
        ByteBuffer bb;
        Frame[] frames;

        public Record(int num, int offset, int len, byte[] data) {
            this.num = num;
            this.offset = offset;
            this.len = len;
            this.data = data;
            this.bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public String toString() {
            boolean raw = isRawChunkBits();
            return String.format("Record %d offset [%d] len [%d] %s shapeSize [%d] offset count [%d] frames [%s]",
                    this.num, this.offset, this.len,
                    raw ? "RAW" : "SHP",
                    shapeSize(),
                    offsetCount(),
                    frames != null ? frames.length : shapeSize() / 8 * 8);
        }

        void set() {

            int nframes;

            if (!isRawChunkBits()) {
                nframes = offsetCount();

                if (nframes > 0) {

                    if (this.num == 188) {
                        int c = 0;
                    }

                    this.frames = new Frame[nframes];

                    int[] offsets = new int[nframes];
                    for (int n = 0; n < nframes; n++) {
                        offsets[n] = toUInt(n * 4 + 4);
                    }

                    for (int n = 0; n < nframes; n++) {

                        this.bb.position(offsets[n]);

                        int xright = bb.getShort() & 0xff;
                        int xleft = bb.getShort() & 0xff;
                        int yabove = bb.getShort() & 0xff;
                        int ybelow = bb.getShort() & 0xff;

                        this.frames[n] = new Frame(n, xright + xleft + 1, yabove + ybelow + 1);

                        while (true) {
                            int scanData = bb.getShort();
                            int scanLen = scanData >> 1;
                            int encoded = scanData & 1;

                            if (scanData == 0) {
                                break;
                            }

                            int scanX = bb.getShort();
                            int scanY = bb.getShort();

                            if (encoded == 0) {
                                for (int b = 0; b < scanLen; b++) {
                                    this.frames[n].pixels.put(bb.get());
                                }
                            } else {

                                while (scanLen > 0) {
                                    int runData = bb.get() & 0xff;
                                    int runLen = runData >> 1;
                                    int repeat = runData & 1;

                                    if (repeat == 0) {
                                        for (int b = 0; b < runLen; b++) {
                                            this.frames[n].pixels.put(bb.get());
                                        }
                                    } else {
                                        byte val = bb.get();
                                        for (int b = 0; b < runLen; b++) {
                                            this.frames[n].pixels.put(val);
                                        }
                                    }

                                    scanLen -= runLen;
                                }
                            }
                        }
                    }
                }

            } else {
                nframes = this.len / (8 * 8);
                this.frames = new Frame[nframes];
                for (int f = 0; f < nframes; f++) {
                    this.frames[f] = new Frame(f, 8, 8);
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            this.frames[f].pixels.put(bb.get());
                        }
                    }
                }
            }

            for (int f = 0; f < nframes; f++) {
                this.frames[f].pixels.flip();
                for (int y = 0; y < this.frames[f].height; y++) {
                    for (int x = 0; x < this.frames[f].width; x++) {
                        if (this.frames[f].pixels.position() >= this.frames[f].pixels.limit()) {
                            break;
                        }
                        int idx = this.frames[f].pixels.get() & 0xff;
                        int rgb = palettes.get(PALETTE_DAY).rgb(idx);
                        this.frames[f].bi.setRGB(x, y, rgb);
                    }
                }
            }

        }

        boolean isRawChunkBits() {
            return this.len != shapeSize();
        }

        int shapeSize() {
            return toUInt(0);
        }

        int offsetCount() {
            return (toUInt(4) - 4) / 4;
        }

        int toUInt(int idx) {
            return (bb.get(idx + 0) & 0xff) << 0
                    | (bb.get(idx + 1) & 0xff) << 8
                    | (bb.get(idx + 2) & 0xff) << 16
                    | (bb.get(idx + 3) & 0xff) << 24;
        }

        int toUShort(int idx) {
            return (bb.get(idx + 0) & 0xff) << 0
                    | (bb.get(idx + 3) & 0xff) << 8;
        }

    }

    private static class Region {

        int id;
        Chunk[][] chunks;
        BufferedImage bi = new BufferedImage(16 * 8, 16 * 8, BufferedImage.TYPE_INT_ARGB);

        public Region(int id, Chunk[][] chunks) {
            this.id = id;
            this.chunks = chunks;
        }

    }

    private static class Chunk {

        int id;
        Shape[][] shapes;

        public Chunk(int id, Shape[][] shapes) {
            this.id = id;
            this.shapes = shapes;
        }

    }

    private static class Shape {

        int id;
        int shapeIndex;
        int frameIndex;

        public Shape(int id, int shapeIndex, int frameIndex) {
            this.id = id;
            this.shapeIndex = shapeIndex;
            this.frameIndex = frameIndex;
        }

    }

    private static class Frame {

        int number;
        int width;
        int height;
        BufferedImage bi;
        ByteBuffer pixels;

        public Frame(int number, int width, int height) {
            this.number = number;
            this.width = width;
            this.height = height;
            this.bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            this.pixels = ByteBuffer.allocate(width * height);
        }

    }

    public static int PALETTE_DAY = 0;
    public static int PALETTE_DUSK = 1;
    public static int PALETTE_DAWN = 1;     // Think this is it.
    public static int PALETTE_NIGHT = 2;
    public static int PALETTE_INVISIBLE = 3;    // When Avatar is invisible.
    public static int PALETTE_OVERCAST = 4;     // When raining or overcast during daytime
    public static int PALETTE_FOG = 5;
    public static int PALETTE_SPELL = 6; // light spell.
    public static int PALETTE_CANDLE = 7; // is somewhat warmer, candles.
    public static int PALETTE_RED = 8;      // Used when hit in combat.
    // 9 has lots of black.
    public static int PALETTE_LIGHTNING = 10;
    public static int PALETTE_SINGLE_LIGHT = 11;
    public static int PALETTE_MANY_LIGHTS = 12;

    private static class Palette {

        int num;
        int offset;
        int len;
        byte[] data;
        ByteBuffer bb;
        int[] palette;

        public Palette(int num, int offset, int len, byte[] data) {
            this.num = num;
            this.offset = offset;
            this.len = len;

            this.data = data;

            this.bb = ByteBuffer.wrap(data);
            this.bb.order(ByteOrder.LITTLE_ENDIAN);

            this.palette = new int[len];
        }

        void set() {
            if (bb.limit() == 768) {
                for (int i = 0; i < 256; i++) {
                    palette[i * 3] = bb.get() << 2;
                    palette[i * 3 + 1] = bb.get() << 2;
                    palette[i * 3 + 2] = bb.get() << 2;
                }
            } else if (bb.limit() == 1536) {
                for (int i = 0; i < 256; i++) {
                    palette[i * 3] = bb.get() << 2;
                    bb.get();
                    palette[i * 3 + 1] = bb.get() << 2;
                    bb.get();
                    palette[i * 3 + 2] = bb.get() << 2;
                    bb.get();
                }
            }
        }

        int rgb(int idx) {

            int r = palette[3 * idx];
            int g = palette[3 * idx + 1];
            int b = palette[3 * idx + 2];

            int rgb = (r << 24) | (g << 16) | (b << 8);
            return rgb;
        }

    }

}
