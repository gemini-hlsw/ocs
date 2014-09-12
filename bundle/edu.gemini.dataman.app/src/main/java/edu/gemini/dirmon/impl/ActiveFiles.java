//
// $Id: ActiveFiles.java 199 2005-10-13 19:30:37Z shane $
//

package edu.gemini.dirmon.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;

/**
 * Small collection of Files that are believed to be active. In other words,
 * that should be checked for updates often.
 */
final class ActiveFiles {
    private LinkedHashMap<String, File> _fileMap;
    private final File _dir;
    private final int _cap;

    ActiveFiles(File dir, int capacity) {
        _dir = dir;
        _cap = capacity;

        float loadFactor = (float) 0.75;
        int mapCap = Math.round(capacity / loadFactor);
        _fileMap = new LinkedHashMap<String, File>(mapCap, loadFactor, true) {
            protected boolean removeEldestEntry(Map.Entry<String, File> me) {
                return size() > _cap;
            }
        };
    }

    public int getCapacity() {
        return _cap;
    }

    /**
     * Adds a file to the active set, possibily replacing an older file.
     * Whether <code>file</code> already exists in the active set already or
     * not, it will become the "newest" active file (i.e., the last to be
     * removed when the capacity of the active set is met.
     */
    synchronized void addFile(String filename) {
        _fileMap.put(filename, new File(_dir, filename));

//        System.out.println("\n\nAdding '" + filename + "' to active files.");
//        System.out.println(toString());
    }

    /**
     * Copies the active set into the given collection.
     */
    synchronized void copyActiveFilesTo(Collection<File> col) {
        col.addAll(_fileMap.values());
    }

    public synchronized String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append('[');
        Iterator<String> it = _fileMap.keySet().iterator();
        if (it.hasNext()) buf.append(it.next());
        while (it.hasNext()) {
            buf.append(", ").append(it.next());
        }
        buf.append(']');
        return buf.toString();
    }
}
