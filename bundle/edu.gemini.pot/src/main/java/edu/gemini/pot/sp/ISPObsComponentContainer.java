/**
 * $Id: ISPObsComponentContainer.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.pot.sp;


import java.util.List;

/**
 * This is the interface for a Science Program Observing Component Container, a node
 * that can contain <code>{@link ISPObsComponent}</code>s.
 */
public interface ISPObsComponentContainer extends ISPProgramNode {
    /**
     * Names the property in the {@link SPStructureChange} object delivered
     * when an observation component is added or removed.
     */
    String OBS_COMPONENTS_PROP = "ObsComponents";

    /**
     * Returns the <code>List</code> of observation components contained in the
     * container.
     *
     * @return a <code>List</code> of contained
     * <code>{@link ISPObsComponent}</code>s
     */
    List<ISPObsComponent> getObsComponents();

    /**
     * Replaces the <code>List</code> of observation components held by the
     * container. All the objects in the list must be
     * <ul>
     * <li><code>ISPObsComponent</code> implementations
     * <li>created in the same JVM as this container
     * <li>"free" (not already in any container)
     * </ul>
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBS_COMPONENTS_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPObsComponent}</code>
     * </ul>
     *
     * @param obsCompList a <code>List</code> that contains the observation
     * components to be contained by this container
     *
     * @throws SPNodeNotLocalException if any of the observation components in
     * the <code>obsCompList</code> were not created in the same JVM as this
     * container
     *
     * @throws SPTreeStateException if any of the observation components in the
     * <code>obsCompList</code> are already in another container or observation
     */
    void setObsComponents(List<? extends ISPObsComponent> obsCompList)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Adds an <code>ISPObsComponent</code> to the container.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *  <li>property name = <code>{@link #OBS_COMPONENTS_PROP}</code>
     *  <li>parent = this node
     *  <li>new value = <code>List</code> of <code>{@link ISPObsComponent}</code>
     * </ul>
     *
     * @param obsComp the <code>ISPObsComponent<code> to be added to the
     * container
     *
     * @throws SPNodeNotLocalException if the <code>obsComp</code> was not
     * created in the same JVM as this container
     *
     * @throws SPTreeStateException if the <code>obsComp</code> is already
     * in another container or observation
     */
    void addObsComponent(ISPObsComponent obsComp)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Add an <code>ISPObsComponent</code> to the container at a location given
     * by <code>index</code>.
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *  <li>property name = <code>{@link #OBS_COMPONENTS_PROP}</code>
     *  <li>parent = this node
     *  <li>new value = <code>List</code> of <code>{@link ISPObsComponent}</code>
     * </ul>
     *
     * @param index the place to locate the <code>ISPObsComponent</code>
     * @param obsComp the <code>ISPObsComponent</code> to be added to the
     * container
     *
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range
     * <code>(index < 0 || index > # components)
     *
     * @throws SPNodeNotLocalException if the <code>obsComp</code> was not
     * created in the same JVM as this container
     *
     * @throws SPTreeStateException if the <code>obsComp</code> is already
     * in another container or observation
     */
    void addObsComponent(int index, ISPObsComponent obsComp)
            throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException;

    /**
     * Removes an <code>ISPObsComponent</code> from the container.
     *
     * <p>A structure change event is fired for this method.  The
     * <code>{@link SPStructureChange}</code> object delivered will have
     * <ul>
     *  <li>property name = <code>{@link #OBS_COMPONENTS_PROP}</code>
     *  <li>parent = this node
     *  <li>new value = <code>List</code> of <code>{@link ISPObsComponent}</code>
     * </ul>
     *
     * @param obsComp the <code>ISPObsComponent</code> to be removed from
     * the container
     */
    void removeObsComponent(ISPObsComponent obsComp);
}
