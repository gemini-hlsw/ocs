/**
 * $Id: SchedNote.java 6172 2005-05-23 12:57:48Z brighton $
 */

package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.SPComponentType;

/** Same as SPNote, but used for scheduling and displayed differently */
public class SchedNote extends SPNote {

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INFO_SCHEDNOTE;

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SchedNote() {
        super(SP_TYPE);
    }
}
