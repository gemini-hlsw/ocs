package edu.gemini.epics.acm;

import java.util.logging.Logger;

import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.EpicsService;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import edu.gemini.epics.api.ChannelListener;
import edu.gemini.epics.impl.EpicsReaderImpl;
import edu.gemini.epics.impl.EpicsWriterImpl;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

public final class CaApplyRecord {

    private static final Logger LOG = Logger.getLogger(CaApplyRecord.class
            .getName());

    private static final String DIR_SUFFIX = ".DIR";
    private static final String VAL_SUFFIX = ".VAL";
    private static final String MSG_SUFFIX = ".MESS";

    private final String epicsName;

    private EpicsReader epicsReader;
    private EpicsWriter epicsWriter;
    private ReadWriteClientEpicsChannel<CadDirective> dir;
    private ReadOnlyClientEpicsChannel<Integer> val;
    private ReadOnlyClientEpicsChannel<String> mess;

    CaApplyRecord(String epicsName, EpicsService epicsService) {
        this.epicsName = epicsName;

        epicsReader = new EpicsReaderImpl(epicsService);
        epicsWriter = new EpicsWriterImpl(epicsService);
        dir = epicsWriter.getEnumChannel(epicsName + DIR_SUFFIX,
                CadDirective.class);
        val = epicsReader.getIntegerChannel(epicsName + VAL_SUFFIX);
        mess = epicsReader.getStringChannel(epicsName + MSG_SUFFIX);
    }

    void unbind() {

        try {
            epicsWriter.destroyChannel(dir);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        dir = null;

        try {
            epicsReader.destroyChannel(val);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        val = null;

        try {
            epicsReader.destroyChannel(mess);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        mess = null;

        epicsWriter = null;
        epicsReader = null;
    }
    
    void registerValListener(ChannelListener<Integer> listener) throws CAException {
        val.registerListener(listener);
    }
    
    void unregisterValListener(ChannelListener<Integer> listener) throws CAException {
        val.unRegisterListener(listener);
    }
    
    int getValValue() throws CAException, TimeoutException {
        return val.getFirst();
    }

    String getMessValue() throws CAException, TimeoutException {
        return mess.getFirst();
    }
    
    void setDir(CadDirective directive) throws CAException, TimeoutException {
        dir.setValue(directive);
    }
    
    public String getEpicsName() {
        return epicsName;
    }
}
