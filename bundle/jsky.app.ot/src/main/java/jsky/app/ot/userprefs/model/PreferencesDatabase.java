package jsky.app.ot.userprefs.model;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlException;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import jsky.util.Preferences;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A singleton "database" of user preferences.  Handles externalizing and
 * reconstituting user preferences via PIO {@link ParamSet}s.
 */
public enum PreferencesDatabase {
    instance;

    private static final Logger LOG = Logger.getLogger(PreferencesDatabase.class.getName());

    private final PioFactory factory = new PioXmlFactory();
    private ParamSet userPrefs;

    /**
     * Gets the factory used to create {@link ParamSet}s.
     */
    private PioFactory getFactory() {
        return factory;
    }

    public static File getPreferencesFile() {
        File dir = Preferences.getPreferences().getDir();
        return new File(dir, "userPreferences.xml");
    }

    private static Option<ParamSet> loadUserPreferences() {
        File f = getPreferencesFile();
        if (f.exists()) {
            try {
                return new Some<>((ParamSet) PioXmlUtil.read(f));
            } catch (PioXmlException ex) {
                LOG.log(Level.WARNING, "Could not load preferences file '" + f.getPath() + "'", ex);
            }
        }
        return None.instance();
    }

    private static boolean storeUserPreferences(ParamSet pset) {
        try {
            PioXmlUtil.write(pset, getPreferencesFile());
            return true;
        } catch (PioXmlException ex) {
            // Just log and continue.  If preferences can't be written for
            // some reason it won't be the end of the world.
            LOG.log(Level.WARNING, "Could not save preferences file '" + getPreferencesFile().getPath() + "'", ex);
            return false;
        }
    }

    private synchronized ParamSet fetchUserPreferences() {
        if (userPrefs != null) return userPrefs;

        Option<ParamSet> doc = loadUserPreferences();

        if (None.instance().equals(doc)) {
            userPrefs = getFactory().createParamSet("userPreferences");
        } else {
            userPrefs = doc.getValue();
        }
        return userPrefs;
    }

    /**
     * Clears the cached preferences.  A subsequent call to {@link #fetch} will
     * load preferences from the backing store on disk.  This method will
     * likely only be of use for testing.
     */
    public synchronized void clear() {
        userPrefs = null;
    }


    /**
     * Creates a preferences collection from the given
     * {@link ExternalizablePreferences.Factory},
     * using the named {@link ParamSet} to initialize it.  If the named
     * param set does not exist in the database, the factory must create a
     * default instance of the preferences.
     *
     * @param name name of the {@link ParamSet} in the database
     * @param factory factory used to create the user preferences
     * @return initialized user preferences corresponding to the items in the
     * {@link ParamSet} with the given <code>name</code>
     */
    public synchronized <T extends ExternalizablePreferences> T fetch(String name, ExternalizablePreferences.Factory<T> factory) {
        T res;

        ParamSet doc = fetchUserPreferences();
        ParamSet cont = doc.getParamSet(name);
        if (cont == null) {
            Option<ParamSet> empty = None.instance();
            res = factory.create(empty);
        } else {
            res = factory.create(new Some<>(cont));
        }

        return res;
    }

    /**
     * Stores the preferences to a {@link ParamSet} with the given
     * <code>name</code>.
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    public synchronized boolean store(String name, ExternalizablePreferences prefs) {
        ParamSet doc = fetchUserPreferences();
        doc.removeChild(name);
        doc.addParamSet(prefs.toParamSet(getFactory()));
        return storeUserPreferences(doc);
    }
}
