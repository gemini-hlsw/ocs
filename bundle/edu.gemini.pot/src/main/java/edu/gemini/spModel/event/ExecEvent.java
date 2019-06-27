package edu.gemini.spModel.event;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioParseException;
import edu.gemini.shared.util.GeminiRuntimeException;

import java.io.Serializable;
import java.time.Instant;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import java.util.Comparator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The base class for execution events.  All execution events are immutable.
 * Specific event types will differ in the additional data that they carry and
 * in their implementation of {@link #doAction(ExecAction)}.
 */
public abstract class ExecEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String PARAM_SET = "event";
    public static final String TIMESTAMP_PARAM = "timestamp";

    /**
     * A comparator that can be used to sort ObsExecutionEvents by timestamp.
     */
    public static final class TimeComparator implements Comparator<ExecEvent>, Serializable {
        public int compare(ExecEvent e1, ExecEvent e2) {
            if (e1._timestamp == e2._timestamp) return 0;
            return (e1._timestamp < e2._timestamp) ? -1 : 1;
        }
    }
    public static final Comparator<ExecEvent> TIME_COMPARATOR = new TimeComparator();


    /**
     * Creates an ExecEvent from the given ParamSet.
     *
     * @param paramSet param set containing the externalized format of the
     *                 event
     * @return ExecEvent instance corresponding to the ParamSet.
     * @throws PioParseException if there is a problem parsing the paramSet
     *                           into an ExecEvent
     */
    public static ExecEvent create(ParamSet paramSet) throws PioParseException {
        // Figures out which event to create based upon kind.
        final String kind = paramSet.getKind();
        if (kind == null) throw new PioParseException("Event missing kind");

        String className = ExecEvent.class.getName();

        // Strip off the trailing "ExecEvent".
        int index = className.lastIndexOf('.');
        if (index == -1) throw new GeminiRuntimeException("ExecEvent not in a package ...");

        String packageName = className.substring(0, index);

        className = packageName + "." + kind + "Event";
        try {
            final Class<?> c = Class.forName(className);
            final Constructor<?> cons = c.getConstructor(ParamSet.class);
            return (ExecEvent) cons.newInstance(paramSet);
        } catch (InvocationTargetException ex) {
            Throwable t = ex.getTargetException();
            if (t instanceof PioParseException) {
                throw (PioParseException) t;
            }
            throw new PioParseException("Problem instantiating event", ex);
        } catch (Exception ex) {
            throw new PioParseException("Problem instantiating event", ex);
        }
    }

    private final long _timestamp;

    /**
     * Creates the execution event with the time at which it occured
     *
     * @param time absolute time at which the event occurred
     */
    ExecEvent(long time) {
        if (time < 0) throw new IllegalArgumentException("" + time);
        _timestamp = time;
    }

    /**
     * Creates the execution event from the given ParamSet.
     */
    ExecEvent(ParamSet paramSet) throws PioParseException {
        _timestamp = Pio.getLongValue(paramSet, TIMESTAMP_PARAM, -1);
        if (_timestamp == -1) {
            throw new PioParseException("missing or illegal '" + TIMESTAMP_PARAM + "'");
        }
    }

    /**
     * Gets the time at which the execution event happened.
     */
    public final long getTimestamp() {
        return _timestamp;
    }

    /**
     * Gets an name for the event, which is used when converting to/from
     * ParamSets.
     */
    public abstract String getKind();

    /**
     * Provides visitor pattern like support to enable actions to be executed
     * without explicit switch statements based upon event type.
     *
     * @param action action to perform
     */
    public abstract void doAction(ExecAction action);

    /**
     * Creates a ParamSet that will contain the params that define the event.
     *
     * @param factory factory to use to build the ParamSet
     * @return ParamSet describing the event, with a parameter for the time
     *         value
     */
    protected ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET);

        paramSet.setKind(getKind());

        Pio.addParam(factory, paramSet, TIMESTAMP_PARAM, String.valueOf(getTimestamp()));

        return paramSet;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;

        ExecEvent that = (ExecEvent) other;
        return _timestamp == that._timestamp;
    }

    public int hashCode() {
        return (int) (_timestamp ^ (_timestamp >>> 32));
    }

    /**
     * Returns a human-readable name for the event.
     */
    public abstract String getName();

    @Override
    public String toString() {
        final String subProps          = toStringProperties();
        final String formattedSubProps = "".equals(subProps) ? "" : ", " + subProps;

        return String.format(
            "%s {timestamp=%d (%s)%s}",
            getName(),
            _timestamp,
            ISO_INSTANT.format(Instant.ofEpochMilli(_timestamp)),
            formattedSubProps
        );
    }

    protected abstract String toStringProperties();
}
