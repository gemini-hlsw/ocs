package edu.gemini.pot.sp;

/**
 * An interface that indicates that the {@link ISPNode} is a
 * component of an {@link ISPProgram} and provides visitor support.
 */
public interface ISPProgramNode extends ISPNode {

    // I think some of the methods in ISPNode should move to this
    // class.  ISPNode is used as the base of anything that is stored
    // in the ODB.  But not everything should have an SPProgramID for
    // example.

    /**
     * Provides visitor pattern support.
     */
    void accept(ISPProgramVisitor visitor);
}
