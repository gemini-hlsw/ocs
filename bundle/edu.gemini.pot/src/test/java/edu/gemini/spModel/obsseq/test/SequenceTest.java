//
// $Id: SequenceTest.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.spModel.obsseq.test;

import junit.framework.TestCase;
import edu.gemini.spModel.obsseq.Sequence;
import edu.gemini.spModel.obsseq.DefaultConfigProducer;
import edu.gemini.spModel.obsseq.ConfigMerger;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.test.ConfigTestUtils;

public class SequenceTest extends TestCase {

    private Object[][][] _instSteps = new Object[][][] {
        {
            { "inst:filter",        "J" },
            { "inst:wavelength",    new Double(100.0) },
        },
        {
            { "inst:filter",        "J'" },
            { "inst:wavelength",    new Double(101.0) },
        },
        {
            { "inst:filter",        "K" },
            { "inst:wavelength",    new Double(102.0) },
        },
    };

    private Object[][][] _offsetSteps = new Object[][][] {
        {
            { "tcs:p",              new Integer(10) },
            { "tcs:q",              new Integer( 0) },
        },
        {
            { "tcs:p",              new Integer(20) },
            { "tcs:q",              new Integer( 0) },
        },
    };

    private Object[][][] _ocsSteps = new Object[][][] {
        {
            { "ocs:x",              "1" },
        },
        {
            { "ocs:x",              "2" },
        }
    };

    private void assertConfigMerger(Object[][][] steps, ConfigMerger merger) {
        assertConfigMerger(steps, merger, false);
    }

    private void assertConfigMerger(Object[][][] steps, ConfigMerger merger, boolean print) {
        Config config = new DefaultConfig();
        for (int i=0; i<steps.length; ++i) {
            assertTrue(merger.hasNextConfig());
            merger.mergeNextConfig(config);

            Object[][] step = steps[i];

            if (print) {
                System.out.println("------");
                System.out.println("  Expected:");
                for (int j=0; j<step.length; ++j) {
                    System.out.println("    " + step[j][0] + " - > " + step[j][1]);
                }
                System.out.println("  Actual:");
                ConfigTestUtils.printConfig(config);
            }

            ConfigTestUtils.assertConfigContains(step, config);
        }
    }

    public void test_NoConfig_0Child_Sequence() {
        Sequence seq = new Sequence();
        ConfigMerger configMerger = seq.getConfigMerger();
        assertFalse(configMerger.hasNextConfig());
    }

    public void test_Config_0Child_Sequence() {
        // Setup a childless sequence with three steps.
        Config[] configs = ConfigTestUtils.createConfigs(_instSteps);
        DefaultConfigProducer producer = new DefaultConfigProducer(configs);

        Sequence seq = new Sequence();
        seq.setConfigProducer(producer);

        // Get the config merger from it and make sure it contains what
        // we expect.
        assertConfigMerger(_instSteps, seq.getConfigMerger());
    }

    public void test_NoConfig_1Child_Sequence() {
        // Setup a childless sequence with three steps.
        Config[] configs = ConfigTestUtils.createConfigs(_instSteps);
        DefaultConfigProducer producer = new DefaultConfigProducer(configs);

        Sequence child = new Sequence();
        child.setConfigProducer(producer);

        // Now make a parent sequence to contain it.
        Sequence parent = new Sequence();
        parent.addSequence(child);

        // Get the config merger from it and make sure it contains what
        // we expect.
        assertConfigMerger(_instSteps, parent.getConfigMerger());
    }

    public void test_Config_1Child_Sequence() {
        // Setup a sequence with offset pos nested inside of instrument changes
        Config[] instConfigs = ConfigTestUtils.createConfigs(_instSteps);
        Config[] offsConfigs = ConfigTestUtils.createConfigs(_offsetSteps);

        Sequence parent = new Sequence(new DefaultConfigProducer(instConfigs));
        Sequence child  = new Sequence(new DefaultConfigProducer(offsConfigs));

        parent.addSequence(child);

        // Expected steps:
        Object[][][] expected = new Object[][][] {
            {
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
        };

        assertConfigMerger(expected, parent.getConfigMerger());
    }

    public void test_NoConfig_2Child_Sequence() {
        // Setup a sequence with offset pos nested inside of instrument changes
        Config[] instConfigs = ConfigTestUtils.createConfigs(_instSteps);
        Config[] offsConfigs = ConfigTestUtils.createConfigs(_offsetSteps);

        Sequence child0 = new Sequence(new DefaultConfigProducer(instConfigs));
        Sequence child1 = new Sequence(new DefaultConfigProducer(offsConfigs));
        Sequence parent = new Sequence();

        parent.addSequence(child0);
        parent.addSequence(child1);

        // Expected steps:
        Object[][][] expected = new Object[][][] {
            {
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
            },
            {
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
        };

        assertConfigMerger(expected, parent.getConfigMerger());
    }

    public void test_Config_2Child_Sequence() {
        // Setup a sequence with offset pos nested inside of instrument changes
        Config[] instConfigs = ConfigTestUtils.createConfigs(_instSteps);
        Config[] offsConfigs = ConfigTestUtils.createConfigs(_offsetSteps);
        Config[] ocsConfigs  = ConfigTestUtils.createConfigs(_ocsSteps);

        Sequence child0 = new Sequence(new DefaultConfigProducer(instConfigs));
        Sequence child1 = new Sequence(new DefaultConfigProducer(offsConfigs));
        Sequence parent = new Sequence(new DefaultConfigProducer(ocsConfigs));

        parent.addSequence(child0);
        parent.addSequence(child1);

        // Expected steps:
        Object[][][] expected = new Object[][][] {
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
        };

        assertConfigMerger(expected, parent.getConfigMerger());
    }

    public void test_NoConfig_Grandchild_Sequence() {
        // Setup a sequence with offset pos nested inside of instrument changes
        Config[] instConfigs = ConfigTestUtils.createConfigs(_instSteps);
        Config[] offsConfigs = ConfigTestUtils.createConfigs(_offsetSteps);

        Sequence parent     = new Sequence();
        Sequence child      = new Sequence(new DefaultConfigProducer(instConfigs));
        Sequence grandchild = new Sequence(new DefaultConfigProducer(offsConfigs));

        parent.addSequence(child);
        child.addSequence(grandchild);

        // Expected steps:
        Object[][][] expected = new Object[][][] {
            {
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
        };

        assertConfigMerger(expected, parent.getConfigMerger());
    }

    public void test_Config_Grandchild_Sequence() {
        // Setup a sequence with offset pos nested inside of instrument changes
        Config[] instConfigs = ConfigTestUtils.createConfigs(_instSteps);
        Config[] offsConfigs = ConfigTestUtils.createConfigs(_offsetSteps);
        Config[] ocsConfigs  = ConfigTestUtils.createConfigs(_ocsSteps);

        Sequence parent     = new Sequence(new DefaultConfigProducer(ocsConfigs));
        Sequence child      = new Sequence(new DefaultConfigProducer(instConfigs));
        Sequence grandchild = new Sequence(new DefaultConfigProducer(offsConfigs));

        parent.addSequence(child);
        child.addSequence(grandchild);

        // Expected steps:
        Object[][][] expected = new Object[][][] {
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "1"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "J"                 },
                { "inst:wavelength",    new Double(100.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "J'"                },
                { "inst:wavelength",    new Double(101.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(10)     },
                { "tcs:q",              new Integer(0)      },
            },
            {
                { "ocs:x",              "2"                 },
                { "inst:filter",        "K"                 },
                { "inst:wavelength",    new Double(102.0)   },
                { "tcs:p",              new Integer(20)     },
                { "tcs:q",              new Integer(0)      },
            },
        };

        assertConfigMerger(expected, parent.getConfigMerger(), false);
    }
}
