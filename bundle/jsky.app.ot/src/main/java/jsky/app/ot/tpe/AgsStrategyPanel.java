package jsky.app.ot.tpe;

import edu.gemini.ags.api.AgsStrategy;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.PredicateOp;
import edu.gemini.shared.util.immutable.Some;
import jsky.app.ot.ags.AgsContext;
import jsky.app.ot.ags.AgsSelectorControl;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        this.label       = new JLabel("Guide With");
        this.buttonGroup = new ButtonGroup();
        this.buttons     = new ArrayList<>();
    }

    public JComponent getUi() { return pan; }

    public void setAgsOptions(final AgsContext opts) {
        // clear current strategies from gui
        pan.remove(label);
        for (JRadioButton button : buttons) {
            pan.remove(button);
            buttonGroup.remove(button);
        }
        buttons.clear();

        if (opts.nonEmpty()) {
            pan.add(label);

            final boolean usingDefault = opts.usingDefault();

            if (opts.defaultStrategy.isDefined()) {
                final JRadioButton button = mkButton(opts.defaultStrategy.getValue(), true);
                buttons.add(button);
                pan.add(button);
                button.setSelected(usingDefault);
            }

            for (final AgsStrategy s : opts.validStrategies) {
                final JRadioButton button = mkButton(s, false);
                buttons.add(button);
                pan.add(button);
                button.setSelected(!usingDefault && opts.strategyOverride.exists(new PredicateOp<AgsStrategy>() {
                    @Override public Boolean apply(AgsStrategy sel) { return sel == s; }
                }));
            }
        }

        pan.revalidate();
        pan.repaint();
    }

    private JRadioButton mkButton(final AgsStrategy s, final boolean isDefault) {
        final String name = String.format(isDefault ? "Auto (%s)" : "%s", s.key().displayName());
        return new JRadioButton(name) {{
            setToolTipText("Perform AGS search for " + s.key().displayName());
            addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    fireSelectionUpdate(isDefault ? None.<AgsStrategy>instance() : new Some<>(s));
                }
            });
        }};
    }
}


