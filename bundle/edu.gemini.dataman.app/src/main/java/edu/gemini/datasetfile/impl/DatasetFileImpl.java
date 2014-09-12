//
// $Id: DatasetFileImpl.java 281 2006-02-13 17:52:21Z shane $
//

package edu.gemini.datasetfile.impl;

import edu.gemini.datasetfile.*;
import edu.gemini.fits.FitsParseException;
import edu.gemini.fits.Header;
import edu.gemini.fits.HeaderItem;
import edu.gemini.fits.Hedit;
import edu.gemini.spModel.dataset.Dataset;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.obsclass.ObsClass;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
public final class DatasetFileImpl implements DatasetFile {
    private static final Logger LOG = Logger.getLogger(DatasetFileImpl.class.getName());

    static final String DATALAB  = "DATALAB";
    static final String OBSCLASS = "OBSCLASS";
    static final String PROP_MD  = "PROP_MD";

    private static final Set<String> KEYWORDS;

    static {
        Set<String> tmp = new TreeSet<String>();
        tmp.add(DATALAB);
        tmp.add(OBSCLASS);
        tmp.addAll(QaStateConverter.KEYWORDS);
        tmp.addAll(TimeConverter.KEYWORDS);
        tmp.add(PROP_MD);
        KEYWORDS = Collections.unmodifiableSet(tmp);
    }

    public static DatasetFile parse(File fitsFile) throws IOException, DatasetFileException, InterruptedException {

        Hedit hedit = new Hedit(fitsFile);
        Header h;
        try {
            h = hedit.readPrimary(KEYWORDS);
        } catch (FitsParseException ex) {
            throw new DatasetFileException("Invalid FITS file: " +
                    ex.getMessage());
        }

        if (LOG.isLoggable(Level.FINER)) {
            StringBuilder buf = new StringBuilder();
            buf.append('\n');
            for (HeaderItem hi : h) {
                buf.append(hi.toString()).append('\n');
            }
            LOG.log(Level.FINER, buf.toString());
        }

        // Parse the datalabel.
        DatasetLabel label = getDatasetLabel(h);

        // Get the timestamp.
        Date ts = TimeConverter.getTimestamp(h);

        String fileName = fitsFile.getName();
        if (fileName.endsWith(".fits")) {
            fileName = fileName.substring(0, fileName.length()-5);
        }
        Dataset dset = new Dataset(label, fileName, ts.getTime());

        // Get the OBSCLASS.
        ObsClass obsClass = getObsClass(h);

        // Get the QA state.
        DatasetQaState qaState = QaStateConverter.getQaState(h);

        // Get the release date.
        Date releaseDate = TimeConverter.getReleaseDate(h);

        // Get the private header flag
        boolean headerPrivate = ProprietaryMetadataConverter.get(h);

        return new DatasetFileImpl(dset, fitsFile, obsClass, qaState, releaseDate, headerPrivate);
    }

    static DatasetLabel getDatasetLabel(Header h)
            throws DatasetFileException {
        HeaderItem item = h.get(DATALAB);
        if (item == null) {
            throw new DatasetFileException("missing 'DATALAB' key");
        }
        String strLabel = item.getValue();
        if (strLabel == null) {
            throw new DatasetFileException("missing 'DATALAB' value");
        }
        DatasetLabel lab;
        try {
            lab = new DatasetLabel(strLabel);
        } catch (Exception ex) {
            throw new DatasetFileException("illegal 'DATALAB': " + strLabel);
        }
        return lab;
    }

    static ObsClass getObsClass(Header h) throws DatasetFileException {
        HeaderItem item = h.get(OBSCLASS);
        if (item == null) {
            throw new DatasetFileException("missing 'OBSCLASS' key");
        }
        String classStr = item.getValue();
        if (classStr == null) {
            throw new DatasetFileException("missing 'OBSCLASS' key");
        }

        ObsClass obsClass = ObsClass.parseType(classStr);
        if (obsClass == null)
            throw new DatasetFileException("invalid 'OBSCLASS': " + classStr);
        return obsClass;
    }

    private Dataset _dataset;
    private File _file;
    private DatasetFileName _fname;
    private ObsClass _obsClass;
    private DatasetQaState _qaState;
    private Date _release;
    private boolean _headerPrivate;
    private long _lastModified;

    private DatasetFileImpl(Dataset dataset, File file, ObsClass obsClass,
                            DatasetQaState qaState, Date release, boolean headerPrivate) {
        if (dataset == null) throw new NullPointerException();
        if (file == null) throw new NullPointerException();
        _dataset = dataset;
        _file    = file;
        _fname   = new DatasetFileName(file.getName());
        _obsClass= obsClass;
        _qaState = qaState;
        _release = release;
        _headerPrivate = headerPrivate;
        _lastModified  = file.lastModified();
    }

    public Dataset getDataset() {
        return _dataset;
    }

    public DatasetLabel getLabel() {
        return _dataset.getLabel();
    }

    public String getObsDateAsString() {
        return TimeConverter.formatDate(new Date(_dataset.getTimestamp()));
    }

    public DatasetQaState getQaState() {
        return _qaState;
    }

    public long lastModified() {
        return _lastModified;
    }

    public DatasetFileUpdate toUpdate() {
        return new DatasetFileUpdateImpl(_qaState, _release, _headerPrivate);
    }

    public DatasetFile apply(DatasetFileUpdate update) {
        DatasetQaState newQaState = update.getQaState();
        Date newRelease = update.getRelease();
        Boolean newHeaderPrivate = update.isHeaderPrivate();

        DatasetQaState qaState = (newQaState == null ? _qaState : newQaState);
        Date release = (newRelease == null ? _release : newRelease);
        boolean headerPrivate = (newHeaderPrivate == null ? _headerPrivate : newHeaderPrivate);

        return new DatasetFileImpl(_dataset, _file, _obsClass, qaState, release, headerPrivate);
    }

    public File getFile() {
        return _file;
    }

    public ObsClass getObsClass() {
        return _obsClass;
    }

    public DatasetFileName getDatasetFileName() {
        return _fname;
    }

    public Date getRelease() {
        return _release;
    }

    public boolean isHeaderPrivate() {
        return _headerPrivate;
    }

    public String getReleaseAsString() {
        if (_release == null) return null;
        return TimeConverter.formatDate(_release);
    }

    public boolean equals(Object o) {
        if (!(o instanceof DatasetFileImpl)) return false;
        DatasetFileImpl that = (DatasetFileImpl) o;

        if (!_dataset.equals(that._dataset)) return false;
        if (!_file.equals(that._file)) return false;
        if (_lastModified != that._lastModified) return false;
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
        return _headerPrivate == that._headerPrivate;
    }

    public int hashCode() {
        int res = _dataset.hashCode();
        res = 31*res + _file.hashCode();
        if (_qaState != null) res = 31*res + _qaState.hashCode();
        if (_release != null) res = 31*res + _release.hashCode();
        res = 31*res + (int)(_lastModified^(_lastModified>>>32));
        return 31*res + (_headerPrivate ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(_dataset.getLabel());
        buf.append(" [file=").append(_dataset.getDhsFilename());
        buf.append(", lastMod=").append(_lastModified);
        buf.append(", qaState=").append(_qaState);
        buf.append(", release=").append(getReleaseAsString());
        buf.append(", headerP=").append(_headerPrivate);
        buf.append(']');
        return buf.toString();
    }
}
