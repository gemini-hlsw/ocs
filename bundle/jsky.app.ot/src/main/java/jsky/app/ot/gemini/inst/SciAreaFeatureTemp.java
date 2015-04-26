// Copyright 1997-2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SciAreaFeature.java 21635 2009-08-24 01:08:24Z swalker $
//
package jsky.app.ot.gemini.inst;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;


/**
 * Draws the science area.
 * <p>
 * This class is a wrapper for one of the instrument specific classes:
 * NIRI_SciAreaFeature, NIFS_SciAreaFeature, GMOS_SciAreaFeature,AcqCam_SciAreaFeature,
 * TReCS_SciAreaFeature, Michelle_SciAreaFeature,Phoenix_SciAreaFeature.
 * The class used depends on the instrument being used.
 */
public class SciAreaFeatureTemp extends TpeImageFeature
        implements TpeDraggableFeature, PropertyWatcher {

    private SciAreaPlotFeature _feat;

    // properties (items are displayed in the OT View menu)
    private static final BasicPropertyList _props = new BasicPropertyList(SciAreaFeatureTemp.class.getName());
    private static final String PROP_DISPLAY_CHOP_BEAMS = "Display Chop Beams";
    private static final String PROP_SHOW_TAGS = "Show Tags";
    private static final String PROP_SCI_AREA_DISPLAY = "Display Science FOV at";
    static {
        // Initialize the properties supported by this feature.
        _props.registerBooleanProperty(PROP_SHOW_TAGS, true);
        _props.registerBooleanProperty(PROP_DISPLAY_CHOP_BEAMS, true);
        _props.registerChoiceProperty(PROP_SCI_AREA_DISPLAY,
                new String[]{"Selected Position", "All Positions", "None"},
                0);
    }

    /**
     * The mode that indicates that the science area should be drawn around
     * the selected offset position.
     */
    public static final int SCI_AREA_SELECTED = 0;

    /**
     * The mode that indicates that the science area should be drawn around
     * all the selected offset positions.
     */
    public static final int SCI_AREA_ALL = 1;

    /**
     * The mode that indicates that the science area should not be drawn
     * at any offset positions.
     */
    public static final int SCI_AREA_NONE = 2;


    /**
     * Construct the feature with its name and description.
     */
    public SciAreaFeatureTemp() {
        super("Science (2)", "Show the science FOV.");
        _props.addWatcher(this);
    }

    /**
     * A property has changed.
     *
     * @see PropertyWatcher
     */
    public void propertyChange(String propName) {
        if (_iw != null) _iw.repaint();
    }


    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    public BasicPropertyList getProperties() {
        return _props;
    }

    /** Static version of getProperties() */
    public static BasicPropertyList getProps() {
        return _props;
    }


    /**
     * Turn on/off the drawing of the offset index.
     */
    public static void setDrawIndex(boolean drawIndex) {
        _props.setBoolean(PROP_SHOW_TAGS, drawIndex);
    }

    /**
     * Get the state of the drawing of the offset index.
     */
    public static boolean getDrawIndex() {
        return _props.getBoolean(PROP_SHOW_TAGS, true);
    }

    /**
     * Set the science area draw mode.  Must be one of SCI_AREA_NONE,
     * SCI_AREA_SELECTED, or SCI_AREA_ALL.
     */
    public static void setSciAreaMode(int mode) {
        _props.setChoice(PROP_SCI_AREA_DISPLAY, mode);
    }

    /**
     * Get the mode.  One of SCI_AREA_SELECTED, SCI_AREA_ALL, or SCI_AREA_NONE.
     */
    public static int getSciAreaMode() {
        return _props.getChoice(PROP_SCI_AREA_DISPLAY, SCI_AREA_SELECTED);
    }

    /**
     * Reinitialize the feature.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        TpeContext ctx = iw.getContext();
        if (ctx.isEmpty()) return;

        SPInstObsComp inst = iw.getInstObsComp();

        if ((inst instanceof InstGmosNorth) || (inst instanceof InstGmosSouth)) {
            _feat = GmosSciAreaPlotFeature$.MODULE$;
        } else {
            _feat = null;
        }

        if (_feat != null) {
            _feat.reinit(iw, tii);
        }
    }

    /**
     *  Unload the feature.
     */
    public void unloaded() {
        super.unloaded();
        if (_feat != null) _feat.unloaded();
    }

    /**
     * Draw the feature.
     */
    public void draw(Graphics g, TpeImageInfo tii) {
        if (_feat != null) _feat.draw(g, tii);
    }

    /**
     * Draw the science area at the given x,y (screen coordinate) offset position.
     */
//    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, double x, double y) {
//        if (_feat != null) _feat.drawAtOffsetPos(g, tii, x, y);
//    }


    /**
     * The position angle has changed.
     */
    public void posAngleUpdate(TpeImageInfo tii) {
        if (_feat != null) _feat.posAngleUpdate(tii);
    }

    /**
     * Start dragging the object.
     */
    public Option<Object> dragStart(TpeMouseEvent tme, TpeImageInfo tii) {
        if (_feat == null) return None.instance();
        return _feat.dragStart(tme, tii);
    }

    /**
     * Drag to a new location.
     */
    public void drag(TpeMouseEvent tme) {
        if (_feat != null) _feat.drag(tme);
    }

    /**
     * Stop dragging.
     */
    public void dragStop(TpeMouseEvent tme) {
        if (_feat != null) _feat.dragStop(tme);
    }


    /**
     * Return true if the mouse is over an active part of this image feature
     * (so that dragging can begin there).
     */
    public boolean isMouseOver(TpeMouseEvent tme) {
        return (_feat != null) && _feat.isMouseOver(tme);
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }
}
