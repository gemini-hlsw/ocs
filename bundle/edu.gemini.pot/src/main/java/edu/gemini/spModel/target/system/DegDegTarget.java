// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: DegDegTarget.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.CoordinateTypes.Epoch;
import jsky.coords.wcscon;

import java.awt.geom.Point2D;

/**
 * This class represents a coordinate position in a system that
 * uses two coordinate positions, c1 and c2.  In this case, c1 and c2
 * are represented by coordinates in units of degrees, minutes, seconds.
 * Az/Alt systems of various types are represented with this class.
 * <p>
 * Unlike HmsDegTarget, there is not two typical coordinate names (ra,dec)
 * to give coordinates using DegDegTarget.  Therefore, only the c1, c2
 * methods are used and provided.
 * <p>
 * This class currently only supports Galactic coordinates (Type II,
 * IAU 1958).  Stubs are in place to support Az/Alt, but the conversion
 * routines for <code>ITarget</code> are not yet in place.
 *
 * @author      Kim Gillies (Gemini Observatory)
 */
public final class DegDegTarget extends CoordinateSystem
        implements ITarget {
    /**
     * Options for the system type.
     */
    public static final class SystemType extends TypeBase {
        public static final int _GALACTIC = 0;
        public static final int _AZ_ALT = 1;

        public static final SystemType AZ_ALT =
                new SystemType(_AZ_ALT, "Az/Alt");

        public static final SystemType GALACTIC =
                new SystemType(_GALACTIC, "Galactic");

        public static final SystemType[] TYPES = new SystemType[]{
            GALACTIC,
            AZ_ALT,
        };

        private SystemType(int type, String name) {
            super(type, name);
        }
    }

    /**
     * Various default values.
     */
    public static final SystemType DEFAULT_SYSTEM_TYPE = SystemType.GALACTIC;
    public static final Epoch DEFAULT_EPOCH_IAU1958 =
            new Epoch(1958, Units.YEARS);
    public static final Epoch DEFAULT_EPOCH_2000 =
            new Epoch(2000, Units.YEARS);

    /**
     * The base name of this coordinate system.
     */
    public static final String SYSTEM_NAME = "Deg Deg";
    public static final String SHORT_SYSTEM_NAME = "degdegTarget";
    public static final String DEFAULT_NAME = "";


    private String _brightness = DEFAULT_NAME;
    private String _name = DEFAULT_NAME;
    // Az/Alt or longitude/latitude
    private ICoordinate _c1 = new DMSLong();      // Az or longitude
    private ICoordinate _c2 = new DMS();          // Alt or latitude
    // Epoch may be needed for Az/Alt
    private Epoch _epoch = _createDefaultEpoch();

    /**
     * Provides clone support.
     */
    public Object clone() {
        DegDegTarget result = (DegDegTarget) super.clone();
        // This is almost C++ quality!
        result._c1 = (DMSLong) ((DMSLong) _c1).clone();
        result._c2 = (DMS) ((DMS) _c2).clone();
        if (_epoch != null) result._epoch = (Epoch) _epoch.clone();
        // _name is a String (immutable)

        return result;
    }

    /**
     * Override equals to return true if both instances are the same.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (!(obj instanceof DegDegTarget)) return false;

        DegDegTarget sys = (DegDegTarget) obj;
        if (!(getSystemOption().equals(sys.getSystemOption()))) return false;
        if (!(_c1.equals(sys._c1)) || !(_c2.equals(sys._c2))) return false;
        // Should we care about these for equals?
        if (!(_name.equals(sys._name))) return false;
        if (!(_epoch.equals(sys._epoch))) return false;

        return true;
    }


    /**
     * Provide a hashcode for this object.  The class <code>{@link
     * CoordinateParam}</code> implements hashCode.
     */
    public int hashCode() {
        long hc = _name.hashCode() ^ _epoch.hashCode() ^
                _c1.hashCode() ^ _c2.hashCode();
        return (int) hc ^ (int) (hc >> 32);
    }

    /**
     * Constructs with the default property values.
     */
    public DegDegTarget() {
        super(DEFAULT_SYSTEM_TYPE);
    }

    /**
     * Constructs with the specific DegDeg system type and other default
     * values.
     */
    public DegDegTarget(SystemType systemOption)
            throws IllegalArgumentException {
        super(systemOption);
    }

    /**
     * Gets the first coordinate (longitude) as a String.
     */
    public String c1ToString() {
        return _c1.toString();
    }

    /**
     * Gets the second coordinate (latitude) as a String.
     */
    public String c2ToString() {
        return _c2.toString();
    }

    /**
     * Gets the first coordinate as an {@link DMS DMS} object.
     * This returns a reference to the actual object so beware!
     * Note that the <code>DMS</code> object returned here is really
     * a {@link DMSLong DMSLong} object, which knows how to format
     * longitudes properly.
     */
    public ICoordinate getC1() {
        // C1 exists at instance creation.
        return _c1;
    }

    /**
     * Gets the second coordinate as an {@link DMS DMS} object.
     * This returns a reference to the actual object.
     */
    public ICoordinate getC2() {
        // C2 exists at instance creation.
        return _c2;
    }


    /**
     * Sets the c1 coordinate using an object implementing
     * the {@link ICoordinate ICoordinate} interface (a DMSLong object).
     * The input object is not cloned.  Therefore, the caller can
     * alter the contents if he is not careful.
     * <p>
     * If newValue is null, the method returns without changing the
     * internal value to null.  This ensures that the object always has a
     * valid DMSLong object.
     * <p>
     * This method throws IllegalArgumentException if the ICoordinate is
     * not an instance of {@link DMSLong DMSLong}.
     */
    public void setC1(ICoordinate newValue) {
        if (newValue == null) {
            newValue = new DMSLong();
        }
        if (!(newValue instanceof DMSLong)) {
            throw new IllegalArgumentException();
        }
        _c1 = newValue;
    }

    /**
     * Sets the c2 coordinate using an object implementing
     * the {@link ICoordinate ICoordinate} interface (a DMS object).
     * The input object is not cloned.  Therefore, the caller can
     * alter the contents if not careful.
     * <p>
     * If newValue is null, the method returns without changing the
     * internal value to null.  This ensures the object always has a valid
     * DMS object.
     * <p>
     * This method throws IllegalArgumentException if the ICoordinate is
     * not an instance of {@link DMS DMS}.
     */
    public void setC2(ICoordinate newValue) {
        if (newValue == null) {
            newValue = new DMS();
        }
        if (!(newValue instanceof DMS)) {
            throw new IllegalArgumentException();
        }
        _c2 = newValue;
    }

    /**
     * Set both the first and second coordinates using DMS objects.
     * @see #setC1
     */
    public void setC1C2(ICoordinate c1NewValue, ICoordinate c2NewValue) {
        setC1(c1NewValue);
        setC2(c2NewValue);
    }

    /**
     * Sets the first coordinate (longitude) using a String.
     */
    public void setC1(String newStringValue) {
        _c1.setValue(newStringValue);
    }

    /**
     * Sets the second coordinate (latitude) using a String.
     */
    public void setC2(String newStringValue) {
        _c2.setValue(newStringValue);
    }

    /**
     * Sets the c1 and c2 positions using String objects.
     */
    public void setC1C2(String newC1, String newC2) {
        _c1.setValue(newC1);
        _c2.setValue(newC2);
    }

    /**
     * Returns the Epoch object for this coordinate system.
     * This returns the actual object (not a copy).
     */
    public Epoch getEpoch() {
        if (_epoch == null) {
            _epoch = _createDefaultEpoch();
        }
        return _epoch;
    }

    /**
     * Override _createDefaultEpoch() to have it depend upon the current
     * system option.
     * We must assume that if not galactic, the user will
     * set the epoch of the coordinate
     */
    protected Epoch _createDefaultEpoch() {
        Epoch ep;
        if ((getSystemOption() == SystemType.GALACTIC)) {
            ep = DEFAULT_EPOCH_IAU1958;
        } else {
            ep = new Epoch(DEFAULT_EPOCH_2000.getValue(),
                           DEFAULT_EPOCH_2000.getUnits());
        }
        return ep;
    }

    /**
     * Sets the epoch.  The value of the parameter is not
     * copied so future modification will an effect upon the value
     * stored in this class.
     */
    public void setEpoch(Epoch newValue) {
        // Set _epoch to use the new value.  This might be null.
        // This probably needs to be more clever since it can change
        // with the system type.
        _epoch = newValue;
    }

    /**
     * Gets the optional brightness for a coordinate.
     * This returns a String description of the brightness.
     */
    public String getBrightness() {
        return _brightness;
    }

    /**
     * Sets the optional brightness for the position.
     */
    public void setBrightness(String brightness) {
        // Make sure the name is never set to null
        if (brightness != null) {
            _brightness = brightness;
        }
    }

    /**
     * Gets the optional name for a coordinate.
     * This returns the actual object reference, not a copy.
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the optional name for the position.
     * The <code>String</code> object reference is not copied.
     */
    public void setName(String name) {
        // Make sure the name is never set to null
        if (name != null) {
            _name = name;
        }
    }

    /**
     * Diagnostic to dump the contents of the target to System.out.
     */
    public void dump() {
        System.out.println(getPosition());
        System.out.println("Epoch=" + getEpoch());
    }

    /**
     * Convenience method for printing out a position in a nice way
     * for diagnotitcs.
     */
    public String toString() {
        return getPosition();
    }

    /**
     * Gets the system name.
     */
    public String getSystemName() {
        return "(" + getSystemOption().getName() + ")";
    }

    /**
     * Gets the short system name.
     */
    public String getShortSystemName() {
        return SHORT_SYSTEM_NAME;
    }

    /**
     * Gets a short description of the position (its Alt and Az).
     */
    public String getPosition() {
        String c1 = "Az: ";
        String c2 = " Alt: ";
        if (getSystemOption() == SystemType.GALACTIC) {
            c1 = "Long: ";
            c2 = " Lat: ";
        }
        return getName() + c1 + getC1() + c2 + getC2() + " " + getSystemName();
    }

    /**
     * Gets the available options for this coordinate system.
     */
    public TypeBase[] getSystemOptions() {
//        SystemType[] stA = new SystemType[SystemType.TYPES.length];
//        System.arraycopy(SystemType.TYPES, 0, stA, 0, SystemType.TYPES.length);
//        return stA;
        return SystemType.TYPES;
    }

    /**
     * Return a new coordinate system object as J2000.
     */
    public HmsDegTarget getTargetAsJ2000() {
        // Make a copy for returning
        HmsDegTarget j2ksys = new HmsDegTarget();

        int etype = j2ksys.getSystemOption().getTypeCode();

        switch (etype) {
            case SystemType._GALACTIC:
                // Convert from Galactic to FK5(J2000)
                j2ksys = _convertGalactictoJ2000(getC1().getAs(Units.DEGREES),
                                                 getC2().getAs(Units.DEGREES),
                                                 j2ksys);
                break;
            case SystemType._AZ_ALT:
                System.out.println("AZ/Alt not yet supported.");
                break;
        }
        // Copy the object name
        j2ksys.setName(getName());
        return j2ksys;
    }

    // Helper method to do the conversion from B1950 to J2000
    private HmsDegTarget _convertGalactictoJ2000(double latitude,
                                                 double longitude,
                                                 HmsDegTarget in) {
        // Get the input position in degrees
        Point2D.Double input = new Point2D.Double(latitude, longitude);

        // Convert the coordinate from Galactic to FK5
        Point2D.Double result = wcscon.gal2fk5(input);

        in.setSystemOption(HmsDegTarget.SystemType.J2000);
        in.getC1().setAs(result.getX(), Units.DEGREES);
        in.getC2().setAs(result.getY(), Units.DEGREES);
        return in;
    }

    /**
     * Set the position using a J2000 HmsDegTarget
     */
    public void setTargetWithJ2000(HmsDegTarget in)
            throws IllegalArgumentException {
        // First check to see what system "this" is using.
        int etype = getSystemOption().getTypeCode();

        switch (etype) {
            case SystemType._GALACTIC:
                // Convert from FK5(J2000) to Galactic
                _convertJ2000toGalactic(in, this);
                break;
            case SystemType._AZ_ALT:
                // Convert the J2000 to Galactic based on internal epoch
                break;
        }
        // Copy the object name
        setName(in.getName());
    }

    // Helper method to do the conversion from J2000 to Galactic
    private DegDegTarget _convertJ2000toGalactic(HmsDegTarget in,
                                                 DegDegTarget out) {
        // Get the RA and Dec as degrees and create a new Point2D
        double ra = in.getC1().getAs(Units.DEGREES);
        double dec = in.getC2().getAs(Units.DEGREES);
        Point2D.Double input = new Point2D.Double(ra, dec);

        // Convert from FK5 to Galactic
        Point2D.Double result = wcscon.fk52gal(input);
        // Set the system with the results
        out.setSystemOption(SystemType.GALACTIC);
        out.getC1().setAs(result.getX(), Units.DEGREES);
        out.getC2().setAs(result.getY(), Units.DEGREES);
        return out;
    }

}
