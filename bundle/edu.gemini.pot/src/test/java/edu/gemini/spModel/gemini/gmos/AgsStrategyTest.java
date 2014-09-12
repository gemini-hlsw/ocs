package edu.gemini.spModel.gemini.gmos;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;
import edu.gemini.shared.util.immutable.None;

/**
 * TODO: Why does this file exist?
 */
public class AgsStrategyTest {

    private ObsContext baseContext;
    private TargetEnvironment env;

    protected void setUp() throws Exception {
        SPTarget base = new SPTarget(0.0, 0.0);
        env = TargetEnvironment.create(base);

        InstGmosNorth inst = new InstGmosNorth();
        inst.setPosAngle(0);
        inst.setIssPort(IssPort.UP_LOOKING);

        baseContext = ObsContext.create(env, inst, None.<Site>instance(), SPSiteQuality.Conditions.BEST, null, null);
    }


    @Test
    public void usesPwfsOnNonsidereal() {

    }
}
