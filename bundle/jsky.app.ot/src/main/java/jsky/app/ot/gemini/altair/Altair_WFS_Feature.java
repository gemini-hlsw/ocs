package jsky.app.ot.gemini.altair;

import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.gemini.inst.SciAreaFeature;
import jsky.app.ot.gemini.inst.WFS_FeatureBase;
import jsky.app.ot.tpe.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * Draws the Altair WFS annulus.
 * <p>
 * This class is responsible for showing the shape and location of the Altair WFS
 * in the image. The positions and angles are determined by the locations of the
 * AOWFS guide stars and the base position. In addition, if an offset
 * position is selected, it is used in place of the base position to determine the
 * AOWFS position and angle.
 * <p>
 *
 * @author Allan Brighton, modified for Altair by Kim Gillies
 */
public class Altair_WFS_Feature extends WFS_FeatureBase {

    // -- Define constants that define geometry of the Altair AOWFS --

    // colour for AO WFS limit
    private static final Color AOWFS_COLOR = Color.RED;

//     // Used to draw semi-fat lines
    protected static final Stroke AOWFS_STROKE = new BasicStroke(2.0F);

    /** Set to if _reinit should be called to recalculate the figures. */
    private boolean _reinitPending = false;

    // The position angle in radians
    private double _posAngle;

    // True if there is an Altair component
    private boolean _isAltair;

    // Used to get the nod/chop offset
    private SciAreaFeature _sciAreaFeature;


    /**
     * Construct the feature with its name and description.
     */
    public Altair_WFS_Feature() {
        super("AOWFS", "Show the field of view of the AO WFS probes (if any).");
    }

    // Return the TpePWFSFeature, or null if none is defined yet.
    private SciAreaFeature _getSciAreaFeature() {
        TelescopePosEditor tpe = TpeManager.get();
        if (tpe != null) {
            return (SciAreaFeature) tpe.getFeature(SciAreaFeature.class);
        }
        return null;
    }

    private final PropertyChangeListener selListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            _redraw();
        }
    };

    /**
     * Reinitialize (recalculate the positions and redraw).
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        _stopMonitorOffsetSelections(selListener);

        super.reinit(iw, tii);

        // Get a reference to the TPE PWFS feature, used to draw the PWFS at the nod/chop offset
        if (_sciAreaFeature == null) _sciAreaFeature = _getSciAreaFeature();

        SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null) return;

        _posAngle = tii.getCorrectedPosAngleRadians();

        // arrange to be notified if telescope positions are added, removed, or selected
        _monitorPosList();

        // Monitor the selections of offset positions, since that affects the positions drawn
        _monitorOffsetSelections(selListener);

        _figureList.clear();
        _isAltair = _isAltair();
        if (_isAltair) _addAltairFigures(tii);
    }

    /**
     * From SCT-236
     *
     * An ellipse of some sort for the patrol field, which is active when
     * LGS is being used.  The limits are: p = +25,-25 and q = +27,-23.  For
     * NGS, I really don't know the limits that accurately, so using the LGS
     * limits is OK, or if that is difficult to program, it can be a circle
     * with 25 arcsec.
     */
    private void _addAltairFigures(TpeImageInfo tii) {
        // Calculate the field position and size for the AOWFS
        double pixelsPerArcsec = tii.getPixelsPerArcsec();
        Point2D.Double baseScreenPos = tii.getBaseScreenPos();
        TpeContext ctx = _iw.getContext();
        OffsetPosBase selectedOffsetPos = ctx.offsets().selectedPosOrNull();
        double basePosX = baseScreenPos.x, basePosY = baseScreenPos.y;
        if (selectedOffsetPos != null) {
            double offsetX, offsetY;
            // Get offset from base pos in pixels
            // Note that the offset positions rotate with the instrument.
            Point2D.Double p = new Point2D.Double(
                    selectedOffsetPos.getXaxis() * pixelsPerArcsec,
                    selectedOffsetPos.getYaxis() * pixelsPerArcsec);
            Angle.rotatePoint(p, _posAngle);
            offsetX = p.x;
            offsetY = p.y;
            basePosX -= offsetX;
            basePosY -= offsetY;
        }
        Point2D.Double offsetBaseScreenPos = new Point2D.Double(basePosX, basePosY);

        // If there is a context (do we ever have no context?) we need to get the corrected patrol field.
        PatrolField patrolField = AltairAowfsGuider.instance.getPatrolField();
        for (ObsContext obsCtx : _iw.getMinimalObsContext()) {
            for (PatrolField corrected : AltairAowfsGuider.instance.getCorrectedPatrolField(obsCtx)) {
                patrolField = corrected;
            }
        }
        edu.gemini.skycalc.Angle angle = new edu.gemini.skycalc.Angle(-_posAngle, edu.gemini.skycalc.Angle.Unit.RADIANS);
        setTransformationToScreen(angle, pixelsPerArcsec, offsetBaseScreenPos);
        addPatrolField(patrolField, AOWFS_COLOR, AOWFS_STROKE);

        // draw offset constrained patrol field
        setTransformationToScreen(angle, pixelsPerArcsec, baseScreenPos);
        addOffsetConstrainedPatrolField(patrolField, ctx.offsets().scienceOffsetsJava());
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosLocationUpdate(WatchablePos tp) {
        _redraw();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosGenericUpdate(WatchablePos tp) {
        _redraw();
    }

    protected void handleTargetEnvironmentUpdate(TargetEnvironmentDiff diff) {
        _redraw();
    }

    /**
     * Schedule a redraw of the image feature.
     */
    private void _redraw() {
        _reinitPending = true;
        _iw.repaint();
    }

    // Return true if the observation contains an Altair component
    private boolean _isAltair() {
        return _iw.getContext().altair().isDefined();
    }

    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        double posAngle = tii.getCorrectedPosAngleRadians();
        boolean isAltair = _isAltair();

        if (_reinitPending || posAngle != _posAngle || isAltair != _isAltair) {
            _reinitPending = false;
            reinit();
        }
        Graphics2D g2d = (Graphics2D) g;
        drawFigures(g2d, false);
    }

    @Override
    public boolean isEnabled(TpeContext ctx) {
        if (!super.isEnabled(ctx)) return false;
        return ctx.altair().isDefined();
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }
}

