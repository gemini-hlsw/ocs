package edu.gemini.spModel.gemini.ghost;

import edu.gemini.pot.sp.SPComponentType;

// We need to do this because of reflection. This is a static property of instruments, but since Ghost is
// coded in Scala, values in the companion object are called as methods, which is incompatible with reflection
// on fields.
public interface GhostMixin {
    SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_GHOST;
}
