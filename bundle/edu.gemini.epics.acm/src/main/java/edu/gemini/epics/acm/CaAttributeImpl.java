package edu.gemini.epics.acm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.api.ChannelListener;
import gov.aps.jca.CAException;

class CaAttributeImpl<T> implements CaAttribute<T> {

    private EpicsReader epicsReader;
    private ChannelListener<T> channelListener;
    private ReadOnlyClientEpicsChannel<T> epicsChannel;
    private List<T> values;
    private boolean isValid;
    private final String name;
    private final String channel;
    private final Class<T> type;
    private final String description;

    protected CaAttributeImpl(String name, String channel, Class<T> type,
            String description, EpicsReader epicsReader) {

        super();
        this.name = name;
        this.channel = channel;
        this.type = type;
        this.description = description;
        this.epicsReader = epicsReader;
    }

    protected void bind(ReadOnlyClientEpicsChannel<T> epicsChannel)
            throws CAException {
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
                            List<T> vals = new ArrayList<T>();
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
            return values;
        } else {
            return null;
        }
    }

    private class Notifier {
        private List<CaAttributeListener<T>> listeners = new LinkedList<CaAttributeListener<T>>();

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

    private Notifier notifier = new Notifier();

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
        assert (epicsChannel != null);
        assert (channelListener != null);

        epicsChannel.unRegisterListener(channelListener);
        channelListener = null;
        epicsChannel = null;
        epicsReader = null;
    }

    static final CaAttributeImpl<Double> createDoubleAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<Double> attr = new CaAttributeImpl<Double>(name,
                channel, Double.class, description, epicsReader);
        attr.bind(epicsReader.getDoubleChannel(channel));
        return attr;
    }

    static public final CaAttributeImpl<Float> createFloatAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<Float> attr = new CaAttributeImpl<Float>(name, channel,
                Float.class, description, epicsReader);
        attr.bind(epicsReader.getFloatChannel(channel));
        return attr;
    }

    static public final CaAttributeImpl<Integer> createIntegerAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<Integer> attr = new CaAttributeImpl<Integer>(name,
                channel, Integer.class, description, epicsReader);
        attr.bind(epicsReader.getIntegerChannel(channel));
        return attr;
    }

    static public final CaAttributeImpl<String> createStringAttribute(
            String name, String channel, String description, EpicsReader epicsReader)
            throws CAException {
        CaAttributeImpl<String> attr = new CaAttributeImpl<String>(name,
                channel, String.class, description, epicsReader);
        attr.bind(epicsReader.getStringChannel(channel));
        return attr;
    }

    static public final <T extends Enum<T>> CaAttributeImpl<T> createEnumAttribute(
            String name, String channel, String description, Class<T> enumType,
            EpicsReader epicsReader) throws CAException {
        CaAttributeImpl<T> attr = new CaAttributeImpl<T>(name, channel,
                enumType, description, epicsReader);
        attr.bind(epicsReader.getEnumChannel(channel, enumType));
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
