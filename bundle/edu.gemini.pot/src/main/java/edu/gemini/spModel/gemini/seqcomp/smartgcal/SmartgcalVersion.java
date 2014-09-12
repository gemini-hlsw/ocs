package edu.gemini.spModel.gemini.seqcomp.smartgcal;

import java.io.Serializable;

/**
 * A type for the smart gcal version number.
 */
public final class SmartgcalVersion implements Serializable, Comparable<SmartgcalVersion> {
    private int number;

    public SmartgcalVersion(int number) { this.number = number; }

    public int getNumber() { return number; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SmartgcalVersion that = (SmartgcalVersion) o;
        return (number == that.number);
    }

    @Override public int hashCode() { return number; }

    @Override public String toString() { return String.valueOf(number); }

    @Override public int compareTo(SmartgcalVersion that) {
        return number == that.number ? 0 : (number < that.number ? -1 : 1);
    }

    /**
     * Parses a string created with {@link #toString} into a verison number.
     */
    public static SmartgcalVersion toVersion(String val) {
        return new SmartgcalVersion(Integer.parseInt(val));
    }

}
