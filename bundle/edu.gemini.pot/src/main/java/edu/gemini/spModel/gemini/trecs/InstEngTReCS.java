// Copyright 2003
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstEngTReCS.java 7452 2006-11-23 13:25:58Z anunez $
//

package edu.gemini.spModel.gemini.trecs;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.trecs.TReCSParams.ApertureWheel;
import edu.gemini.spModel.gemini.trecs.TReCSParams.LyotWheel;
import edu.gemini.spModel.gemini.trecs.TReCSParams.PupilImagingWheel;
import edu.gemini.spModel.gemini.trecs.TReCSParams.SectorWheel;
import edu.gemini.spModel.gemini.trecs.TReCSParams.WellDepth;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

/**
 * The TReCS Engineering Component.
 */
public final class InstEngTReCS extends AbstractDataObject implements PropertyProvider {

    // for serialization
    private static final long serialVersionUID = 3L;

    /**
     * This engineering obs component's SP type.
     */

    public static final String AUTO = "auto"; // auto configure a value

    public static final SPComponentType SP_TYPE =
            SPComponentType.ENG_ENGTRECS;

    public static final ISPNodeInitializer<ISPObsComponent, InstEngTReCS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstEngTReCS(), c -> new InstEngTReCSCB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    public static final PropertyDescriptor SECTOR_WHEEL_PROP;
    public static final PropertyDescriptor LYOT_WHEEL_PROP;
    public static final PropertyDescriptor PUPIL_IMAGING_WHEEL_PROP;
    public static final PropertyDescriptor APERTURE_WHEEL_PROP;
    public static final PropertyDescriptor WELL_DEPTH_PROP;
    public static final PropertyDescriptor NOD_HANDSHAKE_PROP;
    public static final PropertyDescriptor CHOP_FREQUENCY_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;





    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstEngTReCS.class, query, iter);
        pd.setExpert(true);
        PropertySupport.setEngineering(pd, true);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        boolean iter_yes = true;
        boolean query_no = false;
        boolean iter_no = false;
        SECTOR_WHEEL_PROP       = initProp("sectorWheel", query_no, iter_yes);
        LYOT_WHEEL_PROP         = initProp("lyotWheel", query_no, iter_yes);
        PUPIL_IMAGING_WHEEL_PROP = initProp("pupilImagingWheel", query_no, iter_yes);
        APERTURE_WHEEL_PROP     = initProp("apertureWheel", query_no, iter_yes);
        WELL_DEPTH_PROP         = initProp("wellDepth", query_no, iter_yes);
        NOD_HANDSHAKE_PROP      = initProp("nodHandshake", query_no, iter_no);
        CHOP_FREQUENCY_PROP     = initProp("chopFrequency", query_no, iter_no);
        EXPOSURE_TIME_PROP      = initProp("exposureTime", query_no, iter_no);
    }

    // Engineering settings
    private SectorWheel _sectorWheel = SectorWheel.DEFAULT;
    private LyotWheel _lyotWheel = LyotWheel.DEFAULT;
    private PupilImagingWheel _pupilImagingWheel = PupilImagingWheel.DEFAULT;
    private ApertureWheel _apertureWheel = ApertureWheel.DEFAULT;
    private TReCSParams.WellDepth _wellDepth = TReCSParams.WellDepth.DEFAULT;

    private String _exposureTime = AUTO;
    private String _chopFrequency = AUTO;
    private YesNoType _nodHandshake = YesNoType.YES;
    private String _VERSION = "2007A-1";

    /** Constructor */
    public InstEngTReCS() {
        super(SP_TYPE);
        setVersion(_VERSION);
    }

    /**
     * Implementation of the clone method.
     */

    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    /**
     * Get the SectorWheel.
     */
    public SectorWheel getSectorWheel() {
        if (_sectorWheel == null) {
            _sectorWheel = SectorWheel.DEFAULT;
        }
        return _sectorWheel;
    }

    /**
     * Set the SectorWheel.
     */
    public void setSectorWheel(SectorWheel newValue) {
        SectorWheel oldValue = getSectorWheel();
        if (oldValue != newValue) {
            _sectorWheel = newValue;
            firePropertyChange(SECTOR_WHEEL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Sector Wheel with a String.
     */
    private void _setSectorWheel(String name) {
        SectorWheel oldValue = getSectorWheel();
        setSectorWheel(SectorWheel.getSectorWheel(name, oldValue));
    }


    /**
     * Get the Lyot Wheel.
     */
    public LyotWheel getLyotWheel() {
        if (_lyotWheel == null) {
            _lyotWheel = LyotWheel.DEFAULT;
        }
        return _lyotWheel;
    }

    /**
     * Set the Lyot Wheel.
     */
    public void setLyotWheel(LyotWheel newValue) {
        LyotWheel oldValue = getLyotWheel();
        if (oldValue != newValue) {
            _lyotWheel = newValue;
            firePropertyChange(LYOT_WHEEL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Lyot Wheel with a String.
     */
    private void _setLyotWheel(String name) {
        LyotWheel oldValue = getLyotWheel();
        setLyotWheel(LyotWheel.getLyotWheel(name, oldValue));
    }


    /**
     * Get the Pupil Imaging Wheel.
     */
    public PupilImagingWheel getPupilImagingWheel() {
        if (_pupilImagingWheel == null) {
            _pupilImagingWheel = PupilImagingWheel.DEFAULT;
        }
        return _pupilImagingWheel;
    }

    /**
     * Set the Pupil Imaging Wheel.
     */
    public void setPupilImagingWheel(PupilImagingWheel newValue) {
        PupilImagingWheel oldValue = getPupilImagingWheel();
        if (oldValue != newValue) {
            _pupilImagingWheel = newValue;
            firePropertyChange(PUPIL_IMAGING_WHEEL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Pupil Imaging Wheel with a String.
     */
    private void _setPupilImagingWheel(String name) {
        PupilImagingWheel oldValue = getPupilImagingWheel();
        setPupilImagingWheel(PupilImagingWheel.getPupilImagingWheel(name, oldValue));
    }


    /**
     * Get the Aperture Wheel.
     */
    public ApertureWheel getApertureWheel() {
        if (_apertureWheel == null) {
            _apertureWheel = ApertureWheel.DEFAULT;
        }
        return _apertureWheel;
    }

    /**
     * Set the Aperture Wheel.
     */
    public void setApertureWheel(ApertureWheel newValue) {
        ApertureWheel oldValue = getApertureWheel();
        if (oldValue != newValue) {
            _apertureWheel = newValue;
            firePropertyChange(APERTURE_WHEEL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Aperture Wheel with a String.
     */
    private void _setApertureWheel(String name) {
        ApertureWheel oldValue = getApertureWheel();
        setApertureWheel(ApertureWheel.getApertureWheel(name, oldValue));
    }

    /**
     * Get the well depth
     */
    public WellDepth getWellDepth() {
        if (_wellDepth == null) {
            _wellDepth = WellDepth.DEFAULT;
        }
        return _wellDepth;
    }

    /**
     * Set the well depth
     */
    public void setWellDepth(WellDepth newValue) {
        WellDepth oldValue = getWellDepth();
        if (oldValue != newValue) {
            _wellDepth = newValue;
            firePropertyChange(WELL_DEPTH_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the well depth with a String
     */
    private void _setWellDepth(String name) {
        WellDepth oldValue = getWellDepth();
        setWellDepth(WellDepth.getWellDepth(name, oldValue));
    }
    /**
     * Get the Nod Handshake.
     */
    public YesNoType getNodHandshake() {
        return _nodHandshake;
    }

    /**
     * Get the Nod Handshake index.
     */
    public int getNodHandshakeIndex() {
        return _nodHandshake.ordinal();
    }

    /**
     * Set the Nod Handshake.
     */
    public void setNodHandshake(YesNoType newValue) {
        YesNoType oldValue = getNodHandshake();
        if (oldValue != newValue) {
            _nodHandshake = newValue;
            firePropertyChange(NOD_HANDSHAKE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Nod Handshake with a String.
     */
    private void _setNodHandshake(String name) {
        YesNoType oldValue = getNodHandshake();
        setNodHandshake(YesNoType.getYesNoType(name, oldValue));
    }

    /**
     * Get the Chop Frequency.
     */
    public String getChopFrequency() {
        return _chopFrequency;
    }

    /**
     * Set the Chop Frequency.
     */
    public void setChopFrequency(String newValue) {
        if (! _chopFrequency.equals(AUTO)) {
            try {
                Double.parseDouble(_chopFrequency);
            } catch(NumberFormatException e) {
                return;
            }
        }
        String oldValue = getChopFrequency();
        if (!oldValue.equals(newValue)) {
            _chopFrequency = newValue;
            firePropertyChange(CHOP_FREQUENCY_PROP.getName(), oldValue, newValue);
        }
    }


    /**
     * Get the Exposure (Frame) Time.
     */
    public String getExposureTime() {
        return _exposureTime;
    }

    /**
     * Set the Exposure (Frame) Time.
     */
    public void setExposureTime(String newValue) {
        if (! _exposureTime.equals(AUTO)) {
            try {
                Double.parseDouble(_exposureTime);
            } catch(NumberFormatException e) {
                return;
            }
        }
        String oldValue = getExposureTime();
        if (!oldValue.equals(newValue)) {
            _exposureTime = newValue;
            firePropertyChange(EXPOSURE_TIME_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, SECTOR_WHEEL_PROP.getName(), getSectorWheel().name());
        Pio.addParam(factory, paramSet, LYOT_WHEEL_PROP.getName(), getLyotWheel().name());
        Pio.addParam(factory, paramSet, PUPIL_IMAGING_WHEEL_PROP.getName(), getPupilImagingWheel().name());
        Pio.addParam(factory, paramSet, APERTURE_WHEEL_PROP.getName(), getApertureWheel().name());
        Pio.addParam(factory, paramSet, WELL_DEPTH_PROP.getName(), getWellDepth().name());

        Pio.addParam(factory, paramSet, EXPOSURE_TIME_PROP.getName(), _exposureTime);
        Pio.addParam(factory, paramSet, CHOP_FREQUENCY_PROP.getName(), getChopFrequency());
        Pio.addParam(factory, paramSet, NOD_HANDSHAKE_PROP.getName(), getNodHandshake().name());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, SECTOR_WHEEL_PROP.getName());
        if (v != null) {
            _setSectorWheel(v);
        }
        v = Pio.getValue(paramSet, LYOT_WHEEL_PROP.getName());
        if (v != null) {
            _setLyotWheel(v);
        }
        v = Pio.getValue(paramSet, PUPIL_IMAGING_WHEEL_PROP.getName());
        if (v != null) {
            _setPupilImagingWheel(v);
        }
        v = Pio.getValue(paramSet, APERTURE_WHEEL_PROP.getName());
        if (v != null) {
            _setApertureWheel(v);
        }
        v = Pio.getValue(paramSet, WELL_DEPTH_PROP.getName());
        if (v != null) {
            _setWellDepth(v);
        }

        v = Pio.getValue(paramSet, EXPOSURE_TIME_PROP.getName());
        if (v != null) {
            setExposureTime(v);
        }
        v = Pio.getValue(paramSet, CHOP_FREQUENCY_PROP.getName());
        if (v != null) {
            setChopFrequency(v);
        }
        v = Pio.getValue(paramSet, NOD_HANDSHAKE_PROP.getName());
        if (v != null) {
            _setNodHandshake(v);
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(DefaultParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(SECTOR_WHEEL_PROP.getName(), getSectorWheel()));

        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP.getName(), getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(CHOP_FREQUENCY_PROP.getName(), getChopFrequency()));
        sc.putParameter(DefaultParameter.getInstance(NOD_HANDSHAKE_PROP.getName(), getNodHandshake()));

        sc.putParameter(DefaultParameter.getInstance(LYOT_WHEEL_PROP.getName(), getLyotWheel()));
        sc.putParameter(DefaultParameter.getInstance(PUPIL_IMAGING_WHEEL_PROP.getName(), getPupilImagingWheel()));
        sc.putParameter(DefaultParameter.getInstance(APERTURE_WHEEL_PROP.getName(), getApertureWheel()));
        sc.putParameter(DefaultParameter.getInstance(WELL_DEPTH_PROP.getName(), getWellDepth()));

        return sc;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
