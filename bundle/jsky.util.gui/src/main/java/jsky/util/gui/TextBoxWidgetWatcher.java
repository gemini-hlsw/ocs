package jsky.util.gui;

public interface TextBoxWidgetWatcher {
    default void textBoxKeyPress(TextBoxWidget tbwe) {}
    default void textBoxAction(TextBoxWidget tbwe) {}
    default void textBoxDoneEditing(TextBoxWidget tbwe) {}
}

