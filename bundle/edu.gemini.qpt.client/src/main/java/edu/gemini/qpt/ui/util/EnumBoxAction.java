package edu.gemini.qpt.ui.util;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;

import edu.gemini.ui.workspace.BooleanStateAction;

@SuppressWarnings("serial")
public class EnumBoxAction<T extends Enum<T>> extends AbstractAction implements PropertyChangeListener, BooleanStateAction {

    private T value;
    private EnumBox<T> box;
    
    public EnumBoxAction(EnumBox<T> box, T option, String text) {
        super(text);
        this.value = option;
        this.box = box;
        box.addPropertyChangeListener(this);
        propertyChange(null);
    }

    public void actionPerformed(ActionEvent e) {
        box.set(value);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        putValue(BooleanStateAction.BOOLEAN_STATE, box.get() == value);
    }
    
}
