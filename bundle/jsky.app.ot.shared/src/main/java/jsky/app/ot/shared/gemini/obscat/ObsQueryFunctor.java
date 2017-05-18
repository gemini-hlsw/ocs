package jsky.app.ot.shared.gemini.obscat;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.ao.AOTreeUtil;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.dataset.DataflowStatus;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.*;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;
import edu.gemini.spModel.time.TimeAmountFormatter;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.util.SPTreeUtil;
import edu.gemini.util.security.permission.ProgramPermission;
import edu.gemini.util.security.policy.ImplicitPolicyForJava;
import jsky.catalog.SearchCondition;
import jsky.coords.DMS;
import jsky.coords.HMS;

import java.security.AccessControlException;
import java.security.Principal;
import java.util.*;

/**
 * An <code>edu.gemini.pot.spdb.IDBQueryFunctor</code>
 * implementation that can be used by clients to query the science
 * program database for observations matching given constraints.
 *
 * @author Allan Brighton
 */
public class ObsQueryFunctor extends DBAbstractQueryFunctor {

    // Holds the result of the query, in table format (the result is a
    // vector of rows, which are vectors of columns corresponding to the
    // columns defined in the ObsCatalog class).
    private Vector<Vector<Object>> _result;

    // Holds additional information (progID, ObsID) corresponding to each row in the result
    private Vector<Vector<Object>> _ids;

    // local copies of the constructor arguments

    // An array of search conditions (field1=value1,field2=value2...)
    private final SearchCondition[] _sc;

    // The names of the selected instruments, or null if none were selected
    private final String[] _instruments;

    // An array, for each selected instrument, of instrument specific search conditions
    private final SearchCondition[][] _instSc;


    /**
     * Initialize a functor to query the science program database for all
     * observations matching the given search conditions.
     * If more than one search condition is specified, they are ANDed together.
     *
     * @param sc          optional array of search conditions (field1=value1,field2=value2...)
     * @param instruments array of selected instrument names
     * @param instSc      array, for each selected instrument, of instrument specific search conditions
     */
    public ObsQueryFunctor(SearchCondition[] sc, String[] instruments, SearchCondition[][] instSc) {
        _sc = sc;
        _instruments = instruments;
        _instSc = instSc;
    }


    /**
     * Called once before the first call to <code>isDone()</code> or
     * <code>execute</code>.  Provides an opportunity for the functor
     * to initialize itself on the server.
     */
    public void init() {
        if (_result == null) _result = new Vector<>();
        if (_ids == null) _ids = new Vector<>();
    }


    /**
     * Called once when all nodes have been operated upon.
     */
    public void finished() {
    }


    /**
     * Return the result of the query as an array of ObsInfo objects.
     */
    public Vector<Vector<Object>> getResult() {
        return _result;
    }

    /**
     * Return a vector of (progId, obsId) corresponding to the displayed result.
     */
    public Vector<Vector<Object>> getIds() {
        return _ids;
    }


    /**
     * Called once per program by the <code>{@link IDBQueryRunner}</code>
     * implementation.
     */
    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        final ISPProgram prog = (ISPProgram) node;
        try {

            if (prog.getProgramID() != null)
                ImplicitPolicyForJava.checkPermission(database, principals, new ProgramPermission.Read(prog.getProgramID()));

            // check for program related constraints, such as AFFILIATES and PI Last Name
            if (_match(prog)) {
                // check each observation and add a row to the result vector for any matches
                for (ISPObservation o : prog.getAllObservations()) {
                    _match(prog, o);
                }
            }
        } catch (AccessControlException ace) {
            // Ok; this just means we can't see this program
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Return true if the given science program matches the conditions.
     * If no conditions apply at the program level, true is returned.
     * False is only returned if a condition is specified that does not match.
     */
    private boolean _match(ISPProgram prog) {
        if (_sc == null)
            return true;

        // We should be running on the server at this point, so calling getDataObject() should
        // not involve any remote calls...
        final SPProgram spProg = (SPProgram) prog.getDataObject();
        final SPProgram.PIInfo piInfo = spProg.getPIInfo();

        final int n = _sc.length;
        if (n > 0) {
            for (SearchCondition a_sc : _sc) {
                final String name = a_sc.getName();
                if (name.equals(ObsCatalogInfo.PI_LAST_NAME)) {
                    if (!a_sc.isTrueFor(piInfo.getLastName())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.EMAIL)) {
                    // check against all three emails
                    final boolean b1 = a_sc.isTrueFor(piInfo.getEmail());
                    final boolean b2 = a_sc.isTrueFor(spProg.getPrimaryContactEmail());
                    final boolean b3 = a_sc.isTrueFor(spProg.getContactPerson());
                    if (!(b1 || b2 || b3)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.PROG_REF)) {
                    final SPProgramID progId = prog.getProgramID();
                    if (progId == null || !a_sc.isTrueFor(progId.stringValue())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.ACTIVE)) {
                    if (!a_sc.isTrueFor(spProg.getActive().name())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.COMPLETED)) {
                    final YesNoType t = spProg.isCompleted() ? YesNoType.YES : YesNoType.NO;
                    if (!a_sc.isTrueFor(t.name())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.THESIS)) {
                    final YesNoType t = spProg.isThesis() ? YesNoType.YES : YesNoType.NO;
                    if (!a_sc.isTrueFor(t.name())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.ROLLOVER)) {
                    final YesNoType t = spProg.getRolloverStatus() ? YesNoType.YES : YesNoType.NO;
                    if (!a_sc.isTrueFor(t.name())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.SEMESTER)) {
                    final String s = _getSemester(prog);
                    if (s == null || !a_sc.isTrueFor(s)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.PARTNER_COUNTRY)) {
                    Affiliate a = piInfo.getAffiliate();
                    if (a == null || !a_sc.isTrueFor(a.name())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.QUEUE_BAND)) {
                    if (!a_sc.isTrueFor(spProg.getQueueBand())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // Add any matching observations to the query results.
    private void _match(ISPProgram prog, ISPObservation o) {
        final List<ISPDataObject> instruments = _getInstruments(o);
        final Iterator<ISPDataObject> it = instruments.iterator();
        final PioFactory factory = new PioXmlFactory();
        while (it.hasNext()) {
            final ISPDataObject inst = it.next();
            final List<InstConfigInfo> instConfigInfoList = ObsCatalogInfo.getInstConfigInfoList(inst.getType().readableStr);
            final ParamSet instParamSet = inst.getParamSet(factory);
            if (_match(o, inst, instConfigInfoList, instParamSet)) {
                final ISPDataObject mainInst = instruments.get(0);
                _result.add(_makeRow(prog, o, mainInst, instConfigInfoList, instParamSet));
                _ids.add(_makeIdRow(prog, o));
                break;
            }
        }
    }

    /**
     * Return true if the given observation and related subnodes match the conditions.
     *
     * @param o                  the observation node
     * @param inst               the instrument for the observation, or null
     * @param instConfigInfoList maps instrument specific column names to map keys
     * @param instParamSet       describes the instrument settings
     */
    private boolean _match(ISPObservation o, ISPDataObject inst, List<InstConfigInfo> instConfigInfoList,
                           ParamSet instParamSet) {
        final SPObservation obs = (SPObservation) o.getDataObject();
        final SPSiteQuality siteQuality = _getSiteQuality(o);

        // Describes the query region, if specified
        String minRA = null, maxRA = null, minDec = null, maxDec = null;

        // check the search conditions
        if (_sc != null && _sc.length > 0) {
            for (SearchCondition a_sc : _sc) {
                final String name = a_sc.getName();

                if (name.equals(ObsCatalogInfo.MIN_RA)) {
                    minRA = a_sc.getValueAsString();
                } else if (name.equals(ObsCatalogInfo.MAX_RA)) {
                    maxRA = a_sc.getValueAsString();
                } else if (name.equals(ObsCatalogInfo.MIN_DEC)) {
                    minDec = a_sc.getValueAsString();
                } else if (name.equals(ObsCatalogInfo.MAX_DEC)) {
                    maxDec = a_sc.getValueAsString();
                } else if (name.equals(ObsCatalogInfo.TARGET_NAME)) {
                    final String targetName = _getTargetName(o);
                    if (targetName == null || !a_sc.isTrueFor(targetName))
                        return false;
                } else if (name.equals(ObsCatalogInfo.SKY_BACKGROUND)) {
                    String sb = null;
                    if (siteQuality != null) {
                        sb = siteQuality.getSkyBackground().name();
                    }
                    if (!_matchSiteQuality(a_sc, sb)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.WATER_VAPOR)) {
                    String wv = null;
                    if (siteQuality != null) {
                        wv = siteQuality.getWaterVapor().name();
                    }
                    if (!_matchSiteQuality(a_sc, wv)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.CLOUD_COVER)) {
                    String cc = null;
                    if (siteQuality != null) {
                        cc = siteQuality.getCloudCover().name();
                    }
                    if (!_matchSiteQuality(a_sc, cc)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.IMAGE_QUALITY)) {
                    String iq = null;
                    if (siteQuality != null) {
                        iq = siteQuality.getImageQuality().name();
                    }
                    if (!_matchSiteQuality(a_sc, iq)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.INSTRUMENT)) {
                    String instName = null;
                    if (inst != null) {
                        instName = inst.getType().readableStr;
                    }
                    if (!a_sc.isTrueFor(instName)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.OBS_STATUS)) {
                    if (!a_sc.isTrueFor(ObservationStatus.computeFor(o).name())) {
                        return false;
                    }

                } else if (name.equals(ObsCatalogInfo.OBS_QA)) {
                    final ObsQaState qa = ObsQaStateService.getObsQaState(o);
                    if (!a_sc.isTrueFor(qa.name())) return false;

                    // DMAN TODO: why was this commented out?  Do we not need to
                    // match on dataflow step?  Why is it in the menu of options?
//                } else if (name.equals(ObsCatalogInfo.DATAFLOW_STEP)) {
//                    final DatasetDisposition dispo;
//                    dispo = DatasetDispositionService.lookupDatasetDisposition(o);
//                    String desc = "No Data";
//                    if (dispo != null) desc = dispo.getDisplayString();
//                    if (!a_sc.isTrueFor(desc)) return false;

                } else if (name.equals(ObsCatalogInfo.PRIORITY)) {
                    if (!a_sc.isTrueFor(obs.getPriority().displayValue())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.AO)) {
                    if (!a_sc.isTrueFor(_getAO(o).name())) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.OBS_CLASS)) {
                    final ObsClass obsClass = ObsClassService.lookupObsClass(o);
                    String desc = null;
                    if (obsClass != null) {
                        desc = obsClass.name();
                    }
                    if (!a_sc.isTrueFor(desc)) {
                        return false;
                    }
                } else if (name.equals(ObsCatalogInfo.TOO)) {
                    final TooType too = Too.get(o);
                    if (!a_sc.isTrueFor(too.name())) {
                        return false;
                    }
                }
            }
        }
        // check the instrument specific search conditions
        if (inst != null && _instruments != null && _instSc != null && _instSc.length > 0) {
            final int instIndex = _getInstIndex(inst.getType().readableStr);
            if (instIndex != -1) {
                final SearchCondition[] sc = _instSc[instIndex];
                for (SearchCondition aSc : sc) {
                    final String name = aSc.getName();
                    if (!_matchInstOptions(name, aSc, instConfigInfoList, instParamSet)) {
                        return false;
                    }
                }
            }
        }

        try {
            return _regionMatch(o, minRA, maxRA, minDec, maxDec);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Return a description of the observations AO type.
    private AOConstants.AO _getAO(ISPObservation o) {
        final ISPObsComponent obsComp = AOTreeUtil.findAOSystem(o);
        if (obsComp != null) {
            final SPComponentType type = obsComp.getType();
            if (type.equals(InstAltair.SP_TYPE)) {
                final InstAltair inst = (InstAltair) obsComp.getDataObject();
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
     * Return the index of the given instrument name in the _instruments array, or -1 if not found
     */
    private int _getInstIndex(String inst) {
        for (int i = 0; i < _instruments.length; i++)
            if (_instruments[i].equals(inst))
                return i;
        return -1;
    }


    /**
     * Return the semester for the given program
     */
    private String _getSemester(ISPProgram prog) {
        try {
            final SPProgramID progId = prog.getProgramID();
            if (progId != null) {
                final String s = progId.stringValue();
                final int i = s.indexOf('-');
                if (i != -1 && s.charAt(i + 1) == '2') {
                    return s.substring(i + 1, i + 6);
                }
            }
        } catch (Exception e) {
            // Parse problem, presumably
        }
        return null;
    }


    /**
     * Return the target name for the given observation, or null if none is defined.
     */
    private String _getTargetName(ISPObservation o) {
        try {
            final ISPObsComponent targetEnvNode = SPTreeUtil.findTargetEnvNode(o);
            if (targetEnvNode == null)
                return null;
            final TargetObsComp targetEnv = (TargetObsComp) targetEnvNode.getDataObject();
            if (targetEnv == null)
                return null;
            return targetEnv.getAsterism().name();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Return true if any of the requested values match the given SiteQuality value.
     */
    private static boolean _matchSiteQuality(SearchCondition sc, String current) {
        return sc.isTrueFor(current);
    }

    /**
     * Return true if one of the instrument specific options match for the given
     * parameter name, or if no instrument parameters were found.
     *
     * @param paramName          the name of the parameter being checked
     * @param sc                 the search condition object used to test for a match
     * @param instConfigInfoList a list of instrument config info
     * @param instParamSet       describes the instrument settings
     */
    private boolean _matchInstOptions(String paramName, SearchCondition sc,
                                      List<InstConfigInfo> instConfigInfoList, ParamSet instParamSet) {
        for (Object anInstConfigInfoList : instConfigInfoList) {
            final InstConfigInfo info = (InstConfigInfo) anInstConfigInfoList;
            if (paramName.equals(info.getName())) {
                final String value = Pio.getValue(instParamSet, info.getPropertyName());

                if ((value != null) || info.isOptional()) {
                    if (!sc.isTrueFor(value)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    /**
     * Return a list containing the instrument and Altair data objects for the
     * given observation.
     */
    private List<ISPDataObject> _getInstruments(ISPObservation o) {
        final List<ISPDataObject> result = new ArrayList<>();
        final List<ISPObsComponent> l = SPTreeUtil.findInstruments(o);
        for (ISPObsComponent aL : l) {
            result.add(aL.getDataObject());
        }
        return result;
    }

    /**
     * Return the site quality data object for the given observation, or null if not defined
     */
    private SPSiteQuality _getSiteQuality(ISPObservation o) {
        final ISPObsComponent siteQualityObsComp = SPTreeUtil.findObsComponent(o, SPSiteQuality.SP_TYPE);
        if (siteQualityObsComp != null) {
            return (SPSiteQuality) siteQualityObsComp.getDataObject();
        }
        return null;
    }


    /**
     * Return true if the given query region contains the base position of the observation, or
     * if no region was specified.
     */
    private boolean _regionMatch(ISPObservation o, String minRA, String maxRA, String minDec, String maxDec) {

        if (minRA == null && maxRA == null && minDec == null && maxDec == null) {
            return true; // no region was specified
        }

        // get the observation's base position in deg J2000
        final ISPObsComponent targetObsComp = SPTreeUtil.findObsComponent(o, TargetObsComp.SP_TYPE);
        if (targetObsComp == null)
            return false;
        final TargetObsComp targetEnv = (TargetObsComp) targetObsComp.getDataObject();
        final Asterism asterism = targetEnv.getAsterism();

        final Option<Long> when = ((SPObservation) o.getDataObject()).getSchedulingBlockStart();
        final Option<Double> raOp  = asterism.getRaDegrees(when).map(x -> x / 15.);
        final Option<Double> decOp = asterism.getDecDegrees(when);

        final double ra0  = (minRA != null)  ? new HMS(minRA, true).getVal() :   0.;
        final double ra1  = (maxRA != null)  ? new HMS(maxRA, true).getVal() :  24.;
        final double dec0 = (minDec != null) ? new DMS(minDec).getVal()      : -90.;
        final double dec1 = (maxDec != null) ? new DMS(maxDec).getVal()      :  90.;

        return
            raOp.flatMap(ra ->
            decOp.map(dec ->
            _regionMatchAux(ra, dec, ra0, ra1, dec0, dec1)))
            .getOrElse(false); // region does not match if there are no coordinates

    }

    private boolean _regionMatchAux(double ra, double dec, double ra0, double ra1, double dec0, double dec1) {
        if (dec < dec0 || dec > dec1) {
            return false;
        }

        if (ra0 <= ra1) {
            // simple range
            if (ra < ra0 || ra > ra1)
                return false;
        } else {
            // range wraps around past 24h, for example: (23h to 2h)
            if (ra > ra1 && ra < ra0)
                return false;
        }

        return true;
    }


    /**
     * Make and return a result table row for the given observation, in the given science program.
     *
     * @param prog               the program node
     * @param o                  the observation node
     * @param inst               the instrument for the observation, or null
     * @param instConfigInfoList maps instrument specific column names to map keys
     * @param instParamSet       describes the instrument settings
     */
    private Vector<Object> _makeRow(ISPProgram prog, ISPObservation o, ISPDataObject inst,
                            List<InstConfigInfo> instConfigInfoList, ParamSet instParamSet) {

        final SPProgram spProg = (SPProgram) prog.getDataObject();
        final SPObservation obs = (SPObservation) o.getDataObject();
        final SPProgram.PIInfo piInfo = spProg.getPIInfo();
        final String piLastName = piInfo.getLastName();
        final Affiliate country = piInfo.getAffiliate();
        final SPProgramID geminiRef = prog.getProgramID();
        final SPObservationID obsId = o.getObservationID();
        final String queueBand = spProg.getQueueBand();
        final ObservationStatus status = ObservationStatus.computeFor(o);
        final String priority = obs.getPriority().displayValue();

        String skyBackground = null;
        String waterVapor = null;
        String cloudCover = null;
        String imageQuality = null;
        String elevationConstraint = null;
        String timingConstraint = null;

        final ISPObsComponent siteQualityObsComp = SPTreeUtil.findObsComponent(o, SPSiteQuality.SP_TYPE);
        if (siteQualityObsComp != null) {
            final SPSiteQuality siteQuality = (SPSiteQuality) siteQualityObsComp.getDataObject();
            skyBackground = siteQuality.getSkyBackground().displayValue();
            waterVapor = siteQuality.getWaterVapor().displayValue();
            cloudCover = siteQuality.getCloudCover().displayValue();
            imageQuality = siteQuality.getImageQuality().displayValue();

            // RCN: this should be a structured type instead of three members. Sorry.
            elevationConstraint =
                    String.format("{%s %1.2f %1.2f}",
                            siteQuality.getElevationConstraintType().displayValue(),
                            siteQuality.getElevationConstraintMin(),
                            siteQuality.getElevationConstraintMax());

            timingConstraint = siteQuality.getTimingWindows().toString();

        }

        String instrument = null;
        if (inst != null) {
            instrument = inst.getType().readableStr;
        }

        Double ra = null, dec = null;
        final ISPObsComponent targetObsComp = SPTreeUtil.findObsComponent(o, TargetObsComp.SP_TYPE);
        if (targetObsComp != null) {
            final TargetObsComp targetEnv = (TargetObsComp) targetObsComp.getDataObject();
            final Asterism asterism = targetEnv.getAsterism();
            final Option<Long> when = obs.getSchedulingBlockStart();
            ra = asterism.getRaDegrees(when).getOrNull();
            dec = asterism.getDecDegrees(when).getOrNull();
        }

        // Figure out the planned time for all observations.
        String plannedExecTimeStr = null;
        String plannedPiTimeStr = null;
        try {
            final PlannedTimeSummary pt = PlannedTimeSummaryService.getTotalTime(o);
            plannedExecTimeStr = TimeAmountFormatter.getHMSFormat(pt.getExecTime());
            plannedPiTimeStr = TimeAmountFormatter.getHMSFormat(pt.getPiTime());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get the ObsClass for the observation, which should default to
        // SCIENCE if there is no other alternative (i.e., not be null).
        final ObsClass obsClass = ObsClassService.lookupObsClass(o);
        final String obsClassName = obsClass.displayValue();

        // Figure out how time was spent on this observation.
        final ChargeClass cc = obsClass.getDefaultChargeClass();
        String chargedTimeStr = "";
        if (cc != ChargeClass.NONCHARGED) {
            final ObsTimes obsTimes = ObsTimesService.getCorrectedObsTimes(o);
            final ObsTimeCharges otc = obsTimes.getTimeCharges();
            final long chargeTime = otc.getTime(cc);
            chargedTimeStr = TimeAmountFormatter.getHMSFormat(chargeTime);
        }

        // Figure out the Obs QA State.
        final ObsQaState qaState = ObsQaStateService.getObsQaState(o);

        // Figure out the dataflow step
        final scala.Option<DataflowStatus> dispo = DatasetDispositionService.lookupDatasetDisposition(o);
        final String datasetDispoStr = dispo.isEmpty() ? "No Data" : dispo.get().description();

        // Figure out ready status
        final boolean ready = status.isScheduleable();

        // Get the group name and type
        String groupName = null;
        String groupType = null;
        final ISPNode parent = o.getParent();
        if (parent instanceof ISPGroup) {
            final SPGroup groupDataObj = (SPGroup) parent.getDataObject();
            if (groupDataObj != null) {
                final String tmp = groupDataObj.getGroup();
                if (tmp != null) groupName = tmp;
                switch (groupDataObj.getGroupType()) {
                    case TYPE_FOLDER:
                        groupType = "F";
                        break;
                    case TYPE_SCHEDULING:
                        groupType = "S";
                        break;
                }
            }
        }

        final String targetName = _getTargetName(o);

        final String[] ar = ObsCatalogInfo.getTableColumns();
        final int n = ar.length;
        final Vector<Object> row = new Vector<>(n);

        final Map<String,Object> map = new TreeMap<>();
        map.put(ObsCatalogInfo.TARGET_NAME, targetName);
        map.put(ObsCatalogInfo.RA, ra);
        map.put(ObsCatalogInfo.DEC, dec);
        map.put(ObsCatalogInfo.PROG_REF, geminiRef);
        map.put(ObsCatalogInfo.OBS_ID, obsId);
        map.put(ObsCatalogInfo.PI_LAST_NAME, piLastName);
        map.put(ObsCatalogInfo.PARTNER_COUNTRY, country);
        map.put(ObsCatalogInfo.OBS_STATUS, status.displayValue());
        map.put(ObsCatalogInfo.OBS_QA, qaState.displayValue());
        map.put(ObsCatalogInfo.DATAFLOW_STEP, datasetDispoStr);
        map.put(ObsCatalogInfo.PRIORITY, priority);
        map.put(ObsCatalogInfo.QUEUE_BAND, queueBand);
        map.put(ObsCatalogInfo.SKY_BACKGROUND, skyBackground);
        map.put(ObsCatalogInfo.WATER_VAPOR, waterVapor);
        map.put(ObsCatalogInfo.CLOUD_COVER, cloudCover);
        map.put(ObsCatalogInfo.IMAGE_QUALITY, imageQuality);
        map.put(ObsCatalogInfo.PLANNED_EXEC_TIME, plannedExecTimeStr);
        map.put(ObsCatalogInfo.PLANNED_PI_TIME, plannedPiTimeStr);
        map.put(ObsCatalogInfo.CHARGED_TIME, chargedTimeStr);
        map.put(ObsCatalogInfo.OBS_CLASS, obsClassName);
        map.put(ObsCatalogInfo.INSTRUMENT, instrument);
        map.put(ObsCatalogInfo.AO, _getAO(o).displayValue());
        map.put(ObsCatalogInfo.GROUP, groupName);
        map.put(ObsCatalogInfo.GROUP_TYPE, groupType);
        map.put(ObsCatalogInfo.ELEVATION_CONSTRAINT, elevationConstraint);
        map.put(ObsCatalogInfo.TIMING_CONSTRAINT, timingConstraint);
        map.put(ObsCatalogInfo.READY, ready);

        for (final String name : ar) {

            if (map.containsKey(name)) {
                row.add(map.get(name));
            }

            else {
                String s = null;
                if (inst != null && instConfigInfoList != null && instParamSet != null) {
                    // instrument specific columns
                    for (InstConfigInfo info: instConfigInfoList) {
                        if (name.equals(info.getName())) {
                            s = Pio.getValue(instParamSet, info.getPropertyName());
                            //if this is an enum type, and it has a display value, use it
                            @SuppressWarnings("rawtypes")
                            final Class c = info.getEnumType();
                            if ((c != null) && (s != null)) {
                                //noinspection unchecked
                                final Enum<?> x = Enum.valueOf(c, s);
                                if (x instanceof DisplayableSpType) {
                                    final DisplayableSpType d = (DisplayableSpType) x;
                                    s = d.displayValue();
                                }
                            }
                        }
                    }
                }

                // replace true/false with Yes/No
                if (s != null) {
                    if (s.equals("true"))
                        s = "Yes";
                    else if (s.equals("false"))
                        s = "No";
                }

                row.add(s);
            }
        }
        return row;
    }


    /**
     * Make and return a (ProgId, ObsId, group) row for the given observation, in the given science program.
     *
     * @param prog the program node
     * @param o    the observation node
     */
    private Vector<Object> _makeIdRow(ISPProgram prog, ISPObservation o) {
        final SPNodeKey obsId = o.getNodeKey();
        final SPNodeKey progId = prog.getNodeKey();

        final Vector<Object> row = new Vector<>(2);
        row.add(progId);
        row.add(obsId);

        Object groupIndicator = null;
        final ISPNode parent = o.getParent();
        if (parent instanceof ISPGroup) {
            groupIndicator = Boolean.TRUE; // just needs to be some value other than null
        }
        row.add(groupIndicator);

        return row;
    }

}


