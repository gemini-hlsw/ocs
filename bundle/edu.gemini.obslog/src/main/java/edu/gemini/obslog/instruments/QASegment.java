package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.*;
import edu.gemini.obslog.transfer.EObslogVisit;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetRecord;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

//
// Gemini Observatory/AURA
// $Id: QASegment.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

public final class QASegment extends OlBasicSegment implements Serializable {
    private static final Logger LOG = Logger.getLogger(QASegment.class.getName());

    private static final String NARROW_TYPE = "QA";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Quality Assurance Log";

    private boolean _isMultiNight;

    // A special list of all the datasets in the QA Segment to assist in bulk edit
    private List<String> _datasetIDs = new ArrayList<String>();

    public static class QASegmentFilter {
        private List<DatasetLabel> _labels;

        public QASegmentFilter(List<DatasetLabel> labels) {
            _labels = labels;
        }

        boolean includeDataset(DatasetLabel datasetLabel) {
            return _labels.contains(datasetLabel);
        }
    }

    private QASegmentFilter _filter;

    public QASegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions, QASegmentFilter filter) {
        this(logItems, obsLogOptions);
        _filter = filter;
    }

    public QASegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems);
        if (logItems == null || obsLogOptions == null) throw new NullPointerException();

        _isMultiNight = obsLogOptions.isMultiNight();
    }

    /**
     * Add information for one observation to the segment.  This method calls mandatory decorators and allows
     * the instrument segment to decorate its own items.
     * This is only called if the observation has unique configs
     *
     * @param evisit the <tt>EObslogVisit</tt>
     */
    public void addObservationData(EObslogVisit evisit) {
        // Here we get a normal visit with one config and a set of datasets that  match, so unroll it
        List<QAConfigMap> configs = new ArrayList<QAConfigMap>();
        for (DatasetRecord dset : evisit.getDatasetRecords()) {
            // Now check to see if the  dataset should be included
            if (_filter != null) {
                if (!_filter.includeDataset(dset.label())) {
                    if (LOG.isLoggable(Level.FINE)) LOG.fine("Skipping dataset: " + dset.label());
                    continue;
                }
            }
            // Add it to the list of datasets
            _datasetIDs.add(dset.label().toString());

            // Create one map and share the EObslogVisit uniqueconfig for all of them
            QAConfigMap map = new QAConfigMap(evisit.getUniqueConfig(), dset, getTableInfo());

            ConfigMapUtil.decorateDatasetUT(map, _isMultiNight);
            ConfigMapUtil.addCommentRowCount(map);
            configs.add(map);
        }

        // Add them all to the segment data
        _getSegmentDataList().addAll(configs);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param td <code>TransferData</code>
     */
    public void decorateObservationData(ConfigMap td) {
        // No decorations needed at this time
    }

    /**
     * Return the list of dataset IDs as Strings.  Used for bulk edit duties
     * @return a {@link List} of Strings
     */
    public List<String> getDatasetIDs() {
        return _datasetIDs;
    }

    /**
     * Return the segment caption.
     *
     * @return The caption.
     */
    public String getSegmentCaption() {
        return SEGMENT_CAPTION;
    }
}
