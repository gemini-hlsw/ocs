/**
 * $Id: ISPObservationContainer.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.pot.sp;


import java.util.List;

/**
 * This is the interface for a Science Program Observation Container, a node
 * that can contain <code>{@link ISPObservation}</code>s.
 */
public interface ISPObservationContainer extends ISPProgramNode {

    /**
     * Names the property in the {@link SPStructureChange} object delivered
     * when an observation is added or removed.
     */
    String OBSERVATIONS_PROP = "Observations";

    /**
     * Returns the <code>List</code> of observations contained in this container.
     *
     * @return a <code>List</code> of contained
     * <code>{@link ISPObservation}</code>s
     */
    List<ISPObservation> getObservations();

    /**
     * Returns a <code>List</code> of all observations contained in this program,
     * including any observations contained in groups.
     *
     * @return a <code>List</code> of
     * <code>{@link ISPObservation}</code>s
     */
    List<ISPObservation> getAllObservations();

    /**
     * Replaces the <code>List</code> of observations held by this container.
     * All the objects in the list must be
     * <ul>
     * <li><code>ISPObservation</code> implementations
     * <li>created in the same JVM as this container
     * <li>"free" (not already in any container)
     * </ul>
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBSERVATIONS_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPObservation}</code>
     * </ul>
     *
     * @param obsList a <code>List</code> that contains the observations
     *        to be contained by this container
     *
     * @throws SPNodeNotLocalException if any of the observations in the
     * <code>obsList</code> were not created in the same JVM as this container
     *
     * @throws SPTreeStateException if any of the observations in the
     * <code>obsList</code> are already in another container
     */
    void setObservations(List<? extends ISPObservation> obsList)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Adds an <code>ISPObservation</code> to this container.  The observation
     * must have been created in the same JVM as this container and must be
     * "free" (not already in any container).
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBSERVATIONS_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPObservation}</code>
     * </ul>
     *
     * @param obs the observation to be added to this container
     *
     * @throws SPNodeNotLocalException if the observation was not
     * created in the same JVM as this container
     *
     * @throws SPTreeStateException if the observation is already in another
     * container
     */
    void addObservation(ISPObservation obs)
            throws SPNodeNotLocalException, SPTreeStateException;

    /**
     * Add an <code>ISPObservation</code> to this container at a locaton given
     * by <code>index</code>.  The observation must have been created in the
     * same JVM as this container and must be "free" (not already in any container).
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBSERVATIONS_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPObservation}</code>
     * </ul>
     *
     * @param index the place to locate the <code>ISPObservation</code>
     * @param obs the <code>ISPObservation</code> to be added to the
     * container
     *
     * @throws IndexOutOfBoundsException if <code>index</code> is out of range
     * <code>(index < 0 || index > # observations)
     *
     * @throws SPNodeNotLocalException if the observation was not
     * created in the same JVM as this container
     *
     * @throws SPTreeStateException if the observation is already in another
     * container
     */
    void addObservation(int index, ISPObservation obs)
            throws IndexOutOfBoundsException, SPNodeNotLocalException, SPTreeStateException;

    /**
     * Removes an <code>ISPObservation</code>from this container.
     *
     * <p>A structure change event is fired for this method.  The
     * {@link SPStructureChange} object delivered will have
     * <ul>
     *   <li>property name = {@link #OBSERVATIONS_PROP}
     *   <li>parent = this node
     *   <li>new value = <code>List</code> of <code>{@link ISPObservation}</code>
     * </ul>
     *
     * @param obs the <code>ISPObservation</code> to be removed from
     * this container
     */
    void removeObservation(ISPObservation obs);
}
