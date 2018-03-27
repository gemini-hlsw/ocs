package edu.gemini.spModel.ext;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.seqcomp.SeqRepeat;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Test code for the node context code.
 *
 * The property HLPG_PROJECT_BASE must be set to the OCS installation dir.
 */
public class NodeContextTest {
    private IDBDatabaseService odb;

    private ISPProgram prog;
    private SPProgramID progId;

    private static final Set<Principal> user = Collections.emptySet();

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Before
    public void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();

        progId  = SPProgramID.toProgramID("GS-2009B-Q-1");
        prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);
    }

    @After
    public void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
    }

    @Test
    public void testEmptyObs() throws Exception {
        ISPObservation obs = odb.getFactory().createObservation(prog, Instrument.none, null);
        List<ISPObsComponent> emptyObsComponents = Collections.emptyList();
        obs.setObsComponents(emptyObsComponents);
        prog.addObservation(obs);

        ObservationNode obsNode = ObservationNodeFunctor.getObservationNode(odb, obs, user);
        assertEquals(new SPObservationID(progId, 1), obsNode.getObservationId());

        // No obs components but one sequence component.
        assertEquals(1, obsNode.getChildren().size());
        assertNull(obsNode.getAdaptiveOptics());
        assertNull(obsNode.getConstraints());
        assertNull(obsNode.getInstrument());
        assertNull(obsNode.getTarget());

        // One sequence component node.
        SequenceNode seqNode = obsNode.getSequence();
        assertEquals(0, seqNode.getChildren().size());
    }

    private static class Initializer {
        final ISPObservation obs;
        final ISPObsComponent obsComp;

        Initializer(ISPFactory factory, ISPProgram prog, SPComponentType type) throws Exception {
            obs = factory.createObservation(prog, Instrument.none, null);

            obsComp = factory.createObsComponent(prog, type, null);
            obs.addObsComponent(obsComp);

            List<ISPObsComponent> obsComponents = new ArrayList<>();
            obsComponents.add(obsComp);
            obs.setObsComponents(obsComponents);

            prog.addObservation(obs);
        }
    }

    private void verifyNode(NodeContext<?, ?> node, ISPObsComponent obsComp) throws Exception {
        assertNotNull(node);
        assertEquals(obsComp, node.getRemoteNode());
        assertEquals(progId, node.getProgramId());
        assertEquals(obsComp.getNodeKey(), node.getKey());
    }

    @Test
    public void testAo() throws Exception {
        Initializer init = new Initializer(odb.getFactory(), prog, InstAltair.SP_TYPE);

        ObservationNode obsNode = ObservationNodeFunctor.getObservationNode(odb, init.obs, user);
        assertEquals(2, obsNode.getChildren().size());
        assertNull(obsNode.getConstraints());
        assertNull(obsNode.getInstrument());
        assertNull(obsNode.getTarget());

        AoNode compNode = obsNode.getAdaptiveOptics();
        verifyNode(compNode, init.obsComp);
        if (!(compNode.getDataObject() instanceof InstAltair)) fail();
    }

    @Test
    public void testConstraints() throws Exception {
        Initializer init = new Initializer(odb.getFactory(), prog, SPSiteQuality.SP_TYPE);

        ObservationNode obsNode = ObservationNodeFunctor.getObservationNode(odb, init.obs, user);
        assertEquals(2, obsNode.getChildren().size());
        assertNull(obsNode.getAdaptiveOptics());
        assertNull(obsNode.getInstrument());
        assertNull(obsNode.getTarget());

        ConstraintsNode compNode = obsNode.getConstraints();
        verifyNode(compNode, init.obsComp);
        assertNotNull(compNode.getDataObject());
    }

    @Test
    public void testInstrument() throws Exception {
        Initializer init = new Initializer(odb.getFactory(), prog, InstGmosSouth.SP_TYPE);

        ObservationNode obsNode = ObservationNodeFunctor.getObservationNode(odb, init.obs, user);
        assertEquals(2, obsNode.getChildren().size());
        assertNull(obsNode.getAdaptiveOptics());
        assertNull(obsNode.getConstraints());
        assertNull(obsNode.getTarget());

        InstrumentNode compNode = obsNode.getInstrument();
        verifyNode(compNode, init.obsComp);
        if (!(compNode.getDataObject() instanceof InstGmosSouth)) fail();
    }

    @Test
    public void testTarget() throws Exception {
        Initializer init = new Initializer(odb.getFactory(), prog, TargetObsComp.SP_TYPE);

        ObservationNode obsNode = ObservationNodeFunctor.getObservationNode(odb, init.obs, user);
        assertEquals(2, obsNode.getChildren().size());
        assertNull(obsNode.getAdaptiveOptics());
        assertNull(obsNode.getConstraints());
        assertNull(obsNode.getInstrument());

        TargetNode compNode = obsNode.getTarget();
        verifyNode(compNode, init.obsComp);
        assertNotNull(compNode.getDataObject());
    }

    @Test
    public void testNestedSequence() throws Exception {
        Initializer init = new Initializer(odb.getFactory(), prog, TargetObsComp.SP_TYPE);

        ISPSeqComponent seqComp0 = odb.getFactory().createSeqComponent(prog, SeqRepeat.SP_TYPE, null);
        ISPSeqComponent seqComp0_0 = odb.getFactory().createSeqComponent(prog, SeqRepeatFlatObs.SP_TYPE, null);
        ISPSeqComponent seqComp0_1 = odb.getFactory().createSeqComponent(prog, SeqRepeatObserve.SP_TYPE, null);

        init.obs.getSeqComponent().addSeqComponent(seqComp0);
        seqComp0.addSeqComponent(seqComp0_0);
        seqComp0.addSeqComponent(seqComp0_1);

        ISPSeqComponent seqComp1 = odb.getFactory().createSeqComponent(prog, SeqRepeat.SP_TYPE, null);
        ISPSeqComponent seqComp1_0 = odb.getFactory().createSeqComponent(prog, SeqRepeatFlatObs.SP_TYPE, null);
        ISPSeqComponent seqComp1_1 = odb.getFactory().createSeqComponent(prog, SeqRepeatObserve.SP_TYPE, null);

        init.obs.getSeqComponent().addSeqComponent(seqComp1);
        seqComp1.addSeqComponent(seqComp1_0);
        seqComp1.addSeqComponent(seqComp1_1);

        ObservationNode obsNode = ObservationNodeFunctor.getObservationNode(odb, init.obs, user);
        SequenceNode seqNode = obsNode.getSequence();

        assertEquals(2, seqNode.getChildren().size());

        SequenceNode repeat0 = seqNode.getChildren().get(0);
        SequenceNode repeat1 = seqNode.getChildren().get(1);

        assertEquals(repeat0.getKey(), seqComp0.getNodeKey());
        assertEquals(repeat1.getKey(), seqComp1.getNodeKey());

        assertEquals(2, repeat0.getChildren().size());
        assertEquals(2, repeat1.getChildren().size());

        SequenceNode flat0_0 = repeat0.getChildren().get(0);
        SequenceNode obsv0_1 = repeat0.getChildren().get(1);

        assertEquals(flat0_0.getKey(), seqComp0_0.getNodeKey());
        assertEquals(obsv0_1.getKey(), seqComp0_1.getNodeKey());

        SequenceNode flat1_0 = repeat1.getChildren().get(0);
        SequenceNode obsv1_1 = repeat1.getChildren().get(1);

        assertEquals(flat1_0.getKey(), seqComp1_0.getNodeKey());
        assertEquals(obsv1_1.getKey(), seqComp1_1.getNodeKey());

        assertEquals(0, flat0_0.getChildren().size());
        assertEquals(0, obsv0_1.getChildren().size());
        assertEquals(0, flat1_0.getChildren().size());
        assertEquals(0, obsv1_1.getChildren().size());

        assertTrue(repeat0.getDataObject() instanceof SeqRepeat);
        assertTrue(repeat1.getDataObject() instanceof SeqRepeat);

        assertTrue(flat0_0.getDataObject() instanceof SeqRepeatFlatObs);
        assertTrue(flat1_0.getDataObject() instanceof SeqRepeatFlatObs);
        assertTrue(obsv0_1.getDataObject() instanceof SeqRepeatObserve);
        assertTrue(obsv1_1.getDataObject() instanceof SeqRepeatObserve);
    }
}
