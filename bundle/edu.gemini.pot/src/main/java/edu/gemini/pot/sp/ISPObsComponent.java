package edu.gemini.pot.sp;

/**
 * This is the interface for a Science Program Observation Component node.
 */
public interface ISPObsComponent extends ISPProgramNode {
    /**
     * Returns the type of this observation component.
     */
    SPComponentType getType();
}

