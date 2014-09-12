//
// $Id: ISPEventMonitor.java 6326 2005-06-08 00:40:35Z shane $
//

package edu.gemini.pot.sp;

/**
 * An interface that may be implemented by client data objects to be informed
 * of changes to nodes that are nested inside of their parent remote node.
 * See {@link ISPNode#getClientData(String)}.
 */
public interface ISPEventMonitor {
    /**
     * Handles a structure change event, which indicates that the hierarchy of
     * remote nodes rooted at the node containing this client data object has
     * changed.  For example, child nodes or grandchild nodes have been added,
     * removed, or moved.
     */
    void structureChanged(SPStructureChange change);


    /**
     * Handles a composite change event, which indicates that a property has
     * changed in the node containing this client data object or in one of its
     * descendants.
     */
    void propertyChanged(SPCompositeChange change);
}
