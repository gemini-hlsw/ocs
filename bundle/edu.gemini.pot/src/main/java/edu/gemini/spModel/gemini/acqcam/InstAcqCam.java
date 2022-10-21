// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstAcqCam.java 45259 2012-05-14 23:58:29Z fnussber $
//
package edu.gemini.spModel.gemini.acqcam;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.acqcam.AcqCamParams.*;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * The AcqCam instrument.
 */
public final class InstAcqCam extends SPInstObsComp implements PropertyProvider {

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_ACQCAM;

    public static final ISPNodeInitializer<ISPObsComponent, InstAcqCam> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstAcqCam(), c -> new InstAcqCamCB(c));

    // Default values
    public static final int DEF_X = 0;
    public static final int DEF_Y = 0;
    public static final int DEF_WIDTH = 0;
    public static final int DEF_HEIGHT = 0;
    public static final double DEF_EXPOSURE_TIME = 60;

    /**
     * The name of the AcqCam instrument configuration
     */
    public static final String INSTRUMENT_NAME_PROP = "AcqCam";

    public static final PropertyDescriptor COLOR_FILTER_PROP;
    public static final PropertyDescriptor ND_FILTER_PROP;
    public static final PropertyDescriptor BINNING_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor LENS_PROP;
    public static final PropertyDescriptor WINDOWING_PROP;
    public static final PropertyDescriptor X_PROP;
    public static final PropertyDescriptor Y_PROP;
    public static final PropertyDescriptor X_SIZE_PROP;
    public static final PropertyDescriptor Y_SIZE_PROP;
    public static final PropertyDescriptor CASS_ROTATOR_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstAcqCam.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        final boolean query_yes = true;
        final boolean iter_yes  = true;
        final boolean iter_no   = false;

        BINNING_PROP = initProp("binning", query_yes, iter_yes);
        CASS_ROTATOR_PROP = initProp("cassRotator",  query_yes, iter_yes);
        COLOR_FILTER_PROP = initProp("colorFilter", query_yes, iter_yes);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_yes, iter_yes);
        LENS_PROP = initProp("lens", query_yes, iter_yes);
        ND_FILTER_PROP = initProp("ndFilter", query_yes, iter_yes);
        ND_FILTER_PROP.setDisplayName("Neutral Density Filter");
        WINDOWING_PROP = initProp("windowing", query_yes, iter_yes);
        X_PROP = initProp("xStart", query_yes, iter_no);
        X_SIZE_PROP = initProp("xSize", query_yes, iter_no);
        Y_PROP = initProp("yStart", query_yes, iter_no);
        Y_SIZE_PROP = initProp("ySize", query_yes, iter_no);
    }

    private ColorFilter _colorFilter = ColorFilter.DEFAULT;
    private NDFilter _ndFilter = NDFilter.DEFAULT;
    private Binning _binning = Binning.DEFAULT;
    private Windowing _windowing = Windowing.DEFAULT;
    private int _x = DEF_X;
    private int _y = DEF_Y;
    private int _xsize = DEF_WIDTH;
    private int _ysize = DEF_HEIGHT;
    private Lens _lens = Lens.DEFAULT;
    private CassRotator _cassRotator = CassRotator.DEFAULT;

    public InstAcqCam() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = DEF_EXPOSURE_TIME;
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_BOTH;
    }

    public String getPhaseIResourceName() {
        return "gemAcqCam";
    }

    /**
     * Return the science area.
     * @return an array giving the size of the detector in arcsec
     */
    public double[] getScienceArea() {
        return new double[]{120., 120.};
    }


    /**
     * Get the color filter.
     */
    public ColorFilter getColorFilter() {
        return _colorFilter;
    }

    /**
     * Set the color filter.
     */
    public void setColorFilter(ColorFilter newValue) {
        ColorFilter oldValue = getColorFilter();
        if (oldValue != newValue) {
            _colorFilter = newValue;
            firePropertyChange(COLOR_FILTER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the color filter with a String.
     */
    private void _setColorFilter(String name) {
        ColorFilter oldValue = getColorFilter();
        setColorFilter(ColorFilter.getColorFilter(name, oldValue));
    }


    /**
     * Get the neutral density filter.
     */
    public NDFilter getNdFilter() {
        return _ndFilter;
    }

    /**
     * Set the neutral density filter.
     */
    public void setNdFilter(NDFilter newValue) {
        NDFilter oldValue = getNdFilter();
        if (oldValue != newValue) {
            _ndFilter = newValue;
            firePropertyChange(ND_FILTER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the neutral density filter with a String.
     */
    private void _setNdFilter(String name) {
        NDFilter oldValue = getNdFilter();
        setNdFilter(NDFilter.getNDFilter(name, oldValue));
    }


    /**
     * Get the detector binning.
     */
    public Binning getBinning() {
        return _binning;
    }

    /**
     * Set the binning.
     */
    public void setBinning(Binning newValue) {
        Binning oldValue = getBinning();
        if (oldValue != newValue) {
            _binning = newValue;
            firePropertyChange(BINNING_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the binning with a String.
     */
    private void _setBinning(String name) {
        Binning oldValue = getBinning();
        setBinning(Binning.getBinning(name, oldValue));
    }


    /**
     * Get the lens.
     */
    public Lens getLens() {
        if (_lens == null) {
            _lens = Lens.DEFAULT;
        }
        return _lens;
    }

    /**
     * Set the lens.
     */
    public void setLens(Lens newValue) {
        Lens oldValue = getLens();
        if (oldValue != newValue) {
            _lens = newValue;
            firePropertyChange(LENS_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the lens with a String.
     */
    private void _setLens(String name) {
        Lens oldValue = getLens();
        setLens(Lens.getLens(name, oldValue));
    }


    /**
     * Get the windowing setting.
     */
    public Windowing getWindowing() {
        return _windowing;
    }

    /**
     * Set the windowing.
     */
    public void setWindowing(Windowing newValue) {
        Windowing oldValue = getWindowing();
        if (oldValue != newValue) {
            _windowing = newValue;
            firePropertyChange(WINDOWING_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the windowing with a String.
     */
    private void _setWindowing(String name) {
        Windowing oldValue = getWindowing();
        setWindowing(Windowing.getWindowing(name, oldValue));
    }


    /**
     * Set the center X coordinate of the window in pixels.
     */
    public final void setXStart(int newValue) {
        // No bad values allowed.
        if (newValue <= 0) newValue = DEF_X;

        int oldValue = getXStart();
        if (oldValue != newValue) {
            _x = newValue;
            firePropertyChange(X_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the center X coordinate of the window in pixels.
     */
    public final int getXStart() {
        return _x;
    }

    /**
     * Get the center X coordinate of the window as a String.
     */
    public final String getXStartAsString() {
        return Integer.toString(_x);
    }

    /**
     * Set the center Y coordinate of the window in pixels.
     */
    public final void setYStart(int newValue) {
        // No bad values allowed.
        if (newValue <= 0) newValue = DEF_Y;

        int oldValue = getYStart();
        if (oldValue != newValue) {
            _y = newValue;
            firePropertyChange(Y_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the center Y coordinate of the window in pixels.
     */
    public final int getYStart() {
        return _y;
    }

    /**
     * Get the center Y coordinate of the window as a String.
     */
    public final String getYStartAsString() {
        return Integer.toString(_y);
    }


    /**
     * Set the width of window in pixels
     */
    public final void setXSize(int newValue) {
        // No bad values allowed.
        if (newValue <= 0) newValue = DEF_WIDTH;

        int oldValue = getXSize();
        if (oldValue != newValue) {
            _xsize = newValue;
            firePropertyChange(X_SIZE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the width of window in pixels
     */
    public final int getXSize() {
        return _xsize;
    }

    /**
     * Get the width of window as a String.
     */
    public final String getXSizeAsString() {
        return Integer.toString(_xsize);
    }


    /**
     * Set the height of window in pixels
     */
    public final void setYSize(int newValue) {
        // No bad values allowed.
        if (newValue <= 0) newValue = DEF_HEIGHT;

        int oldValue = getYSize();
        if (oldValue != newValue) {
            _ysize = newValue;
            firePropertyChange(Y_SIZE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the height of window in pixels.
     */
    public final int getYSize() {
        return _ysize;
    }

    /**
     * Get the height of window as a String.
     */
    public final String getYSizeAsString() {
        return Integer.toString(_ysize);
    }


    /**
     * Set Cass Rotator to Fixed or Following
     */
    public void setCassRotator(CassRotator newValue) {
        CassRotator oldValue = getCassRotator();
        if (oldValue != newValue) {
            _cassRotator = newValue;
            firePropertyChange(CASS_ROTATOR_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Cass Rotator with a String.
     */
    private void _setCassRotator(String name) {
        CassRotator oldValue = getCassRotator();
        setCassRotator(CassRotator.getCassRotator(name, oldValue));
    }

    /**
     * Get the Cass Rotator.
     */
    public CassRotator getCassRotator() {
        if (_cassRotator == null) {
            _cassRotator = CassRotator.DEFAULT; // backward compat...
        }
        return _cassRotator;
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, COLOR_FILTER_PROP.getName(), getColorFilter().name());
        Pio.addParam(factory, paramSet, ND_FILTER_PROP.getName(), getNdFilter().name());
        Pio.addParam(factory, paramSet, BINNING_PROP.getName(), getBinning().name());
        Pio.addParam(factory, paramSet, WINDOWING_PROP.getName(), getWindowing().name());
        Pio.addParam(factory, paramSet, X_PROP.getName(), getXStartAsString());
        Pio.addParam(factory, paramSet, Y_PROP.getName(), getYStartAsString());
        Pio.addParam(factory, paramSet, X_SIZE_PROP.getName(), getXSizeAsString());
        Pio.addParam(factory, paramSet, Y_SIZE_PROP.getName(), getYSizeAsString());
        Pio.addParam(factory, paramSet, LENS_PROP.getName(), getLens().name());
        Pio.addParam(factory, paramSet, CASS_ROTATOR_PROP.getName(), _cassRotator.name());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, COLOR_FILTER_PROP.getName());
        if (v != null) {
            _setColorFilter(v);
        }
        v = Pio.getValue(paramSet, ND_FILTER_PROP.getName());
        if (v != null) {
            _setNdFilter(v);
        }
        v = Pio.getValue(paramSet, BINNING_PROP.getName());
        if (v != null) {
            _setBinning(v);
        }
        v = Pio.getValue(paramSet, WINDOWING_PROP.getName());
        if (v != null) {
            _setWindowing(v);
        }
        v = Pio.getValue(paramSet, X_PROP.getName());
        if (v != null) {
            setXStart(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, Y_PROP.getName());
        if (v != null) {
            setYStart(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, X_SIZE_PROP.getName());
        if (v != null) {
            setXSize(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, Y_SIZE_PROP.getName());
        if (v != null) {
            setYSize(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, LENS_PROP.getName());
        if (v != null) {
            _setLens(v);
        }
        v = Pio.getValue(paramSet, CASS_ROTATOR_PROP.getName());
        if (v != null) {
            _setCassRotator(v);
        }
    }


    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(COLOR_FILTER_PROP.getName(), getColorFilter()));
        sc.putParameter(DefaultParameter.getInstance(ND_FILTER_PROP.getName(), getNdFilter()));
        sc.putParameter(DefaultParameter.getInstance(BINNING_PROP.getName(), getBinning()));
        sc.putParameter(DefaultParameter.getInstance(WINDOWING_PROP.getName(), getWindowing()));
        sc.putParameter(DefaultParameter.getInstance(LENS_PROP.getName(), getLens()));
        sc.putParameter(DefaultParameter.getInstance(CASS_ROTATOR_PROP.getName(), getCassRotator()));

        sc.putParameter(StringParameter.getInstance(X_PROP.getName(), getXStartAsString()));
        sc.putParameter(StringParameter.getInstance(Y_PROP.getName(), getYStartAsString()));
        sc.putParameter(StringParameter.getInstance(X_SIZE_PROP.getName(), getXSizeAsString()));
        sc.putParameter(StringParameter.getInstance(Y_SIZE_PROP.getName(), getYSizeAsString()));

        sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.POS_ANGLE_PROP, getPosAngleDegrees()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.COADDS_PROP, getCoadds()));

        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(COLOR_FILTER_PROP));
        configInfo.add(new InstConfigInfo(ND_FILTER_PROP));
        configInfo.add(new InstConfigInfo(BINNING_PROP));
        configInfo.add(new InstConfigInfo(WINDOWING_PROP));
        configInfo.add(new InstConfigInfo(LENS_PROP));
        configInfo.add(new InstConfigInfo(CASS_ROTATOR_PROP));

        return configInfo;
    }

    /**  AcqCam doesn't have an OIWFS */
    public boolean hasOIWFS() {
        return false;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(5.8);
    private static final Angle PWFS2_VIG = Angle.arcmins(5.3);

    @Override public Angle pwfs1VignettingClearance(ObsContext ctx) { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance(ObsContext ctx) { return PWFS2_VIG; }
}
