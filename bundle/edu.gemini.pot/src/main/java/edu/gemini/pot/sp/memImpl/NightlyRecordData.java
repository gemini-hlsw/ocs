// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: NightlyRecordData.java 46998 2012-07-26 15:52:22Z swalker $
//

package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;
import java.util.UUID;

/**
 * This implementation class holds data that should be associated with
 * every node in a nightly plan.
 *
 * @author Kim Gillies
 */
class NightlyRecordData extends DocumentData implements Serializable {
    NightlyRecordData(SPNodeKey progKey, SPProgramID progId, UUID uuid, LifespanId lifespanId) {
        super(progKey, progId, uuid, lifespanId);
    }
}
