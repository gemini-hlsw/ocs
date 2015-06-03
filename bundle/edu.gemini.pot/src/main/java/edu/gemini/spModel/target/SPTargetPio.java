package edu.gemini.spModel.target;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.system.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SPTargetPio {

    private static final Logger LOGGER = Logger.getLogger(SPTarget.class.getName());

    public static final String PARAM_SET_NAME = "spTarget";

    private static final String _NAME = "name";
    private static final String _OBJECT = "object";
    private static final String _SYSTEM = "system";
    private static final String _EPOCH = "epoch";
    private static final String _C1 = "c1";
    private static final String _C2 = "c2";
    private static final String _VALID_DATE = "validAt";
    private static final String _PM1 = "pm1";
    private static final String _PM2 = "pm2";
    private static final String _PARALLAX = "parallax";
    private static final String _RV = "rv";
    private static final String _ANODE = "anode";
    private static final String _AQ = "aq";
    private static final String _E = "e";
    private static final String _INCLINATION = "inclination";
    private static final String _LM = "lm";
    private static final String _N = "n";
    private static final String _PERIHELION = "perihelion";
    private static final String _EPOCH_OF_PERIHELION = "epochOfPeri";

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

    public static ParamSet getParamSet(final SPTarget spt, final PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);

        // Based on instance create the right target
        final ITarget target = spt.getTarget();
        Pio.addParam(factory, paramSet, _NAME, target.getName());

        if (target instanceof HmsDegTarget) {
            final HmsDegTarget t = (HmsDegTarget) target;
            Pio.addParam(factory, paramSet, _SYSTEM, t.getTag().tccName);
            paramSet.addParam(t.getEpoch().getParam(factory, _EPOCH));
            Pio.addParam(factory, paramSet, _C1, t.getRa().toString());
            Pio.addParam(factory, paramSet, _C2, t.getDec().toString());
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
            Pio.addParam(factory, paramSet, _C1, nst.getRa().toString());
            Pio.addParam(factory, paramSet, _C2, nst.getDec().toString());
            if (nst.getDateForPosition() != null) {
                Pio.addParam(factory, paramSet, _VALID_DATE, formatDate(nst.getDateForPosition()));
            }

            if (target instanceof ConicTarget) {
                final ConicTarget t = (ConicTarget) target;
                Pio.addParam(factory, paramSet, _SYSTEM, t.getTag().tccName);
                paramSet.addParam(t.getEpoch().getParam(factory, _EPOCH));

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

        // Add spatial profile and spectral distribution
        if (target.getSpatialProfile().isDefined()) {
            paramSet.addParamSet(SourcePio.toParamSet(target.getSpatialProfile().get(), factory));
        }
        if (target.getSpectralDistribution().isDefined()) {
            paramSet.addParamSet(SourcePio.toParamSet(target.getSpectralDistribution().get(), factory));
        }

        return paramSet;
    }

    private static void setCoordinate(ICoordinate c, String s) {
        // We don't know whether we have HH:MM:SS (DD:MM:SS) or a double in
        // degrees.  Try to parse as a Double and use it if that works.
        if (s != null) {
            try {
                c.setAs(Double.parseDouble(s), CoordinateParam.Units.DEGREES);
            } catch (NumberFormatException ex) {
                c.setValue(s);
            }
        }
    }

    public static void setParamSet(final ParamSet paramSet, final SPTarget spt) {
        if (paramSet == null) return;

        final String name = Pio.getValue(paramSet, _NAME);
        final String system = Pio.getValue(paramSet, _SYSTEM);

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
            setCoordinate(t.getRa(), c1);
            setCoordinate(t.getDec(), c2);

            final CoordinateTypes.Epoch e = new CoordinateTypes.Epoch();
            e.setParam(paramSet.getParam(_EPOCH));
            t.setEpoch(e);

            final CoordinateTypes.PM1 pm1 = new CoordinateTypes.PM1();
            pm1.setParam(paramSet.getParam(_PM1));
            t.setPM1(pm1);

            final CoordinateTypes.PM2 pm2 = new CoordinateTypes.PM2();
            pm2.setParam(paramSet.getParam(_PM2));
            t.setPM2(pm2);

            final CoordinateTypes.Parallax p = new CoordinateTypes.Parallax();
            p.setParam(paramSet.getParam(_PARALLAX));
            t.setParallax(p);

            final CoordinateTypes.RV rv = new CoordinateTypes.RV();
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
            setCoordinate(nst.getRa(), c1);
            setCoordinate(nst.getDec(), c2);

            final String dateStr = Pio.getValue(paramSet, _VALID_DATE);
            final Date validDate = parseDate(dateStr);
            nst.setDateForPosition(validDate);


            if (itarget instanceof ConicTarget) {
                final ConicTarget t = (ConicTarget) itarget;


                final CoordinateTypes.Epoch e = new CoordinateTypes.Epoch();
                e.setParam(paramSet.getParam(_EPOCH));
                t.setEpoch(e);

                final CoordinateTypes.ANode anode = new CoordinateTypes.ANode();
                anode.setParam(paramSet.getParam(_ANODE));
                t.setANode(anode);

                final CoordinateTypes.AQ aq = new CoordinateTypes.AQ();
                aq.setParam(paramSet.getParam(_AQ));
                t.setAQ(aq);

                final Double de = Double.valueOf(Pio.getValue(paramSet, _E));
                t.setE(de);

                final CoordinateTypes.Inclination i = new CoordinateTypes.Inclination();
                i.setParam(paramSet.getParam(_INCLINATION));
                t.setInclination(i);

                final CoordinateTypes.LM lm = new CoordinateTypes.LM();
                lm.setParam(paramSet.getParam(_LM));
                t.setLM(lm);

                final CoordinateTypes.N n = new CoordinateTypes.N();
                n.setParam(paramSet.getParam(_N));
                t.setN(n);

                final CoordinateTypes.Perihelion p = new CoordinateTypes.Perihelion();
                p.setParam(paramSet.getParam(_PERIHELION));
                t.setPerihelion(p);

                final CoordinateTypes.Epoch epochOfperi = new CoordinateTypes.Epoch();
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

        // Add magnitude information to the target.
        final ParamSet magCollectionPset = paramSet.getParamSet(MagnitudePio.MAG_LIST);
        if (magCollectionPset != null) {
            try {
                itarget.setMagnitudes(MagnitudePio.instance.toList(magCollectionPset));
            } catch (final ParseException ex) {
                LOGGER.log(Level.WARNING, "Could not parse target magnitudes", ex);
            }
        }

        // Add spatial profile and spectral distribution
        itarget.setSpatialProfile(SourcePio.profileFromParamSet(paramSet));
        itarget.setSpectralDistribution(SourcePio.distributionFromParamSet(paramSet));

        spt.setTarget(itarget);
    }

    public static SPTarget fromParamSet(final ParamSet pset) {
        final SPTarget res = new SPTarget();
        res.setParamSet(pset);
        return res;
    }

}
