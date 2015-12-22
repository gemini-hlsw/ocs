package edu.gemini.ui.gface;

import edu.gemini.ui.gface.util.PropertyChangeSource;

public interface GSelectionBroker<E> extends PropertyChangeSource {

    String PROP_SELECTION = "selection";

    GSelection<E> getSelection();

    void setSelection(GSelection<E> newSelection);
}
