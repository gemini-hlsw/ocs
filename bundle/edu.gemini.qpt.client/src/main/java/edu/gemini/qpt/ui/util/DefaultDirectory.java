package edu.gemini.qpt.ui.util;

import java.io.File;
import java.util.prefs.Preferences;

public class DefaultDirectory {

    private static final String KEY = DefaultDirectory.class.getSimpleName() + ".path";
    
    public static File get() {
        Preferences prefs = Preferences.userNodeForPackage(DefaultDirectory.class);    
        String path = prefs.get(KEY, System.getProperty("user.home"));
        return new File(path);
    }
    
    public static void set(File file) {
        Preferences prefs = Preferences.userNodeForPackage(DefaultDirectory.class);
        if (file == null) {
            prefs.remove(KEY);
        } else {
            prefs.put(KEY, file.isDirectory() ? file.getPath() : file.getParent());
        }
    }
    
}
