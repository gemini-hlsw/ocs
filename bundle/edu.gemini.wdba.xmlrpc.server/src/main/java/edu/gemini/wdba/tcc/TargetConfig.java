//
// $Id: TargetConfig.java 756 2007-01-08 18:01:24Z gillies $
//
package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.Parallax;
import edu.gemini.spModel.core.ProperMotion;
import edu.gemini.spModel.core.Redshift;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.core.Target;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import org.dom4j.Element;

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
    public static String getTag(GuideProbe guider) {
        GuideProbe.Type type = guider.getType();

        if ((type == GuideProbe.Type.OIWFS) && !(guider instanceof GsaoiOdgw)) {
            return GuideProbe.Type.OIWFS.name();
        }

        if ((type == GuideProbe.Type.AOWFS) && !(guider instanceof Canopus.Wfs)) {
            return GuideProbe.Type.AOWFS.name();
        }

        return guider.getKey();
    }

    public static String formatName(String tag, int position) {
        return String.format("%s (%d)", tag, position);
    }

    public TargetConfig(SPTarget spt) throws WdbaGlueException {
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
        putParameter(TccNames.TAG,        "");
        putParameter(TccNames.SYSTEM,     "J2000");
        putParameter(TccNames.C1,         cs.ra().toAngle().formatHMS());
        putParameter(TccNames.C2,         cs.dec().formatDMS());

        if (t instanceof SiderealTarget) {
            add(_addProperMotion((SiderealTarget) t));
        }

        Option<Element> mags = _createMagnitudes(spt);
        if (!mags.isEmpty()) add(mags.getValue());

    }

    private Element _addProperMotion(SiderealTarget t) {
        ParamSet ps = new ParamSet(TccNames.PROPER_MOTION);

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

    private Option<Element> _createMagnitudes(SPTarget target) {
        ImList<Magnitude> magList = target.getNewMagnitudesJava();
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

