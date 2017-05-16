package jsky.app.ot.tpe.feat;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.*;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import jsky.app.ot.gemini.editor.targetComponent.PrimaryTargetToggle;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.OtColor;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.text.AttributedString;
import java.util.*;
import java.util.List;

public class TpeGuidePosFeature extends TpePositionFeature
        implements TpeCreatableFeature, TpeActionableFeature, PropertyWatcher {

    private static final BasicPropertyList _props = new BasicPropertyList(TpeGuidePosFeature.class.getName());
    private static final String PROP_SHOW_TAGS = "Show Tags";
    private static final String PROP_IDENTIFY_PRIMARY = "Identify primary guide star";
    static {
        _props.registerBooleanProperty(PROP_SHOW_TAGS, true);
        _props.registerBooleanProperty(PROP_IDENTIFY_PRIMARY, true);
    }

//    private static final Color PRIMARY_STAR_COLOR         = new Color(225, 182, 68);
    private static final Color PRIMARY_STAR_COLOR         = Color.green;
    private static final Color INVALID_PRIMARY_STAR_COLOR = OtColor.makeSlightlyTransparent(PRIMARY_STAR_COLOR);

    /**
     * Construct the feature with its name and description.
     */
    public TpeGuidePosFeature() {
        super("Guide", "Show the locations of guide stars.");
    }

    /**
     * Reinitialize.
     */
    @Override public void reinit(final TpeImageWidget iw, final TpeImageInfo tii) {
        super.reinit(iw, tii);

        _props.addWatcher(this);

        // Tell the position map that the guide star choices are visible.
        final TpePositionMap pm = TpePositionMap.getMap(iw);
        pm.setFindGuideStars(true);
    }

    /** Called when the feature is unloaded */
    @Override public void unloaded() {
        // Tell the position map that the guide star choices are no longer visible.
        final TpePositionMap pm = TpePositionMap.getExistingMap();
        if (pm != null)
            pm.setFindGuideStars(false);

        _props.deleteWatcher(this);

        super.unloaded();
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    @Override public void propertyChange(final String propName) {
        _iw.repaint();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    @Override public BasicPropertyList getProperties() {
        return _props;
    }

    /**
     * Turn on/off the drawing of position tags.
     */
    public void setDrawTags(final boolean drawTags) {
        _props.setBoolean(PROP_SHOW_TAGS, drawTags);
    }

    /**
     * Get the "draw position tags" property.
     */
    private boolean getDrawTags() {
        return _props.getBoolean(PROP_SHOW_TAGS, true);
    }

    /**
     * Turn on/off the drawing of the primary guide star indicator.
     */
    public void setIdentifyPrimary(final boolean identifyPrimary) {
        _props.setBoolean(PROP_IDENTIFY_PRIMARY, identifyPrimary);
    }

    /**
     * Get the "indicate primary guide star" property value.
     */
    private boolean getIdentifyPrimary() {
        return _props.getBoolean(PROP_IDENTIFY_PRIMARY, true);
    }

    private SPTarget createNewTarget(final TpeMouseEvent tme) {
        final Option<SiderealTarget> targetOpt = tme.target;
        return targetOpt.map(SPTarget::new).getOrElse(() -> {
            final double ra  = tme.pos.ra().toDegrees();
            final double dec = tme.pos.dec().toDegrees();
            // No SkyObject info is present so we use the old way of creating
            // a target from a mouse event using only the coordinates.
            SPTarget target = new SPTarget(ra, dec);
            target.setName(tme.name.getOrElse(""));
            return target;
        });
    }

    // Creatable item for a particular guider.  There will be one for each
    // guider in the GuideProbeMap (that isn't part of an optimize-able group)
    private final class GuiderCreatableItem implements TpeGuidePosCreatableItem {
        private final GuideProbe guider;

        GuiderCreatableItem(final GuideProbe guider) {
            this.guider = guider;
        }

        public Type getType() {
            return Type.wfsTarget;
        }

        public String getLabel() {
            return guider.getKey();
        }

        public GuideProbe getGuideProbe() {
            return guider;
        }

        /**
         * Returns <code>true</code> if the corresponding guider is available
         * in the current observation's context.
         */
        public boolean isEnabled(final TpeContext ctx) {
            if (ctx.isEmpty()) return false;
            if (ctx.targets().isEmpty()) return false;
            final Set<GuideProbe> guiders = GuideProbeUtil.instance.getAvailableGuiders(ctx.obsShell().get());
            return guiders.contains(guider);
        }

        /** This is called, e.g., to create a new guide star in reaction to a mouse down **/
        public void create(final TpeMouseEvent tme, final TpeImageInfo tii) {
            final TargetObsComp obsComp = getTargetObsComp();
            if (obsComp == null) return;

            final TargetEnvironment envInit = obsComp.getTargetEnvironment();
            final GuideEnvironment genvInit = envInit.getGuideEnvironment();
            final GuideGroup gpInit         = genvInit.getPrimary();

            final GuideGroup        gp;
            final TargetEnvironment env;
            final int idx;

            if (gpInit.isAutomatic()) {
                // Only the auto group is enabled, create a new manual group and add it as the primary group.
                final ImList<GuideGroup> oldGroups = genvInit.getOptions();
                gp  = GuideGroup.create(GuideGroup.ManualGroupDefaultName());
                idx = oldGroups.size();
                env = envInit.setGuideEnvironment(genvInit.setOptions(oldGroups.append(gp)).setPrimaryIndex(idx));
            } else {
                gp  = gpInit;
                env = envInit;
                idx = genvInit.getPrimaryIndex();
            }

            final GuideProbeTargets oldTargets = gp.get(guider).getOrElse(GuideProbeTargets.create(guider));
            final SPTarget pos                 = createNewTarget(tme);
            final GuideProbeTargets newTargets = oldTargets.update(OptionsList.UpdateOps.appendAsPrimary(pos));
            final GuideGroup gpNew             = gp.put(newTargets);
            final GuideEnvironment genvNew     = env.getGuideEnvironment().setGroup(idx, gpNew);
            obsComp.setTargetEnvironment(env.setGuideEnvironment(genvNew));

            _iw.getContext().targets().commit();
        }
    }

    // Creatable item for a guide groups.
    private final class GuiderGroupCreatableItem implements TpeCreatableItem {
        private final OptimizableGuideProbeGroup group;

        GuiderGroupCreatableItem(final OptimizableGuideProbeGroup group) {
            this.group = group;
        }

        public Type getType() {
            return Type.wfsTarget;
        }

        public String getLabel() {
            return group.getKey();
        }

        /**
         * Returns <code>true</code> if the corresponding group is available
         * in the current observation's context.
         */
        public boolean isEnabled(final TpeContext ctx) {
            if (ctx.isEmpty()) return false;

            if (ctx.targets().isEmpty()) return false;
            final Set<GuideProbe> guiders = GuideProbeUtil.instance.getAvailableGuiders(ctx.obsShell().get());

            // Assume it should be enabled if any member of the group is present.
            return group.getMembers().stream().anyMatch(guiders::contains);
        }

        public void create(final TpeMouseEvent tme, final TpeImageInfo tii) {
            final TargetObsComp obsComp = getTargetObsComp();
            if (obsComp == null) return;

            final SPTarget   pos = createNewTarget(tme);
            tme.source.foreach(iw -> iw.getObsContext().foreach(ctx -> {
                obsComp.setTargetEnvironment(group.add(pos, ctx));
                _iw.getContext().targets().commit();
            }));
        }
    }

    private TpeCreatableItem[] creatableItems;

    private java.util.List<TpeCreatableItem> createCreatableItems() {
        // Get a collection of all the guiders.
        final Collection<GuideProbe> guiders = GuideProbeMap.instance.values();

        // Create the result list.
        final List<TpeCreatableItem> res = new ArrayList<>();

        // Review each guider.  If part of an optimizable group, remember the
        // group in a Set.  Otherwise, add a GuiderCreateableItem to the
        // result list.
        final Set<OptimizableGuideProbeGroup> groups = new HashSet<>();
        guiders.forEach(guider -> {
            final GuideProbeGroup group = guider.getGroup().getOrNull();
            if (group instanceof OptimizableGuideProbeGroup)
                groups.add((OptimizableGuideProbeGroup) group);
            else
                res.add(new GuiderCreatableItem(guider));
        });

        // Go through the groups and add a GuiderGroupCreatableItem for each one.
        groups.forEach(group -> res.add(new GuiderGroupCreatableItem(group)));

        // Sort the list by label.
        res.sort(Comparator.comparing(TpeCreatableItem::getLabel));

        return res;
    }

    public synchronized TpeCreatableItem[] getCreatableItems() {
        if (creatableItems == null) {
            final List<TpeCreatableItem> lst = createCreatableItems();
            creatableItems = lst.toArray(new TpeCreatableItem[lst.size()]);
        }
        return creatableItems;
    }


    /**
     * Locates the guide position at the given mouse event location, if any.
     *
     * @param tme mouse event to check
     *
     * @return optional tuple of the guide probe and target that was selected
     */
    private Option<Tuple2<GuideProbe, SPTarget>> locatePosition(final TpeMouseEvent tme) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return None.instance();

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        final Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            final PosMapEntry<SPTarget> pme = it.next();
            final SPTarget tp = pme.taggedPos;

            final TargetEnvironment env = obsComp.getTargetEnvironment();
            for (final GuideProbeTargets gt : env.getPrimaryGuideGroup()) {
                if (!gt.getTargets().contains(tp)) continue;

                if (positionIsClose(pme, tme.xWidget, tme.yWidget)) {
                    final Tuple2<GuideProbe, SPTarget> tup = new Pair<>(gt.getGuider(), tp);
                    return new Some<>(tup);
                }

            }
        }
        return None.instance();
    }

    /**
     * Perform the action of selecting the primary guide star in the group.
     */
    public void action(final TpeMouseEvent tme) {
        if (!getIdentifyPrimary()) return;

        locatePosition(tme).foreach(tup -> {
            final SPTarget target   = tup._2();
            final TargetObsComp toc = getTargetObsComp();
            PrimaryTargetToggle.instance.toggle(toc, target);
            _iw.getContext().targets().commit();
        });
    }


    /**
     */
    public boolean erase(final TpeMouseEvent tme) {
        final Option<Tuple2<GuideProbe, SPTarget>> res = locatePosition(tme);
        return res.map(tup -> {
            final GuideProbe probe = tup._1();
            final SPTarget target  = tup._2();

            final TargetObsComp     toc = getTargetObsComp();
            final TargetEnvironment env = getTargetEnvironment();
            if (env == null) return false;

            // We do not allow deletion of auto guide stars.
            final GuideGroup        gp  = env.getPrimaryGuideGroup();
            if (gp.isAutomatic()) return false;

            final Option<GuideProbeTargets> gtOpt = gp.get(probe);
            return gtOpt.map(gt -> {
                final GuideProbeTargets gtNew = gt.removeTarget(target);
                toc.setTargetEnvironment(env.putPrimaryGuideProbeTargets(gtNew));
                _iw.getContext().targets().commit();
                return true;
            }).getOrElse(false);
        }).getOrElse(false);
    }

    /**
     * @see jsky.app.ot.tpe.TpeSelectableFeature
     */
    public Object select(final TpeMouseEvent tme) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return false;

        final TpePositionMap pm = TpePositionMap.getMap(_iw);
        final SPTarget tp = (SPTarget) pm.locatePos(tme.xWidget, tme.yWidget);
        if (tp == null) return null;

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        for (final GuideProbeTargets gt : env.getPrimaryGuideGroup()) {
            if (gt.getTargets().contains(tp)) {
                TargetSelection.setTargetForNode(env, getContext().targets().shell().get(), tp);
                return tp;
            }
        }
        return null;
    }


    /**
     */
    public void draw(final Graphics g, final TpeImageInfo tii) {
        final Graphics2D g2d = (Graphics2D) g;

        // Get the TargetEnvironment, if any.  If none, give up immediately.
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return;
        final TargetEnvironment env = obsComp.getTargetEnvironment();

        // Set up for drawing.
        final int size = MARKER_SIZE * 2;
        final Map<TextAttribute, Object> attrMap = new HashMap<>();
        attrMap.put(TextAttribute.FONT, FONT);
        final Option<ObsContext> obsContextOpt = _iw.getObsContext();
        final boolean drawTags = getDrawTags();
        final boolean drawPrimary = getIdentifyPrimary();

        // Check for overlapping tags
        final Map<Point2D.Double, Integer> overlapMap = new HashMap<>();

        // Draw all the guide targets.
        for (final GuideProbeTargets gt : env.getPrimaryGuideGroup()) {
            final String tagBase = gt.getGuider().getKey();

            // Draw disabled targets in red.  Draw enabled but out of range
            // targets in a slightly transparent color.
            final Color color = obsContextOpt.exists(c -> GuideProbeUtil.instance.isAvailable(c, gt.getGuider())) ? Color.green : Color.red;
            final Color invalidColor = OtColor.makeSlightlyTransparent(color);
            g2d.setColor(color);
            attrMap.put(TextAttribute.FOREGROUND, color);

            // See if we can validate guide stars of this type.
            final ValidatableGuideProbe validator = (gt.getGuider() instanceof ValidatableGuideProbe)
                    ? ((ValidatableGuideProbe) gt.getGuider()) : null;

            // Draw each star of this type.
            int index = 1;
            final ImList<SPTarget> targetList = gt.getTargets();
            for (SPTarget target : targetList) {
                // If there is exactly one of this type, then no need to show
                // the index.
                final String tag = (targetList.size() == 1) ? tagBase :
                                String.format("%s (%d)", tagBase, index++);

                // Find the position map entry for this star, if present.
                final PosMapEntry<SPTarget> pme = TpePositionMap.getMap(_iw).getPositionMapEntry(target);
                if (pme == null) continue;
                final Point2D.Double p = pme.screenPos;
                if (p == null) continue;

                // Check whether the position is valid or not.  If not, switch
                // to the invalid color and make the text strikethrough.
                boolean valid = true;
                final AttributedString txt = new AttributedString(tag, attrMap);
                if (!obsContextOpt.isEmpty() && (validator != null) &&
                        validator.validate(target, obsContextOpt.getValue()) != GuideStarValidation.VALID) {
                    txt.addAttribute(TextAttribute.STRIKETHROUGH, true);
                    txt.addAttribute(TextAttribute.FOREGROUND, invalidColor);
                    g2d.setColor(invalidColor);
                    valid = false;
                }

                // Draw the target.
                g2d.drawRect((int) (p.x - size / 2), (int) (p.y - size / 2), size, size);

                // keep count of tags that have the same position so we can stack them instead of overlapping them
                Integer overlap = overlapMap.get(p);
                if (overlap != null) {
                    overlap = overlap + 1;
                } else {
                    overlap = 0;
                }
                overlapMap.put(p, overlap);

                // Draw a marker to identify the primary guide star.
                int x = (int) (p.x + size);
                if (drawPrimary && !gt.getPrimary().isEmpty() && (gt.getPrimary().getValue() == target)) {
                    final Color orig = g2d.getColor();
                    g2d.setColor(valid ? PRIMARY_STAR_COLOR : INVALID_PRIMARY_STAR_COLOR);
                    g2d.fillOval(x, (int) p.y - overlap*11, size, size);
                    g2d.setColor(orig);
                    x += size + 1;
                }

                // Draw the guide star tag.
                if (drawTags) {
                    g2d.drawString(txt.getIterator(), x, (int) p.y + size - overlap*11);
                }

                // Reset the color if necessary.
                g2d.setColor(color);
            }
        }
    }

    /**
     */
    public Option<Object> dragStart(final TpeMouseEvent tme, final TpeImageInfo tii) {
        final TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return None.instance();

        final TargetEnvironment env = obsComp.getTargetEnvironment();

        // We do not allow deletion of auto guide stars.
        final GuideGroup        gp  = env.getPrimaryGuideGroup();
        if (gp.isAutomatic()) return None.instance();

        final TpePositionMap pm = TpePositionMap.getMap(_iw);

        final Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            final PosMapEntry<SPTarget> pme = it.next();

            if (positionIsClose(pme, tme.xWidget, tme.yWidget) && env.isGuidePosition(pme.taggedPos)) {
                _dragObject = pme;
                return new Some<>(pme.taggedPos);
            }
        }
        return None.instance();
    }

    /**
     */
    public void drag(final TpeMouseEvent tme) {
        if (_dragObject != null && _dragObject.screenPos != null) {
            _dragObject.screenPos.x = tme.xWidget;
            _dragObject.screenPos.y = tme.yWidget;

            final SPTarget tp = _dragObject.taggedPos;
            tp.setRaDecDegrees(tme.pos.ra().toDegrees(), tme.pos.dec().toDegrees());
        }
    }
}

