package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.IObservingLogSegment;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

//
// Gemini Observatory/AURA
// $Id: AbstractTextSegmentExporter.java,v 1.3 2006/12/05 15:14:02 gillies Exp $
//

/**
 * This class takes a segment and produces a table in the traditional obslog ascii text format.
 */
public abstract class AbstractTextSegmentExporter extends TextExportBase {
    private static final Logger LOG = Logger.getLogger(AbstractTextSegmentExporter.class.getName());

    protected static final Pattern ENDLINE_PATTERN = Pattern.compile("$", Pattern.MULTILINE);

    private IObservingLogSegment _segment;
    private List<ConfigMap> _rows;
    private Map<String, ColumnInfo> _columnInfo;
    private int _totalWidth;

    protected class ColumnInfo {
        OlLogItem _logItem;
        int _headingWidth;
        int _maxColumnWidth;

        ColumnInfo(OlLogItem logItem) {
            if (logItem == null) throw new NullPointerException("logItem is null");
            _logItem = logItem;
            _headingWidth = logItem.getColumnHeading().length();
            // While initializing assume heading width and column width are the same
            _maxColumnWidth = _headingWidth;
        }

        String getColumnHeading() {
            return _logItem.getColumnHeading();
        }

        String getProperty() {
            return _logItem.getProperty();
        }

        int getHeadingWidth() {
            return _headingWidth;
        }

        void setHeadingWidth(int headingWidth) {
            _headingWidth = headingWidth;
        }

        int getMaxColumnWidth() {
            return _maxColumnWidth;
        }

        void setMaxColumnWidth(int maxColumnWidth) {
            _maxColumnWidth = maxColumnWidth;
        }
    }

    public AbstractTextSegmentExporter(IObservingLogSegment segment) {
        if (segment == null) throw new NullPointerException("Segment argument is null for export");
        _segment = segment;
        _build();
    }

    protected Map<String,ColumnInfo> _getColumnInfoMap() {
        if (_columnInfo == null) {
            _columnInfo = new LinkedHashMap<String,ColumnInfo>();
        }
        return _columnInfo;
    }

    protected int _getTotalWidth() {
        return _totalWidth;
    }

    protected List<ConfigMap> _getRows() {
        return _rows;
    }

    protected IObservingLogSegment _getSegment() {
        return _segment;
    }

    // Private method to build all the strucutures needed for the text segment export
    private void _build() {
        // Get the rows -- done once since this requires some work
        _rows = _segment.getRows();

        _getTableInfo();
    }

    private void _setRowWidth(List<ConfigMap> rowMaps, ColumnInfo cinfo) {
        int width = cinfo.getHeadingWidth();
        String property = cinfo.getProperty();

        for (int i = 0, size = rowMaps.size(); i < size; i++) {
            Map rowMap = rowMaps.get(i);

            String value = (String) rowMap.get(property);
            if (value == null) {
                if (LOG.isLoggable(Level.FINE)) LOG.fine("Property: " + property + " missing in map for obslog");
                continue;
            }
            if (value.length() > width) width = value.length();
        }
        // Set the maximum column length + space
        cinfo.setMaxColumnWidth(width);
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Width for: " + property + " is: " + width);
    }

    private void _setTotalWidth() {
        Map<String,ColumnInfo> columns = _getColumnInfoMap();

        // Now go through and figure out the column widths based upon the maximum value width
        int totalWidth = 0;
        for (ColumnInfo cinfo : columns.values()) {
            // Now look through the rows
            totalWidth += cinfo.getMaxColumnWidth();
            totalWidth += COLUMN_SPACE.length();
        }
        _totalWidth = totalWidth;
        if (LOG.isLoggable(Level.FINE)) LOG.fine("Total width: " + totalWidth);
    }

    protected OlLogItem _lookupColumnInfo(String propertyName) {
        for (OlLogItem logItem : _segment.getTableInfo()) {
            if (logItem.getProperty().equals(propertyName)) {
                return logItem;
            }
        }
        return null;
    }

    private void _getTableInfo() {
        Map<String, ColumnInfo> columns = _getColumnInfoMap();

        // This call to the abstract method gives exporters a chance to setup initial table columns
        _setupColumns();

        // First create all the table column information
        for (OlLogItem logItem  : _segment.getVisibleTableInfo()) {
            ColumnInfo cinfo = new ColumnInfo(logItem);
            // Now add it to the map
            columns.put(logItem.getProperty(), cinfo);
        }

        // Now go through and figure out the column widths based upon the maximum value width
        for (ColumnInfo cinfo : columns.values()) {
            // Now look through the rows
            _setRowWidth(_getRows(), cinfo);
        }

        _setTotalWidth();
    }

    protected void _printJustifiedMultilineComment(StringBuilder sb, int maxColWidth, String prefix, String comment) {
        //String[] lines = _splitComment(comment);
        //LOG.info("Length is: " + lines.length);
        StringTokenizer st = new StringTokenizer(comment, "\n\r");
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            _printJustifiedText(sb, maxColWidth, prefix);
            sb.append(line);
            sb.append(NEWLINE);
        }
    }

    protected void _printOneRow(StringBuilder sb, ConfigMap row) {
        Map<String,ColumnInfo> columns = _getColumnInfoMap();

        for (ColumnInfo cinfo : columns.values()) {
            String value = row.sget(cinfo.getProperty());
            if (value == null) {
                LOG.fine("Final value for: " + cinfo.getProperty() + " was null.");
                value = MISSING_VALUE;
            }
            _printJustifiedText(sb, cinfo.getMaxColumnWidth(), value);
        }
    }

   protected void _printHeading(StringBuilder sb,  List<ColumnInfo> columns) {
        for (ColumnInfo cinfo : columns) {
            _printJustifiedText(sb, cinfo.getMaxColumnWidth(), cinfo.getColumnHeading());
        }
        sb.append(AbstractTextSegmentExporter.NEWLINE);
    }

    protected void _printHeading(StringBuilder sb) {
        Map<String,ColumnInfo> columns = _getColumnInfoMap();
        _printHeading(sb, new ArrayList<ColumnInfo>(columns.values()));
    }

    /**
     * This is called during initialization to allow subclasses a chance to play with the column ordering
     * User should use <tt>_getColumnInfoMap()</tt> and set the columns
     */
    protected abstract void _setupColumns();

    public abstract StringBuilder export(StringBuilder sb);

}

