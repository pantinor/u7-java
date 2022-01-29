package ultima7;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.apache.commons.io.IOUtils;

public class Constants {

    public static final List<Palette> palettes = new ArrayList<>();
    public static final List<Xform> xforms = new ArrayList<>();

    public static final String STATICDIR = "c://Users//panti/Desktop//ULTIMA7//STATIC//";
    public static final String GAMEDATDIR = "c://Users//panti/Desktop//ULTIMA7//GAMEDAT//";

    public static final int TILE_DIM = 8;
    public static final int MAP_TILE_WIDTH = (2048 * 12) / TILE_DIM;
    public static final int MAP_TILE_HEIGHT = (2048 * 12) / TILE_DIM;

    public static final Region[][] REGIONS = new Region[12][12];
    public static final List<Record> RECORDS = new ArrayList<>();

    public static enum Direction {
        NORTH, EAST, SOUTH, WEST;
    }

    public static void init() throws Exception {

        FileInputStream cis = new FileInputStream(STATICDIR + "U7CHUNKS");
        FileInputStream mis = new FileInputStream(STATICDIR + "U7MAP");
        FileInputStream sis = new FileInputStream(STATICDIR + "SHAPES.VGA");
        FileInputStream pis = new FileInputStream(STATICDIR + "PALETTES.FLX");
        FileInputStream tis = new FileInputStream(STATICDIR + "TFA.DAT");
        FileInputStream xis = new FileInputStream(STATICDIR + "XFORM.TBL");
        FileInputStream occis = new FileInputStream(STATICDIR + "OCCLUDE.DAT");

        ByteBuffer cbb = ByteBuffer.wrap(IOUtils.toByteArray(cis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer mbb = ByteBuffer.wrap(IOUtils.toByteArray(mis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer sbb = ByteBuffer.wrap(IOUtils.toByteArray(sis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer pbb = ByteBuffer.wrap(IOUtils.toByteArray(pis)).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xbb = ByteBuffer.wrap(IOUtils.toByteArray(xis)).order(ByteOrder.LITTLE_ENDIAN);
        byte[] tfa = IOUtils.toByteArray(tis);
        byte[] occludes = IOUtils.toByteArray(occis);

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

        sbb.position(0x80);
        for (int i = 0; i < numRecords; i++) {
            int offset = read4(sbb);
            int len = read4(sbb);
            byte[] entry = new byte[len];

            Record rec = new Record(i, offset, len, entry);
            RECORDS.add(rec);

            rec.tfa[0] = tfa[i * 3];
            rec.tfa[1] = tfa[i * 3 + 1];
            rec.tfa[2] = tfa[i * 3 + 2];

            rec.dims[0] = (1 + (tfa[2] & 7));
            rec.dims[1] = (1 + ((tfa[2] >> 3) & 7));
            rec.dims[2] = ((tfa[0] >> 5) & 7);
        }

        for (Record rec : RECORDS) {
            sbb.position(rec.offset);
            sbb.get(rec.data);
            rec.set();
            //System.out.println(rec);
        }

        for (int i = 0; i < occludes.length; i++) {
            byte bits = occludes[i];
            int shnum = i * 8;
            BitSet bitset = BitSet.valueOf(new byte[]{bits});
            for (int b = 0; b < 8; b++) {
                if (bitset.get(b)) {
                    RECORDS.get(shnum + b).occludes = true;
                }
            }
        }

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
                Region region = new Region(yy * 12 + xx);
                REGIONS[yy][xx] = region;
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int chunkId = chunkIds[yy][xx][y][x];
                        int offset = chunkId * 512;
                        Shape[][] shapes = new Shape[16][16];
                        region.chunks[y][x] = new Chunk(xx, yy, x, y, chunkId, shapes);
                        for (int tiley = 0; tiley < 16; tiley++) {
                            for (int tilex = 0; tilex < 16; tilex++) {
                                int shapeIndex = (cbb.get(offset + 0) & 0xff) + 256 * (cbb.get(offset + 1) & 3);
                                int frameIndex = (cbb.get(offset + 1) >> 2) & 0x1f;
                                Shape s = new Shape(shapeIndex, frameIndex);
                                shapes[tiley][tilex] = s;
                                offset += 2;

                                Record r = RECORDS.get(s.shapeIndex);
                                if (!r.isRawChunkBits()) {
                                    ObjectEntry e = new ObjectEntry();
                                    e.tx = tilex;
                                    e.ty = tiley;
                                    e.tz = 0;
                                    e.shapeIndex = s.shapeIndex;
                                    e.frameIndex = s.frameIndex;
                                    e.frame = r.frames[e.frameIndex];
                                    e.currentChunk = region.chunks[y][x];
                                    region.chunks[y][x].objects.add(e);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {
                Region region = REGIONS[yy][xx];
                getIRegObjects(region, "U7IREG");//these files do not exist until after game data is saved
                getFixedObjects(region, "U7IFIX");
            }
        }

        for (int yy = 0; yy < 12; yy++) {
            for (int xx = 0; xx < 12; xx++) {
                Region region = REGIONS[yy][xx];
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        Chunk c = region.chunks[y][x];
                        if (!c.objects.isEmpty()) {
                            for (ObjectEntry e : c.objects) {
                                ObjectRendering.addObjectDependencies(e);
                            }
                        }
                    }
                }
            }
        }

//        for (int yy = 0; yy < 12; yy++) {
//            for (int xx = 0; xx < 12; xx++) {
//                Region region = REGIONS[yy][xx];
//                for (int y = 0; y < 16; y++) {
//                    for (int x = 0; x < 16; x++) {
//                        Chunk c = region.chunks[y][x];
//                        System.out.printf("Region [%d] chunk [%d,%d] objects %d\n ", region.id, y, x, c.objects.size());
//                        if (!c.objects.isEmpty()) {
//                            for (ObjectEntry e : c.objects) {
//                                System.out.println("\t" + e.toString());
//                            }
//                        }
//                    }
//                }
//            }
//        }

    }

    public static class Region {

        int id;
        Chunk[][] chunks = new Chunk[16][16];

        public Region(int id) {
            this.id = id;
        }

    }

    public static class Chunk {

        public static enum Neighbor {
            SELF, ABOVE, ABOVE_LEFT, ABOVE_RIGHT, LEFT, RIGHT, BELOW, BELOW_RIGHT, BELOW_LEFT
        };

        int sx;
        int sy;
        int cx;
        int cy;

        int id;
        Shape[][] shapes;

        final List<ObjectEntry> objects = new ArrayList<>();

        public Chunk(int sx, int sy, int cx, int cy, int id, Shape[][] shapes) {
            this.sx = sx;
            this.sy = sy;
            this.cx = cx;
            this.cy = cy;
            this.id = id;
            this.shapes = shapes;
        }

        public Chunk getNeighbor(Neighbor direction) {

            Chunk neighborChunk = null;

            try {
                switch (direction) {
                    case ABOVE: {
                        if (cy == 0) {
                            neighborChunk = REGIONS[sy - 1][sx].chunks[15][cx];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy - 1][cx];
                        }
                        break;
                    }
                    case ABOVE_LEFT: {
                        if (cy == 0 && cx == 0) {
                            neighborChunk = REGIONS[sy - 1][sx - 1].chunks[15][15];
                        } else if (cy == 0 && cx > 0) {
                            neighborChunk = REGIONS[sy - 1][sx].chunks[15][cx - 1];
                        } else if (cy > 0 && cx == 0) {
                            neighborChunk = REGIONS[sy][sx - 1].chunks[cy - 1][15];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy - 1][cx - 1];
                        }
                        break;
                    }
                    case ABOVE_RIGHT: {
                        if (cy == 0 && cx == 15) {
                            neighborChunk = REGIONS[sy - 1][sx + 1].chunks[15][0];
                        } else if (cy == 0 && cx < 15) {
                            neighborChunk = REGIONS[sy - 1][sx].chunks[15][cx + 1];
                        } else if (cy < 15 && cx == 15) {
                            neighborChunk = REGIONS[sy][sx + 1].chunks[cy - 1][0];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy - 1][cx + 1];
                        }
                        break;
                    }
                    case LEFT: {
                        if (cx == 0) {
                            neighborChunk = REGIONS[sy][sx - 1].chunks[cy][15];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy][cx - 1];
                        }
                        break;
                    }
                    case RIGHT: {
                        if (cx == 15) {
                            neighborChunk = REGIONS[sy][sx + 1].chunks[cy][0];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy][cx + 1];
                        }
                        break;
                    }
                    case BELOW: {
                        if (cy == 15) {
                            neighborChunk = REGIONS[sy + 1][sx].chunks[0][cx];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy - 1][cx];
                        }
                        break;
                    }
                    case BELOW_RIGHT: {
                        if (cy == 15 && cx == 15) {
                            neighborChunk = REGIONS[sy + 1][sx + 1].chunks[0][0];
                        } else if (cy == 15 && cx < 15) {
                            neighborChunk = REGIONS[sy + 1][sx].chunks[0][cx + 1];
                        } else if (cy < 15 && cx == 15) {
                            neighborChunk = REGIONS[sy][sx + 1].chunks[cy + 1][0];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy + 1][cx + 1];
                        }
                        break;
                    }
                    case BELOW_LEFT: {
                        if (cy == 15 && cx == 0) {
                            neighborChunk = REGIONS[sy + 1][sx - 1].chunks[0][15];
                        } else if (cy == 15 && cx > 0) {
                            neighborChunk = REGIONS[sy][sx - 1].chunks[0][cx - 1];
                        } else if (cy < 15 && cx == 0) {
                            neighborChunk = REGIONS[sy][sx - 1].chunks[cy + 1][15];
                        } else {
                            neighborChunk = REGIONS[sy][sx].chunks[cy + 1][cx - 1];
                        }
                        break;
                    }
                    case SELF:
                        neighborChunk = this;
                        break;
                }

            } catch (Exception e) {
                //nothing
            }

            return neighborChunk;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 17 * hash + this.sx;
            hash = 17 * hash + this.sy;
            hash = 17 * hash + this.cx;
            hash = 17 * hash + this.cy;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Chunk other = (Chunk) obj;
            if (this.sx != other.sx) {
                return false;
            }
            if (this.sy != other.sy) {
                return false;
            }
            if (this.cx != other.cx) {
                return false;
            }
            return this.cy == other.cy;
        }

        @Override
        public String toString() {
            return "Chunk{" + "sx=" + sx + ", sy=" + sy + ", cx=" + cx + ", cy=" + cy + ", id=" + id + '}';
        }

    }

    public static class Shape {

        int shapeIndex;
        int frameIndex;

        public Shape(int shapeIndex, int frameIndex) {
            this.shapeIndex = shapeIndex;
            this.frameIndex = frameIndex;
        }

    }

    public static class Frame {

        int number;
        int width;
        int height;

        int xright;
        int xleft;
        int yabove;
        int ybelow;

        Pixmap bi;
        TextureRegion texture;
        ByteBuffer pixels;

        public Frame(int number, int xright, int xleft, int yabove, int ybelow) {
            this.xright = xright;
            this.xleft = xleft;
            this.yabove = yabove;
            this.ybelow = ybelow;
            this.number = number;
            this.width = xright + xleft + 1;
            this.height = yabove + ybelow + 1;
            this.bi = new Pixmap(width, height, Format.RGBA8888);
            this.pixels = ByteBuffer.allocate(width * height);
        }

        public Frame(int number, int width, int height) {
            this.number = number;
            this.width = width;
            this.height = height;
            this.bi = new Pixmap(width, height, Format.RGBA8888);
            this.pixels = ByteBuffer.allocate(width * height);
        }

    }

    public static class Record {

        int num;
        int offset;
        int len;
        byte[] data;
        ByteBuffer bb;
        Frame[] frames;
        boolean occludes;
        Animation anim;

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
            return String.format("Record %d [%s] isField [%s] isAnimated [%s] isSolid [%s] is Water [%s] isDoor [%s] isLightSource [%s] hgt [%d] frames[%d]",
                    this.num, isRawChunkBits() ? "RAW" : "SHP", isField(), isAnimated(), isSolid(), isWater(), isDoor(), isLightSource(), dims[2],
                    frames != null ? frames.length : this.len / 8 * 8);
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

                        this.frames[n] = new Frame(n, xright, xleft, yabove, ybelow);

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

                        if (!isRawChunkBits() && idx == transparent && checkAllAdjacentsAreTransparent(data, y, x, transparent)) {
                            rgb = 0;
                        }

                        this.frames[f].bi.drawPixel(x, y, rgb);
                    }
                }

                this.frames[f].texture = new TextureRegion(new Texture(this.frames[f].bi));

                this.frames[f].pixels = null;
            }

            if (this.isAnimated()) {
                Array<TextureRegion> arr = new Array<>();
                for (int f = 0; f < nframes; f++) {
                    arr.add(this.frames[f].texture);
                }
                this.anim = new Animation(.3f, arr);
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

        boolean occludes() {
            return this.occludes;
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

        public int getShapeClass() {
            return (int) (tfa[1] & 15);
        }

        public boolean isBuilding() {
            int c = getShapeClass();
            return c == Shapes.building;
        }

        public boolean isNpc() {
            int c = getShapeClass();
            return c == Shapes.human || c == Shapes.monster;
        }

        public int get3dXtiles(int framenum) {
            return dims[(framenum >> 5) & 1];
        }

        public int get3dYtiles(int framenum) {
            return dims[1 ^ ((framenum >> 5) & 1)];
        }

        public int get3dHeight() {
            return dims[2];
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

    public static class Palette {

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

            int rgb8888 = (r << 24) | (g << 16) | (b << 8) | 255;
            return rgb8888;
        }

        int rgb(int idx) {

            int r = palette[3 * idx];
            int g = palette[3 * idx + 1];
            int b = palette[3 * idx + 2];

            int rgb8888 = (r << 24) | (g << 16) | (b << 8) | 255;
            return rgb8888;
        }

    }

    public static class Xform {

        int num;
        byte[] outputIndices = new byte[256];

        public Xform(int num) {
            this.num = num;
        }
    }

    public static void getFixedObjects(Region region, String prefix) throws Exception {

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
                ObjectEntries obje = entries[idx];

                if (obje == null) {
                    continue;
                }

                Chunk chunk = region.chunks[cy][cx];

                for (ObjectEntry obj : obje.entries) {
                    obj.currentChunk = chunk;
                    chunk.objects.add(obj);
                }

            }
        }

    }

    public static int IREG_EXTENDED = 254;
    public static int IREG_SPECIAL = 255;
    public static int IREG_UCSCRIPT = 1;
    public static int IREG_ENDMARK = 2;
    public static int IREG_ATTS = 3;
    public static int IREG_STRING = 4;

    public static void getIRegObjects(Region region, String prefix) throws Exception {

        String chars = "0123456789abcdef";
        String fname = (prefix + chars.charAt(region.id / 16) + chars.charAt(region.id % 16)).toUpperCase();

        if (Files.notExists(Paths.get(GAMEDATDIR + fname))) {
            return;
        }

        FileInputStream is = new FileInputStream(GAMEDATDIR + fname);
        ByteBuffer bb = ByteBuffer.wrap(IOUtils.toByteArray(is)).order(ByteOrder.LITTLE_ENDIAN);

        //System.out.printf("%s - Region [%d] %s\n", fname, region.id, bb.toString());
        while (bb.position() < bb.limit()) {

            int index_id = -1;
            boolean extended = false;

            int len = bb.get() & 0xff;

            if (len == 2) {
                index_id = read2(bb);
                continue;
            }

            if (len == IREG_SPECIAL) {
                //todo
                continue;
            }

            if (len == IREG_EXTENDED) {
                extended = true;
                len = bb.get() & 0xff;
            }

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

            if (extended) {
                //e.shapeIndex = ((int) b2 & 0xff) + 256 * ((int) b3 & 0xff);
                //e.frameIndex = (int) bb.get() & 0xff;
            } else {
                //e.shapeIndex = ((int) b2 & 0xff) + 256 * ((int) b3 & 3);
                //e.frameIndex = ((int) b3 & 0xff) >> 2;
            }

            e.shapeIndex = (b2 & 0xff) + 256 * (b3 & 3);
            e.frameIndex = (b3 >> 2) & 0x1f;
            e.currentChunk = chunk;

            Record rec = RECORDS.get(e.shapeIndex);

            if (e.frameIndex < rec.frames.length) {
                e.frame = rec.frames[e.frameIndex];
            } else {
                //System.out.println("IREG Got object entry with incorrect frame " + e + " " + rec.frames.length);
                e.frame = rec.frames[0];
            }

            if (len == 6) {
                byte b = bb.get();
                e.tz = (b >> 4) & 0xf;
            } else if (len == 12) {
                bb.getInt();//todo
                bb.get();//todo
                byte b = bb.get();
                e.tz = (b >> 4) & 0xf;
                bb.get();//todo
            }

            chunk.objects.add(e);

        }

    }

    public static class ObjectEntry {

        public int tx;
        public int ty;
        public int tz;
        public int shapeIndex;
        public int frameIndex;
        public Frame frame;
        public Chunk currentChunk; //current chunk that this object is in

        public List<ObjectEntry> dependents = new ArrayList<>();

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.tx;
            hash = 79 * hash + this.ty;
            hash = 79 * hash + this.tz;
            hash = 79 * hash + this.shapeIndex;
            hash = 79 * hash + this.frameIndex;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ObjectEntry other = (ObjectEntry) obj;
            if (this.tx != other.tx) {
                return false;
            }
            if (this.ty != other.ty) {
                return false;
            }
            if (this.tz != other.tz) {
                return false;
            }
            if (this.shapeIndex != other.shapeIndex) {
                return false;
            }
            return this.frameIndex == other.frameIndex;
        }

        @Override
        public String toString() {
            return "ObjectEntry{" + "tx=" + tx + ", ty=" + ty + ", tz=" + tz + ", shapeIndex=" + shapeIndex + ", frameIndex=" + frameIndex + ", dependents=" + dependents.size() + '}';
        }

    }

    public static class ObjectEntries {

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

                    Record rec = RECORDS.get(e.shapeIndex);

                    e.frame = rec.frames[e.frameIndex];

                    entries.add(e);
                }
            }

        }

    }

    public static int read4(ByteBuffer bb) {
        int ret = read4(bb, bb.position());
        bb.getInt();
        return ret;
    }

    public static int read4(ByteBuffer bb, int idx) {
        return (bb.get(idx + 0) & 0xff) << 0
                | (bb.get(idx + 1) & 0xff) << 8
                | (bb.get(idx + 2) & 0xff) << 16
                | (bb.get(idx + 3) & 0xff) << 24;
    }

    public static int read2(ByteBuffer bb) {
        int ret = read2(bb, bb.position());
        bb.getShort();
        return ret;
    }

    public static int read2(ByteBuffer bb, int idx) {
        return (bb.get(idx + 0) & 0xff) << 0
                | (bb.get(idx + 1) & 0xff) << 8;
    }

    private static boolean checkAllAdjacentsAreTransparent(int[][] data, int y, int x, int transparent) {

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

    public static Texture getTexture(Color color, int w, int h) {
        Pixmap red = new Pixmap(w, h, Format.RGBA8888);
        red.setColor(color);
        red.fillRectangle(0, 0, w, h);
        Texture t = new Texture(red);
        red.dispose();
        return t;
    }

}
