//
// $Id: SPAbstractProgramVisitor.java 46866 2012-07-20 19:35:51Z swalker $
//
package edu.gemini.pot.sp;

/**
 * An abstract base class for program node visitor implementations.
 * The idea being, if the subclass doesn't need to perform an action
 * for one or more of the program types, there is no need to implement
 * the unneeded methods.
 *
 * <p>All method implementations are empty.  It is up to the subclass
 * to define the content of the methods that are required for its
 * functionality.</p>
 */
public abstract class SPAbstractProgramVisitor implements ISPProgramVisitor {
    public void visitConflictFolder(ISPConflictFolder node) {
    }

    public void visitObsComponent(ISPObsComponent node) {
    }

    public void visitObservation(ISPObservation node) {
    }

    public void visitGroup(ISPGroup node) {
    }

    public void visitProgram(ISPProgram node) {
    }

    public void visitSeqComponent(ISPSeqComponent node) {
    }

    public void visitTemplateFolder(ISPTemplateFolder node) {
    }

    public void visitTemplateGroup(ISPTemplateGroup node) {
    }

    public void visitTemplateParameters(ISPTemplateParameters node) {
    }
}
