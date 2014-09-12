//
// $Id: DefaultDirLocation.java 39 2005-08-20 22:40:25Z shane $
//

package edu.gemini.dirmon.util;

import edu.gemini.dirmon.DirLocation;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * An immutable implementation of the {@link DirLocation} interface.  Contains
 * a {@link #toDictionary} method that creates the necessary service properties
 * to identify the directory that a {@link edu.gemini.dirmon.DirListener}
 * wants to watch.
 */
public final class DefaultDirLocation implements DirLocation {
    private String _dirPath;
    private String _host;

    public DefaultDirLocation(String dirPath) {
        if (dirPath == null) throw new NullPointerException("dirPath is null");
        this._dirPath = dirPath;
    }

    public DefaultDirLocation(String dirPath, String hostAddress) {
        this(dirPath);
        _host = hostAddress;
    }

    public DefaultDirLocation(DirLocation loc) {
        this(loc.getDirPath(), loc.getHostAddress());
    }

    public DefaultDirLocation(Dictionary<String, String> dict) {
        _host    = dict.get(HOST_PROP);
        _dirPath = dict.get(DIR_PATH_PROP);
        if (_dirPath == null) {
            throw new IllegalArgumentException("dirPath is missing");
        }
    }

    public String getDirPath() {
        return _dirPath;
    }

    public String getHostAddress() {
        return _host;
    }

    /**
     * Creates a Dictionary suitable for use as the service properties with
     * which a {@link edu.gemini.dirmon.DirListener} is registered.
     */
    public Dictionary toDictionary() {
        Dictionary<String, Object> dict = new Hashtable<String, Object>();

        if (_host != null) dict.put(HOST_PROP, _host);
        dict.put(DIR_PATH_PROP, _dirPath);

        return dict;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DefaultDirLocation)) return false;

        DefaultDirLocation that = (DefaultDirLocation) o;

        if (_host == null) {
            if (that._host != null) return false;
        } else if (that._host == null) {
            return false;
        } else {
            if (!_host.equals(that._host)) return false;
        }

        return _dirPath.equals(that._dirPath);
    }

    public int hashCode() {
        int res = 37;
        if (_host != null) {
            res = _host.hashCode();
        }
        res = 37*res + _dirPath.hashCode();
        return res;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        if (_host == null) {
            buf.append("localhost");
        } else {
            buf.append(_host);
        }
        buf.append(':').append(_dirPath);

        return buf.toString();
    }
}
