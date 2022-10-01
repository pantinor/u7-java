package ultima7;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import java.util.concurrent.atomic.AtomicBoolean;
import ultima7.Constants.Chunk;
import ultima7.Constants.ObjectEntry;
import static ultima7.Constants.RECORDS;
import ultima7.Constants.Record;

public class ObjectRendering {

    private static final int LESS = -1;
    private static final int GREATER = 1;
    private static final int SAME = 0;

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

//                    if (isDependent(e, dep)) {
//                        if (!e.dependents.contains(dep)) {
//                            e.dependents.add(dep);
//                        }
//                    }
                    if (compare(e, dep) == GREATER) {
                        if (!e.dependents.contains(dep)) {
                            e.dependents.add(dep);
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
                return (0 != rec2.get3dHeight() ? SAME : LESS);
            } else if (0 != rec2.get3dHeight()) {
                return GREATER;
            }
        }

        if (xcmp >= 0 && ycmp >= 0 && zcmp >= 0) {
            return GREATER;
        }

        if (xcmp <= 0 && ycmp <= 0 && zcmp <= 0) {
            return LESS;
        }

        if (yover.get()) {
            
            if (xover.get()) {
                return zcmp;
            } else if (zover.get()) {
                return xcmp;
            } else if (0 != zcmp) {
                return (xcmp);
            } else {
                return (xcmp == zcmp ? xcmp : SAME);
            }
            
        } else if (xover.get()) {
            
            if (zover.get()) {
                return (ycmp);
            } else if (0 != zcmp) {
                return (ycmp);
            } else {
                return (ycmp == zcmp ? ycmp : SAME);
            }
            
        } else if (xcmp == LESS) {
            
            if (ycmp == LESS) {
                return ((zover.get() || zcmp <= 0) ? -1 : SAME);
            }
            
        } else if (ycmp == GREATER) {
            
            if (zover.get() || zcmp >= 0) {
                return GREATER;
            } else {
                return SAME;
            }
        }
        
        return SAME;
    }

    /**
     * Is e2 dependent on e1
     *
     * @param e1
     * @param e2
     * @return
     */
    public static boolean isDependent(ObjectEntry e1, ObjectEntry e2) {

        //only allow flat RLEs as dependents
        if (!e2.flat) {
            return false;
        }

        if (Shapes.isRoof(e1.shapeIndex) || Shapes.isRoof(e2.shapeIndex)) {
            return false;
        }

        set(e1, bb1);
        set(e2, bb2);

        int xcmp = compareRanges(bb1.min.x, bb1.max.x, bb2.min.x, bb2.max.x, xover);
        int ycmp = compareRanges(bb1.min.y, bb1.max.y, bb2.min.y, bb2.max.y, yover);

        if (xover.get() && xcmp == GREATER) {
            return true;
        }

        if (yover.get() && ycmp == GREATER) {
            return true;
        }

        return false;
    }

    /**
     * Compare two bounded boxes in the dimension provided
     *
     * @return -1 if 1st < 2nd, 0 if same, 1 if 1st > 2nd
     */
    private static int compareRanges(float min1, float max1, float min2, float max2, AtomicBoolean overlap) {

        if (max1 < min2) {
            overlap.set(false);
            return LESS;
        } else if (max2 < min1) {
            overlap.set(false);
            return GREATER;
        } else {

            if (max1 == min2) {
                overlap.set(false);
                return LESS;
            }

            if (min1 == max2) {
                overlap.set(false);
                return GREATER;
            }

            if (min1 < min2) {
                overlap.set(true);
                return LESS;
            }

            if (min1 > min2) {
                overlap.set(true);
                return GREATER;
            }

            return SAME;

        }
    }

}
