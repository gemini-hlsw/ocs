package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel.Row;
import jsky.app.ot.util.DnDUtils;
import jsky.util.gui.DialogUtil;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.IOException;
import java.util.Objects;


// Drag & Drop target for the position table.
class TelescopePosTableDropTarget implements DropTargetListener {

    /** The position table widget */
    private final TelescopePosTableWidget table;

    /** The drop target */
    private final DropTarget _dropTarget;

    /** Indicates whether data is acceptable */
    private boolean _acceptableType;

    /** Initially selected row. */
    private Option<Integer> _selection;

    private boolean editable = false;

    /**
     * Constructor
     */
    public TelescopePosTableDropTarget(final TelescopePosTableWidget tree) {
        table = Objects.requireNonNull(tree);

        // Create the DropTarget and register
        // it with the SPTree.
        _dropTarget = new DropTarget(table,
                DnDConstants.ACTION_COPY_OR_MOVE,
                this,
                table.isEnabled(), null);

        // Enable the drop target if the SPTree is enabled
        // and vice versa.
        table.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equals("enabled"))
                _dropTarget.setActive(table.isEnabled());
        });
    }

    void setEditable(final boolean editable) {
        this.editable = editable;
    }

    public void dragEnter(final DropTargetDragEvent dtde) {
        DnDUtils.debugPrintln("dragEnter, drop action = " + DnDUtils.showActions(dtde.getDropAction()));
        saveTableSelection();
        checkTransferType(dtde);
        final boolean acceptedDrag = acceptOrRejectDrag(dtde);
        dragUnderFeedback(dtde, acceptedDrag);
    }

    public void dragExit(final DropTargetEvent dte) {
        DnDUtils.debugPrintln("DropTarget dragExit");
        dragUnderFeedback(null, false);
        restoreTableSelection();
    }

    public void dragOver(final DropTargetDragEvent dtde) {
        DnDUtils.debugPrintln("DropTarget dragOver, drop action = " + DnDUtils.showActions(dtde.getDropAction()));
        final boolean acceptedDrag = acceptOrRejectDrag(dtde);
        dragUnderFeedback(dtde, acceptedDrag);
    }

    public void dropActionChanged(final DropTargetDragEvent dtde) {
        DnDUtils.debugPrintln("DropTarget dropActionChanged, drop action = " + DnDUtils.showActions(dtde.getDropAction()));
        final boolean acceptedDrag = acceptOrRejectDrag(dtde);
        dragUnderFeedback(dtde, acceptedDrag);
    }

    public void drop(final DropTargetDropEvent dtde) {
        DnDUtils.debugPrintln("DropTarget drop, drop action = " + DnDUtils.showActions(dtde.getDropAction()));

        // Check the drop action
        if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
            // Accept the drop and get the transfer data
            dtde.acceptDrop(dtde.getDropAction());
            final Transferable transferable = dtde.getTransferable();
            boolean dropSucceeded = false;

            try {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                dropSucceeded = dropNodes(dtde.getDropAction(), transferable, dtde.getLocation());
                DnDUtils.debugPrintln("Drop completed, success: " + dropSucceeded);
            } catch (Exception e) {
                DnDUtils.debugPrintln("Exception while handling drop " + e);
            } finally {
                table.setCursor(Cursor.getDefaultCursor());
                dtde.dropComplete(dropSucceeded);
            }
        } else {
            DnDUtils.debugPrintln("Drop target rejected drop");
            dtde.dropComplete(false);
        }
    }

    // Internal methods start here
    private boolean acceptOrRejectDrag(final DropTargetDragEvent dtde) {
        if (!editable) return false;
        if (!table.isEnabled()) {
            dtde.rejectDrag();
            return false;
        }

        final int sourceActions = dtde.getSourceActions();
        final int dropAction = dtde.getDropAction();
        DnDUtils.debugPrintln("\tSource actions are " + DnDUtils.showActions(sourceActions) +
                ", drop action is " + DnDUtils.showActions(dropAction));

        // Reject if the object being transferred
        // or the operations available are not acceptable.
        if (!_acceptableType || (sourceActions & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
            DnDUtils.debugPrintln("Drop target rejecting drag: acceptableType = " + _acceptableType);
            dtde.rejectDrag();
            return false;
        } else if (!isAcceptableDropLocation(dtde)) {
            // Can only drag to writable directory
            DnDUtils.debugPrintln("Drop target rejecting drag: no acceptable drop location");
            dtde.rejectDrag();
            return false;
        } else {
            // Offering an acceptable operation: accept
            DnDUtils.debugPrintln("Drop target accepting drag");
            dtde.acceptDrag(dropAction);
            return true;
        }
    }

    // Perform an action while turning off the tree selection listening.
    private void treeIgnoreSelectionAction(final Runnable action) {
        table.setIgnoreSelection(true);
        action.run();
        table.setIgnoreSelection(false);
    }

    private void dragUnderFeedback(final DropTargetDragEvent dtde, final boolean acceptedDrag) {
        if (dtde != null) {
            if (acceptedDrag && isAcceptableDropLocation(dtde)) {
                final Point location = dtde.getLocation();
                treeIgnoreSelectionAction(() -> table.setSelectedRow(location));
                table.setCursor(DragSource.DefaultCopyDrop);

            } else {
                treeIgnoreSelectionAction(table::clearSelection);
                table.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            table.setCursor(Cursor.getDefaultCursor());
            treeIgnoreSelectionAction(table::clearSelection);
        }
    }

    // NOTE: This method sets the object variable _acceptableType if the transfer type is permissible.
    private void checkTransferType(final DropTargetDragEvent dtde) {
        // Accept a list of files
        _acceptableType = dtde.isDataFlavorSupported(TelescopePosTableDragDropObject.DATA_FLAVOR);
        DnDUtils.debugPrintln("Data type acceptable - " + _acceptableType);
    }

    // This method handles a drop for a list of files
    private boolean dropNodes(final int action, final Transferable transferable, final Point location)
            throws IOException, UnsupportedFlavorException {
        final Row parent = table.getNode(location);
        if (parent == null)
            return false;

        final TelescopePosTableDragDropObject ddo = (TelescopePosTableDragDropObject) transferable.getTransferData(TelescopePosTableDragDropObject.DATA_FLAVOR);
        final TelescopePosTableWidget ownerTW = ddo.getOwner();
        final Row item = ddo.getNode();

        DnDUtils.debugPrintln((action == DnDConstants.ACTION_COPY ? "Copy" : "Move") +
                " item " + ddo.getNode() + " to targetNode " + parent);

        // Highlight the drop location while we perform the drop
        treeIgnoreSelectionAction(() -> table.setSelectedRow(location));

        try {
            if (ownerTW == table && table.isOkayToMove(item, parent)) {
                table.moveTo(item, parent);
                return true;
            }
        } catch (final Exception e) {
            DialogUtil.error(e);
        }
        return false;
    }

    /** Return true if its okay to drop the item(s) here */
    private boolean isAcceptableDropLocation(final DropTargetDragEvent dtde) {
        if (!table.isEnabled()) {
            return false;
        }

        // Get the node under the mouse.
        final Row parent = table.getNode(dtde.getLocation());
        if (parent == null) {
            return false;
        }

        // Reject if it would move a node that should not be deleted.
        final TelescopePosTableDragDropObject ddo = TelescopePosTableDragSource._dragObject;
        return (ddo != null) && (ddo.getOwner() == table) && table.isOkayToMove(ddo.getNode(), parent);
    }

    /** Save the current table selection: we only allow one row to be selected. */
    private void saveTableSelection() {
        final int idx = table.getSelectedRow();
        _selection = idx == -1 ? None.instance() : new Some<>(idx);
        treeIgnoreSelectionAction(table::clearSelection);
    }

    /** Restore the table selection: we only allow one row to be selected. */
    private void restoreTableSelection() {
        treeIgnoreSelectionAction(() -> _selection.foreach(table::selectRowAt));
    }
}
