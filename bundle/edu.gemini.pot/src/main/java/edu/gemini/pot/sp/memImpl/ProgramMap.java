//
// $Id: ProgramMap.java 47005 2012-07-26 22:35:47Z swalker $
//
package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.DBIDClashException;

/**
 * Map of document data objects.  Used by the {@link MemFactory}.
 */
public interface ProgramMap {
    MemProgram getProgram(SPNodeKey docKey);
    void putProgram(MemProgram prog) throws DBIDClashException;

    MemNightlyRecord getPlan(SPNodeKey docKey);
    void putPlan(MemNightlyRecord plan) throws DBIDClashException;
}

