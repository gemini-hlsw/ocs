package edu.gemini.obslog.TextExport;

import edu.gemini.obslog.TextExport.support.InstrumentTextSegmentExporter;
import edu.gemini.obslog.TextExport.support.TextSegmentInfoExporter;
import edu.gemini.obslog.TextExport.support.ITextExporter;
import edu.gemini.obslog.obslog.*;
import edu.gemini.spModel.core.SPProgramID;

//
// Gemini Observatory/AURA
// $Id: TextOnlySegmentExporter.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

/**
 * Top-level class for providing a text-based export obslog file.
 */
public class TextOnlySegmentExporter {
    private IObservingLog _observingLog;
    private StringBuilder _sb;
    private String _title;
    private SPProgramID _planID;
    private OlLogOptions _options;
    private ITextExporter.ITextExporterFactory _factory;

    public TextOnlySegmentExporter(IObservingLog observingLog, String title, OlLogOptions options, SPProgramID planID) {
        if (observingLog == null) throw new NullPointerException("ObservingLog must not be null for export");
        _observingLog = observingLog;
        _title = title;
        _options = options;
        _planID = planID;
        _factory = null;
    }

    public TextOnlySegmentExporter(IObservingLog observingLog, ITextExporter.ITextExporterFactory factory, String title, OlLogOptions options, SPProgramID planID) {
        this(observingLog,title,options,planID);
        _factory = factory;
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
            TextSegmentInfoExporter exp = new TextSegmentInfoExporter(_options, _planID, _title);
            infoBuffer = exp.export(infoBuffer);
        }

        StringBuilder segsBuffer = new StringBuilder();

        if (observingLog.getLogSegmentCount() > 0) {
            for (IObservingLogSegment segment : observingLog.getLogSegments()) {
                ITextExporter exporter;

                if (_factory == null) {
                    exporter = new InstrumentTextSegmentExporter(segment);
                } else {
                    exporter = _factory.create(segment);
                }
                segsBuffer = exporter.export(segsBuffer);
            }
        }

        fullView.append(infoBuffer);
        fullView.append(segsBuffer);

        return fullView.toString();
    }

}
