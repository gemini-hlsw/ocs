package edu.gemini.qpt.shared.sp;

import edu.gemini.ags.api.AgsAnalysis;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.data.PreImagingType;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.nici.NICIParams;
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ElevationConstraintType;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;
import edu.gemini.spModel.gemini.texes.TexesParams;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation.Priority;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import jsky.coords.WorldCoords;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mini-model representation of an observation.
 */
@SuppressWarnings("unchecked")
public final class Obs implements Serializable, Comparable<Obs> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Obs.class.getName());

    private static class HeterogeneousEnumComparator implements Comparator<Enum<?>>, Serializable {

        private static final Map<Class<?>, Integer> ORDER = new HashMap<>();

        static {
            // This defines the order in which properties appear in the options strings.
            Class<?>[] classes = {

                    // GMOS North
                    GmosNorthType.FPUnitNorth.class,
                    GmosNorthType.DisperserNorth.class,
                    GmosNorthType.FilterNorth.class,
                    GmosOiwfsGuideProbe.class,

                    // GMOS South
                    GmosSouthType.FPUnitSouth.class,
                    GmosSouthType.DisperserSouth.class,
                    GmosSouthType.FilterSouth.class,
                    GmosOiwfsGuideProbe.class,

                    // GMOS-N/S
                    GmosCommonType.DetectorManufacturer.class,
                    GmosCommonType.UseNS.class,

                    // Flamingos2
                    Flamingos2.FPUnit.class,
                    Flamingos2.Disperser.class,
                    Flamingos2.Filter.class,
                    Flamingos2OiwfsGuideProbe.class,

                    // GSAOI
                    Gsaoi.Filter.class,

                    // NIRI
                    Niri.Mask.class,
                    Niri.Disperser.class,
                    Niri.Filter.class,
                    Niri.Camera.class,
                    NiriOiwfsGuideProbe.class,

                    // GNIRS
                    GNIRSParams.SlitWidth.class,
                    GNIRSParams.Disperser.class,
                    GNIRSParams.Filter.class,
                    GNIRSParams.CrossDispersed.class,
                    GNIRSParams.Camera.class,
                    GnirsOiwfsGuideProbe.class,

                    // NICI
                    NICIParams.FocalPlaneMask.class,
                    NICIParams.DichroicWheel.class,
                    NICIParams.Channel1FW.class,
                    NICIParams.Channel2FW.class,
                    NiciOiwfsGuideProbe.class,

                    // TReCS
                    TReCSParams.Disperser.class,
                    TReCSParams.Mask.class,

                    // Altair stuff last?
                    GuideStarType.class,

                    // REL-293: WFS
                    AltairAowfsGuider.class,
                    Canopus.Wfs.class,
                    GsaoiOdgw.class,
                    PwfsGuideProbe.class,

                    // NIFS
                    NIFSParams.Disperser.class,
                    NIFSParams.Filter.class,
                    NIFSParams.Mask.class,

                    // GPI
                    Gpi.Disperser.class,
                    Gpi.Filter.class,

                    // Texes
                    TexesParams.Disperser.class,

                    // PreImaging (used by GMOS and F2)
                    PreImagingType.class

            };

            for (int i = 0; i < classes.length; i++)
                ORDER.put(classes[i], i);

        }

        private static Integer classWeight(final Enum<?> c) {
            final Integer ret = ORDER.get(c.getClass());
            if (ret != null) {
                // ok, we have a specific order for this class
                return ret;
            } else {
                // if not found, try an additional lookup with the declaring class (this covers specialised enums that
                // have their own implementation class. E.g. Niri.Filter.NBF_H20.getClass() is Niri$Filter$1;
                // getDeclaringClass() is Niri$Filter (as expected)
                return ORDER.get(c.getDeclaringClass());
            }
        }

        private static final long serialVersionUID = 1L;
        @Override
        public int compare(final Enum<?> o1, final Enum<?> o2) {
            final Integer w1 = classWeight(o1);
            final Integer w2 = classWeight(o2);
            if (w1 != null && w2 != null && !w1.equals(w2)) {
                // for different enums with a defined weight use their class weight for sorting
                return w1 - w2;
            } else if (w1 == null && w2 != null) {
                // put enums without weight last
                return 1;
            } else if (w1 != null && w2 == null) {
                // put enums without weight last
                return -1;
            } else  {
                // no weight defined for either enum or the weights are equal: order them by class and ordinal
                final String s1 = o1.getClass().getName() + o1.ordinal();
                final String s2 = o2.getClass().getName() + o2.ordinal();
                return s1.compareTo(s2);
            }
            // phew..
        }

    }

    private final Prog prog;
    private final TargetEnvironment targetEnvironment;
    private final int obsNumber;
    private final String obsId;
    private final long piPlannedTime;
    private final long execPlannedTime;
    private final long elapsedTime;
    private final long remainingTime;
    private final String title;
    private final ObservationStatus obsStatus;
    private final ObsClass obsClass;
    private final Set<String> wavefrontSensors;
    private final Conds conditions;
    private final PlannedStepSummary steps;
    private final SPComponentType[] instrument;
    private final Set<Enum<?>> options = new TreeSet<>(new HeterogeneousEnumComparator());
    private final Group group;
    private final boolean inProgress;
    private final int firstStep;
    private final Priority priority;
    private final TooType tooPriority;
    private final String customMask;
    private final Double centralWavelength;
    private final SPSiteQuality siteQuality;
    private final boolean lgs;
    private final boolean ao;
    private final boolean meanParallacticAngle;
    private final ImList<AgsAnalysis> agsAnalysis;
    private final Option<SchedulingBlock> schedulingBlock;

    // Created/computed lazily
    private transient WorldCoords coords;

    public boolean isInProgress() {
        return inProgress;
    }

    public PlannedStepSummary getSteps() {
        return steps;
    }

    public Set<Enum<?>> getOptions() {
        return Collections.unmodifiableSet(options);
    }

    public String getOptionsString() {
        StringBuilder buf = new StringBuilder();
        for (Object o: options) {
            if (o instanceof GuideProbe) continue; // don't want to display OIWFS here
            if (buf.length() != 0)
                buf.append(", ");
            if (customMask != null && o instanceof Enum && Inst.isCustomMask((Enum) o)) {
                buf.append(customMask);
            } else if (o instanceof LoggableSpType) {
                buf.append(((LoggableSpType) o).logValue());
            } else if (o instanceof DisplayableSpType) {
                buf.append(((DisplayableSpType) o).displayValue());
            } else {
                buf.append(o);
            }
        }
        if(centralWavelength!=null){
            buf.append(", \u03BB=").append(centralWavelength);
        }
        return buf.toString();
    }

    public Set<Object> getFilters() {
        Set<Object> filters = new HashSet<>();
        for (Object o: options) {
            if (isFilter(o)) filters.add(o);
        }
        return Collections.unmodifiableSet(filters);
    }

    private Boolean isFilter(Object o) {
        // TODO: use a marker interface on all filter types instead
        if (o instanceof Flamingos2.Filter) return true;
        if (o instanceof GmosNorthType.FilterNorth) return true;
        if (o instanceof GmosSouthType.FilterSouth) return true;
        if (o instanceof GNIRSParams.Filter) return true;
        if (o instanceof Gpi.Filter) return true;
        if (o instanceof Gsaoi.Filter) return true;
        if (o instanceof NIFSParams.Filter) return true;
        if (o instanceof NICIParams.DichroicWheel) return true;
        if (o instanceof NICIParams.Channel1FW) return true;
        if (o instanceof NICIParams.Channel2FW) return true;
        if (o instanceof Niri.Filter) return true;
        if (o instanceof TReCSParams.Filter) return true;
        return false;
    }

    public Set<Object> getDispersers() {
        Set<Object> dispersers = new HashSet<>();
        for (Object o: options) {
            if (isDisperser(o)) dispersers.add(o);
        }
        return Collections.unmodifiableSet(dispersers);
    }

    private Boolean isDisperser(Object o) {
        // TODO: use a marker interface on all dispersers instead
        if (o instanceof Flamingos2.Disperser) return true;
        if (o instanceof GmosNorthType.DisperserNorth) return true;
        if (o instanceof GmosSouthType.DisperserSouth) return true;
        if (o instanceof GNIRSParams.Disperser) return true;
        if (o instanceof Gpi.Disperser) return true;
        if (o instanceof NIFSParams.Disperser) return true;
        if (o instanceof Niri.Disperser) return true;
        if (o instanceof TReCSParams.Disperser) return true;
        if (o instanceof TexesParams.Disperser) return true;
        return false;
    }

    public Set<Object> getFocalPlanUnits() {
        Set<Object> fpus = new HashSet<>();
        for (Object o: options) {
            if (isFocalPlanUnit(o)) {
                fpus.add((customMask!=null && !customMask.isEmpty()) ? customMask : o);
            }
        }
        return Collections.unmodifiableSet(fpus);
    }

    private Boolean isFocalPlanUnit(Object o) {
        // TODO: use a marker interface on all FPUs instead
        if (o instanceof Flamingos2.FPUnit) return true;
        if (o instanceof GmosNorthType.FPUnitNorth) return true;
        if (o instanceof GmosSouthType.FPUnitSouth) return true;
        if (o instanceof GNIRSParams.SlitWidth) return true;
        if (o instanceof NICIParams.FocalPlaneMask) return true;
        if (o instanceof Niri.Mask) return true;
        if (o instanceof NIFSParams.Mask) return true;
        return false;
    }

    public Set<Object> getCamera() {
        Set<Object> cameras = new HashSet<>();
        for (Object o: options) {
            if (isCamera(o)) cameras.add(o);
        }
        return Collections.unmodifiableSet(cameras);
    }

    private Boolean isCamera(Object o) {
        // TODO: use a marker interface on all Cameras instead
        if (o instanceof Niri.Camera) return true;
        if (o instanceof GNIRSParams.Camera) return true;
        return false;
    }

    /// === get some instrument specific settings
    // TODO: find a more generic way to do this, or - alternatively - do it in the QV ObservationTable code
    public Option<Gpi.ObservingMode> getGpiObservingMode() {
        for (Object o : options) {
            if (o instanceof Gpi.ObservingMode) return new Some<>((Gpi.ObservingMode)o);
        }
        return None.instance();
    }
    public Option<GNIRSParams.CrossDispersed> getGnirsCrossDispersed() {
        for (Object o : options) {
            if (o instanceof GNIRSParams.CrossDispersed) return new Some<>((GNIRSParams.CrossDispersed)o);
        }
        return None.instance();
    }
    public Option<Boolean> getGmosNodShuffle() {
        for (Object o : options) {
            if (o instanceof GmosCommonType.UseNS) return new Some<>(o == GmosCommonType.UseNS.TRUE);
        }
        return None.instance();
    }
    public Option<GmosCommonType.DetectorManufacturer> getGmosCcdManufacturer() {
        for (Object o : options) {
            if (o instanceof GmosCommonType.DetectorManufacturer) return new Some<>((GmosCommonType.DetectorManufacturer)o);
        }
        return None.instance();
    }
    public Option<Boolean> getPreImaging() {
        for (Object o : options) {
            if (o instanceof PreImagingType) return new Some<>(o == PreImagingType.TRUE);
        }
        return None.instance();
    }
    /// =====



    public SPComponentType getInstrumentComponentType() {
        // throw exception when there is no instrument..
        if (instrument[0].broadType == SPComponentBroadType.INSTRUMENT) {
            return instrument[0];
        } else if (instrument[1].broadType == SPComponentBroadType.INSTRUMENT) {
            return instrument[1];
        }
        throw new IllegalArgumentException("observation has no instrument");
    }

    public Inst[] getInstruments() {
        try {
            switch (instrument.length) {
            case 0: return new Inst[0];
            case 1: return new Inst[] { Inst.forSpType(instrument[0]) };
            case 2: return new Inst[] { Inst.forSpType(instrument[0]), Inst.forSpType(instrument[1]) };
            default:
                throw new Error("Impossible.");
            }
        } catch (NoSuchElementException nsee) {
            LOGGER.log(Level.WARNING, "Trouble converting SPComponentType to Inst", nsee);
            return new Inst[0];
        }
    }

    public String getTargetName() {
        return (targetEnvironment != null ? targetEnvironment.getAsterism().name() : "");
    }

    public String getWavefrontSensors() {
        String ret = wavefrontSensors.toString();
        return ret.substring(1, ret.length() - 1);
    }

    public Obs(
            Prog prog,
            Group group,
            int obsNumber,
            String obsId,
            String title,
            Priority priority,
            TooType tooPriority,
            ObservationStatus obsStatus,
            ObsClass obsClass,
            TargetEnvironment targetEnvironment,
            SPComponentType[] instrument,
            Set<Enum<?>> options,
            String customMask,
            Double centralWavelength,
            PlannedStepSummary steps,
            long piPlannedTime,
            long execPlannedTime,
            long elapsedTime,
            SPSiteQuality quality,
            boolean lgs,
            boolean ao,
            boolean meanParallacticAngle,
            ImList<AgsAnalysis> agsAnalysis,
            Option<SchedulingBlock> schedulingBlock)
    {

        this.prog = prog;
        this.obsNumber = obsNumber;
        this.obsId = obsId;
        this.title = title;
        this.steps = steps;
        this.group = group;
        this.obsClass = obsClass;
        this.obsStatus = obsStatus;
        this.inProgress = obsStatus.equals(ObservationStatus.ONGOING);
        this.customMask = customMask;
        this.centralWavelength = centralWavelength;
        this.siteQuality = quality;
        this.lgs = lgs;
        this.ao = ao;
        this.meanParallacticAngle = meanParallacticAngle;
        this.priority = priority;
        this.tooPriority = tooPriority;
        this.schedulingBlock = schedulingBlock;

        // if some steps have been executed the setup time needs to be added, same if there are still steps left
        this.piPlannedTime = piPlannedTime;
        this.execPlannedTime = execPlannedTime;
        this.elapsedTime = elapsedTime;

        // Calculate remaining time.
        // Note: The remaining time is not necessarily the difference between planned and elapsed time
        // because the elapsed time can be longer than planned time because of unexpected problems etc.
        // The remaining time is the time of all steps that still await execution.
        long acc = steps.getSetupTime();
        for (int i = 0; i < steps.size(); i++)
            if (!steps.isStepExecuted(i)) acc += steps.getStepTime(i);
       this.remainingTime = acc;


        Set<String> wfs = new TreeSet<>();

        this.targetEnvironment = targetEnvironment;
        if (this.targetEnvironment != null) {
            for (GuideProbe probe : targetEnvironment.getPrimaryGuideGroup().getReferencedGuiders()) {
                String key = probe.getKey();
                if (key.contains("OIWFS")) key = "OIWFS"; // trim off instrument
                wfs.add(key);
            }
        }


        this.wavefrontSensors = wfs;
        this.instrument = instrument;
        this.options.addAll(options);

        int firstStep = 0;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.isStepExecuted(i)) {
                if (firstStep != i)
                    LOGGER.warning(this + ": skipped step: " + i);
                firstStep = i+1;
            }
        }
        this.firstStep = firstStep;

        // -- derive conditions (QPT specific Conds object) from SPSiteQuality
        // TODO: conds could potentially be replaced with SPSiteQuality.Conditions
        final Conds conds;
        if (quality != null) {
            conds = new Conds(quality);
        } else {
            // Via Andy Stephens:
            // If there are no observing constraints they should be listed as everything=Any.
            // This happens to various standards which just need to be observed with the science target.
            // Usually they will be selected via the program view not the candidate observation view.
            conds = Conds.ANY;
        }
        this.conditions = conds;

        // Store the analyses.
        this.agsAnalysis = agsAnalysis;
    }

    public Obs(Prog prog, SPObservationID id) {
        this.prog = prog;
        this.targetEnvironment = null;
        this.obsNumber = id.getObservationNumber();
        this.obsId = createObsId(prog, obsNumber);
        this.title = null;
        this.steps = null;
        this.group = null;
        this.obsClass = null;
        this.obsStatus = null;
        this.inProgress = false;
        this.customMask = null;
        this.centralWavelength = null;
        this.siteQuality = null;
        this.piPlannedTime = 0;
        this.execPlannedTime = 0;
        this.elapsedTime = 0;
        this.remainingTime = 0;
        this.instrument = null;
        this.wavefrontSensors = null;
        this.firstStep = 0;
        this.priority = null;
        this.tooPriority = null;
        this.conditions = null;
        this.lgs = false;
        this.ao = false;
        this.meanParallacticAngle = false;
        this.agsAnalysis = null;
        this.schedulingBlock = None.instance();
    }

    public int compareTo(Obs o) {
        int ret = prog.compareTo(o.prog);
        return (ret != 0) ? ret : (obsNumber - o.obsNumber);
    }

    public String getObsId() {
        return obsId;
    }

    @Override
    public String toString() {
        return getObsId();
    }

    // checks if an observation is valid, i.e. has an instrument, a target and conditions
    Boolean isValid() {
        if (instrument == null) return false;
        if (instrument.length == 0) return false;
        if (targetEnvironment == null) return false;
        if (siteQuality == null) return false;
        return true;
    }

    public int getObsNumber() {
        return obsNumber;
    }

	public double getRa() {
        return (targetEnvironment != null ? targetEnvironment.getAsterism().getRaDegrees(schedulingBlock.map(SchedulingBlock::start)).getOrElse(0.0) : 0.0);
	}

	public double getDec() {
        return (targetEnvironment != null ? targetEnvironment.getAsterism().getDecDegrees(schedulingBlock.map(SchedulingBlock::start)).getOrElse(0.0) : 0.0);
	}

    public Conds getConditions() {
        return conditions;
    }

    public SPSiteQuality getSiteQuality() {
        return siteQuality;
    }

    public SPSiteQuality.ImageQuality getImageQuality() {
        return siteQuality.getImageQuality();               // access this only if obs is valid (siteQuality != null)
    }

    public SPSiteQuality.CloudCover getCloudCover() {
        return siteQuality.getCloudCover();                 // access this only if obs is valid (siteQuality != null)
    }

    public SPSiteQuality.WaterVapor getWaterVapor() {
        return siteQuality.getWaterVapor();                 // access this only if obs is valid (siteQuality != null)
    }

    public SPSiteQuality.SkyBackground getSkyBackground() {
        return siteQuality.getSkyBackground();             // access this only if obs is valid (siteQuality != null)
    }

    public Boolean getLGS() {
        return lgs;
    }

    public Boolean getNGS() {
        return getAO() && !getLGS();                        // AO means it must either be NGS or LGS; therefore, if AO is true, NGS is true if LGS is not true
    }

    /**
     * For ToOs and occasionally for other (yet unknown) targets the coordinates are set to 0/0.
     * Often we need to treat those values separately, e.g. when running statistics, we don't want those values
     * to contribute to the counts in an RA [0..1) bin for example.
     */
    public boolean hasDummyTarget() { return getRa() == 0.0 && getDec() == 0.0; }

    public Boolean getAO() {
        return ao;
    }

    public Boolean usesMeanParallacticAngle() {
        return meanParallacticAngle;
    }

    public final ImList<AgsAnalysis> getAgsAnalysis() { return agsAnalysis; }

    public TargetEnvironment getTargetEnvironment() {
        return targetEnvironment;
    }

    public long getExecPlannedTime() {
        return execPlannedTime;
    }

    public long getPiPlannedTime() {
        return piPlannedTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public ObsClass getObsClass() {
        return obsClass;
    }

    public ObservationStatus getObsStatus() {
        return obsStatus;
    }

    public String getTitle() {
        return title;
    }

    public Prog getProg() {
        return prog;
    }

    public WorldCoords getCoords() {
        if (coords == null) coords = new WorldCoords(getRa(), getDec());
        return coords;
    }

    public String getInstrumentString() {
        StringBuilder buf = new StringBuilder();
        for (SPComponentType t: instrument) {
            if (InstAltair.SP_TYPE.equals(t)) {
                buf.append("+AO");
            } else {
                if (buf.length() != 0) buf.append(" + ");
                buf.append(t.readableStr);
            }
        }
        return buf.toString();
    }

    public String getInstrumentStringWithConfig() {
        String s = getOptionsString();
        s = s.length() == 0 ? getInstrumentString() : (getInstrumentString() + " / " + s);
        return s;
    }

    public int getFirstUnexecutedStep() {
        return firstStep;
    }

    public Group getGroup() {
        return group;
    }

    public Priority getPriority() {
        return priority;
    }

    public TooType getTooPriority() {
            return tooPriority;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Obs) {
            Obs obs = (Obs) obj;
            return prog.equals(obs.prog) && obsNumber == obs.obsNumber;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return prog.hashCode() + obsNumber;
    }

    public String getCustomMask() {
        return customMask;
    }

    public Double getCentralWavelength() {
        return centralWavelength;
    }

    public double getElevationConstraintMax() {
        if (siteQuality != null) {
            return siteQuality.getElevationConstraintMax();
        }
        return 2.0; // ?
    }

    public double getElevationConstraintMin() {
        if (siteQuality != null) {
            return siteQuality.getElevationConstraintMin();
        }
        return 1.0; // ?
    }

    public ElevationConstraintType getElevationConstraintType() {
        if (siteQuality != null) {
            return siteQuality.getElevationConstraintType();
        }
        return ElevationConstraintType.NONE;
    }

    public List<TimingWindow> getTimingWindows() {
        if (siteQuality != null) {
            return siteQuality.getTimingWindows();
        }
        else return Collections.emptyList();
    }

    public boolean hasElevationConstraints() {
        return getElevationConstraintType() != ElevationConstraintType.NONE;
    }

    public boolean hasTimingConstraints() {
        return !getTimingWindows().isEmpty();
    }

    /**
     * Returns true if the science target is sidereal.
     * Call this only if you know there is a target environment.
     */
    public boolean isSidereal() {
        assert targetEnvironment != null;
        return targetEnvironment.getAsterism().isSidereal();
    }

    /**
     * Returns true if the science target is non-sidereal.
     * Call this only if you know there is a target environment.
     */
    public boolean isNonSidereal() {
        assert targetEnvironment != null;
        return targetEnvironment.getAsterism().isNonSidereal();
    }

    public Object getConstraintsString() {
        switch (getElevationConstraintType()) {
        case AIRMASS:
            return getConditions() + " / " + getElevationConstraintMin() + " \u2264 airmass \u2264 " + getElevationConstraintMax();
        case HOUR_ANGLE:
            long min = (long) (TimeUtils.MS_PER_HOUR * getElevationConstraintMin());
            long max = (long) (TimeUtils.MS_PER_HOUR * getElevationConstraintMax());
            return getConditions() + " / " + TimeUtils.msToHHMMSS(min) + " \u2264 ha \u2264 " + TimeUtils.msToHHMMSS(max);
        default:
            return getConditions();
        }
    }

    public static String createObsId(Prog prog, int obsNumber) {
        return prog.getStructuredProgramId().getShortName() + " [" + obsNumber + "]";
    }

    public Option<SchedulingBlock> getSchedulingBlock() {
        return schedulingBlock;
    }
}


