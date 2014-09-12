//
// $Id: PioNodeElement.java 4888 2004-08-02 22:56:25Z shane $
//

package edu.gemini.spModel.pio.xml;

import org.dom4j.Element;

/**
 * An interface implemented by every Element subclass used in this PIO
 * implementation.  Identifies Elements associated with PioNodes, and provides
 * a generic way to get the node itself via the {@link #getPioNode} method.
 */
interface PioNodeElement extends Element {

    /**
     * Gets the PioNode (subinterface) implementation node.  This is used,
     * for example, to make it possible to
     */
    PioNodeImpl getPioNode();
}
