//
// $Id: LoggableSpType.java 6980 2006-04-27 13:24:45Z shane $
//

package edu.gemini.spModel.type;

/**
 * An SpType derivative that provides a short form suitable for use in an
 * observing log.  By default, the item's name will be used for this purpose,
 * but this interface provides a mechanism whereby the log value may differ
 * from the name.
 */
public interface LoggableSpType extends SpType {
    /**
     * Returns the short form representing this item.  This value is suitable
     * for inclusion in the observing log.
     */
    String logValue();
}
