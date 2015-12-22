package edu.gemini.spModel.config2.test;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.ItemEntry;
import edu.gemini.spModel.config2.DefaultConfig;
import junit.framework.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Shared utilities for test cases dealing with configs.
 */
public final class ConfigTestUtils {

    /**
     * Assert that the specified configuration contains the information in
     * <code>itemVals.
     *
     * @param itemVals table of item information where each row specifies an
     * item key name and its value; so itemVal[i][0] is always a String key
     * name, and itemVal[i][1] is the value
     *
     * @param config configuration whose contents are checked against those of
     * <code>itemVals</code>
     */
    public static void assertConfigContains(Object[][] itemVals, Config config) {
        Assert.assertEquals(itemVals.length, config.size());
        for (Object[] itemVal : itemVals) {
            ItemKey key = new ItemKey((String) itemVal[0]);
            Assert.assertEquals(itemVal[1], config.getItemValue(key));
        }
    }

    public static Config[] createConfigs(Object[][][] steps) {
        Config[] configs = new Config[steps.length];
        for (int i=0; i<steps.length; ++i) {
            configs[i] = new DefaultConfig();

            Object[][] step = steps[i];
            for (Object[] itemDesc : step) {
                ItemKey key = new ItemKey((String) itemDesc[0]);
                Object val = itemDesc[1];
                configs[i].putItem(key, val);
            }
        }
        return configs;
    }

    public static void assertValues(Object[] expected, Object[] actual) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i=0; i<expected.length; ++i) {
            Assert.assertEquals(expected[i], actual[i]);
        }
    }

    public static void assertUnorderedValues(Object[] expected, Object[] actual) {
        Assert.assertEquals(expected.length, actual.length);

        Set<Object> expectedset = new HashSet<>();
        Set<Object> actualset = new HashSet<>();
        for (int i=0; i<expectedset.size(); ++i) {
            expectedset.add(expected[i]);
            actualset.add(actual[i]);
        }
        Assert.assertEquals(expectedset, actualset);
    }

    public static void printConfig(Config config) {
        ItemEntry[] entryArray = config.itemEntries();
        Arrays.sort(entryArray, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                ItemEntry ie1 = (ItemEntry) o1;
                ItemEntry ie2 = (ItemEntry) o2;

                return ie1.getKey().compareTo(ie2.getKey());
            }
        });

        for (ItemEntry ie : entryArray) {
            System.out.println(ie.getKey() + " -> " + ie.getItemValue());
        }
    }
}
