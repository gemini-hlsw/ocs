//
// $Id: GsaDatasetInfo.java 127 2005-09-13 23:36:20Z shane $
//

package edu.gemini.dataman.gsa;

import edu.gemini.spModel.dataset.DatasetLabel;

import java.io.Serializable;

/**
 *
 */
final class GsaDatasetInfo implements Serializable {
    private static final long serialVersionUID = 1;

    private DatasetLabel _label;
    private String _filename;

    GsaDatasetInfo(DatasetLabel label, String filename) {
        if (label == null) throw new NullPointerException("label");
        if (filename == null) throw new NullPointerException("filename");

        _label    = label;
        if (!filename.endsWith(".fits")) filename += ".fits";
        _filename = filename;
    }

    public DatasetLabel getLabel() {
        return _label;
    }

    public String getFilename() {
        return _filename;
    }

    public boolean equals(Object o) {
        if (!(o instanceof GsaDatasetInfo)) return false;
        GsaDatasetInfo that = (GsaDatasetInfo) o;

        if (!_label.equals(that._label)) return false;
        return _filename.equals(that._filename);
    }

    public int hashCode() {
        return 31 * _label.hashCode() + _filename.hashCode();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(_label).append(" ").append(_filename);
        return buf.toString();
    }
}
