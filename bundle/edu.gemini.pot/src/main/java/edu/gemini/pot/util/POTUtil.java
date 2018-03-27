package edu.gemini.pot.util;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.memImpl.MemFactory;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility code useful across the pot package.
 */
public final class POTUtil {
    private static final Logger LOG = Logger.getLogger(POTUtil.class.getName());

    // Disallow instances
    private POTUtil() { }

    public static ISPFactory createFactory(UUID uuid) {
        return new MemFactory(uuid);
    }

    public static boolean isUsedId(SPProgramID id, IDBDatabaseService db) {
        return (db.lookupProgramByID(id) != null) || (db.lookupNightlyRecordByID(id) != null);
    }

    private static SPProgramID getUnusedProgramId(String base, IDBDatabaseService db, int index) {
        final String idStr = base + "-copy" + index;
        final SPProgramID newId;
        try {
            newId = SPProgramID.toProgramID(idStr);
        } catch (SPBadIDException e) {
            throw new RuntimeException(idStr);
        }
        return isUsedId(newId, db) ? getUnusedProgramId(base, db, index + 1) : newId;
    }

    private static final Pattern COPY_PATTERN = Pattern.compile("(.+)-copy(\\d+)$");

    public static SPProgramID getUnusedProgramId(SPProgramID id, IDBDatabaseService db) {
        final Matcher m = COPY_PATTERN.matcher(id.stringValue());
        return m.matches() ?
                getUnusedProgramId(m.group(1), db, Integer.parseInt(m.group(2))) :
                getUnusedProgramId(id.stringValue(), db, 1);
    }
}
