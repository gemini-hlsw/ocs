//
// $Id: ConfigTest.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.spModel.config2.test;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.ItemEntry;
import edu.gemini.spModel.config2.DefaultConfig;
import junit.framework.TestCase;

public class ConfigTest extends TestCase {

    public void testEmpty() {
        Config config = new DefaultConfig();

        ItemKey testKey = new ItemKey("test");
        assertFalse(config.containsItem(testKey));

        assertEquals(0, config.itemEntries().length);
        assertEquals(0, config.itemEntries(testKey).length);

        assertTrue(config.isEmpty());

        assertEquals(0, config.getKeys().length);
        assertEquals(0, config.getKeys(testKey).length);

        assertNull(config.getItemValue(testKey));

        assertEquals(config, config.getAll(testKey));
        assertEquals(config, config.getAll(new ItemKey[] { testKey }));

        assertEquals(null, config.remove(testKey));

        config.removeAll(testKey); // no exception should be thrown
        config.removeAll(new ItemKey[] { testKey });

        config.retainAll(testKey);
        config.retainAll(new ItemKey[] { testKey });

        config.retainAll(new DefaultConfig());
        assertEquals(config, new DefaultConfig());

        assertTrue(config.matches(new DefaultConfig()));

        Config oneItemConfig = new DefaultConfig();
        oneItemConfig.putItem(testKey, "val");
        assertFalse(config.matches(oneItemConfig));
        assertTrue(oneItemConfig.matches(config));

        assertEquals(0, config.size());
    }

    private ItemKey _parentKey1 = new ItemKey("parent1");
    private ItemKey _parentKey2 = new ItemKey("parent2");

    private ItemKey _childKey11  = new ItemKey(_parentKey1, "child1");
    private ItemKey _childKey12  = new ItemKey(_parentKey1, "child2");
    private ItemKey _grandchildKey111 = new ItemKey(_childKey11, "grandchild1");

    private ItemKey _childKey21 = new ItemKey(_parentKey2, "child1");
    private ItemKey _childKey22 = new ItemKey(_parentKey2, "child2");
    private ItemKey _childKey23 = new ItemKey(_parentKey2, "child3");

    private Config _config;

    public void setUp() {

        _config = new DefaultConfig();

        _config.putItem(_childKey11,   "11");
        _config.putItem(_childKey12,   "12");
        _config.putItem(_grandchildKey111, "111");

        _config.putItem(_childKey21,   "21");
        _config.putItem(_childKey22,   "22");
        _config.putItem(_childKey23,   "23");
    }

    public void testContainsItem() {
        ItemKey testKey = new ItemKey("test");
        assertFalse(_config.containsItem(testKey));
        assertTrue(_config.containsItem(_childKey11));
    }

    public void testClearAndIsEmpty() {
        assertFalse(_config.isEmpty());
        _config.clear();
        assertTrue(_config.isEmpty());
    }

    private void _assertEquals(String[] a1, String[] a2) {
        assertEquals(a1.length, a2.length);
        for (int i=0; i<a1.length; ++i) {
            assertEquals(a1[i], a2[i]);
        }
    }

    private String[] _getValues(ItemEntry[] a) {
        String[] res = new String[a.length];
        for (int i=0; i<res.length; ++i) {
            res[i] = a[i].getItemValue().toString();
        }
        return res;
    }

    public void testItemEntries() {
        _assertEquals(new String[] { "11", "111", "12", "21", "22", "23"},
                      _getValues(_config.itemEntries()));

        _assertEquals(new String[] { "11", "111", "12" },
                      _getValues(_config.itemEntries(_parentKey1)));

        _assertEquals(new String[] { "11", "111" },
                      _getValues(_config.itemEntries(_childKey11)));

        _assertEquals(new String[] { "111" },
                      _getValues(_config.itemEntries(_grandchildKey111)));

        _assertEquals(new String[] { "21", "22", "23" },
                      _getValues(_config.itemEntries(_parentKey2)));

        _assertEquals(new String[0],
                      _getValues(_config.itemEntries(new ItemKey("test"))));
    }

    public String[] _getPaths(ItemKey[] a) {
        String[] res = new String[a.length];
        for (int i=0; i<res.length; ++i) {
            res[i] = a[i].getPath();
        }
        return res;
    }

    public void testGetKeys() {
        _assertEquals(new String[] {
                        "parent1:child1",
                        "parent1:child1:grandchild1",
                        "parent1:child2",
                        "parent2:child1",
                        "parent2:child2",
                        "parent2:child3",},
                      _getPaths(_config.getKeys()));

        _assertEquals(new String[] {
                        "parent1:child1",
                        "parent1:child1:grandchild1",
                        "parent1:child2",},
                      _getPaths(_config.getKeys(_parentKey1)));

        _assertEquals(new String[] {
                        "parent1:child1",
                        "parent1:child1:grandchild1",},
                      _getPaths(_config.getKeys(_childKey11)));

        _assertEquals(new String[] {
                        "parent1:child1:grandchild1", },
                      _getPaths(_config.getKeys(_grandchildKey111)));

        _assertEquals(new String[] {
                        "parent2:child1",
                        "parent2:child2",
                        "parent2:child3",},
                      _getPaths(_config.getKeys(_parentKey2)));

        _assertEquals(new String[0],
                      _getPaths(_config.getKeys(new ItemKey("test"))));
    }

    public void testGetItemValue() {
        assertNull(_config.getItemValue(new ItemKey("test")));
        assertNull(_config.getItemValue(_parentKey1));
        assertEquals("11", _config.getItemValue(_childKey11));
        assertEquals("111", _config.getItemValue(_grandchildKey111));
    }

    private Config _getSubconfig(ItemKey[] keys) {
        Config res = new DefaultConfig();

        for (int i=0; i<keys.length; ++i) {
            res.putItem(keys[i], _config.getItemValue(keys[i]));
        }

        return res;
    }

    public void testGetAll1() {
        assertEquals(_getSubconfig(
                        new ItemKey[] { _childKey11, _grandchildKey111, _childKey12 }),
                     _config.getAll(_parentKey1));

        assertEquals(_getSubconfig(
                        new ItemKey[] { _childKey11, _grandchildKey111 }),
                     _config.getAll(_childKey11));

        assertEquals(_getSubconfig(
                        new ItemKey[] { _grandchildKey111 }),
                     _config.getAll(_grandchildKey111));

        assertEquals(_getSubconfig(
                        new ItemKey[] { _childKey21, _childKey22, _childKey23 }),
                     _config.getAll(_parentKey2));

        assertEquals(new DefaultConfig(), _config.getAll(new ItemKey("test")));
    }

    public void testGetAll2() {
        assertEquals(_getSubconfig(
                        new ItemKey[] {
                            _childKey11,
                            _grandchildKey111,
                            _childKey12,
                            _childKey21,
                            _childKey22,
                            _childKey23,
                        }),
                     _config.getAll(new ItemKey[] {_parentKey1, _parentKey2}));

        assertEquals(_getSubconfig(
                        new ItemKey[] { _childKey11, _grandchildKey111, _childKey12 }),
                     _config.getAll(new ItemKey[] {_parentKey1}));

        assertEquals(_getSubconfig(
                        new ItemKey[] {
                            _childKey11,
                            _grandchildKey111,
                            _childKey22,
                        }),
                     _config.getAll(new ItemKey[] {_childKey11, _childKey22}));


        assertEquals(new DefaultConfig(),
                     _config.getAll(new ItemKey[] {new ItemKey("test")}));
    }

    public void testPutAll() {
        // Add an empty config
        Config test = new DefaultConfig(_config);
        test.putAll(new DefaultConfig());
        assertEquals(_config, test);

        // Change a single value
        test.putItem(_childKey11, "11-new");
        _config.putAll(test);
        assertEquals(_config.getItemValue(_childKey11), "11-new");

        // Add a new value
        test.putItem(new ItemKey("testkey1"), "testvalue1");
        _config.putAll(test);
        assertEquals(_config.getItemValue(new ItemKey("testkey1")), "testvalue1");
        assertEquals(_config.getItemValue(_childKey11), "11-new"); // the same?

        // Add a value and change a value
        test = new DefaultConfig();
        test.putItem(new ItemKey("testkey2"), "testvalue2");
        test.putItem(_childKey22,  "22-new");
        _config.putAll(test);

        assertEquals(_config.getItemValue(new ItemKey("testkey1")), "testvalue1");
        assertEquals(_config.getItemValue(_childKey11), "11-new"); // the same?
        assertEquals(_config.getItemValue(new ItemKey("testkey2")), "testvalue2");
        assertEquals(_config.getItemValue(_childKey22), "22-new");
    }

    public void testRemove() {
        assertNull(_config.remove(new ItemKey("test")));
        assertEquals(6, _config.size()); // nothing removed

        assertEquals("11", _config.remove(_childKey11));
        assertEquals("111", _config.getItemValue(_grandchildKey111));
    }

    public void testRemoveAll1() {
        _config.removeAll(new ItemKey("test"));
        assertEquals(6, _config.size()); // nothing removed

        _config.removeAll(_childKey11); // removes 11 and 111
        assertNull(_config.getItemValue(_childKey11));
        assertNull(_config.getItemValue(_grandchildKey111));
        assertEquals("12", _config.getItemValue(_childKey12)); // not changed
        assertEquals(4, _config.size());

        _config.removeAll(_childKey22); // removes just one item
        assertNull(_config.getItemValue(_childKey22));
        assertEquals(3, _config.size());
    }

    public void testRemoveAll2() {
        _config.removeAll(new ItemKey[] { } );
        assertEquals(6, _config.size()); // nothing removed

        _config.removeAll(new ItemKey[] { new ItemKey("test")} );
        assertEquals(6, _config.size()); // nothing removed

        _config.removeAll(new ItemKey[] { _childKey22 });
        assertNull(_config.getItemValue(_childKey22));
        assertEquals(5, _config.size());

        _config.removeAll(new ItemKey[] {_parentKey1, _parentKey2});
        assertEquals(0, _config.size());
    }

    public void testRemoveAll3() {
        Config test = new DefaultConfig();

        _config.removeAll(test);
        assertEquals(6, _config.size()); // nothing removed

        test.putItem(new ItemKey("testkey"), "testvalue");
        _config.removeAll(test);
        assertEquals(6, _config.size()); // nothing removed

        test = _getSubconfig(new ItemKey[] { _childKey22 });
        _config.removeAll(test);
        assertNull(_config.getItemValue(_childKey22));
        assertEquals(5, _config.size());

        test = new DefaultConfig(_config);
        _config.removeAll(test);
        assertEquals(0, _config.size());
    }

    public void testRetainAll1() {
        // Copy the base config
        Config test = new DefaultConfig(_config);
        assertEquals(6, test.size());

        // Retain a non-existing key -- removes everything
        test.retainAll(new ItemKey("test"));
        assertEquals(0, test.size());

        // Retain the parentKey2 children
        _config.retainAll(_parentKey2);
        assertEquals(3, _config.size());
        assertEquals(null, _config.getItemValue(_childKey11));
        assertEquals("21", _config.getItemValue(_childKey21));
    }

    public void testRetainAll2() {
        // Copy the base config
        Config test = new DefaultConfig(_config);
        assertEquals(6, test.size());

        // Retain a non-existing key -- removes everything
        test.retainAll(new ItemKey[] { new ItemKey("test")});
        assertEquals(0, test.size());

        // Retain the childKey11 and childKey21 (also saves grandchildKey111)
        _config.retainAll(new ItemKey[] { _childKey11, _childKey21, });
        assertEquals(3, _config.size());
        assertEquals(null, _config.getItemValue(_childKey12));
        assertEquals(null, _config.getItemValue(_childKey22));
        assertEquals(null, _config.getItemValue(_childKey23));
        assertEquals("11", _config.getItemValue(_childKey11));
        assertEquals("111", _config.getItemValue(_grandchildKey111));
        assertEquals("21", _config.getItemValue(_childKey21));
    }

    public void testRetainAll3() {
        // Copy the base config
        Config test = new DefaultConfig(_config);
        assertEquals(6, test.size());

        // retain everything -- nothing should be changed
        _config.retainAll(test);
        assertEquals(6, test.size());

        // retain nothing -- everything should be removed
        test.retainAll(new DefaultConfig());
        assertEquals(0, test.size());

        // retain the descendents of parentKey1
        test = _getSubconfig(new ItemKey[] {
            _childKey11,
            _childKey12,
            _grandchildKey111,
        });
        _config.retainAll(test);
        assertEquals(3, _config.size());
        assertEquals("11", _config.getItemValue(_childKey11));
        assertEquals(null, _config.getItemValue(_childKey21));
    }
}
