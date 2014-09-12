//
// $Id: ObsoletableSpType.java 6980 2006-04-27 13:24:45Z shane $
//

package edu.gemini.spModel.type;

/**
 * An SpType derivative that contains one or more items that are no longer valid
 * in new programs.  An SpType should implement this interface when an item that
 * it contains has been discontinued.  User interface tools can use this
 * information to determine whether to display the item.
 */
public interface ObsoletableSpType extends SpType {

    /**
     * Returns <code>true</code> if the item should no longer be used;
     * <code>false</code> if it is still valid.
     */
    boolean isObsolete();
}
