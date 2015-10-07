package jsky.app.ot.tpe;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.catalog.api.MagnitudeConstraints;
import edu.gemini.catalog.ui.QueryResultsFrame;
import edu.gemini.shared.cat.CatalogSearchParameters;
import edu.gemini.shared.cat.ICatalogAlgorithm;
import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.ags.AgsStrategyKey;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.*;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.target.system.CoordinateParam.Units;
import edu.gemini.spModel.target.system.ITarget;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.ags.AgsStrategyUtil;
import jsky.app.ot.tpe.gems.GemsGuideStarSearchDialog;
import jsky.app.ot.util.OtColor;
import jsky.app.ot.util.PolygonD;
import jsky.app.ot.util.Resources;
import jsky.app.ot.util.ScreenMath;
import jsky.catalog.gui.TablePlotter;
import jsky.coords.CoordinateConverter;
import jsky.coords.Coordinates;
import jsky.catalog.gui.NamedCoordinates;
import jsky.coords.WorldCoords;
import jsky.navigator.Navigator;
import jsky.navigator.NavigatorImageDisplay;
import jsky.navigator.NavigatorManager;
import jsky.util.gui.DialogUtil;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class is concerned with drawing targets, WFS etc., on an image.
 */
public class TpeImageWidget extends NavigatorImageDisplay implements MouseInputListener,
        TelescopePosWatcher, PropertyChangeListener {

    private static final Logger LOG = Logger.getLogger(TpeImageWidget.class.getName());

    // List of mouse observers
    private final Vector<TpeMouseObserver> _mouseObs = new Vector<>();

    // List of image view observers
    private final Vector<TpeViewObserver> _viewObs = new Vector<>();

    // Information about the image
    private TpeContext _ctx = TpeContext.empty();

    private final TpeImageInfo _imgInfo = new TpeImageInfo();

    // True if the image info is valid
    private boolean _imgInfoValid = false;

    // List of image info observers
    private final Vector<TpeImageInfoObserver> _infoObs = new Vector<>();

    // A list of position editor features that can be drawn on the image.
    private final Vector<TpeImageFeature> _featureList = new Vector<>();

    // The current item being dragged
    private TpeDraggableFeature _dragFeature;

    // Base position in J2000
    private WorldCoords _basePos = new WorldCoords();

    // Base pos not visible
    private boolean _baseOutOfView = false;

    // The default algorithm to use for catalog searches
    private ICatalogAlgorithm _algorithm;

    // Dialog for GeMS manual guide star selection
    private GemsGuideStarSearchDialog _gemsGuideStarSearchDialog;

    private final AbstractAction _skyImageAction = new AbstractAction(
            "Sky Images...",
            Resources.getIcon("guidestars24.gif")) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Get sky image using default parameters");
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                TelescopePosEditor tpe = TpeManager.open();
                tpe.getSkyImage();
            } catch (Exception e) {
                DialogUtil.error(e);
            } finally {
                //make sure the navigator is closed properly
                final Navigator nav = NavigatorManager.get();
                nav.close();
            }
        }
    };

    // Action to use to show the guide star search window
    private final AbstractAction _manualGuideStarAction = new AbstractAction(
            "Manual GS",
            Resources.getIcon("gsmanual.png")) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Query a guide star catalog, review candidates, and select");
        }

        public void actionPerformed(ActionEvent evt) {
            try {
                manualGuideStarSearch();
            } catch (Exception e) {
                DialogUtil.error(e);
            }
        }
    };

    private boolean _viewingOffsets;

    /**
     * Constructor.
     *
     * @param parent the parent frame or internal frame
     */
    public TpeImageWidget(Component parent) {
        super(parent);

        addMouseListener(this);
        addMouseMotionListener(this);
    }


    /**
     * Return true if this is the main application window (enables exit menu item)
     */
    public boolean isMainWindow() {
        return false; // grumble
    }


    /**
     * Overrides the base class version to add the OT graphics.
     *
     * @param g      the graphics context
     * @param region if not null, the region to paint
     */
    public synchronized void paintLayer(Graphics2D g, Rectangle2D region) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        super.paintLayer(g, region);

        if (_ctx.isEmpty()) return;

        if (!_imgInfoValid) {
            if (!setBasePos(_basePos)) {
                return;
            }
        }

        final java.util.List<TpeMessage> messages = new ArrayList<>();
        for (final TpeImageFeature tif : _featureList) {
            tif.draw(g, _imgInfo);

            // Gather any warnings from this feature.
            final Option<Collection<TpeMessage>> opt = tif.getMessages();
            if (opt.isDefined()) {
                messages.addAll(opt.getValue());
            }
        }

        if (_baseOutOfView) {
            messages.add(TpeMessage.warningMessage("Base position is out of view."));
        }
        if (messages.size() > 0) displayMessages(messages, g);
    }

    private static final Font MESSAGE_FONT = new Font("dialog", Font.PLAIN, 12);

    private static final ImageIcon ERROR_ICON = Resources.getIcon("error_tsk.gif");
    private static final ImageIcon WARNING_ICON = Resources.getIcon("warn_tsk.gif");
    private static final ImageIcon INFO_ICON = Resources.getIcon("info_tsk.gif");

    private void displayMessages(java.util.List<TpeMessage> messages, Graphics2D g) {
        Collections.sort(messages);
        Collections.reverse(messages);

        int vPad = 6;
        int hPad = 10;

        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight() + vPad;

        Color origColor = g.getColor();
        Font origFont = g.getFont();
        g.setFont(MESSAGE_FONT);


        int y = getHeight();
        for (TpeMessage msg : messages) {
            y -= lineHeight;
            g.setColor(getMessageColor(msg));
            g.fillRect(0, y, getWidth(), lineHeight);

            ImageIcon icon = getMessageIcon(msg);
            int iconY = y + (lineHeight - icon.getIconHeight()) / 2;
            g.drawImage(icon.getImage(), hPad, iconY, null);

            g.setColor(Color.black);
            int strX = hPad * 2 + icon.getIconWidth();
            int strY = y + fm.getLeading() + fm.getAscent() + vPad / 2;
            g.drawString(msg.getMessage(), strX, strY);
        }

        // Separate the items with a line.
        for (int i = 1; i < messages.size(); ++i) {
            int lineY = getHeight() - i * lineHeight;
            g.setColor(OtColor.DARKER_BG_GREY);
            g.drawLine(0, lineY, getWidth(), lineY);
        }

        g.setColor(origColor);
        g.setFont(origFont);
    }

    private Color getMessageColor(TpeMessage msg) {
        switch (msg.getMessageType()) {
            case INFO:
                return OtColor.LIGHT_GREY;
            case ERROR:
                return OtColor.LIGHT_ORANGE;
            default:
            case WARNING:
                return OtColor.BANANA;
        }
    }

    private ImageIcon getMessageIcon(TpeMessage msg) {
        switch (msg.getMessageType()) {
            case INFO:
                return INFO_ICON;
            case ERROR:
                return ERROR_ICON;
            default:
            case WARNING:
                return WARNING_ICON;
        }
    }

    public synchronized void addMouseObserver(TpeMouseObserver obs) {
        if (!_mouseObs.contains(obs)) {
            _mouseObs.addElement(obs);
        }
    }

    /**
     * Tell all the mouse observers about the new mouse event.
     */
    private void _notifyMouseObs(MouseEvent e) {
        TpeMouseEvent tme = new TpeMouseEvent(e);
        try {
            _initMouseEvent(e, tme);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        for (final TpeMouseObserver mo : _mouseObs) {
            mo.tpeMouseEvent(this, tme);
        }
    }

    /**
     * Tell all the view observers that the view has changed.
     */
    protected void _notifyViewObs() {
        for (final TpeViewObserver vo : _viewObs) {
            vo.tpeViewChange(this);
        }
    }

    public synchronized void addViewObserver(TpeViewObserver obs) {
        if (!_viewObs.contains(obs)) {
            _viewObs.addElement(obs);
        }
    }

    public synchronized void deleteViewObserver(TpeViewObserver obs) {
        _viewObs.removeElement(obs);
    }


    // -- These implement the MouseInputListener interface --

    public void mousePressed(MouseEvent e) {
        _notifyMouseObs(e);
    }

    public void mouseDragged(MouseEvent e) {
        _notifyMouseObs(e);
    }

    public void mouseReleased(MouseEvent e) {
        _notifyMouseObs(e);
    }

    public void mouseMoved(MouseEvent e) {
        _notifyMouseObs(e);
    }

    public void mouseClicked(MouseEvent e) {
        _notifyMouseObs(e);
    }

    public void mouseEntered(MouseEvent e) {
        _notifyMouseObs(e);
    }

    public void mouseExited(MouseEvent e) {
        _notifyMouseObs(e);
    }


    /**
     * Is the TpeImageWidget initialized?
     */
    public boolean isImgInfoValid() {
        return _imgInfoValid;
    }


    public synchronized void addInfoObserver(TpeImageInfoObserver obs) {
        if (!_infoObs.contains(obs)) {
            _infoObs.addElement(obs);
        }
    }

    public synchronized void deleteInfoObserver(TpeImageInfoObserver obs) {
        _infoObs.removeElement(obs);
    }

    private void _notifyInfoObs() {
        final List<TpeImageInfoObserver> l;
        synchronized (_infoObs) {
            l = new ArrayList<>(_infoObs);
        }

        for (final TpeImageInfoObserver o : l) {
            o.imageInfoUpdate(this, _imgInfo);
        }
    }

    /**
     * Return true if there is valid image info, and try to update it if needed
     */
    private boolean _checkImgInfo() {
        if (!_imgInfoValid) {
            setBasePos(_imgInfo.getBasePos());
        }
        return _imgInfoValid;
    }


    /**
     * called when the image has changed to update the display
     */
    public synchronized void updateImage() {
        _imgInfoValid = false;
        super.updateImage();
        try {
            // might throw an exception if changing images and WCS is not yet initialized
            _notifyViewObs();
        } catch (Exception e) {
            // ignore: This happens when a new image was just loaded and WCS was not initialized yet
            // Retry again later in newImage(), below.
        }
    }


    /**
     * Convert the given user coordinates location to world coordinates.
     */
    public WorldCoords userToWorldCoords(double x, double y) {
        Point2D.Double p = new Point2D.Double(x, y);
        getCoordinateConverter().userToWorldCoords(p, false);
        return new WorldCoords(p.x, p.y, getCoordinateConverter().getEquinox());
    }

    /**
     * Convert the given world coordinate position to a user coordinates position.
     */
    public Point2D.Double worldToUserCoords(WorldCoords pos) {
        double[] raDec = pos.getRaDec(getCoordinateConverter().getEquinox());
        Point2D.Double p = new Point2D.Double(raDec[0], raDec[1]);
        getCoordinateConverter().worldToUserCoords(p, false);
        return p;
    }

    /**
     * Convert the given world coordinate position to screen coordinates.
     */
    public Point2D.Double worldToScreenCoords(WorldCoords pos) {
        double[] raDec = pos.getRaDec(getCoordinateConverter().getEquinox());
        Point2D.Double p = new Point2D.Double(raDec[0], raDec[1]);
        getCoordinateConverter().worldToScreenCoords(p, false);
        return p;
    }

    /**
     * Convert an offset from the base position (in arcsec) to a screen coordinates location.
     */
    public Point2D.Double offsetToScreenCoords(double xOff, double yOff) {
        if (!_checkImgInfo()) {
            return null;
        }

        double ppa = _imgInfo.getPixelsPerArcsec();
        Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        double xPix = baseScreenPos.x - (xOff * ppa * _imgInfo.flipRA());
        double yPix = baseScreenPos.y - (yOff * ppa);
        return skyRotate(xPix, yPix);
    }

    /**
     * Convert the given screen coordinates to an offset from the base position (in arcsec).
     */
    public double[] screenCoordsToOffset(double x, double y) {
        if (!_checkImgInfo()) {
            return null;
        }

        // un-rotate
        double angle = -_imgInfo.getCorrectedPosAngleRadians();
        Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        double xBase = baseScreenPos.x;
        double yBase = baseScreenPos.y;
        Point2D.Double pd = ScreenMath.rotateRadians(x, y, angle, xBase, yBase);

        double ppa = _imgInfo.getPixelsPerArcsec();
        double xOff = (baseScreenPos.x - pd.x) / (ppa * _imgInfo.flipRA());
        double yOff = (baseScreenPos.y - pd.y) / ppa;
        xOff = Math.round(xOff * 1000.0) / 1000.0;
        yOff = Math.round(yOff * 1000.0) / 1000.0;
        return new double[]{xOff, yOff};
    }

    /**
     * Convert a TaggedPos to a screen coordinates.
     */
    public Point2D.Double taggedPosToScreenCoords(WatchablePos tp) {
//        if (!tp.isValid()) return null;

        if (tp instanceof OffsetPosBase) {
            double x = ((OffsetPosBase) tp).getXaxis();
            double y = ((OffsetPosBase) tp).getYaxis();
            return offsetToScreenCoords(x, y);
        }

        // Get the equinox assumed by the coordinate conversion methods (depends on current image)
        //double equinox = getCoordinateConverter().getEquinox();
        ITarget target = ((SPTarget) tp).getTarget();
        final Option<Long> when = _ctx.schedulingBlockJava().map(SchedulingBlock::start);
        double x = target.getRaDegrees(when).getOrElse(0.0);
        double y = target.getDecDegrees(when).getOrElse(0.0);
        WorldCoords pos = new WorldCoords(x, y, 2000.);
        return worldToScreenCoords(pos);
    }


    /**
     * Rotate a point through the current position angle, relative to
     * the base position, correcting for sky rotation.
     */
    public Point2D.Double skyRotate(double x, double y) {
        if (!_checkImgInfo()) {
            return null;
        }

        double angle = _imgInfo.getCorrectedPosAngleRadians();
        Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        double xBase = baseScreenPos.x;
        double yBase = baseScreenPos.y;

        return ScreenMath.rotateRadians(x, y, angle, xBase, yBase);
    }

    /**
     * Rotate a polygon through the current position angle, relative to
     * the base position, correcting for sky rotation.
     */
    public void skyRotate(PolygonD p) {
        if (!_checkImgInfo()) {
            return;
        }

        double angle = _imgInfo.getCorrectedPosAngleRadians();
        Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        double xBase = baseScreenPos.x;
        double yBase = baseScreenPos.y;

        ScreenMath.rotateRadians(p, angle, xBase, yBase);
    }


    protected void _initMouseEvent(MouseEvent evt, TpeMouseEvent tme) {
        if (!_checkImgInfo()) return;

        Point2D.Double mp = new Point2D.Double(evt.getX(), evt.getY());

        // snap to catalog symbol position, if user clicked on one
        NamedCoordinates namedCoords = null; // object id and coordinates of a catalog symbol
        if (evt.getID() == MouseEvent.MOUSE_CLICKED) {
            namedCoords = getCatalogPosition(mp);
        }

        Point2D.Double p = new Point2D.Double(mp.x, mp.y);
        getCoordinateConverter().screenToUserCoords(p, false);
        if (namedCoords == null) {
            tme.pos = userToWorldCoords(p.x, p.y);
        } else {
            Coordinates coords = namedCoords.getCoordinates();
            if (coords instanceof WorldCoords) {
                tme.pos = (WorldCoords) coords;
                tme.name = namedCoords.getName();
                tme.setBrightness(namedCoords.getBrightness());
                tme.setSkyObject(namedCoords.getSkyObject());
            } else {
                tme.pos = userToWorldCoords(p.x, p.y);
            }
        }

        tme.id = evt.getID();
        tme.source = this;
        tme.xView = p.x;
        tme.yView = p.y;
        tme.xWidget = (int) Math.round(mp.x);
        tme.yWidget = (int) Math.round(mp.y);

        double[] d = screenCoordsToOffset(mp.x, mp.y);
        tme.xOffset = d[0];
        tme.yOffset = d[1];
    }

    /**
     * This method is called before and after a new image is loaded, each time
     * with a different argument.
     *
     * @param before set to true before the image is loaded and false afterwards
     */
    protected void newImage(boolean before) {
        super.newImage(before);
        if (!before) {
            try {
                _notifyViewObs();
            } catch (Exception e) {
                //die silently
            }
        }
    }


    public TpeContext getContext() {
        return _ctx;
    }

    /**
     * If the given screen coordinates point is within a displayed catalog symbol, set it to
     * point to the center of the symbol and return the world coordinates position
     * from the catalog table row. Otherwise, return null and do nothing.
     */
    protected NamedCoordinates getCatalogPosition(Point2D.Double p) {
        Navigator nav = getNavigator();
        if (nav == null) {
            return null;
        }
        TablePlotter plotter = nav.getPlotter();
        if (plotter == null) {
            return null;
        }
        return plotter.getCatalogPosition(p);
    }

    /**
     * Clear the image display.
     */
    public void clear() {
        LOG.finest("TpeImageWidget.clear()");

        if (_ctx.isEmpty()) {
            blankImage(0.0, 0.0);
        } else {
            WorldCoords basePos = _imgInfo.getBasePos();
            blankImage(basePos.getRaDeg(), basePos.getDecDeg());
        }
    }


    /**
     * Return true if the image has been cleared.
     * (Overrides parent class version to stop the table plotting code from
     * generating new blank images when plotting tables, since the blank
     * images are generated by OT code).
     */
    public boolean isClear() {
        return false;
    }

    /**
     * Display the FITS table at the given HDU index.
     */
    public void displayFITSTable(int hdu) {
        super.displayFITSTable(hdu);
        // make the navigator window pop up in this case
        super.showNavigatorFrame(null);
    }


    /**
     * Reset internal state to view a new observation and position table.
     */
    public void reset(TpeContext ctx) {
        LOG.finest("TpeImageWidget.reset()");

        if (_ctx.instrument().isDefined()) {
            _ctx.instrument().get().removePropertyChangeListener(this);
        }

        if (_ctx.targets().base().isDefined()) {
            _ctx.targets().base().get().deleteWatcher(this);
        }

        _ctx = ctx;

        if (_ctx.targets().base().isEmpty()) {
            // There is no target to view, but we need to update the image
            // widgets with new WCS info.
            clear();
        }

        // add new listeners
        if (_ctx.instrument().isDefined()) {
            _ctx.instrument().get().addPropertyChangeListener(this);
            setPosAngle(_ctx.instrument().get().getPosAngleDegrees());
        }

        if (_ctx.targets().base().isDefined()) {
            SPTarget base = _ctx.targets().base().get();
            base.addWatcher(this);
            basePosUpdate(base.getTarget());
        }

        repaint();
    }


    /**
     * Add the given image feature to the list.
     */
    public void addFeature(TpeImageFeature tif) {
        if (featureAdded(tif)) {
            return;
        }

        _featureList.addElement(tif);
        if (_imgInfoValid) {
            tif.reinit(this, _imgInfo);
        }
        repaint();
    }

    /**
     * Return true if the given image feature has been added already.
     */
    public final boolean featureAdded(TpeImageFeature tif) {
        return _featureList.contains(tif);
    }

    /**
     * Delete the given image feature from the list.
     */
    public void deleteFeature(TpeImageFeature tif) {
        if (!featureAdded(tif)) {
            return;
        }
        _featureList.removeElement(tif);
        tif.unloaded();
        repaint();
    }


    /**
     * Called when a mouse drag operation starts.
     */
    public void dragStart(TpeMouseEvent evt) {
        if ((!_imgInfoValid)) return;

        Object dragObject = null;
        for (final TpeImageFeature tif : _featureList) {
            if (tif instanceof TpeDraggableFeature) {
                final TpeDraggableFeature tdf = (TpeDraggableFeature) tif;

                final Option<Object> dragOpt = tdf.dragStart(evt, _imgInfo);
                if (dragOpt.isDefined()) {
                    dragObject = dragOpt.getValue();
                    _dragFeature = tdf;
                    drag(evt);
                    break;
                }
            }
        }

        if (dragObject == null) return;

        // Let anybody who wants to know about this drag know
        final Option<ObsContext> ctxOpt = getObsContext();
        if (ctxOpt.isDefined()) {
            final ObsContext ctx = ctxOpt.getValue();
            for (final TpeImageFeature tif : _featureList) {
                if (tif instanceof TpeDragSensitive) {
                    ((TpeDragSensitive) tif).handleDragStarted(dragObject, ctx);
                }
            }
        }
    }

    /**
     * Called while dragging the mouse over the image.
     */
    public void drag(TpeMouseEvent evt) {
        if (_dragFeature == null) {
            return;
        }
        _dragFeature.drag(evt);
    }

    /**
     * Called at the end of a mouse drag operation.
     */
    public void dragStop(TpeMouseEvent evt) {
        if (_dragFeature == null) {
            return;   // Weren't dragging anything
        }

        _dragFeature.dragStop(evt);
        _dragFeature = null;

        // Let anybody who wants to know about this drag know
        Option<ObsContext> ctxOpt = getObsContext();
        if (!ctxOpt.isEmpty()) {
            ObsContext ctx = ctxOpt.getValue();
            for (TpeImageFeature tif : _featureList) {
                if (tif instanceof TpeDragSensitive) {
                    ((TpeDragSensitive) tif).handleDragStopped(ctx);
                }
            }
        }

    }

    public void action(TpeMouseEvent tme) {
        if (!_imgInfoValid) return;
        for (TpeImageFeature tif : _featureList) {
            if (tif instanceof TpeActionableFeature) {
                ((TpeActionableFeature) tif).action(tme);
            }
        }
    }

    /**
     * Create an image feature item, based on the given arguments, and return true if successful.
     */
    public void create(TpeMouseEvent tme, TpeCreateableItem item) {
        if (!_imgInfoValid) return;
        item.create(tme, _imgInfo);
    }

    /**
     * Erase the image feature at the mouse position.
     */
    public boolean erase(TpeMouseEvent tme) {
        if (!_imgInfoValid) return false;

        for (final TpeImageFeature tif : _featureList) {
            if (tif instanceof TpeEraseableFeature) {
                final TpeEraseableFeature tef = (TpeEraseableFeature) tif;
                if (tef.erase(tme)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Implements the PropertyChangeListener interface
     */
    public void propertyChange(PropertyChangeEvent evt) {
        _checkPosAngle();
        repaint();
    }

    public void telescopePosUpdate(WatchablePos tp) {
        basePosUpdate(((SPTarget) tp).getTarget());
    }

    /**
     * The Base position has been updated.
     */
    public void basePosUpdate(ITarget target) {
        final Option<Long> when = _ctx.schedulingBlockJava().map(SchedulingBlock::start);
        double x = target.getRaDegrees(when).getOrElse(0.0);
        double y = target.getDecDegrees(when).getOrElse(0.0);
        WorldCoords pos = new WorldCoords(x, y, 2000.);
        setBasePos(pos);
        repaint();
    }

    /**
     * Gets the context of the current observation.
     */
    public Option<ObsContext> getObsContext() {
        return _ctx.obsContextJava();
    }

    /**
     * Gets the context of the current observation (ignoring site conditions
     * if not present)
     */
    public Option<ObsContext> getMinimalObsContext() {
        Option<ObsContext> fullContext = getObsContext();
        // check if "full" context is available
        if (fullContext.isEmpty()) {
            // UX-1012: no -> try to return a context that contains the information needed for drawing only
            return getMinimalDrawingObsContext();
        } else {
            // yes -> return the full context (this should be the normal case)
            return fullContext;
        }
    }

    /**
     * Gets a minimal context for drawing that does not bother with information that is not needed for drawing
     * like the conditions for example. This method will provide a context useful for drawing when there is no
     * condition node. This solves issue UX-1012.
     */
    private Option<ObsContext> getMinimalDrawingObsContext() {
        return _ctx.obsContextJavaWithConditions(SPSiteQuality.Conditions.WORST);
    }

    public boolean isViewingOffsets() {
        return _viewingOffsets;
    }

    public void setViewingOffsets(boolean viewingOffsets) {
        _viewingOffsets = viewingOffsets;
    }

    // Check if the instrument's position angle has changed and update the _imgInfo object
    private void _checkPosAngle() {
        if (_ctx.instrument().isDefined()) {
            final double d = _ctx.instrument().get().getPosAngleDegrees();
            if (d != _imgInfo.getPosAngleDegrees()) {
                _imgInfo.setPosAngleDegrees(d);
                _notifyInfoObs();

                for (final TpeImageFeature tif : _featureList) {
                    tif.posAngleUpdate(_imgInfo);
                }
            }
        }
    }

    /**
     * Set the position angle in degrees.
     */
    public boolean setPosAngle(double posAngle) {
        final SPInstObsComp inst = _ctx.instrument().orNull();
        final Double d = _ctx.instrument().posAngleOrZero();
        if ((d != posAngle) && (inst != null)) {
            inst.setPosAngle(posAngle);
        }

        if (!getCoordinateConverter().isWCS()) {
            _imgInfoValid = false;
            return false;
        }
        _imgInfo.setPosAngleDegrees(posAngle);
        _notifyInfoObs();

        for (final TpeImageFeature tif : _featureList) {
            tif.posAngleUpdate(_imgInfo);
        }
        return true;
    }

    /**
     * Return the instrument node corresponding to the currently selected node
     */
    public SPInstObsComp getInstObsComp() {
        if (_ctx.instrument().isDefined()) return _ctx.instrument().get();
        else return null;
    }

    /**
     * Set the base position to the given coordinates (overrides parent class version).
     */
    public boolean setBasePos(WorldCoords pos) {
        _basePos = pos;

        CoordinateConverter cc = getCoordinateConverter();
        if (!cc.isWCS()) {
            _imgInfoValid = false;
            return false;
        }

        try {
            _imgInfo.setBaseScreenPos(worldToScreenCoords(pos));
            _imgInfo.setBasePos(pos);
        } catch (Exception e) {
            return false;
        }

        // Get two points, one at the base and one an arcmin north of the base
        Point2D.Double temp1 = worldToUserCoords(pos);
        Point2D.Double temp2 = worldToUserCoords(
                new WorldCoords(pos.getRaDeg(), pos.getDecDeg() + 0.01666667));

        // Get the difference in x,y and the distance between the two points
        double xdPrime = temp2.x - temp1.x;
        double ydPrime = temp2.y - temp1.y;

        // Measure theta from the y axis:  ie. a cartesian coordinate system
        // rotated by 90 degrees.
        double theta = Angle.atanRadians(xdPrime / ydPrime);
        if (ydPrime > 0) {
            theta = Angle.normalizeRadians(theta + Math.PI);
        }

        if (Angle.almostZeroRadians(theta)) {
            theta = 0.;
        }

        _imgInfo.setTheta(theta);


        // Convert the two points to pixel coordinates on the screen
        cc.userToScreenCoords(temp1, false);
        cc.userToScreenCoords(temp2, false);

        // Get the difference in x,y pixels between the two points
        double xiPrime = temp2.x - temp1.x;
        double yiPrime = temp2.y - temp1.y;
        double r = Math.sqrt(xiPrime * xiPrime + yiPrime * yiPrime);

        // Divide the 1 min distance by 60 arcsec to get pixels/perArcsec
        _imgInfo.setPixelsPerArcsec(r / 60.0);

        // Find out where WCS East is, so that we know which way the position angle increases
        double angle = Math.PI / 2.;
        Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        double xBase = baseScreenPos.x;
        double yBase = baseScreenPos.y;
        Point2D.Double east = ScreenMath.rotateRadians(temp2.x, temp2.y, angle, xBase,
                yBase);

        try {
            cc.screenToWorldCoords(east, false);
        } catch (Exception e) {
            _imgInfoValid = false;
            return false;
        }

        // Handle 360 / 0 wrap
        double eastX = east.x;
        double posRa = pos.getRaDeg();
        if (Math.abs(Math.round(eastX) - Math.round(posRa)) == 360) {
            if (Math.round(east.x) == 0) {
                eastX += 360.0;
            } else {
                posRa += 360.0;
            }
        }
        _imgInfo.setFlipRA(eastX < posRa);

        _imgInfoValid = true;
        _notifyInfoObs();

        for (final TpeImageFeature tif : _featureList) {
            tif.reinit(this, _imgInfo);
        }

        _baseOutOfView = false;
        if (_imgInfoValid) {
            Point2D.Double p = _imgInfo.getBaseScreenPos();
            if ((p.x < 0) || (p.y < 0) || (p.x >= getWidth()) || (p.y >= getHeight())) {
                _baseOutOfView = true;
            }
        }
        return true;
    }


    /**
     * Return the base or center position in world coordinates.
     * If there is no base position, this method returns the center point
     * of the image. If the image does not support WCS, this method returns (0,0).
     * The position returned here should be used as the base position
     * for any catalog or image server requests.
     */
    public WorldCoords getBasePos() {
        if (_imgInfoValid && !_ctx.isEmpty()) {
            return _basePos;
        }
        return super.getBasePos();
    }

    /**
     * Return the default min and max search radius to use for catalog searches, in arcmin.
     *
     * @param centerPos    the center position for the radius
     * @param useImageSize if true, use the image size to get the search radius, otherwise use the
     *                     current WFS algorithm, if set
     * @return radius values
     */
    public RadiusLimits getDefaultSearchRadius(WorldCoords centerPos, boolean useImageSize) {
        if (useImageSize) {
            // If the user pressed the "Set from Image" button, then stop using the algorithm
            _algorithm = null;
        } else if (_algorithm != null) {
            CatalogSearchParameters params = _algorithm.getParameters();
            return params.getRadiusLimits();
        }
        return super.getDefaultSearchRadius(centerPos, useImageSize);
    }

    /**
     * Return the default min and max magnitude values to use for catalog searches, or null
     * if there is no default.
     *
     * @return mag values and band
     */
    public MagnitudeLimits getDefaultSearchMagRange() {
        if (_algorithm != null) {
            CatalogSearchParameters params = _algorithm.getParameters();
            _algorithm = null; // XXX hack to reset to default after guide star search

            SPSiteQuality sq = _ctx.siteQuality().orNull();
            if (sq != null) {
                final SPSiteQuality.Conditions conditions = sq.conditions();
                return MagnitudeConstraints.conditionsAdjustmentForJava(params.getMagnitudeLimits(), conditions);
            }
            return params.getMagnitudeLimits();
        }
        return super.getDefaultSearchMagRange();
    }

    /**
     * Set the default catalog algorithm to use for catalog searches
     */
    public void setCatalogAlgorithm(ICatalogAlgorithm algorithm) {
        _algorithm = algorithm;
    }

    // manual guide star selection dialog
    public void manualGuideStarSearch() {
        if (GuideStarSupport.hasGemsComponent(_ctx)) {
            showGemsGuideStarSearchDialog();
        } else {
            QueryResultsFrame.instance().showOn(_ctx.obsContext());
        }
    }

    private void showGemsGuideStarSearchDialog() {
        if (_gemsGuideStarSearchDialog == null) {
            _gemsGuideStarSearchDialog = new GemsGuideStarSearchDialog();
        } else {
            _gemsGuideStarSearchDialog.reset();
            _gemsGuideStarSearchDialog.setVisible(true);
        }
    }

    /**
     * Return the action that displays the guide star dialog
     */
    public AbstractAction getManualGuideStarAction() {
        return _manualGuideStarAction;
    }

    public AbstractAction getSkyImageAction() {
        return _skyImageAction;
    }
}

