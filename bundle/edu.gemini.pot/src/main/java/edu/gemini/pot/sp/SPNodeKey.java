//
// $Id: SPNodeKey.java 46787 2012-07-17 19:23:35Z swalker $
//
package edu.gemini.pot.sp;

import java.io.Serializable;
import java.util.UUID;

/**
 * A universally unique key.  This is just a lame wrapper around
 * <code>java.util.UUID</code>>.
 */
public final class SPNodeKey implements Comparable<SPNodeKey>, Serializable {
    static final long serialVersionUID = 2L;

    public final UUID uuid;

    public SPNodeKey() {
        uuid = UUID.randomUUID();
    }

    public SPNodeKey(UUID uuid) {
        this.uuid = uuid;
    }

    public SPNodeKey(String key) {
        if (key == null) throw new NullPointerException();
        uuid = UUID.fromString(key);
    }

    public String toString() {
        return uuid.toString();
    }

    public boolean equals(Object other) {
        if (!(other instanceof SPNodeKey)) return false;
        SPNodeKey that = (SPNodeKey) other;
        return uuid.equals(that.uuid);
    }

    public int hashCode() {
        return uuid.hashCode();
    }

    public int compareTo(SPNodeKey that) {
        return uuid.compareTo(that.uuid);
    }
}
