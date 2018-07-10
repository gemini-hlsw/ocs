package edu.gemini.qpt.ui.view.candidate;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.ShellAdvisor;
import edu.gemini.qpt.ui.action.DragLimit;
import edu.gemini.qpt.ui.util.DragImage;
import edu.gemini.qpt.ui.util.ScrollPanes;
import edu.gemini.qpt.ui.util.SimpleToolbar;
import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GTableViewer;
import edu.gemini.ui.workspace.IShell;
import edu.gemini.ui.workspace.IViewAdvisor;
import edu.gemini.ui.workspace.IViewContext;
import edu.gemini.ui.workspace.util.Factory;

import static edu.gemini.qpt.ui.view.candidate.CandidateObsAttribute.*;

public class CandidateObsViewAdvisor implements IViewAdvisor, PropertyChangeListener {

    // The viewer. Central doodad for this view.
    private final GTableViewer<Schedule, Obs, CandidateObsAttribute> viewer = 
        new GTableViewer<Schedule, Obs, CandidateObsAttribute>(new CandidateObsController());
    
    // Save the context
    private IViewContext context;
    
    // More UI stuff.
    private final JScrollPane scroll = Factory.createStrippedScrollPane(viewer.getTable());
    private final JPanel toolbar = new SimpleToolbar();
    private final JLabel text = new SimpleToolbar.StaticText("Double-click an observation to view it in context.");    
    private final JPanel content = new JPanel(new BorderLayout());
    
    public CandidateObsViewAdvisor() {
    
        // Set up the viewer
        viewer.setColumns(SB, P, Score, Observation, Target, RA, Inst, Dur);
        viewer.setColumnSize(Observation, 125);
        viewer.setColumnSize(Target, 90, Integer.MAX_VALUE);
        viewer.setColumnSize(RA, 50);
        viewer.setColumnSize(Inst, 50);
        viewer.setColumnSize(Dur, 45);
        viewer.setColumnSize(SB, 19);
        viewer.setColumnSize(P, 14);
        viewer.setColumnSize(Score, 35);
        viewer.setDecorator(new CandidateObsDecorator());
        viewer.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        viewer.setFilter(new CandidateObsFilter());
        viewer.setComparator(new CandidateObsComparator());
        viewer.getTable().addMouseListener(doubleClickListener);
        viewer.setTranslator(new CandidateObsTranslator());
        
        // Set viewport size
        ScrollPanes.setViewportWidth(scroll);
        
        // Set up the toolbar
        toolbar.add(text);
        
        // Put it all together.
        content.add(scroll, BorderLayout.CENTER);
        content.add(toolbar, BorderLayout.SOUTH);
        
        // Outgoing drag.
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
            viewer.getTable(), DnDConstants.ACTION_COPY, dragGestureListener);
        
    }
    
    public void close(IViewContext context) {
        // nop
    }

    public void open(IViewContext context) {
        
        this.context = context;
        
        context.setTitle("Candidate Observations");
        context.setContent(content);
        context.setSelectionBroker(viewer);
        context.getShell().addPropertyChangeListener(IShell.PROP_MODEL, this);
        
    }

    public void setFocus() {
        viewer.getTable().requestFocus();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            viewer.setModel((Schedule) evt.getNewValue());            
        }
    }

    private final DragGestureListener dragGestureListener = new DragGestureListener() {            
        public void dragGestureRecognized(DragGestureEvent dge) {            
            Obs obs = viewer.getElementAt(dge.getDragOrigin());            
            if (obs != null) {
                Variant variant = viewer.getModel().getCurrentVariant();
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
                context.getShell().selectView(ShellAdvisor.ViewAdvisor.Program.name());
            }
        }
    };
    

}
