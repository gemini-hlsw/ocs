package edu.gemini.spModel.gemini.gpi;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.NoBands;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;

import java.awt.geom.Area;

/**
 * The GPI OIWFS guider (not actually used, see GPI.getConsumedGuideProbes()).
 */
public enum GpiOiwfsGuideProbe implements GuideProbe {
    instance;

    public String getKey() {
        return "GPI OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "GPI OIWFS";
    }

    public String getSequenceProp() {
        return "guideWithOIWFS";
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return None.instance();
    }

    // GPI does not have an OIWFS patrol field, return empty area
    final private static PatrolField patrolField = new PatrolField(new Area());
    @Override public PatrolField getPatrolField() {
        return patrolField;
    }

    @Override public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        if (ctx.getInstrument() instanceof Gpi) {
            return new Some<>(patrolField);
        } else {
            return None.instance();
        }
    }

    @Override
    public BandsList getBands() { return NoBands.instance(); }
}
