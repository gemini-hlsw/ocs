/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SPTreeDropTarget.java 46733 2012-07-12 20:43:36Z rnorris $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPGroup;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.IDBDatabaseService;
import jsky.app.ot.OTOptions;
import jsky.app.ot.nsp.SPTreeEditUtil;
import jsky.app.ot.util.DnDUtils;
import jsky.util.gui.DialogUtil;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

/**
 * Drag&Drop target for the OT tree widget.
 * Based on an example in the book: "Core Swing, Advanced Programming".
 *
 * @author Allan Brighton
 */
public class SPTreeDropTarget implements DropTargetListener, PropertyChangeListener {

    /** Target SPTree widget */
    private SPTree _spTree;

    /** The internal JTree widget */
    private JTree _tree;

    /** The drop target */
    private DropTarget _dropTarget;

    /** Indicates whether data is acceptable */
    private boolean _acceptableType;

    /** Initially selected rows */
    private TreePath[] _selections;

    /** Initial lead selection */
    private TreePath _leadSelection;


    /**
     * Constructor
     */
    public SPTreeDropTarget(SPTree _spTree) {
        this._spTree = _spTree;
        _tree = _spTree.getTree();

        // Listen for changes in the enabled property
        _tree.addPropertyChangeListener(this);

        // Create the DropTarget and register
        // it with the SPTree.
        _dropTarget = new DropTarget(_tree,
                                     DnDConstants.ACTION_COPY_OR_MOVE,
                                     this,
                                     _tree.isEnabled(), null);
    }

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

        DnDUtils.debugPrintln("Program is " + _spTree.getProgram());
        DnDUtils.debugPrintln("Obs is " + _spTree.getContextObservation());

        if (!OTOptions.areRootAndCurrentObsIfAnyEditable(_spTree.getProgram(), _spTree.getContextObservation())) {
            DnDUtils.debugPrintln("Edit not allowed.");
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
            DnDUtils.debugPrintln("Drop target rejecting drag: no acceptable drop lLocation");
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
                _spTree.setIgnoreSelection(true);
                _spTree.setSelectedNode(location);
                _spTree.setIgnoreSelection(false);
                _tree.setCursor(DragSource.DefaultCopyDrop);

            } else {
                _spTree.setIgnoreSelection(true);
                _tree.clearSelection();
                _spTree.setIgnoreSelection(false);
                // SW: removing this for now because on Linux, the no drop
                // sign is always displayed regardless of whether the drop is
                // accepted or not.
//                _tree.setCursor(DragSource.DefaultMoveNoDrop);
                _tree.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            _tree.setCursor(Cursor.getDefaultCursor());
            _spTree.setIgnoreSelection(true);
            _tree.clearSelection();
            _spTree.setIgnoreSelection(false);
        }
    }

    private void checkTransferType(DropTargetDragEvent dtde) {
        // Accept a list of files
        _acceptableType = dtde.isDataFlavorSupported(SPDragDropObject.DATA_FLAVOR);
        DnDUtils.debugPrintln("Data type acceptable - " + _acceptableType);
    }

    // This method handles a drop for a list of files
    private boolean dropNodes(int action, Transferable transferable, Point location)
            throws IOException, UnsupportedFlavorException {

        SPDragDropObject ddo = (SPDragDropObject) transferable.getTransferData(SPDragDropObject.DATA_FLAVOR);
        SPTree ownerTW = ddo.getOwner();
        ISPNode[] items = ddo.getNodes();
        ISPProgram prog = _spTree.getProgram();

        ISPNode parent = _spTree.getNode(location);
        if (parent == null)
            parent = prog;

        DnDUtils.debugPrintln((action == DnDConstants.ACTION_COPY ? "Copy" : "Move") +
                              " item " + ddo.getNode() +
                              " to targetNode " + parent);

        if (items != null) {
            for (ISPNode item : items) {
                if (!SPTreeEditUtil.isOkayToAdd(prog, item, parent, parent))
                    return false;
            }
        }

        SPViewer viewer = _spTree.getViewer();
        boolean topLevelFlag = false;
        try {
            if (ownerTW == _spTree) {
                // The dragged item was owned by this tree, so just move it, if allowed
                for (ISPNode item : items) {
                    IDBDatabaseService db = SPDB.get();
                    if (!SPTreeEditUtil.isOkayToMove(db, item, parent)) {
                        return false;
                    }
                }
                SPTreeEditUtil.moveOrReplaceTo(_spTree, items, parent);
                topLevelFlag = true;
            } else {
                if (ownerTW != null) {
                    // Make a copy, since these items are owned by another tree.
                    for (int i = 0; i < items.length; ++i) {
                        items[i] = SPTreeEditUtil.copyNode(prog, items[i]);
                    }
                }
                topLevelFlag = parent instanceof ISPProgram
                        || parent instanceof ISPGroup
                        || viewer.containsObservation(items);
                if (topLevelFlag) {
                    // for performance, delay updates
                    viewer.setProgramListenersEnabled(false);
                }
                SPTreeEditUtil.addACopy(_spTree, prog, items);
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        } finally {
            if (topLevelFlag) {
                _spTree.redraw();
                try {
                    viewer.setProgramListenersEnabled(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        _spTree.setSelectedNode(location);
        return true;
    }

    /** Return true if its okay to drop the item(s) here */
    private boolean isAcceptableDropLocation(DropTargetDragEvent dtde) {
        if (!OTOptions.areRootAndCurrentObsIfAnyEditable(_spTree.getProgram(), _spTree.getContextObservation()))
            return false;

        // Get the dragged thing, if it's a SPDragDropObject
        final Transferable transferable = dtde.getTransferable();
        final SPDragDropObject ddo;
        try {
            ddo = (SPDragDropObject) transferable.getTransferData(SPDragDropObject.DATA_FLAVOR);
            DnDUtils.debugPrintln("Dragged object is " + ddo);
            if (ddo == null) return false;
        } catch (UnsupportedFlavorException ignore) {
            DnDUtils.debugPrintln("Dragged object isn't an SPDragDropObject.");
            return false;
        } catch (IOException ioe) {
            // Shouldn't ever happen
            ioe.printStackTrace();
            return false;
        }

        ISPNode[] newItems = ddo.getNodes();
        ISPProgram prog = _spTree.getProgram();

        // get the node under the mouse
        ISPNode parent = _spTree.getNode(dtde.getLocation());
        if (parent == null)
            parent = prog;

        // get the items to be dropped
        if (newItems != null) {
            // Reject if it would move a node that should not be deleted
            SPTree ownerTW = ddo.getOwner();
            if (ownerTW == _spTree) {
                for (ISPNode newItem : newItems) {
                    IDBDatabaseService db = SPDB.get();
                    if (!SPTreeEditUtil.isOkayToMove(db, newItem, parent)) {
                        return false;
                    }
                }
            }
            if (!SPTreeEditUtil.isOkayToAdd(prog, newItems, parent, parent)) return false;
        }
        return true;
    }

    /** Save the current tree selection */
    private void saveTreeSelection() {
        _selections = _tree.getSelectionPaths();
        _leadSelection = _tree.getLeadSelectionPath();
        _spTree.setIgnoreSelection(true);
        _tree.clearSelection();
        _spTree.setIgnoreSelection(false);
    }

    /** Restore the current tree selection */
    private void restoreTreeSelection() {
        _spTree.setIgnoreSelection(true);
        _tree.setSelectionPaths(_selections);

        // Restore the lead selection
        if (_leadSelection != null) {
            _tree.removeSelectionPath(_leadSelection);
            _tree.addSelectionPath(_leadSelection);
        }
        _spTree.setIgnoreSelection(false);
    }
}

