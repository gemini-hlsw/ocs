// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ISPContainerNode.java 46832 2012-07-19 00:28:38Z rnorris $
//

package edu.gemini.pot.sp;

import java.beans.PropertyChangeListener;
import java.util.List;


/**
 * <code>ISPContainerNode</code> is an extension of the
 * <code>{@link ISPNode}</code>
 * interface meant for Science Program nodes that can have children.  Its
 * primary feature is support for "structure" listeners.  Structure events
 * and listeners are built on top of ordinary property change events. When a
 * client registers for structure events with a Science Program node supporting
 * this interface, it will receive an event whenever any child in the node is
 * added or removed.
 *
 * <p>Effectively, "structure" listeners provide a way to listen to one
 * portion of the Science Program hierarchy for all structure related
 * events that occur within that hierarchy.
 *
 * <p>The event's value object will be an instance of the
 * <code>{@link SPStructureChange}</code> class detailing the specific
 * sub-node at which the structure changed occurred along with the
 * effected property and new value.  For instance, if listening to the
 * root program node when an observation component is added to a contained
 * observation, the <code>{@link SPStructureChange}</code> value will contain:
 * <ul>
 *    <li>parent = a reference to the observation that was effected
 *    <li>property = <code>ISPObservation.OBS_COMPONENTS_PROP</code>
 *    <li>old value = list of <code>ISPObsComponent</code> that were
 *        contained in the observation
 *    <li>new value = list of <code>ISPObsComponent</code> now contained in
 *        the observation
 * </ul>
 */
public interface ISPContainerNode extends ISPProgramNode, ISPConflictFolderContainer {

    /**
     * Names the property change event fired when the children (structure) of
     * this node or any contained child is modified.
     */
    String STRUCTURE_CHANGE_PROP = "StructureChange";

    /**
     * Gets all the child nodes that are contained in this container.  Some
     * containers contain multiple types of nodes.  For example, the
     * {@link ISPProgram} contains both {@link ISPObsComponent} and
     * {@link ISPObservation}.  In that case, the List returned to the caller
     * will contain both types of node.  Use the sub-interface (i.e.,
     * {@link ISPProgram}) to find specific accessors for the various types
     * of children.
     *
     * @return List of {@link ISPNode} contained in this node; will not
     * return <code>null</code> but rather an empty List if there are no
     * children
     */
    List<ISPNode> getChildren();

    /**
     * Sets the child nodes that are contained in this container.  The children
     * must be valid children of the container.  For example, an
     * {@link ISPProgram} can only contain {@link ISPObsComponent} and
     * {@link ISPObservation}.
     *
     * @param children new children of this node; any existing children not in
     * the <code>children</code> List will be removed regardless of their type
     *
     * @throws SPException if any of the observations in the
     * <code>obsList</code> were not created in the same JVM as this program,
     * if any of the observations in the <code>obsList</code> are already in
     * another program or if they are not of a type accepted by this node
     */
    void setChildren(List<ISPNode> children) throws SPException;

    /**
     * Adds a structure change listener that will receive events for all
     * structure changes in this or any contained node.  See the class
     * description for more detail.
     *
     * <p>If the event registration lease is not continually renewed, events
     * will stop flowing to the client
     *
     *
     * @param pcl the client that will receive the events
     * @return an EventRegistration from which the client should extract the
     * lease
     *
     * @throws IllegalArgumentException if <code>leaseDuration</code> is
     * negative and not <code>Lease.ANY</code>
     */
    void addStructureChangeListener(PropertyChangeListener pcl);

    /**
     * Removes the given <code>RemoteEventListener</code> from the list of
     * clients registered to receive structure change events.  If the listener
     * is not in fact registered, nothing is done.
     *
     * @param pcl the listener that should no longer receive events
     */
    void removeStructureChangeListener(PropertyChangeListener pcl);

}

