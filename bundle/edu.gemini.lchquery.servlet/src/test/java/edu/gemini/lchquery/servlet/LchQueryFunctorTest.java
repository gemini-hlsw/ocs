package edu.gemini.lchquery.servlet;

import edu.gemini.odb.browser.Observation;
import edu.gemini.odb.browser.Program;
import edu.gemini.odb.browser.QueryResult;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.memImpl.MemFactory;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.altair.InstAltairCB;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.InstGMOSCB;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideEnvironment;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.OptionsListImpl;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetObsCompCB;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.rmi.RemoteException;
import java.security.Principal;
import java.util.*;


/**
 * Tests the LchQueryFunctor class.
 */
public class LchQueryFunctorTest {

    private final SPNodeKey PROG_KEY = new SPNodeKey("6d026d22-d642-4f50-8f99-ab666e286d47");
    private final SPNodeKey KEY = new SPNodeKey("2f7a8b79-1d10-416a-baf3-9b982f77da53");
    private MemFactory fact;
    private List<ISPObsComponent> obscomps;
    private ISPObservation obs;
    private List<ISPObservation> observations;
//    private ISPProgram prog;

    private String programSemester;
    private String programTitle;
    private String programReference;
    private String programActive;
    private String programCompleted;
    private String programNotifyPi;
    private String programRollover;
    private String observationTooStatus;
    private String observationName;
    private String observationStatus;
    private String observationInstrument;
    private String observationAo;
    private String observationClass;

    private ISPObsComponent createObsComp(ISPProgram prog, AbstractDataObject dataObj) throws RemoteException, SPUnknownIDException {
        ISPObsComponent obscomp = fact.doCreateObsComponent(prog, dataObj.getType(), KEY);
        obscomp.setDataObject(dataObj);
        return obscomp;
    }
     private ISPSeqComponent createSeqComp(ISPProgram prog, AbstractDataObject dataObj) throws RemoteException, SPUnknownIDException {
        ISPSeqComponent obscomp = fact.doCreateSeqComponent(prog, dataObj.getType(), KEY);
        obscomp.setDataObject(dataObj);
        return obscomp;
    }
    protected ISPProgram createBasicProgram(String id)  throws  RemoteException, SPBadIDException, SPException {
        SPProgram spProg = new SPProgram();
        ISPProgram prog = fact.createProgram(PROG_KEY, SPProgramID.toProgramID(id));
        spProg.setTitle(id);
        spProg.setActive(SPProgram.Active.YES);
        spProg.setCompleted(false);
        prog.setDataObject(spProg);
        Too.set(prog, TooType.standard);

        obs = fact.createObservation(prog, KEY);
        SPObservation spObs = new SPObservation();
        spObs.setTitle("Test Observation");
        spObs.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE);
        obs.setDataObject(spObs);
        obs.setSeqComponent(fact.createSeqComponent(prog, SPComponentType.OBSERVER_OBSERVE, KEY));
        observations = new ArrayList<ISPObservation>();
        observations.add(obs);

        prog.setObservations(observations);

        obscomps = new ArrayList<ISPObsComponent>();

        obs.setObsComponents(obscomps);

        return prog;
    }

    /**
     * Add an Altair component of the specified type
     *
     * @param mode  mode (LGS or NGS)
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addAltair(ISPProgram prog, AltairParams.Mode mode) throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        InstAltair altair = new InstAltair();
        altair.setMode(mode);
        ISPObsComponent altairObsComp = createObsComp(prog, altair);
        obscomps.add(altairObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new InstAltairCB(altairObsComp));
        obs.setObsComponents(obscomps);
    }

    /**
     * Add a GMOS north component
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     * @param xBin
     * @param yBin
     */
    protected void addGmosNorth(ISPProgram prog, GmosCommonType.Binning xBin, GmosCommonType.Binning yBin) throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        InstGmosNorth gmos = new InstGmosNorth();
        gmos.setCcdXBinning(xBin);
        gmos.setCcdYBinning(yBin);
        ISPObsComponent gmosObsComp = createObsComp(prog, gmos);
        obscomps.add(gmosObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new InstGMOSCB(gmosObsComp));
        obs.setObsComponents(obscomps);
        //obs.setSeqComponent(createSeqComp(new SeqConfigGmosNorth()));
    }

    /**
     * Add a TargetObsComp with an AOWFS and a P1WFS target
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addTargetObsCompAOP1(ISPProgram prog) throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        TargetObsComp target = new TargetObsComp();
        SPTarget sptarget = new SPTarget();
        sptarget.setName("name");
        Set<GuideProbe> guideProbes = new HashSet<GuideProbe>();
        guideProbes.add(AltairAowfsGuider.instance);
        guideProbes.add(PwfsGuideProbe.pwfs1);

        GuideEnvironment guide = GuideEnvironment.create(
                guideProbes,
                OptionsListImpl.<GuideGroup>create(
                        GuideGroup.create(
                                "group",
                                GuideProbeTargets.create(
                                        AltairAowfsGuider.instance,
                                        sptarget),
                                GuideProbeTargets.create(
                                        PwfsGuideProbe.pwfs1,
                                        sptarget))));
        TargetEnvironment env = TargetEnvironment.create(sptarget, guide, ImCollections.EMPTY_LIST);

        target.setTargetEnvironment(env);
        ISPObsComponent targetObsComp = createObsComp(prog, target);
        obscomps.add(targetObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new TargetObsCompCB(targetObsComp));
        obs.setObsComponents(obscomps);
    }

    /**
     * Add a TargetObsComp with an AOWFS target
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addTargetObsCompAO(ISPProgram prog) throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        TargetObsComp target = new TargetObsComp();
        SPTarget sptarget = new SPTarget();
        sptarget.setName("name");
        Set<GuideProbe> guideProbes = new HashSet<GuideProbe>();
        guideProbes.add(AltairAowfsGuider.instance);

        GuideEnvironment guide = GuideEnvironment.create(
                guideProbes,
                OptionsListImpl.<GuideGroup>create(
                        GuideGroup.create(
                                "group",
                                GuideProbeTargets.create(
                                        AltairAowfsGuider.instance,
                                        sptarget))));
        TargetEnvironment env = TargetEnvironment.create(sptarget, guide, ImCollections.EMPTY_LIST);

        target.setTargetEnvironment(env);
        ISPObsComponent targetObsComp = createObsComp(prog, target);
        obscomps.add(targetObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new TargetObsCompCB(targetObsComp));
        obs.setObsComponents(obscomps);
    }

    /**
     * Add a TargetObsComp with no targets
     *
     * @throws edu.gemini.pot.sp.SPUnknownIDException
     * @throws java.rmi.RemoteException
     * @throws edu.gemini.pot.sp.SPTreeStateException
     * @throws edu.gemini.pot.sp.SPNodeNotLocalException
     */
    protected void addTargetObsCompEmpty(ISPProgram prog) throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        TargetObsComp target = new TargetObsComp();

        ISPObsComponent targetObsComp = createObsComp(prog, target);
        obscomps.add(targetObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new TargetObsCompCB(targetObsComp));
        obs.setObsComponents(obscomps);


    }

    private boolean matches(ISPProgram prog) {
        return matches(prog, 1, 1);
    }

    private boolean matches(ISPProgram prog, int expectedPrograms, int expectedObservations) {
        LchQueryFunctor functor = new LchQueryFunctor(LchQueryFunctor.QueryType.OBSERVATIONS,
                programSemester, programTitle, programReference,
                programActive, programCompleted, programNotifyPi, programRollover,
                observationTooStatus, observationName, observationStatus, observationInstrument,
                observationAo, observationClass);
        functor.execute(null, prog, Collections.<Principal>emptySet());
        QueryResult result = functor.getResult();
        List<Program> programs = result.getProgramsNode().getPrograms();
        if (programs.size() != expectedPrograms) return false;
        List<Observation> observations = programs.get(0).getObservationsNode().getObservations();
        if (observations.size() != expectedObservations) return false;

        // XXX Check content of QueryResult..

        return true;
    }

    @Before
    public void setUp() throws RemoteException, SPBadIDException, SPException {
        fact = new MemFactory(UUID.randomUUID());

        SPSiteQuality sq = new SPSiteQuality();
        sq.setImageQuality(SPSiteQuality.ImageQuality.PERCENT_20);

        programSemester = null;
        programTitle = null;
        programReference = null;
        programActive = null;
        programCompleted = null;
        programNotifyPi = null;
        programRollover = null;

        observationTooStatus = null;
        observationName = null;
        observationStatus = null;
        observationInstrument = null;
        observationAo = null;
        observationClass = null;
    }

    @Test
    public void testQuery() throws RemoteException, SPException, SPBadIDException {

        ISPProgram prog = createBasicProgram("GS-2013A-DD-13");


        addGmosNorth(prog, GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);

        programSemester = "2013A";
        assertTrue(matches(prog));

        programSemester = "2012B";
        assertFalse(matches(prog));

        programSemester = "2013B";
        assertFalse(matches(prog));

        programSemester = "2013*";
        assertTrue(matches(prog));


        programTitle = "GS-2013A-DD-13";
        assertTrue(matches(prog));

        programTitle = "GN*";
        assertFalse(matches(prog));

        programTitle = "GS%";
        assertTrue(matches(prog));


        programReference = "GS-2013A-DD-13";
        assertTrue(matches(prog));

        programReference = "GN*";
        assertFalse(matches(prog));

        programReference = "GS%";
        assertTrue(matches(prog));


        programActive = "no";
        assertFalse(matches(prog));

        programActive = "yes";
        assertTrue(matches(prog));

        programActive = "Yes";
        assertTrue(matches(prog));

        programActive = "y*";
        assertTrue(matches(prog));


        programCompleted = "yes";
        assertFalse(matches(prog));

        programCompleted = "Yes";
        assertFalse(matches(prog));

        programCompleted = "y*";
        assertFalse(matches(prog));

        programCompleted = "no";
        assertTrue(matches(prog));


        observationTooStatus = "none";
        assertFalse(matches(prog));

        observationTooStatus = "standard";
        assertTrue(matches(prog));

        observationTooStatus = "s*";
        assertTrue(matches(prog));


        observationName = "observation";
        assertFalse(matches(prog));

        observationName = "t*";
        assertTrue(matches(prog));

        observationName = "Test Observation";
        assertTrue(matches(prog));


        observationStatus = "r*";
        assertTrue(matches(prog));

        observationStatus = "Ready";
        assertTrue(matches(prog));

        observationInstrument = "gnirs";
        assertFalse(matches(prog));

        observationInstrument = "gmos*";
        assertTrue(matches(prog));

        observationInstrument = "gmos-n";
        assertTrue(matches(prog));

        observationAo = "none";
        assertTrue(matches(prog));

        observationClass = "science";
        assertTrue(matches(prog));

        addAltair(prog, AltairParams.Mode.LGS);
        assertFalse(matches(prog));

        observationAo = "Altair + LGS";
        assertTrue(matches(prog));
    }


    @Test
    public void testLargeProgramQuery() throws RemoteException, SPException, SPBadIDException {

        ISPProgram prog = createBasicProgram("GS-2013A-LP-13");

        addGmosNorth(prog, GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);

        programSemester = "2012B";
        assertFalse(matches(prog));

        programSemester = "2013A";
        assertTrue(matches(prog));

        programSemester = "2013B";
        assertFalse(matches(prog));

        // LPs must not be included if not matching the given pattern
        programSemester = "2012A|2012B";
        assertFalse(matches(prog));

        programSemester = "2012*";
        assertFalse(matches(prog));

        // LPs must also be included if matching the given pattern
        programSemester = "2013A|2013B";
        assertTrue(matches(prog));

        programSemester = "2013*|2014A";
        assertTrue(matches(prog));
    }

    @Test
    public void testArbitraryProgramQuery() throws RemoteException, SPException, SPBadIDException {

        ISPProgram prog = createBasicProgram("My-Test-Program");

        addGmosNorth(prog, GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);

        // don't expect arbitrary program to be picked up when looking for a specific semester
        programSemester = "2012B";
        assertFalse(matches(prog));

        // nor when looking for a semester pattern
        programSemester = "2012*";
        assertFalse(matches(prog));

    }

}