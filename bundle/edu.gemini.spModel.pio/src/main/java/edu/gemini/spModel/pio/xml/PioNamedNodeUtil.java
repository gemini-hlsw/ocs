//
// $Id: PioNamedNodeUtil.java 4993 2004-08-19 18:34:50Z shane $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.PioPath;
import org.dom4j.Element;

//
// These methods cannot be adapted for {@link PioNodeImpl}
// because not all {@link edu.gemini.spModel.pio.PioNode}s are named
// (in particular, {@link edu.gemini.spModel.pio.Document}s are not named).
//
// A PioNamedNodeImpl could be created, but Container and ParamSets are also
// PioParents, and it is more beneficial to have a PioParentImpl.
//

/**
 * A utility class used by {@link edu.gemini.spModel.pio.PioNamedNode}
 * implementations.
 */
final class PioNamedNodeUtil {
    private PioNamedNodeUtil() {
    }

    /**
     * Looks up the "name" attribute in the Element referenced by the given
     * <code>node</code>.
     */
    public static String getName(PioNodeImpl node) {
        return node.getElement().attributeValue("name");
    }

    /**
     * Adds a "name" attribute to the Element referenced by the given
     * <code>node</code>.
     */
    public static void setName(PioNodeImpl node, String name) {
        node.setAttribute("name", name);
    }

    /**
     * Gets the PioPath that points to the given <code>node</code> with name
     * <code>myname</code>.
     */
    public static PioPath getPath(PioNodeImpl node, String myname) {
        Element parent = node.getElement().getParent();
        if (!(parent instanceof PioNodeElement)) {
            // No parent so the path just includes this element.
            return new PioPath("/" + myname);
        }

        // Extend the parent's path with our name
        PioPath parentPath = ((PioNodeElement) parent).getPioNode().getPath();
        return new PioPath(parentPath, myname);
    }
}
