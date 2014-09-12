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

public class IterDarkObsGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel2 = new JLabel();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel4 = new JLabel();
    JSpinner repeatSpinner = new JSpinner();
    JLabel jLabel5 = new JLabel();
    JLabel jLabel6 = new JLabel();
    NumberBoxWidget exposureTime = new NumberBoxWidget();
    NumberBoxWidget coadds = new NumberBoxWidget();

    public IterDarkObsGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        jLabel1.setRequestFocusEnabled(true);
    jLabel1.setLabelFor(coadds);
        jLabel1.setText("Coadds");


        jLabel2.setText("(exp / obs)");


        this.setLayout(gridBagLayout1);
        jLabel3.setText("X");


        jLabel4.setLabelFor(repeatSpinner);
        jLabel4.setText("Observe");


        repeatSpinner.setPreferredSize(new Dimension(80, 24));
        repeatSpinner.setToolTipText("Set the number of observations");
        jLabel5.setText("(sec)");


        jLabel6.setLabelFor(exposureTime);
        jLabel6.setText("Exposure Time");


        exposureTime.setBorder(BorderFactory.createLoweredBevelBorder());
        exposureTime.setToolTipText("Set the exposure time in seconds");
        exposureTime.setAllowNegative(false);
        coadds.setBorder(BorderFactory.createLoweredBevelBorder());
        coadds.setToolTipText("Set the coadds (number of exposures per observation)");
        coadds.setAllowNegative(false);
        this.add(jLabel2,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 3, 0, 0), 0, 0));
        this.add(jLabel1,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        this.add(jLabel3,  new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 3, 0, 0), 0, 0));
        this.add(jLabel4,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        this.add(repeatSpinner, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 0, 0), 0, 0));
        this.add(jLabel5,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 3, 0, 0), 0, 0));
        this.add(jLabel6,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        this.add(exposureTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 0, 0), 0, 0));
        this.add(coadds, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                                                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 0, 0), 0, 0));
    }
}
