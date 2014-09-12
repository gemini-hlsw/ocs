package jsky.app.ot.gemini.editor.targetComponent;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * This class ties an item to the position table widget in which its associated
 * tree node lives.  This is used by drop targets when an item is being
 * moved or deleted in order to remove dragged objects from their tree.
 */
final class TelescopePosTableDragDropObject implements Transferable {

    /** Identifies the object being dragged and dropped */
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(TelescopePosTableDragDropObject.class, "TelescopePosTableDragDropObject");

    /** Identifies the object being dragged and dropped */
    private static final DataFlavor[] _transferDataFlavors = new DataFlavor[]{
        // XXX hack: DataFlavor.stringFlavor should not be needed, but otherwise it doesn't work under Windows!
        DATA_FLAVOR, DataFlavor.stringFlavor
    };

    // The treetable that owns the node being dragged.  Will be
    // null if this item is new and is not in any tree.
    private TelescopePosTableWidget _currentOwner;

    // The item(s) being dragged.
    private TelescopePosTableWidget.TableData.Row[] _nodes;

    /**
     * This constructor should be used when dragging a newly created object
     * that hasn't been inserted in any tree.
     */
    TelescopePosTableDragDropObject(TelescopePosTableWidget.TableData.Row node) {
        _nodes = new TelescopePosTableWidget.TableData.Row[1];
        _nodes[0] = node;
    }

    /**
     * This constructor should be used when dragging an object that currently
     * exists in a tree.
     */
    TelescopePosTableDragDropObject(TelescopePosTableWidget.TableData.Row node, TelescopePosTableWidget tree) {
        this(node);
        _currentOwner = tree;
    }

    /**
     * This constructor should be used when dragging a newly created set
     * of items that haven't been inserted in any tree.
     */
    TelescopePosTableDragDropObject(TelescopePosTableWidget.TableData.Row[] nodes) {
        _nodes = nodes;
    }

    /**
     * This constructor should be used when dragging a set of objects
     * that currently exists in a tree.
     */
    TelescopePosTableDragDropObject(TelescopePosTableWidget.TableData.Row[] nodes, TelescopePosTableWidget tree) {
        this(nodes);
        _currentOwner = tree;
    }

    /** Is more than one item being dragged? */
    boolean isMultiDrag() {
        return (_nodes.length > 1);
    }

    /** Get the first TelescopePosTableWidget.TableData.Row. */
    TelescopePosTableWidget.TableData.Row getNode() {
        return getNode(0);
    }

    /** Get the nth TelescopePosTableWidget.TableData.Row. */
    TelescopePosTableWidget.TableData.Row getNode(int i) {
        return _nodes[i];
    }

    /** Get the set of TelescopePosTableWidget.TableData.Rows. */
    TelescopePosTableWidget.TableData.Row[] getNodes() {
        return _nodes;
    }

    /** Get the owner, the SPTree that contains the items being dragged. */
    TelescopePosTableWidget getOwner() {
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

