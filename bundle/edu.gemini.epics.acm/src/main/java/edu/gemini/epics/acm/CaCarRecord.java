package edu.gemini.epics.acm;

import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.EpicsService;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.api.ChannelListener;
import edu.gemini.epics.impl.EpicsReaderImpl;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.logging.Logger;

final class CaCarRecord {
    private static final Logger LOG = Logger.getLogger(CaCarRecord.class
            .getName());

    private static final String CAR_VAL_SUFFIX = ".VAL";
    private static final String CAR_CLID_SUFFIX = ".CLID";
    private static final String CAR_OMSS_SUFFIX = ".OMSS";

    private final String epicsName;
    private EpicsReader epicsReader;
    private ReadOnlyClientEpicsChannel<Integer> clid;
    private ReadOnlyClientEpicsChannel<CarState> val;
    private ReadOnlyClientEpicsChannel<String> omss;

    CaCarRecord(String epicsName, EpicsService epicsService) {
        this.epicsName = epicsName;
        epicsReader = new EpicsReaderImpl(epicsService);
        clid = epicsReader.getIntegerChannel(epicsName + CAR_CLID_SUFFIX);
        val = epicsReader.getEnumChannel(epicsName + CAR_VAL_SUFFIX,
                CarState.class);
        omss = epicsReader.getStringChannel(epicsName + CAR_OMSS_SUFFIX);
    }

    void unbind() {
        try {
            epicsReader.destroyChannel(clid);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        clid = null;

        try {
            epicsReader.destroyChannel(val);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        val = null;

        try {
            epicsReader.destroyChannel(omss);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        omss = null;

        epicsReader = null;
    }

    String getEpicsName() {
        return epicsName;
    }

    void registerClidListener(ChannelListener<Integer> listener) throws CAException {
        clid.registerListener(listener);
    }

    void unregisterClidListener(ChannelListener<Integer> listener) throws CAException {
        clid.unRegisterListener(listener);
    }

    void registerValListener(ChannelListener<CarState> listener) throws CAException {
        val.registerListener(listener);
    }

    void unregisterValListener(ChannelListener<CarState> listener) throws CAException {
        val.unRegisterListener(listener);
    }

    CarState getValValue() throws CAException, TimeoutException {
        return val.getFirst();
    }

    String getOmssValue() throws CAException, TimeoutException {
        return omss.getFirst();
    }

}
