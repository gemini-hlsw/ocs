// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TextBoxWidgetWatcher.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

public interface TextBoxWidgetWatcher {
    default void textBoxKeyPress(TextBoxWidget tbwe) {}
    default void textBoxAction(TextBoxWidget tbwe) {}
    default void textBoxDoneEditing(TextBoxWidget tbwe) {}
}

