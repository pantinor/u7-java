package ultima7;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class MapRender {

    private static List<Palette> palettes = new ArrayList<>();
    private static List<Xform> xforms = new ArrayList<>();
    private static final String STATICDIR = "c://Users//panti/Desktop//ULTIMA7//STATIC//";
    private static final String GAMEDATDIR = "c://Users//panti/Desktop//ULTIMA7//GAMEDAT//";

    public static void main(String[] args) throws Exception {

        FileInputStream cis = new FileInputStream(STATICDIR + "U7CHUNKS");
        FileInputStream mis = new FileInputStream(STATICDIR + "U7MAP");
        FileInputStream sis = new FileInputStream(STATICDIR + "SHAPES.VGA");
        FileInputStream pis = new FileInputStream(STATICDIR + "PALETTES.FLX");
        FileInputStream tis = new FileInputStream(STATICDIR + "TFA.DAT");
        FileInputStream xis = new FileInputStream(STATICDIR + "XFORM.TBL");

        ByteBuffer cbb = ByteBuffer.wrap(IOUtils.toByteArray(cis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer mbb = ByteBuffer.wrap(IOUtils.toByteArray(mis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer sbb = ByteBuffer.wrap(IOUtils.toByteArray(sis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer pbb = ByteBuffer.wrap(IOUtils.toByteArray(pis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xbb = ByteBuffer.wrap(IOUtils.toByteArray(xis)).order(ByteOrder.LITTLE_ENDIAN);
        byte[] tfa = IOUtils.toByteArray(tis);

        Region[][] regions = new Region[12][12];
        int[][][][] chunkIds = new int[12][12][16][16];

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int id = read2(mbb);
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

                        int chunkId = chunkIds[yy][xx][y][x];

                        int offset = chunkId * 512;

                        Shape[][] shapes = new Shape[16][16];
                        chunks[y][x] = new Chunk(chunkId, shapes);

                        for (int tiley = 0; tiley < 16; tiley++) {
                            for (int tilex = 0; tilex < 16; tilex++) {

                                int shapeIndex = (cbb.get(offset + 0) & 0xff) + 256 * (cbb.get(offset + 1) & 3);
                                int frameIndex = (cbb.get(offset + 1) >> 2) & 0x1f;

                                Shape s = new Shape(shapeIndex, frameIndex);
                                shapes[tiley][tilex] = s;

                                offset += 2;
                            }
                        }
                    }
                }

                getIRegObjects(region, "U7IREG");//these files do not exist until after game data is saved
                getFixedObjects(region, "U7IFIX");

            }
        }

        xbb.position(0x80);
        for (int i = 0; i < 20; i++) {
            int offset = read4(xbb);
            int len = read4(xbb);
            int pos = xbb.position();
            Xform xform = new Xform(i);
            xbb.position(offset);
            xbb.get(xform.outputIndices);
            xforms.add(xform);
            xbb.position(pos);
        }

        pbb.position(0x80);
        for (int i = 0; i < 8; i++) {
            int offset = read4(pbb);
            int len = read4(pbb);
            int pos = pbb.position();
            byte[] entry = new byte[len];
            pbb.position(offset);
            pbb.get(entry);
            Palette p = new Palette(i, entry);
            palettes.add(p);
            pbb.position(pos);
        }

        sbb.position(0x54);
        int numRecords = read4(sbb);

        List<Record> records = new ArrayList<>();

        sbb.position(0x80);
        for (int i = 0; i < numRecords; i++) {
            int offset = read4(sbb);
            int len = read4(sbb);
            byte[] entry = new byte[len];

            Record rec = new Record(i, offset, len, entry);
            records.add(rec);

            rec.tfa[0] = tfa[i * 3];
            rec.tfa[1] = tfa[i * 3 + 1];
            rec.tfa[2] = tfa[i * 3 + 2];

            rec.dims[0] = 1 + (rec.tfa[2] & 7);
            rec.dims[1] = 1 + ((rec.tfa[2] >> 3) & 7);
            rec.dims[2] = rec.tfa[0] >> 5;
        }

        for (Record rec : records) {
            sbb.position(rec.offset);
            sbb.get(rec.data);
            rec.set();
            System.out.println(rec);
        }

        if (true) {
            return;
        }

        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.minWidth = 8;
        settings.minHeight = 8;
        settings.maxWidth = 512;
        settings.maxHeight = 2056;
        settings.paddingX = 0;
        settings.paddingY = 0;
        settings.fast = true;
        settings.pot = false;
        settings.grid = true;
        settings.edgePadding = false;
        settings.bleed = false;
        settings.debug = false;
        settings.alias = false;

//        packAtlas(0, 149, "base-tiles", records, settings);
//        packAtlas(150, 300, "shape-tiles-1", records, settings);
//        packAtlas(301, 600, "shape-tiles-2", records, settings);
        packAtlas(700, 730, "shape-tiles-3", records, settings);
//        packAtlas(900, 1024, "shape-tiles-4", records, settings);
        if (true) {
            return;
        }

        BufferedImage mapImage = new BufferedImage(2048 * 6, 2048 * 6, BufferedImage.TYPE_INT_ARGB);

        for (int yy = 0, y = 0; yy < 6; yy++, y++) {
            for (int xx = 0, x = 0; xx < 6; xx++, x++) {
                Region region = regions[yy][xx];
                drawFixed(region, records);
                drawObjects(region, records);
                mapImage.getGraphics().drawImage(region.bi, x * 2048, y * 2048, null);
            }
        }
        ImageIO.write(mapImage, "PNG", new File("target/regions-nw.png"));

        for (int yy = 0, y = 0; yy < 6; yy++, y++) {
            for (int xx = 6, x = 0; xx < 12; xx++, x++) {
                Region region = regions[yy][xx];
                drawFixed(region, records);
                drawObjects(region, records);
                mapImage.getGraphics().drawImage(region.bi, x * 2048, y * 2048, null);
            }
        }
        ImageIO.write(mapImage, "PNG", new File("target/regions-ne.png"));

        for (int yy = 6, y = 0; yy < 12; yy++, y++) {
            for (int xx = 0, x = 0; xx < 6; xx++, x++) {
                Region region = regions[yy][xx];
                drawFixed(region, records);
                drawObjects(region, records);
                mapImage.getGraphics().drawImage(region.bi, x * 2048, y * 2048, null);
            }
        }
        ImageIO.write(mapImage, "PNG", new File("target/regions-sw.png"));

        for (int yy = 6, y = 0; yy < 12; yy++, y++) {
            for (int xx = 6, x = 0; xx < 12; xx++, x++) {
                Region region = regions[yy][xx];
                drawFixed(region, records);
                drawObjects(region, records);
                mapImage.getGraphics().drawImage(region.bi, x * 2048, y * 2048, null);
            }
        }
        ImageIO.write(mapImage, "PNG", new File("target/regions-se.png"));

    }

    private static void drawFixed(Region region, List<Record> records) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Chunk chunk = region.chunks[y][x];
                for (int tiley = 0; tiley < 16; tiley++) {
                    for (int tilex = 0; tilex < 16; tilex++) {
                        Shape shape = chunk.shapes[tiley][tilex];
                        Record rec = records.get(shape.shapeIndex);

                        try {
                            if (!rec.isRawChunkBits()) {
                                Shape tmps = chunk.shapes[tiley][tilex == 0 ? tilex + 1 : tilex - 1];
                                Record tmprec = records.get(tmps.shapeIndex);
                                //HACK - draw a random square underneath it first (tile to left)
                                region.bi.getGraphics().drawImage(tmprec.frames[tmps.frameIndex].bi, (16 * 8 * x) + (8 * tilex), (16 * 8 * y) + (8 * tiley), null);
                            }

                            region.bi.getGraphics().drawImage(rec.frames[shape.frameIndex].bi,
                                    (16 * 8 * x) + (8 * tilex) - rec.frames[shape.frameIndex].width + 8,
                                    (16 * 8 * y) + (8 * tiley) - rec.frames[shape.frameIndex].height + 8, null);
                        } catch (Exception e) {
                            //ignore - must be a bug in the shapes vga for shape number 48
                            System.err.printf("drawing [%d] [%d,%d] si [%d] fidx [%d] flen [%d]\n",
                                    region.id, y, x, shape.shapeIndex, shape.frameIndex, rec.frames.length);
                        }
                    }
                }
            }
        }
    }

    private static void drawObjects(Region region, List<Record> records) {

        int[] loc = new int[2];

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                Chunk chunk = region.chunks[y][x];

                if (!chunk.objects.isEmpty()) {
                    for (int i = chunk.objects.size() - 1; i >= 0; i--) {
                        ObjectEntry e = chunk.objects.get(i);
                        Record rec = records.get(e.shapeIndex);

                        //skip roofs etc above ground level
                        if (e.tz > 1) {
                            continue;
                        }

                        if (e.frameIndex >= rec.frames.length) {
                            System.err.printf("error drawing object [%d] [%d,%d] si [%d] fidx [%d] flen [%d]\n",
                                    region.id, y, x, e.shapeIndex, e.frameIndex, rec.frames.length);
                            continue;
                        }

                        //System.err.printf("%d,%d,%d,%d,%d,%d,%d,%d,%d\n", region.id, y, x, e.shapeIndex, e.tx, e.ty, e.tz, loc[0], loc[1]);
                        region.bi.getGraphics().drawImage(rec.frames[e.frameIndex].bi,
                                //loc[0], loc[1], null);
                                (16 * 8 * x) + (8 * e.tx) - rec.frames[e.frameIndex].width + 8,
                                (16 * 8 * y) + (8 * e.ty) - rec.frames[e.frameIndex].height + 8, null);
                    }
                }
            }
        }
    }

    private static class Region {

        int id;
        Chunk[][] chunks;
        BufferedImage bi = new BufferedImage(16 * 16 * 8, 16 * 16 * 8, BufferedImage.TYPE_INT_ARGB);

        public Region(int id, Chunk[][] chunks) {
            this.id = id;
            this.chunks = chunks;
        }

    }

    private static class Chunk {

        int id;
        Shape[][] shapes;
        List<ObjectEntry> objects = new ArrayList<>();

        public Chunk(int id, Shape[][] shapes) {
            this.id = id;
            this.shapes = shapes;
        }

    }

    private static class Shape {

        int shapeIndex;
        int frameIndex;

        public Shape(int shapeIndex, int frameIndex) {
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

    private static class Record {

        int num;
        int offset;
        int len;
        byte[] data;
        ByteBuffer bb;
        Frame[] frames;

        int[] tfa = new int[3];
        int[] dims = new int[3];

        public Record(int num, int offset, int len, byte[] data) {
            this.num = num;
            this.offset = offset;
            this.len = len;
            this.data = data;
            this.bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        }

        @Override
        public String toString() {
            //return String.format("Record %d offset [%d] len [%d] %s shapeSize [%d] offset count [%d] frames [%s] hgt [%d]",
            //        this.num, this.offset, this.len, isRawChunkBits() ? "RAW" : "SHP", shapeSize(), offsetCount(), frames != null ? frames.length : this.len / 8 * 8, dims[2]);

            return String.format("Record %d [%s] isField [%s] isAnimated [%s] isSolid [%s] is Water [%s] isDoor [%s] isLightSource [%s] frames[%d]",
                    this.num, isRawChunkBits() ? "RAW" : "SHP", isField(), isAnimated(), isSolid(), isWater(), isDoor(), isLightSource(), frames != null ? frames.length : this.len / 8 * 8);
        }

        void set() {

            int nframes;

            if (!isRawChunkBits()) {
                nframes = offsetCount();

                if (nframes > 0) {

                    this.frames = new Frame[nframes];

                    int[] offsets = new int[nframes];
                    for (int n = 0; n < nframes; n++) {
                        offsets[n] = read4(this.bb, n * 4 + 4);
                    }

                    for (int n = 0; n < nframes; n++) {

                        this.bb.position(offsets[n]);

                        int xright = bb.getShort();
                        int xleft = bb.getShort();
                        int yabove = bb.getShort();
                        int ybelow = bb.getShort();

                        int width = xright + xleft + 1;
                        int height = yabove + ybelow + 1;

                        this.frames[n] = new Frame(n, width, height);

                        while (true) {
                            int scanData = bb.getShort();
                            int scanLen = scanData >> 1;
                            int encoded = scanData & 1;

                            if (scanData == 0) {
                                break;
                            }

                            int offsetX = bb.getShort();
                            int offsetY = bb.getShort();

                            int pixPtr = (yabove + offsetY) * width + (xleft + offsetX);
                            this.frames[n].pixels.position(pixPtr);

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

            int transparent = 0;

            for (int f = 0; f < nframes; f++) {

                this.frames[f].pixels.position(0);
                int[][] data = new int[this.frames[f].height][this.frames[f].width];

                for (int y = 0; y < this.frames[f].height; y++) {
                    for (int x = 0; x < this.frames[f].width; x++) {
                        int idx = this.frames[f].pixels.get() & 0xff;
                        data[y][x] = idx;
                    }
                }

                for (int y = 0; y < this.frames[f].height; y++) {
                    for (int x = 0; x < this.frames[f].width; x++) {
                        int idx = data[y][x];
                        int rgb = palettes.get(PALETTE_DAY).rgb(idx);
                        if (!isRawChunkBits() && idx == transparent && ImageTransparency.checkAllAdjacentsAreTransparent(data, y, x, transparent)) {
                            rgb = 0;
                        }

//                        if (!isRawChunkBits()) {
//                            rgb = palettes.get(PALETTE_DAY).rgb(idx, 3);
//                        }
                        this.frames[f].bi.setRGB(x, y, rgb);
                    }
                }

            }

        }

        boolean isRawChunkBits() {
            return this.len != shapeSize();
        }

        int shapeSize() {
            return read4(this.bb, 0);
        }

        int offsetCount() {
            return (read4(this.bb, 4) - 4) / 4;
        }

        boolean isAnimated() {
            return (tfa[0] & (1 << 2)) != 0;
        }

        boolean isSolid() {
            return (tfa[0] & (1 << 3)) != 0;
        }

        boolean isWater() {
            return (tfa[0] & (1 << 4)) != 0;
        }

        boolean isPoisonous() {
            return (tfa[1] & (1 << 4)) != 0;
        }

        boolean isField() {
            return (tfa[1] & (1 << 4)) != 0;
        }

        boolean isDoor() {
            return (tfa[1] & (1 << 5)) != 0;
        }

        boolean isTransparent() {
            return (tfa[1] & (1 << 7)) != 0;
        }

        boolean isLightSource() {
            return (tfa[2] & (1 << 6)) != 0;
        }

        boolean isTranslucent() {
            return (tfa[2] & (1 << 7)) != 0;
        }

    }

    public static int PALETTE_DAY = 0;
    public static int PALETTE_DAWN = 1;
    public static int PALETTE_NIGHT = 2;
    public static int PALETTE_INVISIBLE = 3;
    public static int PALETTE_OVERCAST = 4;
    public static int PALETTE_FOG = 5;
    public static int PALETTE_SPELL = 6;
    public static int PALETTE_CANDLE = 7;
    public static int PALETTE_RED = 8;
    public static int PALETTE_LIGHTNING = 10;
    public static int PALETTE_SINGLE_LIGHT = 11;
    public static int PALETTE_MANY_LIGHTS = 12;

    private static class Palette {

        int num;
        byte[] data = new byte[768];
        int[] palette = new int[768];

        public Palette(int num, byte[] data) {
            this.num = num;
            this.data = data;

            for (int i = 0; i < 256; i++) {
                palette[i * 3] = data[i * 3] << 2;
                palette[i * 3 + 1] = data[i * 3 + 1] << 2;
                palette[i * 3 + 2] = data[i * 3 + 2] << 2;
            }
        }

        int rgb(int idx, int xformIndex) {
            Xform xf = xforms.get(xformIndex);
            int ix = xf.outputIndices[idx] & 0xff;

            int r = palette[3 * ix];
            int g = palette[3 * ix + 1];
            int b = palette[3 * ix + 2];

            int rgb = 255 << 24 | (r << 16) | (g << 8) | (b << 0);
            return rgb;
        }

        int rgb(int idx) {

            int r = palette[3 * idx];
            int g = palette[3 * idx + 1];
            int b = palette[3 * idx + 2];

            int rgb = 255 << 24 | (r << 16) | (g << 8) | (b << 0);
            return rgb;
        }

    }

    private static class Xform {

        int num;
        byte[] outputIndices = new byte[256];

        public Xform(int num) {
            this.num = num;
        }
    }

    private static void getFixedObjects(Region region, String prefix) throws Exception {

        String chars = "0123456789abcdef";
        String fname = (prefix + chars.charAt(region.id / 16) + chars.charAt(region.id % 16)).toUpperCase();

        FileInputStream is = new FileInputStream((STATICDIR) + fname);
        ByteBuffer bb = ByteBuffer.wrap(IOUtils.toByteArray(is)).order(ByteOrder.LITTLE_ENDIAN);

        bb.position(0x54);
        int num = read4(bb);

        if (num < 1) {
            return;
        }

        ObjectEntries[] entries = new ObjectEntries[num];

        bb.position(0x80);
        for (int i = 0; i < num; i++) {
            int offset = read4(bb);
            int len = read4(bb);
            if (len > 0) {
                byte[] data = new byte[len];
                ObjectEntries obj = new ObjectEntries(i, offset, len, data, true);
                entries[i] = obj;
            }
        }

        for (ObjectEntries obj : entries) {
            if (obj == null) {
                continue;
            }
            bb.position(obj.offset);
            bb.get(obj.data);
            obj.set();
        }

        for (int cy = 0; cy < 16; cy++) {
            for (int cx = 0; cx < 16; cx++) {
                int idx = cy * 16 + cx;
                ObjectEntries obj = entries[idx];

                if (obj == null) {
                    continue;
                }

                Chunk chunk = region.chunks[cy][cx];

                if (obj.entries != null && !obj.entries.isEmpty()) {
                    chunk.objects.addAll(obj.entries);
                }

                //System.out.printf("%s - Region [%d] chunk [%d,%d] objects %d\n ", (fixed ? "IFIX" : "IREG"), region.id, cy, cx, chunk.objects.size());
            }
        }

    }

    private static void getIRegObjects(Region region, String prefix) throws Exception {

        String chars = "0123456789abcdef";
        String fname = (prefix + chars.charAt(region.id / 16) + chars.charAt(region.id % 16)).toUpperCase();

        if (Files.notExists(Paths.get(GAMEDATDIR + fname))) {
            return;
        }

        FileInputStream is = new FileInputStream(GAMEDATDIR + fname);
        ByteBuffer bb = ByteBuffer.wrap(IOUtils.toByteArray(is)).order(ByteOrder.LITTLE_ENDIAN);

        //System.out.printf("%s - Region [%d] %s\n", fname, region.id, bb.toString());
        while (bb.position() < bb.limit()) {

            int len = bb.get() & 0xff;

            if (len != 6 && len != 12) {
                continue;
            }

            if (bb.position() + len >= bb.limit()) {
                break;
            }

            byte b0 = bb.get();
            byte b1 = bb.get();
            byte b2 = bb.get();
            byte b3 = bb.get();

            int cx = (b0 >> 4) & 0xf;
            int cy = (b1 >> 4) & 0xf;

            Chunk chunk = region.chunks[cy][cx];

            ObjectEntry e = new ObjectEntry();
            e.tx = b0 & 0xf;
            e.ty = b1 & 0xf;
            e.shapeIndex = (b2 & 0xff) + 256 * (b3 & 3);
            e.frameIndex = (b3 >> 2) & 0x1f;
            e.fixed = false;

            chunk.objects.add(e);

            if (len == 6) {
                bb.get();//todo
            } else if (len == 12) {
                bb.getInt();//todo
                bb.get();//todo
                bb.get();//todo
                bb.get();//todo
            }
        }

    }

    private static class ObjectEntry {

        int tx;
        int ty;
        int tz;
        int shapeIndex;
        int frameIndex;
        boolean fixed;
    }

    private static class ObjectEntries {

        int num;
        int offset;
        int len;
        byte[] data;
        boolean fixed;
        ByteBuffer bb;
        List<ObjectEntry> entries;

        public ObjectEntries(int num, int offset, int len, byte[] data, boolean fixed) {
            this.num = num;
            this.offset = offset;
            this.len = len;
            this.data = data;
            this.fixed = fixed;
            this.bb = ByteBuffer.wrap(data);
            this.bb.order(ByteOrder.LITTLE_ENDIAN);
        }

        void set() {
            if (this.fixed) {
                int cnt = len / 4;
                this.entries = new ArrayList<>();
                for (int i = 0; i < cnt; i++) {
                    byte b0 = bb.get();
                    byte b1 = bb.get();
                    byte b2 = bb.get();
                    byte b3 = bb.get();

                    ObjectEntry e = new ObjectEntry();
                    e.tx = (b0 >> 4) & 0xf;
                    e.ty = b0 & 0xf;
                    e.tz = b1 & 0xf;
                    e.shapeIndex = (b2 & 0xff) + 256 * (b3 & 3);
                    e.frameIndex = (b3 >> 2) & 0x1f;
                    e.fixed = true;

                    entries.add(e);
                }
            }

        }

    }

    private static int read4(ByteBuffer bb) {
        int ret = read4(bb, bb.position());
        bb.getInt();
        return ret;
    }

    private static int read4(ByteBuffer bb, int idx) {
        return (bb.get(idx + 0) & 0xff) << 0
                | (bb.get(idx + 1) & 0xff) << 8
                | (bb.get(idx + 2) & 0xff) << 16
                | (bb.get(idx + 3) & 0xff) << 24;
    }

    private static int read2(ByteBuffer bb) {
        int ret = read2(bb, bb.position());
        bb.getShort();
        return ret;
    }

    private static int read2(ByteBuffer bb, int idx) {
        return (bb.get(idx + 0) & 0xff) << 0
                | (bb.get(idx + 1) & 0xff) << 8;
    }

    private static void packAtlas(int start, int end, String atlasName, List<Record> records, TexturePacker.Settings settings) {
        TexturePacker tp = new TexturePacker(settings);
        for (Record rec : records) {
            if (rec.num < start || rec.num > end) {
                continue;
            }
            System.out.printf("Packing images for %d\n", rec.num);
            for (Frame f : rec.frames) {
                String name = String.format("shape-%d_%d", rec.num, f.number);
                tp.addImage(f.bi, name);
            }
        }
        tp.pack(new File("target/"), atlasName);
    }

}
