package jsky.app.ot.tpe;

import edu.gemini.spModel.target.WatchablePos;

import java.awt.geom.Point2D;

/**
 * An implementation class that groups an (x,y) screen position and a TaggedPos object.
 */
public final class PosMapEntry<T extends WatchablePos> {
    public Point2D.Double screenPos;
    public T taggedPos;

    public PosMapEntry(Point2D.Double p, T tp) {
        screenPos = p;
        taggedPos = tp;
    }

    public String toString() {
        return getClass().getName() + "[" + screenPos + "," + taggedPos + "]";
    }
}

