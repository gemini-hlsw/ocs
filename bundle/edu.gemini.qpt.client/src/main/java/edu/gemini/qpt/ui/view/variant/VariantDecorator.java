package edu.gemini.qpt.ui.view.variant;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.ui.util.CompositeIcon;
import edu.gemini.qpt.ui.util.SharedIcons;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

public class VariantDecorator implements GSubElementDecorator<Schedule, Variant, VariantAttribute> {

//  private static final Logger LOGGER = Logger.getLogger(VariantDecorator.class.getName());

    public static final Icon ICON_VARIANT = SharedIcons.ICON_VARIANT;
    public static final Icon ICON_VARIANT_WARNING = new CompositeIcon(ICON_VARIANT, SharedIcons.OVL_WARN);
    public static final Icon ICON_VARIANT_ERROR = new CompositeIcon(ICON_VARIANT, SharedIcons.OVL_ERROR);

    public void decorate(JLabel label, Variant element, VariantAttribute subElement, Object value) {

        if (subElement == VariantAttribute.Name) {

            // Icon
            Severity sev = element.getSeverity();
            if (sev != null) {
                switch (sev) {
                    case Error:   label.setIcon(ICON_VARIANT_ERROR);   break;
                    case Warning: label.setIcon(ICON_VARIANT_WARNING); break;
                    default:      label.setIcon(ICON_VARIANT);
                }
            } else {
                label.setIcon(ICON_VARIANT);
            }

            // Alignment
            label.setHorizontalAlignment(SwingConstants.LEFT);

        } else if (subElement == VariantAttribute.Wind) {

            // Icon
            label.setIcon(null);

            // Alignment
            label.setHorizontalAlignment(SwingConstants.CENTER);

        } else if (subElement == VariantAttribute.LGS) {

            // Icon
            label.setIcon(null);

            // Alignment
            label.setHorizontalAlignment(SwingConstants.CENTER);

        } else if (value != null && value instanceof Byte) {

            // Value is a byte
            switch ((Byte) value) {
                case (byte)   0: label.setText("-"); break;
                case (byte) 100: label.setText("A"); break;
            }

            // Icon
            label.setIcon(null);

            // Alignment
            label.setHorizontalAlignment(SwingConstants.CENTER);

        }

    }

    public void modelChanged(GViewer<Schedule, Variant> viewer, Schedule oldModel, Schedule newModel) {
        // We don't care; decorations don't depend on the model.
    }

}
