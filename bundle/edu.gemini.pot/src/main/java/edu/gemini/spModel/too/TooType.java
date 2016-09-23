package edu.gemini.spModel.too;

import edu.gemini.spModel.type.SpTypeUtil;

/**
 * Definition of the various TOO program types.
 */
public enum TooType {
    rapid("Rapid", 24 * 60 * 60 * 1000, "one day"),
    standard("Standard", 0, "n/a"),
    none("None", 0, "n/a"),;

    private String _displayValue;
    private long _windowDuration;
    private String _durationDisplay;

    TooType(String displayValue, long defaultWindowDuration, String dur) {
        _displayValue = displayValue;
        _windowDuration = defaultWindowDuration;
        _durationDisplay = dur;
    }

    public String getDisplayValue() {
        return _displayValue;
    }

    public String getDurationDisplayString() {
        return _durationDisplay;
    }

    public long getDefaultWindowDuration() {
        return _windowDuration;
    }

    public static TooType getTooType(String name) {
        return getTooType(name, TooType.none);
    }

    public static TooType getTooType(String name, TooType defaultValue) {
        TooType res = SpTypeUtil.noExceptionValueOf(TooType.class, name);
        if (res == null) res = defaultValue;
        return res;
    }
}
