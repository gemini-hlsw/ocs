package edu.gemini.spModel.target.system;

/**
 * This class represents a target that is defined by its name only
 *
 */
public final class NamedTarget extends NonSiderealTarget {

    /**
     * Options for the system type.
     */
    public static final class SystemType extends TypeBase {
        public static int _count = 0;

        public static final SystemType SOLAR_OBJECT =
                new SystemType("Solar system object");

        public static final SystemType[] TYPES = new SystemType[]{
                SOLAR_OBJECT
        };

        private SystemType(String name) {
            super(_count++, name);
        }
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


    /**
     * Various default values.
     */
    private static final SystemType DEFAULT_SYSTEM_TYPE = SystemType.SOLAR_OBJECT;

    /**
     * The base name of this coordinate system.
     */
    private static final String SHORT_SYSTEM_NAME = "namedTarget";

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
     * Gets the system's name including the selected (sub)option.
     */
    public String getSystemName() {
        return "(" + getSystemOption().getName() + ")";
    }

    /**
     * Return the short system name.
     */
    public String getShortSystemName() {
        return SHORT_SYSTEM_NAME;
    }


    /**
     * Get the objecct associated to this Named Target.
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


    /**
     * Gets the available options for this coordinate system.
     */
    public TypeBase[] getSystemOptions() {
        return NamedTarget.SystemType.TYPES;
    }


    public TypeBase getSystemOption() {
        return SystemType.SOLAR_OBJECT;
    }

    public void setSystemOption(TypeBase newValue) {
        if (!SystemType.SOLAR_OBJECT.equals(newValue))
            throw new IllegalArgumentException("Nope. " + newValue);
    }

}
