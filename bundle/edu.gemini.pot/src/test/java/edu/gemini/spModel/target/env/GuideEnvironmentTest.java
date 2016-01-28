package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs2;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Test cases for {@link edu.gemini.spModel.target.env.GuideEnvironment}.
 */
public class GuideEnvironmentTest {

    // Create four GuideGroups.
    // 1 - has pwfs1
    // 2 - has pwfs1, pwfs2
    // 3 - has gmos
    // 4 - has none
    private final SPTarget t_pwfs1_1 = new SPTarget(1, 1);
    private final SPTarget t_pwfs1_2 = new SPTarget(1, 2);
    private final SPTarget t_pwfs2   = new SPTarget(2, 1);
    private final SPTarget t_gmos    = new SPTarget(3, 1);
    private final GuideProbeTargets gpt_pwfs1 = GuideProbeTargets.create(pwfs1, t_pwfs1_1, t_pwfs1_2);
    private final GuideProbeTargets gpt_pwfs2 = GuideProbeTargets.create(pwfs2, t_pwfs2);
    private final GuideProbeTargets gpt_gmos  = GuideProbeTargets.create(GmosOiwfsGuideProbe.instance, t_gmos);
    private final GuideGroup grp1 = GuideGroup.create("1", gpt_pwfs1);
    private final GuideGroup grp2 = GuideGroup.create("2", gpt_pwfs1, gpt_pwfs2);
    private final GuideGroup grp3 = GuideGroup.create("3", gpt_gmos);
    private final GuideGroup grp4 = GuideGroup.create("4");
    private final GuideEnvironment env = GuideEnvironment.create(OptionsListImpl.create(grp1, grp2, grp3, grp4));
    private final Option<Long> when = None.instance();

    @Test
    public void testGetReferencedGuiders() {
        final Set<GuideProbe> expected = new HashSet<>();
        assertEquals(expected, GuideEnvironment$.MODULE$.Initial().getReferencedGuiders());

        final Set<GuideProbe> actual = env.getReferencedGuiders();
        expected.add(pwfs1);
        expected.add(pwfs2);
        expected.add(GmosOiwfsGuideProbe.instance);
        assertEquals(actual, expected);
    }

    @Test
    public void testGetTargets() {
        final ImList<SPTarget> empty = ImCollections.emptyList();
        assertEquals(empty, GuideEnvironment$.MODULE$.Initial().getTargets());

        // sorted in the order they are encountered in the groups
        final ImList<SPTarget> expected = DefaultImList.create(t_pwfs1_1, t_pwfs1_2, t_pwfs1_1, t_pwfs1_2, t_pwfs2, t_gmos);
        assertEquals(expected, env.getTargets());
    }

    @Test
    public void testContainsTarget() {
        assertTrue(env.containsTarget(t_pwfs1_1));
        assertTrue(env.containsTarget(t_gmos));
        assertFalse(env.containsTarget(new SPTarget(6, 6)));

        assertFalse(GuideEnvironment$.MODULE$.Initial().containsTarget(t_pwfs1_1));
    }

    @Test
    public void testCloneTargets() {
        final GuideEnvironment env2 = env.cloneTargets();
        Fixture.verifyGuideEnvironmentEquals(env, env2, when);
        assertFalse(env2.containsTarget(t_pwfs1_1)); // SPTarget.equals() not defined
    }

    @Test
    public void testRemoveTarget() {
        final GuideEnvironment env2 = env.removeTarget(t_pwfs1_1);
        final ImList<SPTarget> expected2 = DefaultImList.create(t_pwfs1_2, t_pwfs1_2, t_pwfs2, t_gmos);
        assertEquals(expected2, env2.getTargets());

        final GuideEnvironment env3 = env2.removeTarget(t_gmos);
        final ImList<SPTarget> expected3 = DefaultImList.create(t_pwfs1_2, t_pwfs1_2, t_pwfs2);
        assertEquals(expected3, env3.getTargets());

        final GuideEnvironment env4 = env3.removeTarget(t_pwfs2);
        final ImList<SPTarget> expected4 = DefaultImList.create(t_pwfs1_2, t_pwfs1_2);
        assertEquals(expected4, env4.getTargets());

        final GuideEnvironment env5 = env4.removeTarget(t_pwfs1_2);
        final ImList<SPTarget> expected5 = ImCollections.emptyList();
        assertEquals(expected5, env5.getTargets());
    }

    @Test
    public void testIo() {
        // With non-default primary
        final GuideEnvironment env4 = env.setPrimaryIndex(1);

        // Empty
        final GuideEnvironment env5 = GuideEnvironment$.MODULE$.Initial();

        final ImList<GuideEnvironment> lst = DefaultImList.create(env, env4, env5);
        final PioFactory fact = new PioXmlFactory();

        lst.foreach(expected -> {
            final GuideEnvironment actual = GuideEnvironment.fromParamSet(expected.getParamSet(fact));
            Fixture.verifyGuideEnvironmentEquals(expected, actual, when);
        });
    }

    @Test
    public void testPutGuideProbeTargets() {
        // Verify that the 4th group has no guide probe targets.
        assertEquals(0, env.getOptions().get(3).getAll().size());

        // Add guide probe targets to the 4th group.
        final GuideEnvironment env2 = env.putGuideProbeTargets(3, gpt_pwfs1);

        // Make sure that they are there now.
        final GuideGroup newGrp4 = env2.getOptions().get(3);
        final GuideProbeTargets gpt = newGrp4.get(pwfs1).getValue();
        assertNotNull(gpt);

        // Update them with a new target (the 3rd in the list of options)
        final GuideProbeTargets gpt2 = gpt.setOptions(gpt.getOptions().append(new SPTarget()));
        final GuideEnvironment  env3 = env2.putGuideProbeTargets(3, gpt2);

        // Check that they now contain the new target.
        final GuideProbeTargets gpt3 = env3.getOptions().get(3).get(pwfs1).getValue();
        assertEquals(3, gpt3.getOptions().size());
    }
}
