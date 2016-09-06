package edu.gemini.spModel.type;

/**
 * Implemented by SpTypes that have some members that are only for engineering use.
 */
public interface PartiallyEngineeringSpType {
    default boolean isEngineering() {
        return false;
    }
}
