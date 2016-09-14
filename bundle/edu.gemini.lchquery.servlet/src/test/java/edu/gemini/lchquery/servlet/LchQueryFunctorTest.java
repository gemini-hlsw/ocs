package edu.gemini.lchquery.servlet;

import edu.gemini.odb.browser.Observation;
import edu.gemini.odb.browser.Program;
import edu.gemini.odb.browser.QueryResult;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.sp.memImpl.MemFactory;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.altair.InstAltairCB;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.InstGMOSCB;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.ObsPhase2Status;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.rmi.RemoteException;
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


    private ISPObsComponent createObsComp(final ISPProgram prog, final AbstractDataObject dataObj) throws RemoteException, SPUnknownIDException {
        final ISPObsComponent obscomp = fact.doCreateObsComponent(prog, dataObj.getType(), KEY);
        obscomp.setDataObject(dataObj);
        return obscomp;
    }

    private ISPProgram createBasicProgram(final String id)  throws  RemoteException, SPBadIDException, SPException {
        final SPProgram spProg = new SPProgram();
        final ISPProgram prog = fact.createProgram(PROG_KEY, SPProgramID.toProgramID(id));
        spProg.setTitle(id);
        spProg.setActive(SPProgram.Active.YES);
        spProg.setCompleted(false);
        prog.setDataObject(spProg);
        Too.set(prog, TooType.standard);

        obs = fact.createObservation(prog, KEY);
        final SPObservation spObs = new SPObservation();
        spObs.setTitle("Test Observation");
        spObs.setPhase2Status(ObsPhase2Status.PHASE_2_COMPLETE);
        obs.setDataObject(spObs);
        obs.setSeqComponent(fact.createSeqComponent(prog, SPComponentType.OBSERVER_OBSERVE, KEY));

        final List<ISPObservation> observations = new ArrayList<>();
        observations.add(obs);
        prog.setObservations(observations);

        obscomps = new ArrayList<>();
        obs.setObsComponents(obscomps);

        return prog;
    }

    /**
     * Add an Altair component of the specified type
     */
    private void addAltair(final ISPProgram prog, final AltairParams.Mode mode)
            throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        final InstAltair altair = new InstAltair();
        altair.setMode(mode);
        final ISPObsComponent altairObsComp = createObsComp(prog, altair);
        obscomps.add(altairObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new InstAltairCB(altairObsComp));
        obs.setObsComponents(obscomps);
    }

    /**
     * Add a GMOS north component
     */
    private void addGmosNorth(final ISPProgram prog, final GmosCommonType.Binning xBin, final GmosCommonType.Binning yBin)
            throws SPUnknownIDException, RemoteException, SPTreeStateException, SPNodeNotLocalException {
        final InstGmosNorth gmos = new InstGmosNorth();
        gmos.setCcdXBinning(xBin);
        gmos.setCcdYBinning(yBin);
        final ISPObsComponent gmosObsComp = createObsComp(prog, gmos);
        obscomps.add(gmosObsComp);
        obs.putClientData(IConfigBuilder.USER_OBJ_KEY, new InstGMOSCB(gmosObsComp));
        obs.setObsComponents(obscomps);
    }

    private boolean matches(final ISPProgram prog) {
        return matches(prog, 1, 1);
    }

    private boolean matches(final ISPProgram prog, final int expectedPrograms, final int expectedObservations) {
        final LchQueryFunctor functor = new LchQueryFunctor(LchQueryFunctorTestFuncs.obsQuery(),
                LchQueryFunctorTestFuncs.progParamsToList(
                        programSemester, programTitle, programReference,
                        programActive, programCompleted, programNotifyPi, programRollover
                ),
                LchQueryFunctorTestFuncs.obsParamsToList(
                        observationTooStatus, observationName, observationStatus,
                        observationInstrument, observationAo, observationClass
                ));
        functor.execute(null, prog, Collections.emptySet());
        final QueryResult result = functor.queryResult();
        final List<Program> programs = result.getProgramsNode().getPrograms();
        if (programs.size() != expectedPrograms) return false;
        final List<Observation> observations = programs.get(0).getObservationsNode().getObservations();
        return observations.size() == expectedObservations;
    }

    @Before
    public void setUp() throws RemoteException, SPBadIDException, SPException {
        fact = new MemFactory(UUID.randomUUID());

        final SPSiteQuality sq = new SPSiteQuality();
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

        final ISPProgram prog = createBasicProgram("GS-2013A-DD-13");

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

        final ISPProgram prog = createBasicProgram("GS-2013A-LP-13");

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

        final ISPProgram prog = createBasicProgram("My-Test-Program");

        addGmosNorth(prog, GmosCommonType.Binning.DEFAULT, GmosCommonType.Binning.DEFAULT);

        // don't expect arbitrary program to be picked up when looking for a specific semester
        programSemester = "2012B";
        assertFalse(matches(prog));

        // nor when looking for a semester pattern
        programSemester = "2012*";
        assertFalse(matches(prog));

    }
}
