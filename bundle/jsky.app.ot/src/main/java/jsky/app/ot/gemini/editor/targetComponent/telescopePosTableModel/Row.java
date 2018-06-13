package jsky.app.ot.gemini.editor.targetComponent.telescopePosTableModel;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * The generic concept of a row in the model.
 */
public abstract class Row {
    private final boolean enabled;
    private final boolean editable;
    private final String tag;
    private final String name;
    private final Icon icon;
    private final Option<Long> when;

    public boolean enabled() {
        return enabled;
    }

    public boolean editable() {
        return editable;
    }

    public String tag() {
        return tag;
    }

    public String name() {
        return name;
    }

    public Option<Double> distance() {
        return None.instance();
    }

    public boolean movable() {
        return enabled;
    }

    public Icon icon() {
        return icon;
    }

    public Option<Long> when() {
        return when;
    }

    public Border border(int col) {
        return col == 0 ? BorderFactory.createEmptyBorder(0, 5, 0, 0) : null;
    }

    public Row(final boolean enabled,
               final boolean editable,
               final String tag,
               final String name,
               final Icon icon,
               final Option<Long> when) {
        this.enabled = enabled;
        this.editable = editable;
        this.tag = tag;
        this.name = name;
        this.icon = icon;
        this.when = when;
    }
}
