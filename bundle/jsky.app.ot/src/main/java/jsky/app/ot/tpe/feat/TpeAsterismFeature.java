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
import jsky.app.ot.util.OtColor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.time.Instant;

// TODO:ASTERISM: Draw base position … right now we only draw the targets
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
            g.fillOval((int) (p.x - r/2), (int) (p.y - r/2), r, r);
        });
    }

    private Option<Coordinates> baseCoordinates(ObsContext ctx) {
        return ctx.getBaseCoordinates().flatMap(edu.gemini.skycalc.Coordinates::toCoreCoordinates);
    }

    private Option<Coordinates> ghostCoordinates(
      ImEither<SPCoordinates, GhostAsterism.GhostTarget> ifu,
      Option<Instant> when
    ) {
       return ifu.biFold(
           coords -> ImOption.apply(coords.getCoordinates()),
           target -> ImOption.fromScalaOpt(target.coordinates(when.toScalaOpt()))
       );
    }

    private void drawGhostIfu(
      Graphics        g,
      TpePositionMap  pm,
      Option<Instant> when,
      Coordinates     base,
      ImEither<SPCoordinates, GhostAsterism.GhostTarget> ifu,
      GhostIfuPatrolField patrolField
    ) {
        ghostCoordinates(ifu, when).foreach(pos -> {
            final Color color =
                patrolField.inRange(Coordinates.difference(base, pos).offset()) ?
                        ifu.biFold(c -> Color.cyan, t -> Color.yellow)          :
                        Color.red;

            final Point2D.Double loc =
                ifu.biFold(pm::getLocationFromTag, t -> pm.getLocationFromTag(t.spTarget()));

            markPosition(g, color, loc);
        });
    }

    private void drawGhostAsterism(
      final Graphics g,
      Option<ImEither<SPCoordinates, GhostAsterism.GhostTarget>> ifu1,
      Option<ImEither<SPCoordinates, GhostAsterism.GhostTarget>> ifu2,
      Option<SPCoordinates> explicitBase
    ) {
        final TpePositionMap pm = TpePositionMap.getMap(_iw);
        getContext().obsContextJavaWithConditions(SPSiteQuality.Conditions.NOMINAL).foreach(ctx -> {
            final Option<Instant> when = ctx.getSchedulingBlockStart().map(Instant::ofEpochMilli);
            explicitBase.foreach(c -> markBase(g, pm.getLocationFromTag(c)));
            baseCoordinates(ctx).foreach(base -> {
                ifu1.foreach(ifu -> drawGhostIfu(g, pm, when, base, ifu, GhostIfuPatrolField$.MODULE$.ifu1(ctx)));
                ifu2.foreach(ifu -> drawGhostIfu(g, pm, when, base, ifu, GhostIfuPatrolField$.MODULE$.ifu2(ctx)));
            });
        });
    }

    public void draw(final Graphics g, final TpeImageInfo tii) {

        final TargetEnvironment env = getTargetEnvironment();
        if (env == null) return;

        final Asterism a = env.getAsterism();
        switch (a.asterismType()) {
            case GhostSingleTarget:
                final GhostAsterism.SingleTarget st = ((GhostAsterism.SingleTarget) a);
                drawGhostAsterism(
                    g,
                    ImOption.apply(new Right<>(st.target())),
                    ImOption.empty(),
                    ImOption.fromScalaOpt(st.overriddenBase())
                );
                break;

            case GhostDualTarget:
                final GhostAsterism.DualTarget dt = ((GhostAsterism.DualTarget) a);
                drawGhostAsterism(
                    g,
                    ImOption.apply(new Right<>(dt.target1())),
                    ImOption.apply(new Right<>(dt.target2())),
                    ImOption.fromScalaOpt(dt.overriddenBase())
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
                    ImOption.apply(new Right<>(tps.target())),
                    ImOption.apply(new Left<>(tps.sky())),
                    ImOption.fromScalaOpt(tps.overriddenBase())
                );
                break;

            case GhostSkyPlusTarget:
                final GhostAsterism.SkyPlusTarget spt = ((GhostAsterism.SkyPlusTarget) a);
                drawGhostAsterism(
                    g,
                    ImOption.apply(new Left<>(spt.sky())),
                    ImOption.apply(new Right<>(spt.target())),
                    ImOption.fromScalaOpt(spt.overriddenBase())
                );
                break;

            case GhostHighResolutionTargetPlusSky:
                final GhostAsterism.HighResolutionTargetPlusSky hrtps = ((GhostAsterism.HighResolutionTargetPlusSky) a);
                drawGhostAsterism(
                    g,
                    ImOption.apply(new Right<>(hrtps.target())),
                    ImOption.apply(new Left<>(hrtps.sky())),
                    ImOption.fromScalaOpt(hrtps.overriddenBase())
                );
                break;

            default:
                final TpePositionMap pm = TpePositionMap.getMap(_iw);

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
