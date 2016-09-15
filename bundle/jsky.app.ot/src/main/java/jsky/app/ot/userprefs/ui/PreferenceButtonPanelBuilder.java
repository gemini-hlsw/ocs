package jsky.app.ot.userprefs.ui;

import edu.gemini.shared.gui.SeparateLineBorder;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.PredicateOp;

import javax.swing.*;
import java.awt.*;

/**
 * A factory for setting up a panel containing preference buttons.
 */
public final class PreferenceButtonPanelBuilder {
    private final ImList<Action> actions;

    /** Default border color. */
    static final Color SELECTED_BORDER_COLOR = new Color(128, 128, 128);

    /** Default selected color. */
    static final Color SELECTED_COLOR        = new Color(157, 157, 157);

    /** Default background color. */
    static final Color BACKGROUND_COLOR      = new Color(191, 191, 191);

    private Color selectedBorderColor = SELECTED_BORDER_COLOR;
    private Color selectedColor       = SELECTED_COLOR;
    private Color backgroundColor     = BACKGROUND_COLOR;

    /**
     * Constructs with the collection of actions to display as buttons in the
     * panel.
     */
    PreferenceButtonPanelBuilder(ImList<Action> actions) {
        this.actions = actions;
    }

    public JPanel build() {
        final JPanel toolBar = new JPanel(new GridBagLayout());

        // Add some space around the panel and underline it to set it off from
        // the content below.
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                new SeparateLineBorder(null, null, new SeparateLineBorder.Line(Color.darkGray, 1), null),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)));

        toolBar.setBackground(backgroundColor);

        // Setup the UI to be applied to each button in the panel.
        final PreferenceButtonUI buttonUI = new PreferenceButtonUI();
        buttonUI.setBackgroundColor(backgroundColor);
        buttonUI.setSelectedBorderColor(selectedBorderColor);
        buttonUI.setSelectedColor(selectedColor);

        // Figure out what size to show the text in each button.  If there are
        // icons, make it slightly smaller.  If there aren't, make it slightly
        // bigger.
        PredicateOp<Action> icon = act -> act.getValue(Action.SMALL_ICON) != null;
        final int fontSizeAdjustment = actions.exists(icon) ? -2 : 2;

        // Add a button for each action.
        final ButtonGroup grp = new ButtonGroup();
        actions.foreach(action -> {
            final JToggleButton button = new JToggleButton(action) {{
                setHorizontalTextPosition(CENTER);
                setVerticalTextPosition(BOTTOM);
                setFocusable(false);
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                setOpaque(false);
                setFont(getFont().deriveFont(getFont().getSize2D()+fontSizeAdjustment));
                setUI(buttonUI);
            }};
            grp.add(button);
            toolBar.add(button, new GridBagConstraints() {{
                gridx=toolBar.getComponentCount(); fill=NONE; weightx=0.0;
            }});
        });

        // Push everything to the left.
        toolBar.add(new JPanel() {{ setOpaque(false); }}, new GridBagConstraints() {{
            gridx=toolBar.getComponentCount(); fill=HORIZONTAL; weightx=1.0;
        }});

        return toolBar;
    }
}
