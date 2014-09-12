package edu.gemini.pot.util;

import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.memImpl.MemFactory;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.SPNodeInitializerConfigReader;
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

    /**
     * Default location of the program node initializer configuration file
     * within the POT resources configuration area.
     */
    public static final String INIT_CONFIG_FILE = "spdb-initializer.conf";

    // Disallow instances
    private POTUtil() { }

    public static final Map<String, String> propMap;

    static {
        final URL url = POTUtil.class.getResource(INIT_CONFIG_FILE);
        if (url == null) throw new RuntimeException("Missing " + INIT_CONFIG_FILE);
        BufferedInputStream bis = null;
        try {
            final URLConnection con = url.openConnection();
            con.setUseCaches(false);
            bis = new BufferedInputStream(con.getInputStream());
            final Properties props = new Properties();
            props.load(bis);

            final Map<String, String> tmp = new HashMap<String, String>();
            for (String p : props.stringPropertyNames()) tmp.put(p, props.getProperty(p));
            propMap = Collections.unmodifiableMap(tmp);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load " + INIT_CONFIG_FILE, ex);
        } finally {
            if (bis != null) try { bis.close(); } catch (Exception ex) {/*ignore*/ }
        }
    }

    private static Map<String, ISPNodeInitializer> initMap;

    public static Map<String, ISPNodeInitializer> getInitializerMap() {
        synchronized (POTUtil.class) {
            if (initMap != null) return initMap;
        }

        final Map<String, ISPNodeInitializer> m = SPNodeInitializerConfigReader.loadInitializers(propMap);
        if (m == null) {
            LOG.severe("Could not load factory initializers.");
            throw new RuntimeException("Could not load factory initializers");
        }

        synchronized (POTUtil.class) {
            if (initMap == null) initMap = m;
        }
        return m;
    }

    public static ISPFactory createFactory(UUID uuid) {
        final Map<String, ISPNodeInitializer> initMap = getInitializerMap();
        final MemFactory fact = new MemFactory(uuid);
        SPNodeInitializerConfigReader.applyConfig(fact, initMap);

        // Set the creatable components
        final List<SPComponentType> ocl;
        ocl = SPNodeInitializerConfigReader.getCreatableObsComponents(initMap.keySet());
        fact.setCreatableObsComponents(ocl);

        final List<SPComponentType> scl;
        scl = SPNodeInitializerConfigReader.getCreatableSeqComponents(initMap.keySet());
        fact.setCreatableSeqComponents(scl);

        return fact;
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
