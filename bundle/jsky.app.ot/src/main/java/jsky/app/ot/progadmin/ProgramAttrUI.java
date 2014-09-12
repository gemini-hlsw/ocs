//
// $
//

package jsky.app.ot.progadmin;

import javax.swing.*;
import java.awt.*;

/**
 * User interface widgets for program attributes.
 */
final class ProgramAttrUI extends JPanel {

    private JTextField programIdField;
    private JRadioButton classicalModeButton;
    private JRadioButton queueModeButton;
    private JTextField queueBandField;
    private JCheckBox rolloverBox;
    private JCheckBox thesisBox;
    private JCheckBox libraryBox;

    private JRadioButton tooNoneButton;
    private JRadioButton tooStandardButton;
    private JRadioButton tooRapidButton;


    private JTextField geminiEmailField;
    private JComboBox affiliateCombo;

    public ProgramAttrUI() {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        // Row 0
        int row = 0;
        JLabel lab = new JLabel("Id");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 0;
        gbc.gridy      = row;
        gbc.insets     = new Insets(0, 0, 0, 5);
        add(lab, gbc);

        programIdField = new JTextField();
        programIdField.setColumns(15);
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 1;
        gbc.gridy      = row;
        gbc.insets     = new Insets(0, 0, 0, 0);
        add(programIdField, gbc);

        classicalModeButton = new JRadioButton("Classical");
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 2;
        gbc.gridy      = row;
        gbc.insets     = new Insets(0, 10, 0, 5);
        add(classicalModeButton, gbc);

        queueModeButton = new JRadioButton("Queue");
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 3;
        gbc.gridy      = row;
        gbc.insets     = new Insets(0, 0, 0, 0);
        add(queueModeButton, gbc);

        ButtonGroup bg = new ButtonGroup();
        bg.add(classicalModeButton);
        bg.add(queueModeButton);

        lab = new JLabel("Band");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 4;
        gbc.gridy      = row;
        gbc.insets     = new Insets(0, 10, 0, 5);
        add(lab, gbc);

        queueBandField = new JTextField();
        queueBandField.setColumns(2);
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 5;
        gbc.gridy      = row;
        gbc.insets     = new Insets(0, 0, 0, 0);
        add(queueBandField, gbc);

        // Row 1
        ++row;
        lab = new JLabel("Attributes");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 0;
        gbc.gridy      = row;
        gbc.insets     = new Insets(10, 0, 0, 5);
        add(lab, gbc);

        JPanel attrPanel = new JPanel(new FlowLayout());
        rolloverBox = new JCheckBox("Rollover");
        rolloverBox.setToolTipText("Semester rollover valid only Band 1.");
        thesisBox = new JCheckBox("Thesis");
        libraryBox = new JCheckBox("Library");
        attrPanel.add(rolloverBox, gbc);
        attrPanel.add(thesisBox, gbc);
        attrPanel.add(libraryBox, gbc);

        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 1;
        gbc.gridy      = row;
        gbc.gridwidth  = 7;
        gbc.insets     = new Insets(10, 0, 0, 0);
        add(attrPanel, gbc);

        // Row 2
        ++row;
        lab = new JLabel("TOO");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 0;
        gbc.gridy      = row;
        gbc.gridwidth  = 1;
        gbc.insets     = new Insets(10, 0, 0, 5);
        add(lab, gbc);

        tooNoneButton     = new JRadioButton("None");
        tooStandardButton = new JRadioButton("Standard");
        tooRapidButton    = new JRadioButton("Rapid");

        bg = new ButtonGroup();
        bg.add(tooNoneButton);
        bg.add(tooStandardButton);
        bg.add(tooRapidButton);

        JPanel tooPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tooPanel.add(tooNoneButton);
        tooPanel.add(tooStandardButton);
        tooPanel.add(tooRapidButton);

        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 1;
        gbc.gridy      = row;
        gbc.gridwidth  = 7;
        gbc.insets     = new Insets(10, 0, 0, 0);
        add(tooPanel, gbc);

        // Row 3
        ++row;
        lab = new JLabel("Support");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 0;
        gbc.gridy      = row;
        gbc.gridwidth  = 1;
        gbc.insets     = new Insets(10, 0, 0, 5);
        add(lab, gbc);

        affiliateCombo = new JComboBox();
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 1;
        gbc.gridy      = row;
        gbc.gridwidth  = 7;
        gbc.insets     = new Insets(10, 0, 0, 0);
        add(affiliateCombo, gbc);

        // Row 4
        ++row;
        lab = new JLabel("Gem Email(s)");
        gbc.anchor     = GridBagConstraints.EAST;
        gbc.fill       = GridBagConstraints.NONE;
        gbc.gridx      = 0;
        gbc.gridy      = row;
        gbc.gridwidth  = 1;
        gbc.insets     = new Insets(10, 0, 0, 5);
        add(lab, gbc);

        geminiEmailField = new JTextField();
        geminiEmailField.setToolTipText("Gemini contact scientist email(s)");
        gbc.anchor     = GridBagConstraints.WEST;
        gbc.fill       = GridBagConstraints.HORIZONTAL;
        gbc.gridx      = 1;
        gbc.gridy      = row;
        gbc.gridwidth  = 7;
        gbc.weightx    = 1.0;
        gbc.insets     = new Insets(10, 0, 0, 0);
        add(geminiEmailField, gbc);
    }

    public JTextField getProgramIdField() {
        return programIdField;
    }

    public JRadioButton getClassicalModeButton() {
        return classicalModeButton;
    }

    public JRadioButton getQueueModeButton() {
        return queueModeButton;
    }

    public JTextField getQueueBandField() {
        return queueBandField;
    }

    public JCheckBox getRolloverBox() {
        return rolloverBox;
    }

    public JRadioButton getTooNoneButton() {
        return tooNoneButton;
    }

    public JRadioButton getTooStandardButton() {
        return tooStandardButton;
    }

    public JRadioButton getTooRapidButton() {
        return tooRapidButton;
    }

    public JCheckBox getThesisBox() {
        return thesisBox;
    }

    public JCheckBox getLibraryBox() {
        return libraryBox;
    }

    public JComboBox getAffiliateCombo() {
        return affiliateCombo;
    }

    public JTextField getGeminiContactEmailField() {
        return geminiEmailField;
    }
}
