package edu.gemini.spModel.gemini.bhros;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.bhros.BHROSParams.*;
import edu.gemini.spModel.gemini.bhros.ech.BRayLib;
import edu.gemini.spModel.gemini.bhros.ech.Echellogram;
import edu.gemini.spModel.gemini.bhros.ech.HROSHardwareConstants;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioNodeParent;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.util.*;

//$Id: InstBHROS.java 33533 2011-04-08 15:47:09Z nbarriga $

public final class InstBHROS extends SPInstObsComp implements PropertyProvider {

    private static final long serialVersionUID = 1L;

    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_BHROS;

    public static final ISPNodeInitializer<ISPObsComponent, InstBHROS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstBHROS(), c -> new InstBHROSCB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Properties
    public static final PropertyDescriptor CENTRAL_WAVELENGTH_PROP;
    public static final PropertyDescriptor CCD_CENTRE_X_PROP;
    public static final PropertyDescriptor CCD_CENTRE_Y_PROP;
    public static final PropertyDescriptor ECHELLE_AZ_PROP;
    public static final PropertyDescriptor ECHELLE_ALT_PROP;
    public static final PropertyDescriptor GONI_ANG_PROP;
    public static final PropertyDescriptor POST_SLIT_FILTER_PROP;
    public static final PropertyDescriptor HARTMANN_FLAP_PROP;
    public static final PropertyDescriptor ENTRANCE_FIBRE_PROP;
    public static final PropertyDescriptor CCD_XBINNING_PROP;
    public static final PropertyDescriptor CCD_YBINNING_PROP;
    public static final PropertyDescriptor CCD_SPEED_PROP;
    public static final PropertyDescriptor CCD_AMPLIFIERS_PROP;
    public static final PropertyDescriptor CCD_GAIN_PROP;
    public static final PropertyDescriptor ROI_PROP;
    public static final PropertyDescriptor ISS_PORT_PROP;
    public static final PropertyDescriptor EXPOSURE_METER_FILTER_PROP;

    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;


    public static final String INSTRUMENT_NAME_PROP = "bHROS";

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstBHROS.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }
    static {
        boolean query_yes = true;
        boolean iter_yes = true;
        boolean query_no = false;
        boolean iter_no = false;

        CENTRAL_WAVELENGTH_PROP         = initProp("CentralWavelength", query_no, iter_yes);
        CCD_CENTRE_X_PROP               = initProp("CCDCentreX", query_no, iter_yes);
        CCD_CENTRE_Y_PROP               = initProp("CCDCentreY", query_no, iter_yes);
        ECHELLE_AZ_PROP                 = initProp("EchelleAz", query_no, iter_no);
        ECHELLE_ALT_PROP                = initProp("EchelleALT", query_no, iter_no);
        GONI_ANG_PROP                   = initProp("GoniAng", query_no, iter_no);
        POST_SLIT_FILTER_PROP           = initProp("PostSlitFilter", query_yes, iter_yes);
        POST_SLIT_FILTER_PROP.setDisplayName("Post-Slit Filter");
        HARTMANN_FLAP_PROP              = initProp("HartmannFlap", query_yes, iter_yes);
        ENTRANCE_FIBRE_PROP             = initProp("EntranceFibre", query_yes, iter_yes);
        CCD_XBINNING_PROP               = initProp("CCDXBinning", query_yes, iter_yes);
        CCD_XBINNING_PROP.setDisplayName("CCD X-Binning");
        CCD_YBINNING_PROP               = initProp("CCDYBinning", query_yes, iter_yes);
        CCD_YBINNING_PROP.setDisplayName("CCD Y-Binning");
        CCD_SPEED_PROP                  = initProp("CCDSpeed", query_yes, iter_yes);
        CCD_SPEED_PROP.setDisplayName("CCD Speed");
        CCD_AMPLIFIERS_PROP             = initProp("CCDAmplifiers", query_yes, iter_yes);
        CCD_AMPLIFIERS_PROP.setDisplayName("CCD Amplifiers");
        CCD_GAIN_PROP                   = initProp("CCDGain", query_yes, iter_yes);
        CCD_GAIN_PROP.setDisplayName("CCD Gain");
        ROI_PROP                        = initProp("ROI", query_yes, iter_no);
        ROI_PROP.setDisplayName("Region of Interest");
        ISS_PORT_PROP                   = initProp("ISSPort", query_yes, iter_yes);
        ISS_PORT_PROP.setDisplayName("ISS Port");
        EXPOSURE_METER_FILTER_PROP      = initProp("ExposureMeterFilter", query_yes, iter_yes);

        EXPOSURE_TIME_PROP              = initProp("exposureTime", query_no, iter_yes);
        POS_ANGLE_PROP                  = initProp("posAngle", query_no, iter_yes);
    }


    private double centralWavelength = 0.4334; // microns
    private double ccdCentreX = 0.0;
    private double ccdCentreY = HROSHardwareConstants.BLUE_YCENTRE;
    private PostSlitFilter postSlitFilter = PostSlitFilter.DEFAULT;
    private HartmannFlap hartmannFlap = HartmannFlap.DEFAULT;
    private EntranceFibre entranceFibre = EntranceFibre.DEFAULT;
    private CCDXBinning ccdXBinning = CCDXBinning.DEFAULT;
    private CCDYBinning ccdYBinning = CCDYBinning.DEFAULT;
    private CCDSpeed ccdSpeed = CCDSpeed.DEFAULT;
    private CCDReadoutPorts ccdAmplifiers = CCDReadoutPorts.DEFAULT;
    private CCDGain ccdGain = CCDGain.DEFAULT;
    private ROI roi = ROI.DEFAULT;
    private ISSPort issPort = ISSPort.DEFAULT;
    private ExposureMeterFilter exposureMeterFilter = ExposureMeterFilter.DEFAULT;

    private transient double echelleAz;
    private transient double echelleAlt;
    private transient double goniAng;
    private transient boolean initializedEchelle;

    private static final String VERSION =  "2006B-1";

    public InstBHROS() {
        super(SP_TYPE);
        setVersion(VERSION);
    }

    public Object clone() {
        return super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_NONE;
    }

    public String getPhaseIResourceName() {
        return "gembHROS"; //matches what GeminiData.xml has for the Phase1Resource
    }

    public double getSetupTime(ISPObservation obs) {
        // OT-427: setup time should be 30 minutes
        return 60 * 30;
    }

    public double[] getScienceArea() {
        return entranceFibre.getScienceArea();
    }

    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);
        Pio.addParam(factory, paramSet, CCD_AMPLIFIERS_PROP, ccdAmplifiers.name());
        Pio.addParam(factory, paramSet, CCD_CENTRE_X_PROP, Double.toString(ccdCentreX));
        Pio.addParam(factory, paramSet, CCD_CENTRE_Y_PROP, Double.toString(ccdCentreY));
        Pio.addParam(factory, paramSet, CCD_GAIN_PROP, ccdGain.name());
        Pio.addParam(factory, paramSet, CCD_SPEED_PROP, ccdSpeed.name());
        Pio.addParam(factory, paramSet, CCD_XBINNING_PROP, ccdXBinning.name());
        Pio.addParam(factory, paramSet, CCD_YBINNING_PROP, ccdYBinning.name());
        Pio.addParam(factory, paramSet, CENTRAL_WAVELENGTH_PROP, Double.toString(centralWavelength));
        Pio.addParam(factory, paramSet, ECHELLE_ALT_PROP, Double.toString(echelleAlt));
        Pio.addParam(factory, paramSet, ECHELLE_AZ_PROP, Double.toString(echelleAz));
        Pio.addParam(factory, paramSet, ENTRANCE_FIBRE_PROP, entranceFibre.name());
        Pio.addParam(factory, paramSet, EXPOSURE_METER_FILTER_PROP, exposureMeterFilter.name());
        Pio.addParam(factory, paramSet, GONI_ANG_PROP, Double.toString(goniAng));
        Pio.addParam(factory, paramSet, HARTMANN_FLAP_PROP, hartmannFlap.name());
        Pio.addParam(factory, paramSet, ISS_PORT_PROP, issPort.name());
        Pio.addParam(factory, paramSet, POST_SLIT_FILTER_PROP, postSlitFilter.name());
        Pio.addParam(factory, paramSet, ROI_PROP, roi.name());
        return paramSet;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
        String v;
        v = Pio.getValue(paramSet, CCD_AMPLIFIERS_PROP);
        if (v != null) {
            setCCDAmplifiers(BHROSParams.CCDReadoutPorts.getCCDAmplifiers(v, ccdAmplifiers));
        }

        v = Pio.getValue(paramSet, CCD_CENTRE_X_PROP);
        if (v != null) {
            setCCDCentreX(getDoubleValue(paramSet, CCD_CENTRE_X_PROP.getName(), ccdCentreX));
        }

        v = Pio.getValue(paramSet, CCD_CENTRE_Y_PROP);
        if (v != null) {
            setCCDCentreY(getDoubleValue(paramSet, CCD_CENTRE_Y_PROP.getName(), ccdCentreY));
        }

        v = Pio.getValue(paramSet, CCD_GAIN_PROP);
        if (v != null) {
            setCCDGain(BHROSParams.CCDGain.getCCDGain(v, ccdGain));
        }

        v = Pio.getValue(paramSet, CCD_SPEED_PROP);
        if (v != null) {
            setCCDSpeed(BHROSParams.CCDSpeed.getCCDSpeed(v, ccdSpeed));
        }

        v = Pio.getValue(paramSet, CCD_XBINNING_PROP);
        if (v != null) {
            setCCDXBinning(BHROSParams.CCDXBinning.getXBinning(v, ccdXBinning));
        }

        v = Pio.getValue(paramSet, CCD_YBINNING_PROP);
        if (v != null) {
            setCCDYBinning(BHROSParams.CCDYBinning.getYBinning(v, ccdYBinning));
        }

        v = Pio.getValue(paramSet, CENTRAL_WAVELENGTH_PROP);
        if (v != null) {
            setCentralWavelength(getDoubleValue(paramSet, CENTRAL_WAVELENGTH_PROP.getName(), centralWavelength));
        }

        v = Pio.getValue(paramSet, ECHELLE_ALT_PROP);
        if (v != null) {
            setEchelleALT(getDoubleValue(paramSet, ECHELLE_ALT_PROP.getName(), echelleAlt));
        }

        v = Pio.getValue(paramSet, ECHELLE_AZ_PROP);
        if (v != null) {
            setEchelleAz(getDoubleValue(paramSet, ECHELLE_AZ_PROP.getName(), echelleAz));
        }

        v = Pio.getValue(paramSet, ENTRANCE_FIBRE_PROP);
        if (v != null) {
            setEntranceFibre(BHROSParams.EntranceFibre.getEntranceFibre(v, entranceFibre));
        }

        v = Pio.getValue(paramSet, EXPOSURE_METER_FILTER_PROP);
        if (v != null) {
            setExposureMeterFilter(BHROSParams.ExposureMeterFilter.getExposureMeterFilter(v, exposureMeterFilter));
        }

        v = Pio.getValue(paramSet, GONI_ANG_PROP);
        if (v != null) {
            setGoniAng(getDoubleValue(paramSet, GONI_ANG_PROP.getName(), goniAng));
        }

        v = Pio.getValue(paramSet, HARTMANN_FLAP_PROP);
        if (v != null) {
            setHartmannFlap(BHROSParams.HartmannFlap.getHartmannFlap(v, hartmannFlap));
        }

        v = Pio.getValue(paramSet, ISS_PORT_PROP);
        if (v != null) {
            setISSPort(BHROSParams.ISSPort.getISSPort(v, issPort));
        }

        v = Pio.getValue(paramSet, POST_SLIT_FILTER_PROP);
        if (v != null) {
            setPostSlitFilter(BHROSParams.PostSlitFilter.getPostSlitFilter(v, postSlitFilter));
        }

        v = Pio.getValue(paramSet, ROI_PROP);
        if (v != null) {
            setROI(BHROSParams.ROI.getROI(v, roi));
        }
    }

    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(DefaultParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(CCD_AMPLIFIERS_PROP, ccdAmplifiers));
        sc.putParameter(DefaultParameter.getInstance(CCD_CENTRE_X_PROP, ccdCentreX));
        sc.putParameter(DefaultParameter.getInstance(CCD_CENTRE_Y_PROP, ccdCentreY));
        sc.putParameter(DefaultParameter.getInstance(CCD_GAIN_PROP, ccdGain));
        sc.putParameter(DefaultParameter.getInstance(CCD_SPEED_PROP, ccdSpeed));
        sc.putParameter(DefaultParameter.getInstance(CCD_XBINNING_PROP, ccdXBinning));
        sc.putParameter(DefaultParameter.getInstance(CCD_YBINNING_PROP, ccdYBinning));
        sc.putParameter(DefaultParameter.getInstance(CENTRAL_WAVELENGTH_PROP, centralWavelength));
        sc.putParameter(DefaultParameter.getInstance(ENTRANCE_FIBRE_PROP, entranceFibre));
        sc.putParameter(DefaultParameter.getInstance(HARTMANN_FLAP_PROP, hartmannFlap));
        sc.putParameter(DefaultParameter.getInstance(ISS_PORT_PROP, issPort));
        sc.putParameter(DefaultParameter.getInstance(POST_SLIT_FILTER_PROP, postSlitFilter));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_METER_FILTER_PROP, exposureMeterFilter));

// Not configurable yet.
//		sc.putParameter(DefaultParameter.getInstance(BHROSConstants.ROI_PROP, roi));

        return sc;
    }

    /** This method is called by the OT Browser to determine how to query the instrument */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(CCD_AMPLIFIERS_PROP));
        configInfo.add(new InstConfigInfo(CCD_GAIN_PROP));
        configInfo.add(new InstConfigInfo(CCD_SPEED_PROP));
        configInfo.add(new InstConfigInfo(CCD_XBINNING_PROP));
        configInfo.add(new InstConfigInfo(CCD_YBINNING_PROP));
        configInfo.add(new InstConfigInfo(ENTRANCE_FIBRE_PROP));
        configInfo.add(new InstConfigInfo(EXPOSURE_METER_FILTER_PROP));
        configInfo.add(new InstConfigInfo(HARTMANN_FLAP_PROP));
        configInfo.add(new InstConfigInfo(ISS_PORT_PROP));
        configInfo.add(new InstConfigInfo(POST_SLIT_FILTER_PROP));
        configInfo.add(new InstConfigInfo(ROI_PROP));

        return configInfo;
    }

    public CCDReadoutPorts getCCDAmplifiers() {
        return ccdAmplifiers;
    }

    public void setCCDAmplifiers(CCDReadoutPorts ccdAmplifiers) {
        Object prev = this.ccdAmplifiers;
        this.ccdAmplifiers = ccdAmplifiers;
        firePropertyChange(CCD_AMPLIFIERS_PROP, prev, this.ccdAmplifiers);
    }

    public double getCCDCentreX() {
        return ccdCentreX;
    }

    /**
     * Sets the CCD target X position and updates the echelle settings.
     * @param ccdCentreX CCD X offset
     */
    public void setCCDCentreX(double ccdCentreX) {
        double prev = this.ccdCentreX;
        this.ccdCentreX = ccdCentreX;
        try {
            updateEchelleSettings();
            firePropertyChange(CCD_CENTRE_X_PROP, prev, this.ccdCentreX);
        } catch (IllegalArgumentException iae) {
            this.ccdCentreX = prev;
            throw iae;
        }
    }

    public double getCCDCentreY() {
        return ccdCentreY;
    }

    public void setCCDCentreY(double ccdCentreY) {
        double prev = this.ccdCentreY;
        this.ccdCentreY = ccdCentreY;
        try {
            updateEchelleSettings();
            firePropertyChange(CCD_CENTRE_Y_PROP, prev, this.ccdCentreY);
        } catch (IllegalArgumentException iae) {
            this.ccdCentreY = prev;
            throw iae;
        }
    }

    public CCDGain getCCDGain() {
        return ccdGain;
    }

    public void setCCDGain(CCDGain ccdGain) {
        Object prev = this.ccdGain;
        this.ccdGain = ccdGain;
        firePropertyChange(CCD_GAIN_PROP, prev, this.ccdGain);
    }

    public CCDSpeed getCCDSpeed() {
        return ccdSpeed;
    }

    public void setCCDSpeed(CCDSpeed ccdSpeed) {
        Object prev = this.ccdSpeed;
        this.ccdSpeed = ccdSpeed;
        firePropertyChange(CCD_SPEED_PROP, prev, this.ccdSpeed);
    }

    public double getCentralWavelength() {
        return centralWavelength;
    }

    /**
     * Sets the central wavelength and updates the echelle settings.
     * @param lambdaNm wavelength in nanometers
     */
    public void setCentralWavelength(double lambdaNm) {
        double prev = this.centralWavelength;
        this.centralWavelength = lambdaNm;
        try {
            updateEchelleSettings();
            firePropertyChange(CENTRAL_WAVELENGTH_PROP, prev, this.centralWavelength);
        } catch (IllegalArgumentException iae) {
            this.centralWavelength = prev;
            throw iae;
        }
    }

    public EntranceFibre getEntranceFibre() {
        return entranceFibre;
    }

    public void setEntranceFibre(EntranceFibre entranceFibre) {
        Object prev = this.entranceFibre;
        this.entranceFibre = entranceFibre;
        firePropertyChange(ENTRANCE_FIBRE_PROP, prev, this.entranceFibre);

        // fibre switch changes the goni angle
        double prevGoni = goniAng;
        updateEchelleSettings();
        firePropertyChange(GONI_ANG_PROP, prevGoni, goniAng);
    }

    public ExposureMeterFilter getExposureMeterFilter() {
        return exposureMeterFilter;
    }

    public void setExposureMeterFilter(ExposureMeterFilter exposureMeterFilter) {
        Object prev = this.exposureMeterFilter;
        this.exposureMeterFilter = exposureMeterFilter;
        firePropertyChange(EXPOSURE_METER_FILTER_PROP, prev, this.exposureMeterFilter);
    }

    public HartmannFlap getHartmannFlap() {
        return hartmannFlap;
    }

    public void setHartmannFlap(HartmannFlap hartmannFlap) {
        Object prev = this.hartmannFlap;
        this.hartmannFlap = hartmannFlap;
        firePropertyChange(HARTMANN_FLAP_PROP, prev, this.hartmannFlap);
    }

    public ISSPort getISSPort() {
        return issPort;
    }

    public void setISSPort(ISSPort issPort) {
        Object prev = this.issPort;
        this.issPort = issPort;
        firePropertyChange(ISS_PORT_PROP, prev, this.issPort);
    }

    public PostSlitFilter getPostSlitFilter() {
        return postSlitFilter;
    }

    public void setPostSlitFilter(PostSlitFilter postSlitFilter) {
        Object prev = this.postSlitFilter;
        this.postSlitFilter = postSlitFilter;
        firePropertyChange(POST_SLIT_FILTER_PROP, prev, this.postSlitFilter);
    }

    public ROI getROI() {
        return roi;
    }

    public void setROI(ROI roi) {
        Object prev = this.roi;
        this.roi = roi;
        firePropertyChange(ROI_PROP, prev, this.roi);
    }

    public CCDXBinning getCCDXBinning() {
        return ccdXBinning;
    }

    public void setCCDXBinning(CCDXBinning binning) {
        Object prev = ccdXBinning;
        ccdXBinning = binning;
        firePropertyChange(CCD_XBINNING_PROP, prev, ccdXBinning);
    }

    public CCDYBinning getCCDYBinning() {
        return ccdYBinning;
    }

    public void setCCDYBinning(CCDYBinning binning) {
        Object prev = ccdYBinning;
        ccdYBinning = binning;
        firePropertyChange(CCD_XBINNING_PROP, prev, ccdYBinning);
    }

    public double getEchelleALT() {
        if (!initializedEchelle)
            updateEchelleSettings();
        return echelleAlt;
    }

    public void setEchelleALT(double echelleAlt) {
        double prev = this.echelleAlt;
        this.echelleAlt = echelleAlt;
        firePropertyChange(ECHELLE_ALT_PROP, prev, this.echelleAlt);
    }

    public double getEchelleAz() {
        if (!initializedEchelle)
            updateEchelleSettings();
        return echelleAz;
    }

    public void setEchelleAz(double echelleAz) {
        double prev = this.echelleAz;
        this.echelleAz = echelleAz;
        firePropertyChange(ECHELLE_AZ_PROP, prev, this.echelleAz);
    }

    public double getGoniAng() {
        if (!initializedEchelle)
            updateEchelleSettings();
        return goniAng;
    }

    public void setGoniAng(double goniAng) {
        double prev = this.goniAng;
        this.goniAng = goniAng;
        firePropertyChange(GONI_ANG_PROP, prev, this.goniAng);
    }

    private double getDoubleValue(PioNodeParent context, String path, double nValue) {
        String val = Pio.getValue(context, path);
        return val != null ? Double.parseDouble(val) : nValue;
    }

    private void updateEchelleSettings() {
        double[] settings = BRayLib.echellePos(centralWavelength, Echellogram.getOrder(centralWavelength), ccdCentreX, ccdCentreY, entranceFibre.getGoniometerOffset());
        echelleAlt = settings[0];
        echelleAz = settings[1];
        goniAng = settings[2];
        initializedEchelle = true;
    }

    // Ultimately this is just an ADT and needs to have equality defined in terms of
    // the equality of all fields. At least for now. This is important for Undo and
    // possibly for other things.
    /*
    public boolean equals(Object obj) {
        if ((obj instanceof InstBHROS) && super.equals(obj)) {
            InstBHROS other = (InstBHROS) obj;
            return
                other.ccdAmplifiers == ccdAmplifiers &&
                other.ccdCentreX == ccdCentreX &&
                other.ccdCentreY == ccdCentreY &&
                other.ccdGain == ccdGain &&
                other.ccdSpeed == ccdSpeed &&
                other.ccdXBinning == ccdXBinning &&
                other.ccdYBinning == ccdYBinning &&
                other.centralWavelength == centralWavelength &&
//				other.echelleAlt == echelleAlt &&
//				other.echelleAz == echelleAz &&
                other.entranceFibre == entranceFibre &&
                other.exposureMeterFilter == exposureMeterFilter &&
//				other.goniAng == goniAng &&
                other.hartmannFlap == hartmannFlap &&
                other.issPort == issPort &&
                other.postSlitFilter == postSlitFilter &&
                other.roi == roi;
        }
        return false;
    }
    */

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}




