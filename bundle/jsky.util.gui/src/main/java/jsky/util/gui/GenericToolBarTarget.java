package jsky.util.gui;

import javax.swing.Action;

/**
 * Application classes that use the GenericToolBar class need to implement
 * this interface. The target class defines an AbstractAction class for each
 * toolbar item, which should also be used in the menubar. The target class
 * can then enable and disable the actions instead of accessing the menubar
 * and toolbar directly to do this.
 */
public interface GenericToolBarTarget {

    /** Return the action for "Open" */
    Action getOpenAction();

    /** Return the action for "Back" */
    Action getBackAction();

    /** Return the action for "Forward" */
    Action getForwAction();
}
