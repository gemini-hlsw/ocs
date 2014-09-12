package edu.gemini.spModel.util;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.ObservationStatus;

import java.io.Serializable;

import java.util.*;


/**
 * This functor gathers the information needed to display the science program tree and returns
 * it to the client as an object.
 *
 * @author Allan Brighton
 */
public class DBTreeListService {

    // Keys for sorting the tree nodes for display
    public static final String SORT_BY_NONE = "none";
    public static final String SORT_BY_ID = "Id";
    public static final String SORT_BY_STATUS = "Status";
    public static final String SORT_BY_TITLE = "Title";

    /** Stores information for one science program tree node */
    public static final class Node implements Serializable {

        private ISPNode _remoteNode;
        private ISPDataObject _dataObject;
        private SPNodeKey _nodeKey;
        private List<Node> _subNodes;

        Node(ISPNode remoteNode, List<Node> subNodes)  {
            _remoteNode = remoteNode;
            _subNodes = subNodes;
            Object o = remoteNode.getDataObject();
            _dataObject = (ISPDataObject)o;
            _nodeKey = remoteNode.getNodeKey();
        }

        /** Return the science program node */
        public ISPNode getRemoteNode() {
            return _remoteNode;
        }

        /** Return the data object for the remote node */
        public ISPDataObject getDataObject() {
            return _dataObject;
        }

        /** A list of Node objects for the sub nodes, or null if there are no sub nodes */
        public List<Node> getSubNodes() {
            return _subNodes;
        }

        public SPNodeKey getNodeKey() {
            return _nodeKey;
        }
    }

    // Returned for an empty sub node list
    private static final List<Node> _EMPTY_LIST = new ArrayList<Node>(0);

    // Compare nodes based on Class (ObsComps, Observations, Groups, in that order).
    private static class ClassComparator implements Comparator<Node>, Serializable {
        public int compare(Node o1, Node o2) {
            ISPNode n1 = o1.getRemoteNode();
            ISPNode n2 = o2.getRemoteNode();
            return _getVal(n1) - _getVal(n2);
        }
        private int _getVal(ISPNode node) {
            if (node instanceof ISPObsComponent)
                return 1;
            if (node instanceof ISPObservation)
                return 2;
            return 3;
        }
    }
    private static final Comparator<Node> _classComparator = new ClassComparator();


    // Compare nodes based on title
    private static class TitleComparator implements Comparator<Node>, Serializable {
        public int compare(Node n1, Node n2) {
            return n1.getDataObject().getTitle().compareTo(n2.getDataObject().getTitle());
        }
    };
    private static final Comparator<Node> _titleComparator = new TitleComparator();

    // Compare nodes based on obs id
    private static class IdComparator implements Comparator<Node>, Serializable {
        public int compare(Node o1, Node o2) {
            ISPNode n1 = o1.getRemoteNode();
            ISPNode n2 = o2.getRemoteNode();
             if (n1 instanceof ISPObservation && n2 instanceof ISPObservation) {
                SPObservationID id1 = ((ISPObservation)n1).getObservationID();
                SPObservationID id2 = ((ISPObservation)n2).getObservationID();
                return id1.compareTo(id2);
            }
            return _classComparator.compare(o1, o2);
        }
    };
    private static final Comparator<Node> _idComparator = new IdComparator();

    // Compare nodes based on observation status
    private static class StatusComparator implements Comparator<Node>, Serializable {
        public int compare(Node n1, Node n2) {
            if ((n1.getRemoteNode() instanceof ISPObservation) &&
                (n2.getRemoteNode() instanceof ISPObservation)) {

                final ObservationStatus s1 = ObservationStatus.computeFor((ISPObservation) n1.getRemoteNode());
                final ObservationStatus s2 = ObservationStatus.computeFor((ISPObservation) n2.getRemoteNode());
                int val = s1.compareTo(s2);
                if (val == 0) {
                    // Sort observations with equal status by title
                    val = _titleComparator.compare(n1, n2);
                }
                return val;
            }
            return _classComparator.compare(n1, n2);
        }
    };
    private static final Comparator<Node> _statusComparator = new StatusComparator();

    // The result of this functor: a tree starting at the given node
    private Node _result;

    // Set to true if group nodes should be included in the return list, false for a flat
    // list, where the observations all appear under the program node
    private boolean _includeGroupNodes;

    // Sort the results: The value should be one of the SORT_BY_* constants defined here
    private String _sortBy;

    // Corresponds to _sortBy
    private Comparator<Node> _comparator;


    /**
     * Initialize with the sort key.
     */
    private DBTreeListService(boolean includeGroupNodes, String sortBy) {
        _includeGroupNodes = includeGroupNodes;
        _sortBy = sortBy;
        _comparator = _getComparator();
    }

    /**
     * Returns a tree of Nodes that mirrors the science program tree
     */
    private Node getResult() {
        return _result;
    }

    /**
     * Recursively adds the nodes to the list.
     */
    private void execute(ISPNode node) {
        if (node instanceof ISPProgram) {
            // flatten (if requested) and sort nodes under program node
            List<Node> subNodes = _getSubNodes(node);
            if (!_includeGroupNodes) {
                subNodes = _flattenGroupNodes(subNodes);
            }
            _result = new Node(node, _sort(subNodes));
        } else if (node instanceof ISPGroup) {
            // sort group sub nodes
            _result = new Node(node, _sort(_getSubNodes(node)));
        } else {
            // no sorting below group level
            _result = new Node(node, _getSubNodes(node));
        }
    }

    // Return a list of sub nodes (Node objects) of the given node, or an empty list if
    // there are none
    private List<Node> _getSubNodes(ISPNode node)  {
        if (node instanceof ISPContainerNode) {
            List subNodes = ((ISPContainerNode) node).getChildren();
            int n = subNodes.size();
            if (n != 0) {
                List<Node> nodeList = new ArrayList<Node>(n);
                Iterator it = subNodes.iterator();
                while (it.hasNext()) {
                    ISPNode subNode = (ISPNode) it.next();
                    nodeList.add(new Node(subNode, _getSubNodes(subNode)));
                }
                return nodeList;
            }
        }
        return _EMPTY_LIST;
    }


    // Sort the given list of Node objects based on the current sortBy setting and
    // return the new list.
    private List<Node> _sort(List<Node> list) {
        if (SORT_BY_NONE.equals(_sortBy)) {
            return list;
        }

        Collections.sort(list, _comparator);
        if (_includeGroupNodes) {
            // If we are including group nodes, sort them too
            Iterator<Node> iter = list.iterator();
            while (iter.hasNext()) {
                Node node = iter.next();
                if (node.getRemoteNode() instanceof ISPGroup) {
                    Collections.sort(node.getSubNodes(), _comparator);
                }
            }
        }
        return list;
    }

    private Comparator<Node> _getComparator() {
        if (SORT_BY_TITLE.equals(_sortBy)) {
            return _titleComparator;
        }
        if (SORT_BY_ID.equals(_sortBy)) {
            return _idComparator;
        }
        if (SORT_BY_STATUS.equals(_sortBy)) {
            return _statusComparator;
        }
        return _classComparator;
    }

    // Flatten the given list by removing any groups and replacing them with the contained
    // nodes.
    private List<Node> _flattenGroupNodes(List<Node> list) {
        List<Node> result = new ArrayList<Node>();
        Iterator<Node> iter = list.iterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            if (node.getRemoteNode() instanceof ISPGroup) {
                result.addAll(node.getSubNodes());
            } else {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Returns a tree of Node objects representing the science program tree,
     * with the root at the given node.
     *
     * @param node the root science program node (may be a subtree root, such as a sequence node)
     * @param sortBy one of the SORT_BY_* constants defined in this class: Only applies when node
     *               is the root program node. The returned hierarchy will be sorted according to this.
     *
     * @return a hierarchy of Node objects, starting at the given node and sorted in the given way.
     */
    public static Node getNodeTree(ISPNode node, boolean includeGroupNodes, String sortBy) {
        DBTreeListService f = new DBTreeListService(includeGroupNodes, sortBy);
        f.execute(node);
        return f.getResult();
    }
}

