// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ToggleButtonWidgetWatcher.java 6719 2005-11-08 19:35:36Z brighton $
//
package jsky.util.gui;



/**
 * A client implements this interface if it wants to register itself
 * as the watcher of an ToggleButtonWidget widget.
 *
 * @author	Allan Brighton
 */
public interface ToggleButtonWidgetWatcher {
    /**
     * A toggle button was changed.
     */
    public void toggleButtonAction(ToggleButtonWidget tbw);
}

