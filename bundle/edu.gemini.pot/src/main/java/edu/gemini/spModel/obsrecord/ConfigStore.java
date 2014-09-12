//
// $
//

package edu.gemini.spModel.obsrecord;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

/**
 * Mapping to/from dataset labels and their configs.
 */
interface ConfigStore {
    ParamSet toParamSet(PioFactory factory);
    void addConfigAndLabel(Config config, DatasetLabel label);
    void remove(DatasetLabel label);
    Config getConfigForDataset(DatasetLabel label);
    boolean containsDataset(DatasetLabel label);
    ObsClass getObsClass(DatasetLabel label);
}
