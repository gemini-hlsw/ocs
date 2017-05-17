package edu.gemini.qpt.shared.sp;

import edu.gemini.ags.api.*;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Semester;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.gmos.*;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObsTimesService;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.obscomp.SPNote;
import edu.gemini.spModel.core.ProgramId;
import edu.gemini.spModel.core.ProgramId$;
import edu.gemini.spModel.core.ProgramType;
import edu.gemini.spModel.seqcomp.SeqConfigComp;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.qpt.shared.sp.ServerExclusion.*;
import edu.gemini.qpt.shared.sp.Note.Scope;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;

import java.beans.PropertyDescriptor;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import scala.collection.JavaConversions;

/**
 * Query functor that generates the sp mini-model snapshot.
 */
public class ObsQueryFunctor extends DBAbstractQueryFunctor implements Iterable<Prog> {
    /**
     * TODO: Temporary system property to indicate whether or not we should calculate the
     * TODO: AGS anaylsis
     */
    private static final boolean CalculateAgsAnalysis = Boolean.parseBoolean(System.getProperty("edu.gemini.qpt.shared.sp.ObsQueryFunctor.CalculateAgsAnalysis", "true"));

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ObsQueryFunctor.class.getName());

    private final SortedSet<Prog> programSet = new TreeSet<>();
    private final SortedSet<String> misconfiguredObservations = new TreeSet<>();
    private final SortedSet<String> allSemesters = new TreeSet<>();
    private final Map<SPProgramID, ProgramExclusion> programExclusions = new TreeMap<>();
    private final Map<SPObservationID, ObsExclusion> obsExclusions = new TreeMap<>();

    private final Site site;
    private final Calendar date;
    private final List<ProgramType> progTypes;
    private final Set<ObsClass> relevantObsClasses;
    private final Set<ObservationStatus> relevantObsStatuses;
    private final Set<Semester> relevantSemesters;
    private final Set<Semester> rolloverSemesters;
    private final Boolean skipCompletedPrograms;   // skips programs that are set to completed
    private final Boolean skipInactivePrograms;    // skips programs that are set to inactive
    private final Boolean skipInvalidObservations; // skips observations that have no conditions, instrument or target
    private final Boolean skipNoStepsObservations; // skips observations that have no remaining steps (QPT does not need those)
    private final AgsMagnitude.MagnitudeTable magTable;

    /**
     * Constructs a functor that will retrieve candidate observations for the given input values.
     * This functor is tailored to the needs of the QV. It does not return "invalid" observations
     * that don't have a site quality or a target environment defined.
     * @param site
     * @param semesters
     * @param progTypes
     * @param obsClasses
     * @param obsStatuses
     */
    public ObsQueryFunctor(Site site, Set<Semester> semesters, List<ProgramType> progTypes, Set<ObsClass> obsClasses, Set<ObservationStatus> obsStatuses, boolean skipCompletedPrograms, boolean skipInactivePrograms, AgsMagnitude.MagnitudeTable magTable) {

        this.site = site;
        this.date = null;
        this.progTypes = progTypes;
        this.relevantObsClasses = obsClasses;
        this.relevantObsStatuses = obsStatuses;
        this.relevantSemesters = semesters;
        this.rolloverSemesters = Collections.emptySet();
        this.skipCompletedPrograms = skipCompletedPrograms;
        this.skipInactivePrograms = skipInactivePrograms;
        // NOTE: in order to make QV work with invalid observations some additional work would be needed to avoid
        // NPEs when accessing potential null fields (instrument, siteQuality and targetEnvironment in Obs), for now
        // we don't need this and simply skip invalid/incomplete observations.
        this.skipInvalidObservations = true;
        this.skipNoStepsObservations = false; // QV wants those, while QPT is not interested in them
        this.magTable = magTable;

    }

    /**
     * Constructs a functor that will retrieve candidate observations for the given input values.
     * This functor is specialized for the needs of the QPT; it gets all relevant observations for the semester
     * that contains the given date and additionally all rollovers for two previous semesters. It also returns
     * invalid observations (i.e. observations without siteQuality and targetEnvironment. It also skips observations
     * without remaining steps.
     * @param site
     * @param date
     * @param extraSemesters
     * @param progTypes
     * @param obsClasses
     * @param obsStatuses
     */
    public ObsQueryFunctor(Site site, Date date, final Set<Semester> extraSemesters, List<ProgramType> progTypes, Set<ObsClass> obsClasses, Set<ObservationStatus> obsStatuses, AgsMagnitude.MagnitudeTable magTable) {

        this.site = site;
        this.date = Calendar.getInstance(site.timezone());
        this.date.setTimeInMillis(date.getTime());
        this.progTypes = progTypes;
        this.relevantObsClasses = obsClasses;
        this.relevantObsStatuses = obsStatuses;

        // figure out current semester and add two previous ones for roll overs
        // (this is the default behavior for QPT)
        final Semester semCurrent = new Semester(site, date);
        final Semester semRollover1 = semCurrent.prev();
        final Semester semRollover2 = semRollover1.prev();

        this.relevantSemesters = new HashSet<Semester>() {{ add(semCurrent); addAll(extraSemesters); }};
        this.rolloverSemesters = new HashSet<Semester>() {{ add(semRollover1); add(semRollover2); }};

        // QPT skips completed programs, gets invalid observations (e.g. standards without conditions and others)
        // and is not interested in observations that have no remaining steps
        this.skipCompletedPrograms = true;
        this.skipInvalidObservations = false;
        this.skipInactivePrograms = false;
        this.skipNoStepsObservations = true;
        this.magTable = magTable;
    }

    @SuppressWarnings("unchecked")
    public void execute(IDBDatabaseService db, ISPNode progNode, Set<Principal> principals) {
        try {

            final ISPProgram programShell = (ISPProgram) progNode;
            final SPProgram program = (SPProgram) programShell.getDataObject();

            // Get program ID and its string value. Punt if it's null.
            final SPProgramID id = programShell.getProgramID();
            if (id == null) return;

            if (program == null) {
                LOGGER.severe("\n*** NULL PROGRAM DATA OBJECT IN " + id + "\n");
                return;
            }

            // skip programs that are complete (if necessary)
            if (skipCompletedPrograms && program.isCompleted()) {
                programExclusions.put(id, ProgramExclusion.MARKED_COMPLETE);
                return;
            }
            // skip programs that are inactive (if necessary)
            if (skipInactivePrograms && !program.isActive()) {
                programExclusions.put(id, ProgramExclusion.INVALID_SEMESTER_OR_TYPE);
                return;
            }

            // deal with different program types
            final ProgramId programId = ProgramId$.MODULE$.parse(programShell.getProgramID().stringValue());

            // -- if we can't parse the program id we ignore the whole program
            // (it's probably a test/toy program with a spiffy name like "JamesBond-007")
            if (programId.semester().isEmpty() || programId.ptype().isEmpty()) {
                programExclusions.put(id, ProgramExclusion.INVALID_SEMESTER_OR_TYPE);
                return;
            }

            // -- ok, we are sure we have a semester and a program type, get them
            final Semester semester = programId.semester().get();
            final ProgramType ptype = programId.ptype().get();
            allSemesters.add(semester.toString());

            // -- check for relevant program types
            if (!progTypes.contains(ptype)) {
                return;
            }
            // -- check for relevant semesters or rollover (this must be true for all program types)
            if (!relevantSemesters.contains(semester) &&
                !(isRollover(programShell) && rolloverSemesters.contains(semester))) {
                programExclusions.put(id, ProgramExclusion.INVALID_SEMESTER_OR_TYPE);
                return;
            }
            // -- check for exact date if one is given and we have a "daily" program ID (date is relevant for QPT)
            if (date != null && programId instanceof ProgramId.Daily) {
                final ProgramId.Daily dailyId = (ProgramId.Daily) programId;
                // if we have a date (in the id AND input parameters) then compare date
                if (dailyId.year() != date.get(Calendar.YEAR) ||
                    dailyId.month() != (date.get(Calendar.MONTH) + 1) || // cal month is zero based
                    dailyId.day() != date.get(Calendar.DAY_OF_MONTH) ) {

                    programExclusions.put(id, ProgramExclusion.INVALID_SEMESTER_OR_TYPE);
                    return;
                }
            }

            // -- Get the science band and times
            final int band;
            long plannedTime = 0;
            long usedTime = 0;
            long remainingTime = 0;
            long band3MinimumTime = -1, band3RemainingTime = 0;

            if (ptype == ProgramType.Calibration$.MODULE$ ||
                ptype == ProgramType.Engineering$.MODULE$) {

                // set band to 4 for all engineering and calibration programs
                band = 4;

            } else {

                try {
                    band = (program.getProgramMode() == SPProgram.ProgramMode.CLASSICAL) ? 1 // REL-432
                            : Integer.parseInt(program.getQueueBand());
                } catch (NumberFormatException nfe) {
                    LOGGER.warning("Program " + id + " has non-numeric queue band: " + program.getQueueBand());
                    programExclusions.put(id, ProgramExclusion.NON_NUMERIC_BAND);
                    return;
                }

                // Get the planned and used program time.
                final ObsTimes obsTimes = ObsTimesService.getCorrectedObsTimes(programShell);
                final ObsTimeCharges otc = obsTimes.getTimeCharges();
                plannedTime = PlannedTimeSummaryService.getTotalTime(programShell).getExecTime();
                usedTime = otc.getTime(ChargeClass.PROGRAM);

                // Get the remaining program time.
                TimeValue awardedTime = program.getAwardedProgramTime();
                remainingTime = awardedTime == null ? 0 : awardedTime.getMilliseconds() - usedTime;
                if (awardedTime == null) {
                    LOGGER.warning("Program " + id + " has null awarded time.");
                }

                // In the case of post-07A Band 3 programs, there can also be a
                // minimum completion time allotted.
                if (band == 3) {
                    TimeValue minTime = program.getMinimumTime();
                    if ((minTime != null) && (minTime.getTimeAmount() > 0)) {
                        band3MinimumTime = (long) minTime.convertTimeAmountTo(TimeValue.Units.seconds) * 1000;
                        band3RemainingTime = band3MinimumTime;
                        band3RemainingTime -= usedTime;
                        if (band3RemainingTime < 0) {
                            band3RemainingTime = 0;
                        }
                    }
                }
            }

            // Create our science program.
            Prog prog = new Prog(
                    program, id, program.isActive(), band, isRollover(programShell),
                    plannedTime, usedTime, remainingTime,
                    band3MinimumTime == -1 ? null : band3MinimumTime,
                    band3RemainingTime == 0 ? null : band3RemainingTime,
                    program.getPILastName(),
                    program.getPrimaryContactEmail(),
                    program.getContactPerson());
            List<Group> groupList = new ArrayList<>();
            List<Note> noteList = new ArrayList<>();

            // Now collect its direct obs children.
            List<Obs> obsList = new ArrayList<>();
            for (ISPNode rn: programShell.getChildren()) {

                if (rn instanceof ISPObservation) {

                    Obs obs = getObs(prog, (ISPObservation) rn, null);
                    addObsIfOk(obsList, obs);

                } else if (rn instanceof ISPGroup) {

                    ISPGroup groupShell = (ISPGroup) rn;
                    SPGroup group = (SPGroup) rn.getDataObject();
                    List<Obs> groupObservations = new ArrayList<>();
                    List<Note> groupNotes = new ArrayList<>();

                    Group miniGroup = new Group(group.getGroup(), group.getGroupType(), groupShell.getNodeKey().toString());

                    for (ISPNode rn2: groupShell.getChildren()) {

                        if (rn2 instanceof ISPObservation) {

                            Obs obs = getObs(prog, (ISPObservation) rn2, miniGroup);
                            addObsIfOk(groupObservations, obs);

                        } else {

                            Object o = rn2.getDataObject();
                            if (o instanceof SPNote) {
                                SPNote spnote = (SPNote) o;
                                groupNotes.add(new Note(Scope.Group, spnote.getTitle(), spnote.getNote()));
                            }

                        }

                    }

                    miniGroup.setChildren(groupObservations, groupNotes);
                    groupList.add(miniGroup);

                } else {

                    Object o = rn.getDataObject();
                    if (o instanceof SPNote) {
                        SPNote spnote = (SPNote) o;
                        noteList.add(new Note(Scope.Group, spnote.getTitle(), spnote.getNote()));
                    }

                }
            }

            // Finish constructing the program.
            prog.setChildren(obsList, groupList, noteList);
            programSet.add(prog);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Trouble in functor.", e);
        }
    }

    public Iterator<Prog> iterator() {
        return programSet.iterator();
    }

    /** Checks if an observation is ok and should be added to the result. */
    private void addObsIfOk(List<Obs> list, Obs observation) {
        if (observation == null) return;
        if (skipInvalidObservations && !observation.isValid()) return;
        list.add(observation);
    }

    @SuppressWarnings("unchecked")
    private SPComponentType[] instrument(ISPObservation obsShell) throws RemoteException {

        // For each obs, we can have an inst and an AO
        SPComponentType inst = null;
        SPComponentType ao = null;

        // Need to look through all the components.
        for (ISPObsComponent comp: obsShell.getObsComponents()) {
            SPComponentType type = comp.getType();

            if (Gems.SP_TYPE.equals(type))
                continue; // REL-293

            // There will be zero or one.
            if (SPComponentBroadType.AO.equals(type.broadType)) {
                ao = type;
                if (inst != null)
                    break;
                continue;
            }

            // There will be zero or one.
            if (SPComponentBroadType.INSTRUMENT.equals(type.broadType)) {
                inst = type;
                if (ao != null)
                    break;
            }

        }

        return (inst == null) ? new SPComponentType[0] :
            (ao == null) ? new SPComponentType[] { inst } :
                new SPComponentType[] { inst, ao };

    }

    @SuppressWarnings("unchecked")
    private Double centralWavelength(ISPObservation obsShell) throws RemoteException {
        for (ISPObsComponent comp: obsShell.getObsComponents()) {
            SPComponentType type = comp.getType();

            // This is only relevant for some instruments
            if (type.equals(InstGNIRS.SP_TYPE)) return ((InstGNIRS) comp.getDataObject()).getCentralWavelength().doubleValue();
            if (type.equals(InstTReCS.SP_TYPE)) return ((InstTReCS) comp.getDataObject()).getDisperserLambda();

        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Set<Enum<?>> options(ISPObservation obsShell) throws RemoteException {

        Set<Enum<?>> ret = new HashSet<>();

        // Need to look through all the components.
        for (ISPObsComponent comp: obsShell.getObsComponents()) {
            SPComponentType type = comp.getType();

            // There will be zero or one.
            if (SPComponentBroadType.INSTRUMENT.equals(type.broadType)) {

                // GMOS
                if (type.equals(InstGmosNorth.SP_TYPE) || type.equals(InstGmosSouth.SP_TYPE)) {

                    InstGmosCommon<?, ?, ?, ?> gmos = (InstGmosCommon<?, ?, ?, ?>) comp.getDataObject();
                    ret.add(gmos.getFPUnit());
                    ret.add(gmos.getDisperser());
                    ret.add(gmos.getFilter());
                    ret.add(gmos.getDetectorManufacturer());
                    ret.add(gmos.getUseNS());
                    ret.add(gmos.getPreImaging());

                    addFromIterators(obsShell, ret,
                            InstGmosCommon.FPU_PROP_NAME,
                            InstGmosCommon.DISPERSER_PROP_NAME,
                            InstGmosCommon.FILTER_PROP_NAME);

                }

                // Flamingos2
                if (type.equals(Flamingos2.SP_TYPE)) {

                    Flamingos2 f2 = (Flamingos2) comp.getDataObject();
                    ret.add(f2.getFpu());
                    ret.add(f2.getDisperser());
                    ret.add(f2.getFilter());
                    ret.add(f2.getPreImaging());

                    addFromIterators(obsShell, ret,
                            Flamingos2.FPU_PROP,
                            Flamingos2.DISPERSER_PROP,
                            Flamingos2.FILTER_PROP);

                }

                // GSAOI
                if (type.equals(Gsaoi.SP_TYPE)) {

                    Gsaoi gsaoi = (Gsaoi) comp.getDataObject();
                    ret.add(gsaoi.getFilter());

                    addFromIterators(obsShell, ret,
                            Gsaoi.FILTER_PROP);

                }

                // NIRI
                if (type.equals(InstNIRI.SP_TYPE)) {

                    InstNIRI niri = (InstNIRI) comp.getDataObject();
                    ret.add(niri.getFilter());
                    ret.add(niri.getDisperser());
                    ret.add(niri.getMask());
                    ret.add(niri.getCamera());

                    addFromIterators(obsShell, ret,
                            InstNIRI.DISPERSER_PROP,
                            InstNIRI.MASK_PROP,
                            InstNIRI.FILTER_PROP,
                            InstNIRI.CAMERA_PROP);

                }

                // GNIRS
                if (type.equals(InstGNIRS.SP_TYPE)) {

                    InstGNIRS gnirs = (InstGNIRS) comp.getDataObject();
                    ret.add(gnirs.getDisperser());
                    ret.add(gnirs.getSlitWidth());
                    ret.add(gnirs.getCrossDispersed());
                    ret.add(gnirs.getFilter());
                    // don't add the camera here because we don't want the
                    // default value

                    addFromIterators(obsShell, ret,
                            InstGNIRS.FILTER_PROP,
                            InstGNIRS.DISPERSER_PROP,
                            InstGNIRS.SLIT_WIDTH_PROP,
                            InstGNIRS.CROSS_DISPERSED_PROP,
                            InstGNIRS.CAMERA_PROP); // pick up explicitly defined cameras if any

                    // Add implicitly computed camera if not set as an engineering
                    // parameter.
                    boolean hasEngineeringCamera = false;
                    for (GNIRSParams.Camera c : GNIRSParams.Camera.values()) {
                        hasEngineeringCamera = hasEngineeringCamera || ret.contains(c);
                    }
                    if (!hasEngineeringCamera) {
                        final GNIRSParams.PixelScale ps = gnirs.getPixelScale();
                        ret.add(GNIRSParams.Camera.getDefault(gnirs.getCentralWavelength().doubleValue(), ps));
                        addGnirsCameras(obsShell, ret, ps);
                    }
                }

                // TReCS
                if (type.equals(InstTReCS.SP_TYPE)) {

                    InstTReCS trecs = (InstTReCS) comp.getDataObject();
                    ret.add(trecs.getDisperser());
                    ret.add(trecs.getMask());

                    addFromIterators(obsShell, ret,
                            InstTReCS.DISPERSER_PROP,
                            InstTReCS.MASK_PROP);

                }

                // NIFS
                if (type.equals(InstNIFS.SP_TYPE)) {

                    InstNIFS nifs = (InstNIFS) comp.getDataObject();
                    ret.add(nifs.getDisperser());
                    ret.add(nifs.getFilter());
                    ret.add(nifs.getMask());

                    addFromIterators(obsShell, ret,
                            InstNIFS.DISPERSER_PROP,
                            InstNIFS.FILTER_PROP,
                            InstNIFS.MASK_PROP);
                }

                // NICI
                if (type.equals(InstNICI.SP_TYPE)) {

                    InstNICI nici = (InstNICI) comp.getDataObject();
                    ret.add(nici.getFocalPlaneMask());
                    ret.add(nici.getDichroicWheel());
                    ret.add(nici.getChannel1Fw());
                    ret.add(nici.getChannel2Fw());

                    addFromIterators(obsShell, ret,
                            InstNICI.FOCAL_PLANE_MASK_PROP,
                            InstNICI.DICHROIC_WHEEL_PROP,
                            InstNICI.CHANNEL1_FW_PROP,
                            InstNICI.CHANNEL2_FW_PROP);
                }

                // Texes
                if (type.equals(InstTexes.SP_TYPE)) {

                    InstTexes texes = (InstTexes) comp.getDataObject();
                    ret.add(texes.getDisperser());

                    addFromIterators(obsShell, ret,
                            InstTexes.DISPERSER_PROP);
                }

            }

            // There will be zero or one
            if ( SPComponentBroadType.AO.equals(type.broadType)) {

                // If it's altair we need to grab the guide star type
                if (type.equals(InstAltair.SP_TYPE)) {
                    InstAltair altair = (InstAltair)  comp.getDataObject();
                    ret.add(altair.getGuideStarType());
                }

            }

            // REL-293: check for WFS
            if (TargetObsComp.SP_TYPE.equals(type)) {
                TargetObsComp targetObsComp = (TargetObsComp)comp.getDataObject();
                for(GuideProbe guideProbe : targetObsComp.getTargetEnvironment().getPrimaryGuideGroup().getReferencedGuiders()) {
                    if (guideProbe instanceof Enum) {
                        ret.add((Enum)guideProbe);
                    }
                }
            }
        }

        return ret;

    }

    /**
     * Look for Altair with LGS or Gems
     *
     * @param obsShell
     * @return true if LGS is used by this observation
     * @throws RemoteException
     */
    private boolean hasLGS(ISPObservation obsShell) throws RemoteException {

        // Need to look through all the components.
        for (ISPObsComponent comp : obsShell.getObsComponents()) {
            SPComponentType type = comp.getType();
            // There will be zero or one
            if (SPComponentBroadType.AO.equals(type.broadType)) {

                // If it's altair we need to grab the guide star type
                if (type.equals(InstAltair.SP_TYPE)) {
                    InstAltair altair = (InstAltair) comp.getDataObject();
                    AltairParams.GuideStarType gs = altair.getGuideStarType();
                    if (gs == AltairParams.GuideStarType.LGS) return true;
                }
                if (type.equals(Gems.SP_TYPE)) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * Looks for an AO component.
     *
     * @param obsShell
     * @return true if AO is used by this observation
     * @throws RemoteException
     */
    private boolean hasAO(ISPObservation obsShell) throws RemoteException {
        for (ISPObsComponent comp : obsShell.getObsComponents()) {
            SPComponentType type = comp.getType();
            if (SPComponentBroadType.AO.equals(type.broadType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if the observation is scheduled to use the average parallactic angle.
     *
     * @param obsShell
     * @return true if the observation uses the average parallactic angle
     * @throws RemoteException
     */
    private boolean usesAverageParallacticAngle(final ISPObservation obsShell) throws RemoteException {
        return obsShell.getObsComponents().stream().anyMatch(comp -> {
            final ISPDataObject dObj = comp.getDataObject();
            return (dObj instanceof PosAngleConstraintAware) && ((PosAngleConstraintAware) dObj).getPosAngleConstraint().isParallactic();
        });
    }

    private void applySysConfigs(ISPObservation obsShell, ApplyOp<ISysConfig> op) {
        try {
            // Now look at all the instrument iterators in the sequence. This is much faster
            // than unrolling the whole sequence. We're doing a breadth-first search for all
            // ISPSeqComponents, where the root one is conveniently available on the obsShell.
            LinkedList<ISPSeqComponent> queue = new LinkedList<>();
            ISPSeqComponent seqShell = obsShell.getSeqComponent();
            if (seqShell != null) queue.addLast(seqShell);
            while (!queue.isEmpty()) {
                seqShell = queue.removeFirst();
                queue.addAll(seqShell.getSeqComponents());
                Object obj = seqShell.getDataObject();
                if (obj instanceof SeqConfigComp) { // this should always be true, I think
                    SeqConfigComp scc = (SeqConfigComp) obj;
                    ISysConfig config = scc.getSysConfig();
                    op.apply(config);
                }
            }
        } catch (Exception e) {
            // Log and keep going; this will reduce the amount of information available
            // to QPT but isn't fatal. Watch for these exceptions as the model changes;
            // current code makes some assumptions about the contents of untyped collections.
            LOGGER.log(Level.WARNING, "Problem examining instrument iterator.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addFromIterators(ISPObservation obsShell, final Set<Enum<?>> ret, final String... params) {
        applySysConfigs(obsShell, config -> {
            for (String name: params) {
                IParameter param = config.getParameter(name);
                if (param != null) {
                    // magically, this will be a List<Enum>. I think.
                    for (Enum<?> v: (List<Enum<?>>) param.getValue())
                        if (v != null) ret.add(v);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void addFromIterators(ISPObservation obsShell, Set<Enum<?>> ret, PropertyDescriptor... params) throws RemoteException {
        String[] names = new String[params.length];
        for (int i = 0; i < names.length; i++)
            names[i] = params[i].getName();
        addFromIterators(obsShell, ret, names);
    }

    private void addGnirsCameras(ISPObservation obsShell, final Set<Enum<?>> ret, final GNIRSParams.PixelScale pixelScale) {
        applySysConfigs(obsShell, config -> {
            // the parameter can be a single wavelength or a DefaultParameter with a collection of wavelengths
            final IParameter param = config.getParameter(InstGNIRS.CENTRAL_WAVELENGTH_PROP.getName());
            addGnirsCameras(param, ret, pixelScale);
        });
    }

    private void addGnirsCameras(Object param, final Set<Enum<?>> ret, final GNIRSParams.PixelScale pixelScale) {
        if (param instanceof GNIRSParams.Wavelength) {
            final GNIRSParams.Wavelength wl = (GNIRSParams.Wavelength) param;
            ret.add(GNIRSParams.Camera.getDefault(wl.doubleValue(), pixelScale));
        } else if (param instanceof DefaultParameter) {
            final DefaultParameter dParam = (DefaultParameter) param;
            if (dParam.getValue() instanceof ArrayList) {
                for (Object p : (ArrayList) dParam.getValue()) {
                    addGnirsCameras(p, ret, pixelScale);
                }
            }
        }
        // anything else will be ignored.. (including null)
    }

    @SuppressWarnings("unchecked")
    private String customMask(ISPObservation obsShell) throws RemoteException {

        // Need to look through all the components.
        for (ISPObsComponent comp: obsShell.getObsComponents()) {
            SPComponentType type = comp.getType();

            // There will be zero or one.
            if (SPComponentBroadType.INSTRUMENT.equals(type.broadType)) {

                // GMOS
                if (type.equals(InstGmosNorth.SP_TYPE) || type.equals(InstGmosSouth.SP_TYPE)) {
                    InstGmosCommon<?, ?, ?, ?> gmos = (InstGmosCommon<?, ?, ?, ?>) comp.getDataObject();
                    return gmos.getFPUnitCustomMask();
                }

                // Flamingos2 - FPU
                if (type.equals(Flamingos2.SP_TYPE)) {
                    Flamingos2 flam2 = (Flamingos2) comp.getDataObject();
                    return flam2.getFpuCustomMask();
                }

            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Obs getObs(Prog info, ISPObservation obsShell, Group group) throws RemoteException {

        // Collect obs info
        SPObservation obs = (SPObservation) obsShell.getDataObject();
        SPObservationID id = obsShell.getObservationID(); // will never be null if there's a progid (which there is)

        // Only look at those that are of the proper obs class.
        ObsClass obsClass = ObsClassService.lookupObsClass(obsShell);
        if (obsClass == null || !relevantObsClasses.contains(obsClass)) {
            obsExclusions.put(id, ObsExclusion.EXCLUDED_CLASS);
            return null;
        }

        // Only if it's ready or ongoing...
        ObservationStatus obsStatus = ObservationStatus.computeFor(obsShell);
        if (obsStatus == null || !relevantObsStatuses.contains(obsStatus)) {
            obsExclusions.put(id, ObsExclusion.EXCLUDED_STATUS);
            return null;
        }

        // Get the target environment; NOTE: this may be null!
        TargetEnvironment targetEnv = null;
        for (ISPObsComponent obsCompShell: obsShell.getObsComponents()) {
            SPComponentType type = obsCompShell.getType();
            if (type.equals(TargetObsComp.SP_TYPE)) {
                TargetObsComp targetObsComp = (TargetObsComp) obsCompShell.getDataObject();
                if (targetObsComp != null) targetEnv = targetObsComp.getTargetEnvironment();
                break;
            }
        }

        // Get the site conditions
        SPSiteQuality quality = null;
        for (ISPObsComponent obsCompShell: obsShell.getObsComponents()) {
            SPComponentType type = obsCompShell.getType();
            if (type.equals(SPSiteQuality.SP_TYPE)) {
                quality = (SPSiteQuality) obsCompShell.getDataObject();
                break;
            }
        }

        // Get the instrument -- if not present, then ignore.
        // FR-8265: work around for a model bug
        ISPObsComponent inst = null;
        for (ISPObsComponent obsCompShell: obsShell.getObsComponents()) {
            SPComponentType type = obsCompShell.getType();
            if (SPComponentBroadType.INSTRUMENT.equals(type.broadType)) {
                inst = obsCompShell;
                break;
            }
        }
        if (inst == null) return null; //  no instrument component

        // And seq steps
        PlannedStepSummary steps = PlannedTimeSummaryService.getPlannedSteps(obsShell);
        if (skipNoStepsObservations && (steps.size() == 0 || steps.isStepExecuted(steps.size() - 1))) {
            LOGGER.warning(id.toString() + " is " + obsStatus + " but has no remaining steps.");
            // Don't complain for ENG or CAL observations
            if (!info.isEngOrCal()) {
                misconfiguredObservations.add(id.toString());
            }
            obsExclusions.put(id, ObsExclusion.NO_REMAINING_STEPS);
            return null;
        }

        // And times
        PlannedTimeSummary times = PlannedTimeSummaryService.getTotalTime(obsShell);
        long elapsedTime = 0;
        ObsTimes obsTimes = ObsTimesService.getRawObsTimes(obsShell);
        if (obsTimes != null) {
            ObsTimeCharges otc = obsTimes.getTimeCharges();
            elapsedTime += otc.getTime(ChargeClass.PROGRAM);
            elapsedTime += otc.getTime(ChargeClass.PARTNER);
            elapsedTime += otc.getTime(ChargeClass.NONCHARGED);
        }

        // some other values derived from obs or obsShell:
        TooType tooPriority = Too.get(obsShell);
        SPObservation.Priority priority = obs.getPriority() != null ? obs.getPriority() : SPObservation.Priority.DEFAULT;


        // Construct the AgsAnalyses for this observation under the following conditions:
        // 1. IF the system property is not set or is set to true, and
        // 2. IF the observation needs a guide star.
        List<AgsAnalysis> analysis = new ArrayList<>();

        if (CalculateAgsAnalysis && SPObservation.needsGuideStar(obsShell)) {
            Option<ObsContext> ctxOpt = ObsContext.create(obsShell);
            if (!ctxOpt.isEmpty()) {
                ObsContext ctx = ctxOpt.getValue();

                // Perform the analysis.
                scala.Option<AgsStrategy> strategyOption = AgsRegistrar.currentStrategy(ctx);
                if (strategyOption.isDefined()) {
                    AgsStrategy strategy = strategyOption.get();
                    analysis.addAll(JavaConversions.seqAsJavaList(strategy.analyze(ctx, magTable)));
                }
            }
        }


        // create new Obs object based on information collected
        return new Obs(
            info,
            group,
            obsShell.getObservationNumber(),
            Obs.createObsId(info, obsShell.getObservationNumber()),
            obs.getTitle(),
            priority,
            tooPriority,
            ObservationStatus.computeFor(obsShell),
            obsClass,
            targetEnv,
            instrument(obsShell),
            options(obsShell),
            customMask(obsShell),
            centralWavelength(obsShell),
            steps,
            times.getPiTime(),
            times.getExecTime(),
            elapsedTime,
            quality,
            hasLGS(obsShell),
            hasAO(obsShell),
            usesAverageParallacticAngle(obsShell),
            DefaultImList.create(analysis),
            obs.getSchedulingBlock()
        );

    }

    private static boolean isRollover(ISPProgram programShell) throws RemoteException {
        try {
            return ((SPProgram) programShell.getDataObject()).getRolloverStatus();
//            return P1DocumentUtil.getGeminiPart(P1DocumentUtil.lookupProposal(programShell)).getITacExtension().getRolloverFlag();
        } catch (NullPointerException npe) {
            // If the required structure isn't there, rollover is false. This
            // condition would be unusual but is legal. Easier to catch NPE than
            // check all the null conditions.
        }
        return false;
    }

    SortedSet<Prog> getProgramSet() {
        return programSet;
    }

    SortedSet<String> getMisconfiguredObservations() {
        return misconfiguredObservations;
    }

    /**
     * Set of all semesters in the database.
     */
    SortedSet<String> getAllSemesters() {
        return allSemesters;
    }

    Map<SPProgramID, ProgramExclusion> getProgramExclusions() {
        return programExclusions;
    }

    Map<SPObservationID, ObsExclusion> getObsExclusions() {
        return obsExclusions;
    }

}




