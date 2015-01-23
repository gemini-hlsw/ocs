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
import edu.gemini.skycalc.Coordinates;
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


    private static final String COORDINATE_ZERO = "00:00:00.0";

    ///
    /// FIELDS
    ///

    private ITarget _target;

    ///
    /// CONSTRUCTORS
    ///

    /** SPTarget with default empty target. */
    public SPTarget() {
        this(new HmsDegTarget()); // why not
    }

    /** SPTarget with given target. */
    public SPTarget(final ITarget target) {
        _target = target;
    }

    public SPTarget(final double xaxis, final double yaxis) {
        this();
        _target.getC1().setAs(xaxis, Units.DEGREES);
        _target.getC2().setAs(yaxis, Units.DEGREES);
    }

    /**
     * Assigns the list of magnitudes to associate with this target.  If there
     * are multiple magnitudes associated with the same bandpass, only one will
     * be kept.
     *
     * @param magnitudes new collection of magnitude information to store with
     * the target
     */
    public void setMagnitudes(final ImList<Magnitude> magnitudes) {
        _target.setMagnitudes(magnitudes);
        notifyOfGenericUpdate();
    }

    /**
     * Adds the given magnitude to the collection of magnitudes associated with
     * this target, replacing any other magnitude of the same band if any.
     *
     * @param mag magnitude information to add to the collection of magnitudes
     */
    public void putMagnitude(final Magnitude mag) {
        _target.putMagnitude(mag);
        notifyOfGenericUpdate();
    }



    /**
     * Set the name.
     */
    public void setName(final String name) {
        _target.setName(name);
        _notifyOfUpdate();
    }


    /**
     * Set the xaxis and the yaxis.
     */
    public void setXYFromString(final String xaxisStr, final String yaxisStr) {
        synchronized (this) {
            try {
                _target.setC1(xaxisStr);
            } catch (final IllegalArgumentException ex) {
                //a problem found, set it to 00:00:00
                _target.setC1(COORDINATE_ZERO);
            }
            try {
                _target.setC2(yaxisStr);
            } catch( final IllegalArgumentException ex) {
                //a problem found, set it to 00:00:00
                _target.setC2(COORDINATE_ZERO);
            }
        }
        _notifyOfUpdate();
    }

    /**
     * Set the Coordinate System with a string.
     */
    public void setCoordSys(final ITarget.Tag tag) {
        if (tag != _target.getTag())
            setTarget(ITarget.forTag(tag));
    }

    // ----- Specialized methods for an HmsDegTarget ----------
    /**
     * Get the proper motion RA in mas/y
     */
    public double getPropMotionRA() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getPM1().getValue();
        }
        return res;
    }

    /**
     * Set the proper motion ra in mas/y.
     */
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

    /**
     * Get the proper motion Dec in mas/y
     */
    public double getPropMotionDec() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getPM2().getValue();
        }
        return res;
    }

    /**
     * Set the proper motion Dec in mas/y.
     */
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

    /**
     * Get the tracking epoch in julian years
     */
    public double getTrackingEpoch() {
        final double res = 2000.0;
        final Epoch e = _target.getEpoch();
        if (e == null) return res;

        return e.getValue();
    }

    /**
     * Set the tracking epoch as in julian years.
     */
    public void setTrackingEpoch(final double trackEpoch) {
        final Epoch e = new Epoch(trackEpoch);
        _target.setEpoch(e);
        _notifyOfUpdate();
    }

    /**
     * Get the tracking parallax in arcseconds
     */
    public double getTrackingParallax() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getParallax().getValue();
        }
        return res;
    }

    /**
     * Set the tracking parallax as a string.
     */
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

    /**
     * Get the tracking radial velocity in km/s
     */
    public double getTrackingRadialVelocity() {
        double res = 0.0;
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getRV().getValue();
        }
        return res;
    }

    /**
     * Set the tracking radial velocity in km/s.
     */
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

    /**
     * Return a paramset describing this object
     */
    public ParamSet getParamSet(final PioFactory factory) {
        return SPTargetPio.getParamSet(this, factory);
    }

    /**
     * Initialize this object from the given paramset
     */
    public void setParamSet(final ParamSet paramSet) {
        SPTargetPio.setParamSet(paramSet, this);
    }

    public static SPTarget fromParamSet(final ParamSet pset) {
        return SPTargetPio.fromParamSet(pset);
    }

    /**
     * Standard debugging method.
     */
    public String toString() {
        return _target.toString();
    }

    // I'm making this public so I can call it from an editor when I make
    // a change to the contained target, rather than publishing all the
    // target members through this idiotic class. All of this crap needs
    // to be rewritten.
    public void notifyOfGenericUpdate() {
    	super._notifyOfUpdate();
    }

    /**
     * Gets a Skycalc {@link edu.gemini.skycalc.Coordinates} representation.
     */
    public synchronized Coordinates getSkycalcCoordinates() {
        return new Coordinates(getTarget().getC1().getAs(Units.DEGREES), getTarget().getC2().getAs(Units.DEGREES));
    }

    /**
     * Set the x/y position and notify observers
     */
    public void setXY(final double x, final double y) {
        synchronized (this) {
            _target.getC1().setAs(x, Units.DEGREES);
            _target.getC2().setAs(y, Units.DEGREES);
        }
        _notifyOfUpdate();
    }

    /** Set a new ITarget for this position and notify watchers */
    public void setTarget(final ITarget target) {
        _target = target;
        _notifyOfUpdate();
    }

    /**
     * Returns the position's current target.
     */
    public ITarget getTarget() {
        return _target;
    }


    ///
    /// CLONING
    ///

    /** A function wrapper for cloning SPTargets. */
    public static Function1<SPTarget, SPTarget> CLONE_FUNCTION = new Function1<SPTarget, SPTarget>() {
        @Override public SPTarget apply(final SPTarget target) {
            return (SPTarget) target.clone();
        }
    };

    public Object clone() {
        final SPTarget ntarget;
        try {
            ntarget = (SPTarget) super.clone();

            // We also have to clone the inner target object because it is
            // mutable. We don't need to clone the magnitudes list because it
            // is an immutable list holding immutable objects.
            ntarget._target = (ITarget) _target.clone();
        } catch (final CloneNotSupportedException ex) {
            // Should not happen
            throw new UnsupportedOperationException();
        }
        return ntarget;
    }
}
