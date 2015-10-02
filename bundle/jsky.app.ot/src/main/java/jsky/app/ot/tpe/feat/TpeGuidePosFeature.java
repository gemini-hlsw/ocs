package jsky.app.ot.tpe.feat;

import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.system.HmsDegTarget;
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

public class TpeGuidePosFeature extends TpePositionFeature
        implements TpeCreateableFeature, TpeActionableFeature, PropertyWatcher {

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
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        _props.addWatcher(this);

        // Tell the position map that the guide star choices are visible.
        TpePositionMap pm = TpePositionMap.getMap(iw);
        pm.setFindGuideStars(true);
    }

    /** Called when the feature is unloaded */
    public void unloaded() {
        // Tell the position map that the guide star choices are no longer visible.
        TpePositionMap pm = TpePositionMap.getExistingMap();
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
    public void propertyChange(String propName) {
        _iw.repaint();
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /**
     * Turn on/off the drawing of position tags.
     */
    public void setDrawTags(boolean drawTags) {
        _props.setBoolean(PROP_SHOW_TAGS, drawTags);
    }

    /**
     * Get the "draw position tags" property.
     */
    public boolean getDrawTags() {
        return _props.getBoolean(PROP_SHOW_TAGS, true);
    }

    /**
     * Turn on/off the drawing of the primary guide star indicator.
     */
    public void setIdentifyPrimary(boolean identifyPrimary) {
        _props.setBoolean(PROP_IDENTIFY_PRIMARY, identifyPrimary);
    }

    /**
     * Get the "indicate primary guide star" property value.
     */
    public boolean getIdentifyPrimary() {
        return _props.getBoolean(PROP_IDENTIFY_PRIMARY, true);
    }

    private SPTarget createNewTarget(TpeMouseEvent tme) {
        SPTarget pos;

        Option<SkyObject> skyObjectOpt = tme.getSkyObject();
        if (!skyObjectOpt.isEmpty()) {
            pos = new SPTarget(HmsDegTarget.fromSkyObject(skyObjectOpt.getValue()));
        } else {
            // No SkyObject info is present so we use the old way of creating
            // a target from a mouse event.
            double ra  = tme.pos.getRaDeg();
            double dec = tme.pos.getDecDeg();

            pos = new SPTarget(ra, dec);
            if (tme.name != null) {
                pos.setName(tme.name);
            }
        }

        return pos;
    }

    // Create-able item for a particular guider.  There will be one for each
    // guider in the GuideProbeMap (that isn't part of an optimize-able group)
    private final class GuiderCreateableItem implements TpeGuidePosCreateableItem {
        private final GuideProbe guider;

        GuiderCreateableItem(GuideProbe guider) {
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
        public boolean isEnabled(TpeContext ctx) {
            if (ctx.isEmpty()) return false;
            if (ctx.targets().isEmpty()) return false;
            Set<GuideProbe> guiders = GuideProbeUtil.instance.getAvailableGuiders(ctx.obsShell().get());
            return guiders.contains(guider);
        }

        /** This is called, e.g., to create a new guide star in reaction to a mouse down **/
        public void create(TpeMouseEvent tme, TpeImageInfo tii) {
            final TargetObsComp obsComp = getTargetObsComp();
            if (obsComp == null) return;

            final SPTarget pos = createNewTarget(tme);

            final TargetEnvironment env = obsComp.getTargetEnvironment();
            final Option<GuideProbeTargets> gptOpt = env.getPrimaryGuideProbeTargets(guider);
            final GuideProbeTargets gpt = gptOpt.getOrElse(GuideProbeTargets.create(guider)).withManualPrimary(pos);

            obsComp.setTargetEnvironment(env.putPrimaryGuideProbeTargets(gpt));
            _iw.getContext().targets().commit();
        }
    }

    // Create-able item for a guide groups.
    private final class GuiderGroupCreateableItem implements TpeCreateableItem {
        private final OptimizableGuideProbeGroup group;

        GuiderGroupCreateableItem(OptimizableGuideProbeGroup group) {
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
        public boolean isEnabled(TpeContext ctx) {
            if (ctx.isEmpty()) return false;

            if (ctx.targets().isEmpty()) return false;
            Set<GuideProbe> guiders = GuideProbeUtil.instance.getAvailableGuiders(ctx.obsShell().get());

            // Assume it should be enabled if any member of the group is
            // present.
            for (GuideProbe guider : group.getMembers()) {
                if (guiders.contains(guider)) return true;
            }
            return false;
        }

        public void create(TpeMouseEvent tme, TpeImageInfo tii) {
            TargetObsComp obsComp = getTargetObsComp();
            if (obsComp == null) return;

            SPTarget   pos = createNewTarget(tme);
            Option<ObsContext> ctx = tme.source.getObsContext();
            if (ctx.isEmpty()) return;

            obsComp.setTargetEnvironment(group.add(pos, false, ctx.getValue()));
            _iw.getContext().targets().commit();
        }
    }

    private TpeCreateableItem[] createableItems;

    private java.util.List<TpeCreateableItem> createCreateableItems() {
        // Get a collection of all the guiders.
        Collection<GuideProbe> guiders = GuideProbeMap.instance.values();

        // Create the result list.
        java.util.List<TpeCreateableItem> res = new ArrayList<>();

        // Review each guider.  If part of an optimizable group, remember the
        // group in a Set.  Otherwise, add a GuiderCreateableItem to the
        // result list.
        Set<OptimizableGuideProbeGroup> groups = new HashSet<>();
        for (GuideProbe guider : guiders) {
            Option<GuideProbeGroup> groupOption = guider.getGroup();

            GuideProbeGroup group = (groupOption.isEmpty() ? null : groupOption.getValue());
            if (group instanceof OptimizableGuideProbeGroup) {
                groups.add((OptimizableGuideProbeGroup) group);
            } else {
                res.add(new GuiderCreateableItem(guider));
            }
        }

        // Go through the groups and add a GuiderGroupCreatableItem for each one.
        for (OptimizableGuideProbeGroup group : groups) {
            res.add(new GuiderGroupCreateableItem(group));
        }

        // Sort the list by label.
        Collections.sort(res, (item1, item2) -> item1.getLabel().compareTo(item2.getLabel()));

        return res;
    }

    public synchronized TpeCreateableItem[] getCreateableItems() {
        if (createableItems == null) {
            java.util.List<TpeCreateableItem> lst = createCreateableItems();
            createableItems = lst.toArray(new TpeCreateableItem[lst.size()]);
        }
        return createableItems;
    }


    /**
     * Locates the guide position at the given mouse event location, if any.
     *
     * @param tme mouse event to check
     *
     * @return optional tuple of the guide probe and target that was selected
     */
    private Option<Tuple2<GuideProbe, SPTarget>> locatePosition(TpeMouseEvent tme) {
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return None.instance();

        TpePositionMap pm = TpePositionMap.getMap(_iw);

        Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            PosMapEntry<SPTarget> pme = it.next();
            SPTarget tp = pme.taggedPos;

            TargetEnvironment env = obsComp.getTargetEnvironment();
            for (GuideProbeTargets gt : env.getOrCreatePrimaryGuideGroup()) {
                if (!gt.getTargets().contains(tp)) continue;

                if (positionIsClose(pme, tme.xWidget, tme.yWidget)) {
                    Tuple2<GuideProbe, SPTarget> tup = new Pair<>(gt.getGuider(), tp);
                    return new Some<>(tup);
                }

            }
        }
        return None.instance();
    }

    /**
     * Perform the action of selecting the primary guide star in the group.
     */
    public void action(TpeMouseEvent tme) {
        if (!getIdentifyPrimary()) return;

        Option<Tuple2<GuideProbe, SPTarget>> res = locatePosition(tme);
        if (res.isEmpty()) return;

        TargetObsComp   toc = getTargetObsComp();
        SPTarget     target = res.getValue()._2();
        PrimaryTargetToggle.instance.toggle(toc, target);
        _iw.getContext().targets().commit();
    }


    /**
     */
    public boolean erase(TpeMouseEvent tme) {
        final Option<Tuple2<GuideProbe, SPTarget>> res = locatePosition(tme);
        if (res.isEmpty()) return false;

        final TargetObsComp     toc = getTargetObsComp();
        final TargetEnvironment env = getTargetEnvironment();
        final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(res.getValue()._1());
        if (gtOpt.isEmpty()) return false;

        final GuideProbeTargets gt = gtOpt.getValue().removeTargetSelectPrimary(res.getValue()._2());
        toc.setTargetEnvironment(env.putPrimaryGuideProbeTargets(gt));
        _iw.getContext().targets().commit();
        return true;
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
        for (final GuideProbeTargets gt : env.getOrCreatePrimaryGuideGroup()) {
            if (gt.getTargets().contains(tp)) {
                TargetSelection.set(env, getContext().targets().shell().get(), tp);
                return tp;
            }
        }
        return null;
    }


    /**
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        Graphics2D g2d = (Graphics2D) g;

        // Get the TargetEnvironment, if any.  If none, give up immediately.
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return;
        TargetEnvironment env = obsComp.getTargetEnvironment();

        // Get the base position.  If not found, give up.
        TpePositionMap pm = TpePositionMap.getMap(_iw);
        Point2D.Double base = pm.getLocationFromTag(env.getBase());
        if (base == null) return;

        // Set up for drawing.
        int size = MARKER_SIZE * 2;
        Map<TextAttribute, Object> attrMap = new HashMap<>();
        attrMap.put(TextAttribute.FONT, FONT);
        Option<ObsContext> obsContextOpt = _iw.getObsContext();
        boolean drawTags = getDrawTags();
        boolean drawPrimary = getIdentifyPrimary();

        // Check for overlapping tags
        Map<Point2D.Double, Integer> overlapMap = new HashMap<>();

        // Draw all the guide targets.
        for (GuideProbeTargets gt : env.getOrCreatePrimaryGuideGroup()) {
            String tagBase = gt.getGuider().getKey();

            // Draw disabled targets in red.  Draw enabled but out of range
            // targets in a slightly transparent color.
            // TODO: GuideProbeTargets.isEnabled
            final Color color = obsContextOpt.exists(c -> GuideProbeUtil.instance.isAvailable(c, gt.getGuider())) ? Color.green : Color.red;
            Color invalidColor = OtColor.makeSlightlyTransparent(color);
            g2d.setColor(color);
            attrMap.put(TextAttribute.FOREGROUND, color);

            // See if we can validate guide stars of this type.
            ValidatableGuideProbe validator = null;
            if (gt.getGuider() instanceof ValidatableGuideProbe) {
                validator = (ValidatableGuideProbe) gt.getGuider();
            }

            // Draw each star of this type.
            int index = 1;
            final ImList<SPTarget> targetList = gt.getTargets();
            for (SPTarget target : targetList) {
                // If there is exactly one of this type, then no need to show
                // the index.
                String tag = (targetList.size() == 1) ? tagBase :
                                String.format("%s (%d)", tagBase, index++);

                // Find the position map entry for this star, if present.
                PosMapEntry<SPTarget> pme = pm.getPositionMapEntry(target);
                if (pme == null) continue;
                Point2D.Double p = pme.screenPos;
                if (p == null) continue;

                // Check whether the position is valid or not.  If not, switch
                // to the invalid color and make the text strikethrough.
                boolean valid = true;
                AttributedString txt = new AttributedString(tag, attrMap);
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
                    Color orig = g2d.getColor();
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
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        TargetObsComp obsComp = getTargetObsComp();
        if (obsComp == null) return None.instance();

        TargetEnvironment env = obsComp.getTargetEnvironment();

        TpePositionMap pm = TpePositionMap.getMap(_iw);

        Iterator<PosMapEntry<SPTarget>> it = pm.getAllPositionMapEntries();
        while (it.hasNext()) {
            PosMapEntry<SPTarget> pme = it.next();

            if (positionIsClose(pme, tme.xWidget, tme.yWidget) && env.isGuidePosition(pme.taggedPos)) {
                _dragObject = pme;
                return new Some<>(pme.taggedPos);
            }
        }
        return None.instance();
    }

    /**
     */
    public void drag(TpeMouseEvent tme) {
        if (_dragObject != null && _dragObject.screenPos != null) {
            _dragObject.screenPos.x = tme.xWidget;
            _dragObject.screenPos.y = tme.yWidget;

            SPTarget tp = (SPTarget) _dragObject.taggedPos;
            tp.setRaDecDegrees(tme.pos.getRaDeg(), tme.pos.getDecDeg());
        }
    }
}

