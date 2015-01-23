//
// $Id: TargetConfig.java 756 2007-01-08 18:01:24Z gillies $
//
package edu.gemini.wdba.tcc;

import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.system.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
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

    /**
     * Build the portion of the XML document that goes with one Target.
     * <p>
     * Note that only the hmsDegTarget is supported at this time.
     */
    public TargetConfig(SPTarget spTarget) throws WdbaGlueException {
        super(spTarget.getName());

        putParameter(TccNames.OBJNAME, spTarget.getName());

        putParameter(TccNames.BRIGHTNESS, ""); // TODO: can we elide this altogether?
        putParameter(TccNames.TAG, ""); // ugh

        ITarget target = spTarget.getTarget();
        if (target instanceof HmsDegTarget) {
            // System only appears in HmsDegTarget
            addAttribute(TYPE, "hmsdegTarget");
            putParameter(TccNames.SYSTEM, spTarget.getTarget().getTag().tccName);
            _buildHmsDegTarget(spTarget);
        } else if (target instanceof ConicTarget) {
            addAttribute(TYPE, "conicTarget");
            _buildConicTarget(spTarget);
        } else if (target instanceof NamedTarget) {
            addAttribute(TYPE, "namedTarget");
            _buildNamedTarget((NamedTarget) target);
        } else {
            // In all other cases, report a problem and return
            _logAbort("Unsupported target type: " + target.getClass().getName(), null);

        }

        Option<Element> mags = _createMagnitudes(spTarget);
        if (!mags.isEmpty()) add(mags.getValue());
    }

    /**
     * Build a target for a named object
     */
    private void _buildNamedTarget(NamedTarget target) {
        putParameter(TccNames.OBJECT, target.getSolarObject().getDisplayValue());
    }

    /**
     * Build an HMS Deg target
     */
    private void _buildHmsDegTarget(SPTarget target) {

        HmsDegTarget hmsDeg = (HmsDegTarget) target.getTarget();
        putParameter(TccNames.C1, hmsDeg.c1ToString());
        putParameter(TccNames.C2, hmsDeg.c2ToString());
        add(_addProperMotion(hmsDeg));
    }

    private void _buildConicTarget(SPTarget spTarget) {
        ConicTarget target = (ConicTarget) spTarget.getTarget();

        ITarget.Tag option = target.getTag();
        putParameter(TccNames.FORMAT, option.tccName);

        // All have epoch
        putParameter(TccNames.EPOCHOFEL, target.getEpoch().getStringValue());
        // All have orbital inclination
        putParameter(TccNames.ORBINC, target.getInclination().getStringValue());
        // All have longitude of ascending node
        putParameter(TccNames.LONGASCNODE, target.getANode().getStringValue());
        // All have eccentricity
        putParameter(TccNames.ECCENTRICITY, String.valueOf(target.getE()));

        if (option == ITarget.Tag.JPL_MINOR_BODY) {
            putParameter(TccNames.ARGOFPERI, target.getPerihelion().getStringValue());
            putParameter(TccNames.PERIDIST, target.getAQ().getStringValue());
            putParameter(TccNames.EPOCHOFPERI, target.getEpochOfPeri().getStringValue());
        }

        if (option == ITarget.Tag.MPC_MINOR_PLANET) {
            putParameter(TccNames.ARGOFPERI, target.getPerihelion().getStringValue());
            putParameter(TccNames.MEANDIST, target.getAQ().getStringValue());
            putParameter(TccNames.MEANANOM, target.getLM().getStringValue());
        }

    }

    /**
     * Add the proper motion.
     */
    private Element _addProperMotion(HmsDegTarget target) {
        ParamSet ps = new ParamSet(TccNames.PROPER_MOTION);
        ps.putParameter(TccNames.PM1, target.getPM1().getStringValue());
        ps.putParameter(TccNames.PM2, target.getPM2().getStringValue());
        ps.putParameter(TccNames.EPOCH, target.getEpoch().getStringValue());
        // Patch for TCC/OT inconsistency
        String pmunits = target.getPM2().getUnits().getName();
        if (pmunits.equals(TccNames.OT_PMUNITS)) pmunits = TccNames.TCC_PMUNITS;
        ps.putParameter(TccNames.PMUNITS, pmunits);
        ps.putParameter(TccNames.PARALLAX, target.getParallax().getStringValue());
        ps.putParameter(TccNames.RV, target.getRV().getStringValue());
        return ps;
    }

    private Option<Element> _createMagnitudes(SPTarget target) {
        ImList<Magnitude> magList = target.getTarget().getMagnitudes();
        if (magList.size() == 0) return None.instance();

        final ParamSet ps = new ParamSet(TccNames.MAGNITUDES);
        magList.foreach(new ApplyOp<Magnitude>() {
            @Override public void apply(Magnitude mag) {
                String band = mag.getBand().name();
                String brig = String.format("%1.3f", mag.getBrightness());
                ps.putParameter(band, brig);
            }
        });
        return new Some<Element>(ps);
    }

    // private method to log and throw and exception
    private void _logAbort(String message, Exception ex) throws WdbaGlueException {
        LOG.severe(message);
        throw new WdbaGlueException(message, (ex != null) ? ex : null);
    }
}

