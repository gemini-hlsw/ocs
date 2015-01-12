package edu.gemini.spModel.target.system;

import java.util.Date;

/**
 * An abstract base class for Non Sidereal Targets. Contains mechanisms to operate
 * with RA and Dec at a given time, plus the target name, Epoch and brightness.
 */
public abstract class NonSiderealTarget extends ITarget {

    private static final String DEFAULT_NAME = "";

    // XXX temporary, until there is conversion code
    private HMS _ra = new HMS();
    private DMS _dec = new DMS();
    private CoordinateTypes.Epoch _epoch = new CoordinateTypes.Epoch("2000", CoordinateParam.Units.YEARS);
    private String _brightness = DEFAULT_NAME;
    private String _name = DEFAULT_NAME;
    private Date _date = null; // The date for which the position is valid

    /** PIO keys for the horizons object ID; used in XML serialization. */
    public static final String PK_HORIZONS_OBJECT_ID = "horizons-object-id";

    /** PIO keys for the horizons object type ordinal; used in XML serialization. */
    public static final String PK_HORIZONS_OBJECT_TYPE_ORDINAL = "horizons-object-type";

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
     * Sets the first coordinate (right ascension) using a String.
     */
    void setRa(String newStringValue) {
        _ra.setValue(newStringValue);
    }

    /**
     * Sets the second coordinate (declination) using a String.
     */
    void setDec(String newStringValue) {
        _dec.setValue(newStringValue);
    }


    /**
     * Sets the right ascension coordinate using an object implementing
     * the {@link ICoordinate ICoordinate} interface (an HMS object).
     * The input object is not cloned.  Therefore, the caller can
     * alter the contents if he is not careful.
     * <p/>
     * If newValue is null, the method returns without changing the
     * internal value.  This ensures that the object always has a
     * valid <code>ICoordinate</code>(HMS) object.
     * <p/>
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
     * Sets the first coordinate (right ascension) using a String.
     */
    public void setC1(String c1) {
        setRa(c1);
    }

    /**
     * Get the first Coordinate as an ICoordinate.
     */
    public ICoordinate getC1() {
        return _ra;
    }

    /**
     * Gets the first coordinate (right ascension) as a String.
     */
    String raToString() {
        return _ra.toString();
    }

    /**
     * Gets the first coordinate (right ascension) as a String.
     */
    public String c1ToString() {
        return raToString();
    }

    /**
     * Sets the right ascension coordinate using an object implementing
     * the {@link ICoordinate ICoordinate} interface (an DMS object).
     * The input object is not cloned.  Therefore, the caller can
     * alter the contents if not careful.
     * <p/>
     * If newValue is null, the method returns without changing the
     * internal value.  This ensures the object always has a valid
     * <code>ICoordinate</code>(DMS) object.
     * <p/>
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
     * Sets the second coordinate (declination) using a String.
     */
    public void setC2(String c2) {
        setDec(c2);
    }

    /**
     * Get the second Coordinate as an ICoordinate.
     */
    public ICoordinate getC2() {
        return _dec;
    }

    /**
     * Gets the second coordinate (declination) as a String.
     */
    String decToString() {
        return _dec.toString();
    }

    /**
     * Gets the second coordinate (right ascension) as a String.
     */
    public String c2ToString() {
        return decToString();
    }

    /**
     * Set the first and second coordinates using appropriate
     * <code>ICoordinate</code>.
     */
    public void setC1C2(ICoordinate c1, ICoordinate c2)
            throws IllegalArgumentException {
        setC1(c1);
        setC2(c2);
    }

    /**
     * Sets the first and second coordinates using String objects.
     */
    public void setC1C2(String c1, String c2) {
        setC1(c1);
        setC2(c2);
    }


    /**
     * Gets the epoch of this object.
     */
    public CoordinateTypes.Epoch getEpoch() {
        if (_epoch == null) {
            _epoch = _createDefaultEpoch();
        }
        return _epoch;
    }


    /**
     * Sets the epoch.  The value of the parameter is not
     * copied so that future modification will have an effect upon the value
     * stored in this class.
     */
    public void setEpoch(CoordinateTypes.Epoch newValue) {
        _epoch = newValue;
    }


    /**
     * Gets the system's name including the selected (sub)option.
     */
    public String getSystemName() {
        return "(" + getSystemOption().getName() + ")";
    }

    public Date getDateForPosition() {
        return _date;
    }

    public void setDateForPosition(Date date) {
        _date = date;
    }

    /**
     * Returns the current epoch, creating it if necessary.
     */
    private CoordinateTypes.Epoch _createDefaultEpoch() {
        return new CoordinateTypes.Epoch("2000", CoordinateParam.Units.YEARS);
    }

    /**
     * Provide a hashcode for this object.  The class <code>{@link
     * CoordinateParam}</code> implements hashCode.
     */
    public int hashCode() {
        long hc = _name.hashCode() ^ _epoch.hashCode() ^ _brightness.hashCode();
        if (_date != null) hc = hc ^ _date.hashCode();
        return (int) hc ^ (int) (hc >> 32);
    }


    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof NonSiderealTarget)) return false;

        NonSiderealTarget sys = (NonSiderealTarget) obj;

        if (!(_epoch.equals(sys._epoch))) return false;
        if (_date == null && sys._date != null) return false;
        if (_date != null) {
            if (!(_date.equals(sys._date))) return false;
        }
        //Decided that ra and dec won't be considered to decide
        //if two NonSidereal Objects are the same. Therefore, they aren't in
        //the hashCode either.

        // RCN: Horizons Query probably doesn't need to be considered
        // here or in hashcode(). I guess.

        return true;
    }


    public Object clone() {
        NonSiderealTarget result = (NonSiderealTarget) super.clone();
        if (_epoch != null) result._epoch = (CoordinateTypes.Epoch) _epoch.clone();
        if (_date != null) result._date = (Date) _date.clone();
        if (_ra != null) {
            result._ra = (HMS) _ra.clone();
        }
        if (_dec != null) {
            result._dec = (DMS) _dec.clone();
        }
        result._hObjId = _hObjId; // immutable, so don't need to clone
        result._hObjTypeOrd = _hObjTypeOrd;
        return result;
    }

    ///
    /// IHorizonsTarget Impl
    ///

    private Long _hObjId;
    private int _hObjTypeOrd = -1;

	public Long getHorizonsObjectId() {
		return _hObjId;
	}


	public int getHorizonsObjectTypeOrdinal() {
		return _hObjTypeOrd;
	}


	public boolean isHorizonsDataPopulated() {
		return _hObjId != null && _hObjTypeOrd >= 0;
	}


	public void setHorizonsObjectId(Long id) {
		_hObjId = id;
	}


	public void setHorizonsObjectTypeOrdinal(int ord) {
		_hObjTypeOrd = ord;
	}

}
