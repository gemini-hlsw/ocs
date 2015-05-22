// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: HmsDegTarget.java 27482 2010-10-15 18:42:07Z nbarriga $
//
package edu.gemini.spModel.target.system;

import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
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
public final class HmsDegTarget extends ITarget {

    public static final Tag TAG = Tag.SIDEREAL;

    public Tag getTag() {
        return TAG;
    }


    // Various default values.
    // XXX Note: the types derived from CoordinateParam, such as Epoch, are NOT immutable!
    private static final Epoch DEFAULT_EPOCH_2000 = new Epoch(2000, Units.YEARS);
    private static final PM1 DEFAULT_PM1 = new PM1();
    private static final PM2 DEFAULT_PM2 = new PM2();
    private static final RV DEFAULT_RV = new RV();
    private static final Parallax DEFAULT_PARALLAX = new Parallax();
    private static final Date DEFAULT_TAIZ = null;
    private static final String DEFAULT_NAME = "";

    private String _name = DEFAULT_NAME;
    private Epoch _epoch = _createDefaultEpoch();
    private PM1 _pm1 = DEFAULT_PM1;
    private PM2 _pm2 = DEFAULT_PM2;
    // The terms below are rarely used, but someone may want to add them
    // and a software system may be able to use them.
    private RV _rv = DEFAULT_RV;
    private Parallax _parallax = DEFAULT_PARALLAX;
    private Date _taiz = DEFAULT_TAIZ;
    private HMS _ra = new HMS();
    private DMS _dec = new DMS();

    /**
     * The base name of this coordinate system.
     */
    private static final String SYSTEM_NAME = "HMS Deg";

    public static HmsDegTarget fromSkyObject(final SkyObject obj) {
        final HmsDegCoordinates coords = obj.getHmsDegCoordinates();
        final HmsDegCoordinates.Epoch e = coords.getEpoch();

        final HmsDegTarget target = new HmsDegTarget();
        target.setName(obj.getName());
        target.setMagnitudes(obj.getMagnitudes());
        target.setEpoch(new Epoch(e.getYear()));
        target.getRa().setAs(coords.getRa().toDegrees().getMagnitude(), Units.DEGREES);
        target.getDec().setAs(coords.getDec().toDegrees().getMagnitude(), Units.DEGREES);

        // Proper Motion
        final Units mas = Units.MILLI_ARCSECS_PER_YEAR;
        final double pmRa  = coords.getPmRa().toMilliarcsecs().getMagnitude();
        final double pmDec = coords.getPmDec().toMilliarcsecs().getMagnitude();
        target.setPM1(new PM1(pmRa, mas));
        target.setPM2(new PM2(pmDec, mas));

        return target;
    }

    /**
     * Provides clone support.
     * As expected, cloning an HmsDegTarget provides a copy of the
     * object with all new instances of the member objects.
     */
    public HmsDegTarget clone() {
        HmsDegTarget result = (HmsDegTarget) super.clone();

        if (_ra != null) result._ra = (HMS) _ra.clone();
        if (_dec != null) result._dec = (DMS) _dec.clone();

        if (_epoch != null) result._epoch = (Epoch) _epoch.clone();
        if (_pm1 != null) result._pm1 = (PM1) _pm1.clone();
        if (_pm2 != null) result._pm2 = (PM2) _pm2.clone();
        if (_rv != null) result._rv = (RV) _rv.clone();
        if (_parallax != null) result._parallax = (Parallax) _parallax.clone();
        if (_taiz != null) result._taiz = (Date) _taiz.clone();

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
        if (!(_ra.equals(sys._ra)) || !(_dec.equals(sys._dec))) return false;
        if (!(_epoch.equals(sys._epoch))) return false;

        // Should we care about these for equals?
        if (!(_name.equals(sys._name))) return false;

        if (!(_pm1.equals(sys._pm1))) return false;
        if (!(_pm2.equals(sys._pm2))) return false;
        if (!(_rv.equals(sys._rv))) return false;
        if (!(_parallax.equals(sys._parallax))) return false;

        return true;
    }

    /**
     * General method to return the first coordinate (right ascension).
     * This returns a reference to the actual object an {@link HMS HMS}
     * object so take care!
     */
    public ICoordinate getRa() {
        // Ra exists at instance creation.
        return _ra;
    }

    /**
     * General method to return the second coordinate (declination).
     * This returns a reference to the actual object an {@link DMS DMS}
     * object so take care!
     */
    public ICoordinate getDec() {
        // Dec exists at instance creation.
        return _dec;
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



    /** Get the PM RA in mas/y. */
    public double getPropMotionRA() {
        return getPM1().getValue();
    }

    /** Set the PM RA in mas/y. */
    public void setPropMotionRA(final double newValue) {
        setPM1(new PM1(newValue, Units.MILLI_ARCSECS_PER_YEAR));
    }

    /** Get the PM Dec in mas/y. */
    public double getPropMotionDec() {
        return getPM2().getValue();
    }

    /** Set the PM Dec in mas/y. */
    public void setPropMotionDec(final double newValue) {
        setPM2(new PM2(newValue, Units.MILLI_ARCSECS_PER_YEAR));
    }

    /** Get the PM parallax in arcsec. */
    public double getTrackingParallax() {
        return getParallax().arcsecs();
    }

    /** Set the PM parallax in arcsec. */
    public void setTrackingParallax(final double newValue) {
        setParallax(new Parallax(newValue, Units.ARCSECS));
    }

    /** Get the PM radial velocity in km/s. */
    public double getTrackingRadialVelocity() {
        return getRV().getValue();
    }

    /** Set the PM radial velocity in km/s. */
    public void setTrackingRadialVelocity(final double newValue) {
        setRV(new RV(newValue));
    }

    /** Get the contained target epoch in Julian years. */
    public double getTrackingEpoch() {
        return getEpoch().getValue();
    }

    /** Set the contained target epoch as in Julian years. */
    public void setTrackingEpoch(final double trackEpoch) {
        setEpoch(new Epoch(trackEpoch));
    }

}
