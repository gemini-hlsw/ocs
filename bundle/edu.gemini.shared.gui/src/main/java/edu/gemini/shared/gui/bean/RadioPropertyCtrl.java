package edu.gemini.shared.gui.bean;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyDescriptor;

/**
 * A PropertyCtrl implementation for associating a a set of radio buttons with a bean
 * property whose type is an enum.
 */
public final class RadioPropertyCtrl<B, T extends Enum> extends PropertyCtrl<B, T> {

    private final JPanel pan;
    private final JRadioButton[] radio;


    private ActionListener actionListener = e -> {
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
        super(pd);

        pan   = new JPanel();
        pan.setLayout(new BoxLayout(pan, vertical ? BoxLayout.PAGE_AXIS : BoxLayout.LINE_AXIS));
        ButtonGroup group = new ButtonGroup();

        Class c = pd.getPropertyType();
        @SuppressWarnings({"unchecked"}) T[] vals = (T[]) c.getEnumConstants();

        radio = new JRadioButton[vals.length];

        int i=0;
        for (T val : vals) {
            JRadioButton btn = new JRadioButton(val.toString());
            btn.putClientProperty(getClass(), val);
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
        T val = getVal();

        for (JRadioButton btn : radio) {
            if (getAssociatedValue(btn) == val) {
                btn.setSelected(true);
            }
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
