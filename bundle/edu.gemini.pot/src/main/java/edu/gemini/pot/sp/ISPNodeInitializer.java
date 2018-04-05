package edu.gemini.pot.sp;

import edu.gemini.spModel.data.ISPDataObject;

import java.io.Serializable;

/**
 * Initializers provide a means whereby newly created Science Program nodes
 * may be initialized before being returned to the client.  For instance,
 * the <code>{@link ISPFactory}</code> provides a methods to register
 * initializers of various types that it then uses when creating nodes of
 * the matching type.  "Initialization" may refer to any setup work that
 * must be performed, including adding specific user objects, component
 * objects, or child nodes.
 */
public interface ISPNodeInitializer<N extends ISPNode, D extends ISPDataObject> extends Serializable {

    /**
     * Gets the SPComponentType associated with the node.
     */
    SPComponentType getType();

    /**
     * Creates a new data object associated with the component type.
     */
    D createDataObject();

    /**
     * Initializes the given <code>node</code>.
     *
     * @param factory the factory that may be used to create any required
     * science program nodes
     *
     * @param node the science program node to be initialized
     */
    default void initNode(ISPFactory factory, N node) {
        node.setDataObject(createDataObject());
        updateNode(node);
    }

    /**
     * Updates the given <code>node</code>. This should be called on any new
     * nodes created by making a deep copy of another node, so that the user
     * objects are updated correctly.
     *
     * @param node the science program node to be updated
     */
    default void updateNode(N node) {
        // do nothing
    }

}

