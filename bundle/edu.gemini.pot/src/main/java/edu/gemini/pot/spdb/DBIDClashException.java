// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DBIDClashException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.spdb;


import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;

/**
 * <code>DBIDClashException</code> is thrown whenever the database detects that
 * the same node key or ID is being used by two or more distinct programs.
 */
public final class DBIDClashException extends DBException {
    public final SPNodeKey key;
    public final SPProgramID id;

    private static String duplicateMessage(SPProgramID programId) {
        if (programId != null) {
            return String.format("Program ID '%s' is already in use.", programId);
        } else {
            return String.format("Program is already in the database.");
        }
    }

    /**
     * Constructs with no detail message.
     */
    public DBIDClashException(SPNodeKey key, SPProgramID programId) {
        super(duplicateMessage(programId));
        if (programId == null) throw new IllegalArgumentException("Missing id.");
        this.key = key;
        this.id = programId;
    }
}
