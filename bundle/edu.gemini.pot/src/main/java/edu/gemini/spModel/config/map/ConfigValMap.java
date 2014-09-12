package edu.gemini.spModel.config.map;

import java.io.Serializable;

/**
 * Defines a function for mapping a configuration value to another kind of
 * object.
 */
public interface ConfigValMap extends Serializable {
    Object apply(Object val);
}
