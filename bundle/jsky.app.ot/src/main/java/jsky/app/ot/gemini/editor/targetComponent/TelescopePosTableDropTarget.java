package jsky.app.ot.gemini.editor.targetComponent;

import jsky.app.ot.OTOptions;
import jsky.app.ot.util.DnDUtils;
import jsky.util.gui.DialogUtil;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;


/**
 * Drag&Drop target for the position table.
 *
 * @author Allan Brighton
 */
public class TelescopePosTableDropTarget implements DropTargetListener, PropertyChangeListener {

    /** The position table (treetable) widget */
    private TelescopePosTableWidget _tree;

    /** The drop target */
    private DropTarget _dropTarget;

    /** Indicates whether data is acceptable */
    private boolean _acceptableType;

    /** Initially selected rows */
    private TreePath[] _selections;

    /** Initial lead selection */
    private TreePath _leadSelection;

    private boolean editable = false;

    /**
     * Constructor
     */
    public TelescopePosTableDropTarget(TelescopePosTableWidget tree) {
        _tree = tree;

        // Listen for changes in the enabled property
        _tree.addPropertyChangeListener(this);

        // Create the DropTarget and register
        // it with the SPTree.
        _dropTarget = new DropTarget(_tree,
                                     DnDConstants.ACTION_COPY_OR_MOVE,
                                     this,
                                     _tree.isEnabled(), null);
    }

    void setEditable(boolean editable) { this.editable = editable; }


    /** Implementation of the DropTargetListener interface */
    public void dragEnter(DropTargetDragEvent dtde) {
        DnDUtils.debugPrintln("dragEnter, drop action = "
                + DnDUtils.showActions(dtde.getDropAction()));

        // Save the list of selected items
        saveTreeSelection();

        // Get the type of object being transferred and determine
        // whether it is appropriate.
        checkTransferType(dtde);

        // Accept or reject the drag.
        boolean acceptedDrag = acceptOrRejectDrag(dtde);

        // Do drag-under feedback
        dragUnderFeedback(dtde, acceptedDrag);
    }

    /** Implementation of the DropTargetListener interface */
    public void dragExit(DropTargetEvent dte) {
        DnDUtils.debugPrintln("DropTarget dragExit");

        // Do drag-under feedback
        dragUnderFeedback(null, false);

        // Restore the original selections
        restoreTreeSelection();
    }

    /** Implementation of the DropTargetListener interface */
    public void dragOver(DropTargetDragEvent dtde) {
        DnDUtils.debugPrintln("DropTarget dragOver, drop action = "
                              + DnDUtils.showActions(dtde.getDropAction()));

        // Accept or reject the drag
        boolean acceptedDrag = acceptOrRejectDrag(dtde);

        // Do drag-under feedback
        dragUnderFeedback(dtde, acceptedDrag);
    }

    /** Implementation of the DropTargetListener interface */
    public void dropActionChanged(DropTargetDragEvent dtde) {
        DnDUtils.debugPrintln("DropTarget dropActionChanged, drop action = "
                              + DnDUtils.showActions(dtde.getDropAction()));

        // Accept or reject the drag
        boolean acceptedDrag = acceptOrRejectDrag(dtde);

        // Do drag-under feedback
        dragUnderFeedback(dtde, acceptedDrag);
    }

    /** Implementation of the DropTargetListener interface */
    public void drop(DropTargetDropEvent dtde) {
        DnDUtils.debugPrintln("DropTarget drop, drop action = "
                              + DnDUtils.showActions(dtde.getDropAction()));

        // Check the drop action
        if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
            // Accept the drop and get the transfer data
            dtde.acceptDrop(dtde.getDropAction());
            Transferable transferable = dtde.getTransferable();
            boolean dropSucceeded = false;

            try {
                _tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Save the user's selections
                saveTreeSelection();

                dropSucceeded = dropNodes(dtde.getDropAction(), transferable, dtde.getLocation());

                DnDUtils.debugPrintln("Drop completed, success: "
                                      + dropSucceeded);
            } catch (Exception e) {
                DnDUtils.debugPrintln("Exception while handling drop " + e);
            } finally {
                _tree.setCursor(Cursor.getDefaultCursor());

                // Restore the user's selections
                restoreTreeSelection();
                dtde.dropComplete(dropSucceeded);
            }
        } else {
            DnDUtils.debugPrintln("Drop target rejected drop");
            dtde.dropComplete(false);
        }
    }

    /** PropertyChangeListener interface */
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (propertyName.equals("enabled")) {
            // Enable the drop target if the SPTree is enabled
            // and vice versa.
            _dropTarget.setActive(_tree.isEnabled());
        }
    }

    // Internal methods start here
    private boolean acceptOrRejectDrag(DropTargetDragEvent dtde) {
        if (!editable) return false;

        if (!_tree.isEnabled()) {
            dtde.rejectDrag();
            return false;
        }

        int dropAction = dtde.getDropAction();
        int sourceActions = dtde.getSourceActions();
        boolean acceptedDrag = false;

        DnDUtils.debugPrintln("\tSource actions are " +
                              DnDUtils.showActions(sourceActions) +
                              ", drop action is " +
                              DnDUtils.showActions(dropAction));

        boolean acceptableDropLocation = isAcceptableDropLocation(dtde);

        // Reject if the object being transferred
        // or the operations available are not acceptable.
        if (!_acceptableType || (sourceActions & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
            DnDUtils.debugPrintln("Drop target rejecting drag: acceptableType = " + _acceptableType);
            dtde.rejectDrag();
        } else if (!acceptableDropLocation) {
            // Can only drag to writable directory
            DnDUtils.debugPrintln("Drop target rejecting drag: no acceptable drop location");
            dtde.rejectDrag();
        } else {
            // Offering an acceptable operation: accept
            DnDUtils.debugPrintln("Drop target accepting drag");
            dtde.acceptDrag(dropAction);
            acceptedDrag = true;
        }

        return acceptedDrag;
    }

    private void dragUnderFeedback(DropTargetDragEvent dtde, boolean acceptedDrag) {
        if (dtde != null) {
            if (acceptedDrag && isAcceptableDropLocation(dtde)) {
                Point location = dtde.getLocation();
                _tree.setIgnoreSelection(true);
                _tree.setSelectedNode(location);
                _tree.setIgnoreSelection(false);
                _tree.setCursor(DragSource.DefaultCopyDrop);

            } else {
                _tree.setIgnoreSelection(true);
                _tree.clearSelection();
                _tree.setIgnoreSelection(false);
                // SW: removing this for now because on Linux, the no drop
                // sign is always displayed regardless of whether the drop is
                // accepted or not.
//                _tree.setCursor(DragSource.DefaultMoveNoDrop);
                _tree.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            _tree.setCursor(Cursor.getDefaultCursor());
            _tree.setIgnoreSelection(true);
            _tree.clearSelection();
            _tree.setIgnoreSelection(false);
        }
    }

    private void checkTransferType(DropTargetDragEvent dtde) {
        // Accept a list of files
        _acceptableType = dtde.isDataFlavorSupported(TelescopePosTableDragDropObject.DATA_FLAVOR);
        DnDUtils.debugPrintln("Data type acceptable - " + _acceptableType);
    }

    // This method handles a drop for a list of files
    private boolean dropNodes(int action, Transferable transferable, Point location)
            throws IOException, UnsupportedFlavorException {

        TelescopePosTableDragDropObject ddo = (TelescopePosTableDragDropObject) transferable.getTransferData(TelescopePosTableDragDropObject.DATA_FLAVOR);
        TelescopePosTableWidget ownerTW = ddo.getOwner();
        TelescopePosTableWidget.TableData.Row[] items = ddo.getNodes();

        TelescopePosTableWidget.TableData.Row parent = _tree.getNode(location);
        if (parent == null)
            return false;

        DnDUtils.debugPrintln((action == DnDConstants.ACTION_COPY ? "Copy" : "Move") +
                              " item " + ddo.getNode() +
                              " to targetNode " + parent);

        // Highlight the drop location while we perform the drop
        _tree.setIgnoreSelection(true);
        _tree.setSelectedNode(location);
        _tree.setIgnoreSelection(false);

        if (items != null) {
            for (TelescopePosTableWidget.TableData.Row item : items) {
                if (!_tree.isOkayToAdd(item, parent)) {
                    return false;
                }
            }
        }

        try {
            if (ownerTW == _tree) {
                // The dragged item was owned by this tree, so just move it, if allowed
                for (TelescopePosTableWidget.TableData.Row item : items) {
                    if (!_tree.isOkayToMove(item, parent)) {
                        return false;
                    }
                }
                _tree.moveTo(items, parent);
            } else {
                return false;
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        } finally {
//            _tree.reinit(SPProgData.);
        }
        return true;
    }

    /** Return true if its okay to drop the item(s) here */
    private boolean isAcceptableDropLocation(DropTargetDragEvent dtde) {
        if (!_tree.isEnabled()) {
            return false;
        }

        TelescopePosTableDragDropObject ddo = TelescopePosTableDragSource._dragObject;
        if (ddo == null) {
            return false;
        }

        TelescopePosTableWidget.TableData.Row[] newItems = ddo.getNodes();

        // get the node under the mouse
        TelescopePosTableWidget.TableData.Row parent = _tree.getNode(dtde.getLocation());
        if (parent == null) {
            return false;
        }

        // get the items to be dropped
        if (newItems != null) {
            // Reject if it would move a node that should not be deleted
            if (ddo.getOwner() == _tree) {
                for (TelescopePosTableWidget.TableData.Row newItem : newItems) {
                    if (!_tree.isOkayToMove(newItem, parent)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /** Save the current tree selection */
    private void saveTreeSelection() {
        int[] rows = _tree.getSelectedRows();
        _selections = new TreePath[rows.length];
        for(int i = 0; i < rows.length; i++) {
            _selections[i] = _tree.getPathForRow(rows[i]);
        }
//        _leadSelection = _tree.getLeadSelectionPath();
        _tree.setIgnoreSelection(true);
        _tree.clearSelection();
        _tree.setIgnoreSelection(false);
    }

    /** Restore the tree selection */
    private void restoreTreeSelection() {
        _tree.setIgnoreSelection(true);
        int[] rows = new int[_selections.length];
        for(int i = 0; i < rows.length; i++) {
            rows[i] = _tree.getRowForPath(_selections[i]);
        }
        if (rows.length > 0) {
            // only single select for now
            _tree.selectRowAt(rows[0]);
        }

//        // Restore the lead selection
//        if (_leadSelection != null) {
//            _tree.removeSelectionPath(_leadSelection);
//            _tree.addSelectionPath(_leadSelection);
//        }
        _tree.setIgnoreSelection(false);
    }
}

