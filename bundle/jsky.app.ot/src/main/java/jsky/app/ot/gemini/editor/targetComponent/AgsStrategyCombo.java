package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.shared.util.immutable.*;
import jsky.app.ot.ags.AgsContext;
import jsky.app.ot.ags.AgsSelectorControl;

import javax.swing.*;
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

    public void setAgsOptions(final AgsContext opts) {
        combo.removeActionListener(this);
        combo.removeAllItems();

        final ImList<ComboEntry> validEntries = opts.validStrategies.map(agsStrategy -> new ComboEntry(agsStrategy.key().displayName(), new Some<>(agsStrategy)));

        final Option<ComboEntry> defaultEntry = opts.defaultStrategy.map(agsStrategy -> {
            final String name = String.format("Auto (%s)", agsStrategy.key().displayName());
            return new ComboEntry(name, None.<AgsStrategy>instance());
        });

        final ImList<ComboEntry> allEntries = defaultEntry.isEmpty() ? validEntries : validEntries.cons(defaultEntry.getValue());
        final ComboBoxModel<ComboEntry> model = new DefaultComboBoxModel<>(new Vector<>(allEntries.toList()));

        final Option<ComboEntry> sel = opts.usingDefault() ? defaultEntry :
            allEntries.find(comboEntry -> comboEntry.strategy.equals(opts.strategyOverride));

        sel.foreach(model::setSelectedItem);

        combo.setModel(model);
        combo.addActionListener(this);
    }
}
