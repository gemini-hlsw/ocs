package edu.gemini.spModel.config2.test;

import junit.framework.TestCase;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.DefaultConfig;

import java.util.Iterator;

public class ConfigSequenceTest extends TestCase {

    private ConfigSequence _seq;
    private ConfigSequence _emptySeq;

    private ItemKey _nochangeKey = new ItemKey("ocs:nochange");
    private ItemKey _changeKey   = new ItemKey("ocs:change");
    private ItemKey _newKey1     = new ItemKey("ocs:new1");
    private ItemKey _newKey2     = new ItemKey("ocs:new2");

    private Config _config0;
    private Config _config1;
    private Config _config2;

    public void setUp() {
        _config0 = new DefaultConfig();
        _config0.putItem(_nochangeKey, "nochange");
        _config0.putItem(_changeKey,   "change0");

        _config1 = new DefaultConfig();
        _config1.putItem(_changeKey, "change1");
        _config1.putItem(_newKey1,  "new1");

        _config2 = new DefaultConfig();
        _config2.putItem(_nochangeKey, "nochange");
        _config2.putItem(_changeKey, "change2");
        _config2.putItem(_newKey2, "new2");

        _seq = new ConfigSequence(new Config[] { _config0, _config1, _config2 });
        _emptySeq = new ConfigSequence();
    }

    public void testGetAllSteps() {
        // Test an empty sequence.
        Config[] res = _emptySeq.getAllSteps();
        assertEquals(0, res.length);

        // Check each step.
        res = _seq.getAllSteps();
        assertEquals(3, res.length);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change0"  },
        }, res[0]);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change1"  },
            { _newKey1.getPath(),     "new1"  },
        }, res[1]);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change2"  },
            { _newKey1.getPath(),     "new1"     },
            { _newKey2.getPath(),     "new2"     },
        }, res[2]);

        // Change one of the returned sequences and make sure it doesn't
        // actually modify anything internal to the ConfigSequence.

        res[0].putItem(new ItemKey("test"), "test");
        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change0"  },
        }, _seq.getStep(0));
    }

    public void testGetCompactView() {
        // Test an empty sequence.
        Config[] res = _emptySeq.getCompactView();
        assertEquals(0, res.length);

        // Check each step.
        res = _seq.getCompactView();
        assertEquals(3, res.length);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange"},
            { _changeKey.getPath(),   "change0" },
        }, res[0]);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _changeKey.getPath(),   "change1" },
            { _newKey1.getPath(),     "new1"    },
        }, res[1]);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _changeKey.getPath(),   "change2" },
            { _newKey2.getPath(),     "new2"    },
        }, res[2]);

        // Change one of the config items and make sure it doesn't modify the
        // ConfigSequence internals.
        res[1].putItem(new ItemKey("test"), "test");
        res = _seq.getCompactView();
        ConfigTestUtils.assertConfigContains(new String[][] {
            { _changeKey.getPath(),   "change1" },
            { _newKey1.getPath(),     "new1"    },
        }, res[1]);
    }

    public void testGetStep() {
        // Test an empty sequence.
        try {
            _emptySeq.getStep(0);
            fail("should be out of bounds");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        // Test the first config in the sequence.
        Config config = _seq.getStep(0);
        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change0"  },
        }, config);

        // Change the returned config and make sure it doesn't modify the
        // ConfigSequence internals.
        config.putItem(_newKey1,  "new1");
        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change0"  },
        }, _seq.getStep(0));

        // Examine the second and third steps.
        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change1"  },
            { _newKey1.getPath(),     "new1"     },
        }, _seq.getStep(1));

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(), "nochange" },
            { _changeKey.getPath(),   "change2"  },
            { _newKey1.getPath(),     "new1"     },
            { _newKey2.getPath(),     "new2"     },
        }, _seq.getStep(2));
    }

    public void testGetItemValue() {
        // Test an empty sequence.
        try {
            _emptySeq.getItemValue(0, _nochangeKey);
            fail("should be out of bounds");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        assertEquals("nochange", _seq.getItemValue(0, _nochangeKey));
        assertEquals("change0",  _seq.getItemValue(0, _changeKey));
        assertEquals(null,       _seq.getItemValue(0, _newKey1));
        assertEquals(null,       _seq.getItemValue(0, _newKey2));

        assertEquals("nochange", _seq.getItemValue(1, _nochangeKey));
        assertEquals("change1",  _seq.getItemValue(1, _changeKey));
        assertEquals("new1",     _seq.getItemValue(1, _newKey1));
        assertEquals(null,       _seq.getItemValue(1, _newKey2));

        assertEquals("nochange", _seq.getItemValue(2, _nochangeKey));
        assertEquals("change2",  _seq.getItemValue(2, _changeKey));
        assertEquals("new1",     _seq.getItemValue(2, _newKey1));
        assertEquals("new2",     _seq.getItemValue(2, _newKey2));
    }

    public void testGetItemValueAtEachStep() {
        ConfigTestUtils.assertValues(new String[] {
            "nochange",
            "nochange",
            "nochange",
        }, _seq.getItemValueAtEachStep(_nochangeKey));

        ConfigTestUtils.assertValues(new String[] {
            "change0",
            "change1",
            "change2",
        }, _seq.getItemValueAtEachStep(_changeKey));

        ConfigTestUtils.assertValues(new String[] {
            null,
            "new1",
            "new1",
        }, _seq.getItemValueAtEachStep(_newKey1));

        ConfigTestUtils.assertValues(new String[] {
            null,
            null,
            "new2",
        }, _seq.getItemValueAtEachStep(_newKey2));
    }

    public void testGetDistinctItemValues() {
        // Test an empty sequence.
        Object[] vals = _emptySeq.getDistinctItemValues(_nochangeKey);
        assertEquals(0, vals.length);

        vals = _seq.getDistinctItemValues(_nochangeKey);
        ConfigTestUtils.assertUnorderedValues(new String[] {
            "nochange",
        }, _seq.getDistinctItemValues(_nochangeKey));

        ConfigTestUtils.assertUnorderedValues(new String[] {
            "change0",
            "change1",
            "change2",
        }, _seq.getDistinctItemValues(_changeKey));

        ConfigTestUtils.assertUnorderedValues(new String[] {
            "new1",
            null,
        }, _seq.getDistinctItemValues(_newKey1));

        ConfigTestUtils.assertUnorderedValues(new String[] {
            "new2",
            null,
        }, _seq.getDistinctItemValues(_newKey2));
    }

    public void testGetIteratedKeys() {
        ItemKey[] keys = _emptySeq.getIteratedKeys();
        assertEquals(0, keys.length);

        keys = _seq.getIteratedKeys();
        ConfigTestUtils.assertUnorderedValues(new ItemKey[] {
            _changeKey,
            _newKey1,
            _newKey2,
        }, keys);
    }

    public void testMatch() {
        // Empty sequence tests.
        Config match = _emptySeq.match(new DefaultConfig());
        assertNull(match);

        // Empty template -- finds first step.
        Config template = new DefaultConfig();
        match = _seq.match(template);
        assertEquals(2, match.size());
        assertEquals("nochange", match.getItemValue(_nochangeKey));
        assertEquals("change0",  match.getItemValue(_changeKey));


        // Template matching an item in first step.
        template.putItem(_nochangeKey, "nochange");

        match = _seq.match(template);
        assertEquals(2, match.size());
        assertEquals("nochange", match.getItemValue(_nochangeKey));
        assertEquals("change0",  match.getItemValue(_changeKey));


        // Make sure that modifications to the matched config don't impact
        // the sequence.
        match.putItem(_changeKey, "xyz");
        assertEquals("change0", _seq.getItemValue(0, _changeKey));

        // Make sure a match on key but not value doesn't work
        template.putItem(_nochangeKey, "unknown val");
        match = _seq.match(template);
        assertNull(match);

        // Match on two items.
        template.clear();
        template.putAll(_seq.getStep(0));
        match = _seq.match(template);
        assertEquals(2, match.size());
        assertEquals("nochange", match.getItemValue(_nochangeKey));
        assertEquals("change0",  match.getItemValue(_changeKey));

        // Match on two items, but add a third non-present item.
        template.putItem(new ItemKey("test"), "test");
        match = _seq.match(template);
        assertNull(match);

        // Match on the last step.
        template.clear();
        template.putItem(_nochangeKey, "nochange");
        template.putItem(_newKey2, "new2");
        match = _seq.match(template);
        assertEquals(match, _seq.getStep(2));
    }

    public void testAddStep1() {
        // Add a step to the empty sequence.
        Config newConfig = new DefaultConfig();
        newConfig.putItem(new ItemKey("test1"), "test1");
        _emptySeq.addStep(newConfig);
        assertEquals(1, _emptySeq.size());
        assertFalse(newConfig == _emptySeq.getStep(0));
        assertEquals(newConfig, _emptySeq.getStep(0));

        // Now work with the main _seq ConfigSequence.  To start with it is not
        // complete or compact.
        _seq.addStep(newConfig);

        // change the config -- which should not impact the ConfigSequence
        newConfig.putItem(new ItemKey("test2"), "test2");
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change2"  },
            {_newKey1.getPath(),       "new1"     },
            {_newKey2.getPath(),       "new2"     },
            {"test1",                  "test1"    },
        }, _seq.getStep(3));  // now _seq is complete

        Config[] res = _seq.getCompactView();  // now _seq is compact too
        assertEquals(1, res[3].size());
        assertEquals("test1", res[3].getItemValue(new ItemKey("test1")));

        // Now add another step -- the _seq is now complete and compact so the
        // behavior is slightly different
        _seq.addStep(newConfig);
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change2"  },
            {_newKey1.getPath(),       "new1"     },
            {_newKey2.getPath(),       "new2"     },
            {"test1",                  "test1"    },
            {"test2",                  "test2"    },
        }, _seq.getStep(4));

        res = _seq.getCompactView();  // now _seq is compact too
        assertEquals(1, res[4].size());
        assertEquals("test2", res[4].getItemValue(new ItemKey("test2")));
    }

    public void testAddStep2() {
        Config newConfig0 = new DefaultConfig();
        Config newConfig2 = new DefaultConfig();
        Config newConfig4 = new DefaultConfig();
        Config newConfig6 = new DefaultConfig();

        newConfig0.putItem(new ItemKey("config0"), "config0");
        newConfig2.putItem(new ItemKey("config2"), "config2");
        newConfig4.putItem(new ItemKey("config4"), "config4");
        newConfig6.putItem(new ItemKey("config6"), "config6");

        _seq.addStep(0, newConfig0); // add before the start
        _seq.addStep(2, newConfig2);
        _seq.addStep(4, newConfig4);
        _seq.addStep(6, newConfig6); // add to the end

        ConfigTestUtils.assertConfigContains(new String[][] {
            {"config0",                "config0"   },
        }, _seq.getStep(0));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change0"  },
            {"config0",                "config0"   },
        }, _seq.getStep(1));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change0"  },
            {"config0",                "config0"  },
            {"config2",                "config2"  },
        }, _seq.getStep(2));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change1"  },
            {_newKey1.getPath(),       "new1"     },
            {"config0",                "config0"  },
            {"config2",                "config2"  },
        }, _seq.getStep(3));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change1"  },
            {_newKey1.getPath(),       "new1"     },
            {"config0",                "config0"  },
            {"config2",                "config2"  },
            {"config4",                "config4"  },
        }, _seq.getStep(4));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change2"  },
            {_newKey1.getPath(),       "new1"     },
            {_newKey2.getPath(),       "new2"     },
            {"config0",                "config0"  },
            {"config2",                "config2"  },
            {"config4",                "config4"  },
        }, _seq.getStep(5));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change2"  },
            {_newKey1.getPath(),       "new1"     },
            {_newKey2.getPath(),       "new2"     },
            {"config0",                "config0"  },
            {"config2",                "config2"  },
            {"config4",                "config4"  },
            {"config6",                "config6"  },
        }, _seq.getStep(6));
    }

    public void testIterator() {
        // Test the iterator of an empty sequence
        Iterator<Config> it = _emptySeq.iterator();
        assertFalse(it.hasNext());

        // Get the first Config from the _seq.
        it = _seq.iterator();
        assertTrue(it.hasNext());
        Config config = it.next();

        // Make sure it contains what we expect
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change0"  },
        }, config);

        // Make sure that client side modifications don't impact the
        // ConfigSequence
        config.putItem(new ItemKey("test"), "test");
        for (int i=0; i<_seq.size(); ++i) {
            assertNull(_seq.getItemValue(i, new ItemKey("test")));
        }

        // Make sure the remove method behaves as expected.
        try {
            it.remove();
            fail("should throw unsupported operation");
        } catch (UnsupportedOperationException ex) {
            // expected
        }

        // Finally, just iterate the last two steps.
        assertTrue(it.hasNext());
        config = it.next();
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change1"  },
            {_newKey1.getPath(),       "new1"     },
        }, config);

        assertTrue(it.hasNext());
        config = it.next();
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change2"  },
            {_newKey1.getPath(),       "new1"     },
            {_newKey2.getPath(),       "new2"     },
        }, config);

        // Now the iterator is done
        assertFalse(it.hasNext());
    }

    public void testCompactIterator() {
        // Test the compact iterator of an empty sequence
        Iterator<Config> it = _emptySeq.iterator();
        assertFalse(it.hasNext());

        // Get the first Config of the _seq
        it = _seq.compactIterator();
        assertTrue(it.hasNext());
        Config config = it.next();

        // Make sure it contains what we expect
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change0"  },
        }, config);

        // Make sure that client side modifications don't impact the
        // ConfigSequence
        config.putItem(new ItemKey("test"), "test");
        for (int i=0; i<_seq.size(); ++i) {
            assertNull(_seq.getItemValue(i, new ItemKey("test")));
        }

        // Make sure the remove method behaves as expected.
        try {
            it.remove();
            fail("should throw unsupported operation");
        } catch (UnsupportedOperationException ex) {
            // expected
        }

        // Finally, just iterate the last two steps.
        assertTrue(it.hasNext());
        config = it.next();
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "change1"  },
            {_newKey1.getPath(),       "new1"     },
        }, config);

        assertTrue(it.hasNext());
        config = it.next();
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "change2"  },
            {_newKey2.getPath(),       "new2"     },
        }, config);

        // Now the iterator is done
        assertFalse(it.hasNext());
    }

    public void testIsEmpty() {
        assertTrue(_emptySeq.isEmpty());
        assertFalse(_seq.isEmpty());
        _seq.clear();
        assertTrue(_seq.isEmpty());
    }

    public void testRemoveStep() {
        try {
            _emptySeq.removeStep(0);
            fail("should throw index out of bounds");
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        _seq.removeStep(0);
        assertEquals(2, _seq.size());

        // Now step 0 doesn't have "nochange"
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "change1"  },
            {_newKey1.getPath(),       "new1"     },
        }, _seq.getStep(0));

        _seq.removeStep(1);
        _seq.removeStep(0);
        assertEquals(0, _seq.size());
    }

    public void testSetStep() {
        Config conf = new DefaultConfig();
        try {
            _emptySeq.setStep(0, conf);
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        // remove an existing item value (_nochangekey)

        // change an existing item value
        conf.putItem(_changeKey, "changex");

        // add a new item
        conf.putItem(new ItemKey("test"), "test");

        _seq.setStep(0, conf);

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "changex"  },
            {"test",                   "test"     },
        }, _seq.getStep(0));

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "change1"  },
            {"test",                   "test"     },
            {_newKey1.getPath(),       "new1"     },
        }, _seq.getStep(1));

        // demonstrate that local changes have no impact
        conf.putItem(_newKey1, "xxx");
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "changex"  },
            {"test",                   "test"     },
        }, _seq.getStep(0));

        // demonstrate that a key in a prior step isn't deleted, but simply
        // takes on the inherited value
        conf.clear();
        _seq.setStep(1, conf);
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_changeKey.getPath(),     "changex"  },
            {"test",                   "test"     },
        }, _seq.getStep(1));
    }

    public void testSubSequence() {
        try {
            _emptySeq.subSequence(0, 0);
        } catch (IndexOutOfBoundsException ex) {
            // expected
        }

        // Get an empty sequence.
        ConfigSequence seq = _seq.subSequence(0, 0);
        assertEquals(0, seq.size());

        // Test one element sequence containing just the first step.
        seq = _seq.subSequence(0, 1);
        assertEquals(1, seq.size());

        Config conf = seq.getStep(0);
        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change0"  },
        }, conf);

        // Try adding a item -- shouldn't impact the "seq" sequence.
        conf.putItem(new ItemKey("test"), "test");

        ConfigTestUtils.assertConfigContains(new String[][] {
            {_nochangeKey.getPath(),   "nochange" },
            {_changeKey.getPath(),     "change0"  },
        }, seq.getStep(0));


        seq = _seq.subSequence(0, _seq.size());
        assertEquals(3, seq.size());
    }
}
