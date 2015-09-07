// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: TextBoxWidgetWatcher.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;

public interface TextBoxWidgetWatcher {

    public void textBoxKeyPress(TextBoxWidget tbwe);
    public void textBoxAction(TextBoxWidget tbwe);

}

