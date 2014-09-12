// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqRepeatOffset.java 18053 2009-02-20 20:16:23Z swalker $
//

package edu.gemini.spModel.gemini.seqcomp;

import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.pot.sp.SPComponentType;


/**
 * An iterator for telescope offset positions.  It maintains a position
 * list that details the sequence of offset positions and implements the
 * elements() method to Enumerate them.
 *
 * @see edu.gemini.spModel.target.offset.OffsetPosList
 */
public class SeqRepeatOffset extends SeqRepeatOffsetBase<OffsetPos> {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_OFFSET;

    private static final String VERSION = "2009B-1";

    /**
     * Default constructor.
     */
    public SeqRepeatOffset() {
        super(SP_TYPE, OffsetPos.FACTORY);
        setVersion(VERSION);
    }
}
