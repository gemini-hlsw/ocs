// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: AbstractDataObject.java 43466 2012-03-22 14:56:36Z abrighton $
//
package edu.gemini.spModel.data;

import edu.gemini.pot.sp.ISPCloneable;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.io.Serializable;

/**
 * This is a base class for data objects implementing the
 * ISPDataObject interface.
 * <p>
 * Implementers of data objects can inherit from this class to get
 * support for property change and types.
 * <p>
 * The subclass may re-implement the I/O routines which have defaults
 * in this class.
 */
public class AbstractDataObject implements ISPCloneable, ISPDataObject, Serializable, IConfigProvider {
    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Useful for initializing properties.
     */
    public static final String EMPTY_STRING = "";

    /**
     * The unset VERSION value.
     */
    protected static final String DEFAULT_VERSION = "2009A-1";

    // The type of this data object
    private SPComponentType _type;

    // Each DataObject has a title
    private String _title;

    // Each DataObject has a version String
    private String _version = DEFAULT_VERSION;

    // Property change support for this object
    private transient PropertyChangeSupport _pcSupport;

    /**
     * The default constructor.  This sets the internal type to
     * <code><@link SPComponentType.UNKNOWN></code>.
     */
    public AbstractDataObject() {
        _type = SPComponentType.UNKNOWN;
        _title = EMPTY_STRING;
    }

    /**
     * Constructor for a specific SpComponent type.
     */
    public AbstractDataObject(SPComponentType type) {
        _type = type;
        _title = EMPTY_STRING;
    }

    /**
     * Implementation of the clone method.
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public Object clone() {
        AbstractDataObject result;
        try {
            result = (AbstractDataObject) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Won't happen, since Object implements cloneable ...
            throw new InternalError();
        }
        result._pcSupport = null;

        // Fine to share the reference
        result._type = _type;
        // _title is immutable
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends ISPDataObject> A clone(A a) {
        return (A) a.clone();
    }

    /**
     * Get the item's type.  SpType is like an enumerated type in C++.  Each
     * type is mapped one-to-one with its own object.
     */
    public final SPComponentType getType() {
        return _type;
    }

    /**
     * Set the item's type.
     */
    protected final void setType(SPComponentType type) {
        _type = type;
    }

    /**
     * Get the data object's broad type.
     */
    public final SPComponentBroadType getBroadType() {
        return _type.broadType;
    }

    /**
     * Get the data object's narrow type.
     *
     */
    public final String getNarrowType() {
        return _type.narrowType;
    }

    /**
     * Get the data object's readable string.
     *
     */
    public final String getReadable() {
        return _type.readableStr;
    }

    /**
     * Return an objects version.
     */
    public final String getVersion() {
        return _version;
    }

    /**
     * Set the data object's version.  This can only be set by the
     * data object.
     */
    protected final void setVersion(String version) {
        _version = version;
    }

    /**
     * Routine to determine if a title is valid or empty.
     */
    protected boolean isValidTitle() {
        return (_title != null && _title.length() != 0);
    }

    /**
     * A Helper routine to determine if the title has been changed
     * from the default.  If the title is the type readable, it has
     * not been changed.
     */
    public boolean isTitleChanged() {
        // Must use doGetTitle here to keep class getTitle from being called
        // recursively.
        String title = _doGetTitle();
        return (!title.equals(getType().readableStr));
    }

    // Returns the data object title.
    private String _doGetTitle() {
        if (isValidTitle()) {
            return _title;
        }
        return getType().readableStr;
    }

    /**
     * Returns the part of the title that may be edited by a user. Some data objects
     * automatically insert a fixed part, which is not included here.
     */
    public String getEditableTitle() {
        return getTitle(); // By default, just return the title
    }

    /**
     * Returns the data object title.
     */
    public String getTitle() {
        return _doGetTitle();
    }

    /**
     * Sets the title of the data object.
     */
    public void setTitle(String newValue) {
        String oldValue = _title;
        if (newValue != null && !newValue.equals(oldValue)) {
            _title = newValue;
            firePropertyChange(TITLE_PROP, oldValue, newValue);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(_type.readableStr);
        paramSet.setKind(ISPDataObject.PARAM_SET_KIND);

        // Only write the title as a property if it has been changed.
        if (isValidTitle()) {
            Pio.addParam(factory, paramSet, TITLE_PROP, getTitle());
        }

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        String v = Pio.getValue(paramSet, TITLE_PROP);
        if (v != null) {
            setTitle(v);
        }
    }

    /**
     * Gets the PropertyChangeSupport object, creating it if necessary.
     * This object is used to help fire ordinary property change events to
     * registered listeners.
     */
    protected final PropertyChangeSupport getPropertyChangeSupport() {
        if (_pcSupport == null) {
            _pcSupport = new PropertyChangeSupport(this);
        }
        return _pcSupport;
    }

    /**
     * Adds a property change listener that will be called whenever
     * any property is changed.
     */
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        PropertyChangeSupport pcSupport = getPropertyChangeSupport();
        for (PropertyChangeListener cur : pcSupport.getPropertyChangeListeners()) {
            if (cur == pcl) {
                return;
            }
        }
        pcSupport.addPropertyChangeListener(pcl);
    }

    /**
     * Adds a property change listener for a specific property.
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener pcl) {

        PropertyChangeSupport pcSupport = getPropertyChangeSupport();
        for (PropertyChangeListener cur : pcSupport.getPropertyChangeListeners(propertyName)) {
            if (cur == pcl) {
                return;
            }
        }
        getPropertyChangeSupport().addPropertyChangeListener(propertyName, pcl);
    }

    /**
     * Remove an all-properties property change listener.
     */
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        getPropertyChangeSupport().removePropertyChangeListener(pcl);
    }

    /**
     * Remove a property change listener for a specific property.
     */
    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener pcl) {
        getPropertyChangeSupport().removePropertyChangeListener(propertyName,
                                                                pcl);
    }

    /**
     * Fire a property change event to all general listeners and any
     * listeners for the specific property given by propertyName.
     * This method is only called if the new and old property values are
     * different.
     */
    public void firePropertyChange(String propertyName,
                                   Object oldValue, Object newValue) {
        getPropertyChangeSupport().firePropertyChange(propertyName,
                                                      oldValue, newValue);
    }

    /**
     * Fire a property change event to all general listener and any
     * listeners for the specific PropertyDescriptor given by pd.
     * This method is only called if the new and old property values are
     * different.
     */
    public void firePropertyChange(PropertyDescriptor pd,
                                   Object oldValue, Object newValue) {
        firePropertyChange(pd.getName(), oldValue, newValue);

    }


    /**
     * Propagate a property change event.
     */
    public void firePropertyChange(PropertyChangeEvent pce) {
        getPropertyChangeSupport().firePropertyChange(pce);
    }

    /**
     * Copies the property change listeners for all properties from the given
     * data object, adding them all to this data object.  This is a rather
     * weird method, which was created when trying to support a rather weird
     * feature of the old SPTargetList.
     */
    protected void copyPropertyChangeListenersFrom(AbstractDataObject that) {
        if ((that == null) || (that == this)) return;

        for (PropertyChangeListener pcl : that.getPropertyChangeSupport().getPropertyChangeListeners()) {
            addPropertyChangeListener(pcl);
        }
    }

    /**
     * Copies the property change listeners for the named property from the
     * given data object, adding them all to this data object.  This is a rather
     * weird method, which was created when trying to support a rather weird
     * feature of the old SPTargetList.
     */
    protected void copyPropertyChangeListenersFrom(String propertyName, AbstractDataObject that) {
        if ((that == null) || (that == this)) return;

        for (PropertyChangeListener pcl : that.getPropertyChangeSupport().getPropertyChangeListeners(propertyName)) {
            addPropertyChangeListener(propertyName, pcl);
        }
    }

    /**
     * Gets the ISysConfig configuration of the component.
     * The default implementation just returns null. This method may be
     * implemented by derived classes if needed.
     */
    public ISysConfig getSysConfig() {
        return null;
    }

    /**
     * Allows an outside source to set the ISysConfig.
     * The default implementation does nothing. This method may be
     * implemented by derived classes if needed.
     */
    public void setSysConfig(ISysConfig config) {
    }
}
