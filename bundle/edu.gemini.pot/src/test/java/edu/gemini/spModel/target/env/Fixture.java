//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs2;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.None;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Contains common setup for test cases.
 */
final class Fixture {
    final SPTarget t_pwfs1_1, t_pwfs1_2, t_pwfs2;
    final ImList<SPTarget> tl_pwfs1, tl_pwfs2, tl_gmos;
    final GuideProbeTargets gpt_pwfs1, gpt_pwfs2, gpt_gmos;
    final GuideGroup grp_all, grp_pwfs2_gmos, grp_gmos;
    final Option<Long> when;

    Fixture() {
        t_pwfs1_1 = new SPTarget(1, 1);
        t_pwfs1_2 = new SPTarget(1, 2);
        t_pwfs2   = new SPTarget(2, 1);

        tl_pwfs1 = DefaultImList.create(t_pwfs1_1, t_pwfs1_2);
        tl_pwfs2 = DefaultImList.create(t_pwfs2);
        tl_gmos  = DefaultImList.create();

        gpt_pwfs1 = GuideProbeTargets.create(pwfs1, tl_pwfs1).selectPrimary(t_pwfs1_2);
        gpt_pwfs2 = GuideProbeTargets.create(pwfs2, tl_pwfs2).setPrimaryIndex(None.INTEGER);
        gpt_gmos  = GuideProbeTargets.create(GmosOiwfsGuideProbe.instance, tl_gmos);

        grp_all  = GuideGroup.create("All", gpt_gmos, gpt_pwfs1, gpt_pwfs2);
        grp_pwfs2_gmos = GuideGroup.create("PWFS2/GMOS", gpt_gmos, gpt_pwfs2);
        grp_gmos = GuideGroup.create("GMOS Only", gpt_gmos);

        when = None.instance();
    }

    public static void verifyGuideEnvironmentEquals(GuideEnvironment env1, GuideEnvironment env2, Option<Long> when) {
        assertEquals(env1.getReferencedGuiders(), env2.getReferencedGuiders());
        assertEquals(env1.getPrimaryIndex(), env2.getPrimaryIndex());
        verifyGroupListEquals(env1.getOptions(), env2.getOptions(), when);
    }

    /**
     * Compares two lists of GuideGroups for "equality".  They might not
     * be equals in the sense of grp1.equals(grp2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGroupListEquals(ImList<GuideGroup> lst1, ImList<GuideGroup> lst2, Option<Long> when) {
        assertEquals(lst1.size(), lst2.size());
        for (int i=0; i<lst1.size(); ++i) verifyGroupEquals(lst1.get(i), lst2.get(i), when);
    }

    /**
     * Compares two GuideGroups for "equality".  They might not
     * be equals in the sense of grp1.equals(grp2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGroupEquals(GuideGroup grp1, GuideGroup grp2, Option<Long> when) {
        assertEquals(grp1.getName(), grp2.getName());
        verifyGptListEquals(grp1.getAll(), grp2.getAll(), when);
    }

    /**
     * Compares two lists of GuideProbeTargets for "equality".  They might not
     * be equals in the sense of gtp1.equals(gpt2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGptListEquals(ImList<GuideProbeTargets> lst1, ImList<GuideProbeTargets> lst2, Option<Long> when) {
        assertEquals(lst1.size(), lst2.size());
        for (int i=0; i<lst1.size(); ++i) verifyGptEquals(lst1.get(i), lst2.get(i), when);
    }

    /**
     * Compares two GuideProbeTargets instances for "equality".  They might not
     * be equals in the sense of gtp1.equals(gpt2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGptEquals(GuideProbeTargets gpt1, GuideProbeTargets gpt2, Option<Long> when) {
        assertEquals(gpt1.getGuider(), gpt2.getGuider());
        assertEquals(gpt1.getPrimaryIndex(), gpt2.getPrimaryIndex());
        verifySpListEquals(gpt1.getOptions(), gpt2.getOptions(), when);
    }

    /**
     * Compares two target lists for "equality".  SPTargets don't define an
     * equals method, so two SPTargets can contain the same values but
     * nevertheless not compare equals with the equals method.  The OT even
     * relies on this fact.
     *
     * <p>For our purposes of testing TargetEnvironment objects, we just want
     * an idea that they are the same and checking the coordinates is enough.
     */
    public static void verifySpListEquals(ImList<SPTarget> lst1, ImList<SPTarget> lst2, Option<Long> when) {
        assertEquals(lst1.size(), lst2.size());

        for (int i=0; i<lst1.size(); ++i) {
            SPTarget t1 = lst1.get(i);
            SPTarget t2 = lst2.get(i);

            // The two targets won't compare .equals(), but we can just check
            // the coordinates and get enough of an idea that they are the
            // same for the purposes of testing GuideProbeTargets.

            assertOptEquals(t1.getTarget().getRaDegrees(when), t2.getTarget().getRaDegrees(when), 0.000001);
            assertOptEquals(t1.getTarget().getDecDegrees(when), t2.getTarget().getDecDegrees(when), 0.000001);
        }
    }

    /**
     * Asserts that either (a) both are None, or (b) both are defined and values are within the
     * given tolerance.
     */
    public static void assertOptEquals(Option<Double> a, Option<Double> b, Double tolerance) {
        if (a.isDefined() || b.isDefined()) {
            if (a.isEmpty() || b.isEmpty()) fail("unequal: " + a + ", " + b);
            a.foreach(aa -> b.foreach(bb -> assertEquals(aa, bb, tolerance)));
        } // otherwise both are None
    }

}
