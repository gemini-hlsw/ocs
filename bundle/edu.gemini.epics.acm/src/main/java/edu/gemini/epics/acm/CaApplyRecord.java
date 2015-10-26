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

final class CaApplyRecord {

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

    private ChannelListener<Integer> valListener;

    CaApplyRecord(String epicsName, EpicsService epicsService) {
        this.epicsName = epicsName;

        epicsReader = new EpicsReaderImpl(epicsService);
        epicsWriter = new EpicsWriterImpl(epicsService);

        updateChannels();
    }

    synchronized void updateChannels() {
        if (dir == null) {
            try {
                dir = epicsWriter.getEnumChannel(epicsName + DIR_SUFFIX, CadDirective.class);
            } catch (Throwable e) {
                LOG.warning(e.getMessage());
            }
        }
        if (val == null) {
            try {
                val = epicsReader.getIntegerChannel(epicsName + VAL_SUFFIX);
                if (valListener != null) {
                    val.registerListener(valListener);
                }
            } catch (Throwable e) {
                LOG.warning(e.getMessage());
            }
        }
        if (mess == null) {
            try {
                mess = epicsReader.getStringChannel(epicsName + MSG_SUFFIX);
            } catch (Throwable e) {
                LOG.warning(e.getMessage());
            }
        }
    }

    synchronized void unbind() {
        assert(epicsReader!=null);
        assert(epicsWriter!=null);

        if(dir!=null) {
            try {
                epicsWriter.destroyChannel(dir);
            } catch (CAException e) {
                LOG.warning(e.getMessage());
            }
            dir = null;
        }

        if(val!=null) {
            try {
                epicsReader.destroyChannel(val);
            } catch (CAException e) {
                LOG.warning(e.getMessage());
            }
            val = null;
        }

        if(mess!=null) {
            try {
                epicsReader.destroyChannel(mess);
            } catch (CAException e) {
                LOG.warning(e.getMessage());
            }
            mess = null;
        }

        epicsWriter = null;
        epicsReader = null;
    }
    
    synchronized void registerValListener(ChannelListener<Integer> listener) throws CAException {
        if(val!=null) {
            val.registerListener(listener);
        }
        valListener = listener;
    }
    
    synchronized void unregisterValListener(ChannelListener<Integer> listener) throws CAException {
        if(val!=null) {
            val.unRegisterListener(listener);
        }
        valListener = null;
    }
    
    synchronized int getValValue() throws CAException, TimeoutException {
        if(val!=null) {
            return val.getFirst();
        } else {
            throw new CAException("Tried to read from unbound channel  " + epicsName + VAL_SUFFIX);
        }
    }

    synchronized String getMessValue() throws CAException, TimeoutException {
        if(mess!=null) {
            return mess.getFirst();
        } else {
            throw new CAException("Tried to read from unbound channel  " + epicsName + MSG_SUFFIX);
        }
    }
    
    synchronized void setDir(CadDirective directive) throws CAException, TimeoutException {
        if(dir!=null) {
            dir.setValue(directive);
        } else {
            throw new CAException("Tried to read from unbound channel  " + epicsName + DIR_SUFFIX);
        }
    }
    
    public String getEpicsName() {
        return epicsName;
    }
}
