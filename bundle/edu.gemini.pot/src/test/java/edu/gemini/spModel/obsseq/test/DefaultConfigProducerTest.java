//
// $Id: DefaultConfigProducerTest.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.spModel.obsseq.test;

import junit.framework.TestCase;
import edu.gemini.spModel.obsseq.DefaultConfigProducer;
import edu.gemini.spModel.obsseq.ConfigMerger;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.test.ConfigTestUtils;

public class DefaultConfigProducerTest extends TestCase {

    private DefaultConfigProducer _emptyProducer;
    private DefaultConfigProducer _producer;

    private Config _config0;
    private Config _config1;
    private Config _config2;

    private ItemKey _nochangeKey;
    private ItemKey _changeKey;
    private ItemKey _new1Key;
    private ItemKey _new2Key;


    public void setUp() {
        _emptyProducer = new DefaultConfigProducer();

        _nochangeKey = new ItemKey("ocs:nochange");
        _changeKey   = new ItemKey("ocs:change");
        _new1Key     = new ItemKey("ocs:new1");
        _new2Key     = new ItemKey("ocs:new2");

        _config0 = new DefaultConfig();
        _config0.putItem(_nochangeKey, "nochange");
        _config0.putItem(_changeKey,   "change0");

        _config1 = new DefaultConfig();
        _config1.putItem(_changeKey, "change1");
        _config1.putItem(_new1Key,   "new1");

        _config2 = new DefaultConfig();
        _config2.putItem(_nochangeKey, "nochange");
        _config2.putItem(_changeKey,   "change2");
        _config2.putItem(_new2Key,     "new2");

        _producer = new DefaultConfigProducer();
        _producer.addConfig(_config0);
        _producer.addConfig(_config1);
        _producer.addConfig(_config2);
    }

    public void testGetStepCount() {
        assertEquals(0, _emptyProducer.getStepCount());
        assertEquals(3, _producer.getStepCount());
    }

    public void testGetIteratedKeys() {
        ItemKey[] keys = _emptyProducer.getIteratedKeys();
        assertEquals(0, keys.length);

        keys = _producer.getIteratedKeys();
        ConfigTestUtils.assertUnorderedValues(new ItemKey[] {
            _changeKey, _new1Key, _new2Key,
        }, keys);
    }

    public void testGetConfigMerger() {
        ConfigMerger merger = _emptyProducer.getConfigMerger();
        assertFalse(merger.hasNextConfig());

        merger = _producer.getConfigMerger();

        // step 0
        assertTrue(merger.hasNextConfig());
        Config config = new DefaultConfig();
        merger.mergeNextConfig(config);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(),    "nochange" },
            { _changeKey.getPath(),      "change0"  },
        }, config);

        // step 1
        assertTrue(merger.hasNextConfig());
        merger.mergeNextConfig(config);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(),    "nochange" },
            { _changeKey.getPath(),      "change1"  },
            { _new1Key.getPath(),        "new1"     },
        }, config);

        // step 2
        assertTrue(merger.hasNextConfig());
        merger.mergeNextConfig(config);

        ConfigTestUtils.assertConfigContains(new String[][] {
            { _nochangeKey.getPath(),    "nochange" },
            { _changeKey.getPath(),      "change2"  },
            { _new1Key.getPath(),        "new1"     },
            { _new2Key.getPath(),        "new2"     },
        }, config);

        assertFalse(merger.hasNextConfig());
    }

    public void testGetConfigs() {
        Config[] configs = _emptyProducer.getConfigs();
        assertEquals(0, configs.length);

        configs = _producer.getConfigs();
        assertEquals(3, configs.length);
        assertTrue(_config0 == configs[0]);
        assertTrue(_config1 == configs[1]);
        assertTrue(_config2 == configs[2]);
    }

    public void testSetConfigs() {
        _emptyProducer.setConfigs(new Config[] { _config2} );
        Config[] configs = _emptyProducer.getConfigs();
        assertEquals(1, configs.length);
        assertTrue(_config2 == configs[0]);

        _producer.setConfigs(new Config[0]);
        configs = _producer.getConfigs();
        assertEquals(0, configs.length);
    }

    // remainder of the methods are simple wrappers around ArrayList
}
