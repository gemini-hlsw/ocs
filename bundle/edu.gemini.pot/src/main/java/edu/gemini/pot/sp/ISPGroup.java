/**
 * $Id: ISPGroup.java 43724 2012-03-28 14:10:09Z swalker $
 */

package edu.gemini.pot.sp;

/**
 * This is the interface for a Science Program Group node.  Note that
 * as an <code>{@link ISPContainerNode}</code>, the <code>ISPGroup</code>
 * may accept structure listeners.  See the <code>ISPContainerNode</code>
 * class description for more detail. An ISPGroup node contains a list of
 * ISPObservations and ISPObsComponents for related notes.
 */
public interface ISPGroup extends ISPObservationContainer, ISPObsComponentContainer,
        ISPContainerNode, ISPProgramNode {

}

