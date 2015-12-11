package edu.gemini.epics.acm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.api.ChannelListener;
import gov.aps.jca.CAException;

final class CaAttributeImpl<T> implements CaAttribute<T> {
    private static final Logger LOG = Logger.getLogger(CaParameterImpl.class.getName());

    private EpicsReader epicsReader;
    private ChannelListener<T> channelListener;
    private ReadOnlyClientEpicsChannel<T> epicsChannel;
    private List<T> values;
    private boolean isValid;
    private final String name;
    private final String channel;
    private final Class<T> type;
    private final String description;

    private CaAttributeImpl(String name, String channel, Class<T> type,
            String description, EpicsReader epicsReader) {

        super();
        this.name = name;
        this.channel = channel;
        this.type = type;
        this.description = description;
        this.epicsReader = epicsReader;
    }

    void bind(ReadOnlyClientEpicsChannel<T> epicsChannel)
            throws CAException {
        if(epicsChannel!=null) {
            this.epicsChannel = epicsChannel;
            channelListener = new ChannelListener<T>() {

                @Override
                public void valueChanged(String arg0, List<T> arg1) {
                    if (arg1 != null) {
                        if (arg1.isEmpty()) {
                            setValues(new ArrayList<T>());
                            setValidity(true);
                        } else {
                            if (CaAttributeImpl.this.type.isInstance(arg1.get(0))) {
                                List<T> vals = new ArrayList<>();
                                for (T v : arg1) {
                                    vals.add(v);
                                }

                                setValues(vals);
                                setValidity(true);
                            } else {
                                setValidity(false);
                            }
                        }
                    } else {
                        setValidity(false);
                    }

                }
            };
            this.epicsChannel.registerListener(channelListener);
        } else {
            LOG.warning("Unable to bind to channel " + channel);
        }
    }

    synchronized private void setValues(List<T> newVals) {
        values = newVals;
        notifier.notifyValueChange(values);
    }

    synchronized private void setValidity(boolean newValidity) {
        isValid = newValidity;
        notifier.notifyValidityChange(isValid);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    synchronized public T value() {
        if (isValid && values != null && values.size() > 0) {
            return values.get(0);
        } else {
            return null;
        }
    }

    @Override
    public boolean valid() {
        return isValid;
    }

    @Override
    synchronized public List<T> values() {
        if (isValid) {
            return ImmutableList.copyOf(values);
        } else {
            return null;
        }
    }

    private final class Notifier {
        private final List<CaAttributeListener<T>> listeners = new LinkedList<>();

        synchronized public void addListener(CaAttributeListener<T> listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        synchronized public void removeListener(CaAttributeListener<T> listener) {
            listeners.remove(listener);
        }

        synchronized public void notifyValueChange(List<T> newVals) {
            for (CaAttributeListener<T> listener : listeners) {
                listener.onValueChange(newVals);
            }
        }

        synchronized public void notifyValidityChange(boolean newValidity) {
            for (CaAttributeListener<T> listener : listeners) {
                listener.onValidityChange(newValidity);
            }
        }
    }

    private final Notifier notifier = new Notifier();

    @Override
    public void addListener(CaAttributeListener<T> listener) {
        notifier.addListener(listener);
    }

    @Override
    public void removeListener(CaAttributeListener<T> listener) {
        notifier.removeListener(listener);
    }

    public void unbind() throws CAException {
        assert (epicsReader != null);
        assert (channelListener != null);

        if(epicsChannel!=null) {
            epicsChannel.unRegisterListener(channelListener);
            epicsChannel = null;
        }
        channelListener = null;
        epicsReader = null;
    }

    static CaAttributeImpl<Double> createDoubleAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<Double> attr = new CaAttributeImpl<>(name,
                channel, Double.class, description, epicsReader);
        try {
            attr.bind(epicsReader.getDoubleChannel(channel));
        } catch(Throwable e) {
            LOG.warning(e.getMessage());
        }

        return attr;
    }

    static public CaAttributeImpl<Float> createFloatAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<Float> attr = new CaAttributeImpl<>(name, channel,
                Float.class, description, epicsReader);
        try {
            attr.bind(epicsReader.getFloatChannel(channel));
        } catch(Throwable e) {
            LOG.warning(e.getMessage());
        }

        return attr;
    }

    static public CaAttributeImpl<Integer> createIntegerAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<Integer> attr = new CaAttributeImpl<>(name,
                channel, Integer.class, description, epicsReader);
        try {
            attr.bind(epicsReader.getIntegerChannel(channel));
        } catch(Throwable e) {
            LOG.warning(e.getMessage());
        }

        return attr;
    }

    static public CaAttributeImpl<String> createStringAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<String> attr = new CaAttributeImpl<>(name,
                channel, String.class, description, epicsReader);
        try {
            attr.bind(epicsReader.getStringChannel(channel));
        } catch(Throwable e) {
            LOG.warning(e.getMessage());
        }

        return attr;
    }

    static public <T extends Enum<T>> CaAttributeImpl<T> createEnumAttribute(
            String name, String channel, String description, Class<T> enumType,
            EpicsReader epicsReader) throws CAException {
        CaAttributeImpl<T> attr = new CaAttributeImpl<>(name, channel,
                enumType, description, epicsReader);
        try {
            attr.bind(epicsReader.getEnumChannel(channel, enumType));
        } catch(Throwable e) {
            LOG.warning(e.getMessage());
        }

        return attr;
    }

    @Override
    public String channel() {
        return channel;
    }

    @Override
    public String description() {
        return description;
    }
}
