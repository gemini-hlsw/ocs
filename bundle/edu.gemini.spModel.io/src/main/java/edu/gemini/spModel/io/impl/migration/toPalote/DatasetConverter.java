//
// $Id: DatasetConverter.java 6831 2005-12-09 21:58:17Z shane $
//

package edu.gemini.spModel.io.impl.migration.toPalote;

import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.dataset.*;
import edu.gemini.spModel.pio.Container;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Converts the Grillo PIO representation of a dataset to the corresponding
 * Palote {@link edu.gemini.spModel.dataset.DatasetExecRecord}.
 */
final class DatasetConverter {
    private static final Logger LOG = Logger.getLogger(DatasetConverter.class.getName());

    private SPObservationID _obsId;

    DatasetConverter(SPObservationID obsId) {
        _obsId = obsId;
    }

    private void _logMissingInfo(ParamSet grilloDset, String paramName) {
        LOG.log(Level.WARNING, "Grillo dataset '" + grilloDset.getSequence() +
                "' missing param '" + paramName + "' for obs '" + _obsId + "'");
    }

    DatasetLabel getDatasetLabel(ParamSet grilloDset) {
        String labelStr = Pio.getValue(grilloDset, "label", null);
        if (labelStr == null) {
            _logMissingInfo(grilloDset, "label");
            return null;
        }

        DatasetLabel label;
        try {
            label = new DatasetLabel(labelStr);
        } catch (ParseException ex) {
            LOG.log(Level.WARNING, "Grillo dataset '" + grilloDset.getSequence() +
                    "' has an illegal label '" + labelStr + "' for obs '" +
                    _obsId + "'");
            return null;
        }

        return label;
    }

    long getTimeStamp(ParamSet grilloDset) {
        long timestamp = Pio.getLongValue(grilloDset, "time", -1);
        if (timestamp == -1) {
            _logMissingInfo(grilloDset, "time");
        }
        return timestamp;
    }

    String getDhsFileName(ParamSet grilloDset) {
        String dhsFile = Pio.getValue(grilloDset, "file", null);
        if (dhsFile == null) {
            _logMissingInfo(grilloDset, "file");
        }
        return dhsFile;
    }

    String getComment(ParamSet grilloDset) {
        return Pio.getValue(grilloDset, "comment", "");
    }

    public DatasetRecord toDatasetRecord(ParamSet grilloDset) {
        final DatasetLabel label = getDatasetLabel(grilloDset);
        if (label == null) return null;

        final long timestamp = getTimeStamp(grilloDset);
        if (timestamp < 0) return null;

        final String dhsFile = getDhsFileName(grilloDset);
        if (dhsFile == null) return null;

        final Dataset dset = new Dataset(label, dhsFile, timestamp);
        final DatasetQaRecord qa = new DatasetQaRecord(label, DatasetQaState.UNDEFINED, getComment(grilloDset));
        final DatasetExecRecord exec = DatasetExecRecord.apply(dset);

        return new DatasetRecord(qa, exec);
    }

    public DatasetRecord toDatasetRecord(ParamSet grilloDset, DatasetQaState qa) {
        DatasetRecord rec = toDatasetRecord(grilloDset);
        if (rec == null) return null;
        return rec.withQa(rec.qa.withQaState(qa));
    }

    /**
     * Gets a java.util.List of {@link edu.gemini.spModel.pio.ParamSet} where
     * each contains an particular Grillo dataset definition.
     */
    private static List _getDataParamsets(Container obsContainer) {
        Container dataStoreCont = obsContainer.getContainer("Observing Log");
        if (dataStoreCont == null) return Collections.EMPTY_LIST;

        ParamSet pset = dataStoreCont.getParamSet("Observing Log");
        if (pset == null) return Collections.EMPTY_LIST;

        List dsetList = pset.getParamSets();
        if (dsetList == null) return Collections.EMPTY_LIST;
        return dsetList;
    }

    /**
     * Extracts Palote DatasetRecords from the given observation
     * {@link Container}.
     */
    public static DatasetRecord[] getDatasetRecords(
            SPObservationID obsId, Container obsContainer, DatasetQaState qa) {
        DatasetConverter gd = new DatasetConverter(obsId);

        // If there are no datasets, return an empty array.
        List grilloDsets = _getDataParamsets(obsContainer);
        int sz = grilloDsets.size();
        if (sz == 0) return new DatasetRecord[0];

        // Create a list to hold the converted datasets.
        List<DatasetRecord> paloteDatasets = new ArrayList<DatasetRecord>(sz);

        // Convert each dataset record.
        for (Iterator it=grilloDsets.iterator(); it.hasNext(); ) {
            ParamSet grilloDset = (ParamSet) it.next();
            DatasetRecord rec = gd.toDatasetRecord(grilloDset, qa);
            if (rec == null) continue;
            paloteDatasets.add(rec);
        }
        return paloteDatasets.toArray(new DatasetRecord[paloteDatasets.size()]);
    }

}
