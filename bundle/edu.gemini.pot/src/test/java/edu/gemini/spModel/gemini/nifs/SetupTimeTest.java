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
            public void init(final Context ctx) throws Exception {
                ensureNifs(ctx);
                removeComponent(ctx.obs, InstAltair.SP_TYPE);
            }
            public double compute(final double baseTime) {
                return NifsSetupTimeService.BASE_SETUP_TIME_SEC;
            }
        },
        altairNgs() {
            public void init(final Context ctx) throws Exception {
                ensureNifs(ctx);
                ensureAltair(ctx);
                setMode(ctx, AltairParams.Mode.NGS);
            }
            public double compute(final double baseTime) {
                return NifsSetupTimeService.BASE_SETUP_TIME_SEC;
            }
        },
        altairLgs() {
            public void init(final Context ctx) throws Exception {
                ensureNifs(ctx);
                ensureAltair(ctx);
                setMode(ctx, AltairParams.Mode.LGS);
            }
            public double compute(final double baseTime) {
                return NifsSetupTimeService.BASE_LGS_SETUP_TIME_SEC;
            }
        },
        ;

        private static void ensureNifs(final Context ctx) throws Exception {
            if (lookup(ctx.obs, InstNIFS.SP_TYPE) == null) {
                ctx.obs.addObsComponent(ctx.nifsComponent);
            }
        }

        private static void ensureAltair(final Context ctx) throws Exception {
            if (lookup(ctx.obs, InstAltair.SP_TYPE) == null) {
                ctx.obs.addObsComponent(ctx.altairComponent);
            }
        }

        private static void setMode(final Context ctx, final AltairParams.Mode mode) throws Exception {
            final InstAltair dataObj = (InstAltair) ctx.altairComponent.getDataObject();
            dataObj.setMode(mode);
            ctx.altairComponent.setDataObject(dataObj);
        }
    }

    enum CoronographyOption implements Option {
        notCoronography() {
            public void init(final Context ctx) throws Exception {
                final InstNIFS nifs = (InstNIFS) ctx.nifsComponent.getDataObject();
                nifs.setMask(NIFSParams.Mask.CLEAR);
                ctx.nifsComponent.setDataObject(nifs);
            }
            public double compute(final double baseTime) {
                return baseTime;
            }
        },
        coronography() {
            public void init(final Context ctx) throws Exception {
                final InstNIFS nifs = (InstNIFS) ctx.nifsComponent.getDataObject();
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
            public void init(final Context ctx) throws Exception {
                removeComponent(ctx.obs, TargetObsComp.SP_TYPE);
            }
            public double compute(double baseTime) {
                return baseTime;
            }
        },
        noOiwfs() {
            public void init(final Context ctx) throws Exception {
                // Add the target component if it doesn't exist.
                if (lookup(ctx.obs, TargetObsComp.SP_TYPE) == null) {
                    ctx.obs.addObsComponent(ctx.targetComponent);
                }

                // Get the target environment.
                final TargetObsComp dataObj = (TargetObsComp) ctx.targetComponent.getDataObject();
                final TargetEnvironment env = dataObj.getTargetEnvironment();

                // Remove the OIWFS if it exists.
                final GuideGroup grp = env.getPrimaryGuideGroup();
                final ImList<GuideProbeTargets> gtList = grp.getAll().remove(gpt -> gpt.getGuider() == NifsOiwfsGuideProbe.instance);
                final TargetEnvironment env2 = env.setPrimaryGuideGroup(grp.setAll(gtList));

                dataObj.setTargetEnvironment(env2);
                ctx.targetComponent.setDataObject(dataObj);
            }
            public double compute(final double baseTime) {
                return baseTime;
            }
        },
        oiwfs() {
            public void init(final Context ctx) throws Exception {
                // Add the target compnent if it doesn't exist.
                if (lookup(ctx.obs, TargetObsComp.SP_TYPE) == null) {
                    ctx.obs.addObsComponent(ctx.targetComponent);
                }

                // Get the target environment
                final TargetObsComp dataObj = (TargetObsComp) ctx.targetComponent.getDataObject();
                final TargetEnvironment env = dataObj.getTargetEnvironment();

                // Add the OIWFS if it doesn't exist.
                final SPTarget target = new SPTarget();
                final GuideProbeTargets gt = GuideProbeTargets.create(NifsOiwfsGuideProbe.instance, target);
                final TargetEnvironment env2 = env.putPrimaryGuideProbeTargets(gt);
                dataObj.setTargetEnvironment(env2);
                ctx.targetComponent.setDataObject(dataObj);
            }
            public double compute(final double baseTime) {
                return baseTime + NifsSetupTimeService.OIWFS_SETUP_SEC;
            }
        }
    }

    private static void removeComponent(final ISPObservation obs, final SPComponentType type) throws Exception {
        final List<ISPObsComponent> obsComps = obs.getObsComponents();
        final Iterator<ISPObsComponent> it = obsComps.iterator();
        while (it.hasNext()) {
            ISPObsComponent comp = it.next();
            if (type.equals(comp.getType())) {
                it.remove();
                break;
            }
        }
        obs.setObsComponents(obsComps);
    }

    private static ISPObsComponent lookup(final ISPObservation obs, final SPComponentType type) throws Exception {
        return obs.getObsComponents().stream()
                .filter(obsComp -> type.equals(obsComp.getType()))
                .findFirst().orElse(null);
    }

    private Context ctx;
    private IDBDatabaseService odb;

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Before
    public void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();

        final SPProgramID progId = SPProgramID.toProgramID("GS-2009B-Q-1");
        final ISPProgram prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);

        ctx = new Context();
        ctx.obs = odb.getFactory().createObservation(prog, Instrument.none, null);

        final List<ISPObsComponent> emptyObsComponents = Collections.emptyList();
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

    private void verify(final String name, final double expectedTime) throws Exception {
        final double actualTime = NifsSetupTimeService.getSetupTimeSec(ctx.obs);
        assertEquals(name, expectedTime, actualTime, 0.00001);
    }

    @Test
    public void testNoInstrument() throws Exception {
        verify("no instrument", 0);
    }

    @Test
    public void testSetupTimes() throws Exception {
        for (final BaseOption baseOpt : BaseOption.values()) {
            baseOpt.init(ctx);
            for (final OiwfsOption oiwfsOpt : OiwfsOption.values()) {
                oiwfsOpt.init(ctx);
                for (final CoronographyOption corOpt : CoronographyOption.values()) {
                    corOpt.init(ctx);
                    final String name = String.format("(%s, %s, %s)", baseOpt.name(), oiwfsOpt.name(), corOpt.name());
                    final double time = corOpt.compute(oiwfsOpt.compute(baseOpt.compute(0)));
                    verify(name, time);
                }
            }
        }
    }
}
