package edu.gemini.obslog.TextExport;

import edu.gemini.obslog.TextExport.support.InstrumentTextSegmentExporter;
import edu.gemini.obslog.TextExport.support.TextLogInfoExporter;
import edu.gemini.obslog.TextExport.support.WeatherTextSegmentExporter;
import edu.gemini.obslog.obslog.*;
import edu.gemini.spModel.core.SPProgramID;

//
// Gemini Observatory/AURA
// $Id: TextObslogExporter.java,v 1.3 2006/12/05 14:56:16 gillies Exp $
//

/**
 * Top-level class for providing a text-based export obslog file.
 */
public class TextObslogExporter {
    private IObservingLog _observingLog;
    private StringBuilder _sb;
    private SPProgramID _planID;
    private OlLogOptions _options;

    public TextObslogExporter(IObservingLog observingLog, OlLogOptions options, SPProgramID planID) {
        if (observingLog == null) throw new NullPointerException("ObservingLog must not be null for export");
        _observingLog = observingLog;
        _options = options;
        _planID = planID;
    }

    private StringBuilder _getBuffer() {
        if (_sb == null) {
            _sb = new StringBuilder();
        }
        return _sb;
    }

    private IObservingLog _getObservingLog() {
        return _observingLog;
    }

    public String export() throws OlLogException {
        StringBuilder fullView = _getBuffer();
        IObservingLog observingLog = _getObservingLog();

        if (observingLog == null) {
            throw new OlLogException("Observing Log was null.  No report possible.");
        }

        StringBuilder infoBuffer = new StringBuilder();
        OlLogInformation logInfo = observingLog.getLogInformation();
        if (logInfo != null) {
            TextLogInfoExporter exp = new TextLogInfoExporter(logInfo, _options, _planID);
            infoBuffer = exp.export(infoBuffer);
        }

        StringBuilder segsBuffer = new StringBuilder();
        if (observingLog.getLogSegmentCount() > 0) {
            for (IObservingLogSegment segment : observingLog.getLogSegments()) {
                InstrumentTextSegmentExporter exporter = new InstrumentTextSegmentExporter(segment);
                segsBuffer = exporter.export(segsBuffer);
            }
        }

        StringBuilder weatherBuffer = new StringBuilder();
        IObservingLogSegment weatherSegment = observingLog.getWeatherSegment();
        if (weatherSegment != null) {
            WeatherTextSegmentExporter exporter = new WeatherTextSegmentExporter(weatherSegment);
            weatherBuffer = exporter.export(weatherBuffer);
        }

        fullView.append(infoBuffer);
        fullView.append(weatherBuffer);
        fullView.append(segsBuffer);

        return fullView.toString();
    }

}
