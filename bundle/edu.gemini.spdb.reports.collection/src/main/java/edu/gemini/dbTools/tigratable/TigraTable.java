//
// $Id: TigraTable.java 5137 2004-09-24 19:58:29Z shane $
//

package edu.gemini.dbTools.tigratable;

import edu.gemini.spModel.core.SPProgramID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A TigraTable is a pairing of a semester key (of the form
 * <pre>\\d\\d[AB]</pre>) and a List of {@link TigraTableRow}.
 */
class TigraTable implements Serializable {
    private final String _semesterKey;
    private final List<TigraTableRow> _rows = new ArrayList<TigraTableRow>();
    private boolean _sorted;

    public TigraTable(final String semesterKey) {
        _semesterKey = semesterKey;
    }

    public String getSemesterKey() {
        return _semesterKey;
    }

    void addRow(final TigraTableRow row) {
        if (_sorted && (_rows.size() > 0)) {
            // See if the rows will still be sorted after adding this element.
            final int last = _rows.size() - 1;
            final SPProgramID lastId = _rows.get(last).getProgramId();
            _sorted = row.getProgramId().compareTo(lastId) >= 0;
        }
        _rows.add(row);
    }

    public List<TigraTableRow> getRows() {
        if (!_sorted) {
            // Sort the rows.
            Collections.sort(_rows, new Comparator<TigraTableRow>() {
                public int compare(final TigraTableRow ttr1, final TigraTableRow ttr2) {
                    final SPProgramID id1 = ttr1.getProgramId();
                    final SPProgramID id2 = ttr2.getProgramId();
                    return id1.compareTo(id2);
                }
            });
            _sorted = true;
        }

        return Collections.unmodifiableList(_rows);
    }
}
