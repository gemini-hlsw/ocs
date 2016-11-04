package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.ConfigMapUtil;
import edu.gemini.obslog.obslog.IObservingLogSegment;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.util.List;
import java.util.Map;

/**
 * Gemini Observatory/AURA
 * $Id: InstrumentTextSegmentExporter.java,v 1.2 2006/12/05 14:56:16 gillies Exp $
 */
public class InstrumentTextSegmentExporter extends AbstractDatasetExporterSupport implements ITextExporter {
    static protected final String OBSERVATION_ID_PROPERTY_NAME = ConfigMapUtil.OBSLOG_OBSERVATIONID_ITEM_NAME;

    // When set, only look for observations from  this program
    private final Option<SPProgramID> _thisProgramOnly;

    public InstrumentTextSegmentExporter(IObservingLogSegment segment) {
        super(segment);
        _thisProgramOnly = None.instance();
    }

    public InstrumentTextSegmentExporter(IObservingLogSegment segment, SPProgramID pid) {
        super(segment);
        _thisProgramOnly = ImOption.apply(pid);
    }

    protected void _setupColumns() {
        Map<String, ColumnInfo> columns = _getColumnInfoMap();

        // For weather, special case the time info
        OlLogItem logItem = _lookupColumnInfo(OBSERVATION_ID_PROPERTY_NAME);
        ColumnInfo cinfo = new ColumnInfo(logItem);
        columns.put(logItem.getProperty(), cinfo);
    }

    private void _printComment(StringBuilder sb, ConfigMap row) {
        ColumnInfo obsIDinfo = _getColumnInfoMap().get(OBSERVATION_ID_PROPERTY_NAME);
        // Could be null for segments like weather
        if (obsIDinfo == null) return;

        String observationID = row.sget(OBSERVATION_ID_PROPERTY_NAME);
        String comment = row.sget(COMMENT_PROPERTY_NAME);

        //String[] lines = _splitComment(comment);
        //LOG.info("Length is: " + lines.length);
        _printJustifiedMultilineComment(sb, obsIDinfo.getMaxColumnWidth(), observationID, comment);
    }

    // Check to make the text a little clearer
    private boolean _oneProgramOnly() {
        return _thisProgramOnly.isDefined();
    }
    private boolean _useThisRow(ConfigMap rowMap) {
        return _thisProgramOnly.forall(pid -> {
            try {
                return new SPObservationID(rowMap.sget(OBSERVATION_ID_PROPERTY_NAME)).getProgramID().equals(pid);
            } catch (SPBadIDException ex) {
                return false;
            }
        });
    }

    // This goes through all the rows in the segment checking to see if an observation matches the sample observation
    private boolean _inThisSegment(List<ConfigMap> rowMaps) {
        for (ConfigMap rowMap : rowMaps) {
            if (_useThisRow(rowMap)) return true;
        }
        return false;
    }



    private void _printRows(StringBuilder sb, List<ConfigMap> rowMaps) {

        int size = rowMaps.size();
        if (size == 0) {
            sb.append("No information in the database");
            sb.append(NEWLINE);
        }

        for (ConfigMap rowMap : rowMaps) {
            // Check to see if we should use this row
            if (_oneProgramOnly() && !_useThisRow(rowMap)) continue;

            _printOneRow(sb, rowMap);
            sb.append(NEWLINE);
            // Prints each rows
            _printComment(sb, rowMap);
            _printDatasetComments(sb, rowMap);
        }
        sb.append(NEWLINE);
    }

    public StringBuilder export(StringBuilder sb) {

        List<ConfigMap> rowMaps = _getRows();

        if (_oneProgramOnly() && !_inThisSegment(rowMaps)) return sb;

        // This segment has some observation in the program
        _printCaption(sb, _getSegment().getSegmentCaption());

        _printDivider(sb, DIVIDER, _getTotalWidth());

        _printHeading(sb);

        _printDivider(sb, DIVIDER, _getTotalWidth());

        _printRows(sb, _getRows());

        return sb;
    }
}
