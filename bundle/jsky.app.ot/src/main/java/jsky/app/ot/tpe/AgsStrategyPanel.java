package jsky.app.ot.tpe;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;
import jsky.app.ot.ags.AgsContext;
import jsky.app.ot.ags.AgsSelectorControl;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel of radio buttons of AGS strategy offerings used by the TPE.
 */
public final class AgsStrategyPanel extends AgsSelectorControl {
    private final JPanel pan = new JPanel();
    private final JLabel label;
    private final List<JRadioButton> buttons;
    private final ButtonGroup buttonGroup;

    public AgsStrategyPanel() {
        pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
        this.label       = new JLabel("Auto Guide Search");
        this.buttonGroup = new ButtonGroup();
        this.buttons     = new ArrayList<>();
    }

    public JComponent getUi() { return pan; }

    public void setAgsOptions(final AgsContext opts) {
        // clear current strategies from gui
        pan.removeAll();
        for (JRadioButton button : buttons) {
            buttonGroup.remove(button);
        }
        buttons.clear();

        if (opts.nonEmpty()) {
            pan.add(createRow(0, label));

            final boolean usingDefault = opts.usingDefault();

            final int indent = 5;

            if (opts.defaultStrategy.isDefined()) {
                final JRadioButton button = mkButton(opts.defaultStrategy.getValue(), true);
                buttons.add(button);
                pan.add(createRow(indent, button));
                button.setSelected(usingDefault);
            }

            for (final AgsStrategy s : opts.validStrategies) {
                final JRadioButton button = mkButton(s, false);
                buttons.add(button);
                pan.add(createRow(indent, button));
                button.setSelected(!usingDefault && opts.strategyOverride.exists(sel -> sel == s));
            }
        }

        pan.revalidate();
        pan.repaint();
    }

    private static Box createRow(int indent, JComponent cmp) {
        final Box b = Box.createHorizontalBox();
        b.add(Box.createHorizontalStrut(indent));
        b.add(cmp);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    private JRadioButton mkButton(final AgsStrategy s, final boolean isDefault) {
        final String name = String.format(isDefault ? "Default (%s)" : "%s", s.key().displayName());
        return new JRadioButton(name) {{
            setToolTipText("Perform AGS search for " + s.key().displayName());
            addActionListener(e -> fireSelectionUpdate(isDefault ? None.<AgsStrategy>instance() : new Some<>(s)));
        }};
    }
}


