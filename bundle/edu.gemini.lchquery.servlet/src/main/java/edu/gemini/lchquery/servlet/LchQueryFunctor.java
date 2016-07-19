package edu.gemini.lchquery.servlet;

import edu.gemini.odb.browser.Conditions;
import edu.gemini.odb.browser.HmsDms;
import edu.gemini.odb.browser.NonSidereal;
import edu.gemini.odb.browser.Observation;
import edu.gemini.odb.browser.ObservationsNode;
import edu.gemini.odb.browser.Program;
import edu.gemini.odb.browser.ProgramsNode;
import edu.gemini.odb.browser.QueryResult;
import edu.gemini.odb.browser.Sidereal;
import edu.gemini.odb.browser.TargetsNode;
import edu.gemini.odb.browser.TimingWindow;
import edu.gemini.odb.browser.TimingWindowRepeats;
import edu.gemini.odb.browser.TimingWindowsNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;
import edu.gemini.pot.spdb.IDBParallelFunctor;
import edu.gemini.skycalc.DDMMSS;
import edu.gemini.skycalc.HHMMSS;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.ao.AOTreeUtil;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.util.SPTreeUtil;
import jsky.util.StringUtil;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the LCH query on the ODB
 */
public class LchQueryFunctor extends DBAbstractQueryFunctor implements IDBParallelFunctor {
    private static final Logger LOG = Logger.getLogger(LchQueryFunctor.class.getName());

    private static final long MS_PER_SECOND = 1000;
    private static final long MS_PER_MINUTE = MS_PER_SECOND * 60;
    private static final long MS_PER_HOUR = MS_PER_MINUTE * 60;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Type of query results: only programs, progs + observations or progs + obs + targets
    enum QueryType {
        PROGRAMS, OBSERVATIONS, TARGETS
    }

    private QueryResult _queryResult;
    private QueryType _queryType;

    private String _programSemester;
    private String _programTitle;
    private String _programInvestigatorNames;
    private String _programPiEmail;
    private String _programCoIEmails;
    private String _programAbstract;
    private String _programBand;
    private String _programPartners;
    private String _programReference;
    private String _programActive;
    private String _programCompleted;
    private String _programNotifyPi;
    private String _programRollover;
    private String _programTooStatus;
    private String _programAllocTime;
    private String _programRemainTime;

    private String _observationTooStatus;
    private String _observationName;
    private String _observationStatus;
    private String _observationInstrument;
    private String _observationAo;
    private String _observationClass;

    public LchQueryFunctor(QueryType queryType, String programSemester, String programTitle, String programReference,
                           String programActive, String programCompleted, String programNotifyPi, String programRollover,
                           String observationTooStatus, String observationName, String observationStatus, String observationInstrument,
                           String observationAo, String observationClass) {
        _queryResult = new QueryResult();
        _queryResult.setProgramsNode(new ProgramsNode());

        _queryType = queryType;

        _programSemester = programSemester;
        _programTitle = programTitle;
        _programReference = programReference;
        _programActive = programActive;
        _programCompleted = programCompleted;
        _programNotifyPi = programNotifyPi;
        _programRollover = programRollover;

        _observationTooStatus = observationTooStatus;
        _observationName = observationName;
        _observationStatus = observationStatus;
        _observationInstrument = observationInstrument;
        _observationAo = observationAo;
        _observationClass = observationClass;
    }

    public LchQueryFunctor(final QueryType queryType,
                           final String programSemester,
                           final String programTitle,
                           final String programInvestigatorNames,
                           final String programPiEmail,
                           final String programCoIEmails,
                           final String programAbstract,
                           final String programBand,
                           final String programPartners,
                           final String programReference,
                           final String programActive,
                           final String programCompleted,
                           final String programNotifyPi,
                           final String programRollover,
                           final String programTooStatus,
                           final String programAllocTime,
                           final String programRemainTime,
                           final String observationTooStatus,
                           final String observationName,
                           final String observationStatus,
                           final String observationInstrument,
                           final String observationAo,
                           final String observationClass) {
        _queryResult = new QueryResult();
        _queryResult.setProgramsNode(new ProgramsNode());

        _queryType = queryType;

        _programSemester = programSemester;
        _programTitle = programTitle;
        _programInvestigatorNames = programInvestigatorNames;
        _programPiEmail = programPiEmail;
        _programCoIEmails = programCoIEmails;
        _programAbstract = programAbstract;
        _programBand = programBand;
        _programPartners = programPartners;
        _programReference = programReference;
        _programActive = programActive;
        _programCompleted = programCompleted;
        _programNotifyPi = programNotifyPi;
        _programRollover = programRollover;
        _programTooStatus = programTooStatus;
        _programAllocTime = programAllocTime;
        _programRemainTime = programRemainTime;

        _observationTooStatus = observationTooStatus;
        _observationName = observationName;
        _observationStatus = observationStatus;
        _observationInstrument = observationInstrument;
        _observationAo = observationAo;
        _observationClass = observationClass;
    }

    /**
     * Called once per program by the <code>{@link edu.gemini.pot.spdb.IDBQueryRunner}</code>
     * implementation.
     */
    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        ISPProgram prog = (ISPProgram) node;
        try {
            if (_match(prog)) {
                List<ISPObservation> obsList = new ArrayList<ISPObservation>();
                for (ISPObservation obs : prog.getAllObservations()) {
                    if (_match(prog, obs)) {
                        obsList.add(obs);
                    }
                }
                if (obsList.size() != 0) {
                    if (_queryType == QueryType.PROGRAMS) {
                        addProgram(prog, new ArrayList<ISPObservation>());
                    } else {
                        addProgram(prog, obsList);
                    }
                }
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "problem running LchQueryFunctor", t);
            throw new RuntimeException(t);
        }
    }

    // Returns "Yes" for "true" and "No" for "false"
    private static String _booleanValue(String s) {
        if ("true".equalsIgnoreCase(s)) return YesNoType.YES.displayValue();
        if ("false".equalsIgnoreCase(s)) return YesNoType.NO.displayValue();
        return s;
    }

    /**
     * Return true if the given science program matches the conditions.
     * If no conditions apply at the program level, true is returned.
     * False is only returned if a condition is specified that does not match.
     */
    private boolean _match(ISPProgram prog) throws RemoteException {
        SPProgram spProg = (SPProgram) prog.getDataObject();
        SPProgram.PIInfo piInfo = spProg.getPIInfo();

        if (_programSemester != null) {
            String semester = _getSemester(prog);
            if (semester == null || !StringUtil.match(_programSemester, semester)) {
                return false;
            }
        }

        if (_programTitle != null) {
            String title = spProg.getTitle();
            if (title == null || !StringUtil.match(_programTitle, title)) {
                return false;
            }
        }

        if (_programReference != null) {
            SPProgramID progId = prog.getProgramID();
            if (progId == null || !StringUtil.match(_programReference, progId.stringValue())) {
                return false;
            }
        }

        if (_programActive != null) {
            SPProgram.Active active = spProg.getActive();
            if (active == null || !StringUtil.match(_booleanValue(_programActive), active.displayValue())) {
                return false;
            }
        }

        if (_programCompleted != null) {
            YesNoType completed = spProg.isCompleted() ? YesNoType.YES : YesNoType.NO;
            if (!StringUtil.match(_booleanValue(_programCompleted), completed.displayValue())) {
                return false;
            }
        }

        if (_programNotifyPi != null) {
            YesNoType notifyPi = spProg.getNotifyPi();
            if (!StringUtil.match(_booleanValue(_programNotifyPi), notifyPi.displayValue())) {
                return false;
            }
        }

        if (_programRollover != null) {
            YesNoType rollover = spProg.getRolloverStatus() ? YesNoType.YES : YesNoType.NO;
            if (!StringUtil.match(_booleanValue(_programRollover), rollover.displayValue())) {
                return false;
            }
        }

        return true;
    }

    // Add any matching observations to the query results.
    private boolean _match(ISPProgram prog, ISPObservation o) throws RemoteException {
        SPObservation obs = (SPObservation) o.getDataObject();

        if (_observationStatus != null) {
            final ObservationStatus obsStatus = ObservationStatus.computeFor(o);
            if (obsStatus == null || !StringUtil.match(_observationStatus, obsStatus.displayValue())) {
                return false;
            }
        }

        if (_observationTooStatus != null) {
            TooType too = Too.get(o);
            if (too == null || !StringUtil.match(_observationTooStatus, too.getDisplayValue())) {
                return false;
            }
        }

        if (_observationName != null) {
            String title = obs.getTitle();
            if (title == null || !StringUtil.match(_observationName, title)) {
                return false;
            }
        }

        if (_observationInstrument != null) {
            List<SPInstObsComp> instruments = _getInstruments(o);
            if (instruments.size() == 0 || !StringUtil.match(_observationInstrument, instruments.get(0).getType().readableStr)) {
                return false;
            }
        }

        if (_observationAo != null) {
            String ao = _getAO(o).displayValue();
            if (ao == null || !StringUtil.match(_observationAo, ao)) {
                return false;
            }
        }

        if (_observationClass != null) {
            ObsClass obsClass = ObsClassService.lookupObsClass(o);
            if (obsClass == null || !StringUtil.match(_observationClass, obsClass.displayValue())) {
                return false;
            }
        }
        return true;
    }

    // Return a description of the observations AO type.
    private AOConstants.AO _getAO(ISPObservation o) throws RemoteException {
        ISPObsComponent obsComp = AOTreeUtil.findAOSystem(o);
        if (obsComp != null) {
            SPComponentType type = obsComp.getType();
            if (type.equals(InstAltair.SP_TYPE)) {
                InstAltair inst = (InstAltair) obsComp.getDataObject();
                if (inst.getGuideStarType() == AltairParams.GuideStarType.LGS) {
                    return AOConstants.AO.Altair_LGS;
                } else if (inst.getGuideStarType() == AltairParams.GuideStarType.NGS) {
                    return AOConstants.AO.Altair_NGS;
                }
            }
        }
        return AOConstants.AO.NONE;
    }

    /**
     * Return a list containing the instrument and Altair data objects for the
     * given observation.
     */
    private List<SPInstObsComp> _getInstruments(ISPObservation o) throws RemoteException {
        List<SPInstObsComp> result = new ArrayList<SPInstObsComp>();
        List<ISPObsComponent> l = SPTreeUtil.findInstruments(o);
        for (ISPObsComponent obsComp : l) {
            Object dataObject = obsComp.getDataObject();
            if (dataObject instanceof SPInstObsComp) {
                result.add((SPInstObsComp) dataObject);
            }
        }
        return result;
    }


    /**
     * Return the semester for the given program
     */
    private String _getSemester(ISPProgram prog) {
        try {
            SPProgramID progId = prog.getProgramID();
            if (progId != null) {
                String s = progId.stringValue();
                int i = s.indexOf('-');
                if (i != -1 && s.charAt(i + 1) == '2') {
                    return s.substring(i + 1, i + 6);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

//    /**
//     * Return the target name for the given observation, or null if none is defined.
//     */
//    private ImList<SPTarget> _getTargets(ISPObservation o) throws RemoteException {
//        TargetEnvironment targetEnvironment = _getTargetEnvironment(o);
//        if (targetEnvironment == null) return null;
//        return targetEnvironment.getTargets();
//    }

    private TargetEnvironment _getTargetEnvironment(ISPObservation o) throws RemoteException {
        ISPObsComponent targetEnvNode = SPTreeUtil.findTargetEnvNode(o);
        if (targetEnvNode == null)
            return null;
        TargetObsComp targetEnv = (TargetObsComp) targetEnvNode.getDataObject();
        if (targetEnv == null)
            return null;
        return targetEnv.getTargetEnvironment();
    }

    /**
     * Return the conditions timing window for the given observation, or null if none are defined.
     */
    private List<SPSiteQuality.TimingWindow> _getTimingWindows(ISPObservation o) throws RemoteException {
        ISPObsComponent conditionsNode = SPTreeUtil.findObsCondNode(o);
        if (conditionsNode == null)
            return null;
        SPSiteQuality conditions = (SPSiteQuality) conditionsNode.getDataObject();
        if (conditions == null)
            return null;
        return conditions.getTimingWindows();
    }


    /**
     * Returns one of the following for the given target:
     * {@link edu.gemini.odb.browser.NonSidereal }
     * {@link edu.gemini.odb.browser.Sidereal }
     * ... or None if the coordinates are unknown
     */
    private Option<Serializable> _makeTargetNode(SPTarget target, TargetEnvironment targetEnvironment) {
        if (target.isNonSidereal()) {
            NonSidereal nonSidereal = new NonSidereal();
            nonSidereal.setName(target.getName());
            nonSidereal.setType(_getTargetType(target, targetEnvironment));

            // TODO: update for new model, which has better HORIZONS support. we can
            // this BREAKS lch for nonsidereal targets

//            Long id = ((NonSiderealTarget) target.getTarget()).getHorizonsObjectId();
//            if (id != null) {
//                nonSidereal.setHorizonsObjectId(id.toString());
//            }

            return new Some<>(nonSidereal);
        } else if (target.isSidereal()) {
            Sidereal sidereal = new Sidereal();
            sidereal.setName(target.getName());
            sidereal.setType(_getTargetType(target, targetEnvironment));

            // TODO: pattern match with new model and get the coords directly
            // for the time being we know that we don't need a time because this is a sidereal target
            return target.getSkycalcCoordinates(None.instance()).map(coords -> {
                HmsDms hmsDms = new HmsDms();
                hmsDms.setRa(HHMMSS.valStr(coords.getRa().getMagnitude()));
                hmsDms.setDec(DDMMSS.valStr(coords.getDec().getMagnitude()));
                sidereal.setHmsDms(hmsDms);
                return sidereal;
            });

        } else {
            return None.instance(); // it's a TOO target
        }
    }


    // Returns the type (tag) for the given target (have to look it up in the target environment)
    private String _getTargetType(SPTarget target, TargetEnvironment env) {
        if (env.isBasePosition(target)) return "Base";

        if (env.isGuidePosition(target)) {
            for (GuideGroup guideGroup : env.getGroups()) {
                for (GuideProbeTargets guideProbeTargets : guideGroup.getAll()) {
                    if (guideProbeTargets.containsTarget(target)) {
                        return guideProbeTargets.getGuider().getKey();
                    }
                }
            }
        }

        if (env.isUserPosition(target)) return "User";
        return null;
    }

    void addProgram(ISPProgram prog, List<ISPObservation> obsList) throws RemoteException {
        Program program = new Program();
        SPProgram spProg = (SPProgram) prog.getDataObject();
        program.setActive(spProg.getActive().displayValue());
        program.setCompleted(spProg.isCompleted() ? YesNoType.YES.displayValue() : YesNoType.NO.displayValue());
        SPProgramID progId = prog.getProgramID();
        if (progId != null) {
            program.setReference(progId.stringValue());
        }
        program.setSemester(_getSemester(prog));
        program.setTitle(spProg.getTitle());
        program.setContactScientistEmail(spProg.getContactPerson());
        program.setNgoEmail(spProg.getNGOContactEmail());
        program.setNotifyPi(spProg.getNotifyPi().displayValue());
        program.setPiEmail(spProg.getPIInfo() == null ? null : spProg.getPIInfo().getEmail());
        program.setRollover(spProg.getRolloverStatus() ? YesNoType.YES.displayValue() : YesNoType.NO.displayValue());

        if (_queryType != QueryType.PROGRAMS) {
            ObservationsNode observationsNode = new ObservationsNode();
            for (ISPObservation obs : obsList) {
                SPObservation spObs = (SPObservation) obs.getDataObject();
                Observation observation = new Observation();
                observation.setAo(_getAO(obs).displayValue());

                List<SPInstObsComp> instruments = _getInstruments(obs);
                if (instruments.size() != 0) {
                    observation.setInstrument(instruments.get(0).getType().readableStr);
                }
                observation.setName(spObs.getTitle());

                ObsClass obsClass = ObsClassService.lookupObsClass(obs);
                if (obsClass != null) {
                    observation.setObsClass(obsClass.displayValue());
                }

                observation.setId(obs.getObservationID().stringValue());
                observation.setStatus(ObservationStatus.computeFor(obs).displayValue());
                observation.setTooPriority(Too.get(obs).getDisplayValue());

                List<SPSiteQuality.TimingWindow> timingWindows = _getTimingWindows(obs);
                if (timingWindows != null && timingWindows.size() != 0) {
                    Conditions conditions = new Conditions();
                    TimingWindowsNode timingWindowsNode = new TimingWindowsNode();
                    for (SPSiteQuality.TimingWindow timingWindow : timingWindows) {
                        timingWindowsNode.getTimingWindows().add(_makeTimingWindow(timingWindow));
                    }
                    conditions.setTimingWindowsNode(timingWindowsNode);
                    observation.setConditions(conditions);
                }

                if (_queryType == QueryType.TARGETS) {
                    TargetEnvironment targetEnvironment = _getTargetEnvironment(obs);
                    if (targetEnvironment != null) {
                        ImList<SPTarget> targets = targetEnvironment.getTargets();
                        if (targets != null && targets.size() != 0) {
                            TargetsNode targetsNode = new TargetsNode();
                            for (SPTarget target : targets) {
                                _makeTargetNode(target, targetEnvironment).foreach(tn ->
                                    targetsNode.getTargets().add(tn)
                                );
                            }
                            observation.setTargetsNode(targetsNode);
                        }
                    }
                }
                observationsNode.getObservations().add(observation);
            }
            program.setObservationsNode(observationsNode);
        }
        _queryResult.getProgramsNode().getPrograms().add(program);
    }

    private static String _formatDuration(SPSiteQuality.TimingWindow tw) {
        long ms = tw.getDuration();
//        if (ms == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return "forever";
        if (ms == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return null;
        return String.format("%d:%02d", ms / MS_PER_HOUR, (ms % MS_PER_HOUR) / MS_PER_MINUTE);
    }

    private static String _formatPeriod(SPSiteQuality.TimingWindow tw) {
        if (tw.getDuration() == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return null;
        if (tw.getRepeat() == SPSiteQuality.TimingWindow.REPEAT_NEVER) return null;
        long ms = tw.getPeriod();
        return String.format("%d:%02d", ms / MS_PER_HOUR, (ms % MS_PER_HOUR) / MS_PER_MINUTE);
    }

    private static String _formatTimes(SPSiteQuality.TimingWindow tw) {
        if (tw.getDuration() == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) return null;
        switch (tw.getRepeat()) {
            case SPSiteQuality.TimingWindow.REPEAT_FOREVER:
                return null;
            case SPSiteQuality.TimingWindow.REPEAT_NEVER:
                return null;
            default:
                return String.valueOf(tw.getRepeat());
        }
    }

    private static String _formatWindow(SPSiteQuality.TimingWindow tw) {
        long time = tw.getStart();
        return dateFormat.format(new Date(time));
    }


    private TimingWindow _makeTimingWindow(SPSiteQuality.TimingWindow spTimingWindow) {
        TimingWindow tw = new TimingWindow();
        tw.setDuration(_formatDuration(spTimingWindow));
        tw.setTime(_formatWindow(spTimingWindow));
        if (spTimingWindow.getRepeat() != SPSiteQuality.TimingWindow.REPEAT_NEVER) {
            TimingWindowRepeats repeats = new TimingWindowRepeats();
            repeats.setPeriod(_formatPeriod(spTimingWindow));
            repeats.setTimes(_formatTimes(spTimingWindow));
            tw.setRepeats(repeats);
        }
        return tw;
    }


    @Override
    public void mergeResults(Collection<IDBFunctor> functorCollection) {
        QueryResult result = new QueryResult();
        for (IDBFunctor f : functorCollection) {
            result.getProgramsNode().getPrograms().addAll(((LchQueryFunctor) f).getResult().getProgramsNode().getPrograms());
        }
        _queryResult = result;
    }

    public QueryResult getResult() {
        return _queryResult;
    }
}
