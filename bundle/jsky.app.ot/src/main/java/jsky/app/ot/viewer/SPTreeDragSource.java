/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SPTreeDragSource.java 44443 2012-04-12 00:02:12Z rnorris $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObservation;
import jsky.app.ot.OTOptions;
import jsky.app.ot.util.DnDUtils;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * Drag&Drop source for tree widgets.
 * Based on an example in the book: "Core Swing, Advanced Programming".
 *
 * @author Allan Brighton
 */
public class SPTreeDragSource implements DragGestureListener, DragSourceListener {

    // Use the default DragSource
    protected DragSource _dragSource = DragSource.getDefaultDragSource();

    /** Target tree widget */
    protected SPTree _spTree;

    /** The internal JTree widget */
    protected JTree _tree;

    /**
     * Constructor
     */
    public SPTreeDragSource(SPTree spTree) {
        _spTree = spTree;
        _tree = _spTree.getTree();

        // Create a DragGestureRecognizer and register as the listener
        _dragSource.createDefaultDragGestureRecognizer(_tree, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    /** Implementation of DragGestureListener interface. */
    public void dragGestureRecognized(DragGestureEvent dge) {
        // don't conflict with the popup menus
        InputEvent e = dge.getTriggerEvent();
        if (e instanceof MouseEvent && ((MouseEvent) e).isPopupTrigger())
            return;

        // Get the mouse location and convert it to
        // a location within the tree.
        Point location = dge.getDragOrigin();
        TreePath dragPath = _tree.getPathForLocation(location.x, location.y);
        if (dragPath != null && _tree.isPathSelected(dragPath)) {
            // Get the list of selected nodes and create a Transferable
            ISPNode[] nodes = _spTree.getSelectedNodes();
            if (nodes != null && nodes.length > 0) {

                // Must be able to edit the obs, if any
                for (ISPNode n: nodes) {
                    // Is the target obs editable?
                    final ISPObservation o = n.getContextObservation();
                    if (o != null && !OTOptions.isObservationEditable(o)) {
                        DnDUtils.debugPrintln("Obs is not editable");
                        return;
                    }

                }


                SPDragDropObject dragObject = new SPDragDropObject(nodes, _spTree);
                try {
                    // SW: this is causing a problem with Linux or X.
                    // The cursor never changes from NO DROP and the install
                    // is today so I'm switching to something less intimidating
//                    dge.startDrag(DragSource.DefaultCopyNoDrop, dragObject, this);
                    dge.startDrag(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), dragObject, this);
                } catch (Exception ex) {
                    DnDUtils.debugPrintln("SPTreeDragSource.dragGestureRecognized: " + ex);
                }
            }
        }
    }

    // Implementation of DragSourceListener interface
    public void dragEnter(DragSourceDragEvent dsde) {
        DnDUtils.debugPrintln("Drag Source: dragEnter, drop action = "
                              + DnDUtils.showActions(dsde.getDropAction()));
    }

    public void dragOver(DragSourceDragEvent dsde) {
        DnDUtils.debugPrintln("Drag Source: dragOver, drop action = "
                              + DnDUtils.showActions(dsde.getDropAction()));
    }

    public void dragExit(DragSourceEvent dse) {
        DnDUtils.debugPrintln("Drag Source: dragExit");
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
        DnDUtils.debugPrintln("Drag Source: dropActionChanged, drop action = "
                              + DnDUtils.showActions(dsde.getDropAction()));
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
        DnDUtils.debugPrintln("Drag Source: drop completed, drop action = "
                              + DnDUtils.showActions(dsde.getDropAction())
                              + ", success: " + dsde.getDropSuccess());
    }
}
