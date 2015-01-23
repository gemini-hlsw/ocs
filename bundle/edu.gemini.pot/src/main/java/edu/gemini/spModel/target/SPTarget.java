// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPTarget.java 45443 2012-05-23 20:26:52Z abrighton $
//
package edu.gemini.spModel.target;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.system.*;
import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.CoordinateTypes.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A data object that describes a telescope position and includes methods
 * for extracting positions.
 */
public final class SPTarget extends WatchablePos {


    private static final String COORDINATE_ZERO = "00:00:00.0";
    private static final Logger LOGGER = Logger.getLogger(SPTarget.class.getName());

    ///
    /// DATE HANDLING
    ///

    private static final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static synchronized String formatDate(final Date d) {
        return formatter.format(d);
    }

    private static synchronized Date parseDate(final String dateStr) {
        if (dateStr == null) return null;

        DateFormat format = formatter;
        if (!dateStr.contains("UTC")) {
            // OT-755: we didn't used to store the time zone, which
            // led to bugs when exporting in one time zone and importing
            // in another -- say when the program is stored.
            // If the date doesn't include "UTC", then assume it is in
            // the old style and import in the local time zone so that
            // at least the behavior when reading in existing programs for
            // the first time won't change.
            format = DateFormat.getInstance();
        }

        try {
            return format.parse(dateStr);
        } catch (final ParseException e) {
            LOGGER.log(Level.WARNING, " Invalid date found " + dateStr);
            return null;
        }
    }

    ///
    /// PARAMSET
    ///

    public static final String PARAM_SET_NAME = "spTarget";

    private static final String _NAME = "name";
    private static final String _OBJECT = "object";
    private static final String _SYSTEM = "system";
    private static final String _EPOCH = "epoch";
    private static final String _BRIGHTNESS = "brightness";
    private static final String _C1 = "c1";
    private static final String _C2 = "c2";
    private static final String _VALID_DATE = "validAt";
    private static final String _PM1 = "pm1";
    private static final String _PM2 = "pm2";
    private static final String _PARALLAX = "parallax";
    private static final String _RV = "rv";
    private static final String _WAVELENGTH = "wavelength";
    private static final String _ANODE = "anode";
    private static final String _AQ = "aq";
    private static final String _E = "e";
    private static final String _INCLINATION = "inclination";
    private static final String _LM = "lm";
    private static final String _N = "n";
    private static final String _PERIHELION = "perihelion";
    private static final String _EPOCH_OF_PERIHELION = "epochOfPeri";


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

    /** Constructs with a {@link SkyObject}, extracting its coordinate and magnitude information. */
    public SPTarget(final SkyObject obj) {
        final HmsDegCoordinates  coords = obj.getHmsDegCoordinates();
        final HmsDegCoordinates.Epoch e = coords.getEpoch();

        // Epoch, RA, Dec
        final HmsDegTarget target = new HmsDegTarget();
        target.setEpoch(new Epoch(e.getYear()));
        target.setC1(new HMS(coords.getRa().toDegrees().getMagnitude()));
        target.setC2(new DMS(coords.getDec().toDegrees().getMagnitude()));

        // Proper Motion
        final Units mas = Units.MILLI_ARCSECS_PER_YEAR;
        final double pmRa  = coords.getPmRa().toMilliarcsecs().getMagnitude();
        final double pmDec = coords.getPmDec().toMilliarcsecs().getMagnitude();
        target.setPM1(new PM1(pmRa, mas));
        target.setPM2(new PM2(pmDec, mas));

        _target = target;
        setName(obj.getName());
        setMagnitudes(obj.getMagnitudes());
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
     * Gets the set of magnitude bands that have been recorded in this target.
     *
     * @returns a Set of {@link Magnitude.Band magnitude bands} for which
     * we have information in this target
     */
    public Set<Magnitude.Band> getMagnitudeBands() {
        return _target.getMagnitudeBands();
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
     * Get the name.
     */
    public String getName() {
        return _target.getName();
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
        final ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);

        // Based on instance create the right target
        final ITarget target = getTarget();
        Pio.addParam(factory, paramSet, _NAME, getName());

        if (target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget) target;
            Pio.addParam(factory, paramSet, _SYSTEM, t.getTag().tccName);
            paramSet.addParam(t.getEpoch().getParam(factory, _EPOCH));
            Pio.addParam(factory, paramSet, _BRIGHTNESS, t.getBrightness());
            Pio.addParam(factory, paramSet, _C1, t.c1ToString());
            Pio.addParam(factory, paramSet, _C2, t.c2ToString());
            paramSet.addParam(t.getPM1().getParam(factory, _PM1));
            paramSet.addParam(t.getPM2().getParam(factory, _PM2));
            paramSet.addParam(t.getParallax().getParam(factory, _PARALLAX));
            paramSet.addParam(t.getRV().getParam(factory, _RV));
        } else if (target instanceof NonSiderealTarget) {
            final NonSiderealTarget nst = (NonSiderealTarget) target;

            // Horizons data, if any
            if (nst.isHorizonsDataPopulated()) {
                Pio.addLongParam(factory, paramSet, NonSiderealTarget.PK_HORIZONS_OBJECT_ID, nst.getHorizonsObjectId());
                Pio.addLongParam(factory, paramSet, NonSiderealTarget.PK_HORIZONS_OBJECT_TYPE_ORDINAL, nst.getHorizonsObjectTypeOrdinal());
            }

            // OT-495: save and restore RA/Dec for conic targets
            // XXX FIXME: Temporary, until nonsidereal support is implemented
            Pio.addParam(factory, paramSet, _C1, nst.c1ToString());
            Pio.addParam(factory, paramSet, _C2, nst.c2ToString());
            if (nst.getDateForPosition() != null) {
                Pio.addParam(factory, paramSet, _VALID_DATE, formatDate(nst.getDateForPosition()));
            }

            if (target instanceof ConicTarget) {
                final ConicTarget t = (ConicTarget) target;
                Pio.addParam(factory, paramSet, _SYSTEM, t.getTag().tccName);
                paramSet.addParam(t.getEpoch().getParam(factory, _EPOCH));
                Pio.addParam(factory, paramSet, _BRIGHTNESS, t.getBrightness());

                paramSet.addParam(t.getANode().getParam(factory, _ANODE));
                paramSet.addParam(t.getAQ().getParam(factory, _AQ));
                Pio.addParam(factory, paramSet, _E, Double.toString(t.getE()));
                paramSet.addParam(t.getInclination().getParam(factory, _INCLINATION));
                paramSet.addParam(t.getLM().getParam(factory, _LM));
                paramSet.addParam(t.getN().getParam(factory, _N));
                paramSet.addParam(t.getPerihelion().getParam(factory, _PERIHELION));
                paramSet.addParam(t.getEpochOfPeri().getParam(factory, _EPOCH_OF_PERIHELION));
            } else if (target instanceof NamedTarget) {
                final NamedTarget t = (NamedTarget) target;
                Pio.addParam(factory, paramSet, _SYSTEM, t.getTag().tccName);
                Pio.addParam(factory, paramSet, _OBJECT, t.getSolarObject().name());
            }
        }

        // Add magnitude information to the paramset.
        final ImList<Magnitude> magnitudes = target.getMagnitudes();
        if (magnitudes.size() > 0) {
            paramSet.addParamSet(MagnitudePio.instance.toParamSet(factory, magnitudes));
        }

        return paramSet;
    }

    /**
     * Initialize this object from the given paramset
     */
    public void setParamSet(final ParamSet paramSet) {
        if (paramSet == null) return;

        final String name = Pio.getValue(paramSet, _NAME);
        final String system = Pio.getValue(paramSet, _SYSTEM);
        final String brightness = Pio.getValue(paramSet, _BRIGHTNESS);

        // The system is the tccName, so we need to find it.
        ITarget itarget = null;
        for (ITarget.Tag t: ITarget.Tag.values()) {
            if (t.tccName.equals(system)) {
                itarget = ITarget.forTag(t);
                break;
            }
        }
        if (itarget == null)
            throw new IllegalArgumentException("No target tag with tccName " + system);

        itarget.setName(name);

        if (itarget instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)itarget;

            final String c1 = Pio.getValue(paramSet, _C1);
            final String c2 = Pio.getValue(paramSet, _C2);
            t.setC1C2(c1, c2);

            final Epoch e = new Epoch();
            e.setParam(paramSet.getParam(_EPOCH));
            t.setEpoch(e);

            t.setBrightness(brightness);

            final PM1 pm1 = new PM1();
            pm1.setParam(paramSet.getParam(_PM1));
            t.setPM1(pm1);

            final PM2 pm2 = new PM2();
            pm2.setParam(paramSet.getParam(_PM2));
            t.setPM2(pm2);

            final Parallax p = new Parallax();
            p.setParam(paramSet.getParam(_PARALLAX));
            t.setParallax(p);

            final RV rv = new RV();
            rv.setParam(paramSet.getParam(_RV));
            t.setRV(rv);

        } else if (itarget instanceof NonSiderealTarget) {

            final NonSiderealTarget nst = (NonSiderealTarget)itarget;

            // Horizons Info
            nst.setHorizonsObjectId(Pio.getLongValue(paramSet, NonSiderealTarget.PK_HORIZONS_OBJECT_ID, null));
            nst.setHorizonsObjectTypeOrdinal(Pio.getIntValue(paramSet, NonSiderealTarget.PK_HORIZONS_OBJECT_TYPE_ORDINAL, -1));

            // OT-495: save and restore RA/Dec for conic targets
            // XXX FIXME: Temporary, until nonsidereal support is implemented
            final String c1 = Pio.getValue(paramSet, _C1);
            final String c2 = Pio.getValue(paramSet, _C2);
            if (c1 != null && c2 != null) {
                nst.setC1C2(c1, c2);
            }

            final String dateStr = Pio.getValue(paramSet, _VALID_DATE);
            final Date validDate = parseDate(dateStr);
            nst.setDateForPosition(validDate);


            if (itarget instanceof ConicTarget) {
                final ConicTarget t = (ConicTarget) itarget;


                final Epoch e = new Epoch();
                e.setParam(paramSet.getParam(_EPOCH));
                t.setEpoch(e);

                t.setBrightness(brightness);

                final ANode anode = new ANode();
                anode.setParam(paramSet.getParam(_ANODE));
                t.setANode(anode);

                final AQ aq = new AQ();
                aq.setParam(paramSet.getParam(_AQ));
                t.setAQ(aq);

                final Double de = Double.valueOf(Pio.getValue(paramSet, _E));
                t.setE(de);

                final Inclination i = new Inclination();
                i.setParam(paramSet.getParam(_INCLINATION));
                t.setInclination(i);

                final LM lm = new LM();
                lm.setParam(paramSet.getParam(_LM));
                t.setLM(lm);

                final N n = new N();
                n.setParam(paramSet.getParam(_N));
                t.setN(n);

                final Perihelion p = new Perihelion();
                p.setParam(paramSet.getParam(_PERIHELION));
                t.setPerihelion(p);

                final Epoch epochOfperi = new Epoch();
                epochOfperi.setParam(paramSet.getParam(_EPOCH_OF_PERIHELION));
                t.setEpochOfPeri(epochOfperi);
            } else if (itarget instanceof NamedTarget) {
                final NamedTarget t = (NamedTarget) itarget;
                final String planet = Pio.getValue(paramSet, _OBJECT);
                try {
                    t.setSolarObject(NamedTarget.SolarObject.valueOf(planet));
                } catch (final IllegalArgumentException ex) {
                    //this shouldn't happen, unless corrupted data
                    LOGGER.log(Level.WARNING, "Invalid Planet found : " + planet);
                }
            }
        }
        _target = itarget;


        // Add magnitude information to the target.
        final ParamSet magCollectionPset = paramSet.getParamSet(MagnitudePio.MAG_LIST);
        if (magCollectionPset != null) {
            try {
                _target.setMagnitudes(MagnitudePio.instance.toList(magCollectionPset));
            } catch (final ParseException ex) {
                LOGGER.log(Level.WARNING, "Could not parse target magnitudes", ex);
            }
        }
    }

    public static SPTarget fromParamSet(final ParamSet pset) {
        final SPTarget res = new SPTarget();
        res.setParamSet(pset);
        return res;
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
     * Returns the ICoordinateValue for c1
     */
    public final ICoordinate getC1() {
        return _target.getC1();
    }

    /**
     * Get the xaxis.
     */
    public final double getXaxis() {
        final ICoordinate c1 = _target.getC1();
        return c1.getAs(Units.DEGREES);
    }

    /**
     * Returns the ICoordinate for c2
     */
    public final ICoordinate getC2() {
        return _target.getC2();
    }

    /**
     * Get the yaxis.
     */
    public final double getYaxis() {
        final ICoordinate c2 = _target.getC2();
        return c2.getAs(Units.DEGREES);
    }

    /**
     * Get the xaxis as a String.
     */
    public String getXaxisAsString() {
        return _target.c1ToString();
    }

    /**
     * Get the yaxis as a String.
     */
    public String getYaxisAsString() {
        return _target.c2ToString();
    }

    /**
     * Gets a Skycalc {@link edu.gemini.skycalc.Coordinates} representation.
     */
    public synchronized Coordinates getSkycalcCoordinates() {
        return new Coordinates(getXaxis(), getYaxis());
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
