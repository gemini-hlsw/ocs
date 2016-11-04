package edu.gemini.obslog.TextExport;

import edu.gemini.obslog.TextExport.support.InstrumentTextSegmentExporter;
import edu.gemini.obslog.TextExport.support.TextLogInfoExporter;
import edu.gemini.obslog.TextExport.support.WeatherTextSegmentExporter;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.*;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.util.*;
import java.util.logging.Logger;

/**
 * Top-level class for providing a text-based export obslog file.
 */
public class TextJorgeObslogExporter {
    public static final Logger LOG = Logger.getLogger(TextJorgeObslogExporter.class.getName());

    private IObservingLog _observingLog;
    private StringBuilder _sb;
    private SPProgramID _planID;
    private OlLogOptions _options;

    public TextJorgeObslogExporter(IObservingLog observingLog, OlLogOptions options, SPProgramID planID) {
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

    List<SPProgramID> _getUniquePrograms(IObservingLog observingLog) {
        Set<SPProgramID> progs = new HashSet<SPProgramID>();

        if (observingLog.getLogSegmentCount() > 0) {
            for (IObservingLogSegment segment : observingLog.getLogSegments()) {
                for (ConfigMap cmap : segment.getRows()) {
                    String observationID = (String) cmap.get(ConfigMapUtil.OBSLOG_OBSERVATIONID_ITEM_NAME);
                    SPObservationID spObsID;
                    try {
                        spObsID = new SPObservationID(observationID);
                    } catch (SPBadIDException ex) {
                        LOG.info("Observation ID converstion failed: " + observationID);
                        continue;
                    }
                    progs.add(spObsID.getProgramID());
                }
            }
        }
        // Make it a list so it can be sorted and returned
        List<SPProgramID> lprogs = new ArrayList<SPProgramID>(progs);
        Collections.sort(lprogs);
        return lprogs;
    }

    private List<IObservingLogSegment> _mergeLogSegments(List<IObservingLogSegment> segments) {
        if (segments.size() <= 1) return segments;


        List<OlSegmentType> mergedTypes = new ArrayList<OlSegmentType>();
        List<IObservingLogSegment> mergedSegments = new ArrayList<IObservingLogSegment>();

        for (int i=0; i<segments.size(); i++) {

            IObservingLogSegment seg = segments.get(i);

            if (mergedTypes.contains(seg.getType())) continue;

            mergedTypes.add(seg.getType());
            for (int j=i+1; j<segments.size(); j++) {
                IObservingLogSegment mergeSegment = segments.get(j);
                if (seg.getType().equals(mergeSegment.getType())) {
                    seg.mergeSegment(mergeSegment);
                }
            }
            mergedSegments.add(seg);
        }
        return mergedSegments;
    }

    public String export() throws OlLogException {
        StringBuilder fullView = _getBuffer();
        IObservingLog observingLog = _getObservingLog();

        if (observingLog == null) {
            throw new OlLogException("Observing Log was null.  No report possible.");
        }

        if (observingLog.getLogSegmentCount() == 0) {
            return "No segments in log.";
        }

        // This creates the header for each section
        StringBuilder infoBuffer = new StringBuilder();
        OlLogInformation logInfo = observingLog.getLogInformation();
        if (logInfo != null) {
            TextLogInfoExporter exp = new TextLogInfoExporter(logInfo, _options, _planID);
            infoBuffer = exp.export(infoBuffer);
        }

        // This creates the weather for each section
        StringBuilder weatherBuffer = new StringBuilder();
        IObservingLogSegment weatherSegment = observingLog.getWeatherSegment();
        if (weatherSegment != null) {
            WeatherTextSegmentExporter exporter = new WeatherTextSegmentExporter(weatherSegment);
            weatherBuffer = exporter.export(weatherBuffer);
        }

        // This will order and remove duplicate programs
        List<SPProgramID> progs = _getUniquePrograms(observingLog);

        // Now for each program, go through all the segments
        // First merge segments so there is only one segment/instrument/program
        List<IObservingLogSegment> mergedSegments = _mergeLogSegments(observingLog.getLogSegments());

        for (SPProgramID spProg : progs) {
            StringBuilder segsBuffer = new StringBuilder();

            for (IObservingLogSegment segment: mergedSegments) {
                segsBuffer = new InstrumentTextSegmentExporter(segment, spProg).export(segsBuffer);
            }

            fullView.append(infoBuffer);
            fullView.append(weatherBuffer);
            fullView.append(segsBuffer);
        }

        return fullView.toString();
    }

}
