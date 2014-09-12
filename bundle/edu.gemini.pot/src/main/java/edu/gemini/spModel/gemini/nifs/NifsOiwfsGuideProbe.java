//
// $
//

package edu.gemini.spModel.gemini.nifs;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.spModel.data.AbstractDataObject;
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
    public PatrolField getCorrectedPatrolField(ObsContext ctx) {
        return ctx.getAOComponent().filter(new PredicateOp<AbstractDataObject>() {
            @Override public Boolean apply(AbstractDataObject dobj) {
                return (dobj instanceof InstAltair);
            }
        }).map(new MapOp<AbstractDataObject, PatrolField>() {
            @Override public PatrolField apply(AbstractDataObject dobj) {
                InstAltair altair = (InstAltair) dobj;
                return (altair.getFieldLens() == IN) ? fieldLensPatrolField : noFieldLensPatrolField;
            }
        }).getOrElse(noFieldLensPatrolField);
    }

    @Override
    public boolean inRange(ObsContext ctx, Offset offset) {
        return GuideProbeUtil.instance.inRange(this, ctx, offset);
    }

    @Override
    public boolean validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar.getSkycalcCoordinates(), getCorrectedPatrolField(ctx), ctx);
    }
}