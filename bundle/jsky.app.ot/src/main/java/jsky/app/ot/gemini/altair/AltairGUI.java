/**
 * Title:        JSky<p>
 * Description:  <p>
 * Copyright:    Copyright (c) Allan Brighton<p>
 * Company:      <p>
 * @author Allan Brighton
 * @version 1.0
 */
package jsky.app.ot.gemini.altair;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.*;

import java.awt.event.*;

public class AltairGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel adcLabel = new JLabel();
    JLabel beamSplitterLabel = new JLabel();
    ButtonGroup beamsplittergroup = new ButtonGroup();
    CheckBoxWidget adcCheck = new CheckBoxWidget();
    OptionWidget eight50Button = new OptionWidget();
    OptionWidget oneButton = new OptionWidget();

    DropDownListBoxWidget aowfs = new DropDownListBoxWidget();
    JLabel aowfsLabel = new JLabel();
    ButtonGroup beamSplitterButtonGroup = new ButtonGroup();
  JLabel cassRotatorLabel = new JLabel();
  JRadioButton cassRotatorFollowingButton = new JRadioButton();
  JRadioButton cassRotatorFixedButton = new JRadioButton();
  Component component1;
  ButtonGroup cassRotatorButtonGroup = new ButtonGroup();

    public AltairGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        component1 = Box.createVerticalStrut(8);
    adcLabel.setText("Atmospheric Dispersion Corrector");
        adcLabel.setToolTipText("");
        this.setMinimumSize(new Dimension(400, 453));
        this.setPreferredSize(new Dimension(400, 453));
        this.setToolTipText("");
        this.setLayout(gridBagLayout1);
        beamSplitterLabel.setText("Dichroic Beamsplitter");
        beamSplitterLabel.setToolTipText("");
        adcCheck.setSelected(true);
        adcCheck.setText("On");
        eight50Button.setText("850 um");
        oneButton.setText("1 um");
        aowfsLabel.setText("Selected  AO WFS Object");
        aowfsLabel.setToolTipText("");
        aowfs.setToolTipText("The AOWFS that will be used with Altair");
        cassRotatorLabel.setToolTipText("");
    cassRotatorLabel.setVerifyInputWhenFocusTarget(true);
    cassRotatorLabel.setText("Cass Rotator");
    cassRotatorFollowingButton.setText("Following");
    cassRotatorFixedButton.setText("Fixed");
    this.add(adcLabel,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 0, 6), 0, 0));
        this.add(beamSplitterLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(adcCheck, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(eight50Button, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));

        this.add(aowfs, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
                                               , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));

        this.add(oneButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        this.add(aowfsLabel,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
    this.add(cassRotatorLabel,   new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
    this.add(cassRotatorFollowingButton,   new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
    this.add(cassRotatorFixedButton,   new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 11, 0, 0), 0, 0));
        beamSplitterButtonGroup.add(eight50Button);
        beamSplitterButtonGroup.add(oneButton);
    this.add(component1,       new GridBagConstraints(0, 6, 1, 1, 0.0, 1.0
            ,GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0));
    cassRotatorButtonGroup.add(cassRotatorFollowingButton);
    cassRotatorButtonGroup.add(cassRotatorFixedButton);
    }
}
