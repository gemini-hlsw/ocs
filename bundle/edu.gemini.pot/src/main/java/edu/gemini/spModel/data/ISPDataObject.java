package edu.gemini.spModel.data;

import edu.gemini.pot.sp.ISPCloneable;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * This is a base interface for Science Program data objects.
 * It supports two features:
 *
 * <ol>
 * <li>listeners - support for remote property change listeners
 * <li>I/O - input output of a list of attributes.
 * </ol>
 *
 * <h3>Events</h3>
 * The listener feature allows ordinary property change events for the
 * properties of the data object.
 *
 * <h3>I/O</h3>
 * I/O is required to construct and export a data object to/from XML
 * (and other formats).  A property list is expected upon export and
 * passed to a constructor when creating the data object.
 */
public interface ISPDataObject extends ISPCloneable, Serializable {

    /**
     * Names the version property of all data objects.
     */
    String VERSION_PROP = "version";

    /**
     * The name of the title property.
     */
    String TITLE_PROP = "title";

    /**
     * Name of the parameter set kind.
     */
    String PARAM_SET_KIND = "dataObj";

    /**
     * Adds a property change listener that will receive
     * <code>RemoteValueEvent</code>s for all property changes in the
     * event source.  The value object contained in each event will be of type
     * <code>RemotePropertyChangeValue</code>.  If the listener is
     * already registered for all property changes, it will <em>not</em> be
     * re-registered.
     */
    void addPropertyChangeListener(PropertyChangeListener pcl);

    /**
     * Adds a property change listener that will receive
     * <code>RemoteValueEvent</code>s for modifications to the named
     * property in the event source.  The value object contained in each
     * event will be of type
     * <code>RemotePropertyChangeValue</code>. If the listener is
     * already registered for the <code>propName</code> property, it will
     * <em>not</em> be re-registered.
     */
    void addPropertyChangeListener(String propName,
                                   PropertyChangeListener pcl);

    /**
     * Removes the given <code>RemoteEventListener</code> from the list of
     * clients registered to receive events on any property change.  If the
     * listener is not in fact registered, nothing is done.
     *
     * <p><b><i>Note</i></b>, this does not effect registrations to receive
     * specific property change event notifications.
     */
    void removePropertyChangeListener(PropertyChangeListener pcl);

    /**
     * Removes the given <code>RemoteEventListener</code> from the list of
     * clients registered to receive events for changes to the named property.
     * If the listener is not in fact registered for this property, nothing
     * is done.
     *
     * <p><b><i>Note</i></b>, this does not effect registrations to receive
     * generic property change event notifications.
     *
     * @param propName the name of the property whose changes should no longer
     *        be tracked
     * @param rel the <code>RemoteEventListener</code> client that should no
     *        longer receive the events
     */
    void removePropertyChangeListener(String propName,
                                      PropertyChangeListener rel);

    /**
     * Fetch the title of the data object.
     */
    String getTitle();

    /**
     * Store the title of the data object.
     */
    void setTitle(String title);

    /**
     * Returns the part of the title that may be edited by a user. Some data objects
     * automatically insert a fixed part, which is not included here.
     */
    String getEditableTitle();

    /**
     * Return a version String for this data object.  The implication is that
     * data objects have a version that is changed when significant changes are
     * made to its internal structure that influences users of the object.
     */
    String getVersion();

    /**
     * Set the state of this object from the given parameter set.
     */
    void setParamSet(ParamSet paramSet);

    /**
     * Return a parameter set describing the current state of this object.
     */
    ParamSet getParamSet(PioFactory factory);

    /**
     * Get the item's type.  SpType is like an enumerated type in C++.  Each
     * type is mapped one-to-one with its own object.
     */
    SPComponentType getType();

    /**
     * A clone method that is callable from Scala and that hides the cast to the
     * desired type.
     * https://issues.scala-lang.org/browse/SI-6760
     */
    <A extends ISPDataObject> A clone(A a);
}
