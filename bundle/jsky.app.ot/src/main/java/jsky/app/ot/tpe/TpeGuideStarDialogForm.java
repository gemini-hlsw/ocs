package jsky.app.ot.tpe;

import jsky.catalog.Catalog;

import javax.swing.*;
import java.awt.*;

public class TpeGuideStarDialogForm extends JPanel {

    protected final JComboBox<Catalog> catalogComboBox =new JComboBox<>();
    protected final JComboBox<String> typeComboBox = new JComboBox<>();
    protected final JComboBox<String> instComboBox = new JComboBox<>();;
    protected final JLabel catalogWarning = new JLabel();
    protected final JButton okButton = new JButton("    OK    ");
    protected final JButton cancelButton = new JButton("Cancel");
    protected final JLabel guideStarWarning = new JLabel("Warning!");

    public TpeGuideStarDialogForm() {
        initComponents();
    }

    private void initComponents() {
        JLabel typeLabel = new JLabel();
        JLabel catalogLabel = new JLabel();
        JLabel instLabel = new JLabel();
        JPanel buttonPanel = new JPanel();

        //======== this ========
        setLayout(new GridBagLayout());
        setMinimumSize(new Dimension(460,170));
        setPreferredSize(new Dimension(460,170));
        add(catalogComboBox, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(11, 11, 0, 11), 0, 0));

        //---- typeLabel ----
        typeLabel.setLabelFor(null);
        typeLabel.setText("Guide Star Type:");
        add(typeLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(0, 11, 0, 0), 0, 0));
        add(typeComboBox, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(11, 11, 0, 11), 0, 0));

        //---- catalogLabel ----
        catalogLabel.setLabelFor(null);
        catalogLabel.setText("Search in Catalog:");
        add(catalogLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));

        //---- instLabel ----
        instLabel.setRequestFocusEnabled(true);
        instLabel.setVerifyInputWhenFocusTarget(true);
        instLabel.setText("Instrument:");
        add(instLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            new Insets(11, 11, 0, 0), 0, 0));
        add(instComboBox, new GridBagConstraints(1, 0, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(11, 11, 0, 11), 0, 0));

        //---- catalogWarning ----
        catalogWarning.setForeground(Color.red);
        add(catalogWarning, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(6, 11, 6, 0), 0, 0));

        //======== buttonPanel ========
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));

        //---- okButton ----
        buttonPanel.add(okButton);

        //---- cancelButton ----
        buttonPanel.add(cancelButton);

        add(buttonPanel, new GridBagConstraints(2, 3, 1, 2, 0.0, 0.0,
            GridBagConstraints.EAST, GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0), 0, 0));

        //---- guideStarWarning ----
        guideStarWarning.setForeground(Color.red);
        add(guideStarWarning, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
            GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
            new Insets(6, 11, 6, 0), 0, 0));

    }

}
