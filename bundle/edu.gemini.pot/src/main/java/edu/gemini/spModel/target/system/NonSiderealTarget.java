package edu.gemini.spModel.target.system;

import java.util.Date;

/**
 * An abstract base class for Non Sidereal Targets. Contains mechanisms to operate
 * with RA and Dec at a given time, plus the target name, Epoch and brightness.
 */
public abstract class NonSiderealTarget extends ITarget {

    private static final String DEFAULT_NAME = "";

    // XXX temporary, until there is conversion code
    private final HMS _ra = new HMS();
    private final DMS _dec = new DMS();
    private CoordinateTypes.Epoch _epoch = new CoordinateTypes.Epoch("2000", CoordinateParam.Units.YEARS);
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
     * Get the first Coordinate as an ICoordinate.
     */
    public ICoordinate getRa() {
        return _ra;
    }

    /**
     * Get the second Coordinate as an ICoordinate.
     */
    public ICoordinate getDec() {
        return _dec;
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
        long hc = _name.hashCode() ^ _epoch.hashCode();
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


    public NonSiderealTarget clone() {
        NonSiderealTarget result = (NonSiderealTarget) super.clone();
        if (_epoch != null) result._epoch = (CoordinateTypes.Epoch) _epoch.clone();
        if (_date != null) result._date = (Date) _date.clone();
        result._ra.setValue(_ra.getValue());
        result._dec.setValue(_dec.getValue());
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
