package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.shared.util.immutable.*;
import jsky.app.ot.ags.AgsContext;
import jsky.app.ot.ags.AgsSelectorControl;
import jsky.util.gui.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * Combo box displaying available AGS strategy options.
 */
public final class AgsStrategyCombo extends AgsSelectorControl implements ActionListener {
    static final class ComboEntry {
        final String name;
        final Option<AgsStrategy> strategy;

        ComboEntry(String name, Option<AgsStrategy> strategy) {
            this.name     = name;
            this.strategy = strategy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ComboEntry that = (ComboEntry) o;
            return name.equals(that.name) && strategy.equals(that.strategy);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + strategy.hashCode();
            return result;
        }

        @Override public String toString() { return name; }
    }
    private final JComboBox<ComboEntry> combo = new JComboBox<>();

    public JComponent getUi() { return combo; }

    @Override
    public void actionPerformed(ActionEvent evt) {
        // how lame is it that we still have to cast this
        final ComboEntry ce = (ComboEntry) combo.getModel().getSelectedItem();
        if (ce != null) {
            fireSelectionUpdate(ce.strategy);
        }
    }

    private final DefaultListCellRenderer comboRenderer = new DefaultListCellRenderer() {
        private final Icon warningIcon = Resources.getIcon("eclipse/alert.gif");
        @Override public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                                final boolean isSelected, final boolean cellHasFocus) {
            final JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final ComboEntry ce = (ComboEntry) value;
            if (ce.name == "Off") {
                lab.setFont(lab.getFont().deriveFont(Font.ITALIC));
                lab.setIcon(warningIcon);
            }
            return lab;
        }
    };

    public void setAgsOptions(final AgsContext opts) {
        combo.removeActionListener(this);
        combo.removeAllItems();

        final ImList<ComboEntry> validEntries = opts.validStrategies.map(agsStrategy -> new ComboEntry(agsStrategy.key().displayName(), new Some<>(agsStrategy)));

        final Option<ComboEntry> defaultEntry = opts.defaultStrategy.map(agsStrategy -> {
            final String name = String.format("Default (%s)", agsStrategy.key().displayName());
            return new ComboEntry(name, None.instance());
        });

        final ImList<ComboEntry> allEntries = defaultEntry.isEmpty() ? validEntries : validEntries.cons(defaultEntry.getValue());
        final ComboBoxModel<ComboEntry> model = new DefaultComboBoxModel<>(new Vector<>(allEntries.toList()));

        final Option<ComboEntry> sel = opts.usingDefault() ? defaultEntry :
            allEntries.find(comboEntry -> comboEntry.strategy.equals(opts.strategyOverride));

        sel.foreach(model::setSelectedItem);

        combo.setModel(model);
        combo.setRenderer(comboRenderer);
        combo.addActionListener(this);
    }
}
