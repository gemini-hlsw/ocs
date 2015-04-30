package jsky.app.ot.gemini.editor.targetComponent;

import java.util.Date;

/**
 * The time configurations are pre-sets of dates the Horizons query should use to gets its
 * information.
 */
public enum TimeConfig {

    NOW("Now") {
        public Date getDate() {
            return new Date();
        }
    },
    ONE_HOUR("1 Hour") {
        public Date getDate() {
            final Date d = new Date();
            return new Date(d.getTime() + HOUR);
        }
    },
    TWO_HOUR("2 Hours") {
        public Date getDate() {
            final Date d = new Date();
            return new Date(d.getTime() + HOUR * 2);
        }
    },

    THREE_HOUR("3 Hours") {
        public Date getDate() {
            final Date d = new Date();
            return new Date(d.getTime() + HOUR * 3);
        }
    },

    FIVE_HOUR("5 Hours") {
        public Date getDate() {
            final Date d = new Date();
            return new Date(d.getTime() + HOUR * 5);
        }
    },;

    private static final int HOUR = 1000 * 60 * 60;
    private final String _displayValue;

    private TimeConfig(String displayValue) {
        _displayValue = displayValue;
    }

    public String displayValue() {
        return _displayValue;
    }

    /**
     * Return the <code>Date</code>  for the given configuration
     * @return the <code>Date</code> associated to this configuration
     */
    public abstract Date getDate();

}
