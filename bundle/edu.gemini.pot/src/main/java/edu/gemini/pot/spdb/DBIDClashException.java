// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DBIDClashException.java 4336 2004-01-20 07:57:42Z gillies $
//

package edu.gemini.pot.spdb;


import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;

/**
 * <code>DBIDClashException</code> is thrown whenever the database detects that
 * the same ID is being used by two or more distinct programs.
 */
public final class DBIDClashException extends DBException {
    public final SPProgramID id;
    public final SPNodeKey existingKey;
    public final SPNodeKey newKey;

    private static String duplicateMessage(final SPProgramID programId, final SPNodeKey existingKey, final SPNodeKey newKey) {
        return String.format("Program ID '%s' is already in use (existing=%s, new=%s).", programId, existingKey.toString(), newKey.toString());
    }

    /**
     * Constructs with no detail message.
     */
    public DBIDClashException(final SPProgramID programId, final SPNodeKey existingKey, final SPNodeKey newKey) {
        super(duplicateMessage(programId, existingKey, newKey));
        this.id          = programId;
        this.existingKey = existingKey;
        this.newKey      = newKey;
    }
}
