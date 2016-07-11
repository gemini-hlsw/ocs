//
// $
//

package edu.gemini.shared.gui.bean;

import javax.swing.*;
import java.beans.PropertyDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PropertyCtrl implementations are meant to ease a bit of the burden of tying
 * a widget that edits a property in a bean to the property value.  They should
 * set up listeners that keep the two in sync for example.  This class provides
 * a common base clase and get/set methods for the property.
 */
public abstract class PropertyCtrl<B, T> {
    private static final Logger LOG = Logger.getLogger(PropertyCtrl.class.getName());

    private final PropertyDescriptor desc;
    private final PropertyChangeListener beanListener;
    private final Set<EditListener<B, T>> listeners;
    private B bean;

    /**
     * Constructs with the PropertyDescriptor for the property being edited.
     */
    public PropertyCtrl(PropertyDescriptor desc) {
        this.desc = desc;
        listeners = new HashSet<>();

        // Create the bean listener.  When the property described by desc is
        // updated, the component is updated.
        beanListener = evt -> {
            removeComponentChangeListener();
            updateComponent();
            addComponentChangeListener();
        };
    }

    /**
     * Gets the descriptor associated with the property being edited.
     */
    public final PropertyDescriptor getDescriptor() {
        return desc;
    }

    /**
     * Adds a listener for edits to the widget. See {@link EditListener} for
     * more information.
     */
    public void addEditListener(EditListener<B, T> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for edits to the widget. See {@link EditListener} for
     * more information.
     */
    public void removeEditListener(EditListener<B, T> listener) {
        listeners.remove(listener);
    }

    /**
     * Called by subclasses to notify edit listeners that a change has been
     * made in the widget.
     *
     * @param oldValue the value that was previously represented in the widget
     * @param newValue the value that is now represented in the widget
     */
    protected void fireEditEvent(T oldValue, T newValue) {
        EditEvent<B, T> event;
        event = new EditEvent<>(this, oldValue, newValue);

        List<EditListener<B, T>> copy;
        copy = new ArrayList<>(listeners);

        for (EditListener<B, T> l : copy) {
            l.valueChanged(event);
        }
    }

    /**
     * Adds a property change listener to the bean via reflection on the
     * <code>addPropertyChangeListener(String, PropertyChangeListener)</code>
     * method.  When the listener updates the UI component to match.
     */
    protected void addBeanPropertyChangeListener() {
        try {
            Method m = bean.getClass().getMethod("addPropertyChangeListener", String.class, PropertyChangeListener.class);
            m.invoke(bean, desc.getName(), beanListener);
        } catch (Exception ex) {
            LOG.warning(String.format("Cannot watch bean %s for changes on property '%s'.",
                    bean.getClass().getName(), desc.getName()));
        }
    }

    /**
     * Adds a property change listener to the bean via reflection on the
     * <code>removePropertyChangeListener(String, PropertyChangeListener)</code>
     * method.
     */
    protected void removeBeanPropertyChangeListener() {
        B bean = getBean();
        if (bean == null) return;

        try {
            Method m = bean.getClass().getMethod("removePropertyChangeListener", String.class, PropertyChangeListener.class);
            m.invoke(bean, desc.getName(), beanListener);
        } catch (Exception ex) {
            LOG.warning(String.format("Cannot stop watching bean %s for changes on property '%s'.",
                    bean.getClass().getName(), desc.getName()));
        }
    }

    /**
     * Adds a listener to the UI component which should update the bean when
     * the UI is updated.  This method should be implemented by subclasses
     * to handle the details of this specific to the type of component.  It
     * is called by PropertyCtrl at the appropriate times to ensure that
     * the bean and the widget remain in sync.
     */
    protected abstract void addComponentChangeListener();

    /**
     * Removes the listener added by {@link #addBeanPropertyChangeListener()}.
     */
    protected abstract void removeComponentChangeListener();

    /**
     * Gets the current bean whose property is being edited, if any.
     */
    public final B getBean() {
        return bean;
    }

    /**
     * Sets the current bean whose property is to be edited.  Calls the
     * {@link #handleBeanUpdate} method so that subclasses can take any
     * special actions that may be required.
     */
    public final void setBean(B bean) {
        removeBeanPropertyChangeListener();
        B oldBean = getBean();
        this.bean = bean;
        handleBeanUpdate(oldBean, bean);
        addBeanPropertyChangeListener();
    }

    /**
     * Calls {@link #updateComponent} by default.  Subclasses may override if
     * the provide any special behavior.
     * @param oldBean old bean that was being edited
     * @param newBean new bean that should be edited
     */
    protected void handleBeanUpdate(B oldBean, B newBean) {
        removeComponentChangeListener();
        updateComponent();
        addComponentChangeListener();
    }

    /**
     * Returns the JComponent used to edit the property.
     */
    public abstract JComponent getComponent();

    /**
     * Performs whatever actions may be required to make the JComponent match
     * the current value of the property in the bean.  See also
     * {@link #updateBean}.
     */
    public abstract void updateComponent();

    /**
     * Performs whatever actions may be required to make the bean value match
     * what is displayed by the JComponent.  See also
     * {@link #updateComponent}.
     */
    public abstract void updateBean();

    /**
     * Gets the current value of the property from the bean.
     */
    protected T getVal() {
        if (bean == null) return null;
        Method m = desc.getReadMethod();
        try {
            //noinspection unchecked
            return (T) m.invoke(bean);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not get bean property: " + desc.getName(), ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Sets the current value of the property in the bean.
     */
    protected void setVal(T val) {
        if (bean == null) return;
        removeBeanPropertyChangeListener();
        Method m = desc.getWriteMethod();
        try {
            m.invoke(bean, val);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Could not set bean property: " + desc.getName(), ex);
            throw new RuntimeException(ex);
        } finally {
            addBeanPropertyChangeListener();
        }
    }
}
