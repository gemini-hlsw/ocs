package edu.gemini.spModel.target.system;

/**
 * This class represents a target that is defined by its name only
 *
 */
public final class NamedTarget extends NonSiderealTarget {

    public static final Tag TAG = Tag.NAMED;

    public Tag getTag() {
        return TAG;
    }


    /**
     * Solar System Objects
     */
    public static enum SolarObject {
        MOON("Moon", "301"),
        MERCURY("Mercury", "199"),
        VENUS("Venus", "299"),
        MARS("Mars", "499"),
        JUPITER("Jupiter", "599"),
        SATURN("Saturn", "699"),
        URANUS("Uranus", "799"),
        NEPTUNE("Neptune", "899"),
        PLUTO("Pluto", "999");

        private final String _displayValue;
        private final String _horizonsId;

        public static final NamedTarget.SolarObject DEFAULT_SOLAR_OBJECT = MOON;

        SolarObject(String displayValue, String horizonsId) {
            _displayValue = displayValue;
            _horizonsId = horizonsId;
        }

        public String getDisplayValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

        public String getHorizonsId() {
            return _horizonsId;
        }
    }


    private NamedTarget.SolarObject _solarObject = NamedTarget.SolarObject.DEFAULT_SOLAR_OBJECT;

    /**
     * Override equals to return true if both instances are the same.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!super.equals(obj)) return false;

        if (!(obj instanceof NamedTarget)) return false;

        NamedTarget sys = (NamedTarget) obj;

        if (!(_solarObject.equals(sys._solarObject))) return false;
        //Decided that ra and dec won't be considered to decide
        //if two ConicTarget are the same. Therefore, they aren't in
        //the hashCode either.
        return true;
    }

    /**
     * Provide a hashcode for this object.  The class <code>{@link
     * edu.gemini.spModel.target.system.CoordinateParam}</code> implements hashCode.
     */
    public int hashCode() {
        long hc =  _solarObject.hashCode();
        return (int) hc ^ (int) (hc >> 32);
    }

    /**
     * Get the object associated to this Named Target.
     */
    public NamedTarget.SolarObject getSolarObject() {
        return _solarObject;
    }

    /**
     * Set the object associated to this Named target
     */
    public void setSolarObject(NamedTarget.SolarObject solarObject) {
        _solarObject = solarObject;
    }

    /**
     * Gets a short description of the position.
     */
    public String getPosition() {
        return (_solarObject != null) ?  _solarObject.getDisplayValue() : "Named Target";
    }

}
