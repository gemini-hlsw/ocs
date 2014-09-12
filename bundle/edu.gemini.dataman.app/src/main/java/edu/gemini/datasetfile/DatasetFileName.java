//
// $Id: DatasetFileName.java 194 2005-10-11 17:13:49Z shane $
//

package edu.gemini.datasetfile;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A representation of the dataset file name, providing easy access to its
 * constituent parts.
 */
public final class DatasetFileName implements Comparable, Serializable {
    private static final Pattern DATASET_INDEX_PAT = Pattern.compile("([A-Z](\\d\\d\\d\\d)(\\d\\d)(\\d\\d)[A-Z])(\\d+).fits");

    private String _name;
    private String _prefix = null;
    private int _year  = -1;
    private int _month = -1;
    private int _day   = -1;

    private int _index = -1;

    /**
     * Constructs with the given file name, which much match the pattern
     * <code>([A-Z](\d\d\d\d)(\d\d)(\d\d)[A-Z])(\d+).fits</code> to be
     * considered a {@link #isValid() valid} dataset file name.
     *
     * @param name name of the file
     */
    public DatasetFileName(String name) {
        _name = name;
        Matcher mat = DATASET_INDEX_PAT.matcher(name);
        if (!mat.matches()) return;

        _prefix = mat.group(1);
        _year   = Integer.parseInt(mat.group(2));
        _month  = Integer.parseInt(mat.group(3));
        _day    = Integer.parseInt(mat.group(4));
        _index  = Integer.parseInt(mat.group(5));
    }

    private DatasetFileName(DatasetFileName base, int index) {
        if (!base.isValid()) {
            _name = base.toString();
            return;
        }
        if (index < 0) throw new IllegalArgumentException("index = " + index);

        _name   = String.format("%s%04d.fits", base.getPrefix(), index);
        _prefix = base.getPrefix();
        _year   = base.getYear();
        _month  = base.getMonth();
        _day    = base.getDay();
        _index  = index;
    }

    public boolean isValid() {
        return _prefix != null;
    }

    public int getYear() {
        return _year;
    }

    public int getMonth() {
        return _month;
    }

    public int getDay() {
        return _day;
    }

    public String getPrefix() {
        return _prefix;
    }

    public int getIndex() {
        return _index;
    }

    /**
     * Gets the next DatasetFileName in sequence after this one.
     */
    public DatasetFileName getNext() {
        return new DatasetFileName(this, _index+1);
    }

    public String toString() {
        return _name;
    }

    public boolean equals(Object o) {
        if (!(o instanceof DatasetFileName)) return false;
        DatasetFileName that = (DatasetFileName) o;
        return _name.equals(that._name);
    }

    public int hashCode() {
        return _name.hashCode();
    }

    public int compareTo(Object o) {
        DatasetFileName that = (DatasetFileName) o;
        return _index - that._index;
    }
}
