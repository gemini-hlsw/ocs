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

import jsky.util.gui.TextBoxWidget;

import javax.swing.border.*;

public class ObsGroupGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel jLabel1 = new JLabel();
    TextBoxWidget obsGroupName = new TextBoxWidget();
    JLabel jLabel2 = new JLabel();
    JLabel totalTime = new JLabel();
    Border border1;

    public ObsGroupGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142));


        jLabel1.setLabelFor(obsGroupName);
        jLabel1.setText("Group Name");
        this.setLayout(gridBagLayout1);
        this.setPreferredSize(new Dimension(280, 280));
        obsGroupName.setPreferredSize(new Dimension(200, 21));
        obsGroupName.setToolTipText("Enter the name of the observation group");


        jLabel2.setText("Total Planned Time");
        totalTime.setFont(new java.awt.Font("Dialog", 0, 12));
        totalTime.setForeground(Color.black);
        totalTime.setToolTipText("");


        totalTime.setText("0 seconds");
        this.add(jLabel1,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        this.add(obsGroupName, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                      , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 11, 5, 5), 0, 0));
        this.add(jLabel2,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        this.add(totalTime, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 11, 5, 5), 0, 0));
    }
}
