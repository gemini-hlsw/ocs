/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TpeGuideStarDialogGUI.java 4336 2004-01-20 07:57:42Z gillies $
 */

package jsky.app.ot.tpe;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

class TpeGuideStarDialogGUI extends JPanel {

    GridBagLayout gridBagLayout1 = new GridBagLayout();
    Border border1;
    JComboBox catalogComboBox = new JComboBox();
    ButtonGroup buttonGroup1 = new ButtonGroup();
    JPanel buttonPanel = new JPanel();
    JButton cancelButton = new JButton();
    JButton okButton = new JButton();
    JLabel typeLabel = new JLabel();
    JComboBox typeComboBox = new JComboBox();
    JLabel catalogLabel = new JLabel();
    JLabel instLabel = new JLabel();
    JComboBox instComboBox = new JComboBox();

    public TpeGuideStarDialogGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(142, 142, 142));

        this.setLayout(gridBagLayout1);
        this.setMinimumSize(new Dimension(400, 138));
        this.setPreferredSize(new Dimension(400, 138));


        cancelButton.setText("Cancel");
        okButton.setText("OK");
        typeLabel.setLabelFor(typeComboBox);
        typeLabel.setText("Guide Star Type:");
        catalogLabel.setLabelFor(catalogComboBox);
        catalogLabel.setText("Search in Catalog:");
        instLabel.setRequestFocusEnabled(true);
        instLabel.setVerifyInputWhenFocusTarget(true);
        instLabel.setText("Instrument:");
        buttonPanel.add(okButton, null);
        this.add(catalogComboBox,  new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        this.add(buttonPanel,  new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        buttonPanel.add(cancelButton, null);
        this.add(typeLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(typeComboBox,   new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        this.add(catalogLabel,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(instLabel,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(instComboBox,   new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
    }
}



