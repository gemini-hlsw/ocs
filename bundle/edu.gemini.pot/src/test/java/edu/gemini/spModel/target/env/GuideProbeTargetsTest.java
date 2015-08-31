//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs2;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Comparator;

/**
 * Test cases for {@link GuideProbeTargets}.
 */
public final class GuideProbeTargetsTest extends TestCase {

    private final Fixture fix = new Fixture();

    @Test
    public void testMatchGuider() {
        final PredicateOp<GuideProbeTargets> op = GuideProbeTargets.match(pwfs1);
        assertTrue(op.apply(fix.gpt_pwfs1));
        assertFalse(op.apply(fix.gpt_pwfs2));
        assertFalse(op.apply(fix.gpt_gmos));
    }

    @Test
    public void testMatchType() {
        final PredicateOp<GuideProbeTargets> op = GuideProbeTargets.match(GuideProbe.Type.OIWFS);
        assertFalse(op.apply(fix.gpt_pwfs1));
        assertFalse(op.apply(fix.gpt_pwfs2));
        assertTrue(op.apply(fix.gpt_gmos));
    }

    @Test
    public void testComparator() {
        Comparator<GuideProbeTargets> c = GuideProbeTargets.GuiderComparator.instance;
        assertTrue(c.compare(fix.gpt_pwfs1, fix.gpt_pwfs2) < 0);
        assertTrue(c.compare(fix.gpt_pwfs2, fix.gpt_pwfs1) > 0);
        assertTrue(c.compare(fix.gpt_pwfs1, fix.gpt_pwfs1) == 0);
        assertTrue(c.compare(fix.gpt_gmos, fix.gpt_pwfs1) < 0);
        assertTrue(c.compare(fix.gpt_pwfs2, fix.gpt_gmos) > 0);
    }

    @Test
    public void testSortByGuider() {
        ImList<GuideProbeTargets> lst = DefaultImList.create(fix.gpt_pwfs2, fix.gpt_gmos, fix.gpt_pwfs1);
        lst = GuideProbeTargets.sortByGuider(lst);

        assertSame(fix.gpt_gmos, lst.get(0));
        assertSame(fix.gpt_pwfs1, lst.get(1));
        assertSame(fix.gpt_pwfs2, lst.get(2));

        lst = DefaultImList.create();
        ImList<GuideProbeTargets> lst2 = GuideProbeTargets.sortByGuider(lst);
        assertSame(lst, lst2);

        lst = DefaultImList.create(fix.gpt_gmos);
        lst2 = GuideProbeTargets.sortByGuider(lst);
        assertEquals(lst, lst2);
    }

    @Test
    public void testExtractProbe() {
        Function1<GuideProbeTargets, GuideProbe> f = GuideProbeTargets.EXTRACT_PROBE;
        assertEquals(pwfs1, f.apply(fix.gpt_pwfs1));
        assertEquals(pwfs2, f.apply(fix.gpt_pwfs2));
        assertEquals(GmosOiwfsGuideProbe.instance, f.apply(fix.gpt_gmos));
    }

    @Test
    public void testMatchNonEmpty() {
        PredicateOp<GuideProbeTargets> f = GuideProbeTargets.MATCH_NON_EMPTY;
        assertTrue(f.apply(fix.gpt_pwfs1));
        assertTrue(f.apply(fix.gpt_pwfs2));
        assertFalse(f.apply(fix.gpt_gmos));
    }

    @Test
    public void testIo() {
        PioFactory fact = new PioXmlFactory();
        for (GuideProbeTargets expected : DefaultImList.create(fix.gpt_pwfs1, fix.gpt_pwfs2, fix.gpt_gmos)) {
            GuideProbeTargets actual = GuideProbeTargets.fromParamSet(expected.getParamSet(fact));
            Fixture.verifyGptEquals(expected, actual, fix.when);
        }
    }

    public void testContainsTarget() {
        assertTrue(fix.gpt_pwfs1.containsTarget(fix.t_pwfs1_1));
        assertTrue(fix.gpt_pwfs1.containsTarget(fix.t_pwfs1_2));
        assertFalse(fix.gpt_pwfs1.containsTarget(fix.t_pwfs2));

        assertFalse(fix.gpt_pwfs2.containsTarget(fix.t_pwfs1_1));
        assertFalse(fix.gpt_pwfs2.containsTarget(fix.t_pwfs1_2));
        assertTrue(fix.gpt_pwfs2.containsTarget(fix.t_pwfs2));

        assertFalse(fix.gpt_gmos.containsTarget(fix.t_pwfs1_1));
        assertFalse(fix.gpt_gmos.containsTarget(fix.t_pwfs1_2));
        assertFalse(fix.gpt_gmos.containsTarget(fix.t_pwfs2));
    }

    public void testGetTargets() {
        assertEquals(fix.tl_pwfs1, fix.gpt_pwfs1.getTargets());
        assertEquals(fix.tl_pwfs2, fix.gpt_pwfs2.getTargets());
        assertEquals(fix.tl_gmos,  fix.gpt_gmos.getTargets());
    }

    public void testCloneTargets() {
        GuideProbeTargets cgptpwfs1 = fix.gpt_pwfs1.cloneTargets();
        GuideProbeTargets cgptpwfs2 = fix.gpt_pwfs2.cloneTargets();
        GuideProbeTargets cgptgmos  = fix.gpt_gmos.cloneTargets();

        Fixture.verifyGptEquals(fix.gpt_pwfs1, cgptpwfs1, fix.when);
        assertFalse(fix.gpt_pwfs1.equals(cgptpwfs1));
        Fixture.verifyGptEquals(fix.gpt_pwfs2, cgptpwfs2, fix.when);
        assertFalse(fix.gpt_pwfs2.equals(cgptpwfs2));
        Fixture.verifyGptEquals(fix.gpt_gmos,  cgptgmos, fix.when);
        assertEquals(fix.gpt_gmos, cgptgmos); // no targets, so should be same
    }

    public void testRemoveTarget() {
        GuideProbeTargets gpt;

        gpt = fix.gpt_pwfs1.removeTarget(fix.t_pwfs1_1);
        Fixture.verifySpListEquals(DefaultImList.create(fix.t_pwfs1_2), gpt.getTargets(), fix.when);

        gpt = fix.gpt_pwfs1.removeTarget(fix.t_pwfs1_2);
        Fixture.verifySpListEquals(DefaultImList.create(fix.t_pwfs1_1), gpt.getTargets(), fix.when);

        // Remove a target that doesn't exist in the GuideProbeTargets instance.
        gpt = fix.gpt_pwfs1.removeTarget(fix.t_pwfs2);
        Fixture.verifySpListEquals(fix.tl_pwfs1, gpt.getTargets(), fix.when);

        // Remove the only target in the list
        gpt = fix.gpt_pwfs2.removeTarget(fix.t_pwfs2);
        ImList<SPTarget> empty = ImCollections.emptyList();
        Fixture.verifySpListEquals(empty, gpt.getTargets(), fix.when);

        // Remove from an empty list.
        gpt = fix.gpt_gmos.removeTarget(fix.t_pwfs1_1);
        Fixture.verifySpListEquals(fix.tl_gmos, gpt.getTargets(), fix.when);
    }

    public void testTargetMatch() {
        PredicateOp<TargetContainer> f = new TargetContainer.TargetMatch(fix.t_pwfs2);
        assertFalse(f.apply(fix.gpt_pwfs1));
        assertTrue(f.apply(fix.gpt_pwfs2));
        assertFalse(f.apply(fix.gpt_gmos));
    }

    public void testExtractTarget() {
        Function1<TargetContainer, ImList<SPTarget>> f = TargetContainer.EXTRACT_TARGET;
        assertEquals(fix.tl_pwfs1, f.apply(fix.gpt_pwfs1));
        assertEquals(fix.tl_pwfs2, f.apply(fix.gpt_pwfs2));
        assertEquals(fix.tl_gmos,  f.apply(fix.gpt_gmos));
    }
}
