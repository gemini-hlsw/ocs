package edu.gemini.spModel.config;

import edu.gemini.spModel.config2.ConfigSequence;

/**
 * An interface that is implemented by an instrument that performs
 * post-processing on a generated queue.  For example, to set the read mode
 * associated with each step.
 */
public interface ConfigPostProcessor {
    ConfigSequence postProcessSequence(ConfigSequence in);
}
