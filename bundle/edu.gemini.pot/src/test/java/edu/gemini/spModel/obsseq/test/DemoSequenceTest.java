//
// $Id: DemoSequenceTest.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.spModel.obsseq.test;

import junit.framework.TestCase;
import edu.gemini.spModel.obsseq.ConfigMerger;
import edu.gemini.spModel.obsseq.Sequence;
import edu.gemini.spModel.obsseq.DefaultConfigProducer;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.test.ConfigTestUtils;

public class DemoSequenceTest extends TestCase {

    // repeat 2x
    //     inst 3x (filter, wavelength)
    //         offset 2x
    //             observe
    //     cal

    private Sequence _masterSeq;

    private Sequence _instSeq;
    private DefaultConfigProducer _instProducer;

    private Sequence _offsetSeq;
    private DefaultConfigProducer _offsetProducer;

    private Sequence _observeSeq;
    private DefaultConfigProducer _observeProducer;

    private Sequence _calSeq;
    private DefaultConfigProducer _calProducer;

    public void setUp() {
        // Create the master, containing, sequence.  A repeat loop.
        _masterSeq = new Sequence(new RepeatProducer(2));


        // Create the instrument sequence.
        Config step0 = new DefaultConfig();
        step0.putItem(new ItemKey("inst:filter"), "J");
        step0.putItem(new ItemKey("inst:wavelength"), new Double(100.0));

        Config step1 = new DefaultConfig();
        step1.putItem(new ItemKey("inst:filter"), "J'");
        step1.putItem(new ItemKey("inst:wavelength"), new Double(101.0));

        Config step2 = new DefaultConfig();
        step2.putItem(new ItemKey("inst:filter"), "K");
        step2.putItem(new ItemKey("inst:wavelength"), new Double(102.0));

        _instProducer = new DefaultConfigProducer(
                new Config[] { step0, step1, step2 });
        _instSeq = new Sequence(_instProducer);


        // Create the offset sequence
        step0 = new DefaultConfig();
        step0.putItem(new ItemKey("tcs:p"), new Integer(10));
        step0.putItem(new ItemKey("tcs:q"), new Integer(-10));

        step1 = new DefaultConfig();
        step1.putItem(new ItemKey("tcs:p"), new Integer(0));
        step1.putItem(new ItemKey("tcs:q"), new Integer(-10));

        _offsetProducer = new DefaultConfigProducer(
                new Config[] { step0, step1 });
        _offsetSeq = new Sequence(_offsetProducer);


        // Create the observe sequence
        step0 = new DefaultConfig();
        step0.putItem(new ItemKey("observe:observeType"), "OBJECT");

        _observeProducer = new DefaultConfigProducer(new Config[] { step0 });
        _observeSeq = new Sequence(_observeProducer);


        // Create the calibration sequence
        step0 = new DefaultConfig();
        step0.putItem(new ItemKey("observe:observeType"), "CAL");

        _calProducer = new DefaultConfigProducer(new Config[] { step0 });
        _calSeq = new Sequence(_calProducer);

        // Put the sequence together
        _offsetSeq.addSequence(_observeSeq);
        _instSeq.addSequence(_offsetSeq);
        _masterSeq.addSequence(_instSeq);
        _masterSeq.addSequence(_calSeq);
    }

    public void testSetup() {
        assertEquals(14, _masterSeq.getStepCount());

        ConfigMerger merger = _masterSeq.getConfigMerger();


        Object[][][] steps = new Object[][][] {
            {
                { "inst:filter",         "J"               },
                { "inst:wavelength",     new Double(100.0) },
                { "tcs:p",               new Integer(10)   },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "J"               },
                { "inst:wavelength",     new Double(100.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "J'"              },
                { "inst:wavelength",     new Double(101.0) },
                { "tcs:p",               new Integer(10)   },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "J'"              },
                { "inst:wavelength",     new Double(101.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "K"               },
                { "inst:wavelength",     new Double(102.0) },
                { "tcs:p",               new Integer(10)   },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "K"               },
                { "inst:wavelength",     new Double(102.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "K"               },
                { "inst:wavelength",     new Double(102.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "CAL"             },
            },
            {
                { "inst:filter",         "J"               },
                { "inst:wavelength",     new Double(100.0) },
                { "tcs:p",               new Integer(10)   },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "J"               },
                { "inst:wavelength",     new Double(100.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "J'"              },
                { "inst:wavelength",     new Double(101.0) },
                { "tcs:p",               new Integer(10)   },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "J'"              },
                { "inst:wavelength",     new Double(101.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "K"               },
                { "inst:wavelength",     new Double(102.0) },
                { "tcs:p",               new Integer(10)   },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "K"               },
                { "inst:wavelength",     new Double(102.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "OBJECT"          },
            },
            {
                { "inst:filter",         "K"               },
                { "inst:wavelength",     new Double(102.0) },
                { "tcs:p",               new Integer(0)    },
                { "tcs:q",               new Integer(-10)  },
                { "observe:observeType", "CAL"             },
            },
        };

        Config config = new DefaultConfig();
        for (int i=0; i<steps.length; ++i) {
            assertTrue(merger.hasNextConfig());
            merger.mergeNextConfig(config);

//            System.out.println("----");
//            ConfigTestUtils.printConfig(config);

            Object[][] configInfo = steps[i];
            ConfigTestUtils.assertConfigContains(configInfo, config);
        }
    }
}
