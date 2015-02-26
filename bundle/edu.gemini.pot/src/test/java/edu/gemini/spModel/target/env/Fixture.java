//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1;
import static edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs2;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.target.system.CoordinateParam;

import static org.junit.Assert.assertEquals;

/**
 * Contains common setup for test cases.
 */
final class Fixture {
    final SPTarget t_pwfs1_1, t_pwfs1_2, t_pwfs2;
    final ImList<SPTarget> tl_pwfs1, tl_pwfs2, tl_gmos;
    final GuideProbeTargets gpt_pwfs1, gpt_pwfs2, gpt_gmos;
    final GuideGroup grp_all, grp_pwfs2_gmos, grp_gmos;

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
    }

    public static void verifyGuideEnvironmentEquals(GuideEnvironment env1, GuideEnvironment env2) {
        assertEquals(env1.getActiveGuiders(), env2.getActiveGuiders());
        assertEquals(env1.getReferencedGuiders(), env2.getReferencedGuiders());
        assertEquals(env1.getPrimaryIndex(), env2.getPrimaryIndex());
        verifyGroupListEquals(env1.getOptions(), env2.getOptions());
    }

    /**
     * Compares two lists of GuideGroups for "equality".  They might not
     * be equals in the sense of grp1.equals(grp2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGroupListEquals(ImList<GuideGroup> lst1, ImList<GuideGroup> lst2) {
        assertEquals(lst1.size(), lst2.size());
        for (int i=0; i<lst1.size(); ++i) verifyGroupEquals(lst1.get(i), lst2.get(i));
    }

    /**
     * Compares two GuideGroups for "equality".  They might not
     * be equals in the sense of grp1.equals(grp2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGroupEquals(GuideGroup grp1, GuideGroup grp2) {
        assertEquals(grp1.getName(), grp2.getName());
        verifyGptListEquals(grp1.getAll(), grp2.getAll());
    }

    /**
     * Compares two lists of GuideProbeTargets for "equality".  They might not
     * be equals in the sense of gtp1.equals(gpt2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGptListEquals(ImList<GuideProbeTargets> lst1, ImList<GuideProbeTargets> lst2) {
        assertEquals(lst1.size(), lst2.size());
        for (int i=0; i<lst1.size(); ++i) verifyGptEquals(lst1.get(i), lst2.get(i));
    }

    /**
     * Compares two GuideProbeTargets instances for "equality".  They might not
     * be equals in the sense of gtp1.equals(gpt2) because SPTargets don't
     * define an equals method.
     */
    public static void verifyGptEquals(GuideProbeTargets gpt1, GuideProbeTargets gpt2) {
        assertEquals(gpt1.getGuider(), gpt2.getGuider());
        assertEquals(gpt1.getPrimaryIndex(), gpt2.getPrimaryIndex());
        verifySpListEquals(gpt1.getOptions(), gpt2.getOptions());
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
    public static void verifySpListEquals(ImList<SPTarget> lst1, ImList<SPTarget> lst2) {
        assertEquals(lst1.size(), lst2.size());

        for (int i=0; i<lst1.size(); ++i) {
            SPTarget t1 = lst1.get(i);
            SPTarget t2 = lst2.get(i);

            // The two targets won't compare .equals(), but we can just check
            // the coordinates and get enough of an idea that they are the
            // same for the purposes of testing GuideProbeTargets.

            assertEquals(t1.getTarget().getRa().getAs(CoordinateParam.Units.DEGREES), t2.getTarget().getRa().getAs(CoordinateParam.Units.DEGREES), 0.000001);
            assertEquals(t1.getTarget().getDec().getAs(CoordinateParam.Units.DEGREES), t2.getTarget().getDec().getAs(CoordinateParam.Units.DEGREES), 0.000001);
        }
    }
}
