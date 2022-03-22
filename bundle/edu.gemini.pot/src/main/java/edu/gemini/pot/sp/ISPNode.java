package edu.gemini.pot.sp;

import edu.gemini.pot.sp.version.LifespanId;
import edu.gemini.shared.util.VersionVector;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.ISPDataObject;

import java.beans.PropertyChangeListener;
import java.util.Set;

/**
 * This is the base interface for all the Science Program nodes.
 * It supports three features:
 * <p/>
 * <ol>
 * <li>listeners - support for property change listeners
 * <li>user objects - arbitrary (opaque) named objects stored on behalf
 * of the client
 * <li>data object - an arbitrary (opaque) object stored on behalf
 * of the client
 * </ol>
 * <p/>
 * <h3>Events</h3>
 * In addition to ordinary property change events, all nodes support
 * "composite" listeners.  Composite listeners will receive a composite
 * change event for any property change in the node or any of the node's
 * contained children or descendants.  Effectively, "composite" listeners
 * provide a way to listen to one portion of the Science Program hierarchy
 * for all property events that occur within that hierarchy.
 * <p/>
 * <h3>Client Data</h3>
 * Client data is named opaque data stored on behalf of the client. Property
 * change events, using the name "ClientData:<name>", are fired when client
 * data is updated.
 */
public interface ISPNode {

    /**
     * Names the property change event fired when anything in this node or
     * or in any contained child is modified.
     */
    String COMPOSITE_CHANGE_PROP = "CompositeChange";

    /**
     * Names the property change event fired when a data object is added
     * or replaced.
     */
    String CLIENT_DATA_PROP_PREFIX = "ClientData";
    String TRANSIENT_CLIENT_DATA_PROP_PREFIX = "TransientClientData";

    String EVENTS_ACTIVATED = "EventsActivated";

    /**
     * Client data key for the "DataObject" client data.
     */
    String DATA_OBJECT_KEY = "DataObject";
    String CONFLICTS_KEY   = "Conflicts";

    LifespanId getLifespanId();

    /**
     * Gets the unique key of the node.  Each node is uniquely identified.
     */
    SPNodeKey getNodeKey();

    /**
     * Gets key of the program with which this node is associated.  If this
     * is a {@link ISPProgram}, then this key will be the same as the
     * {@link #getNodeKey node key}.
     */
    SPNodeKey getProgramKey();

    /**
     * Gets the program id, which is the Gemini reference number by which the
     * program is known to humans.
     */
    SPProgramID getProgramID();

    /**
     * Gets this node's parent, if there is one.
     *
     * @return containing remote node, if any; <code>null</code> otherwise
     */
    ISPContainerNode getParent();

    /**
     * Gets the root ancestor of this node (which may be this node itself if
     * at the top level or not nested inside of another node).
     */
    ISPRootNode getRootAncestor();

    /**
     * Gets the program in which this node lives (if any).
     */
    ISPProgram getProgram();

    /**
     * Gets the observation associated with this node (if any).
     * @return the observation in which this node lives, if any;
     * <code>null</code> otherwise
     */
    ISPObservation getContextObservation();

    /**
     * Gets the observation ID associated with this node (if any).
     * @return the observation in which this node lives, if any
     */
    Option<SPObservationID> getContextObservationId();

    /**
     * Returns <code>true</code> if events are enabled, <code>false</code>
     * otherwise.  Events may be disabled if a series of updates to a tree
     * of science program nodes rooted at this node will undergo a series of
     * updates.  When events are re-enabled (see
     * {@link #setSendingEvents}) a single composite change event with key
     * {@link #EVENTS_ACTIVATED} is sent.
     *
     * @return <code>true</code> if events are enabled, <code>false</code>
     *         otherwise
     */
    boolean isSendingEvents();

    /**
     * Turns on or off the state of event sending.  If <code>true</code> events
     * will be sent for all future changes as normal, until the events are
     * turned off again.  If transitioning from events off to events on, a
     * single composite change event is sent to all registered listeners.  It
     * will have property name {@link #EVENTS_ACTIVATED}.
     *
     * @param sendEvents whether to turn on events (if <code>true</code>) or
     *                   turn off events (if <code>false</code>)
     */
    PropagationId setSendingEvents(boolean sendEvents);

    /**
     * Adds a property change listener that will receive events for all property
     * changes with the name <code>propName</code>.
     *
     * @param propName name of the property to monitor
     * @param pcl the listener that will receive the events
     */
    void addPropertyChangeListener(String propName, PropertyChangeListener pcl);

    /**
     * Removes the given listener from the list of those registered to receive
     * events for changes to the named property. If the listener is not in fact
     * registered for this property, nothing is done.
     * <p><b><i>Note</i></b>, this does not effect registrations to receive
     * generic property change event notifications.
     *
     * @param propName the name of the property whose changes should no longer
     *                 be tracked
     * @param pcl      the listener that should no longer receive the events
     */
    void removePropertyChangeListener(String propName, PropertyChangeListener pcl);

    /**
     * Adds a property change listener that will receive events for all property
     * changes in this or any contained node.
     *
     * @param pcl the listener that will receive the events
     */
    void addCompositeChangeListener(PropertyChangeListener pcl);

    /**
     * Removes the given property change listener.  If the listener is not in
     * fact registered, nothing is done.
     *
     * @param pcl the listener that should no longer receive the events
     */
    void removeCompositeChangeListener(PropertyChangeListener pcl);

    /**
     * Determines whether this node holds a data object with "staff only"
     * fields.  This is equivalent to calling getDataObject() and then checking
     * whether it implements {@link ISPStaffOnlyFieldProtected}, but without
     * the copy of the data object implied by getDataObject().
     */
    boolean hasStaffOnlyFields();

    /**
     * Returns (a copy of) the data object for this node.  This is a convenience
     * method that allows direct access to the "DataObject" client data.  It is
     * equivalent to <code>getClientData("DataObject")</code>.
     *
     * @return the serializable data object for the node
     */
    ISPDataObject getDataObject();

    /**
     * Replaces the data object for this node.  This is a convenience method
     * that allows direct access to the "DataObject" client data.  It is
     * equivalent to <code>setClientData("DataObject")</code>.
     * <p/>
     * <p>A property change event, using the name "ClientData:DataObject" is
     * fired by this method.
     *
     * @param dataObject a serializable object that contains the new
     *                   data for this node
     */
    PropagationId setDataObject(ISPDataObject dataObject);

    /**
     * Replaces the data object for this node and sets the version to match
     * the provided <code>newVersion</code>.  Usually the version is
     * automatically updated when the data object is changed and it is expected
     * that this method will be used only in very special cases where more
     * control over the resulting version vector is required.
     *
     * @param dataObject a serializable object that contains the new data for
     *                   this node
     * @param newVersion new version for this node.
     */
    PropagationId setDataObjectAndVersion(ISPDataObject dataObject, VersionVector<LifespanId, Integer> newVersion);

    /**
     * Gets the current version vector for this node.
     */
    VersionVector<LifespanId, Integer> getVersion();

    // --Commented out by Inspection (6/23/14 4:17 PM):void setVersion(VersionVector<LifespanId, Integer> version);

    /**
     * Replaces the data object for this node (firing a property change event
     * with name "ClientData:DataObject". If <code>conflicts</code> is
     * <code>true</code>, then the current version of the node's data object
     * is also stored in "ClientData:Conflicts".
     *
     * @param newValue new value for the data object
     * @param conflicts if <code>true</code>, store the current value in client
     *                  data
     */
    PropagationId setDataObject(ISPDataObject newValue, boolean conflicts);

    /**
     * Returns <code>true</code> if the node has a non-empty Conflicts client
     * data object.
     */
    boolean hasConflicts();

    /**
     * Gets the Conflicts object associated with this node, if any (returns
     * an empty Conflicts object if there are no conflicts)..
     * @return the client data keyed under ClientData:Conflicts, if
     * any; {@link Conflicts#EMPTY} otherwise
     */
    Conflicts getConflicts();

    void setConflicts(Conflicts c);

    /**
     * If this node has a {@link DataObjectConflict} stored in its client data,
     * it is updated to contain the current real data object value with the
     * opposite database perspective.  The current real data object is updated
     * to contain the value of the existing DataObjectConflict.
     *
     * <p>This method is called to select the opposite data object for viewing
     * or editing.</p>
     */
    void swapDataObjectConflict();

    void addConflictNote(Conflict.Note cn);

    /**
     * Resolves a data object conflict.
     */
    void resolveDataObjectConflict();

    void resolveConflict(Conflict.Note cn);

    void resolveConflicts();

    /**
     * Returns a <code>Set</code> view of the names of client data owned by
     * this node.  The returned <code>Set</code> is not backed by internal
     * data, and may be modified freely by the client.
     *
     * @return <code>Set</code> of <code>String</code> keys for the client
     *         data
     */
    Set<String> getClientDataKeys();

    /**
     * Gets the named client data, if it exists.
     *
     * @return the named client data; null if the given <code>name</code> is
     *         not known
     */
    Object getClientData(String name);

    /**
     * Adds or updates the named client data.  Listeners are will be notified,
     * and the property name will be "ClientData:<name>" where <name> is the
     * name of the client data.
     *
     * @param name name of the client data that is being added or modified
     * @param obj  the new value of the client data
     */
    PropagationId putClientData(String name, Object obj);

    /**
     * Removes the named user object.  Listeners will be notified.
     *
     * @param name name of the client data that is being removed
     */
    void removeClientData(String name);

    /**
     * Adds or updates the specified transient client data. As its name suggests,
     * transient client data is not serialized. This facility is provided for short-
     * term caching.
     */
    void putTransientClientData(String key, Object value);

    /**
     * Returns the specified transient client data.
     *
     * @see #putTransientClientData(String, Object)
     */
    Object getTransientClientData(String key);

    /**
     * Removes the specified transient client data.
     *
     * @see #putTransientClientData(String, Object)
     */
    void removeTransientClientData(String key);


    void getProgramReadLock();
    void returnProgramReadLock();

    void getProgramWriteLock();
    void returnProgramWriteLock();
    boolean haveProgramWriteLock();


    void addTransientPropertyChangeListener(String propName, PropertyChangeListener pcl);

    void removeTransientPropertyChangeListener(String propName, PropertyChangeListener pcl);
}
