/**
 * $Id: ObsStatusPanel.java 8519 2008-05-04 22:09:28Z swalker $
 */

package jsky.app.ot.viewer;

import edu.gemini.pot.sp.ISPGroup;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.spModel.obs.ObservationStatus;
import jsky.app.ot.ui.util.UIConstants;
import jsky.util.gui.ToggleButtonPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;


/**
 * Displays a row of toggle buttons for choosing observation status
 * values.
 */
public class ObsStatusPanel extends JPanel implements StatusFilter {

    private static final int GROUPS = ObservationStatus.values().length;
    private static final int NUM_BUTTONS = GROUPS+1;

    private ToggleButtonPanel _panel;

    public ObsStatusPanel() {
        Icon[] icons = new Icon[NUM_BUTTONS];
        int i=0;
        for (ObservationStatus status : ObservationStatus.values()) {
            icons[i++] = UIConstants.getObsIcon(status);
        }
        icons[GROUPS] = UIConstants.GROUP_ICON;

        _panel = new ToggleButtonPanel(icons, 1, icons.length, true, 1, 1);
        setLayout(new BorderLayout());
        add(_panel, BorderLayout.NORTH);

        for (i = 0; i < NUM_BUTTONS; i++) {
            JToggleButton b = _panel.getButton(i);
            b.setToolTipText(_getTip(i));
            b.setSelected(true);
        }
    }

    /**
     * Register to receive item events whenever a button is selected or deselected.
     */
    public void addItemListener(ItemListener l) {
        _panel.addItemListener(l);
    }

    /**
     * Stop receiving item events from this object.
     */
    public void removeItemListener(ItemListener l) {
        _panel.removeItemListener(l);
    }

    // Return a tool tip for the given status constant
    private String _getTip(int index) {
        if (index == GROUPS) return "Show/hide observation groups";
        ObservationStatus status;
        status = ObservationStatus.values()[index];
        return String.format("Show/hide observations with status \"%s\"",
                status.displayValue());
    }

    public boolean isStatusEnabled(ISPGroup grp) {
        return _panel.getButton(GROUPS).isSelected();
    }

    /**
     * Returns true if the toggle button corresponding to the given observation's status is
     * selected.
     */
    public boolean isStatusEnabled(ISPObservation obs) {
        final ObservationStatus status = ObservationStatus.computeFor(obs);
        return _panel.getButton(status.ordinal()).isSelected();
    }

    /**
     * Enable the display of observations with any status
     */
    public void setStatusEnabled() {
        for (int i = 0; i < NUM_BUTTONS; i++) {
            JToggleButton b = _panel.getButton(i);
            if (!b.isSelected()) b.setSelected(true);
        }
    }
}
