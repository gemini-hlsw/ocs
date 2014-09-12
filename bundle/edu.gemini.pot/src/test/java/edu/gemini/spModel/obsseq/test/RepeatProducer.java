//
// $Id: RepeatProducer.java 47163 2012-08-01 23:09:47Z rnorris $
//
package edu.gemini.spModel.obsseq.test;

import edu.gemini.spModel.obsseq.ConfigProducer;
import edu.gemini.spModel.obsseq.ConfigMerger;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.Config;

class RepeatProducer implements ConfigProducer {

    private int _repeatCount;

    RepeatProducer(int repeatCount) {
        _repeatCount = repeatCount;
    }

    int getRepeatCount() {
        return _repeatCount;
    }

    void setRepeatCount(int repeatCount) {
        _repeatCount = repeatCount;
    }

    public int getStepCount() {
        return _repeatCount;
    }

    public ItemKey[] getIteratedKeys() {
        return new ItemKey[0];
    }

    public ConfigMerger getConfigMerger() {
        return new ConfigMerger() {
            private int _step;

            public boolean hasNextConfig() {
                return _step < _repeatCount;
            }

            public void mergeNextConfig(Config config) {
                ++_step;
            }
        };
    }
}
