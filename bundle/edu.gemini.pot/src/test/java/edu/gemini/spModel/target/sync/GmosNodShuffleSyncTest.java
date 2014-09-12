////
//// $
////
//
//package edu.gemini.spModel.target.sync;
//
//import edu.gemini.pot.sp.ISPObsComponent;
//import edu.gemini.spModel.data.IOffsetPosListProvider;
//import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
//import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
//import edu.gemini.spModel.guide.GuideOption;
//import edu.gemini.spModel.guide.GuideProbe;
//import edu.gemini.spModel.target.offset.OffsetPosBase;
//import edu.gemini.spModel.target.offset.OffsetPosList;
//import org.junit.Ignore;
//
//
///**
// * Test cases for {@link edu.gemini.spModel.target.sync.GuideSync}.
// *
// * <p>HLPG_PROJECT_BASE property must be set.
// */
//@Ignore public final class GmosNodShuffleSyncTest extends GuideSyncTestBase {
//
//    private static final GuideProbe GUIDER = GmosOiwfsGuideProbe.instance;
//
//    private ISPObsComponent gmos;
//
//    public void setUp() throws Exception {
//        super.setUp();
//        gmos = addObsComponent(InstGmosSouth.SP_TYPE);
//    }
//
//    /**
//     * Tests that links are added and removed as needed when guide stars are
//     * added/removed from the target environment.
//     */
//    @Ignore public void testLinkSync() throws Exception {
//
//        // Get the gmos data object.
//        IOffsetPosListProvider<OffsetPosBase> dobj;
//        //noinspection unchecked
//        dobj = (IOffsetPosListProvider<OffsetPosBase>) gmos.getDataObject();
//
//        // Add an offset position to the list.
//        OffsetPosList<OffsetPosBase> posList = dobj.getPosList();
//        posList.addPosition(1.0, 1.0);
//
//        // Store the changes (posList points to an object held by the data obj :(
//        gmos.setDataObject(dobj);
//
//        // Confirm that there is no link for the guider.
//        verifyLink(GUIDER, null, gmos);
//
//        // Add the guider to the target environment.
//        addGuider(GUIDER);
//
//        // Confirm that there is a link for the guider.
//        GuideOption active = GUIDER.getGuideOptions().getDefaultActive();
//        verifyLink(GUIDER, active, gmos);
//
//        // Remove the primary designation from the target.
//        GuideOption inactive = GUIDER.getGuideOptions().getDefaultInactive();
//        togglePrimaryGuider(GUIDER);
//        verifyLink(GUIDER, inactive, gmos);
//
//        // Remove the guider from the target environment.
//        rmGuider(GUIDER);
//        verifyLink(GUIDER, null, gmos);
//    }
//}