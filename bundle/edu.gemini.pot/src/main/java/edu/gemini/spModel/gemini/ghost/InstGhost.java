package edu.gemini.spModel.gemini.ghost;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;

import edu.gemini.spModel.gemini.parallacticangle.ParallacticAngleSupportInst;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGhost;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;



/**
 * The Ghost instrument.
 */
public class InstGhost
        extends ParallacticAngleSupportInst implements IssPortProvider, PosAngleConstraintAware,
            StepCalculator, PropertyProvider, CalibrationKeyProvider, ItcOverheadProvider {

    private static final Logger LOG = Logger.getLogger(InstGhost.class.getName());

    // for serialization
    private static final long serialVersionUID = 1L;

    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_GHOST;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    /**
     * This obs component's SP type.
     */
    public static final SPComponentBroadType BROAD_TYPE = SPComponentBroadType.INSTRUMENT;



    public static final double DEF_EXPOSURE_TIME = 300.0; // sec
    public static final int DEF_COADDS = 1;

    // The size of the detector in arc secs
    public static final double DETECTOR_WIDTH = 330.34;
    public static final double DETECTOR_HEIGHT = 330.34;

    public static final PropertyDescriptor AMP_GAIN_CHOICE_PROP;

    //public static final PropertyDescriptor AMP_GAIN_SETTING_PROP;

    public static final PropertyDescriptor CCD_X_BIN_PROP;
    public static final PropertyDescriptor CCD_Y_BIN_PROP;


    public static final PropertyDescriptor PORT_PROP;

    public static final ItemKey X_BIN_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "ccdXBinning");
    public static final ItemKey Y_BIN_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "ccdYBinning");

    public static final PropertyDescriptor POS_ANGLE_CONSTRAINT_PROP;


    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstGhost.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    // Initialize the properties.
    static {

        System.out.println("********************** Initial static block ");
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        System.out.println("after RuntimeExeception ");
        PORT_PROP = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no);

        AMP_GAIN_CHOICE_PROP = initProp("gainChoice", false, true);
        AMP_GAIN_CHOICE_PROP.setDisplayName("Gain Choice");

        POS_ANGLE_CONSTRAINT_PROP = initProp("posAngleConstraint", query_no, iter_no);


        CCD_X_BIN_PROP = initProp(X_BIN_KEY.getName(), query_yes, iter_yes);
        CCD_X_BIN_PROP.setDisplayName("X Bin");
        CCD_X_BIN_PROP.setShortDescription("X Binning Factor");
        CCD_Y_BIN_PROP = initProp(Y_BIN_KEY.getName(), query_yes, iter_yes);
        CCD_Y_BIN_PROP.setDisplayName("Y Bin");
        CCD_Y_BIN_PROP.setShortDescription("Y Binning Factor");

        System.out.println("Finish the static method ");

    }



    @Override
    public CalibrationKey extractKey(ISysConfig instrument) {
        //-- get some common values
        GhostType.Binning xBin = (GhostType.Binning) get(instrument, InstGhost.CCD_X_BIN_PROP);
        GhostType.Binning yBin = (GhostType.Binning) get(instrument, InstGhost.CCD_Y_BIN_PROP);
        GhostType.AmpGain ampGain = (GhostType.AmpGain) get(instrument, InstGhost.AMP_GAIN_CHOICE_PROP);

        // check order and wavelength (values will only be present in case of spectroscopy)
        Double wavelength = getWavelength(instrument) * 1000.; // adjust scaling of wavelength from um to nm (as used in config tables)
        IParameter orderParameter = instrument.getParameter("disperserOrder");


        ConfigKeyGhost config = new ConfigKeyGhost(xBin, yBin, ampGain);
        return new CalibrationKeyImpl.WithWavelength(config, wavelength);
    }

    private GhostType.Binning _xBin = GhostType.Binning.DEFAULT;
    private GhostType.Binning _yBin = GhostType.Binning.DEFAULT;
    private PosAngleConstraint _posAngleConstraint = PosAngleConstraint.FIXED;

    private GhostType.AmpGain _ampGain = GhostType.AmpGain.DEFAULT;
    private GhostType.AmpGain _gainChoice = GhostType.AmpGain.DEFAULT;
    private GhostType.ReadMode _readMode = GhostType.ReadMode.DEFAULT;

    private IssPort _port = IssPort.SIDE_LOOKING;


    @Override
    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }


    private int _detectorRows;


    public InstGhost() {
        super(SPComponentType.INSTRUMENT_GHOST);
    }

    /**
     * Constructor
     */
    public InstGhost(SPComponentType type) {
        super(type);


    }

    private void initParamDefault() {
        // Override the default exposure time
        _exposureTime = DEF_EXPOSURE_TIME;
        _coadds = DEF_COADDS;
        // calculate these values from the defaults
        //  _updateDetectorRows();
    }


    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        InstGhost result = (InstGhost) super.clone();
        return result;
    }

    public String getPhaseIResourceName() {
        return "gemGHOST";
    }


    private static final Duration SETUP_TIME_LS_SPECTROSCOPY = Duration.ofMinutes(16);
    //private static final Duration SETUP_TIME_IFU_MOS         = Duration.ofMinutes(18);
    private static final Duration SETUP_TIME_IFU_MOS         = Duration.ofMinutes(0);

    @Override
    public Duration getSetupTime(ISPObservation obs) {
        System.out.println("TODO. Not implemented yet getSetupTime(ISPObservation) ");
        //if (_disperser.isMirror()) return SETUP_TIME_IMAGING;
        //if (_fpu.isSpectroscopic() || _fpu.isNSslit()) return SETUP_TIME_LS_SPECTROSCOPY;
        return SETUP_TIME_IFU_MOS;
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated @Override
    public Duration getSetupTime(Config conf) {
        System.out.println("TODO. Not implemented yet getSetupTime(Config) ");
        return SETUP_TIME_IFU_MOS;
    }

    /**
     * Return the science area based upon the current camera.
     */
    public double[] getScienceArea() {
        //return GhostScienceAreaGeometry.javaScienceAreaDimensions(this.getFPUnit());
        System.out.println("TODO. Not implemented yet");
        return GhostScienceAreaGeometry.javaScienceAreaDimensions(1.0);

    }


    /**
     * Return the current ccd amp readout speed.
     */
    public GhostType.ReadMode getReadMode() {
        return _readMode;
    }



    /**
     * This convenience method implements the algorithm for determining
     * the actual CCD Gain value based upon the CCD choices actually
     * selected.
     */
    public static int getActualGain(final GhostType.AmpGain gain,
                                       final GhostType.ReadMode readMode) {

        LOG.info("TODO. Not implemented. It is necessary to set the different gain for each CCD type");
        return 1;
    }



    /**
     * Return the current CCD amp gain range.
     */
    public GhostType.AmpGain getGainChoice() {
        return _gainChoice;
    }

    /**
     * Set the CCD amp gain choice.
     */
    public void setGainChoice(GhostType.AmpGain newValue) {
        GhostType.AmpGain oldValue = getGainChoice();
        if (newValue != oldValue) {
            _gainChoice = newValue;
            firePropertyChange(AMP_GAIN_CHOICE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the AmpGain with a String.
     */
    protected void _setGainChoice(String name) {
        GhostType.AmpGain oldValue = getGainChoice();
        setGainChoice(GhostType.AmpGain.getAmpGain(name, oldValue));
    }
    
    @Override
    public Option<edu.gemini.spModel.core.Angle> calculateParallacticAngle(ISPObservation obs) {
        System.out.println("TODO. calculateParallacticAngle is not implemented the default values is 90");
        return super.calculateParallacticAngle(obs).map(angle -> angle.$plus(Angle$.MODULE$.fromDegrees(90)));
    }

    /**
     * Is Ghost in imaging mode.
     */
    public boolean isImaging() {

        return true;
    }


    /**
     * Set the X CCD binning.
     */
    public void setCcdXBinning(GhostType.Binning newValue) {
        GhostType.Binning oldValue = getCcdXBinning();
        if (newValue != oldValue) {
            _xBin = newValue;
            firePropertyChange(CCD_X_BIN_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the current X CCD binning value.
     */
    public GhostType.Binning getCcdXBinning() {
        return _xBin;
    }

    /**
     * Set the Y CCD binning.
     */
    public void setCcdYBinning(GhostType.Binning newValue) {
        GhostType.Binning oldValue = getCcdYBinning();
        if (newValue != oldValue) {
            _yBin = newValue;
            firePropertyChange(CCD_Y_BIN_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the current Y CCD binning value.
     */
    public GhostType.Binning getCcdYBinning() {
        return _yBin;
    }

    // Private routine to set x binning with String
    private void _setXBinning(String newXValue) {
        GhostType.Binning oldXValue = getCcdXBinning();
        setCcdXBinning(GhostType.Binning.getBinning(newXValue, oldXValue));
    }

    // Private routine to set y binning with String
    private void _setYBinning(String newYValue) {
        GhostType.Binning oldYValue = getCcdYBinning();
        setCcdYBinning(GhostType.Binning.getBinning(newYValue, oldYValue));
    }

    /**
     * Get the Ghost Port
     */
    public IssPort getIssPort() {
        // for compatibility with previous versions
        if (_port == null) _port = IssPort.DEFAULT;
        return _port;
    }

    /**
     * Set the Port.
     */
    public void setIssPort(IssPort newValue) {
        IssPort oldValue = getIssPort();
        if (oldValue != newValue) {
            _port = newValue;
            firePropertyChange(PORT_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Port with a String.
     */
    private void _setPort(String name) {
        IssPort oldValue = getIssPort();
        setIssPort(IssPort.getPort(name, oldValue));
    }


    @Override
    public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        final Collection<CategorizedTime> times = new ArrayList<>();

        double exposureTime = ExposureCalculator.instance.exposureTimeSec(cur);


        times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, exposureTime));



        double readoutTime = 1.0;
        times.add(CategorizedTime.fromSeconds(Category.READOUT, readoutTime));

        times.add(getDhsWriteTime());

        return CommonStepCalculator.instance.calc(cur, prev).addAll(times);
    }


    // The reacquisition time.
    //private static final Duration REACQUISITION_TIME = Duration.ofMinutes(5);
    private static final Duration REACQUISITION_TIME = Duration.ofMinutes(0);

    @Override
    public Duration getReacquisitionTime(ISPObservation obs) {
        return REACQUISITION_TIME;
    }

    @Override
    public Duration getReacquisitionTime(Config conf) {
        return REACQUISITION_TIME;
    }



    /**
     * This needs to be overridden to support the PosAngleConstraint.
     */
    public PosAngleConstraint getPosAngleConstraint() {
        return (_posAngleConstraint == null) ? PosAngleConstraint.FIXED : _posAngleConstraint;
    }

    public void setPosAngleConstraint(PosAngleConstraint newValue) {
        PosAngleConstraint oldValue = getPosAngleConstraint();
        if (!oldValue.equals(newValue)) {
            _posAngleConstraint = newValue;
            firePropertyChange(POS_ANGLE_CONSTRAINT_PROP.getName(), oldValue, newValue);
        }
    }


    @Override
    public ImList<PosAngleConstraint> getSupportedPosAngleConstraints() {
        return DefaultImList.create(PosAngleConstraint.FIXED,
                                    PosAngleConstraint.FIXED_180,
                                    PosAngleConstraint.UNBOUNDED,
                                    PosAngleConstraint.PARALLACTIC_ANGLE,
                                    PosAngleConstraint.PARALLACTIC_OVERRIDE);
    }

    @Override
    public boolean allowUnboundedPositionAngle() {
        // Note that we disable unbounded position angle as an option for MOS preimaging and FPU Custom Mask.
        System.out.println("TODO. allowUnboundedPositionAngle not implemented ");
        return true;
    }

    // REL-814 Preserve the FPU Custom Mask Name
    @Override
    public void restoreScienceDetails(final SPInstObsComp oldData) {
        super.restoreScienceDetails(oldData);
        if (oldData instanceof InstGhost) {
            final InstGhost oldGhost = (InstGhost)oldData;

        }
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

}
