package edu.gemini.obslog.obslog;

import edu.gemini.obslog.transfer.EChargeObslogVisit;
import edu.gemini.obslog.util.SummaryUtils;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetRecord;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obsrecord.UniqueConfig;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.TimeAmountFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Gemini Observatory/AURA
 * $ID$
 *
 * @author Kim Gillies
 */
public class ConfigMapUtil {
    private static final Logger LOG = Logger.getLogger(ConfigMapUtil.class.getName());

    /**
     * NOTE!!! The names used for the data store columns MUST MATCH the information in the configuration
     * file!!!  So the names used to lookup information such as *dataLabels* must be the <itemName> in the
     * configuration file for the observe_dataLabels item in the config builder.
     */
    static public final String DATA_LABELS_ITEM_NAME = "dataLabels";
    static public final String OBSLOG_FILENAMES_ITEM_NAME = "fileNumbers";
    static public final String OBSLOG_COMMENT_ITEM_NAME = "comment";
    static public final String OBSLOG_COMMENT_ROWS_ITEM_NAME = "commentRows";
    static public final String OBSLOG_DATASETCOMMENTS_ITEM_NAME = "datasetComments";
    static public final String OBSLOG_UT_ITEM_NAME = "datasetUT";
    static public final String OBSLOG_DATSETQA_NAME = "datasetQA";
    static public final String OBSLOG_DATASETCLASS_NAME = "class";
    static public final String OBSLOG_RAWUT_ITEM_NAME = "rawUT";
    static public final String OBSERVE_STATUS_ITEM_NAME = "observeStatus";
    static public final String OBSLOG_OBSERVATIONID_ITEM_NAME = "observationID";
    static public final String OBSLOG_FILENAMEPREFIX_ITEM_NAME = "filePrefix";
    static public final String OBSLOG_WEATHER_TIME_ITEM_NAME = "time";
    // The following is used by to identify the index of a specific unique config within an observation's
    // list of unqiue configs
    static public final String OBSLOG_UNIQUECONFIG_ID = "configID";
    // Time Accounting values
    static public final String OBSLOG_OBSCLASS_NAME = "observeClass";
    static public final String OBSLOG_CHARGECLASS_NAME = "chargeClass";
    static public final String OBSLOG_NONCHARGED_NAME = "noncharged";
    static public final String OBSLOG_PARTNERCHARGED_NAME = "partnercharged";
    static public final String OBSLOG_PROGRAMCHARGED_NAME = "programcharged";
    static public final String OBSLOG_VISITGAP_NAME = "visitgap";
    static public final String OBSLOG_VISITSTARTTIME_NAME = "visitStartTime";
    static public final String OBSLOG_VISITENDTIME_NAME = "visitEndTime";

    static private final String FILE_SEP = "-";
    static private final String EMPTY_STRING = "";

    static private void _setStartTime(String time, ConfigMap m) {
        m.put(OBSLOG_UT_ITEM_NAME, time);
        m.put(OBSLOG_RAWUT_ITEM_NAME, time);
    }

    static void addStartTime(DatasetRecord dset, ConfigMap m) {
        // Note that the time is being placed as a long value as a String.  It needs to be formatted in the
        // higher layers.  The second raw ut is used for navigation
        String time = Long.toString(dset.exec.dataset().getTimestamp());
        _setStartTime(time, m);
    }

    static void addConfigStartTime(UniqueConfig uc, ConfigMap m) {
        String time = Long.toString(uc.getConfigTime());
        _setStartTime(time, m);
    }

    static void addDatasetIDs(List<DatasetRecord> dsetRecords, ConfigMap m) {
        int datasetCount = dsetRecords.size();
        if (datasetCount == 0) {
            // Shouldn't happen
            m.put(DATA_LABELS_ITEM_NAME, FILE_SEP);
            return;
        }

        DatasetLabel first = dsetRecords.get(0).getLabel();
        if (datasetCount == 1) {
            m.put(DATA_LABELS_ITEM_NAME, String.valueOf(first.getIndex()));
            return;
        }

        DatasetLabel last = dsetRecords.get(datasetCount - 1).getLabel();
        m.put(DATA_LABELS_ITEM_NAME, String.valueOf(first.getIndex() + FILE_SEP + String.valueOf(last.getIndex())));
    }

    static void addDatasetID(DatasetRecord dset, ConfigMap m) {
        if (dset == null) {
            // Shouldn't happen
            m.put(DATA_LABELS_ITEM_NAME, FILE_SEP);
            return;
        }

        DatasetLabel first = dset.getLabel();
        m.put(DATA_LABELS_ITEM_NAME, String.valueOf(first));
    }

    static void addDatasetQA(DatasetRecord dset, ConfigMap m) {
        if (dset == null) {
            // Shouldn't happen
            m.put(OBSLOG_DATSETQA_NAME, EMPTY_STRING);
            return;
        }

        DatasetQaState qastate = dset.qa.qaState;
        m.put(OBSLOG_DATSETQA_NAME, qastate.displayValue());
    }

    static void addOneFile(DatasetRecord dset, ConfigMap m) {
        if (dset == null) {
            // Shouldn't happen
            m.put(OBSLOG_FILENAMES_ITEM_NAME, FILE_SEP);

            // Shouldn't happen either
            m.put(OBSLOG_FILENAMEPREFIX_ITEM_NAME, EMPTY_STRING);
            return;
        }

        String filename = dset.exec.dataset().getDhsFilename();
        m.put(OBSLOG_FILENAMES_ITEM_NAME, filename);
    }

    static void addFileInfo(List<DatasetRecord> dsetRecords, ConfigMap m) {
        int datasetCount = dsetRecords.size();
        if (datasetCount == 0) {
            // Shouldn't happen
            m.put(OBSLOG_FILENAMES_ITEM_NAME, FILE_SEP);

            // Shouldn't happen either
            m.put(OBSLOG_FILENAMEPREFIX_ITEM_NAME, EMPTY_STRING);
            return;
        }

        GeminiFileName first = new GeminiFileName(dsetRecords.get(0).exec.dataset().getDhsFilename());
        String firstFileNumber = String.valueOf(first.getSequenceNumber());
        if (datasetCount == 1) {
            m.put(OBSLOG_FILENAMES_ITEM_NAME, firstFileNumber);
            return;
        }

        // Always add the prefix if there is one file
        String prefix = first.getPrefix();
        m.put(OBSLOG_FILENAMEPREFIX_ITEM_NAME, prefix);

        // Add the first to last when there is more than one dataset.  The value after - could be empty
        GeminiFileName last = new GeminiFileName(dsetRecords.get(datasetCount - 1).exec.dataset().getDhsFilename());
        m.put(OBSLOG_FILENAMES_ITEM_NAME, firstFileNumber + FILE_SEP + last.getSequenceNumber());
    }

    static void addComments(List<DatasetRecord> dsets, ConfigMap m) {
        if (dsets == null || dsets.size() == 0) {
            // Shouldn't happen
            m.put(OBSLOG_COMMENT_ITEM_NAME, EMPTY_STRING);
            return;
        }

        String comment = dsets.get(0).qa.comment;
        m.put(OBSLOG_COMMENT_ITEM_NAME, comment);
        // Add the initial step as the configID.   This helps to set the comment later
        m.put(OBSLOG_UNIQUECONFIG_ID, dsets.get(0).getLabel().toString());
    }

    static void addDatasetComments(List<DatasetRecord> dsets, ConfigMap m) {
        final List<DatasetRecord> comments = new ArrayList<>();
        // I'm checking for more than one item because the main obslog comment is dataset 0 comment. Only want to add
        // comments that are after that one
        if (dsets == null || dsets.size() <= 1) {
            // Shouldn't happen
            m.put(OBSLOG_DATASETCOMMENTS_ITEM_NAME, comments);
            return;
        }
        // From 1 inclusive to size exclusive
        for (DatasetRecord dset : dsets.subList(1, dsets.size())) {
            if (dset.qa.comment.equals(EMPTY_STRING)) continue;
            comments.add(dset);
        }
        /*
        for (DatasetRecord dset : comments) {
            LOG.log(Level.INFO, "Dataset: " + dset.getLabel().toString() + ": " + dset.getComment());
        }
        */
        m.put(OBSLOG_DATASETCOMMENTS_ITEM_NAME, comments);
    }

    /**
     * This method checks for a comment and adds a property that is the number of rows in the comment.
     * Used by display code to dynamically set size of text area
     *
     * @param m <code>UniqueConfigMap</code>
     */
    static public void addCommentRowCount(ConfigMap m) {
        if (m == null) return;
        final int MAX_COMMENT_ROWS = 5;

        String comment = m.sget(OBSLOG_COMMENT_ITEM_NAME);
        if (comment == null || comment.length() == 0) {
            m.put(ConfigMapUtil.OBSLOG_COMMENT_ROWS_ITEM_NAME, "1");
            return;
        }

        int rows = 0;
        BufferedReader bread = null;
        try {
            bread = new BufferedReader(new StringReader(comment));
            while (bread.readLine() != null) rows++;
            if (rows > MAX_COMMENT_ROWS) rows = MAX_COMMENT_ROWS;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Exception while counting comment lines: " + ex);
            return;
        } finally {
            try {
                if (bread != null) bread.close();
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Exception while closing BufferedReader");
            }
        }

        m.put(OBSLOG_COMMENT_ROWS_ITEM_NAME, String.valueOf(rows));
    }

    static public void addChargeTimeInfo(EChargeObslogVisit evisit, ConfigMap m) {
        if (m == null) return;

        ObsClass obsClass = evisit.getObsClass();
        m.put(OBSLOG_OBSCLASS_NAME, obsClass.displayValue());
        m.put(OBSLOG_CHARGECLASS_NAME, obsClass.getDefaultChargeClass().displayValue());

        ObsTimeCharges charges = evisit.getObsTimeCharges();
        m.put(OBSLOG_NONCHARGED_NAME, Long.toString(charges.getTimeCharge(ChargeClass.NONCHARGED).getTime()));
        m.put(OBSLOG_PARTNERCHARGED_NAME, Long.toString(charges.getTimeCharge(ChargeClass.PARTNER).getTime()));
        m.put(OBSLOG_PROGRAMCHARGED_NAME, Long.toString(charges.getTimeCharge(ChargeClass.PROGRAM).getTime()));

        m.put(OBSLOG_VISITSTARTTIME_NAME, SummaryUtils.formatUTCTime(evisit.getVisitStartTime()));
        m.put(OBSLOG_VISITENDTIME_NAME, SummaryUtils.formatUTCTime(evisit.getVisitEndTime()));
    }

    private static long _getTimeGap(EChargeObslogVisit v1, EChargeObslogVisit v2) {
        // Get the visit start time, add the charges and subtract from the second visit start time
        long v1StartTime = v1.getVisitStartTime();
        ObsTimeCharges charges = v1.getObsTimeCharges();
        long noncharged = charges.getTime(ChargeClass.NONCHARGED);
        long partner = charges.getTime(ChargeClass.PARTNER);
        long program = charges.getTime(ChargeClass.PROGRAM);

        return v2.getVisitStartTime() - (v1StartTime + noncharged + partner + program);
    }

    static public void addTimeGaps(List<EChargeObslogVisit> evisits, List<ConfigMap> maps) {
        // Look at each map
        int size = maps.size() - 1;
        for (int i = 0; i < size; i++) {
            EChargeObslogVisit v1 = evisits.get(i);
            EChargeObslogVisit v2 = evisits.get(i + 1);
            long gap = _getTimeGap(v1, v2);
            maps.get(i).put(OBSLOG_VISITGAP_NAME, TimeAmountFormatter.getHMSFormat(gap));
        }
        // Add an entry for the  last map
        maps.get(size).put(OBSLOG_VISITGAP_NAME, "00:00:00");
    }

    static public void decorateDatasetUT(ConfigMap m, boolean isMultiNight) {
        if (m == null) return;

        String longValue = m.sget(OBSLOG_UT_ITEM_NAME);
        if (longValue == null) return;

        long utstart = Long.parseLong(longValue);
        m.put(OBSLOG_UT_ITEM_NAME, isMultiNight ? SummaryUtils.formatUTCDateTime(utstart) : SummaryUtils.formatUTCTime(utstart));
    }


}
