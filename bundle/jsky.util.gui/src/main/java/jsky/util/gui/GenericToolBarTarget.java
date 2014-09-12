/*
 * ESO Archive
 *
 * $Id: GenericToolBarTarget.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/31  Created
 */

package jsky.util.gui;

import javax.swing.Action;


/**
 * Application classes that use the GenericToolBar class need to implement
 * this interface. The target class defines an AbstractAction class for each
 * toolbar item, which should also be used in the menubar. The target class
 * can then enable and disable the actions instead of accessing the menubar
 * and toolbar directly to do this.
 */
public abstract interface GenericToolBarTarget {

    /** Return the action for "Open" */
    public Action getOpenAction();

    /** Return the action for "Back" */
    public Action getBackAction();

    /** Return the action for "Forward" */
    public Action getForwAction();
}
