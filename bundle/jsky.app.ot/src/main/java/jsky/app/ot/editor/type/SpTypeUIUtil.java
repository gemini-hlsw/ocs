package jsky.app.ot.editor.type;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Utility methods for working with UI elements and SPType enumerated types.
 * $Id: SpTypeUIUtil.java 8331 2007-12-05 19:16:40Z anunez $
 */
public final class SpTypeUIUtil {

    private SpTypeUIUtil() {
        //avoids instantiation
    }

    /**
     * This method will initialize the JComboBox with the elements
     * indicated in Enum class, and will configure the widget to show all the
     * available options.
     */
    public static  <E extends Enum<E>> void initListBox(JComboBox widget, Class<E> c, ActionListener l) {
        SpTypeComboBoxModel<E> model = new SpTypeComboBoxModel<E>(c);
        widget.setModel(model);
        widget.setRenderer(new SpTypeComboBoxRenderer());
        widget.setMaximumRowCount(c.getEnumConstants().length);
        widget.addActionListener(l);
    }
}
