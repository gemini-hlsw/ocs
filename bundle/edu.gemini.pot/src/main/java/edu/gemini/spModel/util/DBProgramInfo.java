// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DBProgramInfo.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.util;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;

import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * Struct containing basic identifying information about ISPRootNodes.
 */
public class DBProgramInfo implements Serializable, Comparable {
    private static final long serialVersionUID = 2L;

    public final SPNodeKey nodeKey;
    public final String programName;
    public final SPProgramID programID;
    public final long size;
    public final long timestamp;

    public DBProgramInfo(SPNodeKey key, String name, SPProgramID progId, long size, long timestamp) {
        if (key == null) throw new IllegalArgumentException("node key cannot be null");
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        this.nodeKey     = key;
        this.programName = name;
        this.programID   = progId;
        this.size        = size;
        this.timestamp   = timestamp;
    }

    @Deprecated
    public String getProgramIDAsString() {
        return deprecatedProgIdAsString(null);
    }

    @Deprecated
    public String getProgramIDAsString(String def) {
        return deprecatedProgIdAsString(def);
    }

    private String deprecatedProgIdAsString(String def) {
        return programID != null ? programID.toString() : def;
    }

    public int compareTo(Object o) {
        if (o instanceof DBProgramInfo)
            return programName.compareTo(((DBProgramInfo) o).programName);
        return 0;
    }

    private static final Pattern PLAN_PATTERN =
            Pattern.compile("G[NS]-PLAN\\d\\d\\d\\d\\d\\d\\d\\d");

    public boolean isNightlyPlan() {
        if (programID == null) return false;
        Matcher match = PLAN_PATTERN.matcher(programID.stringValue());
        return match.matches();
    }


    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(programID);
        buf.append(" (").append(nodeKey).append(") ");
        buf.append(new Date(timestamp)).append(' ');
        buf.append(programName);
        return buf.toString();
    }

}

