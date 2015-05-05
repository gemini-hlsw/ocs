package jsky.app.ot.gemini.gems;

import edu.gemini.ags.gems.GemsResultsAnalyzer;
import edu.gemini.ags.gems.GemsUtils4Java;
import edu.gemini.ags.gems.mascot.Mascot;
import edu.gemini.ags.gems.mascot.MascotConf;
import edu.gemini.ags.gems.mascot.Star;
import edu.gemini.ags.gems.mascot.Strehl;
import edu.gemini.mascot.gui.contour.ContourPlot;
import edu.gemini.mascot.gui.contour.StrehlContourPlot;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.env.TargetEnvironmentDiff;
import edu.gemini.spModel.target.system.CoordinateParam;
import jsky.app.ot.tpe.*;
import jsky.app.ot.util.BasicPropertyList;
import jsky.app.ot.util.PropertyWatcher;
import jsky.util.gui.SwingWorker;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.*;

/**
 * Adds a Strehl Information Option to the TPE
 * See OT-33.
 */
public class StrehlFeature extends TpeImageFeature implements PropertyWatcher, MouseListener {

    // Current transformation
    private AffineTransform trans;

    // Message to display
    private TpeMessage message;

    // BufferedImage holding the strehl contour plot
    private ContourPlot contourPlot;

    // If true, ignore target list changes while dragging
    private boolean ignoreTargetListChanges;

    // True if the display needs to be updated
    private boolean updatePending;

    // true if there is an error in the target list (can't determine tiptilt group)
    private boolean targetListError;

    // Type of target list
    private GuideProbe.Type guideProbeType;

    private static TpeMessage busyMessage = TpeMessage.infoMessage("Calculating Strehl map...");


    // Used to format strehl message
    private static NumberFormat nf = NumberFormat.getInstance(Locale.US);
    private static NumberFormat nf2 = NumberFormat.getInstance(Locale.US);

    static {
        nf.setMaximumFractionDigits(1);
        nf2.setMaximumFractionDigits(2);
    }

    // Property used to control drawing of the probe ranges.
    private static final BasicPropertyList props = new BasicPropertyList(StrehlFeature.class.getName());
    private static final String PROP_SHOW_STREHL_MAP = "Show Strehl Map";
    static {
        props.registerBooleanProperty(PROP_SHOW_STREHL_MAP, true);
    }

    /**
     * Create with a short name and a longer description.
     */
    public StrehlFeature() {
        super("Strehl", "Show the Strehl contour map and statistics");
    }

    /**
     * Override getProperties to return the properties supported by this
     * feature.
     */
    @Override
    public BasicPropertyList getProperties() {
        return props;
    }

    /**
     * Turn on/off the drawing of strehl map
     */
    public void setShowStrehlMap(boolean draw) {
        props.setBoolean(PROP_SHOW_STREHL_MAP, draw);
    }

    /**
     * Returns true if strehl map should be displayed
     */
    public boolean getShowStrehlMap() {
        return props.getBoolean(PROP_SHOW_STREHL_MAP, true);
    }

    @Override
    public void reinit(TpeImageWidget iw, TpeImageInfo tii) {
        super.reinit(iw, tii);

        props.addWatcher(this);

        SPInstObsComp inst = _iw.getInstObsComp();
        if (inst == null) return;

        // arrange to be notified if telescope positions are added, removed, or selected
        _monitorPosList();

        Point2D.Double base = tii.getBaseScreenPos();
        int size = getContourPlotSize();
        int r = size / 2;

        trans = new AffineTransform();

        // Center at base position
        trans.translate(base.x, base.y);

        // rotate by difference between north and up
        trans.rotate(-tii.getTheta());
        trans.translate(-r, -r);

        // Flip the plot to get in FITS orientation
        trans.scale(tii.flipRA(), -1.0);
        trans.translate(tii.getFlipRA() ? -size : 0.0, -size);

        iw.removeMouseListener(this);
        iw.addMouseListener(this);

        calculateStrehl();
    }

    @Override
    public void unloaded() {
        props.deleteWatcher(this);
        if (_iw != null) {
            _iw.removeMouseListener(this);
        }
        super.unloaded();
    }


    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!isVisible()) return;

        // ignore changes until button released, for better performance
        ignoreTargetListChanges = true;
        updatePending = false;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isVisible()) return;

        // ignore changes until button released, for better performance
        ignoreTargetListChanges = false;
        if (updatePending) {
            updatePending = false;
            calculateStrehl();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Schedule a redraw of the image feature.
     */
    private void _redraw() {
        if (_iw != null) _iw.repaint();
    }


    @Override
    public void draw(Graphics g, TpeImageInfo tii) {
        if (!isVisible()) return;

        Graphics2D g2d = (Graphics2D) g;

        if (contourPlot != null) {
            g2d.drawRenderedImage(contourPlot, trans);
        }
    }

    // Run the mascot strehl algorithm in a background thread, since it is too slow to run in draw()
    private void calculateStrehl() {
        contourPlot = null;
        message = null;

        if (!isVisible() || ignoreTargetListChanges) {
            return;
        }

        final List<SPTarget> targetList = getTargetList();
        if (targetList.size() == 0) {
            _redraw();
            return;
        }

        message = busyMessage;

        new SwingWorker() {
            public Object construct() {
                try {
                    scala.Option<Strehl> strehl = computeStrehlFromTargetList(targetList, guideProbeType);
                    return new Trio<>(strehl, makeStrehlMessage(strehl), makeContourPlot(strehl));
                } catch (Exception e) {
                    e.printStackTrace();
                    return new Trio<Strehl, TpeMessage, ContourPlot>(null,
                            TpeMessage.errorMessage("Strehl Error: " + e.getMessage()),
                            null);
                }
            }

            public void finished() {
                Object o = getValue();
                if (!targetListError) {
                    // Note: targetListError may have been set in event thread while background thead was running
                    Trio<Strehl, TpeMessage, ContourPlot> p = (Trio<Strehl, TpeMessage, ContourPlot>) o;
                    message = p._2();
                    contourPlot = p._3();
                }
                _redraw();
            }

        }.start();
    }

    // REL-1321:
    // On the PE, the Strehl statistics and expected FWHM must be displayed.
    // The approximate FWHM for each IQ bin and effective band for GSAOI are
    // J: IQ20=0.08" IQ70=0.13" IQ85=0.15" IQAny=INDEF
    // H: IQ20=0.07" IQ70=0.10" IQ85=0.13" IQAny=INDEF
    // K: IQ20=0.06" IQ70=0.09" IQ85=0.12" IQAny=INDEF
    // In the future the table will have to be expanded to include values for F2 and GMOS-S.
    private Double calculateFwhm() {
        Option<ObsContext> obsContextOption = _iw.getObsContext();
        if (!obsContextOption.isEmpty()) {
            ObsContext obsContext = obsContextOption.getValue();
            SPInstObsComp inst = obsContext.getInstrument();
            if (inst instanceof Gsaoi) {
                Gsaoi gsaoi = (Gsaoi) inst;
                Option<Magnitude.Band> band = gsaoi.getFilter().getCatalogBand();
                if (!band.isEmpty()) {
                    String s = band.getValue().name();
                    SPSiteQuality.Conditions conditions = obsContext.getConditions();
                    if ("J".equals(s)) {
                        if (conditions != null) {
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_20) return 0.08;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_70) return 0.13;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_85) return 0.15;
                        }
                    }
                    if ("H".equals(s)) {
                        if (conditions != null) {
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_20) return 0.07;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_70) return 0.10;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_85) return 0.13;

                        }
                    }
                    if ("K".equals(s)) {
                        if (conditions != null) {
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_20) return 0.06;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_70) return 0.09;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_85) return 0.12;
                        }
                    }
                }
            }
        }
        return null;
    }


    // Creates and returns a ContourPlot (BufferedImage subclass) containing the contour plot
    private ContourPlot makeContourPlot(scala.Option<Strehl> strehl) {
        if (strehl.isEmpty() || !getShowStrehlMap()) {
            return null;
        } else {
            return StrehlContourPlot.create(strehl.get(), getContourPlotSize());
        }
    }

    // Returns the size of the contour plot in pixels (plot will be size x size pixels)
    private int getContourPlotSize() {
        // XXX not sure yet where this is defined, but the original mascot displays a 2 arcmin square
        return (int) (120 * _tii.getPixelsPerArcsec());
    }

    // Get list of stars to pass to strehl algorithm
    private List<SPTarget> getTargetList() {
        List<SPTarget> targetList = new ArrayList<>();
        Option<ObsContext> ctxOpt = _iw.getObsContext();
        if (ctxOpt.isEmpty()) return targetList;
        ObsContext ctx = ctxOpt.getValue();
        TargetEnvironment env = ctx.getTargets();
        GuideGroup group = env.getOrCreatePrimaryGuideGroup();
        guideProbeType = null;
        // The counts are used to guess which is tiptilt and which is flexure. See OT-33
        int oiwfsCount = 0;
        int aowfsCount = 0;
        for (GuideProbe gp : group.getReferencedGuiders()) {
            GuideProbe.Type t = gp.getType();
            switch (t) {
                case OIWFS:
                    oiwfsCount++;
                    break;
                case AOWFS:
                    aowfsCount++;
                    break;
                default:
            }
        }
        if ("Flamingos2".equals(ctx.getInstrument().getNarrowType())) {
            // OT-33: For Flamingos 2, Canopus always provides tip/tilt correction
            guideProbeType = GuideProbe.Type.AOWFS;
        } else if (oiwfsCount > 1 && aowfsCount > 1) {
            // OT-33: If there are multiple active stars for both Canopus and GSAOI ODGW, we should
            // display an error message on the TPE when the Strehl option is selected.
            // We will be updating the model to store the tip/tilt vs. flexure designation somehow.
            // At that point, we'll just use that information instead of guessing which is which.
            message = TpeMessage.warningMessage("Strehl: Error: Both ODGW and CWFS guide stars defined");
            targetListError = true;
            return new ArrayList<>();
        } else if (oiwfsCount > 1) {
            guideProbeType = GuideProbe.Type.OIWFS;
        } else {
            guideProbeType = GuideProbe.Type.AOWFS;
        }
        message = null;
        targetListError = false;

        for (GuideProbe gp : group.getReferencedGuiders()) {
            GuideProbe.Type t = gp.getType();
            if (t == guideProbeType) {
                Option<SPTarget> primary = group.get(gp).getValue().getPrimary();
                if (!primary.isEmpty()) {
                    SPTarget target = primary.getValue();
                    targetList.add(target);
                    target.addWatcher(this);
                }
            }
        }
        return targetList;
    }


    // Returns a Strehl object calculated by the mascot algorithm based on the given
    // list of 1 to 3 stars (any fourth star is ignored).
    private scala.Option<Strehl> computeStrehlFromTargetList(List<SPTarget> targetList, GuideProbe.Type type) {
        if (targetList.size() == 0) return null;
        Star[] starList = targetListToStarList(targetList);

        double factor = GemsResultsAnalyzer.instance().getStrehlFactor(this.getContext().obsContextJava());
        return Mascot.computeStrehl4Java(getBandpass(type), factor, starList[0], scala.Option.apply(starList[1]), scala.Option.apply(starList[2]));
    }


    // OT-33: If the asterism is a Canopus asterism, use R. If an ODGW asterism,
    // see OT-22 for a mapping of GSAOI filters to J, H, and K.
    // If iterating over filters, I think we can assume the filter in
    // the static component as a first pass at least.
    private MagnitudeBand getBandpass(GuideProbe.Type type) {
        if (type != null) {
            switch (type) {
                case AOWFS:
                    return Mascot.defaultBandpass();
                case OIWFS:
                    SPInstObsComp inst = _iw.getInstObsComp();
                    if (inst instanceof Gsaoi) {
                        Gsaoi gsaoi = (Gsaoi) inst;
                        Option<Magnitude.Band> band = gsaoi.getFilter().getCatalogBand();
                        if (!band.isEmpty()) {
                            return GemsUtils4Java.toNewBand(band.getValue());
                        }
                    }
                default:
            }
        }
        return Mascot.defaultBandpass();
    }

    // Returns an array of mascot Star objects for the given target list
    private Star[] targetListToStarList(List<SPTarget> targetList) {
        Star[] starList = new Star[3];
        int n = targetList.size();
        for (int i = 0; i < starList.length; i++) {
            starList[i] = i < n ? targetToStar(targetList.get(i)) : null;
        }
        return starList;
    }

    // Returns a mascot Star object for the given target.
    private Star targetToStar(SPTarget target) {
        String name = target.getTarget().getName();
        double ra = target.getTarget().getRa().getAs(CoordinateParam.Units.DEGREES);
        double dec = target.getTarget().getDec().getAs(CoordinateParam.Units.DEGREES);
        double baseX = _tii.getBasePos().getRaDeg();
        double baseY = _tii.getBasePos().getDecDeg();
        Magnitude undef = new Magnitude(Magnitude.Band.J, MascotConf.invalidMag());
        double bmag = target.getTarget().getMagnitude(Magnitude.Band.B).getOrElse(undef).getBrightness();
        double vmag = target.getTarget().getMagnitude(Magnitude.Band.V).getOrElse(undef).getBrightness();
        double rmag = target.getTarget().getMagnitude(Magnitude.Band.R).getOrElse(undef).getBrightness();
        double jmag = target.getTarget().getMagnitude(Magnitude.Band.J).getOrElse(undef).getBrightness();
        double hmag = target.getTarget().getMagnitude(Magnitude.Band.H).getOrElse(undef).getBrightness();
        double kmag = target.getTarget().getMagnitude(Magnitude.Band.K).getOrElse(undef).getBrightness();
        return Star.makeStar(name, baseX, baseY, bmag, vmag, rmag, jmag, hmag, kmag, ra, dec);
    }

    // Displays a message with the mean, rms, min and max Strehl values (and FWHM)
    //
    // REL-1321: example: for K band IQ70:
    // Strehl: avg=16.2 ± 4.7, min=13.4, max=17.5; FWHM~0.09"
    private TpeMessage makeStrehlMessage(scala.Option<Strehl> strehl) {
        if (strehl.isDefined()) {
            Strehl s = strehl.get();
            Double fwhm = calculateFwhm();

            StringBuffer sb = new StringBuffer();
            sb.append("Strehl: avg=");
            sb.append(nf.format(s.avgstrehl() * 100));
            sb.append(" \u00B1 ");
            sb.append(nf.format(s.rmsstrehl() * 100));
            sb.append(",  min=");
            sb.append(nf.format(s.minstrehl() * 100));
            sb.append(",  max=");
            sb.append(nf.format(s.maxstrehl() * 100));
            if (fwhm != null)  {
                sb.append(",  FWHM~");
                sb.append(nf2.format(fwhm));
            }
            return TpeMessage.infoMessage(sb.toString());
        } else {
            return TpeMessage.warningMessage("Strehl: does not fit.");
        }
    }

    public void propertyChange(String propName) {
        calculateStrehl();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosLocationUpdate(WatchablePos tp) {
        updatePending = true;
        calculateStrehl();
    }

    /**
     * Implements the TelescopePosWatcher interface.
     * @param tp
     */
    public void telescopePosGenericUpdate(WatchablePos tp) {
        updatePending = true;
        calculateStrehl();
    }

    protected void handleTargetEnvironmentUpdate(TargetEnvironmentDiff diff) {
        updatePending = true;
        calculateStrehl();
    }


    public TpeImageFeatureCategory getCategory() {
        return TpeImageFeatureCategory.fieldOfView;
    }

    @Override
    public boolean isEnabled(TpeContext ctx) {
        if (!super.isEnabled(ctx)) return false;
        return ctx.gems().isDefined();
    }

    public Option<Collection<TpeMessage>> getMessages() {
        if (message != null) {
            return new Some<Collection<TpeMessage>>(Collections.singletonList(message));
        }
        return None.instance();
    }
}
