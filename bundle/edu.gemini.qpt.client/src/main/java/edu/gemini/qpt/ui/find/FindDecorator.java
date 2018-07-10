package edu.gemini.qpt.ui.find;

import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.ui.util.CandidateDecorator;
import edu.gemini.qpt.ui.util.SharedIcons;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

public class FindDecorator implements GSubElementDecorator<Schedule, FindElement, FindColumns> {

    private Variant variant;
    
    public void decorate(JLabel label, FindElement element, FindColumns subElement, Object value) {

        if (element.getTarget() instanceof Obs) {
            Obs obs = (Obs) element.getTarget();
            
            // Color
            Set<Flag> flags;
            if (obs.getObsClass() == null) // this is a bogus obs shell
                flags = Collections.singleton(Flag.INSTRUMENT_UNAVAILABLE); // totally terrible
            else
                flags = (variant != null) ? variant.getFlags(obs) : Collections.<Flag>emptySet();
            label.setForeground(CandidateDecorator.getColor(flags));
            
            // Icon
            switch (subElement) {
            case TARGET: label.setIcon(CandidateDecorator.getIcon(flags, obs)); break;
            default: label.setIcon(null);
            }

        } else {
            
            // Color is always the awful death color
            Set<Flag> flags = Collections.singleton(Flag.INSTRUMENT_UNAVAILABLE); // totally terrible
            label.setForeground(CandidateDecorator.getColor(flags));
            
            // Icon
            switch (subElement) {
            case TARGET: label.setIcon(SharedIcons.PROGRAM_CLOSED); break;
            default: label.setIcon(null);
            }
            
        }
    }

    public void modelChanged(GViewer<Schedule, FindElement> viewer, Schedule oldModel, Schedule newModel) {
        // TODO Auto-generated method stub
        variant = newModel == null ? null : newModel.getCurrentVariant();
    }

}
