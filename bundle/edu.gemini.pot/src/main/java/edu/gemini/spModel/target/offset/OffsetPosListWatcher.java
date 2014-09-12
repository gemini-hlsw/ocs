// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//

package edu.gemini.spModel.target.offset;


import java.util.List;

/**
 * An interface supported by clients of OffsetPosList who want to
 * be notified when the list changes in some way.
 */
public interface OffsetPosListWatcher<P extends OffsetPosBase> {

    /**
     * The list has been reset, or changed so much that the client should
     * start from scratch.
     */
    void posListReset(OffsetPosList<P> opl);

    /**
     * A position has been added to the list.
     */
    void posListAddedPosition(OffsetPosList<P> opl, List<P> newPos);

    /**
     * A position has been removed from the list.
     */
    void posListRemovedPosition(OffsetPosList<P> opl, List<P> rmPos);

    /**
     * A property associated with the list has been updated.
     */
    void posListPropertyUpdated(OffsetPosList<P> opl, String propertyName, Object oldValue, Object newValue);
}
