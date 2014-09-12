//
// $Id: DatasetConfigService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config.map.ConfigValMap;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.dataset.DatasetLabel;



/**
 * The ConfigService is used to obtain the
 * {@link edu.gemini.spModel.config2.Config} related to a particular dataset.
 */
public final class DatasetConfigService {

    /**
     * Determines the {@link Config} in effect when the dataset with the given
     * label was/will be produced <em>according to the current definition of
     * the observing sequence</em>.  Should the observing sequence be modified
     * later, and this method called again, a different result may be returned.
     *
     * @param label label of the dataset whose configuration is sought
     * @param obs observation in which to search
     *
     * @return the Config that produced the dataset with <code>label</code>,
     * if any; <code>null</code> otherwise
     */
    public static Option<Config> deriveConfigForDataset(DatasetLabel label, ISPObservation obs, ConfigValMap map) {
        if (!label.getObservationId().equals(obs.getObservationID())) {
            return None.instance();
        } else {
            final ConfigSequence seq = ConfigBridge.extractSequence(obs, null, map);
            return configForStep(seq, label.getIndex() - 1);
        }
    }

    public static Option<Config> configForStep(ConfigSequence seq, int step) {
        return ((step < 0) || (step >= seq.size())) ? None.<Config>instance() : new Some<Config>(seq.getStep(step));
    }
}
