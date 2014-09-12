package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.obslog.util.SummaryUtils;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.List;

//
// Gemini Observatory/AURA
// $Id: InstrumentLogSegment.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public abstract class InstrumentLogSegment extends OlBasicSegment implements Serializable {
    private static final Logger LOG = Logger.getLogger(InstrumentLogSegment.class.getName());

    private OlLogOptions _obsLogOptions;

    public InstrumentLogSegment(OlSegmentType type, List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(type, logItems);
        if (logItems == null || obsLogOptions == null) throw new NullPointerException();

        _obsLogOptions = obsLogOptions;
    }

    public OlLogOptions getLogOptions() {
        return _obsLogOptions;
    }

    /**
     * Add information for one observation to the segment.  This method calls mandatory decorators and allows
     * the instrument segment to decorate its own items.
     * This is only called if the observation has unique configs
     *
     * @param evisit the <tt>EObslogVisit</tt>
     */
    public void addObservationData(EObslogVisit evisit) {
        // Note that there should be one EObsLogVisit for each row in this case.
        UniqueConfigMap map = new UniqueConfigMap(evisit, getTableInfo());

        _mandatoryDecorations(map);
        decorateObservationData(map);

        // Add them all to the segment data
        _getSegmentDataList().add(map);
    }

    private void _mandatoryDecorations(ConfigMap m) {
        _decorateDatasetUT(m);
        _decorateFilenames(m);
        _decorateComment(m);
    }

    protected void _decorateDatasetUT(ConfigMap m) {
        if (m == null) return;

        boolean isMultiNight = getLogOptions().isMultiNight();

        String longValue = m.sget(ConfigMapUtil.OBSLOG_UT_ITEM_NAME);
        if (longValue == null) return;

        long utstart = Long.parseLong(longValue);
        m.put(ConfigMapUtil.OBSLOG_UT_ITEM_NAME, isMultiNight ? SummaryUtils.formatUTCDateTime(utstart) : SummaryUtils.formatUTCTime(utstart));
    }

    protected void _decorateFilenamesForMultiNight(ConfigMap m) {
        if (m == null) return;

        String filenameValue = m.sget(ConfigMapUtil.OBSLOG_FILENAMES_ITEM_NAME);
        if (filenameValue == null) return;

        // No change if not multinight
        String prefixValue = m.sget(ConfigMapUtil.OBSLOG_FILENAMEPREFIX_ITEM_NAME);
        if (prefixValue != null) {
            m.put(ConfigMapUtil.OBSLOG_FILENAMES_ITEM_NAME, prefixValue + '[' + filenameValue + ']');
        }
    }

    protected void _decorateFilenames(ConfigMap m) {
        if (m == null) return;

        if (getLogOptions().isMultiNight()) {
            _decorateFilenamesForMultiNight(m);
        }
    }

    /**
     * This method checks for a comment and adds a property that is the number of rows in the comment.
     * Used by display code to dynamically set size of text area
     * @param m <code>UniqueConfigMap</code>
     */
    protected void _decorateComment(ConfigMap m) {
        if (m == null) return;
        final int MAX_COMMENT_ROWS = 5;

        String comment = m.sget(ConfigMapUtil.OBSLOG_COMMENT_ITEM_NAME);
        if (comment == null || comment.length() == 0) return;

        int rows = 0;
        BufferedReader bread = null;
        try {
            bread = new BufferedReader(new StringReader(comment));
            while(bread.readLine() != null) rows++;
            if (rows > MAX_COMMENT_ROWS) rows = MAX_COMMENT_ROWS;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Exception while counting comment lines: " + ex);
            return;
        } finally {
            try {
                if (bread != null) bread.close();
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Exception while closing BufferedReader");
            };
        }

        m.put(ConfigMapUtil.OBSLOG_COMMENT_ROWS_ITEM_NAME, String.valueOf(rows));
    }

}
