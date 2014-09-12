//
// $Id: DescribableSpType.java 6980 2006-04-27 13:24:45Z shane $
//

package edu.gemini.spModel.type;

/**
 * An SpType derivative that provides a longer text description of itself.
 */
public interface DescribableSpType extends SpType {

    /**
     * Returns the description of this item for use, for example, as a tool
     * tip in a GUI.
     */
    String description();
}
