// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: MemNightlyRecord.java 47005 2012-07-26 22:35:47Z swalker $
//

package edu.gemini.pot.sp.memImpl;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.spModel.core.SPProgramID;

import java.util.*;

public final class MemNightlyRecord extends MemAbstractBase implements ISPNightlyRecord {

    public static MemNightlyRecord create(SPNodeKey key, SPProgramID progId, UUID databaseId) {
        if (key == null) key = new SPNodeKey();
        return new MemNightlyRecord(new NightlyRecordData(key, progId, databaseId, LifespanId.random()));
    }

    public static MemNightlyRecord rename(ISPNightlyRecord that, SPNodeKey key, SPProgramID progId, UUID databaseId) {
        if (key == null) key = new SPNodeKey();
        return new MemNightlyRecord(new NightlyRecordData(key, progId, databaseId, LifespanId.random()), that);
    }

    private MemNightlyRecord(NightlyRecordData data) {
        super(data, data.getDocumentKey());
    }

    private MemNightlyRecord(NightlyRecordData data, ISPNightlyRecord plan)  {
        super(data, plan, false);
    }

    /** Returns <code>null</code>. */
    @Override public ISPProgram getProgram() {
        return null;
    }

    public long lastModified() {
        return getDocumentData().lastModified();
    }
}

