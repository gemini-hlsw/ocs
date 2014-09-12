//
// $Id: SpType.java 6980 2006-04-27 13:24:45Z shane $
//

package edu.gemini.spModel.type;

/**
 * The basic methods required of an SpType.  Note these match the names of
 * corresponding methods on an enum.  An enum need not implement this interface.
 */
public interface SpType {
    String name();
    int ordinal();
}
