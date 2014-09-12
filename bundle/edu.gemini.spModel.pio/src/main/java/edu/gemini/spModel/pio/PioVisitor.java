//
// $Id: PioVisitor.java 4937 2004-08-14 21:35:20Z shane $
//
package edu.gemini.spModel.pio;

/**
 * A visitor of PIO document nodes, in support of the "Visitor" design pattern.
 */
public interface PioVisitor {
    void visitDocument(Document document);
    void visitContainer(Container container);
    void visitParamSet(ParamSet paramSet);
    void visitParam(Param param);
}
