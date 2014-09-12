/**
 * GUI for TReCS Engineering Component
 */
package jsky.app.ot.gemini.trecs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import jsky.util.gui.*;

import java.awt.event.*;

public class EngTReCSGUI extends JPanel {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel sectorWheelLabel = new JLabel();
    JLabel lyotWheelLabel = new JLabel();
    ButtonGroup beamsplittergroup = new ButtonGroup();
    Component component1;

    DropDownListBoxWidget apertureWheelComboBox = new DropDownListBoxWidget();
    JLabel apertureWheelLabel = new JLabel();
    ButtonGroup buttonGroup1 = new ButtonGroup();
    JLabel pupilImagingWheelLabel = new JLabel();
    DropDownListBoxWidget sectorWheelComboBox = new DropDownListBoxWidget();
    DropDownListBoxWidget lyotWheelComboBox = new DropDownListBoxWidget();
    DropDownListBoxWidget pupilImagingWheelComboBox = new DropDownListBoxWidget();

    public EngTReCSGUI() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        component1 = Box.createVerticalStrut(8);
        sectorWheelLabel.setText("Sector Wheel:");
        sectorWheelLabel.setToolTipText("");
        this.setMinimumSize(new Dimension(400, 453));
        this.setPreferredSize(new Dimension(400, 453));
        this.setToolTipText("");
        this.setLayout(gridBagLayout1);
        lyotWheelLabel.setText("Lyot Wheel:");
        lyotWheelLabel.setToolTipText("");
        apertureWheelLabel.setText("Aperture Wheel:");
        apertureWheelLabel.setToolTipText("");
        apertureWheelComboBox.setToolTipText("");
        apertureWheelComboBox.setActionCommand("");
        pupilImagingWheelLabel.setText("Pupil Imaging Wheel:");
        pupilImagingWheelComboBox.setActionCommand("");
        lyotWheelComboBox.setActionCommand("");
        this.add(sectorWheelLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                          , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(lyotWheelLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(component1, new GridBagConstraints(0, 4, 2, 1, 0.0, 1.0
                                                    , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        this.add(apertureWheelComboBox, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                                                               , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 11, 11), 0, 0));

        this.add(apertureWheelLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                                                            , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(pupilImagingWheelLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(sectorWheelComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
                                                             , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        this.add(lyotWheelComboBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                           , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
        this.add(pupilImagingWheelComboBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                                                                   , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 11), 0, 0));
    }
}
