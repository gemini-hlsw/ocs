//
// $Id: PioNamedNode.java 4904 2004-08-06 13:03:04Z shane $
//
package edu.gemini.spModel.pio;

/**
 * An interface that identifies PioNodes that have a name.
 */
public interface PioNamedNode extends PioNode {
    /**
     * Returns the name of this PIO node, if set, <code>null</code>
     * otherwise.
     */
    String getName();

}
