package edu.gemini.qpt.ui.view.program;

import java.awt.Image;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.qpt.ui.ShellAdvisor;
import edu.gemini.qpt.ui.action.DragLimit;
import edu.gemini.qpt.ui.util.DragImage;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GTreeViewer;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

public class ScienceProgramViewAdvisor implements IViewAdvisor {

    // Tree viewer. This is the main doodad.
    private GTreeViewer<Prog, Object> viewer = 
        new GTreeViewer<Prog, Object>(new ScienceProgramController());
    
    // The context
    private IViewContext context;
    
    // More UI stuff
    ScienceProgramDecorator decorator;
    JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTree());
    
    public ScienceProgramViewAdvisor() {
                
        // Set up viewer        
        viewer.setTranslator(new ScienceProgramTranslator());
        viewer.setInterloper(new ScienceProgramSelectionInterloper());
        viewer.setFilter(new ScienceProgramFilter());
        viewer.getTree().addMouseListener(doubleClickListener);
        viewer.getTree().setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        
        // Outgoing drag.
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
            viewer.getTree(), DnDConstants.ACTION_COPY, dragGestureListener);

    }
    
    public void close(IViewContext context) {
    }

    public void open(IViewContext context) {
    
        // Save the context
        this.context = context;

        // Can't set the decorator until now
        decorator = new ScienceProgramDecorator(context.getShell());
        viewer.setDecorator(decorator);
        
        // Set up context
        context.setContent(scroll);
        context.setSelectionBroker(viewer);
        context.setTitle("Science Program");
        
    }

    public void setFocus() {
        viewer.getTree().requestFocus();
    }

    private final DragGestureListener dragGestureListener = new DragGestureListener() {            
        public void dragGestureRecognized(DragGestureEvent dge) {
            GSelection<Object> sel = viewer.getSelection();
            if (!sel.isEmpty() && sel.first() instanceof Obs) {
                Obs obs = (Obs) sel.first();
                
                // This is kind of cheating; there is no real reason why we should borrow the
                // decorator's variant rather than tracking it here. Just lazy. Easily refactored
                // if the need arises.
                Variant variant = decorator.getVariant();
                
                if (variant == null) return; // only in race condition
                Set<Flag> flags = variant.getFlags(obs);
                if (!flags.contains(Flag.SCHEDULED)) {    
                    GSelection<Alloc> selection = new GSelection<Alloc>(Alloc.forDragging(obs, DragLimit.value()));
                    Image dragImage = DragImage.forSelection(variant, selection);
                    dge.startDrag(DragSource.DefaultMoveNoDrop,    dragImage,
                        DragImage.getCenterOffset(dragImage), selection, null);
                }
            }
        }
    };

    private final MouseListener doubleClickListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {                
                GSelection<?> sel = viewer.getSelection();
                if (!sel.isEmpty() && sel.first() instanceof Obs) 
                    context.getShell().selectView(ShellAdvisor.ViewAdvisor.Candidates.name());
            }
        }
    };

    public void propertyChange(@SuppressWarnings("unused") PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
        
    }

}
