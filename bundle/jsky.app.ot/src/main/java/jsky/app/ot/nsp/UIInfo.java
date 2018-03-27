// Copyright 1999-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: UIInfo.java 18488 2009-03-05 20:43:11Z swalker $
//

package jsky.app.ot.nsp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * This class holds UI and display properties for SP items.
 */
public final class UIInfo implements Serializable {
    // for serialization
    private static final long serialVersionUID = 2L;

    // common use: spNode.getUserObject(UIInfo.NAME);
    /** This name will be used as the user object name. */
    public static final String NAME = "UIINFO";

    /**
     * By default objects will be visible.
     */
    public static final boolean DEFAULT_VISIBILITY = true;

    // -- suggested values for the type field --
    public static final String TYPE_SCIENCE_PROGRAM = "scienceProgram";
    public static final String TYPE_PHASE1 = "phase1";
    public static final String TYPE_OBSERVATION = "observation";
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_DATA = "data";
    public static final String TYPE_SITE_QUALITY = "siteQuality";
    public static final String TYPE_TARGET_ENV = "targetEnv";
    public static final String TYPE_INSTRUMENT = "instrument";
    public static final String TYPE_AO_INSTRUMENT = "ao";
    public static final String TYPE_ENG_COMP = "engComp";    // engineering component
    public static final String TYPE_ITER_COMP = "iterComp";  // iterator component
    public static final String TYPE_ITER_OBS = "iterObs";    // observe iterator
    public static final String TYPE_SEQ_BASE = "seqBase";
    public static final String TYPE_DATA_PROC = "dataProc";


    public static final class Id implements Comparable<Id> {
        private final String id;

        public Id(String id) {
            if (id == null) throw new NullPointerException();
            this.id = id;
        }

        public int compareTo(Id that) {
            return id.compareTo(that.id);
        }

        @Override public int hashCode() {
            return id.hashCode();
        }

        @Override public boolean equals(Object obj) {
            if (!(obj instanceof Id)) return false;
            final Id that = (Id) obj;
            return id.equals(that.id);
        }

        @Override public String toString() {
            return id;
        }
    }

    public static final class UIInfoBuilder {
        private Id _id = null;
        private String _name = null;
        private String _imageKey = null;     // used to create an Icon
        private String _shortDescription = "";
        private String _toolTipText = null;  // default would be no tooltip
        private boolean _visible = DEFAULT_VISIBILITY;
        private boolean _expert = false;
        private String _uiClassName = null;
        private boolean _readOnly = false;
        private boolean _isUnique = true;     // Can the data object appear > once?
        private Site _site = null;
        private boolean _onSite = false;
        private String _type = null;
        private String _dataObjectClassName = null;

        public UIInfoBuilder() {
        }

        public UIInfoBuilder(UIInfo that) {
            _id                  = that._id;
            _name                = that._name;
            _imageKey            = that._imageKey;
            _shortDescription    = that._shortDescription;
            _toolTipText         = that._toolTipText;
            _visible             = that._visible;
            _expert              = that._expert;
            _uiClassName         = that._uiClassName;
            _readOnly            = that._readOnly;
            _isUnique            = that._isUnique;
            _site                = that._site;
            _onSite              = that._onSite;
            _type                = that._type;
            _dataObjectClassName = that._dataObjectClassName;
        }

        public UIInfoBuilder id(Id id) { _id = id; return this; }
        public UIInfoBuilder name(String name) { _name = name; return this; }
        public UIInfoBuilder imageKey(String imageKey) { _imageKey = imageKey; return this; }
        public UIInfoBuilder shortDescription(String shortDescription) { _shortDescription = shortDescription; return this; }
        public UIInfoBuilder toolTipText(String toolTipText) { _toolTipText = toolTipText; return this; }
        public UIInfoBuilder visible(boolean visible) { _visible = visible; return this; }
        public UIInfoBuilder expert(boolean expert) { _expert = expert; return this; }
        public UIInfoBuilder uiClassName(String uiClassName) { _uiClassName = uiClassName; return this; }
        public UIInfoBuilder readOnly(boolean readOnly) { _readOnly = readOnly; return this; }
        public UIInfoBuilder isUnique(boolean isUnique) { _isUnique = isUnique; return this; }
        public UIInfoBuilder site(Site site) { _site = site; return this; }
        public UIInfoBuilder onSite(boolean onSite) { _onSite = onSite; return this; }
        public UIInfoBuilder type(String type) { _type = type; return this; }
        public UIInfoBuilder dataObjectClassName(String dataObjectClassName) { _dataObjectClassName = dataObjectClassName; return this; }

        public UIInfo build() { return new UIInfo(this); }
    }

    private final Id _id;
    private final String _name;
    private final String _imageKey;     // used to create an Icon
    private final String _shortDescription;
    private final String _toolTipText;  // default would be no tooltip
    private final boolean _visible;
    private final boolean _expert;
    private final String _uiClassName;
    private final boolean _readOnly;
    private final boolean _isUnique;     // Can the data object appear > once?
    private final Site _site;
    private final boolean _onSite;
    private final String _type;
    private final String _dataObjectClassName;

    /**
     * Default constructor for <code>{@link UIInfo}</code> object.
     */
    public UIInfo(UIInfoBuilder builder) {
        _id                  = builder._id;
        _name                = builder._name;
        _imageKey            = builder._imageKey;
        _shortDescription    = builder._shortDescription;
        _toolTipText         = builder._toolTipText;
        _visible             = builder._visible;
        _expert              = builder._expert;
        _uiClassName         = builder._uiClassName;
        _readOnly            = builder._readOnly;
        _isUnique            = builder._isUnique;
        _site                = builder._site;
        _onSite              = builder._onSite;
        _type                = builder._type;
        _dataObjectClassName = builder._dataObjectClassName;
    }

    public Id getId() { return _id; }

    /** Returns key to retrieve icon used for UI display */
    public String getImageKey() { return _imageKey; }

    /** Returns name used for UI display */
    public String getDisplayName() {
        return _name;
    }

    /** Returns whether this item should be visible to SP viewer. */
    public boolean isVisible() {
        return _visible;
    }

    /** Returns a short description of the item. */
    public String getShortDescription() {
        return _shortDescription;
    }

    /** Returns class name used for UI */
    public String getUIClassName() {
        return _uiClassName;
    }

    /** Returns the site, or null, if no site was specified. */
    public Site getSite() {
        return _site;
    }

    /** Returns true if item is only allowed on site. */
    public boolean isOnSite() {
        return _onSite;
    }

    /** Returns the read-only state of this component */
    public boolean isReadOnly() {
        return _readOnly;
    }

    /**
     * Returns tooltip text of the item.
     * The UI may pop up this tooltip over the item.
     */
    public String getToolTipText() {
        return _toolTipText;
    }

    /** Returns whether this item is considered an expert item. */
    public boolean isExpert() {
        return _expert;
    }

    /** Returns whether this item can be in it's scope more than once */
    public boolean isUnique() {
        return _isUnique;
    }

    /** Returns class name of the associated data object */
    public String getDataObjectClassName() {
        return _dataObjectClassName;
    }

    /** Returns the type of the node (See type constants defined in this class) */
    public String getType() {
        return _type;
    }

    /** Returns the SPType of the associated data object */
    public SPComponentType getSPType() {
        try {
            Class c = Class.forName(_dataObjectClassName);
            Field field = c.getField("SP_TYPE");
            return (SPComponentType) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString());
        }
    }

    public String toString() {
        String s = "UIInfo " + getDisplayName();
        if (getDisplayName() != null && !(getDisplayName().equals(""))) {
            s += " - " + getShortDescription();
        }
        return s;
    }

    /**
     * Diagnostic routine to do a full dump of the contents of a UIInfo.
     */
    public String dump() {
        StringBuilder b = new StringBuilder();
        b.append("     Display Name: ").append(getDisplayName()).append("\n");
        b.append("Data Object Class: ").append(getDataObjectClassName()).append("\n");
        b.append("             Type: ").append(getType()).append("\n");
        b.append("        Image Key: ").append(getImageKey()).append("\n");
        b.append("        isVisible: ").append(isVisible()).append("\n");
        b.append("Short Description: ").append(getShortDescription()).append("\n");
        b.append("    UI Class Name: ").append(getUIClassName()).append("\n");
        b.append("    Tool Tip Text: ").append(getToolTipText()).append("\n");
        b.append("         isExpert: ").append(isExpert()).append("\n");
        b.append("       isReadOnly: ").append(isReadOnly()).append("\n");
        b.append("         isUnique: ").append(isUnique()).append("\n");
        b.append("          onSite:").append(isOnSite()).append("\n");
        if (_site != null) {
            b.append("         site: ").append(getSite()).append("\n");
        }
        return b.toString();
    }

    /** Returns whether this UIInfo has an associated UI */
    public boolean hasUI() {
        return (_uiClassName != null);
    }
}
