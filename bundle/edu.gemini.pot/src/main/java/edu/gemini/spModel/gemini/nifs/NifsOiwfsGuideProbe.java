package edu.gemini.spModel.gemini.nifs;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SingleBand;
import edu.gemini.spModel.gemini.altair.InstAltair;
import static edu.gemini.spModel.gemini.altair.AltairParams.FieldLens.IN;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

/**
 * NIFS on-instrument guide probe.
 */
public enum NifsOiwfsGuideProbe implements ValidatableGuideProbe, OffsetValidatingGuideProbe {
    instance;

    private static final PatrolField noFieldLensPatrolField = PatrolField.fromRadiusLimits(Angle.arcmins(0.22), Angle.arcmins(1.0));
    private static final PatrolField fieldLensPatrolField   = PatrolField.fromRadiusLimits(Angle.arcmins(0.22), Angle.arcmins(0.5));

    public String getKey() {
        return "NIFS OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "NIFS On-instrument WFS";
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

    @Override
    public PatrolField getPatrolField() {
        return noFieldLensPatrolField;
    }

    @Override
    public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        if (ctx.getInstrument() instanceof InstNIFS) {
            return ctx.getAOComponent().flatMap(ado -> {
                if (ado instanceof InstAltair) {
                    final InstAltair altair = (InstAltair) ado;
                    return new Some<>((altair.getFieldLens() == IN) ? fieldLensPatrolField : noFieldLensPatrolField);
                } else {
                    return None.instance();
                }
            }).orElse(new Some<>(noFieldLensPatrolField));
        } else {
            return None.instance();
        }
    }

    @Override
    public boolean inRange(ObsContext ctx, Offset offset) {
        return GuideProbeUtil.instance.inRange(this, ctx, offset);
    }

    @Override
    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    @Override
    public BandsList getBands() { return SingleBand.apply(MagnitudeBand.K$.MODULE$); }
}
