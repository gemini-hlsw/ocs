package edu.gemini.spModel.gemini.seqcomp.smartgcal;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.spModel.config.MetaDataConfig;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;

/**
 * Utility for detecting smart gcal mapping errors and computing the error
 * message to display.
 */
public final class SmartgcalMappingErrorMessage {
    private static final ItemKey DATASET_KEY = new ItemKey("observe:dataLabel");

    private static final String MAP_ERROR_MESSAGE =
      "Smart calibration mapping%s for dataset%s %s %s not defined.  Please " +
      "use the manual GCal calibration component instead.";

    // Composite of node key and mapping error sequence predicates
    private static final class CompositionPredicate implements ConfigSequence.Predicate {
        private final ConfigSequence.Predicate f;

        public CompositionPredicate(SPNodeKey key) {
            this.f = new MetaDataConfig.NodeKeySequencePredicate(key);
        }

        public boolean matches(Config c) {
            return f.matches(c) && SmartgcalSysConfig.MAPPING_ERROR_PREDICATE.matches(c);
        }
    }

    private SmartgcalMappingErrorMessage() {
        // prevent instantiation
    }

    /**
     * Gets the error message to display for the given sequence, if any.
     *
     * @param seq configuration sequence to examine
     * @param nodeKey if supplied, only the subset of the sequence related to
     * the given node is checked; if <code>null</code>, the entire sequence is
     * checked
     *
     * @return the error message related to this sequence, if any;
     * <code>null</code> if none
     */
    public static String get(ConfigSequence seq, SPNodeKey nodeKey) {
        Config[] errors = seq.filter(new CompositionPredicate(nodeKey)).getAllSteps();
        if (errors.length == 0) return null;

        String[] datasets = mapToDatasetLabels(errors);
        String plural = datasets.length == 1 ? ""   : "s";
        String isAre  = datasets.length == 1 ? "is" : "are";
        String labels = formatLabels(datasets);

        return String.format(MAP_ERROR_MESSAGE, plural, plural, labels, isAre);
    }


    // Extract the dataset labels from the given Configs.
    private static String[] mapToDatasetLabels(Config[] configList) {
        String[] res = new String[configList.length];
        for (int i = 0; i<configList.length; ++i) {
            res[i] = (String) configList[i].getItemValue(DATASET_KEY);
        }
        return res;
    }

    // Formats a string containing the dataset labels for which there is a
    // problem.
    private static String formatLabels(String[] labels) {
        switch (labels.length) {
            case  1: return labels[0];
            case  2: return labels[0] + " and " + labels[1];
            case  3: return labels[0] + ", " + labels[1] + " and " + labels[2];
            default: return labels[0] + ", " + labels[1] + ", " + labels[2] + ", etc.";
        }
    }
}
