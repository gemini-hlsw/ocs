//
// $Id: Document.java 5303 2004-11-05 21:59:10Z shane $
//
package edu.gemini.spModel.pio;

/**
 * Document is the root node containing all the parameter information for an
 * application's state (for example, a Science Program or Phase 1 Document).
 * This interface corresponds to the <code>document</code> element in
 * SpXML2.dtd. Documents can contain any number of {@link Container} instances.
 */
public interface Document extends ContainerParent {

}
