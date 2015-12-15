package jsky.util.gui;

import java.awt.*;
import java.util.Vector;
import java.util.HashSet;

import javax.swing.*;

/**
 * A replacement for JComboBox that allows disabling individual items.
 *
 * @version $Revision: 8331 $
 * @author Allan Brighton
 */
public class SingleSelectComboBox<T> extends JComboBox<T> {

    // Set of disabled items
    private HashSet<T> _disabledSet = new HashSet<>();

    // used to enable/disable individual items
    private ListCellRenderer<Object> _renderer = new DefaultListCellRenderer() {
        @SuppressWarnings("SuspiciousMethodCalls")
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            boolean disabled = _disabledSet.contains(value);
            if (disabled) {
                isSelected = false;
            }
            JLabel label = (JLabel) super.getListCellRendererComponent(list,
                                                                       value,
                                                                       index,
                                                                       isSelected,
                                                                       cellHasFocus);
            label.setEnabled(!disabled);
            return label;
        }
    };


    /** Create an empty SingleSelectComboBox */
    public SingleSelectComboBox() {
        super();
        setRenderer(_renderer);
    }

    /** Create a SingleSelectComboBox based on the given model. */
    public SingleSelectComboBox(ComboBoxModel<T> model) {
        super(model);
        setRenderer(_renderer);
    }

    /** Create a SingleSelectComboBox containing the given items. */
    public SingleSelectComboBox(T[] ar) {
        super(ar);
        setRenderer(_renderer);
    }

    /** Create a SingleSelectComboBox containing the given items. */
    public SingleSelectComboBox(Vector<T> v) {
        super(v);
        setRenderer(_renderer);
    }

    /** Set the enabled state of the given item */
    public void setEnabledIndex(int index, boolean enabled) {
        setEnabledObject(getModel().getElementAt(index), enabled);
    }

    /** Set the enabled state of the given item */
    public void setEnabledObject(T o, boolean enabled) {
        if (enabled) {
            _disabledSet.remove(o);
        } else {
            _disabledSet.add(o);
        }
    }

    /** Set the enabled state of the given item */
    public void setEnabled(String s, boolean enabled) {
        ListModel<T> model = getModel();
        int n = model.getSize();
        for (int i = 0; i < n; i++) {
            T o = model.getElementAt(i);
            if (o != null && o.toString().equals(s)) {
                setEnabledObject(o, enabled);
                break;
            }
        }
    }

    /** Set the choices by specifying a Vector containing the choices. */
    public void setChoices(final Vector<T> v) {
        final DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        v.forEach(model::addElement);
        super.setModel(model);
    }

    /** Set the choices to the given objects. */
    public void setChoices(T[] ar) {
        DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
        for (T anAr : ar) model.addElement(anAr);
        super.setModel(model);
    }

    /** Return the selected item as a String */
    public String getSelected() {
        Object o = getSelectedItem();
        if (o != null) {
            return o.toString();
        }
        return null;
    }

    /** Stop actions for disabled items */
    @SuppressWarnings("unchecked")
    protected void fireActionEvent() {
        T o = (T)getSelectedItem();
        if (!_disabledSet.contains(o)) {
            super.fireActionEvent();
        }
    }

    /** Disallow selecting disabled items */
    public void setSelectedIndex(int anIndex) {
        T o = getModel().getElementAt(anIndex);
        if (o != null && _disabledSet.contains(o)) {
            return;
        }
        super.setSelectedIndex(anIndex);
    }

    /** Test main */
    public static void main(String[] args) {
        String[] ar = new String[]{"Test", "First Item", "Second Item", "Third Item", "Fourth Item", "Fifth Item"};

        final SingleSelectComboBox<String> sscb = new SingleSelectComboBox<>(ar);

        sscb.addActionListener(e -> System.out.println("XXX selected " + sscb.getSelectedItem()));

        //sscb.setSelectedObject("Third Item");
        sscb.setSelectedIndex(3);
        sscb.setEnabledIndex(4, false);
        sscb.setEnabledIndex(5, false);

        JFrame f = new JFrame("Test SingleSelectComboBox");
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        f.getContentPane().add(p);
        p.add(sscb, BorderLayout.SOUTH);
        f.pack();
        f.setVisible(true);
        f.addWindowListener(new BasicWindowMonitor());
    }
}

