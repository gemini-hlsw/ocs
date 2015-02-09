package edu.gemini.epics.acm;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

class CaApplySenderImpl implements CaApplySender {
    
    private static final Logger LOG = Logger.getLogger(CaApplySenderImpl.class.getName()); 

    private final String name;
    private final String carRecord;
    private final String applyRecord;
    private final String description;
    private EpicsReader epicsReader;
    private EpicsWriter epicsWriter;
    private ReadWriteClientEpicsChannel<CadDirective> dirChannel;
    private ReadOnlyClientEpicsChannel<Integer> val;
    private ReadOnlyClientEpicsChannel<Integer> carCLID;
    private ReadOnlyClientEpicsChannel<CarState> carVAL;
    private ReadOnlyClientEpicsChannel<String> carOMSS;
    private ReadOnlyClientEpicsChannel<String> mess;
    private static final String DIR_SUFFIX = ".DIR";
    private static final String VAL_SUFFIX = ".VAL";
    private static final String MSG_SUFFIX = ".MESS";
    private static final String CAR_VAL_SUFFIX = ".VAL";
    private static final String CAR_CLID_SUFFIX = ".CLID";
    private static final String CAR_OMSS_SUFFIX = ".OMSS";
    private long timeout;
    private TimeUnit timeoutUnit;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> timeoutFuture;
    private ChannelListener<Integer> valListener;
    private ChannelListener<Integer> carClidListener;
    private ChannelListener<CarState> carValListener;
    private State currentState;

    public CaApplySenderImpl(String name, String applyRecord, String carRecord, String description,
            EpicsService epicsService) throws CAException {
        super();
        this.name = name;
        this.carRecord = carRecord;
        this.applyRecord = applyRecord;
        this.description = description;

        epicsReader = new EpicsReaderImpl(epicsService);
        epicsWriter = new EpicsWriterImpl(epicsService);
        dirChannel = epicsWriter.getEnumChannel(applyRecord + DIR_SUFFIX,
                CadDirective.class);
        val = epicsReader.getIntegerChannel(applyRecord + VAL_SUFFIX);
        carCLID = epicsReader.getIntegerChannel(carRecord + CAR_CLID_SUFFIX);
        carVAL = epicsReader.getEnumChannel(carRecord + CAR_VAL_SUFFIX,
                CarState.class);
        carOMSS = epicsReader.getStringChannel(carRecord + CAR_OMSS_SUFFIX);
        mess = epicsReader.getStringChannel(applyRecord + MSG_SUFFIX);

        val.registerListener(valListener = new ChannelListener<Integer>() {
            @Override
            public void valueChanged(String arg0, List<Integer> newVals) {
                if (newVals != null && !newVals.isEmpty()) {
                    CaApplySenderImpl.this.onApplyValChange(newVals.get(0));
                }
            }
        });
        carCLID.registerListener(carClidListener = new ChannelListener<Integer>() {
            @Override
            public void valueChanged(String arg0, List<Integer> newVals) {
                if (newVals != null && !newVals.isEmpty()) {
                    CaApplySenderImpl.this.onCarClidChange(newVals.get(0));
                }
            }
        });
        carVAL.registerListener(carValListener = new ChannelListener<CarState>() {
            @Override
            public void valueChanged(String arg0, List<CarState> newVals) {
                if (newVals != null && !newVals.isEmpty()) {
                    CaApplySenderImpl.this.onCarValChange(newVals.get(0));
                }
            }
        });

        executor = new ScheduledThreadPoolExecutor(2);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getApply() {
        return applyRecord;
    }

    @Override
    public String getCAR() {
        return carRecord;
    }

    void unbind() {
        
        try {
            val.unRegisterListener(valListener);
            carCLID.unRegisterListener(carClidListener);
            carVAL.unRegisterListener(carValListener);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }

        try {
            epicsWriter.destroyChannel(dirChannel);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        dirChannel = null;

        try {
            epicsReader.destroyChannel(val);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        val = null;

        try {
            epicsReader.destroyChannel(carCLID);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        carCLID = null;

        try {
            epicsReader.destroyChannel(carVAL);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        carVAL = null;

        try {
            epicsReader.destroyChannel(carOMSS);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        carOMSS = null;

        try {
            epicsReader.destroyChannel(mess);
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }
        mess = null;

        epicsWriter = null;
        epicsReader = null;
    }

    @Override
    public synchronized CaCommandMonitor post() {
        CaCommandMonitorImpl cm = new CaCommandMonitorImpl();
        if (currentState != null) {
            cm.completeFailure(new CaCommandInProgress());
        } else {
            currentState = new WaitPreset(cm);

            try {
                dirChannel.setValue(CadDirective.START);
            } catch (CAException e) {
                cm.completeFailure(e);
            } catch (TimeoutException e) {
                cm.completeFailure(e);
            }

            if (timeout > 0) {
                timeoutFuture = executor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        CaApplySenderImpl.this.onTimeout();
                    }
                }, timeout, timeoutUnit);
            }
        }

        return cm;
    }

    @Override
    public CaCommandMonitor postWait() throws InterruptedException {
        CaCommandMonitor cm = post();
        cm.waitDone();
        return cm;
    }

    @Override
    public CaCommandMonitor postCallback(CaCommandListener callback) {
        CaCommandMonitor cm = post();
        cm.setCallback(callback);
        return cm;
    }

    private interface State {
        public State onApplyValChange(Integer val);

        public State onCarValChange(CarState carState);

        public State onCarClidChange(Integer val);

        public State onTimeout();
    }

    private class WaitPreset implements State {
        CaCommandMonitorImpl cm;
        CarState carVal;
        Integer carClid;

        WaitPreset(CaCommandMonitorImpl cm) {
            this.cm = cm;
        }

        @Override
        public State onApplyValChange(Integer val) {
            if (val > 0) {
                if (carClid != null && carClid.equals(val)) {
                    if (carVal == CarState.ERROR) {
                        failCommand(cm);
                        return null;
                    } else if (carVal == CarState.BUSY) {
                        return new WaitCompletion(cm, val);
                    }
                }
                return new WaitStart(cm, val, carVal, carClid);
            } else {
                failCommand(cm, mess);
                return null;
            }
        }

        @Override
        public State onCarValChange(CarState val) {
            carVal = val;
            return this;
        }

        @Override
        public State onCarClidChange(Integer val) {
            carClid = val;
            return this;
        }

        @Override
        public State onTimeout() {
            cm.completeFailure(new TimeoutException());
            return null;
        }
    }

    private class WaitStart implements State {
        CaCommandMonitorImpl cm;
        int clid;
        Integer carClid;
        CarState carState;

        WaitStart(CaCommandMonitorImpl cm, int clid, CarState carState,
                Integer carClid) {
            this.cm = cm;
            this.clid = clid;
            this.carState = carState;
            this.carClid = carClid;
        }

        @Override
        public State onApplyValChange(Integer val) {
            if (val == clid) {
                return this;
            } else {
                cm.completeFailure(new CaCommandPostError(
                        "Another command was triggered in apply record "
                                + applyRecord));
                return null;
            }
        }

        @Override
        public State onCarValChange(CarState val) {
            carState = val;
            return checkOutConditions();
        }

        @Override
        public State onCarClidChange(Integer val) {
            carClid = val;
            return checkOutConditions();
        }

        @Override
        public State onTimeout() {
            cm.completeFailure(new TimeoutException());
            return null;
        }

        private State checkOutConditions() {
            if (carClid != null && carClid.intValue() == clid) {
                if (carState == CarState.ERROR) {
                    failCommand(cm);
                    return null;
                }
                if (carState == CarState.BUSY) {
                    return new WaitCompletion(cm, clid);
                }
            }
            return this;
        }

    }

    private class WaitCompletion implements State {
        CaCommandMonitorImpl cm;
        int clid;

        WaitCompletion(CaCommandMonitorImpl cm, int clid) {
            this.cm = cm;
            this.clid = clid;
        }

        @Override
        public State onApplyValChange(Integer val) {
            if (val == clid) {
                return this;
            } else {
                cm.completeFailure(new CaCommandPostError(
                        "Another command was triggered in apply record "
                                + applyRecord));
                return null;
            }
        }

        @Override
        public State onCarValChange(CarState val) {
            if (val == CarState.ERROR) {
                failCommand(cm);
                return null;
            }
            if (val == CarState.IDLE) {
                cm.completeSuccess();
                return null;
            }
            return this;
        }

        @Override
        public State onCarClidChange(Integer val) {
            if (val == clid) {
                return this;
            } else {
                cm.completeFailure(new CaCommandPostError(
                        "Another command was triggered in apply record "
                                + applyRecord));
                return null;
            }
        }

        @Override
        public State onTimeout() {
            cm.completeFailure(new TimeoutException());
            return null;
        }

    }

    private synchronized void onApplyValChange(Integer val) {
        if (currentState != null) {
            currentState = currentState.onApplyValChange(val);
            if (currentState == null && timeoutFuture != null) {
                timeoutFuture.cancel(true);
                timeoutFuture = null;
            }
        }
    }

    private synchronized void onCarClidChange(Integer val) {
        if (currentState != null) {
            currentState = currentState.onCarClidChange(val);
            if (currentState == null && timeoutFuture != null) {
                timeoutFuture.cancel(true);
                timeoutFuture = null;
            }
        }
    }

    private synchronized void onCarValChange(CarState carState) {
        if (currentState != null) {
            currentState = currentState.onCarValChange(carState);
            if (currentState == null && timeoutFuture != null) {
                timeoutFuture.cancel(true);
                timeoutFuture = null;
            }
        }
    }

    private synchronized void onTimeout() {
        if (currentState != null) {
            timeoutFuture = null;
            currentState = currentState.onTimeout();
        }
    }

    @Override
    public synchronized boolean isActive() {
        return currentState != null;
    }

    @Override
    public synchronized void setTimeout(long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeoutUnit = timeUnit;
    }

    private void failCommand(final CaCommandMonitorImpl cm) {
        failCommand(cm, carOMSS);
    }
    
    private void failCommand(final CaCommandMonitorImpl cm, final ReadOnlyClientEpicsChannel<String> msgSrc) {
        // I found that if I try to read OMSS from the same thread that is
        // processing a channel notifications, the reads fails with a timeout.
        // But it works if the read is done later from another thread.
        executor.execute(new Runnable () {
            @Override
            public void run() {
                String msg = null;
                try {
                    msg = msgSrc.getFirst();
                } catch (CAException e) {
                    LOG.warning(e.getMessage());
                } catch (TimeoutException e) {
                    LOG.warning(e.getMessage());
                }
                cm.completeFailure(new CaCommandError(msg));
            }
        });
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void clear() throws TimeoutException {
        if (dirChannel != null) {
            try {
                dirChannel.setValue(CadDirective.CLEAR);
            } catch (CAException e) {
                LOG.warning(e.getMessage());
            }
        }
    }

}
