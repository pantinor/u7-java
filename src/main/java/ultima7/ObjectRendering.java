package ultima7;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import ultima7.Constants.Chunk;
import ultima7.Constants.ObjectEntry;
import static ultima7.Constants.RECORDS;
import ultima7.Constants.Record;

public class ObjectRendering {

    private static final BoundingBox bb1 = new BoundingBox();
    private static final BoundingBox bb2 = new BoundingBox();
    private static final Vector3 max = new Vector3();
    private static final Vector3 min = new Vector3();

    public static void addObjectDependencies(ObjectEntry e) {
        addObjectDependencies(e, Chunk.Neighbor.SELF);
//        addObjectDependencies(e, Chunk.Neighbor.BELOW);
//        addObjectDependencies(e, Chunk.Neighbor.RIGHT);
//        addObjectDependencies(e, Chunk.Neighbor.BELOW_RIGHT);
//        addObjectDependencies(e, Chunk.Neighbor.ABOVE);
//        addObjectDependencies(e, Chunk.Neighbor.LEFT);
//        addObjectDependencies(e, Chunk.Neighbor.ABOVE_LEFT);
    }

    private static void addObjectDependencies(ObjectEntry e, Chunk.Neighbor neighbor) {
        Chunk targetChunk = e.currentChunk.getNeighbor(neighbor);
        if (targetChunk != null && !e.flat) {
            for (ObjectEntry dep : targetChunk.objects) {
                if (!dep.equals(e)) {
                    if (isDependent(e, dep)) {
                        if (!e.dependents.contains(dep)) {
                            e.dependents.add(dep);
                        }
                    }
                }
            }
        }
    }

    private static void set(ObjectEntry e, Record rec, BoundingBox bb) {
        int ex = e.tx;
        int ey = e.ty;
        int lft = e.tz * 4;

        float rx = e.currentChunk.sx * 2048 + e.currentChunk.cx * 128 + (8 * ex);
        float ry = e.currentChunk.sy * 2048 + e.currentChunk.cy * 128 + (8 * ey);

        min.set(rx, ry, lft + rec.get3dHeight());
        max.set(rx + e.frame.width, ry + e.frame.height, lft + rec.get3dHeight());

        bb.set(min, max);
    }

    private static boolean check(ObjectEntry e, Record r) {
        return r.isBuilding() || r.isDoor() || r.isUnusable();
    }

    private static boolean isDependent(ObjectEntry e1, ObjectEntry e2) {

        Record rec1 = RECORDS.get(e1.shapeIndex);
        Record rec2 = RECORDS.get(e2.shapeIndex);

        if (check(e1, rec1) && check(e2, rec2)) {
            
            set(e1, rec1, bb1);
            set(e2, rec2, bb2);

            if (bb1.intersects(bb2)) {
                return true;
            }
        }

        return false;
    }

}
