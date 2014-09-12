package edu.gemini.pot.sp;

import java.util.NoSuchElementException;

public enum SPComponentBroadType {

    // N.B. these string values are written/read by PIO and exist in old program XML, so
    // these values should not be changed. Sorry.
    AO("AO"),
    CONFLICT("Conflict"),
    DATA("Data"),
    ENGINEERING("Engineering"),
    GROUP("Group"),
    INFO("Info"),
    INSTRUMENT("Instrument"),
    ITERATOR("Iterator"),
    OBSERVATION("Observation"),
    OBSERVER("Observer"),
    OBSLOG("ObsLog"),
    PLAN("Plan"),
    PROGRAM("Program"),
    SCHEDULING("Scheduling"),
    TELESCOPE("Telescope"),
    TEMPLATE("Template"),
    UNKNOWN("unknown")

    ;

    public final String value;

    private SPComponentBroadType(String value) {
        this.value  = value;
    }

    // These tokens used to be strings, so it's possible that they're appended and/or printed as-is
    // variously in the code. For this reason I'm overriding toString, although it's awful.
    @Override public String toString() {
        return value;
    }

    public static SPComponentBroadType getInstance(String value) {
        for (SPComponentBroadType t: values())
            if (t.value.equals(value))
                return t;
        throw new NoSuchElementException(value);
    }

}
