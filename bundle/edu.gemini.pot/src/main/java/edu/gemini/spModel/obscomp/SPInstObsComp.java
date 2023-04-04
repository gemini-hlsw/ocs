package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.env.AsterismType;
import edu.gemini.spModel.util.Angle;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.time.Duration;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A base class for instrument observation component items.  One of
 * the principal tasks of this component is to keep up-to-date the
 * position angle element of the observation data for the observation
 * context.
 */
public abstract class SPInstObsComp extends AbstractDataObject {

    private static final long serialVersionUID = 2L;

    // The exposure time
    protected double _exposureTime = InstConstants.DEF_EXPOSURE_TIME;

    // The position angle -- value is in DEGREES
    protected double _positionAngle = InstConstants.DEF_POS_ANGLE;

    // The number of coadds
    protected int _coadds = InstConstants.DEF_COADDS;

    // Used to format values as strings.
    private static NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static {
        nf.setMaximumFractionDigits(2);
    }


    /**
     * Construct the SPInstObsComp with its exact subtype.
     */
    public SPInstObsComp(SPComponentType spType) {
        super(spType);
    }

    /**
     * Override clone to clone all the capabilities.
     */
    public Object clone() {
        return super.clone();
    }

    /**
     * Get the site in which the instrument resides.
     *
     * @return the site of the instrument.
     */
    public abstract Set<Site> getSite();

    /**
     * Gets the name of the Phase I resource representing this instrument.
     * This can be used in the Phase I to Phase II conversion process to
     * associate the instrument component with the phase I resource name.
     */
    public abstract String getPhaseIResourceName();

    /**
     * Return the setup time before observing can begin
     * (value may depend on the selected disperser, etc.).
     * Should normally be redefined in a derived class.
     */
    public Duration getSetupTime(ISPObservation obs) {
        return Duration.ofSeconds(20);
    }

    /**
     * Return the reacquisition time in seconds before observing can begin
     * (value may depend on the selected disperser, etc.).
     * Should normally be redefined in a derived class.
     */
    // REL-1346
    public Duration getReacquisitionTime(ISPObservation obs) {
        return Duration.ZERO;
    }

    /**
     * Get the science area in arcsec x arcsec.  For now, this must
     * be a rectangular region.
     */
    public double[] getScienceArea() {
        return new double[]{-1.0, -1.0};
    }

    /**
     * Set the exposure time.
     */
    public void setExposureTime(double newValue) {
        double oldValue = getExposureTime();
        if (oldValue != newValue) {
            _exposureTime = newValue;
            firePropertyChange(InstConstants.EXPOSURE_TIME_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the exposure time from the given String.
     */
    public void setExposureTimeAsString(String newValue) {
        setExposureTime(Double.parseDouble(newValue));
    }

    /**
     * Get the exposure time.
     */
    public double getExposureTime() {
        return _exposureTime;
    }

    /**
     * Get the exposure time as a string.
     */
    public String getExposureTimeAsString() {
        return Double.toString(_exposureTime);
    }

    /**
     * Return the total integration time.  Coadds * exposureTime.
     */
    public double getTotalExposureTime() {
        return getCoadds() * getExposureTime();
    }

    /**
     * Get the total exposure time as a string.
     */
    public String getTotalExposureTimeAsString() {
        return Double.toString(getTotalExposureTime());
    }

    //
    // Lame hack for SCT-328.  The sequence model is so terrible that at any
    // given step, we don't know for certain the value of all parameters.  Only
    // the parameters that are changing at that step.  For that reason, someone
    // hacked a "cached" config that stores needed context for computing the
    // elapsed time of an observation.  Unfortunately the instrument config
    // information isn't always cached -- it depends on the sequence definition.
    // This hack (on top of all the hacks) gives GMOS or whatever instrument I
    // suppose, a chance to store stuff in that cache regardless of the
    // sequence.
    //
    // What we need to do is tear down all the existing sequence model and
    // replace it with the config2 sequence stuff.  Then we wouldn't be fighting
    // to get the information we need it when we needed it.
    //
    public void updateConfig(IConfig config) {
        // do nothing -- see InstGMOSCommon for an example of where it is used
    }


    /**
     * Set the number of coadds.
     */
    public void setCoadds(int newValue) {
        // No bad values allowed.
        if (newValue <= 0)
            newValue = InstConstants.DEF_COADDS;

        int oldValue = getCoadds();
        if (oldValue != newValue) {
            _coadds = newValue;
            firePropertyChange(InstConstants.COADDS_PROP, oldValue, newValue);
        }
    }

    /**
     * Get the number of coadds.
     */
    public int getCoadds() {
        return _coadds;
    }

    /**
     * Get the number of coadds as a String.
     */
    public String getCoaddsAsString() {
        return Integer.toString(_coadds);
    }

    /**
     * Set the position angle in degrees from due north, updating the
     * observation data with the new position angle.  This method is
     * ultimately called by the other setPosAngle methods.
     */
    public void setPosAngleDegrees(double newValue) {
        double oldValue = getPosAngleDegrees();
        if (oldValue != newValue) {
            newValue = Angle.normalizeDegrees(newValue);
            _positionAngle = newValue;
            firePropertyChange(InstConstants.POS_ANGLE_PROP, oldValue, newValue);
        }
    }

    public void setPosAngle(double newValue) {
        setPosAngleDegrees(newValue);
    }

    /**
     * Set the position angle in radians from due north.
     */
    public void setPosAngleRadians(double posAngle) {
        setPosAngleDegrees(Angle.radiansToDegrees(posAngle));
    }

    /**
     * Add the given angle to the current rotation angle
     * and round the result to the nearest degree.
     */
    public void addPosAngleDegrees(double addAngle) {
        double angle = getPosAngleDegrees();
        angle += addAngle;
        setPosAngleDegrees(Math.round(angle));
    }

    /**
     * Add the given angle in radians to the current rotation angle.
     * and round the result to the nearest degree.
     */
    public void addPosAngleRadians(double addAngle) {
        addPosAngleDegrees(Angle.radiansToDegrees(addAngle));
    }

    public double getPosAngle() {
        return getPosAngleDegrees();
    }

    /**
     * Get the rotation of the science area (in radians) from due north.
     */
    public double getPosAngleDegrees() {
        return _positionAngle;
    }

    /**
     * Get the rotation of the science area (in radians) from due north.
     */
    public double getPosAngleRadians() {
        return Angle.degreesToRadians(getPosAngleDegrees());
    }

    /**
     * Get the rotation of the science area as a string (in radians) from
     * due north.
     */
    public String getPosAngleRadiansStr() {
        return nf.format(getPosAngleRadians());
    }

    /**
     * Get the rotation of the science area as a string (in degrees) from
     * due north.
     */
    public String getPosAngleDegreesStr() {
        return nf.format(getPosAngleDegrees());
    }

    /**
     * Returns true if the instrument is in chopping mode.
     */
    public boolean isChopping() {
        return false;
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, InstConstants.EXPOSURE_TIME_PROP, getExposureTimeAsString());
        Pio.addParam(factory, paramSet, InstConstants.POS_ANGLE_PROP, getPosAngleDegreesStr());
        Pio.addParam(factory, paramSet, InstConstants.COADDS_PROP, getCoaddsAsString());

        return paramSet;
    }

    /**
     * The OT "browser" works by extracting a ParamSet representation of the
     * instrument component and comparing against chosen values in the
     * InstConfigInfo list (see getInstConfigInfo in the various instruments).
     * By default, the ParamSet is created by calling getParamSet.  Sometimes
     * however, additional context (in the form of the containing observation)
     * is needed to match query parameters. This method provides an opportunity
     * for an instrument implementation to obtain that context and create the
     * necessary matching ParamSet.
     */
    public ParamSet getBrowserMatchingParamSet(
        PioFactory     factory,
        ISPObservation observation
    ) {
        return getParamSet(factory);
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, InstConstants.EXPOSURE_TIME_PROP);
        if (v != null) {
            setExposureTimeAsString(v);
        }
        v = Pio.getValue(paramSet, InstConstants.POS_ANGLE_PROP);
        if (v != null) {
            setPosAngleDegrees(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, InstConstants.COADDS_PROP);
        if (v != null) {
            setCoadds(Integer.parseInt(v));
        }
    }

    /** Return true if this instrument has an OIWFS */
    public boolean hasOIWFS() {
        return true;  //  redefine in a derived class if not
    }

    /**
     * Return true if it is allowed to add an offset iterator to this instrument in the current mode,
     * given the current list of sequence component types.
     * @param set set of all sequence component types in the sequence
     */
    public boolean offsetsAllowed(SPComponentType type, Set<SPComponentType> set) {
        return true;  //  redefine in a derived class if not
    }

    /**
     * Returns true if the instrument supports adding and removing guide probe targets.
     */
    public boolean hasGuideProbes() {
        return true;  //  redefine in a derived class if not
    }

    // ========== some helper methods for smart calibrations

    /** Helper for calibration key creation */
    protected Object get(ISysConfig instrumentConfig, PropertyDescriptor propertyDescriptor) {
        return get(instrumentConfig, propertyDescriptor.getName());
    }
    /** Helper for calibration key creation */
    protected Object get(ISysConfig instrumentConfig, String name) {
        return instrumentConfig.getParameter(name).getValue();
    }

    /** Helper for calibration key creation */
    protected Double getWavelength(ISysConfig instrumentConfig) {
        Double wavelength = 0.;
        // central wavelength will only be set for spectroscopy
        IParameter wavelengthParamater = instrumentConfig.getParameter(InstConstants.OBSERVING_WAVELENGTH_PROP);
        if (wavelengthParamater != null) {
            wavelength = Double.parseDouble(wavelengthParamater.getValue().toString());
        }
        return wavelength;
    }

    public edu.gemini.skycalc.Angle pwfs1VignettingClearance(ObsContext ctx) {
        return edu.gemini.skycalc.Angle.ANGLE_0DEGREES;
    }

    public edu.gemini.skycalc.Angle pwfs2VignettingClearance(ObsContext ctx) {
        return edu.gemini.skycalc.Angle.ANGLE_0DEGREES;
    }

    /**
     * Restore data items when a template reapply happens
     * Implementations can provide an instrument-specific restore capabilities
     * @param oldData Reference to the previous version of the instrument
     */
    public void restoreScienceDetails(final SPInstObsComp oldData) {
        setPosAngle(oldData.getPosAngle());
    }

    /**
     * Get the Instrument associated with this instance.
     * Will throw an exception if the component type is not an instrument, but this should not happen and would
     * indicate something serious is wrong with the observation.
     */
    public Instrument getInstrument() {
        return Instrument.fromComponentType(getType()).getValue();
    }

    public AsterismType getPreferredAsterismType() {
        return AsterismType.Single;
    }

    // REL-1678: 7 seconds DHS write overhead
    // REL-1934: 10 seconds DHS write overhead for F2

    public static final Map<Instrument, PlannedTime.CategorizedTime> DHS_WRITE_TIMES;

    static {
        Map<Instrument, Duration> m =
            Arrays.stream(Instrument.values()).collect(Collectors.toMap(Function.identity(), i -> Duration.ofSeconds(7)));

        m.put(Instrument.Flamingos2, Duration.ofSeconds( 10));
        m.put(Instrument.Ghost,      Duration.ofSeconds(  5));
        m.put(Instrument.GmosNorth,  Duration.ofSeconds( 10));
        m.put(Instrument.GmosSouth,  Duration.ofSeconds( 10));
        m.put(Instrument.Gnirs,      Duration.ofMillis(   8500));
        m.put(Instrument.Igrins2,    Duration.ofSeconds( 10));
        m.put(Instrument.Nifs,       Duration.ofMillis(   9800));
        m.put(Instrument.Niri,       Duration.ofMillis(   5070));

        DHS_WRITE_TIMES = Collections.unmodifiableMap(
          m.entrySet()
           .stream()
           .collect(Collectors.toMap(Map.Entry::getKey, e -> PlannedTime.CategorizedTime.apply(PlannedTime.Category.DHS_WRITE, e.getValue().toMillis())))
        );
    }

    // REL-3955: Initialization issues prompted a switch to add and use
    // 'getDhsWriteTime()` below instead of referring to a public field
    // DHS_WRITE_TIME. The class has a `serialVersionUID` and not-updated
    // clients will be expecting a DHS_WRITE_TIME field. Adding a `readObject`
    // to initialize the value for not-updated clients.
    private PlannedTime.CategorizedTime DHS_WRITE_TIME;

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        DHS_WRITE_TIME = getDhsWriteTime();
    }

    public final PlannedTime.CategorizedTime getDhsWriteTime() {
        return DHS_WRITE_TIMES.get(getInstrument());
    }

}
