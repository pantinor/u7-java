package ultima7;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import java.util.concurrent.atomic.AtomicBoolean;
import ultima7.Constants.Chunk;
import ultima7.Constants.ObjectEntry;
import static ultima7.Constants.RECORDS;
import ultima7.Constants.Record;

public class ObjectRendering {

    private static final BoundingBox bb1 = new BoundingBox();
    private static final BoundingBox bb2 = new BoundingBox();
    private static final Vector3 max = new Vector3();
    private static final Vector3 min = new Vector3();

    private static final AtomicBoolean xover = new AtomicBoolean();
    private static final AtomicBoolean yover = new AtomicBoolean();
    private static final AtomicBoolean zover = new AtomicBoolean();

    private static void set(ObjectEntry e, BoundingBox bb) {
        int ex = e.tx;
        int ey = e.ty;
        float lft = e.tz * 4;
        ex += 1;
        ey += 1;
        float rx = e.currentChunk.sx * 2048 + e.currentChunk.cx * 128 + (8 * ex - 1 - lft);
        float ry = e.currentChunk.sy * 2048 + e.currentChunk.cy * 128 + (8 * ey - 1 - lft);
        min.set(rx, ry, 0);
        max.set(rx + e.frame.width, ry + e.frame.height, 0);
        bb.set(min, max);
    }

    public static void addObjectDependencies(ObjectEntry e) {
        addObjectDependencies(e, Chunk.Neighbor.SELF);
//        addObjectDependencies(e, Chunk.Neighbor.BELOW);
//        addObjectDependencies(e, Chunk.Neighbor.RIGHT);
//        addObjectDependencies(e, Chunk.Neighbor.BELOW_RIGHT);
//        addObjectDependencies(e, Chunk.Neighbor.ABOVE);
//        addObjectDependencies(e, Chunk.Neighbor.LEFT);
//        addObjectDependencies(e, Chunk.Neighbor.ABOVE_LEFT);
    }

    /**
     * Only add dependencies to non flat objects.
     *
     * @param e
     * @param neighbor
     */
    private static void addObjectDependencies(ObjectEntry e, Chunk.Neighbor neighbor) {
        Chunk targetChunk = e.currentChunk.getNeighbor(neighbor);
        if (targetChunk != null && !e.flat) {
            for (ObjectEntry dep : targetChunk.objects) {
                if (!dep.equals(e)) {
                    int cmp = compare(e, dep);
                    if (cmp == 1) {
                        if (!e.dependents.contains(dep)) {
                            e.dependents.add(dep);
                        }
                    } else if (cmp == -1) {
                        if (!dep.dependents.contains(e)) {
                            dep.dependents.add(e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Compare two objects
     *
     * @param e1
     * @param e2
     * @return -1 if 1st < 2nd, 0 if dont_care, 1 if 1st > 2nd
     */
    public static int compare(ObjectEntry e1, ObjectEntry e2) {

        if (Shapes.isRoof(e1.shapeIndex) || Shapes.isRoof(e2.shapeIndex)) {
            return 0;
        }

        set(e1, bb1);
        set(e2, bb2);

        Record rec1 = RECORDS.get(e1.shapeIndex);
        Record rec2 = RECORDS.get(e2.shapeIndex);

        int xcmp = compareRanges(bb1.min.x, bb1.max.x, bb2.min.x, bb2.max.x, xover);
        int ycmp = compareRanges(bb1.min.y, bb1.max.y, bb2.min.y, bb2.max.y, yover);
        int zcmp = compareRanges(e1.tz, e1.tz + rec1.get3dHeight(), e2.tz, e2.tz + rec2.get3dHeight(), zover);

        if (0 != xcmp && 0 != ycmp && 0 != zcmp) {
            return ((bb1.getWidth() < bb2.getWidth() && bb1.getHeight() < bb2.getHeight()) ? -1
                    : (bb1.getWidth() > bb2.getWidth() && bb2.getHeight() > bb2.getHeight()) ? 1 : 0);
        }

        if (xover.get() && yover.get() && zover.get()) {  // Complete overlap?
            if (0 != rec1.get3dHeight()) { // Flat one is always drawn first.
                return (0 != rec2.get3dHeight() ? 0 : -1);
            } else if (0 != rec2.get3dHeight()) {
                return 1;
            }
        }

        if (xcmp >= 0 && ycmp >= 0 && zcmp >= 0) {
            return 1;
        }

        if (xcmp <= 0 && ycmp <= 0 && zcmp <= 0) {
            return -1;
        }

        if (yover.get()) {// Y's overlap.
            if (xover.get()) {// X's too?
                return zcmp;
            } else if (zover.get()) { // Y's and Z's?
                return xcmp;
            } // Just Y's overlap.
            else if (0 != zcmp) {// Z's equal?
                return (xcmp);
            } else if (xcmp == zcmp) {// See if X and Z dirs. agree.
                return (xcmp);
//            } // Fixes Trinsic mayor statue-through-roof.
//            else if (inf1.ztop / 5 < inf2.zbot / 5 && inf2.info.occludes()) {
//                return (-1);   // A floor above/below.
//            } else if (inf2.ztop / 5 < inf1.zbot / 5 && inf1.info.occludes()) {
//                return 1;
            } else {
                return 0;
            }
        } else if (xover.get()) {     // X's overlap.
            if (zover.get()) {// X's and Z's?
                return (ycmp);
            } else if (0 != zcmp) {// Z's equal?
                return (ycmp);
            } else {
                return (ycmp == zcmp ? ycmp : 0);
            }
        } // Neither X nor Y overlap.
        else if (xcmp == -1) {      // o1 X before o2 X?
            if (ycmp == -1) {// o1 Y before o2 Y?
                // If Z agrees or overlaps, it's LT.
                return ((zover.get() || zcmp <= 0) ? -1 : 0);
            }
        } else if (ycmp == 1) { // o1 Y after o2 Y?
            if (zover.get() || zcmp >= 0) {
                return 1;
                //} // Fixes Brit. museum statue-through-roof.
                //else if (inf1.ztop / 5 < inf2.zbot / 5) {
                //    return (-1);   // A floor above.
            } else {
                return 0;
            }
        }
        return 0;
    }

    private static int compareRanges(float from1, float to1, float from2, float to2, AtomicBoolean overlap) {
        int cmp;
        if (to1 < from2) {
            overlap.set(false);
            cmp = -1;
        } else if (to2 < from1) {
            overlap.set(false);
            cmp = 1;
        } else {
            overlap.set(true);
            if (from1 < from2) {
                cmp = -1;
            } else if (from1 > from2) {
                cmp = 1;
            } else if (to1 < to2) {
                cmp = 1;
            } else if (to1 > to2) {
                cmp = -1;
            } else {
                cmp = 0;
            }
        }
        return cmp;
    }

}
