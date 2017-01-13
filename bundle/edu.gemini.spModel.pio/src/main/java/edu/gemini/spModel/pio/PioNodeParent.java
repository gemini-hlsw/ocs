//
// $Id: PioNodeParent.java 4938 2004-08-14 21:41:51Z shane $
//

package edu.gemini.spModel.pio;

import java.util.List;

/**
 * An interface that identifies {@link PioNode}s that contain other PioNodes.
 * Useful for writing generic tree traversal code.  Methods for adding children
 * to the parent node are not provided, because the type of PioNodes accepted
 * by specific parents is restricted.  For example, {@link ParamSet} may house
 * other ParamSets or {@link Param}s, but not {@link Container}s.
 */
public interface PioNodeParent extends PioNode {

    /**
     * Gets the number of {@link PioNode}s contained in this parent.
     * @return count of nodes contained in this parent
     *
     * @throws PioReferenceException if this node is a {@link ParamSet} that
     * references another (see {@link ParamSet#getReferenceId}) but the
     * referent is not found
     */
    int getChildCount();

    /**
     * Gets the {@link PioNode} children contained in this parent.
     * @return List of {@link PioNode} children of this parent
     *
     * @throws PioReferenceException if this node is a {@link ParamSet} that
     * references another (see {@link ParamSet#getReferenceId}) but the
     * referent is not found
     */
    List<PioNode> getChildren();

    /**
     * Gets the (immediate) child with the given name, if there is one.
     * Grandchildren and other decendants are not searched.  To find a more
     * distant decendant, use {@link #lookupNode}.
     *
     * @param name name of the child node to return
     *
     * @return the PioNamedNode with the given name, or <code>null</code> if
     * there is no such node
     *
     * @throws PioReferenceException if this node is a {@link ParamSet} that
     * references another (see {@link ParamSet#getReferenceId}) but the
     * referent is not found
     */
    PioNamedNode getChild(String name);

    /**
     * Finds the node indicated by the given <code>path</path> taking this
     * node as the reference point unless the PioPath is absolute.  In other
     * words, relative paths are taken to be rooted at this node, but
     * absolute paths may be used to search from the root of the PIO tree
     * that contains this node.
     *
     * @param path identifies the node to retrieve
     *
     * @return node associated with the given <code>path</code> if it exists,
     * <code>null</code> otherwise
     *
     * @throws PioReferenceException if this node is a {@link ParamSet} that
     * references another (see {@link ParamSet#getReferenceId}) but the
     * referent is not found
     */
    PioNode lookupNode(PioPath path);

    /**
     * Removes the given child from this parent, assuming it is a child of this
     * node.  Does nothing otherwise.
     *
     * @param child the {@link PioNode} that should be removed
     *
     * @return <code>true</code> if the child node is removed,
     * <code>false</code> if it did not exist in this node
     *
     * @throws PioReferenceException if this node is a {@link ParamSet} that
     * references another (see {@link ParamSet#getReferenceId}) but the
     * referent is not found
     */
    boolean removeChild(PioNode child);

    /**
     * Removes the given child by name, assuming there is a {@link PioNamedNode}
     * that is a child of this node.  Otherwise does nothing.
     *
     * @param name the name of the {@link PioNamedNode} to remove
     *
     * @return PioNamedNode that was removed, if any; <code>null</code> if there
     * was no child with the given name in the parent
     *
     * @throws PioReferenceException if this node is a {@link ParamSet} that
     * references another (see {@link ParamSet#getReferenceId}) but the
     * referent is not found
     */
    PioNamedNode removeChild(String name);
}
