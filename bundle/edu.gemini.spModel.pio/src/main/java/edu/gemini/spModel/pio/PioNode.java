//
// $Id: PioNode.java 4937 2004-08-14 21:35:20Z shane $
//

package edu.gemini.spModel.pio;

import java.io.Serializable;

/**
 * Common interface for all PIO nodes:
 * {@link Document}, {@link Container}, {@link ParamSet}, and {@link Param}.
 */
public interface PioNode extends Serializable {

    /**
     * Gets the parent, containing {@link PioNodeParent} in which this node is
     * nested.
     *
     * @return the {@link PioNodeParent} that contains this PioNode, if any;
     * <code>null</code> otherwise
     */
    PioNodeParent getParent();

    /**
     * Detaches this node from its {@link PioNodeParent parent}, if there is
     * one.  Otherwise does nothing.
     */
    void detach();

    /**
     * Gets the path to this element from the root ancestor element in which
     * it is contained.
     */
    PioPath getPath();

    /**
     * Provides visitor pattern support.
     *
     * @param visitor object containing a "visit" method for each type of
     * PIO node; the appropriate one will be called based upon the type
     * of this object
     */
    void accept(PioVisitor visitor);
}
