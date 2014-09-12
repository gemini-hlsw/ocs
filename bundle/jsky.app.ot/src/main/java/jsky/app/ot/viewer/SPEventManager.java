//
// $Id: SPEventManager.java 47000 2012-07-26 19:15:10Z swalker $
//

package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPContainerNode;
import edu.gemini.pot.sp.ISPNode;

import java.beans.PropertyChangeListener;

public class SPEventManager {

    private final PropertyChangeListener _listener;
    private ISPNode _rootNode;

    public SPEventManager(PropertyChangeListener listener) {
        _listener = listener;
    }

    public synchronized ISPNode getRootNode() {
        return _rootNode;
    }

    public synchronized void setRootNode(ISPNode node) {
        if (node != _rootNode) {

            // Remove old registrations
            if (_rootNode != null) {
                _rootNode.removeCompositeChangeListener(_listener);
                if (_rootNode instanceof ISPContainerNode) {
                    ((ISPContainerNode)_rootNode).removeStructureChangeListener(_listener);
                }
            }

            // Swap
            _rootNode = node;

            // Add new registrations
            if (node != null) {
                node.addCompositeChangeListener(_listener);
                if (node instanceof ISPContainerNode) {
                    ((ISPContainerNode) node).addStructureChangeListener(_listener);
                }
            }

        }
    }

}
