package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.*;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 *  Class to evaluate the {@link ObservationEnvironment} and produce a guide config and guide config  name.
 */
public final class GuideConfig extends ParamSet {
    private static final Set<GuideProbe> ODGW_PROBES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(GsaoiOdgw.values()))
    );

    private static final Set<GuideProbe> CWFS_PROBES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(CanopusWfs.values()))
    );

    private final ObservationEnvironment _oe;

    public GuideConfig(ObservationEnvironment oe) {
        super(TccNames.GUIDE_CONFIG);
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        _oe = oe;
    }

    private boolean contains(GuideProbe gp) {
        return _oe.containsTargets(gp);
    }

    private boolean containsOiwfs() {
        // Hack here because the TCC, TCS, Seqexec don't consider the GSAOI
        // ODGW to be "on instrument".  Explicitly check for these probes.
        final Set<GuideProbe> gs = _oe.usedGuiders();
        gs.removeAll(ODGW_PROBES);
        return gs.stream().anyMatch(gp -> gp.getType() == GuideProbe.Type.OIWFS);
    }

    private boolean containsOneOf(Set<GuideProbe> probes) {
        final Set<GuideProbe> gs = _oe.usedGuiders();
        gs.retainAll(probes);
        return !gs.isEmpty();
    }

    private boolean containsGsaoi() {
        return containsOneOf(ODGW_PROBES);
    }

    private boolean containsGems() {
        return containsOneOf(CWFS_PROBES);
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
        if (containsOiwfs()) return TccNames.AOOI;
        if (contains(pwfs1)) return TccNames.AOP1;
        if (contains(pwfs2)) return TccNames.AOP2;
        return TccNames.AO;
    }

    private String _getGeMSGuideConfig() {
        if (containsOiwfs()) {
            return contains(pwfs1) ? TccNames.GeMSP1OI : TccNames.GeMSOI;
        }
        return contains(pwfs1) ? TccNames.GeMSP1 : TccNames.GeMS;
    }

    private String _getOIGuideConfig() {
        if (contains(pwfs1)) return TccNames.P1OI;
        if (contains(pwfs2)) return TccNames.P2OI;
        return TccNames.OI;
    }

    private String _getPWFSGuideConfig() {
        if (contains(pwfs1)) {
            return contains(pwfs2) ? TccNames.P1P2 : TccNames.P1;
        }
        // If we get here, then P1 isn't set so check for P2 if not P2 return null
        return contains(pwfs2) ? TccNames.P2 : TccNames.NO_GUIDING;
    }

    private boolean isAltairMode(AltairParams.Mode m) {
        return ImOption.apply(_oe.getAltairConfig()).map(InstAltair::getMode).contains(m);
    }

    private boolean isAltairP1() {
        return contains(pwfs1) && isAltairMode(AltairParams.Mode.LGS_P1);
    }

    private boolean isAltairOi() {
        return contains(GmosOiwfsGuideProbe.instance) && isAltairMode(AltairParams.Mode.LGS_OI);
    }

    public String guideName() {
        final String guideName;

        // Indicate which one is selected
        if (contains(AltairAowfsGuider.instance)) {
            guideName = _getAOGuideConfig();
        } else if (isAltairP1()) {
            // REL-542.
            guideName = TccNames.AOP1;
        } else if (isAltairOi()) {
            guideName = TccNames.AOOI;
        } else if (containsGems()) {
            guideName = _getGeMSGuideConfig();
        } else if (containsOiwfs()) {
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
        final String guideWith = guideName();
        putParameter(TccNames.GUIDE_WITH, guideWith);

        // Hack in special configuration for GeMS.  Ideally this would be
        // relegated to something in the _oe I suppose.  Some day all of this
        // TCC xml stuff should just go away and die.
        createGemsConfig(guideWith).foreach(this::putParamSet);

        return true;
    }

    private Option<ParamSet> createGemsConfig(String guideWith) throws WdbaGlueException {
        // Only relevant if using GSAOI
        final SPInstObsComp inst = _oe.getInstrument();
        if (inst == null) return None.instance();
        if (!Gsaoi.SP_TYPE.equals(inst.getType())) return None.instance();

        // Only relevant if guiding with GeMS and GSAOI
        if (!guideWith.contains(TccNames.GeMS)) return None.instance();
        if (!containsGsaoi()) return None.instance();

        final Gsaoi.OdgwSize size = ((Gsaoi) inst).getOdgwSize();
        final ParamSet gems = new ParamSet(TccNames.GeMS);
        final ParamSet odgw = new ParamSet("odgw");
        gems.putParamSet(odgw);
        odgw.putParameter("size", size.displayValue());
        return new Some<>(gems);
    }

}



