package jsky.plot;

import java.awt.*;
import javax.swing.*;

/**
 * <p>Title: Observing Tool</p>
 * <p>Description: Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Gemini 8m Telescopes Project</p>
 * @author Allan Brighton
 * @version 1.0
 */

/** User interface for TargetListPanel. */
public class TargetListPanelGUI extends JComponent {
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel nameLabel = new JLabel();
    JTextField nameField = new JTextField();
    JLabel raLabel = new JLabel();
    JTextField raField = new JTextField();
    JLabel decLabel = new JLabel();
    JTextField decField = new JTextField();
    JLabel descriptionLabel = new JLabel();
    JTextField descriptionField = new JTextField();
    JLabel priorityLabel = new JLabel();
    JTextField priorityField = new JTextField();
    JLabel categoryLabel = new JLabel();
    JTextField categoryField = new JTextField();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTable table = new JTable();
    JPanel addRemovePanel = new JPanel();
    JButton removeButton = new JButton();
    JButton addButton = new JButton();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JPanel buttonPanel = new JPanel();
    JButton okButton = new JButton();
    JButton cancelButton = new JButton();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JButton changeButton = new JButton();

    public TargetListPanelGUI() {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        nameLabel.setText("Name:");
        this.setLayout(gridBagLayout1);
        raLabel.setText("RA:");
        decLabel.setText("Dec:");
        descriptionLabel.setText("Description:");
        priorityLabel.setText("Priority:");
        categoryLabel.setText("Category:");
        removeButton.setToolTipText("Remove the selected target");
        removeButton.setText("Remove");
        addButton.setToolTipText("Add a new target");
        addButton.setText("Add");
        addRemovePanel.setLayout(gridBagLayout2);
        okButton.setText("OK");
        cancelButton.setText("Cancel");
        buttonPanel.setLayout(gridBagLayout3);
        nameField.setToolTipText("Enter the target name");
        raField.setToolTipText("Enter the RA coordinate in hh:mm:ss J2000");
        decField.setToolTipText("Enter the Dec coordinate in dd:mm:ss J2000");
        descriptionField.setToolTipText("Optional description of target");
        priorityField.setToolTipText("Optional priority (Low, Medium, High) for target");
        categoryField.setToolTipText("Optional category (queue band) for target");
        changeButton.setText("Change");
        this.add(nameLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(nameField, new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0
                                                   , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 0), 0, 0));
        this.add(raLabel, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
                                                 , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(raField, new GridBagConstraints(4, 0, 1, 1, 1.0, 0.0
                                                 , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 0), 0, 0));
        this.add(decLabel, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(decField, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0
                                                  , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 11), 0, 0));
        this.add(descriptionLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                                                          , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        this.add(descriptionField, new GridBagConstraints(2, 1, 3, 1, 0.0, 0.0
                                                          , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 0), 0, 0));
        this.add(priorityLabel, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        this.add(priorityField, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 0), 0, 0));
        this.add(categoryLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 11, 0, 0), 0, 0));
        this.add(categoryField, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                                                       , GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(11, 6, 0, 11), 0, 0));
        this.add(jScrollPane1, new GridBagConstraints(1, 4, 6, 1, 1.0, 1.0
                                                      , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(11, 11, 0, 11), 0, 0));
        jScrollPane1.getViewport().add(table, null);
        this.add(addRemovePanel, new GridBagConstraints(1, 3, 6, 1, 1.0, 0.0
                                                        , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        addRemovePanel.add(addButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                             , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        addRemovePanel.add(removeButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        addRemovePanel.add(changeButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
                                                                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 11, 0, 0), 0, 0));
        this.add(buttonPanel, new GridBagConstraints(1, 5, 6, 1, 1.0, 0.0
                                                     , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        buttonPanel.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
                                                             , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(11, 11, 11, 0), 0, 0));
        buttonPanel.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                                                         , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(11, 11, 11, 11), 0, 0));
    }
}
