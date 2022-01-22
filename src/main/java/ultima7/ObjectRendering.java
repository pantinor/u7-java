package ultima7;

import ultima7.Constants.Chunk;
import ultima7.Constants.Chunk.Neighbor;
import ultima7.Constants.Frame;
import ultima7.Constants.ObjectEntry;
import ultima7.Constants.Record;
import static ultima7.Constants.RECORDS;
import static ultima7.Constants.TILE_DIM;

public class ObjectRendering {

    private static final OrderingInfo ordInfo1 = new OrderingInfo();
    private static final OrderingInfo ordInfo2 = new OrderingInfo();

    private static final Rectangle ordArea1 = new Rectangle();
    private static final Rectangle ordArea2 = new Rectangle();

    private static class OrderingInfo {

        Rectangle area;
        Record info;
        int tx, ty, tz;
        int xs, ys, zs;
        int xleft, xright, ynear, yfar, zbot, ztop;

        void set(ObjectEntry obj, Rectangle a) {
            area = a;
            info = RECORDS.get(obj.shapeIndex);
            tx = obj.tx;
            ty = obj.ty;
            tz = obj.tz;
            int frnum = obj.frameIndex;
            xs = info.get3dXtiles(frnum);
            ys = info.get3dYtiles(frnum);
            zs = info.get3dHeight();
            xleft = tx - xs + 1;
            xright = tx;
            yfar = ty - ys + 1;
            ynear = ty;
            ztop = tz + zs - 1;
            zbot = tz;
            if (zs == 0) {
                zbot--;
            }
        }

    }

    private static OrderingInfo getOrderingInfo1(ObjectEntry obj) {
        ordInfo1.set(obj, getShapeRect(ordArea1, obj));
        return ordInfo1;
    }

    private static OrderingInfo getOrderingInfo2(ObjectEntry obj, Rectangle a) {
        ordInfo2.set(obj, a);
        return ordInfo2;
    }

    public static void addObjectEntry(Chunk chunk, ObjectEntry obj) {

        OrderingInfo ord = getOrderingInfo1(obj);

        chunk.objects.add(obj);

        if (obj.tz > 0 || ord.info.get3dHeight() > 0) {

            addDependencies(chunk, obj, ord);

            if (chunk.fromBelow > 0) {
                addOutsideDependencies(chunk, Neighbor.BELOW, obj, ord);
            }
            if (chunk.fromRight > 0) {
                addOutsideDependencies(chunk, Neighbor.RIGHT, obj, ord);
            }
            if (chunk.fromBelowRight > 0) {
                addOutsideDependencies(chunk, Neighbor.BELOW_RIGHT, obj, ord);
            }

            boolean ext_left = (obj.tx - ord.xs) < 0 && chunk.cx > 0;
            boolean ext_above = (obj.ty - ord.ys) < 0 && chunk.cy > 0;
            Chunk neighbor = null;

            if (ext_left) {
                neighbor = addOutsideDependencies(chunk, Neighbor.LEFT, obj, ord);
                if (neighbor != null) {
                    neighbor.fromRight++;
                }
                if (ext_above) {
                    neighbor = addOutsideDependencies(chunk, Neighbor.ABOVE_LEFT, obj, ord);
                    if (neighbor != null) {
                        neighbor.fromBelowRight++;
                    }
                }
            }

            if (ext_above) {
                neighbor = addOutsideDependencies(chunk, Neighbor.ABOVE, obj, ord);
                if (neighbor != null) {
                    neighbor.fromBelow++;
                }
            }

        }

    }

    private static Chunk addOutsideDependencies(Chunk chunk, Neighbor direction, ObjectEntry newobj, OrderingInfo newinfo) {
        Chunk neighborChunk = chunk.getNeighbor(direction);
        if (neighborChunk != null) {
            addDependencies(neighborChunk, newobj, newinfo);
        }
        return neighborChunk;
    }

    private static void addDependencies(Chunk chunk, ObjectEntry newObj, OrderingInfo newInfo) {
        for (ObjectEntry obj : chunk.objects) {
            int newcmp = compare(newInfo, obj);
            int cmp = newcmp == -1 ? 1 : newcmp == 1 ? 0 : -1;
            if (cmp == 0) {
                newObj.dependencies.add(obj);
                obj.dependors.add(newObj);
            } else if (cmp == 1) {
                obj.dependencies.add(newObj);
                newObj.dependors.add(obj);
            }
        }
    }

    private static int compare(OrderingInfo inf1, ObjectEntry obj2) {

        Rectangle r2 = getShapeRect(ordArea2, obj2);
        if (!inf1.area.intersects(r2)) {
            return 0;
        }

        OrderingInfo inf2 = getOrderingInfo2(obj2, r2);
        int xcmp, ycmp, zcmp;

        boolean xover, yover, zover;
        
        xcmp = compareRanges(inf1.xleft, inf1.xright, inf2.xleft, inf2.xright);
        xover = (xcmp & 0x100) != 0;
        xcmp = (xcmp & 0xff) - 1;
        ycmp = compareRanges(inf1.yfar, inf1.ynear, inf2.yfar, inf2.ynear);
        yover = (ycmp & 0x100) != 0;
        ycmp = (ycmp & 0xff) - 1;
        zcmp = compareRanges(inf1.zbot, inf1.ztop, inf2.zbot, inf2.ztop);
        zover = (zcmp & 0x100) != 0;
        zcmp = (zcmp & 0xff) - 1;
        
        if (xcmp == 0 && ycmp == 0 && zcmp == 0) {
            return (inf1.area.w < inf2.area.w
                    && inf1.area.h < inf2.area.h) ? -1
                            : (inf1.area.w > inf2.area.w
                            && inf1.area.h > inf2.area.h) ? 1 : 0;
        }

        if (xover & yover & zover) {
            if (inf1.zs == 0) {
                return inf2.zs == 0 ? 0 : -1;
            } else if (inf2.zs == 0) {
                return 1;
            }
        }
        
        if (xcmp >= 0 && ycmp >= 0 && zcmp >= 0) {
            return 1;
        }
        
        if (xcmp <= 0 && ycmp <= 0 && zcmp <= 0) {
            return -1;
        }
        
        if (yover) {
            if (xover) {
                return zcmp;
            } else if (zover) {
                return xcmp;
            } else if (zcmp == 0) {
                return xcmp;
            } else if (xcmp == zcmp) {
                return xcmp;
            } else {
                return 0;
            }
        } else if (xover) {
            if (zover) {
                return ycmp;
            } else if (zcmp == 0) {
                return ycmp;
            } else {
                return ycmp == zcmp ? ycmp : 0;
            }
        } else if (xcmp == -1) {
            if (ycmp == -1) {
                return (zover || zcmp <= 0) ? -1 : 0;
            }
        } else if (ycmp == 1) {
            if (zover || zcmp >= 0) {
                return 1;
            } else if (inf1.ztop / 5 < inf2.zbot / 5) {
                return -1;
            } else {
                return 0;
            }
        }
        return 0;
    }

    private static int compareRanges(int from1, int to1, int from2, int to2) {
        byte cmp, overlap;
        if (to1 < from2) {
            overlap = 0;
            cmp = 0;
        } else if (to2 < from1) {
            overlap = 0;
            cmp = 2;
        } else {
            overlap = 1;
            if (from1 < from2) {
                cmp = 0;
            } else if (from1 > from2) {
                cmp = 2;
            } else if (to1 - from1 < to2 - from2) {
                cmp = 2;
            } else if (to1 - from1 > to2 - from2) {
                cmp = 0;
            } else {
                cmp = 1;
            }
        }
        return (overlap << 8) | cmp;
    }

    private static Rectangle getShapeRect(Rectangle r, ObjectEntry obj) {
        int lftpix = 4 * obj.tz;
        int otx = obj.tx;
        int oty = obj.ty;
        otx += 1;
        oty += 1;
        return getShapeRect(r, obj.frame, otx * TILE_DIM - 1 - lftpix, oty * TILE_DIM - 1 - lftpix);
    }

    private static Rectangle getShapeRect(Rectangle r, Frame fr, int x, int y) {
        if (fr != null) {
            r.set(x - fr.xleft, y - fr.yabove, fr.width, fr.height);
        }
        return r;
    }

    private static class Rectangle {

        public int x, y, w, h;

        public Rectangle(int xin, int yin, int win, int hin) {
            x = xin;
            y = yin;
            w = win;
            h = hin;
        }

        public Rectangle(Rectangle r) {
            x = r.x;
            y = r.y;
            w = r.w;
            h = r.h;
        }

        public Rectangle() {
            x = y = w = h = -1;
        }

        public final void set(int xx, int yy, int ww, int hh) {
            x = xx;
            y = yy;
            w = ww;
            h = hh;
        }

        public final void set(Rectangle r) {
            x = r.x;
            y = r.y;
            w = r.w;
            h = r.h;
        }

        @Override
        public String toString() {
            return "Rect(" + x + "," + y + "," + w + "," + h + ")";
        }

        public final boolean hasPoint(int px, int py) {
            return (px >= x && px < x + w && py >= y && py < y + h);
        }

        public final void enlarge(int left, int right, int top, int bottom, int maxw, int maxh) {
            x -= left;
            w += left + right;
            y -= top;
            h += top + bottom;

            if (x < 0) {
                w += x;
                x = 0;
            }
            if (y < 0) {
                h += y;
                y = 0;
            }

            if (x + w > maxw) {
                w = maxw - x;
            }
            if (y + h > maxh) {
                h = maxh - y;
            }
        }

        public void shift(int deltax, int deltay) {
            x += deltax;
            y += deltay;
        }

        public final void enlarge(int delta) {
            x -= delta;
            y -= delta;
            w += 2 * delta;
            h += 2 * delta;
        }

        public final int distance(int px, int py) {	// Get distance from a point (max.
            //   dist. along x or y coord.)
            int xdist = px <= x ? (x - px) : (px - x - w + 1);
            int ydist = py <= y ? (y - py) : (py - y - h + 1);
            int dist = xdist > ydist ? xdist : ydist;
            return dist < 0 ? 0 : dist;
        }
        // Does it intersect another?

        public boolean intersects(Rectangle r2) {
            return (x >= r2.x + r2.w ? false : r2.x >= x + w ? false
                    : y >= r2.y + r2.h ? false : r2.y >= y + h ? false : true);
        }
        //	Intersect another with this.

        public final void intersect(Rectangle r2) {
            int xend = x + w, yend = y + h;
            int xend2 = r2.x + r2.w, yend2 = r2.y + r2.h;
            x = x >= r2.x ? x : r2.x;
            y = y >= r2.y ? y : r2.y;
            w = (xend <= xend2 ? xend : xend2) - x;
            h = (yend <= yend2 ? yend : yend2) - y;
        }

        public final void add(Rectangle r2) {
            int xend = x + w, yend = y + h;
            int xend2 = r2.x + r2.w, yend2 = r2.y + r2.h;
            x = x < r2.x ? x : r2.x;
            y = y < r2.y ? y : r2.y;
            w = (xend > xend2 ? xend : xend2) - x;
            h = (yend > yend2 ? yend : yend2) - y;
        }

        public final boolean equals(Rectangle r2) {
            return x == r2.x && y == r2.y && w == r2.w && h == r2.h;
        }
    }

}
