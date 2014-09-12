//
// $
//

package edu.gemini.spModel.ext;

/**
 * Work in progress...
 */
public interface ObservationNodeVisitor {
    void visitAoNode(AoNode node);
    void visitConstraintsNode(ConstraintsNode node);
    void visitInstrumentNode(InstrumentNode node);
    void visitSequenceNode(SequenceNode node);
    void visitTargetNode(TargetNode node);
}
