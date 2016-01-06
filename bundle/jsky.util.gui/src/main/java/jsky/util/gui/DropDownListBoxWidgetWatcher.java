package jsky.util.gui;

/**
 * An interface supported by clients that which to be notified of
 * DropDownListBoxWidget selection and action.
 */
@FunctionalInterface
public interface DropDownListBoxWidgetWatcher<T> {
    /**
     * Called when an item is selected.
     */
    void dropDownListBoxAction(DropDownListBoxWidget<T> ddlbwe, int index, String val);
}

