package jsky.app.ot.gemini.editor.targetComponent;

import jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel.Row;
import jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel.TelescopePosTableModel;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Objects;

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

    // The treetable that owns the node being dragged. Will be null if this item is new and is not in any tree.
    private TelescopePosTableWidget _currentOwner;

    // The item(s) being dragged.
    private final Row _node;

    /**
     * This constructor should be used when dragging an object that currently
     * exists in a tree.
     */
    TelescopePosTableDragDropObject(final Row node, final TelescopePosTableWidget tree) {
        _node = Objects.requireNonNull(node);
        _currentOwner = tree;
    }

    /** Get the TelescopePosTableWidget.TableData.Row. */
    Row getNode() {
        return _node;
    }

    /** Get the owner, the SPTree that contains the items being dragged. */
    TelescopePosTableWidget getOwner() {
        return _currentOwner;
    }


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
