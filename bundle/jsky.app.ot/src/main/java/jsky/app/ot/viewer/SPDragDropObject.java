// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPDragDropObject.java 44414 2012-04-11 17:55:07Z rnorris $
//
package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPNode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * This class ties an SpItem to the tree widget in which its associated
 * tree node lives.  This is used by drop targets when an item is being
 * moved or deleted in order to remove dragged objects from their tree.
 */
public final class SPDragDropObject implements Transferable {

    /** Identifies the object being dragged and dropped */
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(SPDragDropObject.class, "SPDragDropObject");

    /** Identifies the object being dragged and dropped */
    private static final DataFlavor[] _transferDataFlavors = new DataFlavor[]{
        // XXX hack: DataFlavor.stringFlavor should not be needed, but otherwise it doesn't work under Windows!
        DATA_FLAVOR, DataFlavor.stringFlavor
    };

    // The tree node widget that owns the ISPNode being dragged.  Will be
    // null if this item is new and is not in any tree.
    private SPTree _currentOwner;

    // The item(s) being dragged.
    private ISPNode[] _nodes;

    /**
     * This constructor should be used when dragging a newly created object
     * that hasn't been inserted in any tree.
     */
    public SPDragDropObject(ISPNode node) {
        _nodes = new ISPNode[1];
        _nodes[0] = node;
    }

    /**
     * This constructor should be used when dragging an object that currently
     * exists in a tree.
     */
    public SPDragDropObject(ISPNode node, SPTree _spTree) {
        this(node);
        _currentOwner = _spTree;
    }

    /**
     * This constructor should be used when dragging a newly created set
     * of items that haven't been inserted in any tree.
     */
    public SPDragDropObject(ISPNode[] nodes) {
        _nodes = nodes;
    }

    /**
     * This constructor should be used when dragging a set of objects
     * that currently exists in a tree.
     */
    public SPDragDropObject(ISPNode[] nodes, SPTree _spTree) {
        this(nodes);
        _currentOwner = _spTree;
    }

    /** Is more than one item being dragged? */
    boolean isMultiDrag() {
        return (_nodes.length > 1);
    }

    /** Get the first ISPNode. */
    ISPNode getNode() {
        return getNode(0);
    }

    /** Get the nth ISPNode. */
    ISPNode getNode(int i) {
        return _nodes[i];
    }

    /** Get the set of ISPNodes. */
    ISPNode[] getNodes() {
        return _nodes;
    }

    /** Get the owner, the SPTree that contains the items being dragged. */
    SPTree getOwner() {
        return _currentOwner;
    }

    // Implementation of the Transferable interface

    public DataFlavor[] getTransferDataFlavors() {
        return _transferDataFlavors;
    }

    public boolean isDataFlavorSupported(DataFlavor fl) {
        return fl.equals(DATA_FLAVOR);
    }

    public Object getTransferData(DataFlavor fl) {
        if (!isDataFlavorSupported(fl)) {
            return null;
        }

        return this;
    }
}

