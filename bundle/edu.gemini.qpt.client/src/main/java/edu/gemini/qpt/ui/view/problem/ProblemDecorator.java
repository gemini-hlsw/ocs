package edu.gemini.qpt.ui.view.problem;

import static edu.gemini.qpt.ui.util.SharedIcons.*;

import javax.swing.JLabel;

import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

public class ProblemDecorator implements GSubElementDecorator<Schedule, Marker, ProblemAttribute> {

    public void decorate(JLabel label, Marker element, ProblemAttribute subElement, Object value) {
        
        // Text
        switch (subElement) {
        case Severity: label.setText(null);
        }
        
        // Icon
        switch (subElement) {
        case Severity:
            switch ((Severity) value) {
            case Error: label.setIcon(ICON_ERROR); break;
            case Warning: label.setIcon(ICON_WARN); break;
            case Notice: label.setIcon(ICON_NOTICE); break;
            case Info: label.setIcon(ICON_INFO); break;
            }
            break;
        default: label.setIcon(null);
        }
        
    }

    public void modelChanged(GViewer<Schedule, Marker> viewer, Schedule oldModel, Schedule newModel) {
    }

}
