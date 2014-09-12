//
// $Id: DBProgramKeyAndId.java 7513 2006-12-27 15:59:27Z shane $
//

package edu.gemini.pot.spdb;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;

/**
 * Grouping of science program (or plan) node key and id.
 */
public final class DBProgramKeyAndId implements Serializable {
    private SPNodeKey key;
    private SPProgramID id;

    public DBProgramKeyAndId(SPNodeKey key, SPProgramID id) {
        if (key == null) throw new NullPointerException();
        this.key = key;
        this.id  = id;
    }

    public SPNodeKey getKey() {
        return key;
    }

    public SPProgramID getId() {
        return id;
    }

    public boolean equals(Object other) {
        if (!(other instanceof DBProgramKeyAndId)) return false;

        DBProgramKeyAndId that = (DBProgramKeyAndId) other;
        if (!key.equals(that.key)) return false;

        if (id == null) {
            return that.id == null;
        }
        //noinspection SimplifiableIfStatement
        if (that.id == null) return false;
        return id.equals(that.id);
    }

    public int hashCode() {
        int res = key.hashCode();
        if (id != null) res = 37*res + id.hashCode();
        return res;
    }

    public int compareTo(Object other) {
        DBProgramKeyAndId that = (DBProgramKeyAndId) other;

        if (id == null) {
            if (that.id != null) return -1;
        } else if (that.id == null) {
            return 1;
        } else {
            int res = id.compareTo(that.id);
            if (res != 0) return res;
        }

        return key.compareTo(that.key);
    }

}
