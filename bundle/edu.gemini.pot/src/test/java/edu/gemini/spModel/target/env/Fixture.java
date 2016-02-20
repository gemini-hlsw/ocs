package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs2;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;

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

    public static void verifyGuideEnvironmentEquals(final GuideEnvironment env1, final GuideEnvironment env2, final Option<Long> when) {
        assertEquals(env1.getReferencedGuiders(), env2.getReferencedGuiders());
        assertEquals(env1.getPrimaryIndex(), env2.getPrimaryIndex());
        verifyGroupListEquals(env1.getOptions(), env2.getOptions(), when);
    }

    /**
     * Compares two lists of GuideGroups for "equality".  They might not
     * be equals in the sense of grp1.equals(grp2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGroupListEquals(final ImList<GuideGroup> lst1, final ImList<GuideGroup> lst2, final Option<Long> when) {
        assertEquals(lst1.size(), lst2.size());
        lst1.zip(lst2).foreach(p -> verifyGroupEquals(p._1(), p._2(), when));
    }

    /**
     * Compares two GuideGroups for "equality".  They might not
     * be equals in the sense of grp1.equals(grp2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGroupEquals(final GuideGroup grp1, final GuideGroup grp2, final Option<Long> when) {
        assertEquals(grp1.getName(), grp2.getName());
        verifyGptListEquals(grp1.getAll(), grp2.getAll(), when);
    }

    /**
     * Compares two lists of GuideProbeTargets for "equality".  They might not
     * be equals in the sense of gtp1.equals(gpt2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGptListEquals(final ImList<GuideProbeTargets> lst1, final ImList<GuideProbeTargets> lst2, final Option<Long> when) {
        assertEquals(lst1.size(), lst2.size());
        lst1.zip(lst2).foreach(p -> verifyGptEquals(p._1(), p._2(), when));
    }

    /**
     * Compares two GuideProbeTargets instances for "equality".  They might not
     * be equals in the sense of gtp1.equals(gpt2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGptEquals(final GuideProbeTargets gpt1, final GuideProbeTargets gpt2, final Option<Long> when) {
        assertEquals(gpt1.getGuider(), gpt2.getGuider());
        assertEquals(gpt1.getPrimaryIndex(), gpt2.getPrimaryIndex());
        verifySpListEquals(gpt1.getTargets(), gpt2.getTargets(), when);
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
    public static void verifySpListEquals(final ImList<SPTarget> lst1, final ImList<SPTarget> lst2, final Option<Long> when) {
        assertEquals(lst1.size(), lst2.size());

        lst1.zip(lst2).foreach(p -> {
            final SPTarget t1 = p._1();
            final SPTarget t2 = p._2();

            // The two targets won't compare .equals(), but we can just check
            // the coordinates and get enough of an idea that they are the
            // same for the purposes of testing GuideProbeTargets.

            assertOptEquals(t1.getTarget().getRaDegrees(when), t2.getTarget().getRaDegrees(when), 0.000001);
            assertOptEquals(t1.getDecDegrees(when), t2.getDecDegrees(when), 0.000001);
        });
    }

    /**
     * Asserts that either (a) both are None, or (b) both are defined and values are within the
     * given tolerance.
     */
    public static void assertOptEquals(final Option<Double> a, final Option<Double> b, final Double tolerance) {
        if (a.isDefined() || b.isDefined()) {
            if (a.isEmpty() || b.isEmpty()) fail("unequal: " + a + ", " + b);
            a.foreach(aa -> b.foreach(bb -> assertEquals(aa, bb, tolerance)));
        } // otherwise both are None
    }

}
