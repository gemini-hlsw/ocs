package edu.gemini.spModel.target.obsComp;

import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.Set;

/**
 * The Peripheral guide probes.
 */
public enum PwfsGuideProbe implements ValidatableGuideProbe, OffsetValidatingGuideProbe {

    pwfs1(1) {
        @Override public double getScale() {
           return getPwfs1Scale();
        }

        @Override public Angle getVignettingClearance(ObsContext ctx) {
            final SPInstObsComp oc = ctx.getInstrument();
            return (oc == null) ? Angle.ANGLE_0DEGREES : oc.pwfs1VignettingClearance();
        }
    },

    pwfs2(2) {
        @Override public double getScale() {
            return getPwfs2Scale();
        }

        @Override public Angle getVignettingClearance(ObsContext ctx) {
            final SPInstObsComp oc = ctx.getInstrument();
            return (oc == null) ? Angle.ANGLE_0DEGREES : oc.pwfs2VignettingClearance();
        }
    };

    // The radius of the outer limit for PWFS 417" or 6.95' (valid for all instruments).
    private static final int PWFS_RADIUS_ARCSEC = 417;
    public static final Angle PWFS_RADIUS = Angle.arcsecs(PWFS_RADIUS_ARCSEC);

    /* Geometry */
    // -- Define constants that define geometry of the two PWFS --

    // Used for conversion between mm and pixel
    private static final double MM_FACTOR = 1.611;

    // Distance from M2 to focal plane
    private static final double M2_FPLANE_SEP = 16539.326;

    // Radius of M2 (mm)
    private static final double M2_RADIUS = 511.0;

    // Pivot to mirror centre (mm)
    private static final double ARM_LENGTH = 400.0;

    // Distance of pwfs1 from focal plane
    private static final double PWFS1_FPLANE_SEP = 1995.0;

    // Distance of pwfs2 from focal plane
    private static final double PWFS2_FPLANE_SEP = 1610.0;

    // Width of pickoff mirror (mm)
    private static final double PWFS1_M_WIDTH = 157.0;

    // Length of pickoff mirror (mm)
    private static final double PWFS1_M_LENGTH = 157.0;

    // Width of pickoff mirror (mm)
    private static final double PWFS2_M_WIDTH = 127.0;

    // Length of pickoff mirror (mm)
    private static final double PWFS2_M_LENGTH = 127.0;

    // Width of probe arm (mm)
    private static final double PWFS1_A_WIDTH = 202.0;

    // Width of probe arm (mm)
    private static final double PWFS2_A_WIDTH = 202.0;

    // Scale to convert mm in focal plane to mm at height of pwfs1
    private static final double PWFS1_SCALE = (getM2FplaneSep() - getPwfs1FplaneSep()) / getM2FplaneSep();

    // Scale to convert mm in focal plane to mm at height of pwfs2
    private static final double PWFS2_SCALE = (getM2FplaneSep() - getPwfs2FplaneSep()) / getM2FplaneSep();

    private final int index;


    private final PatrolField patrolField;

    public abstract double getScale();
    public abstract Angle getVignettingClearance(ObsContext ctx);

    private final double planeSep;
    private final double mWidth;

    public double getMWidth() {
        return mWidth;
    }

    private final double mLength;

    public double getMLength() {
        return  mLength;
    }

    private final double aWidth;

    public double getAWidth() {
        return aWidth;
    }

    private final int radius;

    public int getRadius() {
       return radius;
    }

    public double getFPlaneSep() {
        return planeSep;
    }

    public static double getArmLength() {
        return ARM_LENGTH;
    }

    public static double getM2FplaneSep() {
        return M2_FPLANE_SEP;
    }

    public static double getM2Radius() {
        return M2_RADIUS;
    }

    /**
     * Creates a polygon The polygon has four corners in the correct orientation
     * and projection in the focal plane
     *
     * @param pwfsScale scale for PWFS1 or 2
     * @param xc        the X screen coordinate for the base position
     * @param yc        the Y screen coordinate for the base position
     * @param xp        X point of pivot
     * @param yp        Y point of pivot
     * @param dx        WFS X position relative to field centre with allowance for height of wfs above focal plane
     * @param dy        WFS Y position relative to field centre with allowance for height of wfs above focal plane
     * @param xyList    array of points in partially vignetted region
     * @param xySize    length of xyList array to use
     * @param tx        translate resulting figure by this amount of pixels in X (used when WFS is frozen)
     * @param ty        translate resulting figure by this amount of pixels in Y (used when WFS is frozen)
     */
    public static Shape buildFigure(double pwfsScale, double xc, double yc, double xp, double yp, double dx, double dy, double[] xyList, int xySize, double tx, double ty) {
        // Rotate each x,y pair to a frame aligned along the axis of the probe
        double dxp = dx + xc - xp;
        double dyp = dy + yc - yp;
        double rot = Math.atan2(-dyp, dxp);

        double cosrot = Math.cos(rot);
        double sinrot = Math.sin(rot);

        GeneralPath path = new GeneralPath();
        int numPoints = xySize / 2;
        double firstX = 0.0;
        double firstY = 0.0;
        for (int i = 0; i < numPoints; i++) {
            int xIndex = 2 * i;
            int yIndex = xIndex + 1;

            // Rotate these points to the correct orientation
            double xnp = (xyList[xIndex] * cosrot + xyList[yIndex] * sinrot) + xp;
            double ynp = (xyList[xIndex] * -sinrot + xyList[yIndex] * cosrot) + yp;

            // project points into focal plane
            final double x = (xnp - xc) / pwfsScale + xc + tx;
            final double y = (ynp - yc) / pwfsScale + yc + ty;

            if (i == 0) {
                path.moveTo(x, y);
                firstX = x;
                firstY = y;
            } else {
                path.lineTo(x, y);
            }
        }
        //Close the path
        path.lineTo(firstX, firstY);

        return path;
    }

   public static double getMmFactor() {
        return MM_FACTOR;
    }

    PwfsGuideProbe(int index) {
        this.index = index;

        radius = PWFS_RADIUS_ARCSEC;
        patrolField = new PatrolField(
                new Ellipse2D.Double(-radius, -radius, radius * 2.0, radius * 2.0),                           // fov
                new Ellipse2D.Double(-radius + 2.0, -radius + 2.0, radius * 2.0 - 4.0, radius * 2.0 - 4.0),   // fov in  (-2 arcsecs)
                new Ellipse2D.Double(-radius - 2.0, -radius - 2.0, radius * 2.0 + 4.0, radius * 2.0 + 4.0)    // fov out (+2 arcsecs)
        );

        if (index == 1) {
            planeSep = getPwfs1FplaneSep();
            mWidth   = PWFS1_M_WIDTH;
            mLength  = PWFS1_M_LENGTH;
            aWidth   = PWFS1_A_WIDTH;
        } else {
            planeSep = getPwfs2FplaneSep();
            mWidth   = PWFS2_M_WIDTH;
            mLength  = PWFS2_M_LENGTH;
            aWidth   = PWFS2_A_WIDTH;
        }
    }

    public static double getPwfs1Scale() {
        return PWFS1_SCALE;
    }

    public static double getPwfs1FplaneSep() {
        return PWFS1_FPLANE_SEP;
    }

    public static double getPwfs2Scale() {
        return PWFS2_SCALE;
    }

    public static double getPwfs2FplaneSep() {
        return PWFS2_FPLANE_SEP;
    }

    public String getKey() {
        return "PWFS" + index;
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.PWFS;
    }

    public String getDisplayName() {
        return "Peripheral WFS " + index;
    }

    public String getSequenceProp() {
        return "guideWithPWFS" + index;
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return None.instance();
    }

    /* ValidatableGuideProbe */

    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        final Option<Long> when = ctx.getSchedulingBlockStart();
        return guideStar.getSkycalcCoordinates(when).map(coords ->
            validate(coords, ctx)
        ).getOrElse(GuideStarValidation.UNDEFINED);
    }

    public GuideStarValidation validate(Coordinates coords, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(coords, this, ctx);
    }

    public Option<PwfsProbeRangeArea> checkBoundaries(SPTarget guideStar, ObsContext ctx){
        return guideStar
            .getSkycalcCoordinates(ctx.getSchedulingBlockStart())
            .flatMap(cs -> checkBoundaries(cs, ctx));
    }

    /**
     * Returns:
     *
     * - BoundaryCheck.outOfRange if outside the limit.
     *
     * - BoundaryCheck.vignetting if it obscures the science area.
     *
     * - BoundaryCheck.inRange if inside the limit.
     *
     * @param coords the coordinates
     * @param ctx the context
     */
    public Option<PwfsProbeRangeArea> checkBoundaries(final Coordinates coords, final ObsContext ctx) {
        return ctx.getBaseCoordinates().map(baseCoordinates -> {
            final Angle positionAngle = ctx.getPositionAngleJava();
            final Set<Offset> sciencePositions = ctx.getSciencePositions();

            // check positions against corrected outer patrol field bounds
            return getCorrectedPatrolField(ctx).map(pf -> {
                final BoundaryPosition bp = pf.checkBoundaries(coords, baseCoordinates, positionAngle, sciencePositions);
                final PwfsProbeRangeArea result;
                switch (bp) {
                    case inside:
                        result = PwfsProbeRangeArea.inRange;
                        break;
                    case innerBoundary:
                        // Check if any of the guide stars are inside the inner bounds (opposite logic needed, union instead of intersection)
                        final double minLimit = getVignettingClearance(ctx).toArcsecs().getMagnitude();
                        final Ellipse2D e = new Ellipse2D.Double(-minLimit, -minLimit, minLimit * 2.0, minLimit * 2.0);
                        final PatrolField p = getCorrectedPatrolField(new PatrolField(e, e, e), ctx);
                        if (p.anyInside(coords, baseCoordinates, positionAngle, sciencePositions)) {
                            result = PwfsProbeRangeArea.vignetting;
                        } else {
                            result = PwfsProbeRangeArea.inRange;
                        }
                        break;
                    // outside and outerBoundary
                    default:
                        result = PwfsProbeRangeArea.outOfRange;
                        break;
                }
                return result;
            }).getOrElse(PwfsProbeRangeArea.outOfRange);
        });
    }


    /* OffsetValidatingGuideProbe */

    public boolean inRange(ObsContext ctx, Offset offset) {
        return GuideProbeUtil.instance.inRange(this, ctx, offset);
    }

    @Override
    public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        return GuideProbeUtil.instance.isAvailable(ctx, this) ? new Some<>(getCorrectedPatrolField(getPatrolField(), ctx)) : None.instance();
    }

    public PatrolField getCorrectedPatrolField(PatrolField patrolField, ObsContext ctx) {
        // special case for GMOS: take IFU offest into account for patrol field
        final SPInstObsComp instrument = ctx.getInstrument();
        if (InstGmosCommon.class.isAssignableFrom(instrument.getClass())) {
            final double wfsOffset = ((GmosCommonType.FPUnit) ((InstGmosCommon) instrument).getFPUnit()).getWFSOffset();
            AffineTransform correction = AffineTransform.getTranslateInstance(wfsOffset, 0.0);
            return patrolField.getTransformed(correction);

        // TODO: if needed add special corrections (translation) for nod and chop mode for Michelle and TReCS
        // see: TReCS_SciAreaFeature and Michelle_SciAreaFeature : getNodChopOffset()

        } else {
            return patrolField;
        }
    }

    @Override
    public PatrolField getPatrolField() {
        return patrolField;
    }
}
