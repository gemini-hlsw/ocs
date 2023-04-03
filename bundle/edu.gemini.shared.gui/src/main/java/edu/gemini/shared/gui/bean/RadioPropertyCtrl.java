package edu.gemini.shared.gui.bean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A PropertyCtrl implementation for associating a a set of radio buttons with a bean
 * property whose type is an enum.
 */
public final class RadioPropertyCtrl<B, T extends Enum<T>> extends PropertyCtrl<B, T> {

    private final JPanel pan;
    private final JRadioButton[] radio;
    private final Set<T> obsolete;

    private final ActionListener actionListener = e -> {
        T oldVal = getVal();
        T newVal = getSelectedRadioButtonValue();
        setVal(newVal);
        fireEditEvent(oldVal, newVal);
    };

    public RadioPropertyCtrl(final PropertyDescriptor pd) {
        this(pd, true);
    }

    /**
     * Constructs with the property descriptor, which must refer to an enum
     * property.
     */
    public RadioPropertyCtrl(final PropertyDescriptor pd, boolean vertical) {
        this(pd, vertical, Collections.emptyList());
    }

    public RadioPropertyCtrl(final PropertyDescriptor pd, boolean vertical, Iterable<T> obsoleteElements) {
        super(pd);

        pan   = new JPanel();
        pan.setLayout(new BoxLayout(pan, vertical ? BoxLayout.PAGE_AXIS : BoxLayout.LINE_AXIS));
        ButtonGroup group = new ButtonGroup();

        Class<?> c = pd.getPropertyType();
        @SuppressWarnings({"unchecked"}) T[] vals = (T[]) c.getEnumConstants();

        radio = new JRadioButton[vals.length];

        final Set<T> s = new HashSet<T>();
        obsoleteElements.forEach(s::add);
        obsolete = Collections.unmodifiableSet(s);

        int i=0;
        for (T val : vals) {
            final JRadioButton btn = new JRadioButton(val.toString());
            btn.setVisible(!obsolete.contains(val));
            btn.putClientProperty(getClass(), val);

            if (obsolete.contains(val)) {
                btn.setForeground(Color.gray);
                final Font f = btn.getFont();
                final Map attributes = f.getAttributes();
                attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                btn.setFont(new Font(attributes));
            }

            group.add(btn);
            pan.add(btn);
            radio[i++] = btn;
        }
    }

    private T getSelectedRadioButtonValue() {
        for (JRadioButton btn : radio) {
            if (btn.isSelected()) return getAssociatedValue(btn);
        }
        return getAssociatedValue(radio[0]);
    }

    private T getAssociatedValue(JRadioButton btn) {
        //noinspection unchecked
        return (T) btn.getClientProperty(getClass());
    }


    /**
     * Watches the comobo box for updates to the widget, setting the
     * corresponding value in the bean.
     */
    protected void addComponentChangeListener() {
        for (JRadioButton btn : radio) {
            btn.addActionListener(actionListener);
        }
    }

    /**
     * Stops watching the radio buttons for updates to the widget.
     */
    protected void removeComponentChangeListener() {
        for (JRadioButton btn : radio) {
            btn.removeActionListener(actionListener);
        }
    }

    /**
     * Gets the panel that is used to edit the property.
     */
    public JComponent getComponent() {
        return pan;
    }

    /**
     * Updates the value displayed in the radio buttons based upon the value of
     * the property in the bean.
     */
    public void updateComponent() {
        final T val = getVal();

        for (JRadioButton btn : radio) {
            final T btnValue = getAssociatedValue(btn);
            final boolean selected = btnValue == val;
            btn.setSelected(selected);
            final boolean visible  = selected || !obsolete.contains(btnValue);
            btn.setVisible(visible);
        }
    }

    /**
     * Updates the bean enum property based upon the value displayed in the
     * radio buttons.
     */
    public void updateBean() {
        T val = getSelectedRadioButtonValue();
        setVal(val);
    }
}
