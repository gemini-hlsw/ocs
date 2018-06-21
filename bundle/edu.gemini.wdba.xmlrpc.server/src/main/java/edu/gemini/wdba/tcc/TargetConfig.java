package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import org.dom4j.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>{@link ParamSet}</code> implementation for a single target.
 *
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public final class TargetConfig extends ParamSet {
    private static final Logger LOG = Logger.getLogger(TargetConfig.class.getName());

    /**
     * Figure out the tag name associated with the given guider, which requires
     * a bit of hackery to support the TCC and all the code for dealing with
     * "OIWFS" and "AOWFS" as though it were a single guider and not a class
     * of guiders.
     */
    public static String getTag(final GuideProbe guider) {
        final GuideProbe.Type type = guider.getType();

        if ((type == GuideProbe.Type.OIWFS) && !(guider instanceof GsaoiOdgw)) {
            return GuideProbe.Type.OIWFS.name();
        }

        if ((type == GuideProbe.Type.AOWFS) && !(guider instanceof Canopus.Wfs)) {
            return GuideProbe.Type.AOWFS.name();
        }

        return guider.getKey();
    }

    public static String formatName(final String tag, final int position) {
        return String.format("%s (%d)", tag, position);
    }

    public TargetConfig(final SPCoordinates spc, final String tag) throws WdbaGlueException {
        super(spc.getName());

        // The coordinates are fixed.
        final Coordinates cs = spc.coordinates();
        addAttribute(TYPE, "hmsdegTarget");
        putParameter(TccNames.OBJNAME,     spc.getName());
        putParameter(TccNames.BRIGHTNESS, "");
        putParameter(TccNames.TAG,         tag);
        putParameter(TccNames.SYSTEM,     "J2000");
        putParameter(TccNames.C1,          cs.ra().toAngle().formatHMS());
        putParameter(TccNames.C2,          cs.dec().formatDMS());
    }

    public TargetConfig(final SPTarget spt, final String tag) throws WdbaGlueException {
        super(spt.getName());
        final Target t = spt.getTarget();

        // Get coordinates right now
        final scala.Option<Coordinates> ocs = t.coords(System.currentTimeMillis());
        final Coordinates cs = ocs.isDefined() ? ocs.get() : Coordinates.zero();

        // All targets are sidereal as far as the TCC is concerned
        addAttribute(TYPE, "hmsdegTarget");

        // Parameters
        putParameter(TccNames.OBJNAME,    t.name());
        putParameter(TccNames.BRIGHTNESS, "");
        putParameter(TccNames.TAG,        tag);
        putParameter(TccNames.SYSTEM,     "J2000");
        putParameter(TccNames.C1,         cs.ra().toAngle().formatHMS());
        putParameter(TccNames.C2,         cs.dec().formatDMS());

        if (t instanceof SiderealTarget) {
            add(_addProperMotion((SiderealTarget) t));
        }
        if (t instanceof NonSiderealTarget) {
            final NonSiderealTarget ns = (NonSiderealTarget) t;
            ImOption.fromScalaOpt(ns.horizonsDesignation()).foreach(hd ->
                    putParameter(TccNames.EPHEMERIS, ephemerisFile(hd)));
        }

        Option<Element> mags = _createMagnitudes(spt);
        if (!mags.isEmpty()) add(mags.getValue());
    }

    /**
     * Returns the name of the ephemeris file associated with the given
     * horizons designation.
     */
    public static String ephemerisFile(final HorizonsDesignation hd) {
        try {
            // See TcsEphemerisExport.
            return URLEncoder.encode(hd.show() + ".eph", StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, "UTF-8 is not supported!", ex);
            throw new RuntimeException(ex);
        }
    }

    private Element _addProperMotion(final SiderealTarget t) {
        final ParamSet ps = new ParamSet(TccNames.PROPER_MOTION);

        final scala.Option<ProperMotion> opm = t.properMotion();
        final ProperMotion pm = opm.isDefined() ? opm.get() : ProperMotion.zero();
        ps.putParameter(TccNames.PM1,     Double.toString(pm.deltaRA().velocity()));
        ps.putParameter(TccNames.PM2,     Double.toString(pm.deltaDec().velocity()));
        ps.putParameter(TccNames.EPOCH,   Double.toString(pm.epoch().year()));
        ps.putParameter(TccNames.PMUNITS, TccNames.TCC_PMUNITS);

        final scala.Option<Redshift> or = t.redshift();
        final Redshift r = or.isDefined() ? or.get() : Redshift.zero();
        ps.putParameter(TccNames.RV, Double.toString(r.toRadialVelocity().toKilometersPerSecond()));

        final scala.Option<Parallax> op = t.parallax();
        final Parallax p = op.isDefined() ? op.get() : Parallax.zero();
        ps.putParameter(TccNames.PARALLAX, Double.toString(p.mas()));

        return ps;
    }

    private Option<Element> _createMagnitudes(final SPTarget target) {
        final ImList<Magnitude> magList = target.getMagnitudesJava();
        if (magList.size() == 0) return None.instance();
        final ParamSet ps = new ParamSet(TccNames.MAGNITUDES);
        magList.foreach(mag -> {
            String band = mag.band().name();
            String brig = String.format("%1.3f", mag.value());
            ps.putParameter(band, brig);
        });
        return new Some<>(ps);
    }

}

