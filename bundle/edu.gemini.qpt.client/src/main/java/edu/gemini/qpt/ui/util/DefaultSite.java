package edu.gemini.qpt.ui.util;

import edu.gemini.spModel.core.Site;

import java.util.prefs.Preferences;

public class DefaultSite {

    private static final String KEY = DefaultSite.class.getSimpleName() + ".name";
    
    public static Site get() {
        Preferences prefs = Preferences.userNodeForPackage(DefaultSite.class);
        String name = prefs.get(KEY, Site.GS.name());
        try {
            return Site.valueOf(name);
        } catch (IllegalArgumentException iae) {
            return Site.GS;
        }
    }
    
    public static void set(Site site) {
        Preferences prefs = Preferences.userNodeForPackage(DefaultSite.class);
        if (site == null) {
            prefs.remove(KEY);
        } else {
            prefs.put(KEY, site.name());
        }
    }
    
}
