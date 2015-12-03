package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs2;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Test cases for {@link GuideGroup}.
 */
public final class GuideGroupTest extends TestCase {
    private final Fixture fix = new Fixture();

    public void testEmpty() {
        assertEquals(None.STRING, GuideGroup.EMPTY().getName());
        assertEquals(GuideProbeTargets.EMPTY_LIST, GuideGroup.EMPTY().getAll());
    }

    public void testCreateNullName() {
        final GuideGroup grp = GuideGroup.create(null);
        assertEquals(None.STRING, grp.getName());
        assertEquals(GuideProbeTargets.EMPTY_LIST, grp.getAll());

        final GuideGroup grp2 = GuideGroup.create((String)null, GuideProbeTargets.EMPTY_LIST);
        assertEquals(None.STRING, grp2.getName());
        assertEquals(GuideProbeTargets.EMPTY_LIST, grp2.getAll());
    }

    public void testNormalize() {
        // Remove duplicates
        final GuideGroup grp = GuideGroup.create("x", fix.gpt_pwfs1, fix.gpt_pwfs1);
        Fixture.verifyGptListEquals(DefaultImList.create(fix.gpt_pwfs1), grp.getAll(), fix.when);

        // Sort
        final GuideGroup grp2 = GuideGroup.create("x", fix.gpt_pwfs2, fix.gpt_gmos, fix.gpt_pwfs1);
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp2.getAll(), fix.when);

        // Sort and remove duplicates.
        final GuideGroup grp3 = GuideGroup.create("x", fix.gpt_pwfs2, fix.gpt_gmos, fix.gpt_pwfs1, fix.gpt_pwfs2, fix.gpt_gmos);
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp3.getAll(), fix.when);
    }

    public void testSetName() {
        final GuideGroup grp = fix.grp_all.setName((String)null);
        assertEquals(None.STRING, grp.getName());

        final GuideGroup grp2 = fix.grp_all.setName("x");
        assertEquals(new Some<>("x"), grp2.getName());

        try {
            grp.setName((Option<String>)null);
            fail("can't assign a null option");
        } catch (IllegalArgumentException ex) {
            // okay
        }

        final GuideGroup grp3 = fix.grp_all.setName(None.STRING);
        assertEquals(None.STRING, grp3.getName());

        final GuideGroup grp4 = fix.grp_all.setName(new Some<>("x"));
        assertEquals(new Some<>("x"), grp4.getName());
    }

    public void testContains() {
        assertTrue(fix.grp_all.contains(pwfs1));
        assertTrue(fix.grp_all.contains(pwfs2));
        assertTrue(fix.grp_all.contains(GmosOiwfsGuideProbe.instance));

        assertFalse(GuideGroup.EMPTY().contains(pwfs1));
        assertFalse(fix.grp_gmos.contains(pwfs1));
    }

    public void testGet() {
        Fixture.verifyGptEquals(fix.gpt_pwfs1, fix.grp_all.get(pwfs1).getValue(), fix.when);
        Fixture.verifyGptEquals(fix.gpt_gmos, fix.grp_gmos.get(GmosOiwfsGuideProbe.instance).getValue(), fix.when);
        assertTrue(fix.grp_gmos.get(pwfs1).isEmpty());
        assertTrue(GuideGroup.EMPTY().get(pwfs1).isEmpty());
    }

    public void testPut() {
        // Add a pwfs1 and pwfs2 to the gmos group, which should then be the
        // same as the "all" group.
        final GuideGroup grp = fix.grp_gmos.put(fix.gpt_pwfs2).put(fix.gpt_pwfs1);
        assertEquals(grp.getName(), fix.grp_gmos.getName());
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp.getAll(), fix.when);

        // Add to an empty group
        final GuideGroup grp2 = GuideGroup.EMPTY().put(fix.gpt_gmos);
        assertEquals(grp2.getName(), GuideGroup.EMPTY().getName());
        Fixture.verifyGptListEquals(fix.grp_gmos.getAll(), grp2.getAll(), fix.when);

        // Replace an existing GuideProbeTargets.
        final GuideProbeTargets gpt = GuideProbeTargets.create(pwfs1, fix.t_pwfs1_2);
        final GuideGroup grp3 = fix.grp_all.put(gpt);  // now the pwfs1 guide probe targets has just one target
        assertEquals(grp3.getName(), fix.grp_all.getName());
        Fixture.verifySpListEquals(DefaultImList.create(fix.t_pwfs1_2), grp3.get(pwfs1).getValue().getTargets(), fix.when);
    }

    public void testRemove() {
        final GuideGroup grp = fix.grp_all.remove(pwfs1);
        assertEquals(grp.getName(), fix.grp_all.getName());
        Fixture.verifyGptListEquals(DefaultImList.create(fix.gpt_gmos, fix.gpt_pwfs2), grp.getAll(), fix.when);

        final GuideGroup grp2 = grp.remove(GmosOiwfsGuideProbe.instance);
        assertEquals(grp2.getName(), fix.grp_all.getName());
        Fixture.verifyGptListEquals(DefaultImList.create(fix.gpt_pwfs2), grp2.getAll(), fix.when);

        final GuideGroup grp3 = grp2.remove(pwfs2);
        assertEquals(grp3.getName(), fix.grp_all.getName());
        Fixture.verifyGptListEquals(GuideGroup.EMPTY().getAll(), grp3.getAll(), fix.when);

        // Remove from an empty list
        final GuideGroup grp4 = grp3.remove(pwfs2);
        assertSame(grp4, grp3);
    }

    public void testClear() {
        final GuideGroup grp = fix.grp_all.clear();
        assertEquals(grp.getName(), fix.grp_all.getName());
        Fixture.verifyGptListEquals(GuideGroup.EMPTY().getAll(), grp.getAll(), fix.when);

        final GuideGroup grp2 = GuideGroup.EMPTY().clear();
        assertSame(GuideGroup.EMPTY(), grp2);
    }

    public void testPutAll() {
        // Put all on an empty group.
        final GuideGroup grp = GuideGroup.EMPTY().putAll(fix.grp_all.getAll());
        assertEquals(GuideGroup.EMPTY().getName(), grp.getName());
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp.getAll(), fix.when);

        // Put all, replacing some members.
        final GuideGroup grp2 = fix.grp_gmos.putAll(fix.grp_all.getAll());
        assertEquals(fix.grp_gmos.getName(), grp2.getName());
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp2.getAll(), fix.when);

        // Test replace via putAll
        final GuideProbeTargets gpt = GuideProbeTargets.create(pwfs1, fix.t_pwfs1_2);
        final GuideGroup grp3 = fix.grp_all.putAll(DefaultImList.create(gpt));  // now the pwfs1 guide probe targets has just one target
        assertEquals(grp3.getName(), fix.grp_all.getName());
        Fixture.verifySpListEquals(DefaultImList.create(fix.t_pwfs1_2), grp3.get(pwfs1).getValue().getTargets(), fix.when);

        // put all empty
        final GuideGroup grp4 = fix.grp_all.putAll(GuideProbeTargets.EMPTY_LIST);
        assertEquals(grp4.getName(), fix.grp_all.getName());
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp4.getAll(), fix.when);
    }

    public void testSetAll() {
        // Set all on an empty group.
        final GuideGroup grp = GuideGroup.EMPTY().setAll(fix.grp_all.getAll());
        assertEquals(GuideGroup.EMPTY().getName(), grp.getName());
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp.getAll(), fix.when);

        // Clear via set all
        final GuideGroup grp2 = fix.grp_all.setAll(GuideProbeTargets.EMPTY_LIST);
        assertEquals(fix.grp_all.getName(), grp2.getName());
        Fixture.verifyGptListEquals(GuideProbeTargets.EMPTY_LIST, grp2.getAll(), fix.when);
    }

    public void testGetAllContaining() {
        Fixture.verifyGptListEquals(DefaultImList.create(fix.gpt_pwfs1), fix.grp_all.getAllContaining(fix.t_pwfs1_1), fix.when);
        Fixture.verifyGptListEquals(GuideProbeTargets.EMPTY_LIST, fix.grp_gmos.getAllContaining(fix.t_pwfs1_1), fix.when);

        // Put the same target in more than one GuideProbeTargets object.
        final GuideProbeTargets gpt = fix.gpt_gmos.update(OptionsList.UpdateOps.append(fix.t_pwfs1_1));

        // t_pwfs1_1 should be in the first two GuideProbeTarets sets here
        final GuideGroup grp = GuideGroup.create("x", fix.gpt_pwfs1, gpt, fix.gpt_pwfs2);
        Fixture.verifyGptListEquals(DefaultImList.create(gpt, fix.gpt_pwfs1), grp.getAllContaining(fix.t_pwfs1_1), fix.when);
    }

    public void testGetAllMatching() {
        Fixture.verifyGptListEquals(DefaultImList.create(fix.gpt_pwfs1, fix.gpt_pwfs2), fix.grp_all.getAllMatching(GuideProbe.Type.PWFS), fix.when);
        Fixture.verifyGptListEquals(DefaultImList.create(fix.gpt_pwfs2), fix.grp_pwfs2_gmos.getAllMatching(GuideProbe.Type.PWFS), fix.when);
        Fixture.verifyGptListEquals(GuideProbeTargets.EMPTY_LIST, fix.grp_gmos.getAllMatching(GuideProbe.Type.PWFS), fix.when);
        Fixture.verifyGptListEquals(GuideProbeTargets.EMPTY_LIST, GuideGroup.EMPTY().getAllMatching(GuideProbe.Type.PWFS), fix.when);
    }

    public void testGetReferencedGuiders() {
        // GMOS is removed because it is empty, even though there is a
        // GuideProbeTargets instance for it in all the GuideGroups.
        final Set<GuideProbe> all = new HashSet<>();
        all.add(pwfs1);
        all.add(pwfs2);

        final Set<GuideProbe> pwfs2_gmos = new HashSet<>();
        pwfs2_gmos.add(pwfs2);

        final Set<GuideProbe> gmos = new HashSet<>();

        assertEquals(all, fix.grp_all.getReferencedGuiders());
        assertEquals(pwfs2_gmos, fix.grp_pwfs2_gmos.getReferencedGuiders());
        assertEquals(gmos, fix.grp_gmos.getReferencedGuiders());
        assertEquals(new HashSet<GuideProbe>(), GuideGroup.EMPTY().getReferencedGuiders());
    }

    public void testGetReferencedGuidersByType() {
        // Make a GMOS GuideProbeTargets instance with a guide star so that
        // it is not eliminated from the results because it is empty.
        GuideProbeTargets gpt = GuideProbeTargets.create(GmosOiwfsGuideProbe.instance, new SPTarget());
        final GuideGroup all = fix.grp_all.put(gpt);

        final Set<GuideProbe> pwfs = new HashSet<>();
        pwfs.add(pwfs1); pwfs.add(pwfs2);

        final Set<GuideProbe> oiwfs = new HashSet<>();
        oiwfs.add(GmosOiwfsGuideProbe.instance);

        final Set<GuideProbe> empty = Collections.emptySet();

        assertEquals(pwfs,  all.getReferencedGuiders(GuideProbe.Type.PWFS));
        assertEquals(oiwfs, all.getReferencedGuiders(GuideProbe.Type.OIWFS));
        assertEquals(empty, all.getReferencedGuiders(GuideProbe.Type.AOWFS));
        assertEquals(empty, GuideGroup.EMPTY().getReferencedGuiders(GuideProbe.Type.PWFS));
    }

    public void testGetTargets() {
        final ImList<SPTarget> expected = DefaultImList.create(
            fix.t_pwfs1_1, fix.t_pwfs1_2, fix.t_pwfs2
        );
        assertEquals(expected, fix.grp_all.getTargets());

        final ImList<SPTarget> empty = ImCollections.emptyList();
        assertEquals(empty, fix.grp_gmos.getTargets());
        assertEquals(empty, GuideGroup.EMPTY().getTargets());
    }

    public void testContainsTarget() {
        assertTrue(fix.grp_all.containsTarget(fix.t_pwfs1_2));
        assertFalse(fix.grp_gmos.containsTarget(fix.t_pwfs1_2));
        assertFalse(GuideGroup.EMPTY().containsTarget(fix.t_pwfs1_2));
    }

    public void testRemoveTargetUpdate() {
        final UpdateOp<GuideGroup> f = g -> g.removeTarget(fix.t_pwfs1_2);

        final GuideGroup grp = f.apply(fix.grp_all);
        final ImList<SPTarget> expected = DefaultImList.create(
            fix.t_pwfs1_1, fix.t_pwfs2
        );
        assertEquals(expected, grp.getTargets());

        final GuideGroup grp2 = f.apply(fix.grp_pwfs2_gmos);
        final ImList<SPTarget> expected2 = DefaultImList.create(fix.t_pwfs2);
        assertEquals(expected2, grp2.getTargets());

        final GuideGroup grp3 = f.apply(GuideGroup.EMPTY());
        final ImList<SPTarget> expected3 = DefaultImList.create();
        assertEquals(expected3, grp3.getTargets());
    }

    public void testRemoveTarget() {
        final ImList<SPTarget> expected = DefaultImList.create(
            fix.t_pwfs1_1, fix.t_pwfs2
        );

        final GuideGroup grp = fix.grp_all.removeTarget(fix.t_pwfs1_2);
        assertEquals(expected, grp.getTargets());

        final GuideGroup grp2 = fix.grp_pwfs2_gmos.removeTarget(fix.t_pwfs2);
        final ImList<SPTarget> empty = ImCollections.emptyList();
        assertEquals(empty, grp2.getTargets());
        assertEquals(empty, GuideGroup.EMPTY().removeTarget(fix.t_pwfs1_2).getTargets());
    }

    public void testCloneTargets() {
        final GuideGroup grp = fix.grp_all.cloneTargets();
        assertFalse(fix.grp_all.equals(grp));
        Fixture.verifyGptListEquals(fix.grp_all.getAll(), grp.getAll(), fix.when);
        assertEquals(fix.grp_all.getName(), grp.getName());
    }

    public void testIterateAllTargets() {
        ImList<SPTarget> targetList = DefaultImList.create();
        for (final Iterator<SPTarget> it = fix.grp_all.iterateAllTargets(); it.hasNext(); ) {
            targetList = targetList.cons(it.next());
        }
        targetList = targetList.reverse();

        final ImList<SPTarget> expected = DefaultImList.create(
            fix.t_pwfs1_1, fix.t_pwfs1_2, fix.t_pwfs2
        );
        assertEquals(expected, targetList);

        for (final Iterator<SPTarget> it = fix.grp_gmos.iterateAllTargets(); it.hasNext(); ) {
            fail("nothing to iterate");
        }
        for (final Iterator<SPTarget> it = GuideGroup.EMPTY().iterateAllTargets(); it.hasNext(); ) {
            fail("nothing to iterate");
        }
    }

    public void testIo() {
        final PioFactory fact = new PioXmlFactory();
        final ImList<GuideGroup> groups = DefaultImList.create(fix.grp_all, fix.grp_pwfs2_gmos, fix.grp_gmos);
        groups.foreach(expected -> {
            final GuideGroup actual = GuideGroup.fromParamSet(expected.getParamSet(fact));
            Fixture.verifyGroupEquals(expected, actual, fix.when);
        });
    }
}
