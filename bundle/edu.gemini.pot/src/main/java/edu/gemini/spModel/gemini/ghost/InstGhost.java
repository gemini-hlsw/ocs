package edu.gemini.spModel.gemini.ghost;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.core.Site;
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
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.telescope.PosAngleConstraint;
//import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGhost;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;

import static edu.gemini.spModel.obscomp.InstConstants.EXPOSURE_TIME_KEY;


/**
 * The Ghost instrument.
 */
public class InstGhost
        extends ParallacticAngleSupportInst implements IssPortProvider,
            StepCalculator, PropertyProvider,  ItcOverheadProvider {


    private static final Logger LOG = Logger.getLogger(InstGhost.class.getName());

    // for serialization
    private static final long serialVersionUID = 1L;

    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_GHOST;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    /**
     * This obs component's SP type.
     */

    public static final double DEF_EXPOSURE_TIME = 300.0; // sec
    public static final int DEF_COADDS = 0;
    public static final PropertyDescriptor CCD_X_BIN_PROP;
    public static final PropertyDescriptor CCD_Y_BIN_PROP;

    public static final PropertyDescriptor PORT_PROP;

    public static final PropertyDescriptor AMP_GAIN_CHOICE_PROP;

    public static final ItemKey X_BIN_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "ccdXBinning");
    public static final ItemKey Y_BIN_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "ccdYBinning");


    //private GhostType.Binning _xBin = GhostType.Binning.DEFAULT;
    //private GhostType.Binning _yBin = GhostType.Binning.DEFAULT;
    private PosAngleConstraint _posAngleConstraint = PosAngleConstraint.FIXED;

//    private GhostType.AmpGain _ampGain = GhostType.AmpGain.DEFAULT;
//    private GhostType.AmpGain _gainChoice = GhostType.AmpGain.DEFAULT;
//    private GhostType.ReadMode _readMode = GhostType.ReadMode.DEFAULT;

   private static final Duration SETUP_TIME = Duration.ofSeconds(900); // This value was provided by Venus
    // should be added 900 seconds of the SETUP_TIME
    public static final Duration REACQUISITION_TIME = Duration.ofSeconds(300);

    private IssPort _port = IssPort.SIDE_LOOKING;

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstGhost.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    // Initialize the properties.
    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        PORT_PROP = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no);

        AMP_GAIN_CHOICE_PROP = initProp("gainChoice", false, true);
        AMP_GAIN_CHOICE_PROP.setDisplayName("Gain Choice");
        CCD_X_BIN_PROP = initProp(X_BIN_KEY.getName(), query_yes, iter_yes);
        CCD_X_BIN_PROP.setDisplayName("X Bin");
        CCD_X_BIN_PROP.setShortDescription("X Binning Factor");
        CCD_Y_BIN_PROP = initProp(Y_BIN_KEY.getName(), query_yes, iter_yes);
        CCD_Y_BIN_PROP.setDisplayName("Y Bin");
        CCD_Y_BIN_PROP.setShortDescription("Y Binning Factor");
    }



    /*
    @Override
    public CalibrationKey extractKey(ISysConfig instrument) {
        //-- get some common values
        GhostType.Binning xBin = (GhostType.Binning) get(instrument, InstGhost.CCD_X_BIN_PROP);
        GhostType.Binning yBin = (GhostType.Binning) get(instrument, InstGhost.CCD_Y_BIN_PROP);
        GhostType.AmpGain ampGain = (GhostType.AmpGain) get(instrument, InstGhost.AMP_GAIN_CHOICE_PROP);

        // check order and wavelength (values will only be present in case of spectroscopy)
        Double wavelength = getWavelength(instrument) * 1000.; // adjust scaling of wavelength from um to nm (as used in config tables)
        ConfigKeyGhost config = new ConfigKeyGhost(xBin, yBin, ampGain);
        return new CalibrationKeyImpl.WithWavelength(config, wavelength);
    }
    */


    @Override
    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public InstGhost() {
        super(SPComponentType.INSTRUMENT_GHOST);
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

    @Override
    public Duration getSetupTime(ISPObservation obs) {
        LOG.info("LALALALALALLALALALALAALLA1. This getSetupTime(ISPOBservation obs) is not implemented. Default value is return");
        return SETUP_TIME;
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated @Override
    public Duration getSetupTime(Config conf) {
        // TODO. Venu has to confirm the values.
        //return Duration.ofSeconds(SETUP_TIME_IFU_ARM.getSeconds() +
        //                          SETUP_TIME_INST_GUIDING.getSeconds() +
        //                          SETUP_AVG_TIME_ADQ.getSeconds());
        LOG.info("TODO. Venu has to confirm the values. getSetupTime: "+conf.getItemValue(EXPOSURE_TIME_KEY) + " " + ExposureCalculator.instance.totalExposureTimeSec(conf));

        return SETUP_TIME;
    }

    /**
     * Return the current ccd amp readout speed.
     */
//    public GhostType.ReadMode getReadMode() {
//        return _readMode;
//    }

    /**
     * Return the current CCD amp gain range.
     */
//    public GhostType.AmpGain getGainChoice() {
//        return _gainChoice;
//    }
//
//    /**
//     * Set the CCD amp gain choice.
//     */
//    public void setGainChoice(GhostType.AmpGain newValue) {
//        GhostType.AmpGain oldValue = getGainChoice();
//        if (newValue != oldValue) {
//            _gainChoice = newValue;
//            firePropertyChange(AMP_GAIN_CHOICE_PROP.getName(), oldValue, newValue);
//        }
//    }
//
//    /**
//     * Set the AmpGain with a String.
//     */
//    protected void _setGainChoice(String name) {
//        GhostType.AmpGain oldValue = getGainChoice();
//        setGainChoice(GhostType.AmpGain.getAmpGain(name, oldValue));
//    }

    @Override
    public Option<edu.gemini.spModel.core.Angle> calculateParallacticAngle(ISPObservation obs) {
        System.out.println("TODO. calculateParallacticAngle is not implemented the default values is 90");
        return super.calculateParallacticAngle(obs).map(angle -> angle.$plus(Angle$.MODULE$.fromDegrees(90)));
    }
    /**
     * Set the X CCD binning.
     */
//    public void setCcdXBinning(GhostType.Binning newValue) {
//        GhostType.Binning oldValue = getCcdXBinning();
//        if (newValue != oldValue) {
//            _xBin = newValue;
//            firePropertyChange(CCD_X_BIN_PROP.getName(), oldValue, newValue);
//        }
//    }
//
//    /**
//     * Return the current X CCD binning value.
//     */
//    public GhostType.Binning getCcdXBinning() {
//        return _xBin;
//    }
//
//    /**
//     * Set the Y CCD binning.
//     */
//    public void setCcdYBinning(GhostType.Binning newValue) {
//        GhostType.Binning oldValue = getCcdYBinning();
//        if (newValue != oldValue) {
//            _yBin = newValue;
//            firePropertyChange(CCD_Y_BIN_PROP.getName(), oldValue, newValue);
//        }
//    }
//
//    /**
//     * Return the current Y CCD binning value.
//     */
//    public GhostType.Binning getCcdYBinning() {
//        return _yBin;
//    }
//
//    // Private routine to set x binning with String
//    private void _setXBinning(String newXValue) {
//        GhostType.Binning oldXValue = getCcdXBinning();
//        setCcdXBinning(GhostType.Binning.getBinning(newXValue, oldXValue));
//    }
//
    // Private routine to set y binning with String
//    private void _setYBinning(String newYValue) {
//        GhostType.Binning oldYValue = getCcdYBinning();
//        setCcdYBinning(GhostType.Binning.getBinning(newYValue, oldYValue));
//    }

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
        double readoutTime = GhostReadoutTime.getReadoutOverhead(cur);
        System.out.println("*** readoutTime: " + readoutTime );
        times.add(CategorizedTime.fromSeconds(Category.READOUT, readoutTime));
        System.out.println("DHS suppouse value: " + getDhsWriteTime().time);
        //times.add(getDhsWriteTime());
        return CommonStepCalculator.instance.calc(cur, prev).addAll(times);
    }

    @Override
    public Duration getReacquisitionTime(ISPObservation obs) {
        LOG.info("TODO, ######################### getReacquisitonTime(ConISPObservation obs) method");
        // Venu described that for each 120 minutes of observation the setup time is increased with 900 seconds.
        /*
        int t = (int) ( _exposureTime / REACQUISTION_TIME.getSeconds());
        if (t == 0)
            return Duration.ZERO;
        return Duration.ofSeconds(t*SETUP_TIME.getSeconds());
         */
        return REACQUISITION_TIME;
    }

    @Override
    public Duration getReacquisitionTime(Config conf) {
        LOG.info("TODO, &&&&&&&&&&&&&&&&&& getReacquisitonTime(Config conf) method &&&&&&&&&&&&");
        /*double exposureTime = ExposureCalculator.instance.exposureTimeSec(conf);
        System.out.println("exposureTime is: "+ exposureTime);
        System.out.println("$$$$$$$$$$$$$$$$$$$$$");
        int t = (int) ( exposureTime / REACQUISTION_TIME.getSeconds());
        if (t == 0)
            return Duration.ZERO;

        return Duration.ofSeconds(t*SETUP_TIME.getSeconds());
         */
        return REACQUISITION_TIME;

    }

    /**
     * This needs to be overridden to support the PosAngleConstraint.
     */
    public PosAngleConstraint getPosAngleConstraint() {
        return (_posAngleConstraint == null) ? PosAngleConstraint.FIXED : _posAngleConstraint;
    }


    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

}
