//
// $Id: DisplayableSpType.java 6980 2006-04-27 13:24:45Z shane $
//

package edu.gemini.spModel.type;

/**
 * An SpType derivative that provides a display value different from its name.
 * By default, the item's name will be used but this interface provides a
 * mechanism whereby the display value may differ from the name.
 */
public interface DisplayableSpType extends SpType {
    /**
     * Returns a value representing this item as it should be displayed to a
     * user.
     */
    String displayValue();
}
