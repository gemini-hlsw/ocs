//
// $Id: OdbMailEvent.java 6986 2006-05-01 17:05:49Z shane $
//
package edu.gemini.dbTools.mail;

import edu.gemini.spModel.obs.ObservationStatus;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/** An enumerated type that describes observation state transitions that should trigger an email. */
public abstract class OdbMailEvent implements Serializable, Comparable<OdbMailEvent> {
    static final long serialVersionUID = 1;

    private static final Map<String, OdbMailEvent> TYPE_MAP = new HashMap<String, OdbMailEvent>();

    /**
     * Describes the "direction" from which we arrived at a particular
     * observation status.  For example, when moving from "ForReview"
     * to "Phase2", we are going DOWN.
     */
    public static final class Direction implements Comparable<Direction>, Serializable {
        static final long serialVersionUID = 1;

        private static int _nextOrdinal = 0;
        private final int _ordinal = _nextOrdinal++;
        private final String _name;

        public static final Direction UP = new Direction("up");
        public static final Direction ANY = new Direction("any");
        public static final Direction DOWN = new Direction("down");

        private static final Direction[] TYPES = new Direction[]{
            UP, ANY, DOWN,
        };

        private Direction(final String name) {
            _name = name;
        }

        public int compareTo(final Direction that) {
            return _ordinal - that._ordinal;
        }

        // Guarantee that no duplicate copies are created via serialization.
        Object readResolve() throws ObjectStreamException {
            return TYPES[_ordinal];
        }

        public String toString() {
            return _name;
        }
    }

    /**
     * An interface for performing actions based upon an event value.
     * Use of this interface and the {@link OdbMailEvent#doAction} method
     * allows switch statements to be avoided.
     */
    public interface Action {
        void down_ForReview();

        void down_Phase2();

        void onHold();

        void observed();

        void up_ForActivation();

        void up_ForReview();

        void up_Ready();
    }

    private static final OdbMailEvent DOWN_FOR_REVIEW =
            new OdbMailEvent(Direction.DOWN, ObservationStatus.FOR_REVIEW) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.down_ForReview();
                }
            };

    private static final OdbMailEvent DOWN_PHASE2 =
            new OdbMailEvent(Direction.DOWN, ObservationStatus.PHASE2) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.down_Phase2();
                }
            };

    private static final OdbMailEvent ON_HOLD =
            new OdbMailEvent(Direction.ANY, ObservationStatus.ON_HOLD) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.onHold();
                }
            };

    private static final OdbMailEvent OBSERVED =
            new OdbMailEvent(Direction.ANY, ObservationStatus.OBSERVED) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.observed();
                }
            };

    private static final OdbMailEvent UP_FOR_ACTIVATION =
            new OdbMailEvent(Direction.UP, ObservationStatus.FOR_ACTIVATION) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.up_ForActivation();
                }
            };

    private static final OdbMailEvent UP_FOR_REVIEW =
            new OdbMailEvent(Direction.UP, ObservationStatus.FOR_REVIEW) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.up_ForReview();
                }
            };

    private static final OdbMailEvent UP_READY =
            new OdbMailEvent(Direction.UP, ObservationStatus.READY) {
                public void doAction(final OdbMailEvent.Action action) {
                    action.up_Ready();
                }
            };

    private static final OdbMailEvent[] TYPES = new OdbMailEvent[]{
        DOWN_FOR_REVIEW,
        DOWN_PHASE2,
        OBSERVED,
        ON_HOLD,
        UP_FOR_ACTIVATION,
        UP_FOR_REVIEW,
        UP_READY,
    };

    private static String getKey(final Direction direction,
                                 final ObservationStatus status) {

        final StringBuilder buf = new StringBuilder(direction.toString());
        buf.append(" ");
        buf.append(status.toString());
        return buf.toString();
    }

    /**
     * Lookup the OdbMailEvent corresponding with the given observation status
     * transition, if any.  If the transition shouldn't trigger an event, then
     * <code>null</code> is returned.
     */
    public static OdbMailEvent lookup(final ObservationStatus fromStatus,
                                      final ObservationStatus toStatus) {

        // If no status change, then there is no event.
        if (fromStatus.equals(toStatus)) return null;

        // See whether we're moving up to the "toStatus" or down to
        // the "toStatus".
        Direction direction = Direction.UP;
        if (fromStatus.isGreaterThan(toStatus)) {
            direction = Direction.DOWN;
        }
        String key = getKey(direction, toStatus);
        OdbMailEvent evt = TYPE_MAP.get(key);

        if (evt == null) {
            // try "any"
            key = getKey(Direction.ANY, toStatus);
            evt = TYPE_MAP.get(key);
        }

        return evt;
    }

    private static int _nextOrdinal = 0;

    private final int _ordinal = _nextOrdinal++;
    private final Direction _direction;
    private final ObservationStatus _status;


    private OdbMailEvent(final Direction direction,
                         final ObservationStatus status) {
        _direction = direction;
        _status = status;
        TYPE_MAP.put(getKey(direction, status), this);
    }

    public String toString() {
        return getKey(_direction, _status);
    }

    public abstract void doAction(OdbMailEvent.Action action);

    // Guarantee that no duplicate copies are created via serialization.
    Object readResolve() throws ObjectStreamException {
        return TYPES[_ordinal];
    }

    public int compareTo(final OdbMailEvent that) {
        final int res = _status.ordinal() - that._status.ordinal();
        if (res != 0) return res;
        return _direction.compareTo(that._direction);
    }
}

