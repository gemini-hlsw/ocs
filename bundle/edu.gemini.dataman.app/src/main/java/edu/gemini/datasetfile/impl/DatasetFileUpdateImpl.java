//
// $Id: DatasetFileUpdateImpl.java 73 2005-09-03 19:10:22Z shane $
//

package edu.gemini.datasetfile.impl;

import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.datasetfile.DatasetFileUpdate;
import edu.gemini.datasetfile.DatasetFile;

import java.util.Date;

/**
 *
 */
public final class DatasetFileUpdateImpl implements DatasetFileUpdate {

    private DatasetQaState _qaState;
    private Date _release;
    private Boolean _headerPrivate;

    public DatasetFileUpdateImpl() {
    }

    public DatasetFileUpdateImpl(DatasetFile dsetFile) {
        _qaState = dsetFile.getQaState();
        _release = dsetFile.getRelease();
        _headerPrivate = dsetFile.isHeaderPrivate();
    }

    public DatasetFileUpdateImpl(DatasetQaState qaState) {
        _qaState = qaState;
    }

    public DatasetFileUpdateImpl(DatasetQaState qaState, Date releaseDate, Boolean headerPrivate) {
        _qaState = qaState;
        _release = releaseDate;
        _headerPrivate = headerPrivate;
    }

    public DatasetQaState getQaState() {
        return _qaState;
    }

    public void setQaState(DatasetQaState qaState) {
        _qaState = qaState;
    }

    public Date getRelease() {
        return _release;
    }

    public void setRelease(Date date) {
        _release = date;
    }

    public Boolean isHeaderPrivate() {
        return _headerPrivate;
    }

    public void setHeaderPrivate(Boolean headerPrivate) {
        _headerPrivate = headerPrivate;
    }

    /*
    public void update(File fitsFile) throws IOException {
        Hedit hedit = new Hedit(fitsFile);

        Collection<HeaderItem> updates = new ArrayList<HeaderItem>(3);

        if (_qaState != null) {
            updates.add(DatamanHeader.toRawGemQaItem(_qaState));
            updates.add(DatamanHeader.toRawPiReqItem(_qaState));
        }
        if (_release != null) {
            updates.add(DatamanHeader.toReleaseItem(_release));
        }
        hedit.updatePrimary(updates);
    }
    */

    public boolean equals(Object o) {
        if (!(o instanceof DatasetFileUpdateImpl)) return false;
        DatasetFileUpdateImpl that = (DatasetFileUpdateImpl) o;

        if (_qaState == null) {
            if (that._qaState != null) return false;
        } else if (!_qaState.equals(that._qaState)) {
            return false;
        }
        if (_release == null) {
            if (that._release != null) return false;
        } else if (!_release.equals(that._release)) {
            return false;
        }
        if (_headerPrivate == null) {
            if (that._headerPrivate != null) return false;
        } else if (!_headerPrivate.equals(that._headerPrivate)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int res = 1;
        if (_qaState != null) res = _qaState.hashCode();
        if (_release != null) res = 31*res + _release.hashCode();
        if (_headerPrivate != null) res = 31*res + _headerPrivate.hashCode();
        return res;
    }
}
