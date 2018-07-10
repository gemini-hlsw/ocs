package edu.gemini.qpt.ui.view.candidate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import edu.gemini.qpt.core.Schedule;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.ui.util.CandidateDecorator;
import edu.gemini.skycalc.HHMMSS;
import edu.gemini.ui.gface.GSubElementDecorator;
import edu.gemini.ui.gface.GViewer;

public class CandidateObsDecorator implements GSubElementDecorator<Schedule, Obs, CandidateObsAttribute>, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(CandidateObsDecorator.class.getName());

    private GViewer<Schedule, Obs> viewer;
    private Variant variant;

    public void decorate(JLabel label, Obs obs, CandidateObsAttribute subElement, Object value) {

//        if (obs == null || subElement == null || value == null) {
//            LOGGER.warning("obs == " + obs + ", subElement == " + subElement + ", value == " + value);
////            return;
//        }

        // Alignment (and value, in the case of Score)
        switch (subElement) {
        case SB:
        case P:
        case Score:
        case RA: label.setHorizontalAlignment(SwingConstants.CENTER); break;
        default: label.setHorizontalAlignment(SwingConstants.LEFT); break;
        }

        // Format
        switch (subElement) {

        case P:

            label.setText(value.toString().substring(0, 1));
            break;

        case Score:

            int score = (variant == null) ? 0 : (int) (10000 * variant.getScore(obs));
            label.setText(Integer.toString(score));
            break;

        case RA:

            String hhmmss = HHMMSS.valStr((Double) value);
            int pos = hhmmss.indexOf('.');
            label.setText((pos == -1) ? hhmmss : hhmmss.substring(0, pos));
            break;

        case Dur:

            label.setText(TimeUtils.msToHHMMSS((Long) value));
            break;

        }

        // Color
        Set<Flag> flags = (variant != null) ? variant.getFlags(obs) : Collections.<Flag>emptySet();
        label.setForeground(CandidateDecorator.getColor(flags));

        // Icon
        switch (subElement) {
        case Observation: label.setIcon(CandidateDecorator.getIcon(flags, obs)); break;
        default: label.setIcon(null);
        }


    }

    public void propertyChange(PropertyChangeEvent evt) {

        // If the current variant changed, we need to swap our listeners.
        if (Schedule.PROP_CURRENT_VARIANT.equals(evt.getPropertyName())) {
            if (variant != null) variant.removePropertyChangeListener(Variant.PROP_FLAGS, this);
            variant = (Variant) evt.getNewValue();
            if (variant != null) variant.addPropertyChangeListener(Variant.PROP_FLAGS, this);
        }

//        LOGGER.info("Change: " + evt.getPropertyName());

        // And refresh the viewer.
        viewer.refresh();

    }

    public void modelChanged(GViewer<Schedule, Obs> viewer, Schedule oldModel, Schedule newModel) {

        // Ok, we need to track the current variant because its flags will determine how we want
        // to decorate stuff. So we want to unhook the old listeners, if any, then hook the new.
        if (variant != null) variant.removePropertyChangeListener(Variant.PROP_FLAGS, this);
        if (oldModel != null) oldModel.removePropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
        variant = newModel == null ? null : newModel.getCurrentVariant();
        if (newModel != null) newModel.addPropertyChangeListener(Schedule.PROP_CURRENT_VARIANT, this);
        if (variant != null) variant.addPropertyChangeListener(Variant.PROP_FLAGS, this);

        // Don't need to refresh yet (the viewer will do it), but keep track of the viewer.
        this.viewer = viewer;

    }

}


