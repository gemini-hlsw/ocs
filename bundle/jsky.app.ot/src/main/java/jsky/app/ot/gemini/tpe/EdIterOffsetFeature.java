package jsky.app.ot.gemini.tpe;

import edu.gemini.pot.ModelConverters;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.OffsetValidatingGuideProbe;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import jsky.app.ot.gemini.inst.SciAreaFeature;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;
import jsky.util.gui.DialogUtil;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * A TpeImageFeature extension to draw and manipulate offset positions on
 * the graphical position editor.  This feature will be displayed whenever
 * the SpIterOffset item is selected in the hierarchy view with the position
 * editor open.
 */
public class EdIterOffsetFeature extends TpeImageFeature
        implements TpeDraggableFeature, TpeEraseableFeature,
        TpeCreatableFeature, TpeSelectableFeature, TpeActionableFeature,
        PropertyWatcher, ChangeListener {

    /** Color used to draw offset features */
    public static final Color OFFSET_COLOR = Color.yellow;
    public static final Color INVALID_OFFSET_COLOR = Color.red;

    /** Color used to draw offset features */
    public static final Color OFFSET_SCI_AREA_COLOR = OFFSET_COLOR.darker();

    private static final class SelectedPos {
        final OffsetPosMap posMap;
        final PosMapEntry<OffsetPosBase> entry;

        SelectedPos(OffsetPosMap posMap, PosMapEntry<OffsetPosBase> entry) {
            this.posMap = posMap;
            this.entry  = entry;
        }

        static SelectedPos find(OffsetPosMap posMap, TpeMouseEvent tme) {
            @SuppressWarnings("unchecked") final PosMapEntry<OffsetPosBase> entry = posMap.locate(tme.xWidget, tme.yWidget);
            return (entry == null) ? null : new SelectedPos(posMap, entry);
        }
    }

    /** Array of offset position maps corresponding to the offset position lists. */
    private List<OffsetPosMap> posMaps = Collections.emptyList();

    // The position map entry for the object being dragged.
    private SelectedPos _dragObject;

    // Used to draw the science area at offset positions
    private SciAreaFeature _sciAreaFeature;

    // Offset display options
    private static final BasicPropertyList _props = new BasicPropertyList(EdIterOffsetFeature.class.getName());
    public static final String PROP_OFFSET_DISPLAY = "Display only offsets corresponding to selected offset node";
    static {
        _props.registerBooleanProperty(PROP_OFFSET_DISPLAY, false);
    }


    /**
     * Construct the feature with its name and description.
     */
    public EdIterOffsetFeature() {
        super("Offset", "Base position Offset Display.");
        _props.addWatcher(this);
    }


    /**
     * A property has changed: redraw with the new settings
     *
     * @see PropertyWatcher
     */
    public void propertyChange(String propName) {
        redraw();
    }


    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /**
     * Get the "Display only offsets..." property.
     */
    public boolean isShowOnlySelectedOffset() {
        return _props.getBoolean(PROP_OFFSET_DISPLAY, false);
    }

    public void stateChanged(ChangeEvent e) {
        _maybeRedraw();
    }

    /**
     * Reinitialize the feature (after the base position moves for instance).
     * Part of the TpeImageFeature interface and called by the TpeImageWidget.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        iw.setViewingOffsets(true);

        TpeContext ctx = iw.getContext();
        if (ctx.isEmpty()) return;

        // Get a reference to the science area feature, used to draw the science area
        // at offset positions
        if (_sciAreaFeature == null) _sciAreaFeature = _getSciAreaFeature();

        // Explicitly reset it here because we rely upon it.
        _sciAreaFeature.reinit(iw, tii);

        for (OffsetPosMap opm : posMaps) {
            opm.free();
            _iw.deleteInfoObserver(opm);
        }

        //noinspection unchecked
        final List<SingleOffsetListContext> iterators = ctx.offsets().allJava();

        // iterators map { it => new OffsetPosMap(it) }
        posMaps = new ArrayList<>(iterators.size());
        for (SingleOffsetListContext iterator : iterators) {
            posMaps.add(new OffsetPosMap(iw, iterator));
        }
    }


    /**
     * Unloaded, so free the position map.
     */
    public void unloaded() {
        if (_iw != null) {
            _iw.setViewingOffsets(false);
        }

        super.unloaded();

        for (OffsetPosMap opm : posMaps) opm.free();
        posMaps = Collections.emptyList();
    }

    // Return the SciAreaFeature, or null if none is defined yet
    private SciAreaFeature _getSciAreaFeature() {
        TelescopePosEditor tpe = TpeManager.get();
        return (tpe == null) ? null : (SciAreaFeature) tpe.getFeature(SciAreaFeature.class);
    }

    private void _maybeRedraw() {
        if (posMaps.size() > 1 && isShowOnlySelectedOffset()) redraw();
    }

    public void redraw() {
        if (_iw != null) _iw.repaint();
    }

    private boolean offsetInRange(ObsContext ctx, OffsetPosBase op) {
        final GuideOption activeOpt = StandardGuideOptions.instance.getDefaultActive();
        for (GuideProbe gp : op.getGuideProbes()) {
            if (!(gp instanceof OffsetValidatingGuideProbe)) continue;

            GuideOption opt = op.getLink(gp, activeOpt);
            if (!opt.isActive()) continue;  // ignore inactive guiders

            // Any active OIWFS guide probe that fails the range test makes the
            // offset position out of range.
            if (!((OffsetValidatingGuideProbe) gp).inRange(ctx, op.toSkycalcOffset())) return false;
        }
        return true;
    }

    /**
     * Draw the offset positions.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        if (posMaps.size() == 0) return;

        g.setFont(FONT);
        final int r = MARKER_SIZE / 2 + 1;
        final int d = r * 2;

        // get the currently selected offset position list, if needed
        final boolean showOnlySelected = isShowOnlySelectedOffset();
        final OffsetPosList<OffsetPosBase> selectedOpl = ((posMaps.size() > 1) && showOnlySelected) ?
             getContext().offsets().selectedPosListOrNull() : null;

        for (OffsetPosMap posMap : posMaps) {
            final OffsetPosList<OffsetPosBase> opl = posMap.getTelescopePosList();
            if (posMaps.size() > 1 && showOnlySelected && selectedOpl != null && opl != selectedOpl) {
                continue;
            }

            final List<OffsetPosBase> selected = posMap.getIterator().allSelectedJava();
            final OffsetPosBase first    = (opl.size() > 0) ? opl.getPositionAt(0) : null;

            Iterator<PosMapEntry<OffsetPosBase>> it = posMap.getAllPositionMapEntries();
            while (it.hasNext()) {
                final PosMapEntry<OffsetPosBase> pme = it.next();
                final Point2D.Double p = pme.screenPos;
                final OffsetPosBase op = pme.taggedPos;
                if (p == null || op == null) return;

                final edu.gemini.spModel.core.Offset newOff = ModelConverters.toOffset(op);

                double x = p.x;
                double y = p.y;

                switch (SciAreaFeature.getSciAreaMode()) {
                    case SciAreaFeature.SCI_AREA_SELECTED:
                        if ((selected.contains(op)) || ((selected.isEmpty()) && (op == first))) {
                            g.setColor(OFFSET_SCI_AREA_COLOR);
                            _sciAreaFeature.drawAtOffsetPos(g, tii, newOff, x, y);
                        }
                        break;
                    case SciAreaFeature.SCI_AREA_ALL:
                        g.setColor(OFFSET_SCI_AREA_COLOR);
                        _sciAreaFeature.drawAtOffsetPos(g, tii, newOff, x, y);
                        break;
                }
                final Option<ObsContext> obsCtxOpt=_iw.getObsContext();
                final ObsContext ctx = obsCtxOpt.getOrNull();

                final Color c = (ctx != null) && !offsetInRange(ctx, op) ? INVALID_OFFSET_COLOR : OFFSET_COLOR;
                g.setColor(c);

                // draw an oval marking the offset position
                final int x0 = (int) (x - r), y0 = (int) (y - r);
                g.drawOval(x0, y0, d, d);

                // draw the offset tag, if needed
                if (SciAreaFeature.getDrawIndex()) {
                    // Should probably use font metrics to position the tag ...
                    // This is rather arbitrary ...
                    g.drawString(String.valueOf(opl.getPositionIndex(op)), (int) x + d, (int) y + d + r);
                }
            }
        }
    }


    /**
     * Start dragging an offset position.
     *
     * @see TpeDraggableFeature
     */
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        for (OffsetPosMap posMap : posMaps) {
            _dragObject = SelectedPos.find(posMap, tme);
            if (_dragObject != null) return new Some<>(_dragObject.entry.taggedPos);
        }
        return None.instance();
    }

    /**
     * Drag an offset position.
     *
     * @see TpeDraggableFeature
     */
    public void drag(TpeMouseEvent tme) {
        if (_dragObject != null) {
            OffsetPosBase pos = _dragObject.entry.taggedPos;
            Point2D.Double p = pos.computeXY(tme.xOffset, tme.yOffset, getContext().instrument().issPortOrDefault());
            p = _iw.offsetToScreenCoords(p.getX(), p.getY());
            _dragObject.entry.screenPos.x = p.getX();
            _dragObject.entry.screenPos.y = p.getY();
            _dragObject.posMap.updatePosition(_dragObject.entry, tme);
            _iw.repaint();
        }
    }

    /**
     * Stop dragging an offset position.
     *
     * @see TpeDraggableFeature
     */
    public void dragStop(TpeMouseEvent tme) {
        if (_dragObject != null) {
            _dragObject.posMap.updatePosition(_dragObject.entry, tme);
            _dragObject = null;
            getContext().offsets().commit();
        }
    }

    private SelectedPos find(TpeMouseEvent tme) {
        for (OffsetPosMap posMap : posMaps) {
            final SelectedPos sel = SelectedPos.find(posMap, tme);
            if (sel != null) return sel;
        }
        return null;
    }

    /**
     * If there is an offset position under the mouse, erase it and
     * return true.  Return false otherwise.
     *
     * @see TpeEraseableFeature
     */
    public boolean erase(TpeMouseEvent tme) {
        final SelectedPos sel = find(tme);
        if (sel == null) return false;

        final OffsetPosList<OffsetPosBase> lst = sel.posMap.getTelescopePosList();
        lst.removePosition(sel.entry.taggedPos);
        getContext().offsets().commit();
        return true;
    }

    /**
     * Perform the action of selecting the primary guide star in the group.
     */
    public void action(TpeMouseEvent tme) {
        select(tme);
    }

    /**
     * If there is an offset position under the mouse, return it.
     *
     * @see TpeCreatableFeature
     */
    public Object select(TpeMouseEvent tme) {
        final SelectedPos sel = find(tme);
        if (sel == null) return null;

        final ISPNode node = sel.posMap.getIterator().shell().get();
        final OffsetPosList<OffsetPosBase> lst = sel.posMap.getTelescopePosList();
        if (tme.mouseEvent.isShiftDown() || tme.mouseEvent.isControlDown()) {
            OffsetPosSelection.apply(node).add(lst, sel.entry.taggedPos).commit(node);
        } else {
            OffsetPosSelection.select(lst, sel.entry.taggedPos).commit(node);
        }

        return sel.entry.taggedPos;
    }


    /**
     * Return true if the mouse event is over the image feature drawing.
     */
    public boolean isMouseOver(TpeMouseEvent tme) {
        return find(tme) != null;
    }

    /**
     * Create an offset position at the given mouse position, if possible.
     * Return true if anything is actually created.
     */
    private final TpeCreatableItem[] createableItems = new TpeCreatableItem[] {
        new TpeCreatableItem() {
            public String getLabel() {
                return "Offset";
            }

            public Type getType() {
                return Type.offsetPosition;
            }

            public boolean isEnabled(TpeContext ctx) {
                if (ctx.isEmpty()) return false;
                if (ctx.targets().isEmpty()) return false;
                return (ctx.offsets().allPosLists().size() > 0);
            }

            public void create(TpeMouseEvent tme, TpeImageInfo tii) {
                final List<SingleOffsetListContext> iterators = getContext().offsets().allJava();
                final SingleOffsetListContext iterator;
                if (iterators.size() == 1) {
                    iterator = iterators.get(0);
                } else {
                    // If there are multiple offset nodes in the observation, the user
                    // must select the one to use
                    // TPE REFACTOR
                    iterator = getContext().offsets().selectedOrNull();
                }
                if (iterator == null) {
                    DialogUtil.error("Please select an offset node to append the new position to.");
                } else {
                    final OffsetPosBase pos = iterator.posList().addPosition(tme.xOffset, tme.yOffset);
                    getContext().offsets().commit();

                    final ISPNode shell = iterator.shell().get();
                    OffsetPosSelection.select(iterator.posList(), pos).commit(shell);
                }
            }
        }
    };

    /**
     * Get the label that should be displayed on the create button.
     */
    public TpeCreatableItem[] getCreatableItems() {
        return createableItems;
    }

    @Override public boolean isEnabled(TpeContext ctx) {
        if (!super.isEnabled(ctx)) return false;
        return ctx.offsets().allPosLists().size() > 0;
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.target;
    }
}

