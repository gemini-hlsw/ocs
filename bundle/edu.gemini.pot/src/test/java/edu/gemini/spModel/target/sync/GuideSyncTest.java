//
// $
//

package edu.gemini.spModel.target.sync;

import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

/**
 * Test cases for {@link GuideSync}.
 *
 * <p>HLPG_PROJECT_BASE property must be set.
 */
public final class GuideSyncTest extends GuideSyncTestBase {

    private static final GuideProbe GUIDER = Flamingos2OiwfsGuideProbe.instance;
    private static final GuideProbe GUIDER2 = PwfsGuideProbe.pwfs2;

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void setUp() throws Exception {
        super.setUp();

        // Add an instrument that has an OIWFS.
        addObsComponent(Flamingos2.SP_TYPE);
    }

    // TODO: GuideProbeTargets.isEnabled
    /**
     * Ensures that removing a guide star provider should disable any
     * associated guide stars.
     */
    public void testDisable() throws Exception {
        // Start out with the guide targets enabled.
        addGuider(GUIDER);
        verify(GUIDER, true);

        // Remove the instrument and the guide targets get disabled.
        removeObsComponent(Flamingos2.SP_TYPE);
        verify(GUIDER, false);

        // Add a new flamingos 2 and the target should be enabled again.
        addObsComponent(Flamingos2.SP_TYPE);
        verify(GUIDER, true);
    }

    /*
    private void addOffsetPos(ISPSeqComponent offsetIter) throws Exception {
        // Add an offset position to the list.
        SeqRepeatOffset dataObj = (SeqRepeatOffset) offsetIter.getDataObject();
        OffsetPosList<OffsetPos> posList = dataObj.getPosList();

        int size = posList.size();

        // p & q are set to the step number here.  The could be 0 or any value.
        OffsetPos pos = new OffsetPos("pos" + size, size, size);

        posList.addPosition(pos);

        // Store the changes (posList points to an object held by the data obj :(
        offsetIter.setDataObject(dataObj);
    }

    private void setGuideOptions(ISPSeqComponent offsetIter, int posIndex, GuideProbe guider, GuideOption opt) throws Exception {
        SeqRepeatOffset dataObj = (SeqRepeatOffset) offsetIter.getDataObject();
        OffsetPosList<OffsetPos> posList = dataObj.getPosList();
        OffsetPos pos = posList.getPositionAt(posIndex);
        pos.setLink(guider, opt);
        offsetIter.setDataObject(dataObj);
    }
*/

    /**
     * Tests that links are added and removed as needed when guide stars are
     * added/removed from the target environment.
     */
/*
    @Ignore public void testLinkSync() throws Exception {

        // Add an offset iterator.
        ISPSeqComponent offsetIter;
        offsetIter = addSeqComponent(getObs().getSeqComponent(), SeqRepeatOffset.SP_TYPE);

        // Add an offset position to the list.
        addOffsetPos(offsetIter);

        // Confirm that there is no link for the guider.
        verifyLink(GUIDER, null, offsetIter);

        // Add the guider to the target environment.
        addGuider(GUIDER);

        // Confirm that there is a link for the guider.
        GuideOption active = GUIDER.getGuideOptions().getDefaultActive();
        verifyLink(GUIDER, active, offsetIter);

        // Remove the primary designation from the target.
        GuideOption inactive = GUIDER.getGuideOptions().getDefaultOff();
        togglePrimaryGuider(GUIDER);
        verifyLink(GUIDER, inactive, offsetIter);

        // Remove the guider from the target environment.
        rmGuider(GUIDER);
        verifyLink(GUIDER, null, offsetIter);
    }
*/
    /**
     * Tests that "sky" positions.
     */
/*
    @Ignore public void testSkyPositions() throws Exception {

        // Add an offset iterator.
        ISPSeqComponent offsetIter;
        offsetIter = addSeqComponent(getObs().getSeqComponent(), SeqRepeatOffset.SP_TYPE);

        // Add an offset position to the list.
        addOffsetPos(offsetIter);

        // Confirm that there is no link for the guider.
        verifyLink(GUIDER, null, offsetIter);

        // Add the guider to the target environment.
        addGuider(GUIDER);

        // Confirm that there is a link for the guider.
        GuideOption active = GUIDER.getGuideOptions().getDefaultActive();
        verifyLink(GUIDER, active, offsetIter);

        // Turn off guiding for this position.
        GuideOption inactive = GUIDER.getGuideOptions().getDefaultOff();
        setGuideOptions(offsetIter, 0, GUIDER, inactive);
        verifyLink(GUIDER, inactive, offsetIter);

        // Add a new guider to the target environment.
        addGuider(GUIDER2);

        // Confirm that there is an inactive link for the guider.  Since all
        // existing guiders were turned off when the new position was added.
        verifyLink(GUIDER2, inactive, offsetIter);
    }
*/

    /**
     * Tests "moving" a guider star from one guider to another.
     */
/*
    @Ignore public void testOdgwPositions() throws Exception {
        GuideOption active   = GsaoiOdgw.odgw1.getGuideOptions().getDefaultActive();
        GuideOption inactive = GsaoiOdgw.odgw1.getGuideOptions().getDefaultOff();

        GuideProbe odgw1 = GsaoiOdgw.odgw1;
        GuideProbe odgw2 = GsaoiOdgw.odgw2;

        // Add an offset iterator.
        ISPSeqComponent offsetIter;
        offsetIter = addSeqComponent(getObs().getSeqComponent(), SeqRepeatOffset.SP_TYPE);

        // Add an offset position to the list.
        addOffsetPos(offsetIter);

        // Confirm that there is no link for the guider.
        verifyLink(odgw1, null, offsetIter);
        verifyLink(odgw2, null, offsetIter);

        // Add the guider to the target environment.
        addGuider(odgw1);
        verifyLink(odgw1, active, offsetIter);
        verifyLink(odgw2, null, offsetIter);

        // Turn off guiding for odgw1
        setGuideOptions(offsetIter, 0, odgw1, inactive);
        verifyLink(odgw1, inactive, offsetIter);

        // Remove odgw1 and add odgw2 in the same step.  This simulates what
        // happens when dragging a star from one detector window to another.
        TargetObsComp targetDataObj = (TargetObsComp) getTarget().getDataObject();
        TargetEnvironment env = targetDataObj.getTargetEnvironment();
        GuideProbeTargets gt = GuideProbeTargets.create(odgw2, new SPTarget());

        ImList<GuideProbeTargets> gtList = DefaultImList.create(gt);
        env = env.setAllPrimaryGuideProbeTargets(gtList);
        targetDataObj.setTargetEnvironment(env);
        getTarget().setDataObject(targetDataObj);

        // Confirm that there is a link for the guider.
        verifyLink(odgw1, null, offsetIter);
        verifyLink(odgw2, inactive, offsetIter);
    }
*/

    /**
     * Tests inheritance of guide positions.
     */
/*
    @Ignore public void testInheritance() throws Exception {
        GuideOption active   = GsaoiOdgw.odgw1.getGuideOptions().getDefaultActive();
        GuideOption inactive = GsaoiOdgw.odgw1.getGuideOptions().getDefaultOff();

        GuideProbe odgw1 = GsaoiOdgw.odgw1;
        GuideProbe odgw2 = GsaoiOdgw.odgw2;

        // Add an offset iterator.
        ISPSeqComponent offsetIter;
        offsetIter = addSeqComponent(getObs().getSeqComponent(), SeqRepeatOffset.SP_TYPE);

        // Add an offset position to the list.
        addOffsetPos(offsetIter);

        // Confirm that there is no link for the guider.
        verifyLink(odgw1, null, offsetIter);
        verifyLink(odgw2, null, offsetIter);

        // Add the guiders to the target environment.
        addGuider(odgw1);
        addGuider(odgw2);
        verifyLink(odgw1, active, offsetIter);
        verifyLink(odgw2, active, offsetIter);

        // Turn off guiding for odgw1
        setGuideOptions(offsetIter, 0, odgw1, inactive);
        verifyLink(odgw1, inactive, offsetIter);
        verifyLink(odgw2, active, offsetIter);

        // Add another offset position and manually update the offset list.
        // This needs to be done automatically in a future revision.
        addOffsetPos(offsetIter);
        TargetObsComp targetDataObj = (TargetObsComp) getTarget().getDataObject();
        TargetEnvironment env = targetDataObj.getTargetEnvironment();
        SeqRepeatOffset dataObj = (SeqRepeatOffset) offsetIter.getDataObject();
//        assertTrue(GuideSync.updatePosList(dataObj.getPosList(), new Some<TargetEnvironment>(env)));
        offsetIter.setDataObject(dataObj);

        // Confirm that there is a link for the guider.
        verifyLink(odgw1, inactive, offsetIter);
        verifyLink(odgw2, active, offsetIter);
    }
*/

    /**
     * Tests deactivating when toggling off the primary guide star.
     */
/*
    @Ignore public void testToggle() throws Exception {
        GuideOption guide  = StandardGuideOptions.Value.guide;
        GuideOption park   = StandardGuideOptions.Value.park;
        GuideOption freeze = StandardGuideOptions.Value.freeze;

        // Add an offset iterator.
        ISPSeqComponent offsetIter;
        offsetIter = addSeqComponent(getObs().getSeqComponent(), SeqRepeatOffset.SP_TYPE);

        // Add 3 offset positions to the list.
        addOffsetPos(offsetIter);
        addOffsetPos(offsetIter);
        addOffsetPos(offsetIter);

        // Confirm that there is no link for the guider.
        verifyLink(GUIDER, null, offsetIter);

        // Add the guide star.
        addGuider(GUIDER);

        // Should star out with all links guiding.
        verifyLink(GUIDER, guide, offsetIter);

        // Set position 1 to park and position 2 to freeze
        setGuideOptions(offsetIter, 1, GUIDER, park);
        setGuideOptions(offsetIter, 2, GUIDER, freeze);
        verifyLink(0, GUIDER, guide,  offsetIter);
        verifyLink(1, GUIDER, park,   offsetIter);
        verifyLink(2, GUIDER, freeze, offsetIter);

        // Now toggle off the primary guider.
        togglePrimaryGuider(GUIDER);

        // Everything parks.
        verifyLink(GUIDER, park, offsetIter);

        // Toggle it back on.  Everything is restored.
        togglePrimaryGuider(GUIDER);
        verifyLink(0, GUIDER, guide,  offsetIter);
        verifyLink(1, GUIDER, park,   offsetIter);
        verifyLink(2, GUIDER, freeze, offsetIter);
    }
*/
}