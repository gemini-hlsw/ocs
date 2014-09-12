/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.editor;

import java.awt.*;
import javax.swing.*;

import jsky.util.gui.NumberBoxWidget;

public class IterBiasObsGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel coaddsUnitsLabel = new JLabel();
    JLabel coaddsLabel = new JLabel();
    JLabel obsUnitsLabel = new JLabel();
    JLabel observeLabel = new JLabel();
    JSpinner repeatSpinner = new JSpinner();
    NumberBoxWidget coadds = new NumberBoxWidget();

    public IterBiasObsGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        coaddsLabel.setText("Coadds");

        coaddsLabel.setToolTipText("Set the number of exposures per observation");
        coaddsLabel.setLabelFor(coadds);

        coaddsUnitsLabel.setText("(exp / obs)");


        this.setLayout(gridBagLayout1);
        obsUnitsLabel.setText("X");


        observeLabel.setLabelFor(repeatSpinner);
        observeLabel.setText("Observe");


        repeatSpinner.setPreferredSize(new Dimension(80, 24));
        repeatSpinner.setToolTipText("Set the number of observations");
        coadds.setBorder(BorderFactory.createLoweredBevelBorder());
        coadds.setToolTipText("Set the coadds (number of exposures per observation)");
        coadds.setAllowNegative(false);
        this.add(coaddsUnitsLabel,   new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 3, 0, 0), 0, 0));
        this.add(coaddsLabel,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        this.add(obsUnitsLabel,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 6, 0, 0), 0, 0));
        this.add(observeLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        this.add(repeatSpinner, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 0, 0), 0, 0));
        this.add(coadds, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                                                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 0, 0), 0, 0));
    }
}
