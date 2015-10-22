package jsky.app.ot.tpe;

import edu.gemini.skycalc.Angle;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPosSelection;
import jsky.app.ot.util.BasicPropertyList;
import jsky.util.gui.DialogUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

/**
 * TpeImageFeature is a class used by the Position Editor to manipulate the
 * many potential image features that may be drawn.  The Position Editor
 * classes use the TpeImageFeature interface rather than having to know
 * about individual subclasses.  For instance, the TpeImageWidget has
 * methods to add and remove TpeImageFeatures.  When the image widget is
 * painted, it simply loops through its list of image features calling the
 * <code>draw()</code> method on each.
 */
public abstract class TpeImageFeature implements TelescopePosWatcher {

    /**
     * This is the size of the width and height, or radius, of the item.
     * (How big are items that don't depend upon the scale of the image?)
     */
    public static final int MARKER_SIZE = PosMap.MARKER_SIZE;

    /** Font used to draw text items.  */
    public static final Font FONT = new Font("dialog", Font.ITALIC, 10);

    /** The image widget in which to draw. */
    protected TpeImageWidget _iw;

    /** Contains information about the base position */
    protected TpeImageInfo _tii;

    /** Set to -1 if RA axis is reversed */
    protected double _flipRA;

    /** Whether the feature is being drawn. */
    protected boolean _isVisible;

    /** The feature's name. */
    protected final String _name;

    /** The feature's description. */
    protected final String _description;

    /**
     * Instantiate a TpeImageFeature from a fully qualified class name.
     */
    public static TpeImageFeature createFeature(Class<?> clazz) {
        TpeImageFeature tif = null;
        try {
            tif = (TpeImageFeature) clazz.newInstance();
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
        return tif;
    }

    /**
     * Create with a short name and a longer description.
     */
    public TpeImageFeature(String name, String descr) {
        _name = name;
        _description = descr;
    }

    /**
     * Reinitialize.  Override if additional initialization is required.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        _iw = iw;
        _tii = tii;
        _isVisible = true;
        _flipRA = tii.flipRA();
    }

    /**
     * Reinitialize with the previously used arguments.
     */
    public void reinit() {
        reinit(_iw, _tii);
    }

    public TpeContext getContext() {
        return (_iw == null) ? TpeContext.empty() : _iw.getContext();
    }

    /**
     * Return true if the mouse pointer is over the image feature.
     */
    public boolean isMouseOver(TpeMouseEvent tme) {
        return false;  // redefined in a derived class
    }


    /**
     * The position angle has been updated.  Override if this is important
     * to the feature.
     */
    public void posAngleUpdate(TpeImageInfo tii) {
    }

    /**
     * Receive notification that the feature has been unloaded.  Subclasses
     * should override if interrested, but call super.unloaded().
     */
    public void unloaded() {
        _isVisible = false;
    }

    /**
     * Is this feature currently visible?
     */
    public boolean isVisible() {
        return _isVisible;
    }

    /**
     * Get the feature's name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Get the feature's description.
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Get the property set assoicated with this feature.  Image features
     * may support a property set, which can be used to provide a means
     * to configure their display.  Unless overriden, this method simply
     * returns null, indicating that there are no configurable properties.
     *
     * @see jsky.app.ot.util.BasicPropertyList
     */
    public BasicPropertyList getProperties() {
        return null;
    }

    /**
     * Draw the feature.
     */
    public abstract void draw(Graphics g, TpeImageInfo tii);

    // -- Utility methods for monitoring telescope positions --

    private final PropertyChangeListener targetEnvListener = evt -> {
        TargetEnvironment oldEnv = (TargetEnvironment) evt.getOldValue();
        TargetEnvironment newEnv = (TargetEnvironment) evt.getNewValue();
        handleTargetEnvironmentUpdate(TargetEnvironmentDiff.all(oldEnv, newEnv));
    };

    /** Arrange to be notified if the PWFS targets are added, removed, or selected */
    protected void _monitorPosList() {
        if (_iw != null) {
            final TargetObsComp obsComp = _iw.getContext().targets().orNull();
            if (obsComp != null) {
                obsComp.removePropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetEnvListener);
                obsComp.addPropertyChangeListener(TargetObsComp.TARGET_ENV_PROP, targetEnvListener);
            }
        }
    }


    /**
     * Arrange to be notified if an offset position is selected (so that we can
     * use the associated guide star settings). If a new observation is selected,
     * reset the offset position selections, to avoid confusion, since there can
     * be more than one offset node, making it is hard to determine which selection
     * to use later on.
     */
    protected void _monitorOffsetSelections(PropertyChangeListener listener) {
        if (_iw != null) {
            TpeContext ctx = _iw.getContext();
            for (SingleOffsetListContext olc : ctx.offsets().allJava()) {
                OffsetPosSelection.listenTo(olc.shell().get(), listener);
            }
        }
    }
    protected void _stopMonitorOffsetSelections(PropertyChangeListener listener) {
        if (_iw != null) {
            TpeContext ctx = _iw.getContext();
            for (SingleOffsetListContext olc : ctx.offsets().allJava()) {
                OffsetPosSelection.deafTo(olc.shell().get(), listener);
            }
        }
    }

    public void telescopePosUpdate(WatchablePos tp) {
    }

    protected void handleTargetEnvironmentUpdate(TargetEnvironmentDiff diff) {
        // hook for subclasses
    }

    public boolean isEnabled(TpeContext ctx) {
        return !ctx.isEmpty() && ctx.targets().isDefined();
    }

    public boolean isEnabledByDefault() {
        return false;
    }

    /**
     * Gets the key, or legend, associated with this image feature.  For
     * example to explain the meaning of one or more elements that it draws.
     * This method returns {@link None} by default.  A subclass must override
     * to return a specific legend.
     *
     * @return {@link None}; override to provide a component that contains the
     * legend
     */
    public Option<Component> getKey() {
        // hook for subclasses
        return None.instance();
    }

    /**
     * Gets this feature's category, which is used for separating the categories
     * in the tool button display.
     */
    public abstract TpeImageFeatureCategory getCategory();

    /**
     * An override hook for image features that need to display
     * messages.  This method returns {@link None}.  Override to return
     * messages when needed.
     */
    public Option<Collection<TpeMessage>> getMessages() {
        return None.instance();
    }

    // === helper methods for translating geometry from the instrument's
    // === coordinate system to screen coordinates

    private AffineTransform toScreenTransform = new AffineTransform();

    /**
     * Initializes the transformation for mapping a geometry to the screen.
     * The rotation covers the position angle of the telescope, the scaling factor changes the size of the area to
     * map the pixel to arc seconds ratio of the current screen coordinate system and finally the translation will
     * move the area to the current screen position that represents (0,0) of the instrument's coordinate system.
     * Note: This method implicitly includes the correction needed for images in the TPE with coordinate systems
     * that are flipped along the x-Axis (flipRA).
     */
    protected void setTransformationToScreen(Angle rotation, double scaleFactor, Point2D.Double translation) {
        double radians = rotation.convertTo(Angle.Unit.RADIANS).getMagnitude();
        toScreenTransform = new AffineTransform();
        toScreenTransform.translate(translation.x, translation.y);
        toScreenTransform.scale(scaleFactor, scaleFactor);
        toScreenTransform.rotate(radians);
        toScreenTransform.scale(_flipRA, 1.0);
    }

    /**
     * Gets the current transformation for mapping a geometry to the screen.
     */
    protected AffineTransform getTransformationToScreen() {
        return toScreenTransform;
    }

    /**
     * Transforms a geometry to screen coordinates according to the parameter set with
     * {@see setTransformationFlipXY} and {@see setTransformationToScreen}.
     */
    protected Area transformToScreen(Area area) {
        area.transform(toScreenTransform);
        return area;
    }

    // === collect figures for later drawing.
    protected static class Figure {
        public final Shape shape;
        public final Color color;
        public final Composite composite;
        public final Stroke stroke;

        public Figure(Shape shape, Color color, Composite composite, Stroke stroke) {
            this.shape     = shape;
            this.color     = color;
            this.composite = composite;
            this.stroke    = stroke;
        }

        public void draw(Graphics2D g2d, boolean fillObscuredArea) {
            final Stroke startStroke = g2d.getStroke();
            if (stroke != null) {
                g2d.setStroke(stroke);
            }
            g2d.setColor(color);
            g2d.draw(shape);

            if (composite != null && fillObscuredArea) {
                g2d.setComposite(composite);
                g2d.fill(shape);
                g2d.setPaintMode();
            }

            g2d.setStroke(startStroke);
        }

        public Figure transform(AffineTransform xform) {
            return new Figure(xform.createTransformedShape(shape), color, composite, stroke);
        }
    }
    // List of Figures to draw.
    protected final LinkedList<Figure> _figureList = new LinkedList<>();
    protected void clearFigures() {
        _figureList.clear();
    }
    protected void drawFigures(Graphics2D g2d, boolean fillObscuredArea) {
        for (Figure fig : _figureList) {
            fig.draw(g2d, fillObscuredArea);
        }
    }
    protected void addFigure(Figure figure) {
        _figureList.add(figure);
    }
    protected void addFigure(Shape shape, Color color, Composite composite, Stroke stroke) {
        _figureList.add(new Figure(shape, color, composite, stroke));
    }


}

