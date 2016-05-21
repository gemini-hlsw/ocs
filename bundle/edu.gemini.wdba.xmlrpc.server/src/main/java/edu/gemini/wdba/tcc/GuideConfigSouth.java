package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 *  Class to evaluate the {@link ObservationEnvironment} and produce a guide config and guide config  name.
 */
public class GuideConfigSouth extends ParamSet {
    //private static final Logger LOG = LogUtil.getLogger(GuideConfigSouth.class);

    private ObservationEnvironment _oe;

    public GuideConfigSouth(ObservationEnvironment oe) {
        super(TccNames.GUIDE_CONFIG);
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        _oe = oe;
    }

    private static boolean containsP1(ObservationEnvironment oe) {
        return oe.containsTargets(PwfsGuideProbe.pwfs1);
    }

    private static boolean containsOiwfs(ObservationEnvironment oe) {
        // Hack here because the TCC, TCS, Seqexec don't consider the GSAOI
        // ODGW to be "on instrument".  Explicitly check for these probes.
        if (!oe.containsTargets(GuideProbe.Type.OIWFS)) return false;
        Set<GuideProbe> odgwSet = new HashSet<>(Arrays.asList(GsaoiOdgw.values()));
        GuideGroup grp = oe.getTargetEnvironment().getPrimaryGuideGroup();
        for (GuideProbeTargets gt : grp.getAllMatching(GuideProbe.Type.OIWFS)) {
            if (!odgwSet.contains(gt.getGuider())) return true;
        }
        return false;
    }

    private static boolean containsGsaoi(ObservationEnvironment oe) {
        for (GuideProbe probe : GsaoiOdgw.values()) {
            if (oe.containsTargets(probe)) return true;
        }
        return false;
    }

    private static boolean containsGems(ObservationEnvironment oe) {
        for (GuideProbe probe : Canopus.Wfs.values()) {
            if (oe.containsTargets(probe)) return true;
        }
        return false;
    }

    /**
     * Checks to see if an AO guide config name should be returned.
     * 1. If there is no AO guide object, it returns
     * So there is an AO guide object
     * 2. If there is an OIWFS return AO-OI
     * 3. If there is a P1WFS object return AO-P1
     * 4. If there is a P2WFS object return AO-P2
     * 5. Else No guiding
     *
     * @return a String that is the AO guiding config or Null
     */
    private String _getAOGuideConfig() {
        if (containsOiwfs(_oe)) return TccNames.AOOI;
        if (_oe.containsTargets(PwfsGuideProbe.pwfs1)) return TccNames.AOP1;
        if (_oe.containsTargets(PwfsGuideProbe.pwfs2)) return TccNames.AOP2;
        return TccNames.AO;
    }

    private String _getGeMSGuideConfig() {
        if (containsOiwfs(_oe)) {
            return containsP1(_oe) ? TccNames.GeMSP1OI : TccNames.GeMSOI;
        }
        return containsP1(_oe) ? TccNames.GeMSP1 : TccNames.GeMS;
    }

    private String _getOIGuideConfig() {
        if (_oe.containsTargets(PwfsGuideProbe.pwfs1)) return TccNames.P1OI;
        if (_oe.containsTargets(PwfsGuideProbe.pwfs2)) return TccNames.P2OI;
        return TccNames.OI;
    }

    private String _getPWFSGuideConfig() {
        if (_oe.containsTargets(PwfsGuideProbe.pwfs1)) {
            return _oe.containsTargets(PwfsGuideProbe.pwfs2) ? TccNames.P1P2 : TccNames.P1;
        }
        // If we get here, then P1 isn't set so check for P2 if not P2 return null
        return _oe.containsTargets(PwfsGuideProbe.pwfs2) ? TccNames.P2 : TccNames.NO_GUIDING;
    }

    private boolean isAltairP1() {
        return containsP1(_oe) && _oe.isAltair() &&
                ImOption.apply(_oe.getAltairConfig()).map(InstAltair::getMode).contains(AltairParams.Mode.LGS_P1);
    }

    private boolean isAltairOi() {
        return _oe.containsTargets(GmosOiwfsGuideProbe.instance) && _oe.isAltair() &&
                ImOption.apply(_oe.getAltairConfig()).map(InstAltair::getMode).contains(AltairParams.Mode.LGS_OI);
    }


    public String guideName() {
        String guideName;

        // Indicate which one is selected
        if (_oe.containsTargets(AltairAowfsGuider.instance)) {
            guideName = _getAOGuideConfig();
        } else if (isAltairP1()) {
            // REL-542.
            guideName = TccNames.AOP1;
        } else if (isAltairOi()) {
            guideName = TccNames.AOOI;
        } else if (containsGems(_oe)) {
            guideName = _getGeMSGuideConfig();
        } else if (containsOiwfs(_oe)) {
            guideName = _getOIGuideConfig();
        } else {
            guideName = _getPWFSGuideConfig();
        }
        return guideName;
    }

    /**
     * build will use the <code>(@link ObservationEnvironment}</code> to construct
     * an XML document.
     */
    public boolean build() throws WdbaGlueException {
        String guideWith = guideName();
        putParameter(TccNames.GUIDE_WITH, guideWith);

        // Hack in special configuration for GeMS.  Ideally this would be
        // relegated to something in the _oe I suppose.  Some day all of this
        // TCC xml stuff should just go away and die.
        createGemsConfig(guideWith).foreach(this::putParamSet);

        return true;
    }

    private Option<ParamSet> createGemsConfig(String guideWith) throws WdbaGlueException {
        // Only relevant if using GSAOI
        SPInstObsComp inst = _oe.getInstrument();
        if (inst == null) return None.instance();
        if (!Gsaoi.SP_TYPE.equals(inst.getType())) return None.instance();

        // Only relevant if guiding with GeMS and GSAOI
        if (!guideWith.contains(TccNames.GeMS)) return None.instance();
        if (!containsGsaoi(_oe)) return None.instance();

        Gsaoi.OdgwSize size = ((Gsaoi) inst).getOdgwSize();
        ParamSet gems = new ParamSet(TccNames.GeMS);
        ParamSet odgw = new ParamSet("odgw");
        gems.putParamSet(odgw);
        odgw.putParameter("size", size.displayValue());
        return new Some<>(gems);
    }

}



