/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: OffsetPosCase.java 19597 2009-05-04 15:36:57Z swalker $
 */

package edu.gemini.spModel.util.test;

import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.target.TelescopePosWatcher;
import edu.gemini.spModel.target.WatchablePos;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.telescope.IssPort;

import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class OffsetPosTest tests the OffsetPos and
 * OffsetPosList classes.
 */
public final class OffsetPosCase {

    private OffsetPos _t1;
    private OffsetPos _t2;
    private OffsetPosList<OffsetPos> _l1;

    class MyPosWatcher implements TelescopePosWatcher {

        int locUpdateCount = 0;
        int genericUpdateCount = 0;

        public void telescopePosLocationUpdate(WatchablePos tp) {
            locUpdateCount++;
        }

        public void telescopePosGenericUpdate(WatchablePos tp) {
            genericUpdateCount++;
        }

        public int getLocUpdateCount() {
            return locUpdateCount;
        }

        public int getGenericUpdateCount() {
            return genericUpdateCount;
        }
    }

    static final private double _ERROR = .00001;

    @Before
    public void setUp() throws Exception {
        _l1 = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        _t1 = _l1.addPosition();
        assertNotNull(_t1);
        assertTrue(_t1.isOffsetPosition());
        _t2 = _l1.addPosition();
        assertNotNull(_t2);
        assertTrue(_t2.isOffsetPosition());
    }

    // Setup an av list with three attributes each with three values
    // Tests set/add/fetch
    @Test
    public void testSetup() {
        // Should have size 2
        assertEquals(2, _l1.size());
    }

    // Test simple removal. Remove One and then the other.
    @Test
    public void testSimpleRemoval() {
        // Should have size 1
        assertEquals(2, _l1.size());
        // Test simple removal
        String tag1 = _t1.getTag();
        String tag2 = _t2.getTag();
        _l1.removePosition(_t2);
        assertEquals(1, _l1.size());
        assertTrue(_l1.exists(tag1));
        assertTrue(!_l1.exists(tag2));

        _l1.removePosition(_t1);
        assertEquals(0, _l1.size());
        assertTrue(!_l1.exists(tag1));
        assertTrue(!_l1.exists(tag2));
    }

    // Test change xy
    @Test
    public void testXYSetting() {
        String tag0 = _t1.getTag();
        OffsetPos off0 = _l1.getPositionAt(0);
        assertTrue(off0.getTag().equals(tag0));

        // Add watcher
        MyPosWatcher w = new MyPosWatcher();
        off0.addWatcher(w);

        off0.setXY(2.0, 3.0, IssPort.DEFAULT);
        assertEquals(1, w.getLocUpdateCount());
        assertEquals(0, w.getGenericUpdateCount());
    }

    // Test the static creation methods.
    @Test
    public void testCreation() {
        OffsetPos op;

        double x = 0.0;
        double y = 0.0;

        op = _l1.addPosition();
        assertNotNull(op);
        assertEquals(x, op.getXaxis(), _ERROR);
        assertEquals(y, op.getYaxis(), _ERROR);

        assertEquals(3, _l1.size());

        op = _l1.addPosition(0);
        assertNotNull(op);
        assertEquals(x, op.getXaxis(), _ERROR);
        assertEquals(y, op.getYaxis(), _ERROR);
        assertEquals(4, _l1.size());
        OffsetPos op2 = _l1.getPositionAt(0);
        assertEquals("New tag=", op.getTag(), op2.getTag());

        x = 22.2;
        y = 33.3;
        op = _l1.addPosition(2, x, y);
        assertNotNull(op);
        assertEquals(x, op.getXaxis(), _ERROR);
        assertEquals(y, op.getYaxis(), _ERROR);
        assertEquals(5, _l1.size());
        op2 = _l1.getPositionAt(2);
        assertEquals("New tag=", op.getTag(), op2.getTag());

        x = 1.75;
        y = -2.33;
        op = _l1.addPosition(x, y);
        assertNotNull(op);
        assertEquals(x, op.getXaxis(), _ERROR);
        assertEquals(y, op.getYaxis(), _ERROR);
        assertEquals(6, _l1.size());
    }

    // Test removePosition
    @Test
    public void testRemoval() {
        OffsetPos op;

        op = _l1.addPosition(0.0, 0.0);
        assertNotNull(op);

        op = _l1.addPosition(1.0, 1.0);
        assertNotNull(op);

        op = _l1.addPosition(2.0, 2.0);
        assertNotNull(op);

        assertEquals(5, _l1.size());
        OffsetPos oc = _l1.getPositionAt(0);
        assertEquals("Offset0", oc.getTag());
        oc = _l1.getPositionAt(1);
        assertEquals("Offset1", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("Offset2", oc.getTag());
        oc = _l1.getPositionAt(3);
        assertEquals("Offset3", oc.getTag());
        oc = _l1.getPositionAt(4);
        assertEquals("Offset4", oc.getTag());

        _l1.removePosition("Offset0");
        assertEquals(4, _l1.size());
        oc = _l1.getPositionAt(0);
        assertEquals("Offset1", oc.getTag());
        oc = _l1.getPositionAt(1);
        assertEquals("Offset2", oc.getTag());
        oc = _l1.getPositionAt(2);
        assertEquals("Offset3", oc.getTag());
        oc = _l1.getPositionAt(3);
        assertEquals("Offset4", oc.getTag());

        _l1.removePosition("Offset0");
        assertEquals(4, _l1.size());

        oc = _l1.getPositionAt(2);
        _l1.removePosition(oc);
        assertEquals(3, _l1.size());
        oc = _l1.getPositionAt(0);
        assertEquals("Offset1", oc.getTag());
        oc = _l1.getPositionAt(1);
        assertEquals("Offset2", oc.getTag());
        oc = _l1.getPositionAt(2);
        assertEquals("Offset4", oc.getTag());

        // Now create a new one back in!
        _l1.addPosition(100, 200);
        assertEquals(4, _l1.size());
        oc = _l1.getPositionAt(0);
        assertEquals("Offset1", oc.getTag());
        oc = _l1.getPositionAt(1);
        assertEquals("Offset2", oc.getTag());
        oc = _l1.getPositionAt(2);
        assertEquals("Offset4", oc.getTag());
        oc = _l1.getPositionAt(3);
        assertEquals("Offset0", oc.getTag());

        _l1.removePosition("Offset0");
        assertEquals(3, _l1.size());

        _l1.removeAllPositions();
        assertEquals(0, _l1.size());
    }

    // Test decrementIndex
    @Test
    public void testDecrement() {
        OffsetPos op1 = _l1.addPosition(1.0, 1.0);
        assertNotNull(op1);
        String s1 = op1.getTag();

        OffsetPos op2 = _l1.addPosition(10.0, 10.0);
        assertNotNull(op2);
//        String s2 = op2.getTag();

        OffsetPos op = _l1.getPositionAt(0);
        int result = _l1.decrementPosition(op);
        assertEquals(4, _l1.size());
        assertEquals(0, result);

        int pre = _l1.getPositionIndex(s1);
        int post = _l1.decrementPosition(op1);
        assertEquals(4, _l1.size());
        assertEquals("Decrement s1", pre - 1, post);

        pre = post;
        post = _l1.decrementPosition(op1);
        assertEquals(4, _l1.size());
        assertEquals("Decrement again s1", pre - 1, post);
        assertEquals("Front:", 0, post);

        // Remove 0 and try to decrement
        _l1.removePosition(op1);
        assertEquals(OffsetPosList.UNKNOWN_INDEX, _l1.decrementPosition(op1));
        assertEquals(3, _l1.size());
    }

    // Test increment
    @Test
    public void testIncrement() {
        OffsetPos op1 = _l1.addPosition(1.0, 1.0);
        assertNotNull(op1);
        String s1 = op1.getTag();

        OffsetPos op2 = _l1.addPosition(10.0, 10.0);
        assertNotNull(op2);
        String s2 = op2.getTag();

        OffsetPos op =  _l1.getPositionAt(_l1.size() - 1);
        int result = _l1.incrementPosition(op);
        assertEquals(4, _l1.size());
        assertEquals(_l1.size() - 1, result);

        // Move Offset3 to position 3
        int pre = _l1.getPositionIndex(s1);
        int post = _l1.incrementPosition(op1);
        assertEquals(4, _l1.size());
        assertEquals("Increment s1", pre + 1, post);

        pre = _l1.getPositionIndex(s2);
        post = _l1.incrementPosition(op2);
        assertEquals(4, _l1.size());
        assertEquals("Increment again s1", pre + 1, post);
        assertEquals("Front:", _l1.size() - 1, post);

        // Remove 0 and try to decrement
        _l1.removePosition(op1);
        assertEquals(OffsetPosList.UNKNOWN_INDEX, _l1.incrementPosition(op1));
        assertEquals(3, _l1.size());
    }

    // Test positionToBack
    @Test
    public void testToBack() {
        OffsetPos op1 = _l1.addPosition(1.0, 1.0);
        assertNotNull(op1);
//        String s1 = op1.getTag();

        OffsetPos op2 = _l1.addPosition(10.0, 10.0);
        assertNotNull(op2);
//        String s2 = op2.getTag();

        assertEquals(4, _l1.size());
        OffsetPos oc = _l1.getPositionAt(0);
        assertEquals("Offset0", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("Offset1", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("Offset2", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("Offset3", oc.getTag());
        // put 2 to end
        _l1.positionToBack(op1);
        oc =  _l1.getPositionAt(0);
        assertEquals("Offset0", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("Offset1", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("Offset3", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("Offset2", oc.getTag());

        // Put 0 to end
        oc =  _l1.getPositionAt(0);
        assertEquals("Offset0", oc.getTag());
        _l1.positionToBack(oc);
        oc =  _l1.getPositionAt(0);
        assertEquals("Offset1", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("Offset3", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("Offset2", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("Offset0", oc.getTag());

        // Already at the end
        oc =  _l1.getPositionAt(3);
        _l1.positionToBack(oc);
        oc =  _l1.getPositionAt(0);
        assertEquals("Offset1", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("Offset3", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("Offset2", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("Offset0", oc.getTag());

        // Remove 0 and try to decrement
        _l1.removePosition(op1);
        assertEquals(OffsetPosList.UNKNOWN_INDEX, _l1.incrementPosition(op1));
        assertEquals(3, _l1.size());
    }

    // Test positionToFront
    @Test
    public void testToFront() {
        OffsetPos op1 = _l1.addPosition(1.0, 1.0);
        assertNotNull(op1);
//        String s1 = op1.getTag();

        OffsetPos op2 = _l1.addPosition(10.0, 10.0);
        assertNotNull(op2);
//        String s2 = op2.getTag();

        assertEquals(4, _l1.size());
        OffsetPos oc =  _l1.getPositionAt(0);
        assertEquals("t1", "Offset0", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("t1", "Offset1", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("t1", "Offset2", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("t1", "Offset3", oc.getTag());

        // put 2 to front
        _l1.positionToFront(op1);
        oc =  _l1.getPositionAt(0);
        assertEquals("t2", "Offset2", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("t2", "Offset0", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("t2", "Offset1", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("t2", "Offset3", oc.getTag());

        // put 3 to front
        oc =  _l1.getPositionAt(3);
        _l1.positionToFront(oc);
        oc =  _l1.getPositionAt(0);
        assertEquals("t2", "Offset3", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("t2", "Offset2", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("t2", "Offset0", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("t2", "Offset1", oc.getTag());

        // Already at front
        oc =  _l1.getPositionAt(0);
        _l1.positionToFront(oc);
        oc =  _l1.getPositionAt(0);
        assertEquals("t2", "Offset3", oc.getTag());
        oc =  _l1.getPositionAt(1);
        assertEquals("t2", "Offset2", oc.getTag());
        oc =  _l1.getPositionAt(2);
        assertEquals("t2", "Offset0", oc.getTag());
        oc =  _l1.getPositionAt(3);
        assertEquals("t2", "Offset1", oc.getTag());

        // Remove 0 and try to decrement
        _l1.removePosition(op1);
        assertEquals(OffsetPosList.UNKNOWN_INDEX, _l1.incrementPosition(op1));
        assertEquals(3, _l1.size());
    }

    // Test the link support
    @Test
    public void testLinks() {
        assertNotNull(_t1);
        assertTrue(_t1.getLinkCount() == 0);

        GuideProbe id1 = PwfsGuideProbe.pwfs2;
        GuideProbe id2 = GmosOiwfsGuideProbe.instance;

        GuideOption value1 = StandardGuideOptions.Value.guide;
        GuideOption value2 = StandardGuideOptions.Value.freeze;
        GuideOption value3 = StandardGuideOptions.Value.park;

        assertTrue(!_t1.linkExists(id1));

        _t1.setLink(id1, value1);
        assertTrue(_t1.linkExists(id1));
        assertTrue(_t1.getLinkCount() == 1);

        // Check for value then change
        assertEquals("Init link value", value1, _t1.getLink(id1));
        _t1.setLink(id1, value2);
        assertEquals("Init link value", value2, _t1.getLink(id1));
        _t1.setLink(id2, value3);
        assertTrue(_t1.getLinkCount() == 2);

        _t1.removeAllLinks();
        assertTrue(_t1.getLinkCount() == 0);

        // Add links back and test remove one
        _t1.setLink(id1, value1);
        _t1.setLink(id2, value3);
        assertTrue(_t1.getLinkCount() == 2);

        _t1.removeLink(id2);
        assertTrue(_t1.getLinkCount() == 1);
        assertTrue(_t1.linkExists(id1));
        assertTrue(!_t1.linkExists(id2));

        _t1.removeLink(id1);
        assertTrue(_t1.getLinkCount() == 0);
        assertTrue(!_t1.linkExists(id1));
        assertTrue(!_t1.linkExists(id2));
    }

//    @Test
//    public void testCloneable() {
//        // Now cline the BDO
//        OffsetPos t2 = (OffsetPos) _t1.clone();
//        assertEquals(t2.toString(), _t1.toString()); // RCN: equality doesn't work
//        // Now change clone
//        t2.setXY(-100, -200, IssPort.DEFAULT);
//        assertTrue("Not equals:", !t2.contentsEqual(_t1));
//
//        //noinspection unchecked
//        OffsetPosList<OffsetPos> l1 = (OffsetPosList<OffsetPos>) _l1.clone();
//        assertEquals(_l1.toString(), l1.toString());
//
//        assertEquals(2, l1.size());
//
//        // Get a member of the cloned list, alter it
//        OffsetPos t3 =  l1.getPosition("Offset0");
//        t3.setXY(-100, -399, IssPort.DEFAULT);
//        assertTrue("Not equals:", !l1.equals(_l1));
//    }

    //Test cloneable with Links
//    @Test
//    public void testCloneableLinks() {
//        GuideProbe id1 = PwfsGuideProbe.pwfs2;
//        GuideProbe id2 = GmosOiwfsGuideProbe.instance;
//
//        GuideOption value1 = StandardGuideOptions.Value.guide;
//        GuideOption value2 = StandardGuideOptions.Value.freeze;
//        GuideOption value3 = StandardGuideOptions.Value.park;
//
//        _t1.setLink(id1, value1);
//        _t1.setLink(id2, value2);
//
//        // Now cline the test obj
//        OffsetPos t2 = (OffsetPos) _t1.clone();
//        assertEquals(t2.toString(), _t1.toString());
//
//        // Now change clone
//        t2.setLink(id1, value3);
//        assertTrue("Not equals:", !t2.contentsEqual(_t1));
//
//    }

    @Test
    public void testSeripalization() throws Exception {
        final OffsetPosList<OffsetPos> outObject = new OffsetPosList<OffsetPos>(OffsetPos.FACTORY);
        outObject.addPosition(-100, -200);
        outObject.addPosition(100, 200);
        assertEquals("Initial size", 2, outObject.size());

        OffsetPos op = outObject.getPositionAt(0);
        assertNotNull(op);

        GuideProbe id1 = PwfsGuideProbe.pwfs2;
        GuideProbe id2 = GmosOiwfsGuideProbe.instance;

        GuideOption value1 = StandardGuideOptions.Value.guide;
        GuideOption value2 = StandardGuideOptions.Value.freeze;
//        GuideOption value3 = StandardGuideOptions.Value.park;

        op.setLink(id1, value1);
        op.setLink(id2, value2);

        final OffsetPosList<OffsetPos> inObject = ser(outObject);
        if (inObject == null) fail("couldn't create inObject");
        assertEquals("InObject size:", outObject.size(), inObject.size());
        OffsetPos opin =  inObject.getPositionAt(0);
        assertEquals("InObject links", 2, opin.getLinkCount());

        assertEquals(inObject.getAdvancedGuiding(), outObject.getAdvancedGuiding());

        // RCN: it's not possible in general to compare positions because there are so many different kinds; it depends
        // on your perspective whether they're equal or not. So I'm just punting and comparing their toStrings(), which
        // should be sufficient evidence of valid serialization.
        assertEquals(inObject.getAllPositions().toString(), outObject.getAllPositions().toString());
    }
}
