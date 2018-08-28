package edu.gemini.qpt.ui.view.mask;

import edu.gemini.spModel.ictd.CustomMaskKey;
import edu.gemini.qpt.core.Schedule;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

import java.util.Map;
import javax.swing.JLabel;

// Decorator just needed to format "SummitCabinet" as "Summit Cabinet".
public final class MaskDecorator implements GSubElementDecorator<Schedule, Map.Entry<CustomMaskKey, Availability>, MaskAttribute> {

    @Override
    public void decorate(JLabel label, Map.Entry<CustomMaskKey, Availability> element, MaskAttribute subElement, Object value) {
        switch (subElement) {
            case Availability:
                switch ((Availability) value) {
                    case SummitCabinet: label.setText("Summit Cabinet"); break;
                }
        }
    }

    @Override
    public void modelChanged(GViewer<Schedule, Map.Entry<CustomMaskKey, Availability>> viewer, Schedule oldModel, Schedule newModel) {
    }
}
