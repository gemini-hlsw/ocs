package edu.gemini.obslog.obslog;

import edu.gemini.obslog.config.model.OlConfiguration;
import edu.gemini.obslog.config.model.OlObsLogData;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.instruments.*;
import edu.gemini.obslog.transfer.EObslogVisit;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.List;

public class InstrumentSegmentBuilder {
    private static final Logger LOG = Logger.getLogger(InstrumentSegmentBuilder.class.getName());

    private OlConfiguration _obsLogConfig;
    private OlLogOptions _obsLogOptions;

    public static IObservingLog create(IObservingLog obsLog, OlConfiguration obsLogConfig, OlLogOptions obsLogOptions, List<EObslogVisit> transferDataList) throws OlLogException {
        if (transferDataList == null || transferDataList.size() == 0 || obsLogOptions == null || obsLog == null) return null;

        InstrumentSegmentBuilder builder = new InstrumentSegmentBuilder(obsLogConfig, obsLogOptions);

        builder._addOneSegment(obsLog, transferDataList);

        return obsLog;
    }

    /**
     * OlSegmentBuidler instances are created using the factory method.  This is the private constructor.
     *
     * @param obsLogConfig  log configuration
     * @param obsLogOptions log options
     */
    private InstrumentSegmentBuilder(OlConfiguration obsLogConfig, OlLogOptions obsLogOptions) {
        _obsLogConfig = obsLogConfig;
        _obsLogOptions = obsLogOptions;
    }


    private OlConfiguration _getObsLogConfig() {
        return _obsLogConfig;
    }

    private OlLogOptions _getObsLogOptions() {
        return _obsLogOptions;
    }

    /**
     * Breaks the observations into segments and creates new segments adding them in the proper
     * order to the observing log.
     *
     * @param obsLog           an instance of a class implementing <tt>IObservingLog</tt>.
     * @param transferDataList a <tt>List</tt> of {@link EObslogVisit} instances.
     * @throws NullPointerException throws when the <tt>observationDataList</tt> is null or the
     *                              <tt>obsLog</tt> parameter is null.
     */
    private void _addOneSegment(IObservingLog obsLog, List<EObslogVisit>transferDataList) throws OlLogException {

        int obsCount = transferDataList.size();
        if (obsCount == 0) return;

        // Create a new segment and add observation data until a new segment type is encountered
        InstrumentLogSegment seg = _createSegment(transferDataList);
        for (int i = 0; i < obsCount; i++) {
            EObslogVisit obsData = transferDataList.get(i);
            if (!obsData.getType().equals(seg.getType())) {
                break;
            }
            seg.addObservationData(obsData);
        }

        // add the completed segment to the observing log
        int segSize = seg.getSize();
        obsLog.addLogSegment(seg);

        // Check to see if there are observations left, if so, call again with the unprocessed
        // end of the list
        if (segSize < obsCount) {
            _addOneSegment(obsLog, transferDataList.subList(seg.getSize(), transferDataList.size()));
        }
    }

    private InstrumentLogSegment _createSegment(List<EObslogVisit> transferData) throws IllegalArgumentException, OlLogException {
        if (transferData == null || transferData.size() == 0) return null;

        // Get the log type
        OlSegmentType type = transferData.get(0).getType();
        // Check the config for information on that type
        OlConfiguration obsLogConfig = _getObsLogConfig();
        if (obsLogConfig == null) {
            // Currently there isn't anything I can do since the generic bean is a failure
            throw new OlLogException("No observation log configuration was provided.  Unable to create segments.");
        }
        OlObsLogData logData = obsLogConfig.getDataForLogByType(type.getType());
        if (logData == null) {
            throw new OlLogException("No configuration data for instrument: " + type.toString());
        }

        LOG.log(Level.FINE, "Inst type: " + type.toString());
        if (type.equals(GMOSLogSegment.SEG_TYPE)) {
            return new GMOSLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(GNIRSLogSegment.SegmentType())) {
            return new GNIRSLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(GsaoiLogSegment.SEG_TYPE)) {
            return new GsaoiLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(TRECSLogSegment.SEG_TYPE)) {
            return new TRECSLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(NIRILogSegment.SEG_TYPE)) {
            return new NIRILogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(MichelleLogSegment.SEG_TYPE)) {
            return new MichelleLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(AcqCamLogSegment.SEG_TYPE)) {
            return new AcqCamLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(BHROSLogSegment.SEG_TYPE)) {
            return new BHROSLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(NIFSLogSegment.SEG_TYPE)) {
            return new NIFSLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(NICILogSegment.SEG_TYPE)) {
            return new NICILogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(Flamingos2LogSegment.SEG_TYPE)) {
            return new Flamingos2LogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(VisitorInstrumentLogSegment.SEG_TYPE)) {
            return new VisitorInstrumentLogSegment(logData.getLogTableData(), _getObsLogOptions());
        } else if (type.equals(GpiLogSegment.SEG_TYPE)) {
            return new GpiLogSegment(logData.getLogTableData(), _getObsLogOptions());
        }
        // Default catches unknown instruments
        return new UnknownLogSegment(logData.getLogTableData(), _getObsLogOptions());
    }
}
