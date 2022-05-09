package jsky.app.ot.tpe.feat;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.gemini.ghost.GhostAsterism;
import edu.gemini.spModel.gemini.ghost.GhostIfuPatrolField;
import edu.gemini.spModel.gemini.ghost.GhostIfuPatrolField$;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPCoordinates;
import edu.gemini.spModel.target.SPSkyObject;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import jsky.app.ot.tpe.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.time.Instant;

public class TpeAsterismFeature extends TpePositionFeature {

    public TpeAsterismFeature() {
        super("Asterism", "Show the science target asterism.");
    }

    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        // Tell the position map that the base position is visible.
        TpePositionMap pm = TpePositionMap.getMap(iw);
        pm.setFindAsterism(true);
    }

    public void unloaded() {
        // Tell the position map that the base position is no longer visible.
        TpePositionMap pm = TpePositionMap.getExistingMap();
        if (pm != null) pm.setFindAsterism(false);

        super.unloaded();
    }

    public boolean erase(final TpeMouseEvent tme) {
        // You can't erase the base position
        return false;
    }

    /**
     * @see jsky.app.ot.tpe.TpeSelectableFeature
     */
    public Object select(final TpeMouseEvent tme) {
        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        final int x = tme.xWidget;
        final int y = tme.yWidget;

        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp != null) {
            for (final SPTarget spt: obsComp.getAsterism().allSpTargetsJava()) {
              final PosMapEntry<SPSkyObject> pme = pm.getPositionMapEntry(spt);
              if ((pme != null) && (positionIsClose(pme, x, y)) && getContext().targets().shell().isDefined()) {
                  final TargetEnvironment env = getContext().targets().envOrNull();
                  final ISPObsComponent ispObsComponent = getContext().targets().shell().get();
                  if (pme.taggedPos instanceof SPTarget)
                    TargetSelection.setTargetForNode(env, ispObsComponent, (SPTarget) pme.taggedPos);
                  return pme.taggedPos;
              }
          }
          for (final SPCoordinates spc: obsComp.getAsterism().allSpCoordinatesJava()) {
              final PosMapEntry<SPSkyObject> pme = pm.getPositionMapEntry(spc);
              if ((pme != null) && (positionIsClose(pme, x, y)) && getContext().targets().shell().isDefined()) {
                  final TargetEnvironment env = getContext().targets().envOrNull();
                  final ISPObsComponent ispObsComponent = getContext().targets().shell().get();
                  if (pme.taggedPos instanceof SPCoordinates)
                      TargetSelection.setCoordinatesForNode(env, ispObsComponent, (SPCoordinates) pme.taggedPos);
                  return pme.taggedPos;
              }
          }
        }
        return null;
    }

    private void markPosition(final Graphics g, final Color color, final Point2D.Double loc) {
        ImOption.apply(loc).foreach(p -> {
            final int r = MARKER_SIZE;
            final int d = 2 * r;
            g.setColor(color);
            g.drawOval((int) (p.x - r), (int) (p.y - r), d, d);
            g.drawLine((int) p.x, (int) (p.y - r), (int) p.x, (int) (p.y + r));
            g.drawLine((int) (p.x - r), (int) p.y, (int) (p.x + r), (int) p.y);
        });
    }

    private void markBase(final Graphics g, final Point2D.Double loc) {
        ImOption.apply(loc).foreach(p -> {
            final int r = MARKER_SIZE;
            final int d = 2 * r;
            g.setColor(Color.cyan);
            g.drawOval((int) (p.x - r), (int) (p.y - r), d, d);
            g.fillOval((int) (p.x - r/2), (int) (p.y - r/2), r+1, r+1);
        });
    }

    private Option<Coordinates> baseCoordinates(ObsContext ctx) {
        return ctx.getBaseCoordinates().flatMap(edu.gemini.skycalc.Coordinates::toCoreCoordinates);
    }

    private Option<Point2D.Double> explicitBaseLocation(
        scala.Option<SPCoordinates> base,
        TpePositionMap pm
    ) {
        return ImOption.fromScalaOpt(base).map(pm::getLocationFromTag);
    }

    /**
     *  Position information extracted from a Ghost asterism used to facilitate
     *  drawing.
     */
    static final class GhostPosition {

        // Coordinates where the IFU should be placed.
        private final Coordinates    ifuPosition;

        // Screen location where to draw the feature associated with the IFU.
        // This might not correspond 1:1 to where the IFU is located.  HRIFU
        // sky fibers are about 3.28 arcsec above the SRIFU2.
        private final Point2D.Double drawingLocation;

        // Is this a target (or a sky position).
        private final boolean        isTarget;

        public GhostPosition(
            Coordinates ifuPosition,
            Point2D     drawingLocation,
            boolean     isTarget
        ) {
            this.ifuPosition     = ifuPosition;
            this.drawingLocation = new Point2D.Double(drawingLocation.getX(), drawingLocation.getY());
            this.isTarget        = isTarget;
        }

        public static Option<GhostPosition> fromTarget(
            GhostAsterism.GhostTarget target,
            Option<Instant> when,
            TpePositionMap  pm
        ) {
            return ImOption.fromScalaOpt(target.coordinates(when.toScalaOpt())).map(c ->
                new GhostPosition(
                    c,
                    pm.getLocationFromTag(target.spTarget()),
                    true
                )
            );
        }

        public static GhostPosition fromSky(
            Coordinates    ifuPosition,
            SPCoordinates  skyPosition,
            TpePositionMap pm
        ) {
            return new GhostPosition(
                ifuPosition,
                pm.getLocationFromTag(skyPosition),
                false
            );
        }
    }

    private void drawGhostIfuPosition(
        final Graphics    g,
        final Coordinates pos
    ) {
        final SPCoordinates spC = new SPCoordinates(pos);
        final Point2D.Double  p = _iw.taggedPosToScreenCoords(spC);
        markPosition(g, Color.magenta, p);
    }

    private void drawGhostIfu(
      final Graphics            g,
      final GhostPosition       pos,
      final Coordinates         base,
      final GhostIfuPatrolField patrolField
    ) {
        final Color color =
            patrolField.inRange(Coordinates.difference(base, pos.ifuPosition).offset()) ?
                    (pos.isTarget ? Color.magenta : Color.cyan)                          :
                    Color.red;

        // For debugging, it can be useful to turn on IFU position.  For HR
        // the IFU position is SRIFU2 and the drawing location is the sky
        // position itself.  Normally we don't draw the IFU position though in
        // this case.
        //drawGhostIfuPosition(g, pos.ifuPosition);

        markPosition(g, color, pos.drawingLocation);
    }

    private void drawGhostAsterism(
      final Graphics              g,
      final Option<ObsContext>    ctx,
      final Option<GhostPosition> ifu1,
      final Option<GhostPosition> ifu2,
      Option<Point2D.Double>      explicitBase
    ) {
        explicitBase.foreach(p -> markBase(g, p));
        ctx.foreach(c ->
            baseCoordinates(c).foreach(base -> {
                ifu1.foreach(ifu -> drawGhostIfu(g, ifu, base, GhostIfuPatrolField$.MODULE$.ifu1(c)));
                ifu2.foreach(ifu -> drawGhostIfu(g, ifu, base, GhostIfuPatrolField$.MODULE$.ifu2(c)));
            })
        );
    }

    public void draw(final Graphics g, final TpeImageInfo tii) {

        final TargetEnvironment env = getTargetEnvironment();
        if (env == null) return;

        final TpePositionMap     pm  = TpePositionMap.getMap(_iw);
        final Option<ObsContext> ctx = getContext().obsContextJavaWithConditions(SPSiteQuality.Conditions.NOMINAL);
        final Option<Instant>   when = ctx.flatMap(ObsContext::getSchedulingBlockStart).map(Instant::ofEpochMilli);

        final Asterism a = env.getAsterism();
        switch (a.asterismType()) {
            case GhostSingleTarget:
                final GhostAsterism.SingleTarget st = ((GhostAsterism.SingleTarget) a);

                drawGhostAsterism(
                    g,
                    ctx,
                    GhostPosition.fromTarget(st.target(), when, pm),
                    ImOption.empty(),
                    explicitBaseLocation(st.overriddenBase(), pm)
                );
                break;

            case GhostDualTarget:
                final GhostAsterism.DualTarget dt = ((GhostAsterism.DualTarget) a);
                drawGhostAsterism(
                    g,
                    ctx,
                    GhostPosition.fromTarget(dt.target1(), when, pm),
                    GhostPosition.fromTarget(dt.target2(), when, pm),
                    explicitBaseLocation(dt.overriddenBase(), pm)
                );

                // Draw the base position if it is not explicitly overridden
                if (dt.overriddenBase().isEmpty()) {
                    ImOption.fromScalaOpt(dt.basePosition(ImOption.scalaNone())).foreach(c -> {
                        final SPCoordinates spC = new SPCoordinates(c);
                        final Point2D.Double p = _iw.taggedPosToScreenCoords(spC);
                        markBase(g, p);
                    });
                }
                break;

            case GhostTargetPlusSky:
                final GhostAsterism.TargetPlusSky tps = ((GhostAsterism.TargetPlusSky) a);
                drawGhostAsterism(
                    g,
                    ctx,
                    GhostPosition.fromTarget(tps.target(), when, pm),
                    ImOption.apply(GhostPosition.fromSky(tps.sky().coordinates(), tps.sky(), pm)),
                    explicitBaseLocation(tps.overriddenBase(), pm)
                );
                break;

            case GhostSkyPlusTarget:
                final GhostAsterism.SkyPlusTarget spt = ((GhostAsterism.SkyPlusTarget) a);
                drawGhostAsterism(
                    g,
                    ctx,
                    ImOption.apply(GhostPosition.fromSky(spt.sky().coordinates(), spt.sky(), pm)),
                    GhostPosition.fromTarget(spt.target(), when, pm),
                    explicitBaseLocation(spt.overriddenBase(), pm)
                );
                break;

            case GhostHighResolutionTargetPlusSky:
                final GhostAsterism.HighResolutionTargetPlusSky hrtps = ((GhostAsterism.HighResolutionTargetPlusSky) a);
                drawGhostAsterism(
                    g,
                    ctx,
                    GhostPosition.fromTarget(hrtps.target(), when, pm),
                    ctx.map(ObsContext::getPositionAngle).map(posAngle ->
                       GhostPosition.fromSky(hrtps.srifu2(posAngle), hrtps.sky(), pm)
                    ),
                    explicitBaseLocation(hrtps.overriddenBase(), pm)
                );
                break;

            default:

                // Draw the sky positions first, so that overlapping targets will take precedence.
                a.allSpCoordinatesJava().foreach(spc ->
                    markPosition(g, Color.cyan, pm.getLocationFromTag(spc))
                );

                // Draw the targets.
                a.allSpTargetsJava().foreach(t ->
                    markPosition(g, Color.yellow, pm.getLocationFromTag(t))
                );

        }
    }

    /**
     */
    public Option<Object> dragStart(final TpeMouseEvent tme, final TpeImageInfo tii) {
        final TargetEnvironment env = getTargetEnvironment();
        if (env == null) return None.instance();

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        // Look for targets close to drag position.
        for (final SPTarget spt: env.getAsterism().allSpTargetsJava()) {
            final PosMapEntry<SPSkyObject> pme = pm.getPositionMapEntry(spt);
            if (pme != null && positionIsClose(pme, tme.xWidget, tme.yWidget)) {
                _dragObject = pme;
                return new Some<>(pme.taggedPos);
            }
        }

        // Look for coordinates close to drag position.
        for (final SPCoordinates spc: env.getAsterism().allSpCoordinatesJava()) {
            final PosMapEntry<SPSkyObject> pme = pm.getPositionMapEntry(spc);
            if (pme != null && positionIsClose(pme, tme.xWidget, tme.yWidget)) {
                _dragObject = pme;
                return new Some<>(pme.taggedPos);
            }
        }

        return None.instance();
    }

    /**
     */
    public void drag(final TpeMouseEvent tme) {
        if (_dragObject != null) {
            if (_dragObject.screenPos == null) {
                _dragObject.screenPos = new Point2D.Double(tme.xWidget, tme.yWidget);
            } else {
                _dragObject.screenPos.x = tme.xWidget;
                _dragObject.screenPos.y = tme.yWidget;
            }

            final SPSkyObject tp = _dragObject.taggedPos;
            tp.setRaDecDegrees(tme.pos.ra().toDegrees(), tme.pos.dec().toDegrees());
        }
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
