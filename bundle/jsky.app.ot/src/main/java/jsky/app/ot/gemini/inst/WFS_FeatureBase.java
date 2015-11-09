package jsky.app.ot.gemini.inst;

import edu.gemini.catalog.api.RadiusConstraint;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.guide.PatrolField;
import jsky.app.ot.tpe.TpeImageFeature;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Collection;

/**
 * Base class for all WFS Feature classes (PWFS and OIWFS).
 */
public abstract class WFS_FeatureBase extends TpeImageFeature {

    // The color to use to draw the patrol field
    private static final Color PATROL_FIELD_COLOR = Color.green;
    private static final Color OFFSET_PATROL_FIELD_COLOR = Color.red;

    // Used to draw dashed lines
    private static final Stroke DOTTED_LINE_STROKE
            = new BasicStroke(0.5F,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1F,
            new float[]{2f},
            1F);
    private static final Stroke DASHED_LINE_STROKE
            = new BasicStroke(0.5F,
                              BasicStroke.CAP_BUTT,
                              BasicStroke.JOIN_BEVEL,
                              0.0F,
                              new float[]{12.0F, 12.0F},
                              0.0F);
    private static final Stroke THICK_DASHED_LINE_STROKE
               = new BasicStroke(2.0F,
                                 BasicStroke.CAP_BUTT,
                                 BasicStroke.JOIN_BEVEL,
                                 0.0F,
                                 new float[]{12.0F, 12.0F},
                                 0.0F);

    /**
     * Construct the feature with its name and description.
     */
    public WFS_FeatureBase(String name, String desc) {
        super(name, desc);
    }


    public void addPatrolField(PatrolField patrolField) {
        addPatrolField(patrolField, OFFSET_PATROL_FIELD_COLOR);
    }

    public void addPatrolField(PatrolField patrolField, Color color) {
        addPatrolField(patrolField, color, THICK_DASHED_LINE_STROKE, null);
    }

    public void addPatrolField(PatrolField patrolField, Color color, Stroke stroke) {
        addPatrolField(patrolField, color, stroke, null);
    }

    public void addPatrolField(PatrolField patrolField, Color color, Stroke stroke, Composite composite) {
        Area patrolFieldArea = transformToScreen(patrolField.getArea());
        addFigure(patrolFieldArea, color, null, stroke);
        if (!patrolField.getBlockedArea().isEmpty()) {
            Area blockedArea = transformToScreen(patrolField.getBlockedArea());
            addFigure(blockedArea, color, composite, stroke);
        }
    }

    public void addRadiusLimits(RadiusConstraint radiusLimits, Color color) {
        Double minLimit = radiusLimits.minLimit().toArcsecs();
        Double maxLimit = radiusLimits.maxLimit().toArcsecs();
        addRadiusLimit(minLimit, color, DOTTED_LINE_STROKE);
        addRadiusLimit(maxLimit, color, THICK_DASHED_LINE_STROKE);
    }

    private void addRadiusLimit(Double limit, Color color, Stroke stroke) {
        Area radiusLimit = new Area(new Ellipse2D.Double(-limit, -limit, limit*2, limit*2));
        transformToScreen(radiusLimit);
        addFigure(radiusLimit, color, null, stroke);
    }

    public void addOffsetConstrainedPatrolField(PatrolField patrolField, Collection<Offset> offsets) {
        Area safe = patrolField.safeOffsetIntersection(offsets);
        Area area = patrolField.offsetIntersection(offsets);
        Area outer = patrolField.outerLimitOffsetIntersection(offsets);
        transformToScreen(safe);
        transformToScreen(area);
        transformToScreen(outer);
        outer.subtract(safe);
        addFigure(area, PATROL_FIELD_COLOR, null, DASHED_LINE_STROKE);
        addFigure(outer, PATROL_FIELD_COLOR, AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25F), new BasicStroke(0.0f));
    }
}


