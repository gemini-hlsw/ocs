package jsky.util.gui;

/**
 * An interface supported by clients that which to be notified of
 * ListBoxWidget selection and action.
 */
public interface ListBoxWidgetWatcher<T> {
    /**
     * Called when an item is selected.
     */
    void listBoxSelect(ListBoxWidget<T> lbwe, int index, Object val);

    /**
     * Called when an item is double clicked.
     */
    void listBoxAction(ListBoxWidget<T> lbwe, int index, Object val);
}

