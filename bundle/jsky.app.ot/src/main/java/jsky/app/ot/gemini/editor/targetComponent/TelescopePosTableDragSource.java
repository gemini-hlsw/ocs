package jsky.app.ot.gemini.editor.targetComponent;

import jsky.app.ot.OTOptions;
import jsky.app.ot.util.DnDUtils;

import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * Drag&Drop source for the position table widget.
 *
 * @author Allan Brighton
 */
public class TelescopePosTableDragSource implements DragGestureListener, DragSourceListener {

    // Use the default DragSource
    protected DragSource _dragSource = DragSource.getDefaultDragSource();

    /** The pos table widget */
    protected TelescopePosTableWidget _tree;

    private boolean editable = false;

    /** Saved reference to drag object, for use in TelescopePosTableDropTarget */
    static TelescopePosTableDragDropObject _dragObject;

    /**
     * Constructor
     */
    public TelescopePosTableDragSource(TelescopePosTableWidget tree) {
        _tree = tree;

        // Create a DragGestureRecognizer and register as the listener
        _dragSource.createDefaultDragGestureRecognizer(_tree, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    void setEditable(boolean editable) { this.editable = editable; }

    /** Implementation of DragGestureListener interface. */
    public void dragGestureRecognized(DragGestureEvent dge) {
        if (!editable) return;

        // don't conflict with popup menus
        InputEvent e = dge.getTriggerEvent();
        if (e instanceof MouseEvent && ((MouseEvent) e).isPopupTrigger())
            return;


        // Get the mouse location and convert it to
        // a location within the tree.
        Point location = dge.getDragOrigin();
        TreePath dragPath = _tree.getPathForLocation(location.x, location.y);
        if (dragPath != null && _tree.isPathSelected(dragPath)) {
            // Get the list of selected nodes and create a Transferable
            TelescopePosTableWidget.TableData.Row[] nodes = _tree.getSelectedNodes();
            if (nodes != null && nodes.length > 0) {
                _dragObject = new TelescopePosTableDragDropObject(nodes, _tree);
                try {
                    dge.startDrag(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), _dragObject, this);
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
