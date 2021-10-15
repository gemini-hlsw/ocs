package edu.gemini.shared.gui.bean;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyDescriptor;
import java.util.function.Function;

/**
 * A PropertyCtrl implementation for associating a JComboBox with a bean
 * property whose type is an enum.
 */
@SuppressWarnings({"unchecked"})
public final class ComboPropertyCtrl<B, T> extends PropertyCtrl<B, T> {

    /**
     * Creates a ComboPropertyCtrl instance that can be used for editing a
     * property whose type is an enum.
     *
     * @param pd property descriptor, assumed to refer to an enumerated type
     *
     * @param <B> bean class
     * @param <T> property type
     *
     * @return ComboPropertyCtrl configured to edit an enumerated type
     */
    public static <B, T> ComboPropertyCtrl<B, T> enumInstance(PropertyDescriptor pd) {
        Class c = pd.getPropertyType();
        T[] vals = (T[]) c.getEnumConstants();
        return new ComboPropertyCtrl<>(pd, vals);
    }

    /**
     * A renderer for an {@link Option} value whose type is an enumerated type.
     * Shows the {@link None} option as "Unspecified".
     */
    private static class OptionRenderer extends BasicComboBoxRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Option o = (Option) value;
            if (o == null || o.isEmpty()) { // can be null if combobox selection was set or restored to -1 in code
                lab.setText("*Unspecified");
                lab.setFont(getFont().deriveFont(Font.ITALIC));
            } else {
                lab.setText(o.getValue().toString());
                lab.setFont(getFont().deriveFont(Font.PLAIN));
            }
            return lab;
        }
    }

    private static <T> BasicComboBoxRenderer renderObject(final Function<T, String> toDisplay) {
        return new BasicComboBoxRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final JLabel lab = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lab.setText(toDisplay.apply((T) value));
                return lab;
            }
        };
    }

    /**
     * Creates a ComboPropertyCtrl for associating a JComboBox with a bean
     * property whose type is {@link Option} with a contained enumerated type.
     *
     * @param pd property descriptor
     * @param enumClass class of the enumerated type contained in the
     * {@link Option}
     *
     * @param <B> bean class
     * @param <T> property type wrapped by the {@link Option}
     *
     * @return ComboPropertyCtrl configured to edit an {@link Option} type that
     * wraps an enumerated type
     */
    public static <B, T> ComboPropertyCtrl<B, Option<T>> optionEnumInstance(PropertyDescriptor pd, Class enumClass) {
        T[] enumVals = (T[]) enumClass.getEnumConstants();
        Option<T>[] vals = new Option[enumVals.length + 1];

        vals[0] = None.instance();
        int i = 1;
        for (T enumVal : enumVals) {
            vals[i++] = new Some<>(enumVal);
        }

        ComboPropertyCtrl<B, Option<T>> res;
        res = new ComboPropertyCtrl<>(pd, vals);
        ((JComboBox<T>) res.getComponent()).setRenderer(new OptionRenderer());
        return res;
    }

    public static <B, T> ComboPropertyCtrl<B, Option<T>> optionInstance(PropertyDescriptor pd, T[] values) {
        final Option<T>[] optValues = new Option[values.length + 1];

        optValues[0] = None.instance();
        int i = 1;
        for (T v : values) optValues[i++] = new Some<>(v);

        final ComboPropertyCtrl<B, Option<T>> res = new ComboPropertyCtrl<>(pd, optValues);
        ((JComboBox<T>) res.getComponent()).setRenderer(new OptionRenderer());
        return res;
    }

    private final JComboBox<T> combo;

    private final ActionListener actionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            T oldVal = getVal();
            @SuppressWarnings({"unchecked"}) T newVal = (T) combo.getModel().getSelectedItem();
            setVal(newVal);
            fireEditEvent(oldVal, newVal);
        }
    };

    /**
     * Constructs with the property descriptor, which must refer to an enum
     * property.
     */
    public ComboPropertyCtrl(final PropertyDescriptor pd) {
        super(pd);

        Class c = pd.getPropertyType();
        combo = new JComboBox(c.getEnumConstants());
    }

    public ComboPropertyCtrl(PropertyDescriptor pd, T[] values) {
        this(pd, values, Object::toString);
    }

    public ComboPropertyCtrl(PropertyDescriptor pd, T[] values, Function<T, String> renderer) {
        super(pd);
        combo = new JComboBox<>(values);
        combo.setRenderer(renderObject(renderer));
    }

    /**
     * Watches the combo box for updates to the widget, setting the
     * corresponding value in the bean.
     */
    protected void addComponentChangeListener() {
        combo.addActionListener(actionListener);
    }

    /**
     * Stops watching the combo box for updates to the widget.
     */
    protected void removeComponentChangeListener() {
        combo.removeActionListener(actionListener);
    }

    /**
     * Gets the JComboBox that is used to edit the property.
     */
    public JComponent getComponent() {
        return combo;
    }


    /**
     * Updates the value displayed in the JCombo box based upon the value of
     * the property in the bean.
     */
    public void updateComponent() {
        combo.getModel().setSelectedItem(getVal());
    }

    /**
     * Updates the bean enum property based upon the value displayed in the
     * JComboBox.
     */
    public void updateBean() {
        @SuppressWarnings({"unchecked"}) T val = (T) combo.getModel().getSelectedItem();
        setVal(val);
    }
}
