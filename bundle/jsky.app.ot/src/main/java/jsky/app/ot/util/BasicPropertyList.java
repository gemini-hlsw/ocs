// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: BasicPropertyList.java 19598 2009-05-04 20:50:39Z swalker $
//
package jsky.app.ot.util;

import jsky.util.Preferences;

import java.io.Serializable;
import java.util.Vector;

/**
 * A basic (ordered) property list inspired by the Maribma PropertyList
 * interface.
 */
public class BasicPropertyList {

    // Base name of file where this object is serialized
    private String _name;

    // Stores the properties
    private Vector<PropertyListEntry> _props = new Vector<PropertyListEntry>();

    // Store a set of change watchers.
    private Vector<PropertyWatcher> _watchers = new Vector<PropertyWatcher>();

    // Class that holds the data for a property.
    private static class PropertyListEntry implements Serializable {
        String name;
        Object value;
    }


    /** Default constructor */
    public BasicPropertyList() {
    }

    /**
     * Construct an empty BasicPropertyList with the given name.
     * The name is used as the base name for serialization, which is
     * used to save the information between sessions.
     */
    public BasicPropertyList(String name) {
        _name = name;
    }


    // Lookup an entry, returning null if not found.
    private PropertyListEntry _lookupEntry(String name) {
        int sz = _props.size();
        for (int i = 0; i < sz; ++i) {
            PropertyListEntry ple = _props.elementAt(i);
            if (ple.name.equals(name))
                return ple;
        }
        return null;
    }

    // Lookup an entry, creating and inserting a new PropertyListEntry if
    // not found.
    private PropertyListEntry _getEntry(String name) {
        PropertyListEntry ple = _lookupEntry(name);
        if (ple == null) {
            ple = new PropertyListEntry();
            ple.name = name;
            _props.addElement(ple);
        }
        return ple;
    }

    /**
     * Add a property watchers.  Watchers are notified when a property
     * changes value.
     */
    public synchronized void addWatcher(PropertyWatcher watcher) {
        if (_watchers.contains(watcher))
            return;
        _watchers.addElement(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized void deleteWatcher(PropertyWatcher watcher) {
        _watchers.removeElement(watcher);
    }

    /**
     * Delete all watchers.
     */
    public synchronized final void deleteWatchers() {
        _watchers.removeAllElements();
    }

    /**
     * Get a copy of the _watchers Vector.
     */
    private synchronized Vector _getWatchers() {
        return (Vector) _watchers.clone();
    }

    /**
     * Notify watchers that a property has changed.
     */
    private void _notifyChange(String propertyName) {
        Vector v = _getWatchers();
        int cnt = v.size();
        for (int i = 0; i < cnt; ++i) {
            PropertyWatcher pw = (PropertyWatcher) v.elementAt(i);
            pw.propertyChange(propertyName);
        }
    }


    /** Save the settings for the next session */
    public void saveSettings() {
        // serialize this object for use in the next session
        if (_name != null) {
            try {
                Preferences.getPreferences().serialize(_name, _props);
            } catch (Exception e) {
                // ignore?
            }
        }
    }


    /** Restore the settings from the previous session */
    public void restoreSettings() {
        if (_name != null) {
            try {
                // deserialize the saved object from the previous session (ignore any errors)
                Preferences prefs = Preferences.getPreferences();
                Vector<PropertyListEntry> v = (Vector<PropertyListEntry>) prefs.deserialize(_name);

                int sz = _props.size();
                if (sz == v.size()) {
                    for (int i = 0; i < sz; i++) {
                        PropertyListEntry ple1 = _props.elementAt(i);
                        PropertyListEntry ple2 = v.elementAt(i);
                        if (ple1.name.equals(ple2.name)) {
                            ple1.value = ple2.value;
                        }
                    }
                }
            } catch (Exception e) {
                // ignore?
            }
        }
    }

    /**
     * Get the generic (Object) value associated with the named property.
     * This will have to be casted to the proper type.
     */
    public synchronized Object getValue(String name) {
        PropertyListEntry ple = _lookupEntry(name);
        if (ple == null)
            return null;
        return ple.value;
    }

    /**
     * Get the list of property names.
     */
    public String[] getPropertyNames() {
        Vector<String> propNames = new Vector<String>();

        synchronized (this) {
            int sz = _props.size();
            for (int i = 0; i < sz; ++i) {
                PropertyListEntry ple = _props.elementAt(i);
                propNames.addElement(ple.name);
            }
        }

        String[] namesA = new String[propNames.size()];
        propNames.copyInto(namesA);
        return namesA;
    }

    /**
     * Get a boolean property by the given name, defaulting to <tt>def</tt>
     * if the property doesn't exist or isn't a boolean.
     */
    public synchronized boolean getBoolean(String name, boolean def) {
        PropertyListEntry ple = _lookupEntry(name);
        if ((ple != null) && (ple.value instanceof Boolean)) {
            return (Boolean) ple.value;
        }
        return def;
    }

    /**
     * Set a boolean property with the given name and value.  This method
     * will create and add the property if it doesn't exist.  If it does
     * exist and is a boolean it will change the value to the argument
     * <tt>value</tt>.  If the property exists and isn't a boolean, it
     * will become a boolean and the previous value will be lost.
     */
    public synchronized void setBoolean(String name, boolean value) {
        PropertyListEntry ple = _getEntry(name);
        ple.value = value;
        _notifyChange(name);
    }

    /**
     * Get the value of the "choice" property with the given name, defaulting
     * to <code>def</def>.
     *
     * @see ChoiceProperty
     * @return The integer index of the current choice.
     */
    public synchronized int getChoice(String name, int def) {
        PropertyListEntry ple = _lookupEntry(name);
        if ((ple != null) && (ple.value instanceof ChoiceProperty)) {
            return ((ChoiceProperty) ple.value).getCurValue();
        }
        return def;
    }

    /**
     * Get the set of options for the given "choice" property.
     *
     * @see ChoiceProperty
     */
    public synchronized String[] getChoiceOptions(String name) {
        PropertyListEntry ple = _lookupEntry(name);
        if ((ple != null) && (ple.value instanceof ChoiceProperty)) {
            return ((ChoiceProperty) ple.value).getChoices();
        }
        return null;
    }

    /**
     * Set or initialize a "choice" property of the given name.  If
     * the property does not exist, it will be created.  If it does
     * exist, the <code>options</code> array will be ignored.
     *
     * @param name The name of the choice property.
     * @param options The array of possible options.
     * @param value The index of the current choice.
     *
     * @see ChoiceProperty
     */
    public synchronized void setChoice(String name, String[] options, int value) {
        PropertyListEntry ple = _getEntry(name);
        ChoiceProperty cp = new ChoiceProperty(options);
        cp.setCurValue(value);
        ple.value = cp;
        _notifyChange(name);
    }

    /**
     * Set an existing "choice" property to the given value, the
     * index of the desired option.
     *
     * @see ChoiceProperty
     */
    public synchronized void setChoice(String name, int value) {
        PropertyListEntry ple = _lookupEntry(name);
        if ((ple != null) && (ple.value instanceof ChoiceProperty)) {
            ((ChoiceProperty) ple.value).setCurValue(value);
            _notifyChange(name);
        }
    }

}

