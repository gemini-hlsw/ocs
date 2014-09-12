package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.ConfigMapUtil;
import edu.gemini.obslog.obslog.IObservingLogSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gemini Observatory/AURA
 * $Id: QATextSegmentExporter.java,v 1.2 2006/12/05 14:56:16 gillies Exp $
 */
public class QATextSegmentExporter extends AbstractDatasetExporterSupport implements ITextExporter {
    private static final Logger LOG = Logger.getLogger(QATextSegmentExporter.class.getName());

    static protected final String OBSERVATION_ID_PROPERTY_NAME = ConfigMapUtil.OBSLOG_OBSERVATIONID_ITEM_NAME;

    public static ITextExporterFactory FACTORY = new ITextExporterFactory() {
        public ITextExporter create(IObservingLogSegment segment) {
            return new QATextSegmentExporter(segment);
        }
    };

    public QATextSegmentExporter(IObservingLogSegment segment) {
        super(segment);
    }

    public void printComment(StringBuilder sb, ConfigMap row) {
        String comment = row.sget(COMMENT_PROPERTY_NAME);
        String datasetLabel = row.sget("dataLabels");
        if (datasetLabel == null) {
            LOG.severe("Failed to find a dataLabel property for: ");
            return;
        }

        _printJustifiedMultilineComment(sb, DATASETID_WIDTH, datasetLabel, comment);
    }

    protected void _setupColumns() {}

    private void printOneRow(StringBuilder sb, ConfigMap row) {
        Map<String, ColumnInfo> columns = _getColumnInfoMap();

        for (ColumnInfo cinfo : columns.values()) {
            String value = row.sget(cinfo.getProperty());
            if (value == null) {
                LOG.severe("Final value for: " + cinfo.getProperty() + " was null.");
                value = MISSING_VALUE;
            }
            // Here we purposefully skip the observation ID
            if (cinfo.getProperty().equals(OBSERVATION_ID_PROPERTY_NAME)) continue;

            _printJustifiedText(sb, cinfo.getMaxColumnWidth(), value);
        }
    }

    private void printRows(StringBuilder sb, List<ConfigMap> rowMaps) {

        int size = rowMaps.size();
        if (size == 0) {
            sb.append("No information in the database");
            sb.append(NEWLINE);
        }

        for (int i = 0; i < size; i++) {
            ConfigMap rowMap = rowMaps.get(i);
            printOneRow(sb, rowMap);
            sb.append(NEWLINE);
            // Prints each rows comments
            printComment(sb, rowMap);
            _printDatasetComments(sb, rowMap);
        }
        sb.append(NEWLINE);
    }

    public StringBuilder export(StringBuilder sb) {

        _printCaption(sb, _getSegment().getSegmentCaption());

        _printDivider(sb, DIVIDER, _getTotalWidth());

        Map<String,ColumnInfo> columnInfo = _getColumnInfoMap();
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>(columnInfo.values());
        ColumnInfo observationIdInfo = columnInfo.get(OBSERVATION_ID_PROPERTY_NAME);
        if  (observationIdInfo != null) {
            columns.remove(observationIdInfo);
        }

        _printHeading(sb, columns);

        _printDivider(sb, DIVIDER, _getTotalWidth());

        printRows(sb, _getRows());

        return sb;
    }
}
