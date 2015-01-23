// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPTarget.java 45443 2012-05-23 20:26:52Z abrighton $
//
package edu.gemini.spModel.target;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.system.*;
import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.CoordinateTypes.*;

/**
 * A data object that describes a telescope position and includes methods
 * for extracting positions.
 */
public final class SPTarget extends WatchablePos {

    private ITarget _target;

    /** SPTarget with default empty target. */
    public SPTarget() {
        this(new HmsDegTarget()); // why not
    }

    /** SPTarget with given target. */
    public SPTarget(final ITarget target) {
        _target = target;
    }

    /** SPTarget with the given RA/Dec in degrees. */
    public SPTarget(final double raDeg, final double degDec) {
        this();
        _target.getC1().setAs(raDeg, Units.DEGREES);
        _target.getC2().setAs(degDec, Units.DEGREES);
    }

    /** Return the contained target. */
    public ITarget getTarget() {
        return _target;
    }

    /** Replace the contained target and notify listeners. */
    public void setTarget(final ITarget target) {
        _target = target;
        _notifyOfUpdate();
    }

    /**
     * Replace the contained target with a new, empty target of the specified type, or do nothing
     * if the contained target is of the specified type.
     */
    public void setTargetType(final ITarget.Tag tag) {
        if (tag != _target.getTag())
            setTarget(ITarget.forTag(tag));
    }

    /** Return a paramset describing this SPTarget. */
    public ParamSet getParamSet(final PioFactory factory) {
        return SPTargetPio.getParamSet(this, factory);
    }

    /** Re-initialize this SPTarget from the given paramset */
    public void setParamSet(final ParamSet paramSet) {
        SPTargetPio.setParamSet(paramSet, this);
    }

    /** Construct a new SPTarget from the given paramset */
    public static SPTarget fromParamSet(final ParamSet pset) {
        return SPTargetPio.fromParamSet(pset);
    }

    /** Clone this SPTarget. */
    public SPTarget clone() {
        return new SPTarget((ITarget) _target.clone());
    }


    ///
    /// END OF PUBLIC API ... EVERYTHING FROM HERE DOWN GOES AWAY
    ///


    /** Set contained target magnitudes and notify listeners. */
    public void setMagnitudes(final ImList<Magnitude> magnitudes) {
        _target.setMagnitudes(magnitudes);
        _notifyOfUpdate();
    }

    /** Set a magnitude on the contained target and notify listeners. */
    public void putMagnitude(final Magnitude mag) {
        _target.putMagnitude(mag);
        _notifyOfUpdate();
    }

    /** Set the contained target's name and notify listeners. */
    public void setName(final String name) {
        _target.setName(name);
        _notifyOfUpdate();
    }

    /**
     * Set the contained target's RA and Dec from Strings in HMS/DMS format and notify listeners.
     * Invalid values are replaced with 00:00:00.
     */
    public void setHmsDms(final String hms, final String dms) {
        synchronized (this) {
            try {
                _target.setC1(hms);
            } catch (final IllegalArgumentException ex) {
                _target.setC1("00:00:00.0");
            }
            try {
                _target.setC2(dms);
            } catch( final IllegalArgumentException ex) {
                _target.setC2("00:00:00.0");
            }
        }
        _notifyOfUpdate();
    }

    /** Get the PM RA in mas/y if the contained target is sidereal, otherwise zero. */
    public double getPropMotionRA() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getPM1().getValue();
        }
        return res;
    }

    /** Set the PM RA in mas/y if the contained target is sidereal, otherwise throw. */
    public void setPropMotionRA(final double newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final PM1 pm1 = new PM1(newValue, Units.MILLI_ARCSECS_PER_YEAR);
            t.setPM1(pm1);
            _notifyOfUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /** Get the PM Dec in mas/y if the contained target is sidereal, otherwise zero. */
    public double getPropMotionDec() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getPM2().getValue();
        }
        return res;
    }

    /** Set the PM Dec in mas/y if the contained target is sidereal, otherwise throw. */
    public void setPropMotionDec(final double newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final PM2 pm2 = new PM2(newValue, Units.MILLI_ARCSECS_PER_YEAR);
            t.setPM2(pm2);
            _notifyOfUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /** Get the contained target epoch in Julian years. */
    public double getTrackingEpoch() {
        return getTarget().getEpoch().getValue();
    }

    /** Set the contained target epoch as in Julian years and notify listeners. */
    public void setTrackingEpoch(final double trackEpoch) {
        final Epoch e = new Epoch(trackEpoch);
        _target.setEpoch(e);
        _notifyOfUpdate();
    }

    /** Get the PM parallax in arcsec if the contained target is sidereal, otherwise zero. */
    public double getTrackingParallax() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getParallax().getValue();
        }
        return res;
    }

    /** Set the PM parallax in arcsec if the contained target is sidereal, otherwise throw. */
    public void setTrackingParallax(final double newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final Parallax p = new Parallax(newValue);
            t.setParallax(p);
            _notifyOfUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /** Get the PM radial velocity in km/s if the contained target is sidereal, otherwise zero. */
    public double getTrackingRadialVelocity() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getRV().getValue();
        }
        return res;
    }

    /** Set the PM radial velocity in km/s if the contained target is sidereal, otherwise throw. */
    public void setTrackingRadialVelocity(final double newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final RV rv = new RV(newValue);
            t.setRV(rv);
            _notifyOfUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    // I'm making this public so I can call it from an editor when I make
    // a change to the contained target, rather than publishing all the
    // target members through this idiotic class. All of this crap needs
    // to be rewritten.
    /** @deprecated */
    public void notifyOfGenericUpdate() {
    	super._notifyOfUpdate();
    }

    /** Set the contained target RA/Dec in degrees and notify observers. */
    public void setRaDecDegrees(final double raDeg, final double decDeg) {
        synchronized (this) {
            _target.getC1().setAs(raDeg, Units.DEGREES);
            _target.getC2().setAs(decDeg, Units.DEGREES);
        }
        _notifyOfUpdate();
    }

}
