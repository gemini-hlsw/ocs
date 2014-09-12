package edu.gemini.obslog.TextExport.support;

import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.IObservingLogSegment;
import edu.gemini.obslog.obslog.ConfigMapUtil;
import edu.gemini.obslog.config.model.OlLogItem;

import java.util.List;
import java.util.Map;

/**
 * Gemini Observatory/AURA
 * $Id: WeatherTextSegmentExporter.java,v 1.1 2006/12/05 14:56:16 gillies Exp $
 */
public class WeatherTextSegmentExporter extends AbstractTextSegmentExporter implements ITextExporter {
    static protected final String WEATHER_TIME_PROPERTY_NAME = ConfigMapUtil.OBSLOG_WEATHER_TIME_ITEM_NAME;
    static protected final String COMMENT_PROPERTY_NAME = ConfigMapUtil.OBSLOG_COMMENT_ITEM_NAME;

    public WeatherTextSegmentExporter(IObservingLogSegment segment) {
        super(segment);
    }

    public static ITextExporterFactory FACTORY = new ITextExporterFactory() {
        public ITextExporter create(IObservingLogSegment segment) {
            return new WeatherTextSegmentExporter(segment);
        }
    };

    protected void _setupColumns() {
        Map<String, ColumnInfo> columns = _getColumnInfoMap();

        // For weather, special case the time info
        OlLogItem logItem  = _lookupColumnInfo(WEATHER_TIME_PROPERTY_NAME);
        ColumnInfo cinfo = new ColumnInfo(logItem);
        columns.put(logItem.getProperty(), cinfo);
    }

    private void _printWeatherComment(StringBuilder sb, ConfigMap row) {
        ColumnInfo obsIDinfo = _getColumnInfoMap().get(WEATHER_TIME_PROPERTY_NAME);
        // Could be null for segments like weather
        if (obsIDinfo == null) return;

        String weatherTime = row.sget(WEATHER_TIME_PROPERTY_NAME);
        String comment = row.sget(COMMENT_PROPERTY_NAME);

        //String[] lines = _splitComment(comment);
        //LOG.info("Length is: " + lines.length);
        _printJustifiedMultilineComment(sb, obsIDinfo.getMaxColumnWidth(), weatherTime, comment);
    }

    private void _printRows(StringBuilder sb, List<ConfigMap> rowMaps) {

        int size = rowMaps.size();
        if (size == 0) {
            sb.append("No information in the database");
            sb.append(NEWLINE);
        }

        for (int i = 0; i < size; i++) {
            ConfigMap rowMap = rowMaps.get(i);
            _printOneRow(sb, rowMap);
            sb.append(NEWLINE);
            // Prints each rows
            _printWeatherComment(sb, rowMap);
        }
        sb.append(NEWLINE);
    }

    public StringBuilder export(StringBuilder sb) {

        _printCaption(sb, _getSegment().getSegmentCaption());

        _printDivider(sb, DIVIDER, _getTotalWidth());

        _printHeading(sb);

        _printDivider(sb, DIVIDER, _getTotalWidth());

        _printRows(sb, _getRows());

        return sb;
    }
}
