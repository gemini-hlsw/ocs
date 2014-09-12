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
import javax.swing.plaf.basic.*;

public class IterObserveGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JSpinner repeatSpinner = new JSpinner();

    public IterObserveGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {


        jLabel1.setLabelFor(repeatSpinner);
        jLabel1.setText("Observe");
        this.setLayout(gridBagLayout1);


        jLabel2.setText("X");
        repeatSpinner.setPreferredSize(new Dimension(80, 24));
        repeatSpinner.setToolTipText("Set the number of observations");
        this.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
        this.add(jLabel2,  new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 3, 0, 0), 0, 0));
        this.add(repeatSpinner, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 0, 0), 0, 0));
    }
}

