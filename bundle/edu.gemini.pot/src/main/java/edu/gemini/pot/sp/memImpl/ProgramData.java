// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ProgramData.java 46846 2012-07-19 20:30:56Z swalker $
//

package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.spModel.core.SPProgramID;

import java.util.UUID;

/**
 * This implementation class holds data that should be associated with
 * every node in a program.
 */
class ProgramData extends DocumentData {

    // The index of the next observation created in this program.
    private int _nextObsNumber;

    ProgramData(SPNodeKey progKey, SPProgramID progId, UUID uuid, LifespanId lifespanId) {
        super(progKey, progId, uuid, lifespanId);
    }

    int nextObsNumber() {
        getProgramWriteLock();
        try {
            return ++_nextObsNumber;
        } finally {
            returnProgramWriteLock();
        }
    }

    // make sure _nextObsNumber >= n
    void updateNextObsNumber(int n) {
        if (n > _nextObsNumber) {
            getProgramWriteLock();
            try {
                _nextObsNumber = n;
            } finally {
                returnProgramWriteLock();
            }
        }
    }
}
