// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
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

    // The highest index of the any observation ever created in this program.
    private int _maxObsNumber;

    ProgramData(SPNodeKey progKey, SPProgramID progId, UUID uuid, LifespanId lifespanId) {
        super(progKey, progId, uuid, lifespanId);
    }

    int nextObsNumber() {
        getProgramWriteLock();
        try {
            return ++_maxObsNumber;
        } finally {
            returnProgramWriteLock();
        }
    }

    // make sure _maxObsNumber >= n
    void updateMaxObsNumber(int n) {
        getProgramWriteLock();
        try {
            _maxObsNumber = Math.max(n, _maxObsNumber);
        } finally {
            returnProgramWriteLock();
        }
    }
}
