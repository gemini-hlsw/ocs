package edu.gemini.shared.gui.bean;

import javax.swing.*;
import java.beans.PropertyDescriptor;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A {@link PropertyCtrl} implementation for boolean properties that are
 * edited with a check box.
 */
public final class CheckboxPropertyCtrl<B> extends PropertyCtrl<B, Boolean> {

    private final JCheckBox check;

    private ActionListener actionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Boolean oldVal = getVal();
            Boolean newVal = check.getModel().isSelected();
            setVal(newVal);
            fireEditEvent(oldVal, newVal);
        }
    };

    public CheckboxPropertyCtrl(final PropertyDescriptor pd)  {
        super(pd);

        check = new JCheckBox(pd.getDisplayName());
    }

    protected void addComponentChangeListener() {
        check.addActionListener(actionListener);
    }

    protected void removeComponentChangeListener() {
        check.removeActionListener(actionListener);
    }

    public JComponent getComponent() {
        return check;
    }

    public void updateComponent() {
        check.getModel().setSelected(getVal());
    }

    public void updateBean() {
        setVal(check.isSelected());
    }
}
