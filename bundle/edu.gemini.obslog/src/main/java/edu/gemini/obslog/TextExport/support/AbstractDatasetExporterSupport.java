package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.ConfigMapUtil;
import edu.gemini.obslog.obslog.IObservingLogSegment;
import edu.gemini.spModel.dataset.DatasetRecord;

import java.util.List;

/**
 * Gemini Observatory/AURA
 * $Id: AbstractDatasetExporterSupport.java,v 1.1 2006/12/05 14:56:16 gillies Exp $
 */
public abstract class AbstractDatasetExporterSupport extends AbstractTextSegmentExporter {
    static protected final String OBSERVATION_ID_PROPERTY_NAME = ConfigMapUtil.OBSLOG_OBSERVATIONID_ITEM_NAME;
    static protected final String DATASETCOMMENT_PROPERTY_NAME = ConfigMapUtil.OBSLOG_DATASETCOMMENTS_ITEM_NAME;
    static protected final String COMMENT_PROPERTY_NAME = ConfigMapUtil.OBSLOG_COMMENT_ITEM_NAME;

    // The following width is used to justify
    static protected final int    DATASETID_WIDTH = 24;

    public AbstractDatasetExporterSupport(IObservingLogSegment segment) {
        super(segment);
    }

    @SuppressWarnings("unchecked")
    protected void _printDatasetComments(StringBuilder sb, ConfigMap row) {
        List<DatasetRecord> datasetComments = (List<DatasetRecord>) row.get(DATASETCOMMENT_PROPERTY_NAME);
        if (datasetComments == null) return;

        for (DatasetRecord dset : datasetComments) {
            _printJustifiedMultilineComment(sb, DATASETID_WIDTH, dset.getLabel().toString(), dset.qa.comment);
        }
        sb.append(NEWLINE);
    }


}
