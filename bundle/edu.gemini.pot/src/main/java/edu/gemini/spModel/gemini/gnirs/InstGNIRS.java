package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config.ConfigPostProcessor;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGnirs;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;
import edu.gemini.spModel.gemini.parallacticangle.ParallacticAngleSupportInst;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.plannedtime.DefaultStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;

import java.beans.PropertyDescriptor;

import java.text.NumberFormat;
import java.util.*;

/**
 * The GNIRS instrument.
 */
public class InstGNIRS extends ParallacticAngleSupportInst implements PropertyProvider, GuideProbeProvider,
        IssPortProvider, ConfigPostProcessor, StepCalculator, CalibrationKeyProvider, PosAngleConstraintAware {

    // for serialization
    private static final long serialVersionUID = 3L;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_GNIRS;

    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor CROSS_DISPERSED_PROP;
    public static final PropertyDescriptor READ_MODE_PROP;
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor PIXEL_SCALE_PROP;
    public static final PropertyDescriptor SLIT_WIDTH_PROP;
    public static final PropertyDescriptor DECKER_PROP;
    public static final PropertyDescriptor CAMERA_PROP;
    public static final PropertyDescriptor WELL_DEPTH_PROP;
    public static final PropertyDescriptor CENTRAL_WAVELENGTH_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor ACQUISITION_MIRROR_PROP;
    public static final PropertyDescriptor POS_ANGLE_CONSTRAINT_PROP;

    // REL-2646.  This is an unfortunate requirement that falls out of REL-2646.
    // The observing wavelength for acquisition observations should be computed
    // based on the imaging filter.  Unfortunately in the past this was not
    // done and instead the observing wavelength was always taken from the
    // grating central wavelength during sequence construction.  Old
    // observations therefore used the wrong observing wavelength and we must
    // continue to compute the value that was actually used for them.  To avoid
    // setting the observing wavelength during sequence construction for old
    // observed observations, we have to track a property in the model that is
    // set during migration.
    //
    // If "override acquisition observing wavelength" is true (which it will be
    // by default for new observations), then the new method of setting the
    // observing wavelength from the filter will be used.  If not, the old
    // method of using the grating central wavelength will be used.
    public static final PropertyDescriptor OVERRIDE_ACQ_OBS_WAVELENGTH_PROP;

    public static final PropertyDescriptor PORT_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstGNIRS.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        ACQUISITION_MIRROR_PROP = initProp("acquisitionMirror", query_yes, iter_yes);
        CAMERA_PROP = initProp("camera", query_yes, iter_yes);
        CAMERA_PROP.setExpert(true);
        CENTRAL_WAVELENGTH_PROP = initProp("centralWavelength", query_yes, iter_yes);
        PropertySupport.setVolatile(CENTRAL_WAVELENGTH_PROP, true);
        COADDS_PROP = initProp("coadds", query_no, iter_yes);
        CROSS_DISPERSED_PROP = initProp("crossDispersed", query_yes, iter_yes);
        CROSS_DISPERSED_PROP.setDisplayName("Cross-dispersed");
        DECKER_PROP = initProp("decker", query_yes, iter_yes);
        DISPERSER_PROP = initProp("disperser", query_yes, iter_yes);
        EXPOSURE_TIME_PROP = initProp(InstConstants.EXPOSURE_TIME_PROP, query_no, iter_yes);
        FILTER_PROP = initProp("filter", query_yes, iter_yes);
        PIXEL_SCALE_PROP = initProp("pixelScale", query_yes, iter_no);
        READ_MODE_PROP = initProp(ReadMode.KEY.getName(), query_yes, iter_yes);
        READ_MODE_PROP.setDisplayName("Read Mode");
        //READ_MODE_PROP.setExpert(true);
        SLIT_WIDTH_PROP = initProp("slitWidth", query_yes, iter_yes);
        SLIT_WIDTH_PROP.setDisplayName("Focal Plane Unit");
        WELL_DEPTH_PROP = initProp("wellDepth", query_yes, iter_no);
        PORT_PROP = initProp("issPort", query_yes, iter_no);
        POS_ANGLE_CONSTRAINT_PROP = initProp("posAngleConstraint", query_no, iter_no);

        OVERRIDE_ACQ_OBS_WAVELENGTH_PROP = initProp("overrideAcqObsWavelength", query_no, iter_no);
    }

    private PixelScale _pixelScale = PixelScale.DEFAULT;
    private Disperser _disperser = Disperser.DEFAULT;
    private SlitWidth _slitWidth = SlitWidth.DEFAULT;
    private double _centralWavelength = GNIRSConstants.DEF_CENTRAL_WAVELENGTH;
    private CrossDispersed _crossDispersed = CrossDispersed.DEFAULT;
    private WollastonPrism _wollastonPrism = WollastonPrism.DEFAULT;
    private ReadMode _readMode = ReadMode.DEFAULT;
    private AcquisitionMirror _acquisitionMirror = AcquisitionMirror.DEFAULT;
    private Camera _camera = Camera.DEFAULT;
    private Decker _decker = Decker.DEFAULT;
    private Filter _filter = Filter.DEFAULT;
    private WellDepth _wellDepth = WellDepth.DEFAULT;

    private IssPort port = IssPort.SIDE_LOOKING;

    private PosAngleConstraint _posAngleConstraint = PosAngleConstraint.FIXED;

    // See note above where OVERRIDE_ACQ_OBS_WAVELENGTH PROP is declared.
    private boolean _overrideAcqObsWavelength = true;

    private static final String _VERSION = "2017A-1";

    // table of wavelengths per order
    private transient double[] _centralWavelengthOrderN = null;

    // Used to format the central wavelength as a string
    private static final NumberFormat _nf = NumberFormat.getInstance(Locale.US);

    static {
        _nf.setMaximumFractionDigits(3);
    }

    /**
     * Constructor
     */
    public InstGNIRS() {
        super(SP_TYPE);

        // Override the default exposure time
        _exposureTime = GNIRSConstants.DEF_EXPOSURE_TIME;
        _coadds = GNIRSConstants.DEF_COADDS;
        setVersion(_VERSION);
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        InstGNIRS result = (InstGNIRS) super.clone();

        // copy the array
        if (_centralWavelengthOrderN != null) {
            result._centralWavelengthOrderN = _centralWavelengthOrderN.clone();
        }
        return result;
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GN;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public String getPhaseIResourceName() {
        return "gemGNIRS";
    }

    /**
     * Return the setup time in seconds before observing can begin
     * (nominally 30 minutes, may be longer for IFU, and thermal (WL>2.5um))
     * (Update: see OT-243: 10 and 20 min).
     * (Update: SCT-275: 15 minutes longslit, 20 minutes IFU)
     */
    public double getSetupTime(ISPObservation obs) {
        final InstAltair altair = InstAltair.lookupAltair(obs);

        // REL-437: When GNIRS is used with Altair in LGS mode then the acq overhead must be 25 minutes.
        // The only change is to GNIRS with Altair LGS, for other modes the overhead should remain 15 minutes.
        if (altair != null && altair.getMode().guideStarType() == AltairParams.GuideStarType.LGS) {
            return 25 * 60;
        }

        return (_slitWidth == SlitWidth.IFU) ? (20 * 60) : (15 * 60);
    }

    /**
     * Time needed to re-setup the instrument before the Observation following a previous full setup.
     *
     * @param obs the observation for which the setup time is wanted
     * @return time in seconds
     */
    public double getReacquisitionTime(ISPObservation obs) {
        return 6 * 60; // 6 minutes as defined in REL-1346
    }

    /**
     * Return the minimum exposure time, based on the current ReadMode value.
     */
    public double getMinExpTime() {
        return _readMode.getMinExp();
    }

    /**
     * Return the science area based upon the current camera.
     */
    public double[] getScienceArea() {
        // This is the size of the science area, which is a rectangle.
        // The height is defined by "slit width" (except IFU), the width is as follows:
        //   pixel scale 0.05"+XD => 3.1arcsec
        //   pixel scale 0.15"+XD => 6arcsec
        //   Wollaston => 14arcsec (the FOV when Wollaston=Yes is 14.3" x slit width)
        //   pixel scale 0.05" => 49arcsec (no XD, IFU or Woll)
        //   pixel scale 0.15" => 99arcsec ( " ")
        //   IFU => 3.15arcsec x 4.8arcsec

        if (_slitWidth == SlitWidth.IFU) {
            return new double[]{3.15, 4.8};
        }

        double w = _slitWidth.getValue();
        double h;
        if (checkWollastonPrism()) {
            h = 14.3;
        } else if (_pixelScale == PixelScale.PS_005) {
            if (checkCrossDispersed()) {
                if (_crossDispersed.equals(CrossDispersed.SXD)) {
                    h = 7.;
                } else {
                    h = 5.1;
                }
            } else {
                h = 49.;
            }
        } else {
            if (checkCrossDispersed()) {
                h = 7.;
            } else {
                h = 99.;
            }
        }
        return new double[]{w, h};
    }

    // ------------------------------------------------------------------------

    /**
     * Get the acquisition mirror value.
     * NOTE: these are not available in the public interface but must be present for properties and to be iterable.
     */
    public AcquisitionMirror getAcquisitionMirror() {
        return _acquisitionMirror;
    }

    /**
     * Set the acquisition mirror.
     */
    public void setAcquisitionMirror(AcquisitionMirror newValue) {
        AcquisitionMirror oldValue = getAcquisitionMirror();
        if (oldValue != newValue) {
            _acquisitionMirror = newValue;
            firePropertyChange(ACQUISITION_MIRROR_PROP.getName(), oldValue, newValue);
        }
    }

    public boolean isOverrideAcqObsWavelength() {
        return _overrideAcqObsWavelength;
    }

    public void setOverrideAcqObsWavelength(boolean newValue) {
        _overrideAcqObsWavelength = newValue;
    }

    // ------------------------------------------------------------------------

    /**
     * Get the decker value.
     * NOTE: these are not available in the public interface but must be present for properties and to be iterable.
     */
    public Decker getDecker() {
        return _decker;
    }

    /**
     * Set the decker
     */
    public void setDecker(Decker newValue) {
        Decker oldValue = getDecker();
        if (oldValue != newValue) {
            _decker = newValue;
            firePropertyChange(DECKER_PROP.getName(), oldValue, newValue);
        }
    }

    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------

    /**
     * Get the well depth  value.
     */
    public WellDepth getWellDepth() {
        return _wellDepth;
    }

    /**
     * Set the decker
     */
    public void setWellDepth(WellDepth newValue) {
        WellDepth oldValue = getWellDepth();
        if (oldValue != newValue) {
            _wellDepth = newValue;
            firePropertyChange(WELL_DEPTH_PROP, oldValue, newValue);
        }
    }

    // ------------------------------------------------------------------------


    /**
     * Get the filter value.
     * NOTE: these are not available in the public interface but must be present for properties and to be iterable.
     */
    public Filter getFilter() {
        return _filter;
    }

    /**
     * Set the filter
     */
    public void setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (oldValue != newValue) {
            _filter = newValue;
            firePropertyChange(FILTER_PROP.getName(), oldValue, newValue);
        }
    }

    // ------------------------------------------------------------------------


    /**
     * Get the camera value.
     * NOTE: these are not available in the public interface but must be present for properties and to be iterable.
     */
    public Camera getCamera() {
        return _camera;
    }

    /**
     * Set the camera
     */
    public void setCamera(Camera newValue) {
        Camera oldValue = getCamera();
        if (oldValue != newValue) {
            _camera = newValue;
            firePropertyChange(CAMERA_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the PixelScale.
     */
    public PixelScale getPixelScale() {
        return _pixelScale;
    }

    /**
     * Set the PixelScale.
     */
    public void setPixelScale(PixelScale newValue) {
        PixelScale oldValue = getPixelScale();
        if (oldValue != newValue) {
            _pixelScale = newValue;
            firePropertyChange(GNIRSConstants.PIXEL_SCALE_PROP, oldValue, newValue);

            if (!_pixelScale.getXdOptions().contains(getCrossDispersed())) {
                setCrossDispersed(_pixelScale.getXdOptions().iterator().next());
            }
        }
    }

    /**
     * Set the PixelScale with a String.
     */
    private void _setPixelScale(String name) {
        PixelScale oldValue = getPixelScale();
        setPixelScale(PixelScale.getPixelScale(name, oldValue));
    }

    // ------------------------------------------------------------------------

    /**
     * Get the Disperser.
     */
    public Disperser getDisperser() {
        return _disperser;
    }

    /**
     * Set the Disperser.
     */
    public void setDisperser(Disperser newValue) {
        Disperser oldValue = getDisperser();
        if (oldValue != newValue) {
            _disperser = newValue;
            firePropertyChange(GNIRSConstants.DISPERSER_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Disperser with a String.
     */
    private void _setDisperser(String name) {
        Disperser oldValue = getDisperser();
        setDisperser(Disperser.getDisperser(name, oldValue, _pixelScale));
    }

    // ------------------------------------------------------------------------

    /**
     * Get the SlitWidth.
     */
    public SlitWidth getSlitWidth() {
        return _slitWidth;
    }

    /**
     * Set the SlitWidth.
     */
    public void setSlitWidth(SlitWidth newValue) {
        SlitWidth oldValue = getSlitWidth();
        if (oldValue != newValue) {
            _slitWidth = newValue;
            firePropertyChange(GNIRSConstants.SLIT_WIDTH_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the SlitWidth with a String.
     */
    private void _setSlitWidth(String name) {
        SlitWidth oldValue = getSlitWidth();
        setSlitWidth(SlitWidth.getSlitWidth(name, oldValue));
    }

    // ------------------------------------------------------------------------

    /**
     * Get the CrossDispersed.
     */
    public CrossDispersed getCrossDispersed() {
        return _crossDispersed;
    }

    public boolean checkCrossDispersed() {
        return _crossDispersed != CrossDispersed.NO;
    }

    /**
     * Set the CrossDispersed.
     */
    public void setCrossDispersed(CrossDispersed newValue) {
        if (!getPixelScale().getXdOptions().contains(newValue)) return;
        CrossDispersed oldValue = getCrossDispersed();
        if (oldValue != newValue) {
            _crossDispersed = newValue;
            firePropertyChange(GNIRSConstants.CROSS_DISPERSED_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the CrossDispersed with a String.
     */
    private void _setCrossDispersed(String name, PixelScale ps) {
        CrossDispersed oldValue = getCrossDispersed();
        setCrossDispersed(CrossDispersed.getCrossDispersed(name, oldValue, ps));
    }

    // ------------------------------------------------------------------------


    /**
     * Get the WollastonPrism.
     */
    public WollastonPrism getWollastonPrism() {
        return _wollastonPrism;
    }

    public boolean checkWollastonPrism() {
        return _wollastonPrism == WollastonPrism.YES;
    }

    /**
     * Set the WollastonPrism.
     */
    public void setWollastonPrism(WollastonPrism newValue) {
        WollastonPrism oldValue = getWollastonPrism();
        if (oldValue != newValue) {
            _wollastonPrism = newValue;
            firePropertyChange(GNIRSConstants.WOLLASTON_PRISM_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the WollastonPrism with a String.
     */
    private void _setWollastonPrism(String name) {
        WollastonPrism oldValue = getWollastonPrism();
        setWollastonPrism(WollastonPrism.getWollastonPrism(name, oldValue));
    }

    // ------------------------------------------------------------------------


    /**
     * Get the ReadMode.
     */
    public ReadMode getReadMode() {
        return _readMode;
    }

    /**
     * Set the ReadMode.
     */
    public void setReadMode(ReadMode newValue) {
        ReadMode oldValue = getReadMode();
        if (oldValue != newValue) {
            _readMode = newValue;
            firePropertyChange(GNIRSConstants.READ_MODE_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Read mode without firing a property change..
     * I had to add this so the method _updateReadModeBasedOnExpTime on EdCompInstGnirs
     * doesn't throw java.lang.IllegalStateException: Attempt to mutate in notification
     * There must be a way to do this in a better way, but I'm wasted now.
     */
    public void setReadModeNoProp(ReadMode newValue) {
        _readMode = newValue;
    }

    /**
     * Set the ReadMode with a String.
     */
    private void _setReadMode(String name) {
        ReadMode oldValue = getReadMode();
        setReadMode(ReadMode.getReadMode(name, oldValue));
    }

    // ------------------------------------------------------------------------

    /**
     * Set the central wavelength in um for the given order and update the
     * values for the other order wavelengths.
     */
    public final void setCentralWavelength(double wavelength, Order order) {
        if ((wavelength > 2.5) && checkCrossDispersed()) {
            setCentralWavelength(2.5);
            return;
        }

        double oldWavelength = _centralWavelength;
        if (_setCentralWavelength(wavelength, order)) {
            firePropertyChange(GNIRSConstants.CENTRAL_WAVELENGTH_ORDER_N_PROP, oldWavelength, wavelength);
        }
    }

    // Set the central wavelength in um for the given order and update the
    // values for the other order wavelengths.
    // Returns true if something changed.
    private boolean _setCentralWavelength(double wavelength, Order order) {
        // keep in range
        double minVal = order.getMinWavelength();
        double maxVal = order.getMaxWavelength();
        if (wavelength < minVal) {
            wavelength = minVal;
        } else if (wavelength > maxVal) {
            wavelength = maxVal;
        }

        // Update _centralWavelength
        if (wavelength != _centralWavelength) {
            _centralWavelength = wavelength;
            _updateWavelengthTable(order);
            return true;
        }
        return false;
    }


    // Update the table of wavelengths based on the current wavelength and given order.
    // Note:
    // order3*(central-wavelength-in-order3) = order4*(central-wavelength-in-order4) =
    // order5*(central-wavelength-in-order5) etc.
    //
    // so, to get central-wavelength-in-order"Y" when you know order"X" and wavelength-in-order"X",
    // you just divide by order"Y".
    //
    // Example:
    //
    // user enters central wavelength=2.2, order=3  (roughly the default)
    //
    // in order 4, central wavelength = 3*2.2/4 = 0.75*2.2 = 1.65
    // in order 5,  " " = 3*2.2/5 = 0.6*2.2 = 1.32
    // etc.
    // order 8, cen. wave.= 3/8*2.2 = 0.825
    private void _updateWavelengthTable(Order order) {
        if (_centralWavelengthOrderN == null) {
            _centralWavelengthOrderN = new double[Order.NUM_ORDERS];
        }
        double d = order.getOrder() * _centralWavelength;
        for (int i = 1; i <= Order.NUM_ORDERS; i++) {
            _centralWavelengthOrderN[i - 1] = d / i;
        }
    }

    // Update the table of wavelengths for each order
    private void _updateWavelengthTable() {
        Order o = Order.getOrder(_centralWavelength, null);
        if (o != null) {
            _updateWavelengthTable(o);
        }
    }

    /**
     * Set the central wavelength in um for the order corresponding to the wavelength
     * range and update the values for the other order wavelengths. The order's min and
     * max wavelength values are used to determine the one to use here.
     */
    public final void setCentralWavelength(double wavelength) {
        Order o = Order.getOrder(wavelength, null);
        if (o != null) {
            setCentralWavelength(wavelength, o);
        }
    }

    public final void setCentralWavelength(Wavelength wavelength) {
        double dvalue = wavelength.doubleValue();
        setCentralWavelength(dvalue);
    }

    public Wavelength getCentralWavelength() {
        return new Wavelength(Double.toString(_centralWavelength));
    }

    /**
     * Return the central wavelength for the given order in um.
     */
    public final double getCentralWavelength(Order order) {
        if (_centralWavelengthOrderN == null) {
            _updateWavelengthTable();
        }
        return _centralWavelengthOrderN[order.getOrder() - 1];
    }

    // ------------------------------------------------------------------------

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, GNIRSConstants.PIXEL_SCALE_PROP, getPixelScale().name());
        Pio.addParam(factory, paramSet, GNIRSConstants.DISPERSER_PROP, getDisperser().name());
        Pio.addParam(factory, paramSet, GNIRSConstants.SLIT_WIDTH_PROP, getSlitWidth().name());
        Pio.addParam(factory, paramSet, GNIRSConstants.CROSS_DISPERSED_PROP, getCrossDispersed().name());
        Pio.addParam(factory, paramSet, GNIRSConstants.WOLLASTON_PRISM_PROP, getWollastonPrism().name());
        Pio.addParam(factory, paramSet, GNIRSConstants.READ_MODE_PROP, getReadMode().name());
        Pio.addParam(factory, paramSet, WELL_DEPTH_PROP, getWellDepth().name());
        Pio.addParam(factory, paramSet, GNIRSConstants.CENTRAL_WAVELENGTH_PROP, getCentralWavelength().getStringValue());

        Pio.addParam(factory, paramSet, ACQUISITION_MIRROR_PROP, getAcquisitionMirror().name());
        Pio.addParam(factory, paramSet, CAMERA_PROP, getCamera().name());
        Pio.addParam(factory, paramSet, DECKER_PROP, getDecker().name());
        Pio.addParam(factory, paramSet, FILTER_PROP, getFilter().name());
        Pio.addParam(factory, paramSet, POS_ANGLE_CONSTRAINT_PROP.getName(), getPosAngleConstraint().name());

        Pio.addBooleanParam(factory, paramSet, OVERRIDE_ACQ_OBS_WAVELENGTH_PROP.getName(), isOverrideAcqObsWavelength());

        Pio.addParam(factory, paramSet, PORT_PROP, port.name());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, GNIRSConstants.PIXEL_SCALE_PROP);
        if (v != null) {
            _setPixelScale(v);
        }
        v = Pio.getValue(paramSet, GNIRSConstants.DISPERSER_PROP);
        if (v != null) {
            _setDisperser(v);
        }
        v = Pio.getValue(paramSet, GNIRSConstants.SLIT_WIDTH_PROP);
        if (v != null) {
            _setSlitWidth(v);
        }
        v = Pio.getValue(paramSet, GNIRSConstants.CROSS_DISPERSED_PROP);
        if (v != null) {
            _setCrossDispersed(v, getPixelScale());
        }
        v = Pio.getValue(paramSet, GNIRSConstants.WOLLASTON_PRISM_PROP);
        if (v != null) {
            _setWollastonPrism(v);
        }
        v = Pio.getValue(paramSet, GNIRSConstants.READ_MODE_PROP);
        if (v != null) {
            _setReadMode(v);
        }
        v = Pio.getValue(paramSet, GNIRSConstants.CENTRAL_WAVELENGTH_PROP);
        if (v != null) {
            setCentralWavelength(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, WELL_DEPTH_PROP);
        if (v != null) {
            setWellDepth(WellDepth.valueOf(v));
        }

        v = Pio.getValue(paramSet, ACQUISITION_MIRROR_PROP);
        if (v != null) {
            setAcquisitionMirror(AcquisitionMirror.getAcquisitionMirror(v));
        }
        v = Pio.getValue(paramSet, CAMERA_PROP);
        if (v != null) {
            setCamera(Camera.getCamera(v));
        }
        v = Pio.getValue(paramSet, DECKER_PROP);
        if (v != null) {
            setDecker(Decker.getDecker(v));
        }
        v = Pio.getValue(paramSet, FILTER_PROP);
        if (v != null) {
            setFilter(Filter.getFilter(v));
        }

        // REL-2090: Special workaround for elimination of former PositionAngleMode, since functionality has been
        // merged with PosAngleConstraint but we still need legacy code.
        v = Pio.getValue(paramSet, POS_ANGLE_CONSTRAINT_PROP.getName());
        final String pam = Pio.getValue(paramSet, "positionAngleMode");
        if ("MEAN_PARALLACTIC_ANGLE".equals(pam))
            _setPosAngleConstraint(PosAngleConstraint.PARALLACTIC_ANGLE);
        else if (v != null)
            _setPosAngleConstraint(v);

        setOverrideAcqObsWavelength(
            Pio.getBooleanValue(paramSet, OVERRIDE_ACQ_OBS_WAVELENGTH_PROP.getName(), true)
        );

        v = Pio.getValue(paramSet, PORT_PROP);
        if (v == null) {
            // Old GNIRS programs w/o an explicit ISS port option were using
            // the side-looking port.
            setIssPort(IssPort.SIDE_LOOKING);
        } else {
            setIssPort(IssPort.valueOf(v));
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.PIXEL_SCALE_PROP, getPixelScale()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.DISPERSER_PROP, getDisperser()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.SLIT_WIDTH_PROP, getSlitWidth()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.CROSS_DISPERSED_PROP, getCrossDispersed()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.WOLLASTON_PRISM_PROP, getWollastonPrism()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.READ_MODE_PROP, getReadMode()));
    //    sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.FILTER_PROP, getFilter()));
        sc.putParameter(DefaultParameter.getInstance(WELL_DEPTH_PROP, getWellDepth()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.ACQUISITION_MIRROR_PROP, getAcquisitionMirror()));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.CENTRAL_WAVELENGTH_PROP, getCentralWavelength()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.POS_ANGLE_PROP, getPosAngleDegrees()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.COADDS_PROP, getCoadds()));
        sc.putParameter(DefaultParameter.getInstance(PORT_PROP, getIssPort()));

        return sc;
    }

    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        final List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(PIXEL_SCALE_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(SLIT_WIDTH_PROP));
        configInfo.add(new InstConfigInfo(CROSS_DISPERSED_PROP));
        configInfo.add(new InstConfigInfo(READ_MODE_PROP));
        configInfo.add(new InstConfigInfo(CENTRAL_WAVELENGTH_PROP, false));

        // REL-379: REQUIREMENT: The OT browser must include the GNIRS acquisition mirror,
        // filters, ISS port, well depth, camera and decker.
//        configInfo.add(new InstConfigInfo(ACQUISITION_MIRROR_PROP));
//        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(PORT_PROP));
        configInfo.add(new InstConfigInfo(WELL_DEPTH_PROP));
//        configInfo.add(new InstConfigInfo(CAMERA_PROP));
//        configInfo.add(new InstConfigInfo(DECKER_PROP));

        return configInfo;
    }

    private static Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(GnirsOiwfsGuideProbe.instance);

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    /**
     * Get the ISS Port
     */
    @Override
    public IssPort getIssPort() {
        if (port == null) return IssPort.UP_LOOKING;
        return port;
    }

    /**
     * Set the Port.
     */
    @Override
    public void setIssPort(IssPort newValue) {
        IssPort oldValue = getIssPort();
        if (oldValue != newValue) {
            port = newValue;
            firePropertyChange(PORT_PROP, oldValue, newValue);
        }
    }

    @Override
    public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        final ReadMode readMode = (ReadMode) cur.getItemValue(ReadMode.KEY);
        final int coadds = ExposureCalculator.instance.coadds(cur);
        final double secs = GnirsReadoutTime.getReadoutOverhead(readMode, coadds);
        final CategorizedTime readout = CategorizedTime.fromSeconds(Category.READOUT, secs);
        final CategorizedTime dhs = CategorizedTime.fromSeconds(Category.DHS_WRITE, 2.8); // REL-1678
        return DefaultStepCalculator.instance.calc(cur, prev).addAll(readout, dhs);
    }


    private static final ItemKey CAL_EXP_TIME_KEY = new ItemKey("calibration:exposureTime");

    private static boolean isCalStep(Config c) {
        return CalConfigBuilderUtil.isCalStep(c);
    }

    private static Double calExposureTime(Config c) {
        // The value should be a Double here after a change in 2012B in which
        // value mapping is done only after all other sequence manipulation is
        // done.  Nevertheless, I'll leave the code for handing a String value.
        Object expTime = c.getItemValue(CAL_EXP_TIME_KEY);
        if (expTime == null) return null;

        if (expTime instanceof Double) return (Double) expTime;
        if (expTime instanceof String) return Double.parseDouble(expTime.toString());
        return null;
    }

    @Override public ConfigSequence postProcessSequence(ConfigSequence in) {
        final Config[] configs = in.getAllSteps();

        for (Config c : configs) {
            // Override the observing wavelength for acquisition steps.  It must
            // match the filter wavelength in this case (unless this is an old
            // executed pre-2017A observation in which case we must continue to
            // use the old method for calculating the observing wavelength).
            final AcquisitionMirror am = (AcquisitionMirror) c.getItemValue(GNIRSConstants.ACQUISITION_MIRROR_KEY);
            if (isOverrideAcqObsWavelength() && (am == AcquisitionMirror.IN)) {
                final Option<Filter> f  = ImOption.apply((Filter) c.getItemValue(GNIRSConstants.FILTER_KEY));
                final Option<Double> wl = f.flatMap(f0 -> ImOption.apply(f0.wavelength()));
                // Sorry, yes observing wavelength stored as a String for GNRIS.
                wl.foreach(d -> c.putItem(GNIRSConstants.OBSERVING_WAVELENGTH_KEY, String.format("%.2f", d)));
            }

            if (isCalStep(c)) {
                final Double expTime = calExposureTime(c);
                if (expTime != null) {
                    c.putItem(ReadMode.KEY, selectCalReadMode(expTime));
                }
            }

            // Fill in the default camera value if necessary.
            final Object explicitCamera = c.getItemValue(GNIRSConstants.CAMERA_KEY);
            if (explicitCamera == null) {
                final Wavelength cwl = (Wavelength) c.getItemValue(GNIRSConstants.CENTRAL_WAVELENGTH_KEY);
                final PixelScale  ps = (PixelScale) c.getItemValue(GNIRSConstants.PIXEL_SCALE_KEY);
                final Camera  camera = (cwl == null) || (ps == null) ? Camera.DEFAULT : Camera.getDefault(cwl.doubleValue(), ps);
                c.putItem(GNIRSConstants.CAMERA_KEY, camera);
            }
        }

        return new ConfigSequence(configs);
    }

    /**
     * Selects the read mode to use for calibration datasets with the given
     * exposure time in seconds.
     */
    public static ReadMode selectCalReadMode(double exposure) {
        // REL-205
        return (exposure < 0.6) ? ReadMode.VERY_BRIGHT : ReadMode.BRIGHT;
    }

    public static ReadMode selectReadMode(double exposure) {
        if (exposure < 0.6) {
            return ReadMode.VERY_BRIGHT;
        } else if (exposure <= 20.0) {
            return ReadMode.BRIGHT;
        } else if (exposure <= 60) {
            return ReadMode.FAINT;
        } else {
            return ReadMode.VERY_FAINT;
        }
    }

    public CalibrationKey extractKey(ISysConfig instrumentConfig) {
        GNIRSParams.PixelScale pixelScale = (GNIRSParams.PixelScale) get(instrumentConfig, InstGNIRS.PIXEL_SCALE_PROP);
        GNIRSParams.Disperser disperser = (GNIRSParams.Disperser) get(instrumentConfig, InstGNIRS.DISPERSER_PROP);
        GNIRSParams.CrossDispersed crossDispersed = (GNIRSParams.CrossDispersed) get(instrumentConfig, InstGNIRS.CROSS_DISPERSED_PROP);
        GNIRSParams.SlitWidth slitWidth = (GNIRSParams.SlitWidth) get(instrumentConfig, InstGNIRS.SLIT_WIDTH_PROP);
        GNIRSParams.WellDepth wellDepth = (GNIRSParams.WellDepth) get(instrumentConfig, InstGNIRS.WELL_DEPTH_PROP);

        // check mode imaging if acquisition mirror is in, spectroscopy otherwise
        CalibrationProvider.GNIRSMode mode = CalibrationProvider.GNIRSMode.SPECTROSCOPY;
        IParameter acquisitionMirrorParameter = instrumentConfig.getParameter("acquisitionMirror");
        if (acquisitionMirrorParameter != null) {
            if (acquisitionMirrorParameter.getValue().toString().equalsIgnoreCase("in")) {
                mode = CalibrationProvider.GNIRSMode.IMAGING;
            }
        }

        ConfigKeyGnirs config = new ConfigKeyGnirs(mode, pixelScale, disperser, crossDispersed, slitWidth, wellDepth);
        return new CalibrationKeyImpl.WithWavelength(config, getWavelength(instrumentConfig));
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(5.0);
    private static final Angle PWFS2_VIG = Angle.arcmins(4.8);

    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance() { return PWFS2_VIG; }


    /**
     * Implementation of methods from PosAngleConstraintAware and support.
     */
    @Override
    public PosAngleConstraint getPosAngleConstraint() {
        return (_posAngleConstraint == null) ? PosAngleConstraint.FIXED : _posAngleConstraint;
    }

    @Override
    public void setPosAngleConstraint(PosAngleConstraint newValue) {
        PosAngleConstraint oldValue = getPosAngleConstraint();
        if (oldValue != newValue) {
            _posAngleConstraint = newValue;
            firePropertyChange(POS_ANGLE_CONSTRAINT_PROP.getName(), oldValue, newValue);
        }
    }

    private void _setPosAngleConstraint(final String name) {
        final PosAngleConstraint oldValue = getPosAngleConstraint();
        try {
            _posAngleConstraint = PosAngleConstraint.valueOf(name);
        } catch (Exception ex) {
            _posAngleConstraint = oldValue;
        }
    }

    private void _setPosAngleConstraint(final PosAngleConstraint pac) {
        _posAngleConstraint = pac;
    }

    @Override
    public ImList<PosAngleConstraint> getSupportedPosAngleConstraints() {
        return DefaultImList.create(PosAngleConstraint.FIXED,
                                    PosAngleConstraint.PARALLACTIC_ANGLE,
                                    PosAngleConstraint.PARALLACTIC_OVERRIDE);
    }

    @Override
    public boolean allowUnboundedPositionAngle() {
        // Unsupported for GNIRS.
        return false;
    }
}
