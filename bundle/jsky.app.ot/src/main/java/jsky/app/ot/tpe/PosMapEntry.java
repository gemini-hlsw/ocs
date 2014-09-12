// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: PosMapEntry.java 18053 2009-02-20 20:16:23Z swalker $
//
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

