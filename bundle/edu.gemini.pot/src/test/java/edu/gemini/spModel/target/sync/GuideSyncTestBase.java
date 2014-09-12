//
// $
//

package edu.gemini.spModel.target.sync;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.OptionsList;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.test.SpModelTestBase;

/**
 * Test cases for {@link GuideSync}.
 */
abstract class GuideSyncTestBase extends SpModelTestBase {


    // TODO: GuideProbeTargets.isEnabled
    protected void verify(GuideProbe guider, boolean enabled) throws Exception {
        TargetEnvironment env = getTargetEnvironment();
        assertEquals(enabled, env.getGuideEnvironment().getActiveGuiders().contains(guider));
        /*
        Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        assertEquals(enabled, gtOpt.getValue().isEnabled());
        */
    }

    protected void verifyLink(GuideProbe guider, GuideOption opt, ISPNode provNode) throws Exception {
        IOffsetPosListProvider<OffsetPos> dataObj;
        //noinspection unchecked
        dataObj = (IOffsetPosListProvider<OffsetPos>) provNode.getDataObject();

        OffsetPosList<OffsetPos> posList = dataObj.getPosList();
        for (OffsetPos pos : posList) {
            GuideOption curOpt = pos.getLink(guider);
            assertEquals(opt, curOpt);
        }
    }

    protected void verifyLink(int index, GuideProbe guider, GuideOption opt, ISPNode provNode) throws Exception {
        IOffsetPosListProvider<OffsetPos> dataObj;
        //noinspection unchecked
        dataObj = (IOffsetPosListProvider<OffsetPos>) provNode.getDataObject();

        OffsetPosList<OffsetPos> posList = dataObj.getPosList();
        OffsetPos pos = posList.getPositionAt(index);
        GuideOption curOpt = pos.getLink(guider);
        assertEquals(opt, curOpt);
    }

    protected void addGuider(GuideProbe guider) throws Exception {
        // Find the target, add a guider
        TargetObsComp targetDataObj = (TargetObsComp) getTarget().getDataObject();
        assertNotNull(targetDataObj);

        TargetEnvironment env = targetDataObj.getTargetEnvironment();
        GuideProbeTargets gt = GuideProbeTargets.create(guider, new SPTarget());
        env = env.putPrimaryGuideProbeTargets(gt);
        targetDataObj.setTargetEnvironment(env);
        getTarget().setDataObject(targetDataObj);
    }

    protected void rmGuider(GuideProbe guider) throws Exception {
        // Find the target component, remove the guider
        TargetObsComp targetDataObj = (TargetObsComp) getTarget().getDataObject();
        TargetEnvironment env = targetDataObj.getTargetEnvironment();
        GuideGroup grp = env.getOrCreatePrimaryGuideGroup();
        ImList<GuideProbeTargets> gtList = grp.getAll().remove(GuideProbeTargets.match(guider));
        env = env.setPrimaryGuideGroup(grp.setAll(gtList));

        targetDataObj.setTargetEnvironment(env);
        getTarget().setDataObject(targetDataObj);
    }

    protected void togglePrimaryGuider(GuideProbe guider) throws Exception {
        // Find the target, toggle the primary status  guider
        TargetObsComp targetDataObj = (TargetObsComp) getTarget().getDataObject();
        TargetEnvironment env = targetDataObj.getTargetEnvironment();
        Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
        GuideProbeTargets gt = gtOpt.getValue();
        Option<SPTarget> primaryOpt = gt.getPrimary();
        SPTarget primary;
        if (primaryOpt.isEmpty()) {
            primary = gt.getOptions().head();
        } else {
            primary = primaryOpt.getValue();
        }
        gt = gt.update(OptionsList.UpdateOps.togglePrimary(primary));
        targetDataObj.setTargetEnvironment(env.putPrimaryGuideProbeTargets(gt));
        getTarget().setDataObject(targetDataObj);
    }
}