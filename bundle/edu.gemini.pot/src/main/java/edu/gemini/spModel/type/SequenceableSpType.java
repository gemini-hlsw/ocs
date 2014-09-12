//
// $Id: SequenceableSpType.java 6980 2006-04-27 13:24:45Z shane $
//

package edu.gemini.spModel.type;

/**
 * An SpType derivative that provides a value to be sent to the sequence
 * executor which differs from the item's name.  The sequence value is the
 * value expected by the sequence executor. By default, the item's name should
 * will be used, but this interface may be implemented to provide a distinct
 * value.
 */
public interface SequenceableSpType {

    /**
     * Returns the sequence value representing this item as expected by the
     * sequence executor.
     */
    String sequenceValue();
}
