//
// $
//

package edu.gemini.spModel.target.offset;

import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCSECS;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.guide.DefaultGuideOptions;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideOption;
import junit.framework.TestCase;

import java.util.Set;

/**
 * Test cases for {@link OffsetUtil}.
 */
public class OffsetUtilTest extends TestCase {
    private static final GuideProbe GUIDER1 = GsaoiOdgw.odgw1;
    private static final GuideProbe GUIDER2 = GsaoiOdgw.odgw2;

    private OffsetPosList<OffsetPos> emptyPosList;
    private OffsetPosList<OffsetPos> singlePosList;
    private OffsetPosList<OffsetPos> twoPosList;

    private Offset offset0;
    private Offset offset1;

    private OffsetPos addPosition(OffsetPosList<OffsetPos> lst, int index) {
        final OffsetPos pos = lst.addPosition(index, index);
        pos.setLink(GUIDER1, GsaoiOdgw.odgw1.getGuideOptions().getDefaultActive());
        pos.setLink(GUIDER2, GsaoiOdgw.odgw2.getGuideOptions().getDefaultActive());
        return pos;
    }

    protected void setUp() {
        emptyPosList  = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        singlePosList = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        twoPosList    = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);

        addPosition(singlePosList, 0);
        addPosition(twoPosList, 0);
        addPosition(twoPosList, 1);

        offset0 = new Offset(new Angle(0, ARCSECS), new Angle(0, ARCSECS));
        offset1 = new Offset(new Angle(1, ARCSECS), new Angle(1, ARCSECS));
    }

    public void testGetOffsets() throws Exception {
        Set<Offset> res = OffsetUtil.getOffsets((OffsetPosList[])null);
        assertEquals(0, res.size());

        res = OffsetUtil.getOffsets(new OffsetPosList[] {});
        assertEquals(0, res.size());

        res = OffsetUtil.getOffsets(new OffsetPosList[] {emptyPosList});
        assertEquals(0, res.size());

        res = OffsetUtil.getOffsets(new OffsetPosList[] {emptyPosList, singlePosList});
        assertEquals(1, res.size());
        assertEquals(offset0, res.iterator().next());

        res = OffsetUtil.getOffsets(new OffsetPosList[] {singlePosList, twoPosList});
        assertEquals(2, res.size());
        assertTrue(res.contains(offset0));
        assertTrue(res.contains(offset1));
    }

    public void testOptionGetOffsets() throws Exception {
        Option<OffsetPosList[]> none = None.instance();
        Set<Offset> res = OffsetUtil.getOffsets(none);
        assertEquals(0, res.size());

        res = OffsetUtil.getOffsets(new Some<OffsetPosList[]>(new OffsetPosList[] {}));
        assertEquals(0, res.size());

        res = OffsetUtil.getOffsets(new Some<OffsetPosList[]>(new OffsetPosList[] {emptyPosList}));
        assertEquals(0, res.size());

        res = OffsetUtil.getOffsets(new Some<OffsetPosList[]>(new OffsetPosList[] {emptyPosList, singlePosList}));
        assertEquals(1, res.size());
        assertEquals(offset0, res.iterator().next());

        res = OffsetUtil.getOffsets(new Some<OffsetPosList[]>(new OffsetPosList[] {singlePosList, twoPosList}));
        assertEquals(2, res.size());
        assertTrue(res.contains(offset0));
        assertTrue(res.contains(offset1));
    }

    public void testGetScienceOffsets() throws Exception {
        Set<Offset> res = OffsetUtil.getSciencePositions((OffsetPosList[])null);
        assertEquals(1, res.size());
        assertTrue(res.contains(offset0));

        res = OffsetUtil.getSciencePositions(new OffsetPosList[] {});
        assertEquals(1, res.size());
        assertTrue(res.contains(offset0));

        res = OffsetUtil.getSciencePositions(new OffsetPosList[] {emptyPosList});
        assertEquals(1, res.size());
        assertTrue(res.contains(offset0));

        OffsetPosList<OffsetPos> pl = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        addPosition(pl, 1);
        res = OffsetUtil.getSciencePositions(new OffsetPosList[] {pl});
        assertEquals(1, res.size());
        assertTrue(res.contains(offset1));
    }

    public void testFilterSkyPositions() throws Exception {
        OffsetPosList<OffsetPos> pl = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        final OffsetPos pos0 = addPosition(pl, 0);
        final OffsetPos pos1 = addPosition(pl, 1);
        final OffsetPos pos2 = addPosition(pl, 2);

        GuideOption inactive = GUIDER1.getGuideOptions().getDefaultInactive();
        pos0.setLink(GUIDER1, inactive);
        pos1.setLink(GUIDER2, inactive);
        pos2.setDefaultGuideOption(DefaultGuideOptions.Value.off);
        pos2.setLink(GUIDER1, inactive);
        pos2.setLink(GUIDER2, inactive);

        Set<Offset> res = OffsetUtil.getSciencePositions(new OffsetPosList[] {pl});
        assertEquals(2, res.size());
        assertTrue(res.contains(offset0));
        assertTrue(res.contains(offset1));
    }

    public void testFilterAllPositions() throws Exception {

        OffsetPosList<OffsetPos> pl = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        final OffsetPos pos1 = addPosition(pl, 1);
        final OffsetPos pos2 = addPosition(pl, 2);

        GuideOption inactive = GUIDER1.getGuideOptions().getDefaultInactive();
        pos1.setDefaultGuideOption(DefaultGuideOptions.Value.off);
        pos1.setLink(GUIDER1, inactive);
        pos1.setLink(GUIDER2, inactive);
        pos2.setDefaultGuideOption(DefaultGuideOptions.Value.off);
        pos2.setLink(GUIDER1, inactive);
        pos2.setLink(GUIDER2, inactive);

        Set<Offset> res = OffsetUtil.getSciencePositions(new OffsetPosList[] {pl});
        assertEquals(1, res.size());
        assertTrue(res.contains(offset0));
    }
}
