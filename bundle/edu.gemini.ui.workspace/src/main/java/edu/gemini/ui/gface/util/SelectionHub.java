package edu.gemini.ui.gface.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import edu.gemini.ui.gface.GSelection;
import edu.gemini.ui.gface.GSelectionBroker;

/**
 * A SelectionHub propagates selection changes between SelectionBrokers, keeping them
 * "in sync" as well as possible. It also provides methods allowing an external actor to push 
 * selection changes down to all member SelectionBrokers. A single SelectionBroker may be
 * designated as the primary broker, and its selection will be announced via property change
 * events.
 * @author rnorris
 */
@SuppressWarnings("unchecked")
public class SelectionHub<E> implements GSelectionBroker<E> {

    private static final Logger LOGGER = Logger.getLogger(SelectionHub.class.getName());
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final Set<GSelectionBroker<E>> brokers = new HashSet<>();
    private boolean broadcasting = false;
    private GSelectionBroker<E> primaryBroker;

    private final PropertyChangeListener listener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            synchronized (SelectionHub.this) {

                // Whose selection changed?
                GSelectionBroker<E> source = (GSelectionBroker<E>) evt.getSource();

                // we want to push a combination delta
                GSelection<E> oldSelection = (GSelection<E>) evt.getOldValue();
                GSelection<E> newSelection = (GSelection<E>) evt.getNewValue();

                // We only want to propogate changes if we're not broadcasting already.
                if (!broadcasting) {
                    broadcasting = true;

                    // Ok, do it.
                    for (GSelectionBroker<E> b: brokers)
                        if (source != b) {
                            LOGGER.finer("Informing " + b);
                            b.setSelection(newSelection);
                        }

                    broadcasting = false;
                }

                // Whether we are broadcasting or not, we want to forward
                // changes in the primary selection.
                if (source == primaryBroker) {
                    pcs.firePropertyChange(PROP_SELECTION, oldSelection, newSelection);
                }

            }
        }

    };

    @Override
    public synchronized void setSelection(GSelection<?> newSelection) {
        broadcasting = true;
        LOGGER.fine("External actor: selection set: + " + newSelection);
        for (GSelectionBroker<E> b: brokers)
            b.setSelection(newSelection);
        if (primaryBroker == null)
            pcs.firePropertyChange(PROP_SELECTION, GSelection.emptySelection(), newSelection);
        broadcasting = false;
    }

    public synchronized void add(GSelectionBroker<E> b) {
        brokers.add(b);
        b.addPropertyChangeListener(GSelectionBroker.PROP_SELECTION, listener);
    }

    public synchronized boolean remove(GSelectionBroker<E> b) {
        b.removePropertyChangeListener(GSelectionBroker.PROP_SELECTION, listener);
        return brokers.remove(b);
    }

    public synchronized void setPrimaryBroker(GSelectionBroker<E> newPrimaryBroker) {
        // re-assert the primary guy's selection?

        broadcasting = true;
        GSelection<E> prev = getSelection();
        primaryBroker = newPrimaryBroker;
        GSelection<E> newSelection = getSelection();
        LOGGER.fine("New Primary Broker: selection set: + " + newSelection);
        for (GSelectionBroker<E> b: brokers)
            if (b != primaryBroker)
                b.setSelection(newSelection);
        pcs.firePropertyChange(PROP_SELECTION, prev, newSelection);
        broadcasting = false;

    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    public synchronized GSelection<E> getSelection() {
        return primaryBroker != null ? primaryBroker.getSelection() : GSelection.emptySelection();
    }

}
