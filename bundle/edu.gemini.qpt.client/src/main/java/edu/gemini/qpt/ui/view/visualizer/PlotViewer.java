package edu.gemini.qpt.ui.view.visualizer;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.util.SortedSet;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.util.Variants.EditException;
import edu.gemini.qpt.ui.util.BooleanToolPreference;
import edu.gemini.qpt.ui.util.DragImage;
import edu.gemini.qpt.ui.util.Platform;
import edu.gemini.qpt.ui.util.TimePreference;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.gface.GViewerPlugin;
import scala.Option;
import scala.Some;

public class PlotViewer extends GViewer<Variant, Alloc> {

    private static final Logger LOGGER = Logger.getLogger(PlotViewer.class.getName());

    private static final DataFlavor GSELECTION_OF_ALLOCS = GSelection.flavorForSelectionOf(Alloc.class);

    public PlotViewer(GViewerPlugin<Variant, Alloc> controller) {
        this(controller, true, null);
    }

    public PlotViewer(GViewerPlugin<Variant, Alloc> controller, boolean qcOnly, TimePreference timePreference) {
        super(controller, new Visualizer(qcOnly, timePreference));

        // Configure the control
        getControl().setMinimumSize(new Dimension(400, 242));
        getControl().setPreferredSize(getControl().getMinimumSize());
        getControl().addMouseMotionListener(mouseMoveListener);
        getControl().addMouseListener(mousePressedListener);

        // Set up incoming and outgoing drag. Neither will get GC'd so we don't need
        // to save references.
        new DropTarget(getControl(), dropTargetListener);
        new DragSource().createDefaultDragGestureRecognizer(
                getControl(), DnDConstants.ACTION_MOVE, dragGestureListener);

    }

    @Override
    public Alloc getElementAt(Point p) {
        return getControl().getAllocAtPoint(p.x, p.y);
    }

    @Override
    protected Runnable getSelectionTask(final GSelection<?> newSelection) {
        return () -> {
            // Translator will guarantee that the cast works.
            getControl().setSelection((GSelection<Alloc>) newSelection);
            setPulledSelection((GSelection<Alloc>) newSelection);
        };
    }

    @Override
    public void refresh() {
        getControl().setModel(getModel());
        if (!getControl().getIgnoreRepaint())
            getControl().repaint();
    }

    @Override
    public Visualizer getControl() {
        return (Visualizer) super.getControl();
    }

    ///
    /// MOUSE LISTENERS
    ///

    // Set the mouse pointer to a hand cursor if the mouse is over an Alloc.
    private final MouseMotionAdapter mouseMoveListener = new MouseMotionAdapter() {
        public void mouseMoved(MouseEvent e) {
            Alloc a = getElementAt(e.getPoint());
            getControl().setCursor(a != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        }
    };

    // If the user clicks on an Alloc, select it. Otherwise, clear the selection.
    private final MouseAdapter mousePressedListener = new MouseAdapter() {

        private Alloc clickedOn;

        @SuppressWarnings("unchecked")
        public void mousePressed(MouseEvent e) {
            getControl().requestFocusInWindow();
            Alloc a = getElementAt(e.getPoint());
            boolean extend = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK;
            boolean toggle = (e.getModifiersEx() & Platform.TOGGLE_ACTION_MASK) == Platform.TOGGLE_ACTION_MASK;
            GSelection<Alloc> current = getControl().getSelection();
            GSelection<Alloc> sel;

            if (extend) {

                // Shift-clicking on nothig is a meaningless operation. Ignore it.
                if (a == null)    return;

                // Ok, we will mimic Java-style selection extension. The anchor is
                // always the first element of the current selection, which is extended
                // to the shift+clicked element. It's fairly lame actually.
                if (current.isEmpty()) {
                    sel = new GSelection<Alloc>(a);
                } else {
                    // TODO: use interval math
                    Alloc b = current.first();
                    if (a.compareTo(b) > 0) {
                        Alloc temp = a;
                        a = b;
                        b = temp;
                    }
                    GSelection<Alloc> accum = GSelection.<Alloc>emptySelection();
                    final SortedSet<Alloc> allocs = ImOption.apply(getModel())
                                                            .map(v -> v.getAllocs())
                                                            .getOrElse(Collections.<Alloc>emptySortedSet());
                    for (Alloc x : allocs) {
                        if (x.compareTo(b) > 0) {
                            break;
                        } else if (x.compareTo(a) >= 0) {
                            accum = accum.plus(new GSelection<Alloc>(x));
                        }
                    }
                    sel = accum;
                }

            } else if (toggle) {

                if (a == null) return;
                sel = current.contains(a) ?
                    current.minus(new GSelection<Alloc>(a)) :
                    current.plus(new GSelection<Alloc>(a));

            } else {

                // The selection is simply what we clicked on.
                sel = a == null ? GSelection.<Alloc>emptySelection() :    new GSelection<Alloc>(a);

                // If it's already selected, we don't want to do anything yet because
                // the user might make a drag gesture. If the mouseup is on the same
                // alloc, it means we haven't dragged.
                if (current.contains(a)) {
                    clickedOn = a;
                    return;
                }

            }

            // Change the selection if needed.
            if (!getControl().getSelection().equals(sel)) {
                getControl().setSelection(sel);
                setPulledSelection(sel);
            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (clickedOn != null) {
                Alloc a = getElementAt(e.getPoint());
                if (clickedOn == a) {
                    GSelection<Alloc> sel = new GSelection<Alloc>(a);
                    getControl().setSelection(sel);
                    setPulledSelection(sel);
                }
                clickedOn = null;
            }
        }

    };

    DragGestureListener dragGestureListener = new DragGestureListener() {

        public void dragGestureRecognized(final DragGestureEvent dge) {

            // Don't allow shift-drag (yet).
            if ((dge.getTriggerEvent().getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK)
                return;

            final Variant variant = getModel();

            GSelection<Alloc> selection = getControl().getSelection();
            if (variant != null && !selection.isEmpty()) {

                // Calculate the offset from the start of the first alloc in the selection.
                selection.sort(); // be sure it's sorted

                double clickTime = getControl().clientToModel(dge.getDragOrigin()).getX();

                // Set the offset as a decoration on the selection
                selection.putProperty("offset", (long) (clickTime - selection.first().getStart()));

                Image dragImage = DragImage.forSelection(variant, selection);

                dge.startDrag(
                        DragSource.DefaultCopyNoDrop,
                        dragImage,
                        DragImage.getCenterOffset(dragImage),
                        selection,
                        null);

                // [QPT-185] Removing the alloc causes a repaint with no selection, but the
                // drag object has not been set yet because no dragOver event has arrived. To
                // fix this, we turn off repaints and turn them back on in dragEnter() below.
                // [QPT-224] This hack causes shearing on Linux and Win32, which sometimes
                // never recover until a subsequent drag.
                if (Platform.IS_MAC)
                    getControl().setIgnoreRepaint(true);

                // The problem here is that it's legal to move a non-final slice of a split
                // visit, but it's not legal to delete one. So we have to use a forced
                // deletion here ... we have to be very careful to repair this in the
                // dragOut event.
                for (Alloc a: selection)
                    a.forceRemove();

            }

        }

    };

    ///
    /// DROP TARGET LISTENER IMPLEMENTATION
    ///

    DropTargetListener dropTargetListener = new DropTargetListener() {

        public void drop(final DropTargetDropEvent dtde) {
            final Option<GSelection<Alloc>> obj = getDraggedObject(dtde.getTransferable());
            if (obj.isDefined()) {
                doDrop(obj.get());
                dtde.dropComplete(true);
            }
        }

        @SuppressWarnings("unchecked")
        private void doDrop(GSelection<Alloc> obj) {

            // Clear the drag
            getControl().clearDrag();

            // Get the offset
            long delta = getControl().getDragDelta();

            // We're creating a new list of allocs by offsetting the stuff in the dragObject

            // TODO: make this actually work
            ImOption.apply(getModel()).foreach(variant -> {
                Alloc[] allocs = obj.toArray(Alloc.class);
                for (int i = 0; i < allocs.length; i++) {
                    Alloc a = allocs[i];
                    try {
                        allocs[i] = variant.addAlloc(a.getObs(), a.getStart() + delta, a.getFirstStep(), a.getLastStep(), a.getSetupType(), a.getComment());
                    } catch (EditException e) {
                        LOGGER.log(Level.SEVERE, "Problem dropping " + a + " with delta " + delta, e);
                        JOptionPane.showMessageDialog(getControl(), "The drop action for " + a + " failed due to a " + e.getClass().getSimpleName() + ".\n" +
                                "This should never happen. Sorry.", "Drop Exception", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }
                }

                GSelection<Alloc> newSelection = new GSelection<Alloc>(allocs);
                getControl().setSelection(newSelection);
                setPulledSelection(newSelection);
                getControl().requestFocus();
            });

        }

        public void dragExit(DropTargetEvent dte) {

            // If the thing being dragged has a successor, put it back. We have to remember to take it
            // out again on dragEnter. How annoying.
            GSelection<Alloc> drag = getControl().getDragObject();
            drag.sort();

            ImOption.apply(getModel()).foreach(variant -> {
                for (Alloc a : drag) {
                    boolean dragContainsSuccessor = false;
                    for (Alloc s : drag) {
                        if (a.isSuccessor(s)) {
                            dragContainsSuccessor = true;
                            break;
                        }
                    }
                    if (dragContainsSuccessor || a.getSuccessor() != null) {
                        try {
                            variant.addAlloc(a);
                        } catch (EditException e) {
                            LOGGER.log(Level.SEVERE, "Problem putting back split slice " + a, e);
                            JOptionPane.showMessageDialog(getControl(), "The drop action for " + a + " failed due to a " + e.getClass().getSimpleName() + ".\n" +
                                    "This should never happen. Sorry.", "Drop Exception", JOptionPane.ERROR_MESSAGE, null);
                            return;
                        }
                    }
                }
            });


            getControl().clearDrag();
        }

        public void dropActionChanged(DropTargetDragEvent dtde) {
        }

        public void dragOver(final DropTargetDragEvent dtde) {
            final Option<GSelection<Alloc>> obj = getDraggedObject(dtde.getTransferable());
            if (obj.isDefined()) {
                final boolean snap = BooleanToolPreference.TOOL_SNAP.get();
                // Get the offset. This will be in ms between 0 and the obs/alloc length (or null).
                getControl().setDrag(dtde.getLocation(), obj.get(), snap, (Long) obj.get().getProperty("offset"));
            } else {
                dtde.rejectDrag();
            }
        }

        public void dragEnter(final DropTargetDragEvent dtde) {
            dtde.acceptDrag(DnDConstants.ACTION_MOVE);

            // If we're dragging a non-final slice back in, we have to force-remove it again
            // from the model. We can just go ahead and remove anything from the model that's
            // in the drag.
            final Option<GSelection<Alloc>> obj = getDraggedObject(dtde.getTransferable());
            if (obj.isDefined()) {
                final SortedSet<Alloc> allocs = ImOption.apply(getModel())
                                                        .map(v -> v.getAllocs())
                                                        .getOrElse(Collections.<Alloc>emptySortedSet());

                for (Alloc a: allocs) {
                    if (obj.get().contains(a)) {
                        a.forceRemove();
                    }
                }
                dragOver(dtde);
            } else {
                dtde.rejectDrag();
            }

            getControl().setIgnoreRepaint(false); // [QPT-185]; see dragGestureRecognized() above
        }

    };


    @SuppressWarnings("unchecked")
    private static Option<GSelection<Alloc>> getDraggedObject(final Transferable t) {
        try {
            // We will accept an alloc or an obs
            if (t.isDataFlavorSupported(GSELECTION_OF_ALLOCS)) {
                return new Some<>((GSelection<Alloc>) t.getTransferData(GSELECTION_OF_ALLOCS));
            }
        } catch (UnsupportedFlavorException | IOException e) {
            LOGGER.log(Level.WARNING, "Problem getting drag data. This should never happen.", e);
        }

        return Option.<GSelection<Alloc>>empty();
    }

}
