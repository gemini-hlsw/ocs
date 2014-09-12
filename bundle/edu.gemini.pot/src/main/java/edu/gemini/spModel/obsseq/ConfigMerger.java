//
// $Id: ConfigMerger.java 5858 2005-03-08 13:50:11Z shane $
//
package edu.gemini.spModel.obsseq;

import edu.gemini.spModel.config2.Config;

/**
 * The ConfigMerger class is used to aid the construction of
 * {@link edu.gemini.spModel.config2.ConfigSequence} objects.  It is used by
 * the client, somewhat like an iterator, to step through the various
 * {@link Config}s offered by a {@link ConfigProducer}.  Its job is to simply
 * add the "next" set of items to the supplied Config with each successive
 * call to {@link #mergeNextConfig}.
 */
public interface ConfigMerger {
    /**
     * Returns <code>true</code> if there is more configuration information that
     * needs to be merged.  In other words, if there are more steps.
     */
    boolean hasNextConfig();

    /**
     * Merges the "next" set of items into the supplied <code>config</code>
     * argument.
     *
     * @param config configuration to which the next items should be applied
     */
    void mergeNextConfig(Config config);
}
