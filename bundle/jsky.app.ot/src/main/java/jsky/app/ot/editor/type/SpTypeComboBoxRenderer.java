package jsky.app.ot.editor.type;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.ObsoletableSpType;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;

/**
 * A renderer for JComboBox instances that use the {@link SpTypeComboBoxModel}.
 * It displays an obsolete SP type with an asterisk.
 */
public class SpTypeComboBoxRenderer extends BasicComboBoxRenderer implements ListCellRenderer {
    public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean hasFocus) {
        String text = value.toString();
        if (value instanceof DisplayableSpType) {
            text = ((DisplayableSpType) value).displayValue();
        }
        if (value instanceof ObsoletableSpType) {
            ObsoletableSpType otype = (ObsoletableSpType) value;
            if (otype.isObsolete()) {
                text = text + "*";
            }
        }
        return super.getListCellRendererComponent(jList, text, index, isSelected, hasFocus);
    }
}
