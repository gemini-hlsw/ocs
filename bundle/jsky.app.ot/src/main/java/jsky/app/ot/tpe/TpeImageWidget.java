package jsky.app.ot.tpe;

import edu.gemini.catalog.ui.QueryResultsFrame;
import edu.gemini.catalog.ui.tpe.CatalogImageDisplay;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Coordinates;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.*;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;
import edu.gemini.spModel.util.Angle;
import jsky.app.ot.tpe.gems.GemsGuideStarSearchDialog;
import jsky.app.ot.util.OtColor;
import jsky.app.ot.util.PolygonD;
import jsky.util.gui.Resources;
import jsky.app.ot.util.ScreenMath;
import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.gui.PickObject;
import jsky.image.gui.PickObjectStatistics;
import jsky.navigator.NavigatorPane;
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
public class TpeImageWidget extends CatalogImageDisplay implements MouseInputListener,
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

    // Dialog for GeMS manual guide star selection
    private GemsGuideStarSearchDialog _gemsGuideStarSearchDialog;

    // Action to use to show the guide star search window
    private final AbstractAction _manualGuideStarAction = new AbstractAction(
            "Manual GS",
            Resources.getIcon("gsmanual.png")) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Query a guide star catalog, review candidates, and select");
        }

        @Override public void actionPerformed(ActionEvent evt) {
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
    public TpeImageWidget(final Component parent) {
        super(parent, new NavigatorPane());

        addMouseListener(this);
        addMouseMotionListener(this);
    }


    /**
     * Return true if this is the main application window (enables exit menu item)
     */
    @Override
    public boolean isMainWindow() {
        return false; // grumble
    }


    /**
     * Overrides the base class version to add the OT graphics.
     *
     * @param g      the graphics context
     * @param region if not null, the region to paint
     */
    @Override
    public synchronized void paintLayer(final Graphics2D g, final Rectangle2D region) {
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

    private void displayMessages(final java.util.List<TpeMessage> messages, final Graphics2D g) {
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

    private Color getMessageColor(final TpeMessage msg) {
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

    private ImageIcon getMessageIcon(final TpeMessage msg) {
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

    synchronized void addMouseObserver(final TpeMouseObserver obs) {
        if (!_mouseObs.contains(obs)) {
            _mouseObs.addElement(obs);
        }
    }

    /**
     * Tell all the mouse observers about the new mouse event.
     */
    private void _notifyMouseObs(final MouseEvent e) {
        try {
            final TpeMouseEvent tme = _initMouseEvent(e);
            for (final TpeMouseObserver mo : _mouseObs) {
                mo.tpeMouseEvent(this, tme);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Tell all the view observers that the view has changed.
     */
    private void _notifyViewObs() {
        for (final TpeViewObserver vo : _viewObs) {
            vo.tpeViewChange(this);
        }
    }

    synchronized void addViewObserver(final TpeViewObserver obs) {
        if (!_viewObs.contains(obs)) {
            _viewObs.addElement(obs);
        }
    }

    synchronized void deleteViewObserver(final TpeViewObserver obs) {
        _viewObs.removeElement(obs);
    }

    @Override
    public void loadSkyImage() {
        try {
            TelescopePosEditor tpe = TpeManager.get();
            if (tpe != null) {
                tpe.getSkyImage(_ctx);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }


    // -- These implement the MouseInputListener interface --

    @Override public void mousePressed(final MouseEvent e) {
        _notifyMouseObs(e);
    }

    @Override public void mouseDragged(final MouseEvent e) {
        _notifyMouseObs(e);
    }

    @Override public void mouseReleased(final MouseEvent e) {
        _notifyMouseObs(e);
    }

    @Override public void mouseMoved(final MouseEvent e) {
        _notifyMouseObs(e);
    }

    @Override public void mouseClicked(final MouseEvent e) {
        _notifyMouseObs(e);
    }

    @Override public void mouseEntered(final MouseEvent e) {
        _notifyMouseObs(e);
    }

    @Override public void mouseExited(final MouseEvent e) {
        _notifyMouseObs(e);
    }


    /**
     * Is the TpeImageWidget initialized?
     */
    boolean isImgInfoValid() {
        return _imgInfoValid;
    }


    public synchronized void addInfoObserver(final TpeImageInfoObserver obs) {
        if (!_infoObs.contains(obs)) {
            _infoObs.addElement(obs);
        }
    }

    public synchronized void deleteInfoObserver(final TpeImageInfoObserver obs) {
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
    private boolean _isImageValid() {
        if (!_imgInfoValid) {
            setBasePos(_imgInfo.getBasePos());
        }
        return _imgInfoValid;
    }


    /**
     * called when the image has changed to update the display
     */
    @Override
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
    private WorldCoords userToWorldCoords(final double x, final double y) {
        final Point2D.Double p = new Point2D.Double(x, y);
        getCoordinateConverter().userToWorldCoords(p, false);
        return new WorldCoords(p.x, p.y, getCoordinateConverter().getEquinox());
    }

    /**
     * Convert the given world coordinate position to a user coordinates position.
     */
    private Point2D.Double worldToUserCoords(final WorldCoords pos) {
        final double[] raDec = pos.getRaDec(getCoordinateConverter().getEquinox());
        final Point2D.Double p = new Point2D.Double(raDec[0], raDec[1]);
        getCoordinateConverter().worldToUserCoords(p, false);
        return p;
    }

    /**
     * Convert the given world coordinate position to screen coordinates.
     */
    private Point2D.Double worldToScreenCoords(final WorldCoords pos) {
        final double[] raDec = pos.getRaDec(getCoordinateConverter().getEquinox());
        final Point2D.Double p = new Point2D.Double(raDec[0], raDec[1]);
        getCoordinateConverter().worldToScreenCoords(p, false);
        return p;
    }

    /**
     * Convert an offset from the base position (in arcsec) to a screen coordinates location.
     */
    public Point2D.Double offsetToScreenCoords(final double xOff, final double yOff) {
        if (!_isImageValid()) {
            return null;
        }

        final double ppa = _imgInfo.getPixelsPerArcsec();
        final Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        final double xPix = baseScreenPos.x - (xOff * ppa * _imgInfo.flipRA());
        final double yPix = baseScreenPos.y - (yOff * ppa);
        return skyRotate(xPix, yPix);
    }

    /**
     * Convert the given screen coordinates to an offset from the base position (in arcsec).
     */
    private double[] screenCoordsToOffset(final double x, final double y) {
        if (!_isImageValid()) {
            return null;
        }

        // un-rotate
        final double angle = -_imgInfo.getCorrectedPosAngleRadians();
        final Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        final double xBase = baseScreenPos.x;
        final double yBase = baseScreenPos.y;
        final Point2D.Double pd = ScreenMath.rotateRadians(x, y, angle, xBase, yBase);

        final double ppa = _imgInfo.getPixelsPerArcsec();
        double xOff = (baseScreenPos.x - pd.x) / (ppa * _imgInfo.flipRA());
        double yOff = (baseScreenPos.y - pd.y) / ppa;
        xOff = Math.round(xOff * 1000.0) / 1000.0;
        yOff = Math.round(yOff * 1000.0) / 1000.0;
        return new double[]{xOff, yOff};
    }

    /**
     * Convert a TaggedPos to a screen coordinates.
     */
    Point2D.Double taggedPosToScreenCoords(final WatchablePos tp) {
        if (tp instanceof OffsetPosBase) {
            final double x = ((OffsetPosBase) tp).getXaxis();
            final double y = ((OffsetPosBase) tp).getYaxis();
            return offsetToScreenCoords(x, y);
        }

        // Get the equinox assumed by the coordinate conversion methods (depends on current image)
        final SPTarget target = (SPTarget) tp;
        final Option<Long> when = _ctx.schedulingBlockStartJava();
        final double x = target.getRaDegrees(when).getOrElse(0.0);
        final double y = target.getDecDegrees(when).getOrElse(0.0);
        final WorldCoords pos = new WorldCoords(x, y, 2000.);
        return worldToScreenCoords(pos);
    }


    /**
     * Rotate a point through the current position angle, relative to
     * the base position, correcting for sky rotation.
     */
    private Point2D.Double skyRotate(final double x, final double y) {
        if (!_isImageValid()) {
            return null;
        }

        final double angle = _imgInfo.getCorrectedPosAngleRadians();
        final Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        final double xBase = baseScreenPos.x;
        final double yBase = baseScreenPos.y;

        return ScreenMath.rotateRadians(x, y, angle, xBase, yBase);
    }

    /**
     * Rotate a polygon through the current position angle, relative to
     * the base position, correcting for sky rotation.
     */
    public void skyRotate(final PolygonD p) {
        if (!_isImageValid()) {
            return;
        }

        final double angle = _imgInfo.getCorrectedPosAngleRadians();
        final Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        final double xBase = baseScreenPos.x;
        final double yBase = baseScreenPos.y;

        ScreenMath.rotateRadians(p, angle, xBase, yBase);
    }


    private TpeMouseEvent _initMouseEvent(final MouseEvent evt) {
        if (_isImageValid()) {
            final Point2D.Double mp = new Point2D.Double(evt.getX(), evt.getY());

            // snap to catalog symbol position, if user clicked on one
            final Point2D.Double p = new Point2D.Double(mp.x, mp.y);
            getCoordinateConverter().screenToUserCoords(p, false);
            final double[] d = screenCoordsToOffset(mp.x, mp.y);
            final Option<TpeImageWidget> source = new Some<>(this);

            if (evt.getID() == MouseEvent.MOUSE_CLICKED) {
                final Option<SiderealTarget> skyObject = getCatalogPosition(mp);
                final Option<String> name = skyObject.map(SiderealTarget::name);
                if (!skyObject.isDefined()) {
                    Coordinates pos = CoordinatesUtilities.userToWorldCoords(getCoordinateConverter(), p.x, p.y);
                    return new TpeMouseEvent(evt, evt.getID(), source, pos, name, (int) Math.round(mp.x), (int) Math.round(mp.y), skyObject, d[0], d[1]);
                } else {
                    Coordinates pos = skyObject.map(SiderealTarget::coordinates).getOrElse(Coordinates.zero());
                    return new TpeMouseEvent(evt, evt.getID(), source, pos, name, (int) Math.round(mp.x), (int) Math.round(mp.y), skyObject, d[0], d[1]);
                }
            } else {
                Coordinates pos = CoordinatesUtilities.userToWorldCoords(getCoordinateConverter(), p.x, p.y);
                return new TpeMouseEvent(evt, evt.getID(), source, pos, None.instance(), (int) Math.round(mp.x), (int) Math.round(mp.y), None.instance(), d[0], d[1]);
            }
        } else {
            return new TpeMouseEvent(evt);
        }
    }

    /**
     * This method is called before and after a new image is loaded, each time
     * with a different argument.
     *
     * @param before set to true before the image is loaded and false afterwards
     */
    @Override
    public void newImage(final boolean before) {
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
    private Option<SiderealTarget> getCatalogPosition(final Point2D.Double p) {
        return plotter().getCatalogObjectAt(p);
    }

    /**
     * Clear the image display.
     */
    @Override
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
    @Override
    public boolean isClear() {
        return false;
    }

    /**
     * Display the FITS table at the given HDU index.
     */
    @Override
    public void displayFITSTable(final int hdu) {
        super.displayFITSTable(hdu);
    }

    /**
     * Reset internal state to view a new observation and position table.
     */
    public void reset(final TpeContext ctx) {
        LOG.finest("TpeImageWidget.reset()");

        if (_ctx.instrument().isDefined()) {
            _ctx.instrument().get().removePropertyChangeListener(this);
        }

        if (_ctx.targets().asterism().isDefined()) {
            _ctx.targets().asterism().get().allSpTargetsJava().foreach(a -> a.deleteWatcher(this));
        }

        _ctx = ctx;

        if (_ctx.targets().asterism().isEmpty()) {
            // There is no target to view, but we need to update the image
            // widgets with new WCS info.
            clear();
        }

        // add new listeners
        if (_ctx.instrument().isDefined()) {
            // This is bad but it is the only way to link changes from the instrument
            // to the dialog box, talk about side-effects
            if (_gemsGuideStarSearchDialog != null) {
                _gemsGuideStarSearchDialog.updatedInstrument(ctx.instrument());
            }
            _ctx.instrument().get().addPropertyChangeListener(this);
            setPosAngle(_ctx.instrument().get().getPosAngleDegrees());
        }

        if (_ctx.targets().asterism().isDefined()) {
            for (final SPTarget base: _ctx.targets().asterism().get().allSpTargetsJava()) {
              base.addWatcher(this);
            }
        }
        resetBaseFromAsterism(); // ok even if asterism is undefined (goes to 0,0 in this case)

        repaint();
    }

    /**
     * Add the given image feature to the list.
     */
    void addFeature(final TpeImageFeature tif) {
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
    private boolean featureAdded(final TpeImageFeature tif) {
        return _featureList.contains(tif);
    }

    /**
     * Delete the given image feature from the list.
     */
    void deleteFeature(final TpeImageFeature tif) {
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
    public void dragStart(final TpeMouseEvent evt) {
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
    public void drag(final TpeMouseEvent evt) {
        if (_dragFeature == null) {
            return;
        }
        _dragFeature.drag(evt);
    }

    /**
     * Called at the end of a mouse drag operation.
     */
    public void dragStop(final TpeMouseEvent evt) {
        if (_dragFeature == null) {
            return;   // Weren't dragging anything
        }

        _dragFeature.dragStop(evt);
        _dragFeature = null;

        // Let anybody who wants to know about this drag know
        final Option<ObsContext> ctxOpt = getObsContext();
        if (!ctxOpt.isEmpty()) {
            final ObsContext ctx = ctxOpt.getValue();
            _featureList.stream().filter(tif -> tif instanceof TpeDragSensitive).forEach(tif ->
                ((TpeDragSensitive) tif).handleDragStopped(ctx)
            );
        }

    }

    public void action(final TpeMouseEvent tme) {
        if (!_imgInfoValid) return;
        _featureList.stream().filter(tif -> tif instanceof TpeActionableFeature).forEach(tif ->
                        ((TpeActionableFeature) tif).action(tme)
        );
    }

    /**
     * Create an image feature item, based on the given arguments, and return true if successful.
     */
    public void create(final TpeMouseEvent tme, final TpeCreatableItem item) {
        if (!_imgInfoValid) return;
        item.create(tme, _imgInfo);
    }

    /**
     * Erase the image feature at the mouse position.
     */
    public boolean erase(final TpeMouseEvent tme) {
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
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        _checkPosAngle();
        repaint();
    }

    @Override
    public void telescopePosUpdate(final WatchablePos unused) {
      resetBaseFromAsterism();
    }

    /** Reset our base position from the context asterism if any (zenith otherwise) and repaint. */
    private void resetBaseFromAsterism() {
        final Asterism asterism = _ctx.targets().asterismOrZero();
        final Option<Long> when = _ctx.schedulingBlockStartJava();
        final double x = asterism.getRaDegrees(when).getOrElse(0.0);
        final double y = asterism.getDecDegrees(when).getOrElse(0.0);
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
        final Option<ObsContext> fullContext = getObsContext();
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
    public boolean setPosAngle(final double posAngle) {
        final SPInstObsComp inst = _ctx.instrument().orNull();
        final Double d = _ctx.instrument().posAngleOrZero();
        if ((d != posAngle) && (inst != null)) {
            inst.setPosAngle(posAngle);
            if (inst instanceof PosAngleConstraintAware) {
                final PosAngleConstraintAware pacInst = (PosAngleConstraintAware) inst;
                if (pacInst.getPosAngleConstraint() == PosAngleConstraint.PARALLACTIC_ANGLE)
                    pacInst.setPosAngleConstraint(PosAngleConstraint.PARALLACTIC_OVERRIDE);
            }
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
    private boolean setBasePos(final WorldCoords pos) {
        _basePos = pos;

        final CoordinateConverter cc = getCoordinateConverter();
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
        final Point2D.Double temp1 = worldToUserCoords(pos);
        final Point2D.Double temp2 = worldToUserCoords(
                new WorldCoords(pos.getRaDeg(), pos.getDecDeg() + 0.01666667));

        // Get the difference in x,y and the distance between the two points
        final double xdPrime = temp2.x - temp1.x;
        final double ydPrime = temp2.y - temp1.y;

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
        final double xiPrime = temp2.x - temp1.x;
        final double yiPrime = temp2.y - temp1.y;
        final double r = Math.sqrt(xiPrime * xiPrime + yiPrime * yiPrime);

        // Divide the 1 min distance by 60 arcsec to get pixels/perArcsec
        _imgInfo.setPixelsPerArcsec(r / 60.0);

        // Find out where WCS East is, so that we know which way the position angle increases
        final double angle = Math.PI / 2.;
        final Point2D.Double baseScreenPos = _imgInfo.getBaseScreenPos();
        final double xBase = baseScreenPos.x;
        final double yBase = baseScreenPos.y;
        final Point2D.Double east = ScreenMath.rotateRadians(temp2.x, temp2.y, angle, xBase,
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

        _baseOutOfView = _imgInfoValid && !isVisible(_imgInfo.getBaseScreenPos());
        return true;
    }

    /**
     * @return <code>true</code> if the point is visible in the image widget;
     * <code>false</code> otherwise
     */
    public boolean isVisible(Point2D.Double p) {
        return (p.x >= 0) && (p.y >= 0) && (p.x < getWidth()) && (p.y < getHeight());
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

    // manual guide star selection dialog
    public void manualGuideStarSearch() {
        if (GuideStarSupport.hasGemsComponent(_ctx)) {
            showGemsGuideStarSearchDialog();
        } else {
            openCatalogNavigator();
        }
    }

    private void openCatalogNavigator() {
        QueryResultsFrame.instance().showOn(this, _ctx);
    }

    private void showGemsGuideStarSearchDialog() {
        if (_gemsGuideStarSearchDialog == null) {
            _gemsGuideStarSearchDialog = new GemsGuideStarSearchDialog(this, scala.concurrent.ExecutionContext$.MODULE$.global());
        } else {
            _gemsGuideStarSearchDialog.reset();
            _gemsGuideStarSearchDialog.setVisible(true);
        }
        try {
            _gemsGuideStarSearchDialog.query();
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Return the action that displays the guide star dialog
     */
    AbstractAction getManualGuideStarAction() {
        return _manualGuideStarAction;
    }

    @Override
    public void pickedObject() {
        super.pickedObject();
        PickObject pickObject = getPickObjectPanel();
        PickObjectStatistics stats = pickObject.getStatistics();
        if (stats != null) {
            WorldCoords coords = stats.getCenterPos();
            TargetObsComp obsComp = getContext().targets().orNull();

            if (obsComp != null && !pickObject.isUpdate()) {
                SPTarget newTarget = new SPTarget(coords.getRaDeg(), coords.getDecDeg());
                if (stats.getRow().size() > 0 && stats.getRow().elementAt(0) != null) {
                    newTarget.setName(stats.getRow().elementAt(0).toString());
                }
                TargetEnvironment te = obsComp.getTargetEnvironment().setUserTargets(obsComp.getTargetEnvironment().getUserTargets().append(newTarget));
                obsComp.setTargetEnvironment(te);
                getContext().targets().commit();
            }
        } else {
            DialogUtil.error("No object was selected");
        }
    }
}

