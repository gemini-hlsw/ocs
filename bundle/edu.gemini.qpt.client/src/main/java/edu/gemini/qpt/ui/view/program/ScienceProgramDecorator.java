package edu.gemini.qpt.ui.view.program;

import static edu.gemini.qpt.ui.util.SharedIcons.GROUP_CLOSED;
import static edu.gemini.qpt.ui.util.SharedIcons.NOTE;
import static edu.gemini.qpt.ui.util.SharedIcons.PROGRAM_OPEN;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.JLabel;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.shared.sp.Note;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.sp.Prog;
import edu.gemini.qpt.ui.util.CandidateDecorator;
import edu.gemini.ui.gface.GElementDecorator;
import edu.gemini.ui.gface.GViewer;
import edu.gemini.ui.workspace.IShell;

public class ScienceProgramDecorator implements GElementDecorator<Prog, Object>, PropertyChangeListener {

    private GViewer<Prog, Object> viewer;
    private Variant variant;
    private Schedule sched;
    
    public ScienceProgramDecorator(IShell shell) {
        shell.addPropertyChangeListener(IShell.PROP_MODEL, this);
    }
    
    public void decorate(JLabel label, Object element) {

        if (element instanceof Prog) {
            
            // Prog
            label.setIcon(PROGRAM_OPEN);
            label.setForeground(Color.BLACK);
            
        } else if (element instanceof Note) {
            
            // Note
            label.setIcon(NOTE);
            label.setForeground(Color.BLACK);
            
        } else if (element instanceof Group) {
            
            label.setIcon(GROUP_CLOSED);
            label.setForeground(Color.BLACK);

        } else if (element instanceof Obs) {
            
            Obs obs = (Obs) element;
            Set<Variant.Flag> flags = variant.getFlags(obs);
            label.setForeground(CandidateDecorator.getColor(flags));
            label.setIcon(CandidateDecorator.getIcon(flags, obs));
            label.setText("[" + obs.getObsNumber() + "] " +  obs.getTitle());
            
        }
        
        
        
    }

    public void modelChanged(GViewer<Prog, Object> viewer, Prog oldModel, Prog newModel) {
        this.viewer = viewer;
    }

    public void propertyChange(PropertyChangeEvent evt) {

        if (IShell.PROP_MODEL.equals(evt.getPropertyName())) {
            
            if (sched != null) sched.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);            
            sched = (Schedule) evt.getNewValue();
            if (sched != null) sched.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
            variant = sched == null ? null : sched.getCurrentVariant();
            viewer.getControl().repaint(); // enough?
                
        } else if (Schedule.PROP_CURRENT_VARIANT.equals(evt.getPropertyName())) {
            
            variant = (Variant) evt.getNewValue();
            viewer.getControl().repaint();
            
        }
        
    }

    public Variant getVariant() {
        return variant;
    }
    
}
