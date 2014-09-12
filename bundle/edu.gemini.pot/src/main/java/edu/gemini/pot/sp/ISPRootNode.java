package edu.gemini.pot.sp;

import edu.gemini.spModel.core.SPProgramID;

/**
 * Marks a node that serves as a root of the tree (for example, ISPProgram).
 */
public interface ISPRootNode extends ISPNode {
    SPProgramID getProgramID();

    /**
     * Returns the time that the program was last modified.
     */
    long lastModified();
}
