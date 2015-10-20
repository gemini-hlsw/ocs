package jsky.app.ot.tpe;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.nici.SeqRepeatNiciOffset;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.guide.GuideProbeAvailabilityVolatileDataObject;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetSelection;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import edu.gemini.spModel.target.system.ITarget;
import jsky.app.jskycat.JSkyCat;
import jsky.app.ot.OT;
import jsky.app.ot.ags.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.catalog.BasicQueryArgs;
import jsky.catalog.Catalog;
import jsky.catalog.CatalogException;
import jsky.catalog.QueryArgs;
import jsky.catalog.gui.CatalogNavigator;
import jsky.catalog.gui.CatalogQueryTool;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.navigator.*;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Implements a telescope position editor for the Gemini telescope
 * and instruments. This class displays an image and/or graphics overlays for guide
 * stars and instruments and allows the user to edit the positions to use
 * for an observation.
 *
 * @author Allan Brighton (based on code from original OT version)
 * @version $Revision: 46768 $ $Date: 2012-07-16 14:58:53 -0400 (Mon, 16 Jul 2012) $
 */
public class TelescopePosEditor extends JSkyCat implements TpeMouseObserver {

    /**
     * All feature class names
     */
    private static final Vector<String> _featureClasses = new Vector<>();

    /**
     * List of all features
     */
    private final Vector<TpeImageFeature> _allFeatures;

    private TpeContext _ctx = TpeContext.empty();

    /**
     * The image widget
     */
    private TpeImageWidget _iw;

    /**
     * Tool button helper
     */
    private final TpeEditorTools _editorTools;

    /**
     * Object managing the image features
     */
    private final TpeFeatureManager _featureMan;

    /**
     * Toolbar (on the side with toggle buttons)
     */
    private final TpeToolBar _tpeToolBar;

    /**
     * Menu bar
     */
    private final TpeImageDisplayMenuBar _tpeMenuBar;

    private final AgsContextPublisher _agsPub = new AgsContextPublisher();

    /**
     * Create the application class and display the contents of the
     * given image file or URL, if not null.
     */
    public TelescopePosEditor() {
        super(null);

        _iw.setTitle("Position Editor");

        // get the TPE toolbar handle
        Component parent = _iw.getParentFrame();
        _tpeToolBar = ((TpeImageDisplayFrame) parent).getTpeToolBar();
        _tpeMenuBar = (TpeImageDisplayMenuBar) ((TpeImageDisplayFrame) parent).getJMenuBar();

        _tpeToolBar.getGuiderSelector().addSelectionListener(strategy -> AgsStrategyUtil.setSelection(_ctx.obsShellOrNull(), strategy));

        // Don't want the "New Window" menu item here
        _tpeMenuBar.getFileMenu().remove(_tpeMenuBar.getNewWindowMenuItem());

        _editorTools = new TpeEditorTools(this);

        //
        // Create and add the standard TpeImageFeatures
        //

        _iw.addMouseObserver(this);
        _featureMan = new TpeFeatureManager(this, _iw);

        // Get the list of all features and add them to the user
        // interface in the order in which they were registered
        _allFeatures = _createFeatures();
        for (int i = 0; i < _allFeatures.size(); ++i) {
            _addFeature(_allFeatures.elementAt(i));
        }

        // Select the "Browse" tool.
        _editorTools.gotoBrowseMode();

        // Update the GUI when the OT editable state changes
        OT.addEditableStateListener(new OT.EditableStateListener() {
            @Override public ISPNode getEditedNode() { return _ctx.nodeOrNull(); }
            @Override public void updateEditableState() { _editorTools.updateAvailableOptions(_allFeatures); }
        });

        _agsPub.subscribe((obs, oldOptions, newOptions) -> _tpeToolBar.getGuiderSelector().setAgsOptions(newOptions));
    }

    /**
     * Return true if the given SP node is one of the nodes managed by
     * this class and used by the tpe. These are the nodes that need
     * to be shared between the SP viewer and the position editor,
     * since they both display data based on them. This includes the
     * target env, offset and instrument nodes.
     */
    public static boolean isTpeObject(ISPNode node) {
        SPComponentType type = null;
        if (node instanceof ISPObsComponent) {
            type = ((ISPObsComponent) node).getType();
        } else if (node instanceof ISPSeqComponent) {
            type = ((ISPSeqComponent) node).getType();
        }
        if (type != null) {
            final SPComponentBroadType broadType = type.broadType;
            return (broadType.equals(TargetObsComp.SP_TYPE.broadType) ||
                    broadType.equals(SPComponentBroadType.INSTRUMENT) ||
                    type.equals(SeqRepeatOffset.SP_TYPE) ||
                    type.equals(SeqRepeatNiciOffset.SP_TYPE) ||
                    type.equals(SPSiteQuality.SP_TYPE));
        }
        return false;
    }

    /**
     * Make and return a frame for displaying the given image (may be null).
     *
     * @param imageFileOrUrl specifies the image file or URL to display
     */
    @Override
    protected NavigatorImageDisplayFrame makeNavigatorImageDisplayFrame(String imageFileOrUrl) {
        TpeImageDisplayFrame frame = new TpeImageDisplayFrame(imageFileOrUrl);
        // Very ugly this sets _iw indirectly on the constructor
        _iw = (TpeImageWidget) frame.getImageDisplayControl().getImageDisplay();
        return frame;
    }

    /**
     * Return the telescope position editor toolbar (the one with the toggle buttons)
     */
    public TpeToolBar getTpeToolBar() {
        return _tpeToolBar;
    }

    /**
     * Register an independent feature.  These features will be
     * created when a TelescopePositionEditor is created.
     */
    public static void registerFeature(String className) {
        _featureClasses.addElement(className);
    }

    /**
     * Return a reference to the first registered TPE image feature
     * corresponding to the given class, or null if not found.
     */
    public TpeImageFeature getFeature(Class<?> c) {
        int size = _allFeatures.size();
        for (int i = 0; i < size; i++) {
            TpeImageFeature feat = _allFeatures.elementAt(i);
            if (feat.getClass().equals(c))
                return feat;
        }
        return null;
    }

    public Set<TpeImageFeature> getFeatures() {
        return new HashSet<>(_allFeatures);
    }

    /**
     * Instantiate all the TpeImageFeatures indicated in the given Vector.
     */
    private static Vector<TpeImageFeature> _createFeatures() {
        final Vector<TpeImageFeature> v = new Vector<>();
        for (int i = 0; i < TelescopePosEditor._featureClasses.size(); ++i) {
            final String className = TelescopePosEditor._featureClasses.elementAt(i);
            v.addElement(TpeImageFeature.createFeature(className));
        }
        return v;
    }

    /**
     * Do the work of adding a feature without worrying about whether
     * it should be visible or not.
     */
    private void _addFeature(TpeImageFeature tif) {
        if (_featureMan.isFeaturePresent(tif)) return;// already being displayed

        _featureMan.addFeature(tif);
        _editorTools.addFeature(tif);

        // If this feature has properties, show them in the "View" menu.
        BasicPropertyList pl = tif.getProperties();
        if (pl != null) {
            _tpeMenuBar.addPropertyConfigMenuItem(tif.getName(), pl);
        }
    }

    public void selectFeature(TpeImageFeature tif) {
        _featureMan.setSelected(tif, true);
    }


    /**
     * Update the position list's base position to coincide with the location
     * of the center of the current image.
     */
    // TPE REFACTOR -- why isn't this used?
    public void setBasePositionFromImage() {
        if (_ctx.targets().isEmpty()) {
            DialogUtil.error("There is no target list!");
            return;
        }

        SPTarget tp = _ctx.targets().baseOrNull();
        if (tp == null) {
            DialogUtil.error("There is no base position!");
            return;
        }

        WorldCoords basePos = getImageCenterLocation();
        if (basePos == null) {
            DialogUtil.error("The image does not support world coordinates!");
            return;
        }

        tp.setRaDecDegrees(basePos.getRaDeg(), basePos.getDecDeg());
    }

    /**
     * Load a cached image for the base position, or generate a blank image with
     * world coordinate support and the base position at the center.
     */
    private void loadImage() {
        double ra = 0.0;
        double dec = 0.0;
        SPTarget tp = _ctx.targets().baseOrNull();
        if (tp != null) {
            // Get the RA and Dec from the pos list.
            ITarget target = tp.getTarget();
            final Option<Long> when = _ctx.schedulingBlockJava().map(SchedulingBlock::start);
            ra = target.getRaDegrees(when).getOrElse(0.0);
            dec = target.getDecDegrees(when).getOrElse(0.0);
        }

        _iw.loadCachedImage(ra, dec);
    }

    /**
     * Get the location of the center of the image being displayed.
     */
    public WorldCoords getImageCenterLocation() {
        CoordinateConverter converter = _iw.getCoordinateConverter();
        if (!converter.isWCS()) {
            return null;
        }

        Point2D.Double p = converter.getWCSCenter();
        return new WorldCoords(p.x, p.y, converter.getEquinox());
    }

    private boolean basePosUpdated(TpeContext oldCtx, TpeContext newCtx) {
        SPTarget oldBasePos = oldCtx.targets().baseOrNull();
        SPTarget newBasePos = newCtx.targets().baseOrNull();
        if (newBasePos == null) return oldBasePos != null;
        if (oldBasePos == null) return true;

        ITarget oldBase = oldBasePos.getTarget();
        ITarget newBase = newBasePos.getTarget();

        Option<Long> oldWhen = oldCtx.schedulingBlockJava().map(SchedulingBlock::start);
        Option<Long> newWhen = newCtx.schedulingBlockJava().map(SchedulingBlock::start);

        return !(oldBase.getRaDegrees(oldWhen).equals(newBase.getRaDegrees(newWhen)) &&
                 oldBase.getDecDegrees(oldWhen).equals(newBase.getDecDegrees(newWhen)));
    }

    private final PropertyChangeListener obsListener = evt -> {
        if (!(evt.getSource() instanceof ISPNode)) return;
        if (!evt.getPropertyName().equals(SPUtil.getDataObjectPropertyName()) &&
                !(evt instanceof SPStructureChange)) return;

        final ISPNode src = (ISPNode) evt.getSource();
        final ISPDataObject obj = src.getDataObject();
        if (isTpeObject(src) || (obj instanceof GuideProbeAvailabilityVolatileDataObject)) {
            if (_ctx.node().isDefined()) {
                reset(_ctx.node().get());
            }
        }
    };
    private final PropertyChangeListener selListener = evt -> {
        final ISPNode src = (ISPNode) evt.getSource();
        reset(src);
    };

    /**
     * Reset the position editor based on the currently selected science program
     * tree node.
     */
    public void reset(ISPNode node) {
        if (_ctx.obsShell().isDefined()) {
            final ISPObservation obs = _ctx.obsShell().get();
            obs.removeCompositeChangeListener(obsListener);
            obs.removeStructureChangeListener(obsListener);
        }

        if (_ctx.targets().shell().isDefined()) {
            final ISPObsComponent targetComp = _ctx.targets().shell().get();
            TargetSelection.deafTo(targetComp, selListener);
        }
        for (SingleOffsetListContext off : _ctx.offsets().allJava()) {
            final ISPNode n = off.shell().get();
            OffsetPosSelection.deafTo(n, selListener);
        }

        final TpeContext oldCtx = _ctx;
        _ctx = TpeContext.apply(node);
        _iw.reset(_ctx);

        // Reset the position map with the new position list to work from.
        final TpePositionMap pm = TpePositionMap.getMap(_iw);
        pm.reset(_ctx);

        if (_ctx.isEmpty()) {
            _iw.clear();
        } else {
            if (basePosUpdated(oldCtx, _ctx)) {
                loadImage();
            } else if (_ctx.targets().baseOrNull() == null) {
                _iw.clear();
            }
        }

        _featureMan.updateAvailableOptions(_allFeatures, _ctx);
        _editorTools.updateAvailableOptions(_allFeatures);

        _editorTools.updateEnabledStates();

        // update selected guiders in toolbar
        _agsPub.watch(_ctx.obsShellOrNull());
        _tpeToolBar.getGuiderSelector().setAgsOptions(_agsPub.getAgsContext());

        if (_ctx.obsShell().isDefined()) {
            final ISPObservation obs = _ctx.obsShell().get();
            obs.addCompositeChangeListener(obsListener);
            obs.addStructureChangeListener(obsListener);
        }

        if (_ctx.targets().shell().isDefined()) {
            final ISPObsComponent targetComp = _ctx.targets().shell().get();
            TargetSelection.listenTo(targetComp, selListener);
        }
        for (SingleOffsetListContext off : _ctx.offsets().allJava()) {
            final ISPNode n = off.shell().get();
            OffsetPosSelection.listenTo(n, selListener);
        }
    }

    /**
     * Get a Sky Image based on the properties set by the user
     *
     * @throws IOException      If a problem happens reading from the catalog
     * @throws CatalogException if a Catalog Problem is found
     */
    public void getSkyImage() throws IOException, CatalogException {
        String catalogProperties = Preferences.get(Catalog.SKY_USER_CATALOG);
        // If no properties, show the catalog browser
        if (catalogProperties == null) {
            TpeSkyDialogEd ed = TpeSkyDialogEd.getInstance();
            ed.showDialog(getImageFrame());
            return;
        }

        String args[] = catalogProperties.split("\\*");

        if (args.length <= 0) return;
        Catalog c = CatalogNavigator.getCatalogDirectory().getCatalog(args[0]);

        if (c == null || !c.isImageServer()) return;

//        // XXX REL-201: This prevents a bug related to the progress dialog, which
//        // is displayed during the background query started below and refers to the
//        // catalog window as its modal dialog parent frame.
//        getImageWidget().openCatalogWindow();

        final Navigator nav = NavigatorManager.get();
        nav.setImageDisplayManager(new NavigatorImageDisplayManager() {
            @Override public NavigatorImageDisplay getImageDisplay() {
                NavigatorImageDisplay imageDisplay = getImageWidget();
                imageDisplay.setNavigator(nav);
                return imageDisplay;
            }

            @Override public Component getImageDisplayControlFrame() {
                return getImageDisplay().getParentFrame();
            }
        });
        CatalogQueryTool cqt = new CatalogQueryTool(c, nav);
        QueryArgs queryArgs = new BasicQueryArgs(c);
        queryArgs.setParamValue(0, null);
        queryArgs.setParamValue(1, c.getName());

        SPTarget _baseTarget = _ctx.targets().baseOrNull();
        if (_baseTarget == null) return;

        // XXX FIXME: We shouldn't have to use numeric indexes here
        final Option<Long> when = _ctx.schedulingBlockJava().map(SchedulingBlock::start);
        boolean go = // true if coords are known
            _baseTarget.getTarget().getRaString(when).flatMap(ra ->
            _baseTarget.getTarget().getDecString(when).map(dec -> {
                queryArgs.setParamValue(2, ra);
                queryArgs.setParamValue(3, dec);
                queryArgs.setParamValue(4, _baseTarget.getTarget().getTag().tccName);
                if (args.length > 2) {
                    //first argument must be a Double, it represent the size on AstroCatalogs
                    queryArgs.setParamValue(5, Double.valueOf(args[1]));
                    queryArgs.setParamValue(6, args[2]);
                } else { //use default parameters
                    queryArgs.setParamValue(5, 15.0);
                    // REL-269: don't set the mag band here
                    if (c instanceof SkycatCatalog) {
                        queryArgs.setParamValue(6, 15.0);
                    }
                }
                return true;
            })).getOrElse(false);
        if (go) {
            cqt.setQueryResult(c.query(queryArgs)); // throws; can't go in block above
        }
    }


    /**
     * Receive a mouse event in the image widget.
     */
    @Override
    public void tpeMouseEvent(TpeImageWidget iw, TpeMouseEvent mouseEvent) {
        // only interested in button 1 here
        if (mouseEvent.mouseEvent.getButton() > 1)
            return;

        if (mouseEvent.id == MouseEvent.MOUSE_PRESSED) {
            switch (_editorTools.getMode()) {
                case BROWSE:
                    break;

                case ERASE:
                    _iw.erase(mouseEvent);
                    break;

                case DRAG:
                    _iw.dragStart(mouseEvent);
                    break;
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_CLICKED) {
            switch (_editorTools.getMode()) {
                case BROWSE:
                    if (mouseEvent.mouseEvent.getClickCount() == 2) {
                        _iw.action(mouseEvent);
                    }
                    break;

                case ERASE:
                    _iw.erase(mouseEvent);
                    break;

                case CREATE:
                    _iw.create(mouseEvent, _editorTools.getCurrentCreatableItem());
                    break;
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_DRAGGED) {
            switch (_editorTools.getMode()) {
                case DRAG: {
                    _iw.drag(mouseEvent);
                    break;
                }
                case ERASE: {
                    _iw.erase(mouseEvent);
                    break;
                }
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_RELEASED) {
            if (_editorTools.getMode() == TpeMode.DRAG) {
                _iw.dragStop(mouseEvent);
            }
            return;
        }

        if (mouseEvent.id == MouseEvent.MOUSE_MOVED) {
            TpeCursor c = TpeCursor.browse;
            switch (_editorTools.getMode()) {
                case ERASE:
                    c = TpeCursor.erase;
                    break;
                case DRAG:
                    c = TpeCursor.drag;
                    break;
                case CREATE:
                    c = TpeCursor.add;
                    break;
            }
            ((TpeImageDisplayFrame) getImageFrame()).getImageDisplayControl().setCursor(c.get());
        }
    }


    /**
     * Return the image widget
     */
    public TpeImageWidget getImageWidget() {
        return _iw;
    }
}
