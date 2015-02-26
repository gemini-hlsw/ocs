//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Set;

/**
 * Tests parsing pre-2010B science programs.
 */
public class Pre2010BTargetEnvironmentIoTest {

    private static ParamSet load(String name) throws Exception {
        URL url = Pre2010BTargetEnvironmentIoTest.class.getResource(name);
        File f = new File(url.toURI());
        return (ParamSet) PioXmlUtil.read(f);
    }

    private static TargetEnvironment parse(String name) throws Exception {
        return TargetEnvironment.fromParamSet(load(name));
    }

    // We're not trying to verify the entire SPTarget parsing code.  It's
    // enough to know that the name is what we expected.
    private static void verifyTarget(String name, SPTarget target) throws Exception {
        assertEquals(name, target.getTarget().getName());
    }

    private enum Case {
        BASE_ONLY("IoBaseOnly") {
            public void verify(TargetEnvironment env) throws Exception {
                verifyTarget("BaseName", env.getBase());
                assertEquals(0, env.getUserTargets().size());

                // No guide groups
                assertEquals(0, env.getGuideEnvironment().getOptions().size());
            }
        },

        ONE_GUIDE_PROBE_TARGET("IoOneGuideProbeTarget") {
            public void verify(TargetEnvironment env) throws Exception {
                verifyTarget("BaseName", env.getBase());
                assertEquals(0, env.getUserTargets().size());

                GuideEnvironment genv = env.getGuideEnvironment();

                // PWFS2 is active.
                Set<GuideProbe> active = genv.getActiveGuiders();
                assertEquals(1, active.size());
                assertEquals(PwfsGuideProbe.pwfs2, active.iterator().next());

                // There is one guide group
                assertEquals(1, genv.getOptions().size());

                // It is the primary group option
                GuideGroup grp = genv.getPrimary().getValue();
                assertSame(genv.getOptions().head(), grp);

                // It contains a single GuideProbeTargets instance
                assertEquals(1, grp.getAll().size());

                Option<GuideProbeTargets> gptOpt = grp.get(PwfsGuideProbe.pwfs2);
                GuideProbeTargets gpt = gptOpt.getValue();

                // Which contains a single target
                assertEquals(1, gpt.getOptions().size());

                SPTarget primary = gpt.getPrimary().getValue();
                verifyTarget("GSC001", primary);
            }
        },

        TWO_GUIDE_PROBE_TARGET("IoTwoGuideProbeTarget") {
            public void verify(TargetEnvironment env) throws Exception {
                GuideEnvironment genv = env.getGuideEnvironment();

                // PWFS1, PWFS2 is active.
                Set<GuideProbe> active = genv.getActiveGuiders();
                assertEquals(2, active.size());
                assertTrue(active.contains(PwfsGuideProbe.pwfs1));
                assertTrue(active.contains(PwfsGuideProbe.pwfs2));

                GuideGroup grp = genv.getPrimary().getValue();
                Option<GuideProbeTargets> gpt1Opt = grp.get(PwfsGuideProbe.pwfs1);
                Option<GuideProbeTargets> gpt2Opt = grp.get(PwfsGuideProbe.pwfs2);

                SPTarget primary1 = gpt1Opt.getValue().getPrimary().getValue();
                verifyTarget("GSC001", primary1);

                SPTarget primary2 = gpt2Opt.getValue().getPrimary().getValue();
                verifyTarget("GSC002", primary2);
            }
        },

        DISABLED_GUIDE_PROBE_TARGET("IoDisabledGuideProbeTarget") {
            public void verify(TargetEnvironment env) throws Exception {
                GuideEnvironment genv = env.getGuideEnvironment();

                // nothing active
                Set<GuideProbe> active = genv.getActiveGuiders();
                assertEquals(0, active.size());

                GuideGroup grp = genv.getPrimary().getValue();
                Option<GuideProbeTargets> gptOpt = grp.get(PwfsGuideProbe.pwfs2);

                SPTarget primary = gptOpt.getValue().getPrimary().getValue();
                verifyTarget("GSC001", primary);
            }
        }
        ;

        private final String name;
        Case(String name) { this.name = name; }
        public String fileName() { return name + ".xml"; }

        public abstract void verify(TargetEnvironment env) throws Exception;
    }

    @Test
    public void testIO() throws Exception {
        PioFactory fact = new PioXmlFactory();
        for (Case c : Case.values()) {
            try {
                TargetEnvironment env = parse(c.fileName());
                c.verify(env);
                c.verify(TargetEnvironment.fromParamSet(env.getParamSet(fact)));
            } catch (Exception ex) {
                System.err.println("Failed " + c.fileName());
                throw ex;
            }
        }
    }
}
