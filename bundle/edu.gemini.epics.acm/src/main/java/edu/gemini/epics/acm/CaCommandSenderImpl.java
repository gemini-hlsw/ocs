package edu.gemini.epics.acm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.gemini.epics.EpicsService;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import edu.gemini.epics.impl.EpicsWriterImpl;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

final class CaCommandSenderImpl implements CaCommandSender {
    
    private static final Logger LOG = Logger.getLogger(CaCommandSenderImpl.class.getName()); 

    private static final String DIR_SUFFIX = ".DIR";
    private final String name;
    private final String description;
    private final Map<String, CaParameterImpl<String>> stringParameters;
    private final Map<String, CaParameterImpl<Double>> doubleParameters;
    private final Map<String, CaParameterImpl<Float>> floatParameters;
    private final Map<String, CaParameterImpl<Integer>> integerParameters;
    private final CaApplySender apply;
    private EpicsWriter epicsWriter;
    private ReadWriteClientEpicsChannel<CadDirective> dirChannel;

    private CaCommandSenderImpl(String name, CaApplySender apply,
                                String description, EpicsService epicsService) {
        this(name, apply, description, epicsService, null);
    }

    public CaCommandSenderImpl(String name, CaApplySender apply,
            String description, EpicsService epicsService, String cadName) {
        this.name = name;
        this.apply = apply;
        this.description = description;
        this.epicsWriter = new EpicsWriterImpl(epicsService);

        if (cadName != null) {
            try {
                dirChannel = epicsWriter.getEnumChannel(cadName + DIR_SUFFIX, CadDirective.class);
            } catch(Throwable e) {
                LOG.warning(e.getMessage());
            }
        }

        stringParameters = new HashMap<>();
        doubleParameters = new HashMap<>();
        floatParameters = new HashMap<>();
        integerParameters = new HashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getInfo() {
        Set<String> set = new HashSet<>(doubleParameters.keySet());
        set.addAll(floatParameters.keySet());
        set.addAll(integerParameters.keySet());
        set.addAll(stringParameters.keySet());
        return set;
    }

    public void unbind() {
        assert (epicsWriter != null);

        for (CaParameterImpl<?> param : stringParameters.values()) {
            param.unbind();
        }
        for (CaParameterImpl<?> param : doubleParameters.values()) {
            param.unbind();
        }
        for (CaParameterImpl<?> param : floatParameters.values()) {
            param.unbind();
        }
        for (CaParameterImpl<?> param : integerParameters.values()) {
            param.unbind();
        }
        
        stringParameters.clear();
        doubleParameters.clear();
        floatParameters.clear();
        integerParameters.clear();

        try {
            if (dirChannel != null) {
                epicsWriter.destroyChannel(dirChannel);
                dirChannel = null;
            }
        } catch (CAException e) {
            LOG.warning(e.getMessage());
        }

        epicsWriter = null;
    }

    @Override
    public CaCommandMonitor post() {
        return apply.post();
    }

    @Override
    public CaCommandMonitor postWait() throws InterruptedException {
        return apply.postWait();
    }

    @Override
    public CaCommandMonitor postCallback(CaCommandListener callback) {
        return apply.postCallback(callback);
    }

    @Override
    public CaParameter<Integer> addInteger(String name, String channel)
            throws CaException {
        return addInteger(name, channel, null);
    }

    @Override
    public CaParameter<Integer> addInteger(String name, String channel,
            String description) throws CaException {
        CaParameterImpl<Integer> param = integerParameters.get(name);
        if (param == null) {
            if (alreadyExist(name)) {
                throw new CaException(
                        "Parameter already exists with a different type.");
            } else {
                param = CaParameterImpl.createIntegerParameter(name, channel,
                        description, epicsWriter);
                integerParameters.put(name, param);
            }
        } else {
            if (!channel.equals(param.channel())) {
                throw new CaException(
                        "Parameter already exists for a different channel.");
            }
        }
        return param;
    }

    @Override
    public CaParameter<Double> addDouble(String name, String channel)
            throws CaException {
        return addDouble(name, channel, null);
    }

    @Override
    public CaParameter<Double> addDouble(String name, String channel,
            String description) throws CaException {
        CaParameterImpl<Double> param = doubleParameters.get(name);
        if (param == null) {
            if (alreadyExist(name)) {
                throw new CaException(
                        "Parameter already exists with a different type.");
            } else {
                param = CaParameterImpl.createDoubleParameter(name, channel,
                        description, epicsWriter);
                doubleParameters.put(name, param);
            }
        } else {
            if (!channel.equals(param.channel())) {
                throw new CaException(
                        "Parameter already exists for a different channel.");
            }
        }
        return param;
    }

    @Override
    public CaParameter<Float> addFloat(String name, String channel)
            throws CaException {
        return addFloat(name, channel, null);
    }

    @Override
    public CaParameter<Float> addFloat(String name, String channel,
            String description) throws CaException {
        CaParameterImpl<Float> param = floatParameters.get(name);
        if (param == null) {
            if (alreadyExist(name)) {
                throw new CaException(
                        "Parameter already exists with a different type.");
            } else {
                param = CaParameterImpl.createFloatParameter(name, channel,
                        description, epicsWriter);
                floatParameters.put(name, param);
            }
        } else {
            if (!channel.equals(param.channel())) {
                throw new CaException(
                        "Parameter already exists for a different channel.");
            }
        }
        return param;
    }

    @Override
    public CaParameter<String> addString(String name, String channel)
            throws CaException {
        return addString(name, channel, null);
    }

    @Override
    public CaParameter<String> addString(String name, String channel,
            String description) throws CaException {
        CaParameterImpl<String> param = stringParameters.get(name);
        if (param == null) {
            if (alreadyExist(name)) {
                throw new CaException(
                        "Parameter already exists with a different type.");
            } else {
                param = CaParameterImpl.createStringParameter(name, channel,
                        description, epicsWriter);
                stringParameters.put(name, param);
            }
        } else {
            if (!channel.equals(param.channel())) {
                throw new CaException(
                        "Parameter already exists for a different channel.");
            }
        }
        return param;
    }

    @Override
    public CaParameter<Integer> getInteger(String name) {
        return integerParameters.get(name);
    }

    @Override
    public CaParameter<Double> getDouble(String name) {
        return doubleParameters.get(name);
    }

    @Override
    public CaParameter<Float> getFloat(String name) {
        return floatParameters.get(name);
    }

    @Override
    public CaParameter<String> getString(String name) {
        return stringParameters.get(name);
    }

    @Override
    public void remove(String name) {
        doubleParameters.remove(name);
        floatParameters.remove(name);
        integerParameters.remove(name);
        stringParameters.remove(name);
    }

    private boolean alreadyExist(String name) {
        return doubleParameters.containsKey(name)
                || floatParameters.containsKey(name)
                || integerParameters.containsKey(name)
                || stringParameters.containsKey(name);
    }

    @Override
    public void mark() throws TimeoutException {
        if (dirChannel != null) {
            try {
                dirChannel.setValue(CadDirective.MARK);
            } catch (CAException e) {
                LOG.warning(e.getMessage());
            }
        }
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

    @Override
    public String getDescription() {
        return description;
    }


}
