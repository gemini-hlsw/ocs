package jsky.app.ot.gemini.inst;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Offset;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.flamingos2.F2ScienceAreaGeometry$;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.gemini.ghost.GhostScienceAreaGeometry$;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.igrins2.Igrins2;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import jsky.app.ot.gemini.acqcam.AcqCam_SciAreaFeature;
import jsky.app.ot.gemini.bhros.BHROS_SciAreaFeature;
import jsky.app.ot.gemini.gnirs.GNIRS_SciAreaFeature;
import jsky.app.ot.gemini.gpi.Gpi_SciAreaFeature;
import jsky.app.ot.gemini.gsaoi.GsaoiDetectorArrayFeature;
import jsky.app.ot.gemini.michelle.Michelle_SciAreaFeature;
import jsky.app.ot.gemini.nici.NICI_SciAreaFeature;
import jsky.app.ot.gemini.nifs.NIFS_SciAreaFeature;
import jsky.app.ot.gemini.niri.NIRI_SciAreaFeature;
import jsky.app.ot.gemini.phoenix.Phoenix_SciAreaFeature;
import jsky.app.ot.gemini.texes.Texes_SciAreaFeature;
import jsky.app.ot.gemini.trecs.TReCS_SciAreaFeature;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Draws the science area.
 * <p>
 * This class is a wrapper for one of the instrument specific classes:
 * NIRI_SciAreaFeature, NIFS_SciAreaFeature, GMOS_SciAreaFeature,AcqCam_SciAreaFeature,
 * TReCS_SciAreaFeature, Michelle_SciAreaFeature,Phoenix_SciAreaFeature.
 * The class used depends on the instrument being used.
 */
public class SciAreaFeature extends TpeImageFeature
        implements TpeDraggableFeature, PropertyWatcher {

    // The instrument OIWFS feature
    private TpeImageFeature _feat;

    // The instrument specific subclasses
    private NIRI_SciAreaFeature _niriFeat;
    private NIFS_SciAreaFeature _nifsFeat;
    private BHROS_SciAreaFeature _bhrosFeat;
    private AcqCam_SciAreaFeature _acqCamFeat;
    private Phoenix_SciAreaFeature _phoenixFeat;
    private TReCS_SciAreaFeature _trecsFeat;
    private Michelle_SciAreaFeature _michelleFeat;
    private GNIRS_SciAreaFeature _gnirsFeat;
    private SciAreaPlotFeature _ghost2Feat;
    private SciAreaPlotFeature _flamingos2Feat;
    private NICI_SciAreaFeature _niciFeat;
    private Texes_SciAreaFeature _texesFeat;
    private Gpi_SciAreaFeature _gpiFeat;
    private GsaoiDetectorArrayFeature _gsaoiFeat;

    // properties (items are displayed in the OT View menu)
    private static final BasicPropertyList _props = new BasicPropertyList(SciAreaFeature.class.getName());
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
    public SciAreaFeature() {
        super("Science", "Show the science FOV.");
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
     * Turn the display of the chop beams on or off.
     */
    public static void setDisplayChopBeams(boolean show) {
        _props.setBoolean(PROP_DISPLAY_CHOP_BEAMS, show);
    }

    /**
     * Get the "Display Chop Beams" property.
     */
    public static boolean getDisplayChopBeams() {
        return _props.getBoolean(PROP_DISPLAY_CHOP_BEAMS, true);
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
     * Return the current Nod/Chop offset in screen pixels.
     *
     * @see jsky.app.ot.gemini.trecs.TReCS_SciAreaFeature
     */
    public Point2D.Double getNodChopOffset() {
        return (_feat instanceof SciAreaFeatureBase) ?
                ((SciAreaFeatureBase) _feat).getNodChopOffset() :
                new Point2D.Double();
    }


    /**
     * Reinitialize the feature.
     */
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        TpeContext ctx = iw.getContext();
        if (ctx.isEmpty()) return;

        SPInstObsComp inst = iw.getInstObsComp();

        if (inst instanceof InstNIRI) {
            if (_niriFeat == null) {
                _niriFeat = new NIRI_SciAreaFeature();
            }
            _feat = _niriFeat;
        } else if (inst instanceof InstNIFS) {
            if (_nifsFeat == null) {
                _nifsFeat = new NIFS_SciAreaFeature();
            }
            _feat = _nifsFeat;
        } else if (inst instanceof InstBHROS) {
            if (_bhrosFeat == null) {
                _bhrosFeat = new BHROS_SciAreaFeature();
            }
            _feat = _bhrosFeat;
        } else if (inst instanceof InstGmosCommon) {
            _feat = GmosSciAreaPlotFeature$.MODULE$;
        } else if (inst instanceof InstAcqCam) {
            if (_acqCamFeat == null) {
                _acqCamFeat = new AcqCam_SciAreaFeature();
            }
            _feat = _acqCamFeat;
        } else if (inst instanceof InstPhoenix) {
            if (_phoenixFeat == null) {
                _phoenixFeat = new Phoenix_SciAreaFeature();
            }
            _feat = _phoenixFeat;
        } else if (inst instanceof InstTReCS) {
            if (_trecsFeat == null) {
                _trecsFeat = new TReCS_SciAreaFeature();
            }
            _feat = _trecsFeat;
        } else if (inst instanceof InstMichelle) {
            if (_michelleFeat == null) {
                _michelleFeat = new Michelle_SciAreaFeature();
            }
            _feat = _michelleFeat;
        } else if (inst instanceof InstGNIRS) {
            if (_gnirsFeat == null) {
                _gnirsFeat = new GNIRS_SciAreaFeature();
            }
            _feat = _gnirsFeat;
        } else if (inst instanceof Ghost) {
            if (_ghost2Feat == null) {
                _ghost2Feat = new SciAreaPlotFeature(GhostScienceAreaGeometry$.MODULE$);
            }
            _feat = _ghost2Feat;
        } else if (inst instanceof Flamingos2) {
            if (_flamingos2Feat == null) {
                _flamingos2Feat = new SciAreaPlotFeature(F2ScienceAreaGeometry$.MODULE$);
            }
            _feat = _flamingos2Feat;
        } else if (inst instanceof InstNICI) {
            if (_niciFeat == null) {
                _niciFeat = new NICI_SciAreaFeature();
            }
            _feat = _niciFeat;
        } else if (inst instanceof InstTexes) {
            if (_texesFeat ==  null) {
                _texesFeat = new Texes_SciAreaFeature();
            }
            _feat = _texesFeat;
        } else if (inst instanceof Gpi) {
            if (_gpiFeat ==  null) {
                _gpiFeat = new Gpi_SciAreaFeature();
            }
            _feat = _gpiFeat;
        } else if (inst instanceof Gsaoi) {
            if (_gsaoiFeat == null) {
                _gsaoiFeat = new GsaoiDetectorArrayFeature();
            }
            _feat = _gsaoiFeat;
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
     * Draw the science area at the given offset.  The x/y coordinates are the
     * screen coordinates and are used by the old SciAreaFeatureBase-based
     * delegates.
     */
    public void drawAtOffsetPos(Graphics g, TpeImageInfo tii, Offset offset, double x, double y) {
        if (_feat instanceof SciAreaFeatureBase) {
            ((SciAreaFeatureBase) _feat).drawAtOffsetPos(g, tii, x, y);
        } else if (_feat instanceof SciAreaPlotFeature) {
            ((SciAreaPlotFeature) _feat).drawAtOffset(g, tii, offset);
        }
    }


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
        if (_feat instanceof TpeDraggableFeature) {
            return ((TpeDraggableFeature) _feat).dragStart(tme, tii);
        } else {
            return None.instance();
        }
    }

    /**
     * Drag to a new location.
     */
    public void drag(TpeMouseEvent tme) {
        if (_feat instanceof TpeDraggableFeature) {
            ((TpeDraggableFeature) _feat).drag(tme);
        }
    }

    /**
     * Stop dragging.
     */
    public void dragStop(TpeMouseEvent tme) {
        if (_feat instanceof TpeDraggableFeature) {
            ((TpeDraggableFeature) _feat).dragStop(tme);
        }
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
