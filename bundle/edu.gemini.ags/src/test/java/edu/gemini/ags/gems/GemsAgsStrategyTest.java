package edu.gemini.ags.gems;

import jsky.catalog.skycat.SkycatConfigFile;
import org.junit.Before;
import org.junit.Ignore;

import java.net.URL;

/**
 */
@Ignore
public class GemsAgsStrategyTest {
    @Before
    public void init() {
        // Set the default catalog config file URL (override with -Djsky.catalog.skycat.config=...)
        URL url = getClass().getResource("/resources/conf/test.skycat.cfg");
        assert(url != null);
        SkycatConfigFile.setConfigFile(url);
    }

    /*
    @Test
    public void testShouldWork() throws IOException {
        SPTarget base = new SPTarget();
        base.noNotifySetXYFromString("00:52:43.722", "-26:34:35.97");
        TargetEnvironment env = TargetEnvironment.create(base);
        Gsaoi inst = new Gsaoi();
        ObsContext context = ObsContext.create(env, inst, SPSiteQuality.Conditions.WORST, null, null);
        assertEquals(AgsEstimate.GUARANTEED_SUCCESS, AgsEstimator.instance.estimate(context, new AgsSkycatCatalogServer("NOMAD1@CDS")));
    }

    @Test
    public void testShouldNotWork() throws IOException {
        SPTarget base = new SPTarget(0.0, 0.0);
        TargetEnvironment env = TargetEnvironment.create(base);
        Gsaoi inst = new Gsaoi();
        ObsContext context = ObsContext.create(env, inst, SPSiteQuality.Conditions.BEST, null, null);
        assertEquals(AgsEstimate.COMPLETE_FAILURE, AgsEstimator.instance.estimate(context, new AgsSkycatCatalogServer("NOMAD1@CDS")));
    }
    */
}
