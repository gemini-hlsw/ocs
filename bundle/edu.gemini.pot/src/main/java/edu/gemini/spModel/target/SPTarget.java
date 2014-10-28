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
import java.util.HashSet;
import java.util.List;
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
    private static final TypeBase DEFAULT_TARGET_TYPE = HmsDegTarget.SystemType.J2000;

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

    private ImList<Magnitude> magnitudes = ImCollections.emptyList();
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
        synchronized (this) {
            _target = SPTarget.createTarget(DEFAULT_TARGET_TYPE);
            _target.getC1().setAs(xaxis, Units.DEGREES);
            _target.getC2().setAs(yaxis, Units.DEGREES);
        }
    }

    /** Constructs with a {@link SkyObject}, extracting its coordinate and magnitude information. */
    public SPTarget(final SkyObject obj) {
        final HmsDegCoordinates  coords = obj.getHmsDegCoordinates();
        final HmsDegCoordinates.Epoch e = coords.getEpoch();

        // Type
        final HmsDegTarget.SystemType t;
        if (HmsDegCoordinates.Epoch.J2000.equals(e)) {
            t = HmsDegTarget.SystemType.J2000;
        } else if (HmsDegCoordinates.Epoch.B1950.equals(e)) {
            t = HmsDegTarget.SystemType.B1950;
        } else if (HmsDegCoordinates.Epoch.Type.JULIAN.equals(e.getType())) {
            t = HmsDegTarget.SystemType.JNNNN;
        } else {
            t = HmsDegTarget.SystemType.BNNNN;
        }

        // Epoch, RA, Dec
        final HmsDegTarget target = new HmsDegTarget(t);
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
        magnitudes = filterDuplicates(obj.getMagnitudes());
    }

    /** Create a default base position using the HmsDegTarget. */
    public static SPTarget createDefaultBasePosition() {
        final ITarget target = createTarget(HmsDegTarget.DEFAULT_SYSTEM_TYPE);
        if (target == null) return null;
        return new SPTarget(target);
    }

    /**
     * A public factory method to create target instances.
     */
    private static ITarget createTarget(final TypeBase type) {
        ITarget target = null;
        // Based on instance create the right target
        if (type instanceof HmsDegTarget.SystemType) {
            target = new HmsDegTarget((HmsDegTarget.SystemType)type);
        } else if (type instanceof DegDegTarget.SystemType) {
            target = new DegDegTarget((DegDegTarget.SystemType)type);
        } else if (type instanceof ConicTarget.SystemType) {
            target = new ConicTarget((ConicTarget.SystemType)type);
        } else if (type instanceof NamedTarget.SystemType) {
            target = new NamedTarget((NamedTarget.SystemType)type);
        }
        return target;
    }

    /**
     * A public factory method to create a target instance based
     * upon the String name of the coordinate system.
     */
    private static ITarget createTarget(final String coordSys) {
        // Cycle through each known target type looking for matches
        // First HmsDegTarget
        {
            final HmsDegTarget.SystemType[] types = HmsDegTarget.SystemType.TYPES;
            for (final HmsDegTarget.SystemType type : types) {
                if (coordSys.equals(type.getName())) {
                    return new HmsDegTarget(type);
                }
            }
            if (coordSys.equals("Hipparcos")) {
                //LOGGER.info("Transforming Hipparcos to J2000");
                return new HmsDegTarget(HmsDegTarget.SystemType.J2000);
            }
        }
        // Then DegDeg
        {
            final DegDegTarget.SystemType[] types = DegDegTarget.SystemType.TYPES;
            for (final DegDegTarget.SystemType type : types) {
                if (coordSys.equals(type.getName())) {
                    return new DegDegTarget(type);
                }
            }
        }
        // Conic
        {
            final ConicTarget.SystemType[] types = ConicTarget.SystemType.TYPES;
            for (final ConicTarget.SystemType type : types) {
                if (coordSys.equals(type.getName())) {
                    return new ConicTarget(type);
                }
            }
        }
        // Named Target
        {
            final NamedTarget.SystemType[] types = NamedTarget.SystemType.TYPES;
            for (final NamedTarget.SystemType type: types) {
                if (coordSys.equals(type.getName())) {
                    return new NamedTarget(type);
                }
            }
        }

        return null;
    }


    /**
     * Set the brightness.
     * @deprecated
     */
    @Deprecated
    public void setBrightness(final String brightness) {
        synchronized (this) {
            _target.setBrightness(brightness);
        }
        _notifyOfGenericUpdate();
    }

    /**
     * Get the brightness.
     * @deprecated
     */
    @Deprecated
    public String getBrightness() {
        return _target.getBrightness();
    }

    /**
     * Gets all the {@link Magnitude} information associated with this target,
     * if any.
     *
     * @return (possibly empty) immutable list of {@link Magnitude} values
     * associated with this target
     */
    public ImList<Magnitude> getMagnitudes() {
        return magnitudes;
    }

    /**
     * Filters {@link Magnitude} values with the same passband.
     *
     * @param magList original magnitude list possibly containing values with
     * duplicate passbands
     *
     * @return immutable list of {@link Magnitude} where each value in the
     * list is guaranteed to have a distinct passband
     */
    private static ImList<Magnitude> filterDuplicates(final ImList<Magnitude> magList) {
        return magList.filter(new PredicateOp<Magnitude>() {
            private final Set<Magnitude.Band> bands = new HashSet<Magnitude.Band>();
            @Override public Boolean apply(final Magnitude magnitude) {
                final Magnitude.Band band = magnitude.getBand();
                if (bands.contains(band)) return false;
                bands.add(band);
                return true;
            }
        });
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
        this.magnitudes = filterDuplicates(magnitudes);
        notifyOfGenericUpdate();
    }

    /**
     * Gets the {@link Magnitude} value associated with the given magnitude
     * passband.
     *
     * @param band passband of the {@link Magnitude} value to retrieve
     *
     * @return {@link Magnitude} value associated with the given passband,
     * wrapped in a {@link Some} object; {@link None} if none
     */
    public Option<Magnitude> getMagnitude(final Magnitude.Band band) {
        return magnitudes.find(new PredicateOp<Magnitude>() {
            @Override public Boolean apply(final Magnitude magnitude) {
                return band.equals(magnitude.getBand());
            }
        });
    }

    /**
     * Gets the set of magnitude bands that have been recorded in this target.
     *
     * @returns a Set of {@link Magnitude.Band magnitude bands} for which
     * we have information in this target
     */
    public Set<Magnitude.Band> getMagnitudeBands() {
        final ImList<Magnitude.Band> bandList = magnitudes.map(new MapOp<Magnitude, Magnitude.Band>() {
            @Override public Magnitude.Band apply(final Magnitude magnitude) {
                return magnitude.getBand();
            }
        });
        return new HashSet<Magnitude.Band>(bandList.toList());
    }

    /**
     * Adds the given magnitude to the collection of magnitudes associated with
     * this target, replacing any other magnitude of the same band if any.
     *
     * @param mag magnitude information to add to the collection of magnitudes
     */
    public void putMagnitude(final Magnitude mag) {
        magnitudes = magnitudes.filter(new PredicateOp<Magnitude>() {
            @Override public Boolean apply(final Magnitude cur) {
                return cur.getBand() != mag.getBand();
            }
        }).cons(mag);
        notifyOfGenericUpdate();
    }



    /**
     * Set the name.
     */
    public void setName(final String name) {
        synchronized (this) {
            _target.setName(name);
        }
        _notifyOfGenericUpdate();
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
            //set the first coordinate in the target.

            //repeat operation for C2
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
        }
        _notifyOfLocationUpdate();
    }

    /**
     * Set the Coordinate System with an int (presumably from the coordinate
     * system static constants).
     */
    public void setCoordSys(final TypeBase systemOption) throws IllegalArgumentException {
        _target.setSystemOption(systemOption);

        _notifyOfGenericUpdate();
    }

    /**
     * Set the Coordinate System with a string.
     */
    public void setCoordSys(final String coordSysString) throws IllegalArgumentException {
        final TypeBase newCoordSys = _getCoordSys(_target.getSystemOptions(), coordSysString);
        if (newCoordSys == null) {
            final ITarget newTarget = _newTargetType(coordSysString);
            if (newTarget != null) {
                _target = newTarget;
            }
        } else {
            setCoordSys(newCoordSys);
        }

        _notifyOfGenericUpdate();
    }

    // Return the TypeBase object from the array matching the given coordSysString
    private TypeBase _getCoordSys(final TypeBase[] options, final String coordSysString) {
        for (final TypeBase option : options) {
            if (coordSysString.equals(option.getName())) {
                return option;
            }
        }
        return null;
    }

    // return a new target with the given coordSysString, searching in all of the
    // known coordinate types for a matching value
    private ITarget _newTargetType(final String coordSysString) {
        TypeBase newCoordSys = _getCoordSys(ConicTarget.SystemType.TYPES, coordSysString);
        if (newCoordSys != null) {
            return new ConicTarget((ConicTarget.SystemType)newCoordSys);
        }
        newCoordSys = _getCoordSys(HmsDegTarget.SystemType.TYPES, coordSysString);
        if (newCoordSys != null) {
            return new HmsDegTarget((HmsDegTarget.SystemType)newCoordSys);
        }
        newCoordSys = _getCoordSys(DegDegTarget.SystemType.TYPES, coordSysString);
        if (newCoordSys != null) {
            return new DegDegTarget((DegDegTarget.SystemType)newCoordSys);
        }
        newCoordSys = _getCoordSys(NamedTarget.SystemType.TYPES, coordSysString);
        if (newCoordSys != null) {
            return new NamedTarget((NamedTarget.SystemType)newCoordSys);
        }
        return null;
    }

    /**
     * Get coordinate system used by this position.
     */
    public TypeBase getCoordSys() {
        return _target.getSystemOption();
    }

    /**
     * Get coordinate system used by this position as a String.
     */
    public String getCoordSysAsString() {
        return _target.getSystemOption().getName();
    }


    // ----- Specialized methods for an HmsDegTarget ----------
    /**
     * Get the proper motion RA.
     */
    public String getPropMotionRA() {
        String res = "0";
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getPM1().getStringValue();
        }
        return res;
    }

    /**
     * Set the proper motion ra as a string.
     */
    public void setPropMotionRA(final String newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final PM1 pm1 = new PM1(newValue, Units.MILLI_ARCSECS_PER_YEAR);
            t.setPM1(pm1);
            _notifyOfGenericUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get the proper motion Dec.
     */
    public String getPropMotionDec() {
        String res = "0";
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getPM2().getStringValue();
        }
        return res;
    }

    /**
     * Set the proper motion Dec as a string.
     */
    public void setPropMotionDec(final String newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final PM2 pm2 = new PM2(newValue, Units.MILLI_ARCSECS_PER_YEAR);
            t.setPM2(pm2);
            _notifyOfGenericUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get the tracking system.
     */
    public String getTrackingSystem() {
        return getCoordSysAsString();
    }

    /**
     * Set the tracking system as a string.
     */
    public void setTrackingSystem(final String trackSys) {
        setCoordSys(trackSys);
        _notifyOfGenericUpdate();
    }

    /**
     * Get the tracking epoch.
     */
    public String getTrackingEpoch() {
        final String res = "2000";
        final Epoch e = _target.getEpoch();
        if (e == null) return res;

        return Double.toString(e.getValue());
    }

    /**
     * Set the tracking epoch as a string.
     */
    public void setTrackingEpoch(final String trackEpoch) {
        if (trackEpoch == null) return;

        final Epoch e = new Epoch(trackEpoch);
        _target.setEpoch(e);
        _notifyOfGenericUpdate();
    }

    /**
     * Get the tracking parallax.
     */
    public String getTrackingParallax() {
        String res = "0";
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getParallax().getStringValue();
        }
        return res;
    }

    /**
     * Set the tracking parallax as a string.
     */
    public void setTrackingParallax(final String newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final Parallax p = new Parallax(newValue);
            t.setParallax(p);
            _notifyOfGenericUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get the tracking radial velocity.
     */
    public String getTrackingRadialVelocity() {
        String res = "0";
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            res = t.getRV().getStringValue();
        }
        return res;
    }

    /**
     * Set the tracking radial velocity as a string.
     */
    public void setTrackingRadialVelocity(final String newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final RV rv = new RV(newValue);
            t.setRV(rv);
            _notifyOfGenericUpdate();
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Get the tracking effective wavelength.
     */
    public String getTrackingEffectiveWavelength() {
        String res = "";
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final EffWavelength ew = t.getEffWavelength();
            if (ew.equals(HmsDegTarget.AUTO_EFF_WAVELENGTH)) {
                res = "auto";
            } else {
                res = Double.toString(ew.getValue());
            }
        }
        return res;
    }

    /**
     * Set the tracking effective wavelength as a string.
     */
    public void setTrackingEffectiveWavelength(final String newValue) {
        if (_target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget)_target;
            final EffWavelength ew;
            if (newValue.equals("auto"))  // allan: added check
                ew = HmsDegTarget.AUTO_EFF_WAVELENGTH;
            else
                ew = new EffWavelength(newValue);
            t.setEffWavelength(ew);
            _notifyOfGenericUpdate();
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

        // Ok, this is such bullshit. This stuff needs to be done polymorphically.
        // Whoever wrote this class needs to be re-educated.
        if (target instanceof IHorizonsTarget) {
        	final IHorizonsTarget ht = (IHorizonsTarget) target;
        	if (ht.isHorizonsDataPopulated()) {
        		Pio.addLongParam(factory, paramSet, IHorizonsTarget.PK_HORIZONS_OBJECT_ID, ht.getHorizonsObjectId());
        		Pio.addLongParam(factory, paramSet, IHorizonsTarget.PK_HORIZONS_OBJECT_TYPE_ORDINAL, ht.getHorizonsObjectTypeOrdinal());
        	}
        }

        if (target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget) target;
            Pio.addParam(factory, paramSet, _SYSTEM, t.getSystemOption().getName());
            paramSet.addParam(t.getEpoch().getParam(factory, _EPOCH));
            Pio.addParam(factory, paramSet, _BRIGHTNESS, t.getBrightness());
            Pio.addParam(factory, paramSet, _C1, t.c1ToString());
            Pio.addParam(factory, paramSet, _C2, t.c2ToString());
            paramSet.addParam(t.getPM1().getParam(factory, _PM1));
            paramSet.addParam(t.getPM2().getParam(factory, _PM2));
            paramSet.addParam(t.getParallax().getParam(factory, _PARALLAX));
            paramSet.addParam(t.getRV().getParam(factory, _RV));
            paramSet.addParam(t.getEffWavelength().getParam(factory, _WAVELENGTH));
        } else if (target instanceof DegDegTarget) {
            final DegDegTarget t = (DegDegTarget) target;
            Pio.addParam(factory, paramSet, _SYSTEM, t.getSystemOption().getName());
            paramSet.addParam(t.getEpoch().getParam(factory, _EPOCH));
            Pio.addParam(factory, paramSet, _BRIGHTNESS, t.getBrightness());
            Pio.addParam(factory, paramSet, _C1, t.c1ToString());
            Pio.addParam(factory, paramSet, _C2, t.c2ToString());
        } else if (target instanceof NonSiderealTarget) {
            final NonSiderealTarget nst = (NonSiderealTarget) target;
            // OT-495: save and restore RA/Dec for conic targets
            // XXX FIXME: Temporary, until nonsidereal support is implemented
            Pio.addParam(factory, paramSet, _C1, nst.c1ToString());
            Pio.addParam(factory, paramSet, _C2, nst.c2ToString());
            if (nst.getDateForPosition() != null) {
                Pio.addParam(factory, paramSet, _VALID_DATE, formatDate(nst.getDateForPosition()));
            }

            if (target instanceof ConicTarget) {
                final ConicTarget t = (ConicTarget) target;
                Pio.addParam(factory, paramSet, _SYSTEM, t.getSystemOption().getName());
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
                Pio.addParam(factory, paramSet, _SYSTEM, t.getSystemOption().getName());
                Pio.addParam(factory, paramSet, _OBJECT, t.getSolarObject().name());
            }
        }

        // Add magnitude information to the paramset.
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

        final ITarget itarget = createTarget(system);
        if (itarget == null) {
            return;
        }

        itarget.setName(name);

        // Ok, this is such bullshit. This stuff needs to be done polymorphically.
        // Whoever wrote this class needs to be re-educated.
        if (itarget instanceof IHorizonsTarget) {
        	final IHorizonsTarget ht = (IHorizonsTarget) itarget;
        	ht.setHorizonsObjectId(Pio.getLongValue(paramSet, IHorizonsTarget.PK_HORIZONS_OBJECT_ID, null));
        	ht.setHorizonsObjectTypeOrdinal(Pio.getIntValue(paramSet, IHorizonsTarget.PK_HORIZONS_OBJECT_TYPE_ORDINAL, -1));
        }

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

            final EffWavelength eff = new EffWavelength();
            eff.setParam(paramSet.getParam(_WAVELENGTH));
            t.setEffWavelength(eff);

        } else if (itarget instanceof DegDegTarget) {
            final DegDegTarget t = (DegDegTarget)itarget;

            final String c1 = Pio.getValue(paramSet, _C1);
            final String c2 = Pio.getValue(paramSet, _C2);
            t.setC1C2(c1, c2);

            final Epoch e = new Epoch();
            e.setParam(paramSet.getParam(_EPOCH));
            t.setEpoch(e);
            t.setBrightness(brightness);

        } else if (itarget instanceof NonSiderealTarget) {

            final NonSiderealTarget nst = (NonSiderealTarget)itarget;
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
        magnitudes = ImCollections.emptyList();
        final ParamSet magCollectionPset = paramSet.getParamSet(MagnitudePio.MAG_LIST);
        if (magCollectionPset != null) {
            try {
                magnitudes = MagnitudePio.instance.toList(magCollectionPset);
                fixForRel549(magCollectionPset);
            } catch (final ParseException ex) {
                LOGGER.log(Level.WARNING, "Could not parse target magnitudes", ex);
            }

        }
    }

    // REL-549: "AB" and "Jy" were erroneously added to the list of Magnitude.Band options.
    // Format any found as a String like "10.4 AB, 10.5 Jy" and append it to the brightness value.
    // If the current brightness value is not empty, first append a semi-colon separator as in: "2.0 R; 10.4 AB".
    private void fixForRel549(final ParamSet pset) {
        final List<ParamSet> magPsetList = pset.getParamSets(MagnitudePio.MAG);
        if (magPsetList != null) {
            for (final ParamSet magPset : magPsetList) {
                final String bandValue = Pio.getValue(magPset, MagnitudePio.MAG_VAL);
                final String bandName = Pio.getValue(magPset, MagnitudePio.MAG_BAND);
                if (bandValue != null && bandValue.length() != 0 && ("AB".equals(bandName) || "Jy".equals(bandName))) {
                    String s = _target.getBrightness();
                    if (s == null || s.length() == 0) {
                        s = "";
                    } else {
                        s += "; ";
                    }
                    _target.setBrightness(s + bandValue + " " + bandName);
                }
            }
        }
    }

    public static SPTarget fromParamSet(final ParamSet pset) {
//        String name = Pio.getValue(pset, _NAME);
        final SPTarget res = new SPTarget();
        res.setParamSet(pset);
        return res;
    }

// --Commented out by Inspection START (8/18/14 2:45 PM):
//    /**
//     * Standard debugging method.
//     */
//    public void dump() {
//        _target.dump();
//    }
// --Commented out by Inspection STOP (8/18/14 2:45 PM)

    /**
     * Standard debugging method.
     */
    public synchronized String toString() {
        return _target.toString();
    }

    // I'm making this public so I can call it from an editor when I make
    // a change to the contained target, rather than publishing all the
    // target members through this idiotic class. All of this crap needs
    // to be rewritten.
    public void notifyOfGenericUpdate() {
    	super._notifyOfGenericUpdate();
    }

    /**
     * Returns the ICoordinateValue for c1
     */
    public final synchronized ICoordinate getC1() {
        return _target.getC1();
    }

    /**
     * Get the xaxis.
     */
    public final synchronized double getXaxis() {
        final ICoordinate c1 = _target.getC1();
        return c1.getAs(Units.DEGREES);
    }

    /**
     * Returns the ICoordinate for c2
     */
    public final synchronized ICoordinate getC2() {
        return _target.getC2();
    }

    /**
     * Get the yaxis.
     */
    public final synchronized double getYaxis() {
        final ICoordinate c2 = _target.getC2();
        return c2.getAs(Units.DEGREES);
    }

    /**
     * Get the xaxis as a String.
     */
    public synchronized String getXaxisAsString() {
        return _target.c1ToString();
    }

    /**
     * Get the yaxis as a String.
     */
    public synchronized String getYaxisAsString() {
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
        _notifyOfLocationUpdate();
    }

    /**
     * Set the target from (ra, dec) in J2000, converting to
     * the current target's coordinate system internally.
     */
    public void setTargetWithJ2000(final double ra, final double dec) {
        final HmsDegTarget t = new HmsDegTarget(HmsDegTarget.SystemType.J2000);
        t.getC1().setAs(ra, Units.DEGREES);
        t.getC2().setAs(dec, Units.DEGREES);
        t.setName(_target.getName());
        _target.setTargetWithJ2000(t);
        _notifyOfLocationUpdate();
    }

    /** Set a new ITarget for this position and notify watchers */
    public void setTarget(final ITarget target) {
        synchronized (this) {
            _target = target;
        }
        _notifyOfLocationUpdate();
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
            ntarget = (SPTarget)super.clone();
        } catch (final CloneNotSupportedException ex) {
            // Should not happen
            throw new UnsupportedOperationException();
        }
        return ntarget;
    }
}
