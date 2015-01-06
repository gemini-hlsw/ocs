// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: HmsDegTarget.java 27482 2010-10-15 18:42:07Z nbarriga $
//
package edu.gemini.spModel.target.system;

import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.CoordinateTypes.*;

import java.util.Date;

/**
 * This class represents a coordinate position in a system that
 * uses two primary positions, c1 and c2.  In this case, c1 is
 * represented by a coordinate in hours, minutes, seconds.  The c2
 * coordinate is repersented by degrees, minutes, seconds of arc.
 * RA/Dec systems of various ilks are represented with this class.
 * <p>
 * This class supports all of the features this type of coordinate
 * can depend on including: parallax, radial velocity, proper
 * motions, and a simple non-sidereal track rate.  These values
 * are not needed however (and at this point are not used
 * much).
 *
 * @author      Shane Walker
 * @author      Kim Gillies (mangled for SP)
 */
public final class HmsDegTarget extends CoordinateSystem
        implements ITarget {
    /**
     * Options for the system type.
     */
    public static final class SystemType extends TypeBase {
        public static final int _J2000 = 0;

        public static final SystemType J2000 =
                new SystemType(_J2000, "J2000");

        public static final SystemType[] TYPES = new SystemType[]{
            J2000,
        };

        private SystemType(int type, String name) {
            super(type, name);
        }

    }

    // Various default values.
    // XXX Note: the types derived from CoordinateParam, such as Epoch, are NOT immutable!
    private static final SystemType DEFAULT_SYSTEM_TYPE = SystemType.J2000;
//    private static final Epoch DEFAULT_EPOCH_1950 = new Epoch(1950, Units.YEARS);
    private static final Epoch DEFAULT_EPOCH_2000 = new Epoch(2000, Units.YEARS);
    private static final PM1 DEFAULT_PM1 = new PM1();
    private static final PM2 DEFAULT_PM2 = new PM2();
    private static final RV DEFAULT_RV = new RV();
    private static final Parallax DEFAULT_PARALLAX = new Parallax();
    private static final Date DEFAULT_TAIZ = null;
    private static final String DEFAULT_NAME = "";
    public static final EffWavelength AUTO_EFF_WAVELENGTH = new EffWavelength(-1.0);
    private static final EffWavelength DEFAULT_EFF_WAVELENGTH = AUTO_EFF_WAVELENGTH;

    private String _brightness = DEFAULT_NAME;
    private String _name = DEFAULT_NAME;
    private Epoch _epoch = _createDefaultEpoch();
    private PM1 _pm1 = DEFAULT_PM1;
    private PM2 _pm2 = DEFAULT_PM2;
    // The terms below are rarely used, but someone may want to add them
    // and a software system may be able to use them.
    private RV _rv = DEFAULT_RV;
    private Parallax _parallax = DEFAULT_PARALLAX;
    private EffWavelength _ew = DEFAULT_EFF_WAVELENGTH;
    private Date _taiz = DEFAULT_TAIZ;
    private HMS _ra = new HMS();
    private DMS _dec = new DMS();

    /**
     * The base name of this coordinate system.
     */
    private static final String SYSTEM_NAME = "HMS Deg";
    private static final String SHORT_SYSTEM_NAME = "hmsdegTarget";

    /**
     * Provides clone support.
     * As expected, cloning an HmsDegTarget provides a copy of the
     * object with all new instances of the member objects.
     */
    public Object clone() {
        HmsDegTarget result = (HmsDegTarget) super.clone();
        result._ra = (HMS) _ra.clone();
        result._dec = (DMS) _dec.clone();
        if (_epoch != null) result._epoch = (Epoch) _epoch.clone();
        if (_pm1 != null) result._pm1 = (PM1) _pm1.clone();
        if (_pm2 != null) result._pm2 = (PM2) _pm2.clone();
        if (_rv != null) result._rv = (RV) _rv.clone();
        if (_parallax != null) result._parallax = (Parallax) _parallax.clone();
        if (_taiz != null) result._taiz = (Date) _taiz.clone();
        if (_ew != null) result._ew = (EffWavelength) _ew.clone();

        // _raTrackingRate is a double (immutable)
        // _decTrackingRate is a double (immutable)
        // _name is a String (immutable)

        return result;
    }

    /**
     * Override equals to return true if both instances are the same.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;

        if (!(obj instanceof HmsDegTarget)) return false;

        HmsDegTarget sys = (HmsDegTarget) obj;
        if (!(getSystemOption().equals(sys.getSystemOption()))) return false;
        if (!(_ra.equals(sys._ra)) || !(_dec.equals(sys._dec))) return false;
        if (!(_epoch.equals(sys._epoch))) return false;

        // Should we care about these for equals?
        if (!(_name.equals(sys._name))) return false;

        if (!(_pm1.equals(sys._pm1))) return false;
        if (!(_pm2.equals(sys._pm2))) return false;
        if (!(_rv.equals(sys._rv))) return false;
        if (!(_parallax.equals(sys._parallax))) return false;
        if (!(_ew.equals(sys._ew))) return false;

        return true;
    }


    /**
     * Constructs with the default property values.
     */
    public HmsDegTarget() {
        super(DEFAULT_SYSTEM_TYPE);
    }

    /**
     * Constructs with the specific HmsDeg system type and default
     * values.
     */
    public HmsDegTarget(SystemType systemOption)
            throws IllegalArgumentException {
        super(systemOption);
    }

    /**
     * Gets the first coordinate (right ascension) as a String.
     */
    public String raToString() {
        return _ra.toString();
    }

    /**
     * Gets the first coordinate (right ascension) as a String.
     */
    public String c1ToString() {
        return raToString();
    }

    /**
     * Gets the second coordinate (declination) as a String.
     */
    public String decToString() {
        return _dec.toString();
    }

    /**
     * Gets the second coordinate (right ascension) as a String.
     */
    public String c2ToString() {
        return decToString();
    }

    /**
     * General method to return the first coordinate (right ascension).
     * This returns a reference to the actual object an {@link HMS HMS}
     * object so take care!
     */
    public ICoordinate getC1() {
        // Ra exists at instance creation.
        return _ra;
    }

    /**
     * General method to return the second coordinate (declination).
     * This returns a reference to the actual object an {@link DMS DMS}
     * object so take care!
     */
    public ICoordinate getC2() {
        // Dec exists at instance creation.
        return _dec;
    }

    /**
     * Sets the right ascension coordinate using an object implementing
     * the {@link ICoordinate ICoordinate} interface (an HMS object).
     * The input object is not cloned.  Therefore, the caller can
     * alter the contents if he is not careful.
     * <p>
     * If newValue is null, the method returns without changing the
     * internal value.  This ensures that the object always has a
     * valid <code>ICoordinate</code>(HMS) object.
     * <p>
     * This method throws IllegalArgumentException if the ICoordinate is
     * not an instance of {@link HMS HMS}.
     */
    public void setC1(ICoordinate newValue)
            throws IllegalArgumentException {
        if (newValue == null) {
            newValue = new HMS();
        }
        if (!(newValue instanceof HMS)) {
            throw new IllegalArgumentException();
        }
        _ra = (HMS) newValue;
    }

    /**
     * Sets the right ascension coordinate using an object implementing
     * the {@link ICoordinate ICoordinate} interface (an DMS object).
     * The input object is not cloned.  Therefore, the caller can
     * alter the contents if not careful.
     * <p>
     * If newValue is null, the method returns without changing the
     * internal value.  This ensures the object always has a valid
     * <code>ICoordinate</code>(DMS) object.
     * <p>
     * This method throws IllegalArgumentException if the ICoordinate is
     * not an instance of {@link HMS HMS}.
     */
    public void setC2(ICoordinate newValue) {
        if (newValue == null) {
            newValue = new DMS();
        }
        if (!(newValue instanceof DMS)) {
            throw new IllegalArgumentException();
        }
        _dec = (DMS) newValue;
    }

    /**
     * General method to set the first and second coordinates together.
     */
    public void setC1C2(ICoordinate c1, ICoordinate c2)
            throws IllegalArgumentException {
        setC1(c1);
        setC2(c2);
    }

    /**
     * Sets the first coordinate (right ascension) using a String.
     */
    void setRa(String newStringValue) {
        _ra.setValue(newStringValue);
    }

    /**
     * Sets the first coordinate (right ascension) using a String.
     */
    public void setC1(String c1) {
        setRa(c1);
    }

    /**
     * Sets the second coordinate (declination) using a String.
     */
    void setDec(String newStringValue) {
        _dec.setValue(newStringValue);
    }

    /**
     * Sets the second coordinate (declination) using a String.
     */
    public void setC2(String c2) {
        setDec(c2);
    }

    /**
     * Sets the right ascension and declination using String objects.
     */
    public void setRaDec(String newRa, String newDec) {
        setRa(newRa);
        setDec(newDec);
    }

    /**
     * Sets the first and second coordinates using String objects.
     */
    public void setC1C2(String c1, String c2) {
        setC1(c1);
        setC2(c2);
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
     * We must assume that if JNNNN or BNNNN are in use, the caller will
     * set the epoch of the coordinate
     */
    private Epoch _createDefaultEpoch() {
        return new Epoch(DEFAULT_EPOCH_2000.getValue(),
                         DEFAULT_EPOCH_2000.getUnits());
    }

    /**
     * Sets the epoch.  The value of the parameter is not
     * copied so future modification will an effect upon the value
     * stored in this class.
     */
    public void setEpoch(Epoch newValue) {
        // Set _epoch to use the new value.  This might be null.
        // This probably needs to be more clever since it can change
        // the system type.
        _epoch = newValue;
    }

    // Override setSystemOption
    protected void _setSystemOption(TypeBase systemOption) {
        super._setSystemOption(systemOption);
        _epoch = DEFAULT_EPOCH_2000;
    }


    /**
     * Gets the proper motion in the first coordinate.
     * This returns a reference to the internal PM1 object.
     */
    public PM1 getPM1() {
        if (_pm1 == null) {
            _pm1 = DEFAULT_PM1;
        }
        return _pm1;
    }


    /**
     * Sets the proper motion in ra.  The value of the
     * parameter is not copied so future modification will have an effect
     * upon the value stored in this class.  The newvalue can be null.
     */
    public void setPM1(PM1 newValue) {
        _pm1 = newValue;
    }


    /**
     * Gets the proper motion in the second coordinate (dec).
     * This returns a reference to the internal PM2 object.
     */
    public PM2 getPM2() {
        if (_pm2 == null) {
            _pm2 = DEFAULT_PM2;
        }
        return _pm2;
    }


    /**
     * Sets the proper motion in Dec.  The value of the
     * parameter is not copied so that future modification will have an effect
     * upon the value stored in this class.  The new value can be null.
     */
    public void setPM2(PM2 newValue) {
        _pm2 = newValue;
    }


    /**
     * Gets the radial velocity object.
     * This method returns a reference to the internal object.
     */
    public RV getRV() {
        if (_rv == null) {
            _rv = DEFAULT_RV;
        }
        return _rv;
    }


    /**
     * Sets the radial velocity.  The value of the
     * parameter is not copied so future modification will have an effect
     * upon the value stored in this class.  The new value can be null.
     */
    public void setRV(RV newValue) {
        _rv = newValue;
    }


    /**
     * Gets the parallax object.
     * This method returns a reference to the internal object.
     */
    public Parallax getParallax() {
        if (_parallax == null) {
            _parallax = DEFAULT_PARALLAX;
        }
        return _parallax;
    }


    /**
     * Sets the parallax.  The value of the
     * parameter is not copied so future modification will have an effect
     * upon the value stored in this class.  The new value can be null.
     */
    public void setParallax(Parallax newValue) {
        _parallax = newValue;
    }

    /**
     * Gets the effective wavelength of the target.
     * This method returns a reference to the internal object.
     */
    public EffWavelength getEffWavelength() {
        if (_ew == null) {
            _ew = DEFAULT_EFF_WAVELENGTH;
        }
        return _ew;
    }

    /**
     * Sets the effective wavelength.  The value of the
     * parameter is not copied so future modification will have an effect
     * upon the value stored in this class.  The new value can be null.
     */
    public void setEffWavelength(EffWavelength newValue) {
        _ew = newValue;
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
     * Gets the system name.
     */
    public String getSystemName() {
        return SYSTEM_NAME + " (" + getSystemOption().getName() + ")";
    }

    /**
     * Gets the short system name.
     */
    public String getShortSystemName() {
        return SHORT_SYSTEM_NAME;
    }

    /**
     * Gets a short description of the position (its RA and Dec, epoch).
     */
    public String getPosition() {
        return (getName().isEmpty() ? "" : getName() + " ") + "RA: " + getC1() + " Dec: " + getC2() + " " + getSystemName();
    }

    /**
     * Convenience method for printing out a position.
     */
    public String toString() {
        return getPosition();
    }


    /**
     * Gets the available options for this coordinate system.
     */
    public TypeBase[] getSystemOptions() {
        return SystemType.TYPES;
    }

}
