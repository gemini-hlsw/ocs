package edu.gemini.wdba.tcc;

import edu.gemini.spModel.ext.SequenceNode;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.wdba.glue.api.WdbaGlueException;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @deprecated
 */
@Deprecated
public class UnusedGuideConfig extends ParamSet {
    private static final Logger LOG = Logger.getLogger(UnusedGuideConfig.class.getName());

    private ObservationEnvironment _oe;
    private String _name;

    public UnusedGuideConfig(ObservationEnvironment oe) {
        super("");
        if (oe == null) throw new NullPointerException("Config requires a non-null observation environment");
        _oe = oe;
        _name = _oe.getBasePositionName();
    }

    /**
     * The name to be used in the "value" of the field rotator param
     * @return String that is name
     */
    String getConfigName() {
        return _name;
    }

    /**
     * Build will use the <code>(@link ObservationEnvironment}</code> to
     * construct an XML document.
     */
    public boolean build() throws WdbaGlueException {

        // The guide config is named after the base position
        addAttribute(NAME, _name);
        addAttribute(TYPE, TccNames.GUIDE);

        // SW: These never seem to change, adding them here to get it out of
        // the way.  TODO: are these parameters even necessary?
        putParameter(TccNames.OIWFSNODSTATE, TccNames.AB);
        putParameter(TccNames.PWFS1NODSTATE, TccNames.AB);
        putParameter(TccNames.PWFS2NODSTATE, TccNames.AB);

        List<SequenceNode> offsetNodes = _oe.getOffsetNodes();

        // First handle the case in which there are no offset positions in the
        // sequence.
        if (offsetNodes.size() == 0) {
            putParameter(TccNames.OIWFSACTIVE,  TccNames.NEVER);
            if (_oe.containsTargets(GuideProbe.Type.OIWFS)) {
                _oe.getInstrumentSupport().addGuideDetails(this);
            }
            putParameter(TccNames.PWFS1ACTIVE, TccNames.NEVER);
            putParameter(TccNames.PWFS2ACTIVE, TccNames.NEVER);
            return true;
        }

        // Now, handle the case where there are one or more offset iterators
        // in the sequence.
        Set<GuideProbe> refGuiders;
        refGuiders = _oe.getTargetEnvironment().getOrCreatePrimaryGuideGroup().getReferencedGuiders();

        if (refGuiders.contains(PwfsGuideProbe.pwfs1) && probeUsed(PwfsGuideProbe.pwfs1, offsetNodes)) {
            putParameter(TccNames.PWFS1ACTIVE, TccNames.A);
        } else {
            putParameter(TccNames.PWFS1ACTIVE, TccNames.NEVER);
        }
        if (refGuiders.contains(PwfsGuideProbe.pwfs2) && probeUsed(PwfsGuideProbe.pwfs2, offsetNodes)) {
            putParameter(TccNames.PWFS2ACTIVE, TccNames.A);
        } else {
            putParameter(TccNames.PWFS2ACTIVE, TccNames.NEVER);
        }

        GuideProbe oi = getOiwfsGuider(refGuiders);
        if ((oi != null) && probeUsed(oi, offsetNodes)) {
            putParameter(TccNames.OIWFSACTIVE, TccNames.A);
            _oe.getInstrumentSupport().addGuideDetails(this);
        } else {
            putParameter(TccNames.OIWFSACTIVE, TccNames.NEVER);
        }

        return true;
    }

    private static GuideProbe getOiwfsGuider(Set<GuideProbe> guiders) {
        for (GuideProbe guider : guiders) {
            if (guider.getType() == GuideProbe.Type.OIWFS) return guider;
        }

        return null;
    }

    private static boolean probeUsed(GuideProbe probe, Collection<SequenceNode> offsetNodes) {
        for (SequenceNode offsetNode : offsetNodes) {
            SeqRepeatOffsetBase<OffsetPosBase> dataObj;
            //noinspection unchecked
            dataObj = (SeqRepeatOffsetBase<OffsetPosBase>) offsetNode.getDataObject();

            OffsetPosList<OffsetPosBase> posList = dataObj.getPosList();
            for (OffsetPosBase pos : posList) {
                GuideOption opt = pos.getLink(probe);
                if ((opt == null) || (opt == probe.getGuideOptions().getDefaultActive())) return true;
            }
        }
        return false;
    }
}
