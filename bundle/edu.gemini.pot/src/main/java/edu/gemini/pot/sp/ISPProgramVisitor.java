//
// $Id: ISPProgramVisitor.java 46866 2012-07-20 19:35:51Z swalker $
//
package edu.gemini.pot.sp;

/**
 * Visitor pattern support for {@link ISPProgramNode}s.
 */
public interface ISPProgramVisitor {
    void visitConflictFolder(ISPConflictFolder node);
    void visitObsExecLog(ISPObsExecLog node);
    void visitObsQaLog(ISPObsQaLog node);
    void visitObsComponent(ISPObsComponent node);
    void visitObservation(ISPObservation node);
    void visitGroup(ISPGroup node);
    void visitProgram(ISPProgram node);
    void visitSeqComponent(ISPSeqComponent node);
    void visitTemplateFolder(ISPTemplateFolder node);
    void visitTemplateGroup(ISPTemplateGroup node);
    void visitTemplateParameters(ISPTemplateParameters node);
}
