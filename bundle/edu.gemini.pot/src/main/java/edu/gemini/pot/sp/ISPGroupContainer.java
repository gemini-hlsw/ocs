/**
 * $Id: ISPGroupContainer.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.pot.sp;


import java.util.List;

/**
 * This is the interface for a Science Program Group Container, a node
 * that can contain <code>{@link ISPGroup}</code>s.
 */
public interface ISPGroupContainer extends ISPProgramNode {

    /**
     * Names the property in the {@link SPStructureChange} object delivered
     * when an observation group component is added or removed.
     */
    String OBS_GROUP_PROP = "ObsGroups";

    /**
     * Returns the <code>List</code> of observation groups contained in the container.
     *
     * @return a <code>List</code> of contained
     * <code>{@link ISPGroup}</code>s
     */
    List<ISPGroup> getGroups();

    /**
     * Replaces the <code>List</code> of groups held by the container.
     * All the objects in the list must be
     * <ul>
     * <li><code>ISPGroup</code> implementations
     * <li>created in the same JVM as this container
     * <li>"free" (not already in any container)
     * </ul>
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBS_GROUP_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPGroup}</code>
     * </ul>
     *
     * @param groupList a <code>List</code> that contains the groups
     *        to be contained by this container
     *
     * @throws SPNodeNotLocalException if any of the groups in the
     * <code>obsList</code> were not created in the same JVM as this container
     *
     * @throws SPTreeStateException if any of the groups in the
     * <code>obsList</code> are already in another container
     */
    void setGroups(List<? extends ISPGroup> groupList)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Adds a <code>ISPGroup</code> to the container.  The group
     * must have been created in the same JVM as this container and must be
     * "free" (not already in any container).
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBS_GROUP_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPGroup}</code>
     * </ul>
     *
     * @param group the group to be added to the container
     *
     * @throws SPNodeNotLocalException if the group was not
     * created in the same JVM as this container
     *
     * @throws SPTreeStateException if the group is already in another
     * container
     */
    void addGroup(ISPGroup group)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Add an <code>ISPGroup</code> to the container at a location given
     * by <code>index</code>.  The group must have been created in the
     * same JVM as this container and must be "free" (not already in any container).
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBS_GROUP_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPGroup}</code>
     * </ul>
     *
     * @param index the place to locate the <code>ISPGroup</code>
     * @param group the <code>ISPGroup</code> to be added to the
     * container
     *
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range
     * <code>(index < 0 || index > # groups)
     *
     * @throws SPNodeNotLocalException if the group was not
     * created in the same JVM as this container
     *
     * @throws SPTreeStateException if the group is already in another
     * container
     */
    void addGroup(int index, ISPGroup group)
            throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException;

    /**
     * Removes an <code>ISPGroup</code>from the container.
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBS_GROUP_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPGroup}</code>
     * </ul>
     *
     * @param group the <code>ISPGroup</code> to be removed from
     * the container
     */
    void removeGroup(ISPGroup group);
}
