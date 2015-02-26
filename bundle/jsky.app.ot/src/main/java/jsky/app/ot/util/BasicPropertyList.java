// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: BasicPropertyList.java 19598 2009-05-04 20:50:39Z swalker $
//
package jsky.app.ot.util;

import jsky.util.Preferences;

import java.util.ArrayList;
import java.util.List;


/**
 * A basic (ordered) property list of values used to record TpeImageFeature statuses.
 */
public class BasicPropertyList {
    // A unique name for property identification. Should be something along the lines of OwnerClass.class.getName().
    final private String classId;

    // The list of properties registered for this class.
    private List<BasicProperty> _props = new ArrayList<>();

    // The list of interested watchers.
    private List<PropertyWatcher> _watchers = new ArrayList<>();



    /**
     * Abstract superclass for all properties manageable by this property list.
     * We currently support only BooleanProperties and ChoiceProperties.
     * @see jsky.app.ot.util.BasicPropertyList.BooleanProperty
     * @see jsky.app.ot.util.BasicPropertyList.ChoiceProperty
     */
    public abstract class BasicProperty {
        final private String name;

        BasicProperty(String name) {
            this.name = name;
            _props.add(this);
        }

        public String getName() { return name; }
        public String getID()   { return classId + "." + name; }
    }

    /**
     * A property with a boolean value associated with it.
     */
    public class BooleanProperty extends BasicProperty {
        private boolean value;

        BooleanProperty(String name, boolean defaultValue) {
            super(name);
            value = Preferences.get(getID(), defaultValue);
        }

        public boolean getValue() { return value; }

        public void setValue(boolean value) {
            if (_setValue(value))
                _notifyChange(getName());
        }
        private synchronized boolean _setValue(boolean value) {
            Preferences.set(getID(), value);

            boolean oldValue = this.value;
            this.value = value;
            return oldValue != value;
        }
    }

    /**
     * A property with a list of choices (represented by strings) associated with it, and the
     * index of the selected choice in the list.
     */
    public class ChoiceProperty extends BasicProperty {
        final private String[] choices;
        private int selection;

        ChoiceProperty(String name, String[] choices, int defaultSelection) {
            super(name);
            this.choices = choices;
            selection = Integer.parseInt(Preferences.get(getID(), Integer.toString(defaultSelection)));
            if (selection < 0 || selection >= choices.length)
                selection = defaultSelection >= 0 && defaultSelection < choices.length ? defaultSelection : 0;
        }

        public int getSelection() { return selection; }

        public void setSelection(int selection) {
            if (_setSelection(selection))
                _notifyChange(getName());
        }
        public synchronized boolean _setSelection(int selection) {
            if (selection < 0 || selection >= choices.length)
                selection = 0;

            Preferences.set(getID(), Integer.toString(selection));

            int oldSelection = this.selection;
            this.selection = selection;
            return oldSelection != selection;
        }

        public String[] getChoices() { return choices; }
    }




    /**
     * Construct an empty BasicPropertyList with the given name.
     * The name is used as the base name for serialization, which is
     * used to save the information between sessions.
     */
    public BasicPropertyList(String classId) {
        this.classId = classId;
    }

    /**
     * Add a property watchers.  Watchers are notified when a property
     * changes value.
     */
    public synchronized void addWatcher(PropertyWatcher watcher) {
        if (_watchers.contains(watcher))
            return;
        _watchers.add(watcher);
    }

    /**
     * Delete a watcher.
     */
    public synchronized void deleteWatcher(PropertyWatcher watcher) {
        _watchers.remove(watcher);
    }

    /**
     * Delete all watchers.
     */
    public synchronized final void deleteWatchers() {
        _watchers.clear();
    }

    /**
     * Get a thread-safe list of the watchers for notification.
     */
    private synchronized List<PropertyWatcher> _getWatchers() {
        return new ArrayList<>(_watchers);
    }

    /**
     * Notify watchers that a property has changed.
     */
    private void _notifyChange(String propertyName) {
        List<PropertyWatcher> cloned = _getWatchers();
        for (PropertyWatcher pw : cloned)
            pw.propertyChange(propertyName);
    }

    /**
     * Get the list of properties.
     */
    public List<BasicProperty> getProperties() {
        return _props;
    }

    /**
     * Register a boolean property with the given name and default value.
     * If the property already has a value, that value will be restored.
     */
    public void registerBooleanProperty(String name, boolean defaultValue) {
        try {
            BooleanProperty bp = _getBooleanEntry(name, defaultValue);
        } catch (Exception e) {}
    }

    /**
     * Get a boolean property by the given name, defaulting to <tt>def</tt>
     * if the property doesn't exist or isn't a boolean.
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        try {
            return ((BooleanProperty) _lookupEntry(name)).getValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Set an existing boolean property to the given value.
     */
    public void setBoolean(String name, boolean value) {
        try {
            BooleanProperty bp = (BooleanProperty) _lookupEntry(name);
            bp.setValue(value);
        } catch (Exception e) {}
    }

    /**
     * Register a ChoiceProperty with the given name, options, and default value.
     * If the property already has a recorded value, that value will be restored.
     */
    public void registerChoiceProperty(String name, String[] options, int value) {
        try {
            ChoiceProperty cp = _getChoiceEntry(name, options, value);
        } catch (Exception e) {}
    }

    /**
     * Get the value of the choice property with the given name, returning a default
     * value if not able to do so.
     */
    public int getChoice(String name, int defaultValue) {
        try {
            return ((ChoiceProperty) _lookupEntry(name)).getSelection();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Set an existing choice property to the given value, the
     * index of the desired option.
     */
    public void setChoice(String name, int value) {
        try {
            ChoiceProperty cp = (ChoiceProperty) _lookupEntry(name);
            cp.setSelection(value);
        } catch (Exception e) {}
    }



    // Look up properties, returning null if not found.
    private BasicProperty _lookupEntry(String name) {
        for (BasicProperty bp : _props)
            if (bp.getName().equals(name))
                return bp;
        return null;
    }

    // Look up properties, creating and inserting new properties if not found.
    private synchronized BooleanProperty _getBooleanEntry(String name, boolean defaultValue) {
        BooleanProperty bp = (BooleanProperty) _lookupEntry(name);
        if (bp == null)
            bp = new BooleanProperty(name, defaultValue);
        return bp;
    }
    private synchronized ChoiceProperty _getChoiceEntry(String name, String[] choices, int selection) {
        ChoiceProperty cp = (ChoiceProperty) _lookupEntry(name);
        if (cp == null)
            cp = new ChoiceProperty(name, choices, selection);
        return cp;
    }
}

