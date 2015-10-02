//
// $
//

package edu.gemini.spModel.gemini.nifs;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Test cases for NIFS setup time calculations.
 *
 * <p>HLPG_PROJECT_BASE property must be set.
 */
public final class SetupTimeTest {
    private static class Context {
        ISPObservation obs;
        ISPObsComponent nifsComponent;
        ISPObsComponent targetComponent;
        ISPObsComponent altairComponent;
    }

    interface Option {
        void init(Context ctx) throws Exception;
        double compute(double baseTime);
    }

    enum BaseOption implements Option {
        noAltair() {
            public void init(Context ctx) throws Exception {
                ensureNifs(ctx);
                removeComponent(ctx.obs, InstAltair.SP_TYPE);
            }
            public double compute(double baseTime) {
                return NifsSetupTimeService.BASE_SETUP_TIME_SEC;
            }
        },
        altairNgs() {
            public void init(Context ctx) throws Exception {
                ensureNifs(ctx);
                ensureAltair(ctx);
                setMode(ctx, AltairParams.Mode.NGS);
            }
            public double compute(double baseTime) {
                return NifsSetupTimeService.BASE_SETUP_TIME_SEC;
            }
        },
        altairLgs() {
            public void init(Context ctx) throws Exception {
                ensureNifs(ctx);
                ensureAltair(ctx);
                setMode(ctx, AltairParams.Mode.LGS);
            }
            public double compute(double baseTime) {
                return NifsSetupTimeService.BASE_LGS_SETUP_TIME_SEC;
            }
        },
        ;

        private static void ensureNifs(Context ctx) throws Exception {
            if (lookup(ctx.obs, InstNIFS.SP_TYPE) == null) {
                ctx.obs.addObsComponent(ctx.nifsComponent);
            }
        }

        private static void ensureAltair(Context ctx) throws Exception {
            if (lookup(ctx.obs, InstAltair.SP_TYPE) == null) {
                ctx.obs.addObsComponent(ctx.altairComponent);
            }
        }

        private static void setMode(Context ctx, AltairParams.Mode mode) throws Exception {
            InstAltair dataObj = (InstAltair) ctx.altairComponent.getDataObject();
            dataObj.setMode(mode);
            ctx.altairComponent.setDataObject(dataObj);
        }
    }

    enum CoronographyOption implements Option {
        notCoronography() {
            public void init(Context ctx) throws Exception {
                InstNIFS nifs = (InstNIFS) ctx.nifsComponent.getDataObject();
                nifs.setMask(NIFSParams.Mask.CLEAR);
                ctx.nifsComponent.setDataObject(nifs);
            }
            public double compute(double baseTime) {
                return baseTime;
            }
        },
        coronography() {
            public void init(Context ctx) throws Exception {
                InstNIFS nifs = (InstNIFS) ctx.nifsComponent.getDataObject();
                nifs.setMask(NIFSParams.Mask.OD_1);
                ctx.nifsComponent.setDataObject(nifs);
            }
            public double compute(double baseTime) {
                return baseTime + NifsSetupTimeService.CORONOGRAPHY_SETUP_SEC;
            }
        }
    }

    enum OiwfsOption implements Option {
        noTargetComp() {
            public void init(Context ctx) throws Exception {
                removeComponent(ctx.obs, TargetObsComp.SP_TYPE);
            }
            public double compute(double baseTime) {
                return baseTime;
            }
        },
        noOiwfs() {
            public void init(Context ctx) throws Exception {
                // Add the target component if it doesn't exist.
                if (lookup(ctx.obs, TargetObsComp.SP_TYPE) == null) {
                    ctx.obs.addObsComponent(ctx.targetComponent);
                }

                // Get the target environment.
                TargetObsComp dataObj = (TargetObsComp) ctx.targetComponent.getDataObject();
                TargetEnvironment env = dataObj.getTargetEnvironment();

                // Remove the OIWFS if it exists.
                GuideGroup grp = env.getOrCreatePrimaryGuideGroup();
                final ImList<GuideProbeTargets> gtList = grp.getAll().remove(gpt -> gpt.getGuider() == NifsOiwfsGuideProbe.instance);
                env = env.setPrimaryGuideGroup(grp.setAll(gtList));

                dataObj.setTargetEnvironment(env);
                ctx.targetComponent.setDataObject(dataObj);
            }
            public double compute(double baseTime) {
                return baseTime;
            }
        },
        oiwfs() {
            public void init(Context ctx) throws Exception {
                // Add the target compnent if it doesn't exist.
                if (lookup(ctx.obs, TargetObsComp.SP_TYPE) == null) {
                    ctx.obs.addObsComponent(ctx.targetComponent);
                }

                // Get the target environment
                TargetObsComp dataObj = (TargetObsComp) ctx.targetComponent.getDataObject();
                TargetEnvironment env = dataObj.getTargetEnvironment();

                // Add the OIWFS if it doesn't exist.
                final SPTarget target = new SPTarget();
                GuideProbeTargets gt = GuideProbeTargets.create(NifsOiwfsGuideProbe.instance, target).withExistingPrimary(target);
                env = env.putPrimaryGuideProbeTargets(gt);
                dataObj.setTargetEnvironment(env);
                ctx.targetComponent.setDataObject(dataObj);
            }
            public double compute(double baseTime) {
                return baseTime + NifsSetupTimeService.OIWFS_SETUP_SEC;
            }
        }
    }

    private static void removeComponent(ISPObservation obs, SPComponentType type) throws Exception {
        List<ISPObsComponent> obsComps = obs.getObsComponents();
        Iterator<ISPObsComponent> it = obsComps.iterator();
        while (it.hasNext()) {
            ISPObsComponent comp = it.next();
            if (type.equals(comp.getType())) {
                it.remove();
                break;
            }
        }
        obs.setObsComponents(obsComps);
    }

    private static ISPObsComponent lookup(ISPObservation obs, SPComponentType type) throws Exception {
        List<ISPObsComponent> obsComps = obs.getObsComponents();
        for (ISPObsComponent obsComp : obsComps) {
            if (type.equals(obsComp.getType())) return obsComp;
        }
        return null;
    }

    private Context ctx;
    private IDBDatabaseService odb;

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Before
    public void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();

        SPProgramID progId = SPProgramID.toProgramID("GS-2009B-Q-1");
        ISPProgram prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);

        ctx = new Context();
        ctx.obs = odb.getFactory().createObservation(prog, null);

        List<ISPObsComponent> emptyObsComponents = Collections.emptyList();
        ctx.obs.setObsComponents(emptyObsComponents);
        prog.addObservation(ctx.obs);

        ctx.nifsComponent   = odb.getFactory().createObsComponent(prog, InstNIFS.SP_TYPE, null);
        ctx.targetComponent = odb.getFactory().createObsComponent(prog, TargetObsComp.SP_TYPE, null);
        ctx.altairComponent = odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
    }

    @After
    public void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
    }

    private void verify(String name, double expectedTime) throws Exception {
        double actualTime = NifsSetupTimeService.getSetupTimeSec(ctx.obs);
        assertEquals(name, expectedTime, actualTime, 0.00001);
    }

    @Test
    public void testNoInstrument() throws Exception {
        verify("no instrument", 0);
    }

    @Test
    public void testSetupTimes() throws Exception {
        for (BaseOption baseOpt : BaseOption.values()) {
            baseOpt.init(ctx);
            for (OiwfsOption oiwfsOpt : OiwfsOption.values()) {
                oiwfsOpt.init(ctx);
                for (CoronographyOption corOpt : CoronographyOption.values()) {
                    corOpt.init(ctx);
                    String name = String.format("(%s, %s, %s)", baseOpt.name(), oiwfsOpt.name(), corOpt.name());
                    double time = corOpt.compute(oiwfsOpt.compute(baseOpt.compute(0)));
                    verify(name, time);
                }
            }
        }
    }
}
