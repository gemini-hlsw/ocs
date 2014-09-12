//
// $
//

package jsky.app.ot.too;

import edu.gemini.spModel.obs.ObsSchedulingReport;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public final class TooAlertDialog {
    private boolean _showObservation;

    public void show(ObsSchedulingReport report, Component parent) {

        String title = "Target of Opportunity Notification";

        String[] choices;
        choices = new String[] {
            "OK",
            "View Observation"
        };

        JPanel pan = new TooAlertPanel(report);
        pan.doLayout();

        int val = 0;
        String defaultChoice = choices[val];
        val = JOptionPane.showOptionDialog(parent,
                pan,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                choices,
                defaultChoice);

        _showObservation = (val == 1);
    }

    public boolean shouldViewObservation() {
        return _showObservation;
    }
}
