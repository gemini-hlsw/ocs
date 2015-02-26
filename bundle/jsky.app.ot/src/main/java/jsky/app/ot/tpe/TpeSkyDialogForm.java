package jsky.app.ot.tpe;

import jsky.catalog.Catalog;

import java.awt.*;
import javax.swing.*;

public class TpeSkyDialogForm extends JPanel {
    // TODO Make it not public
    final JComboBox<Catalog> catalogComboBox = new JComboBox<>();

    public TpeSkyDialogForm() {
        initComponents();
    }

    private void initComponents() {
        JLabel catalogLabel = new JLabel();

        //======== this ========
        setLayout(new GridBagLayout());
        add(catalogComboBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(11, 11, 0, 11), 0, 0));

        //---- catalogLabel ----
        catalogLabel.setLabelFor(null);
        catalogLabel.setText("Search in Catalog:");
        add(catalogLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(11, 11, 0, 0), 0, 0));
    }
}

