package jsky.util.gui;

import java.awt.*;

/**
 * Utility class for use with the GridBagLayout layout manager.
 */
public class GridBagUtil {

    private final GridBagLayout layout;
    private final Container container;

    public GridBagUtil(final Container container) {
        this.container = container;
        layout = new GridBagLayout();
        container.setLayout(layout);
    }

    public GridBagUtil(final Container container, final GridBagLayout layout) {
        this.layout = layout;
        this.container = container;
    }

    /**
     * Add the given component to the given container with the given options.
     */
    public void add(final Component component,
                    final int gridx, final int gridy,
                    final int gridwidth, final int gridheight,
                    final double weightx, final double weighty,
                    final int fill, final int anchor, final Insets insets,
                    final int ipadx, final int ipady) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.fill = fill;
        gbc.anchor = anchor;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.insets = insets;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        layout.setConstraints(component, gbc);
        container.add(component);
    }

    /**
     * Add the given component to the given container with the given options.
     */
    public void add(final Component component,
                    final int gridx, final int gridy,
                    final int gridwidth, final int gridheight,
                    final double weightx, final double weighty,
                    final int fill, final int anchor, final Insets insets) {

        add(component, gridx, gridy, gridwidth, gridheight,
            weightx, weighty, fill, anchor, insets, 0, 0);
    }

    /**
     * Add the given component to the given container with the given options and
     * default insets.
     */
    public void add(final Component component,
                    final int gridx, final int gridy,
                    final int gridwidth, final int gridheight,
                    final double weightx, final double weighty,
                    final int fill, final int anchor) {

        add(component, gridx, gridy, gridwidth, gridheight,
            weightx, weighty, fill, anchor,
            new Insets(1, 2, 1, 2));
    }
}

