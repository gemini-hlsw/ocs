// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SPNote.java 20842 2009-07-07 22:05:45Z swalker $
//
package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.CompressedString;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

/**
 * The Note item.  Notes are arbitrary text information that may be
 * entered at any level of the hierarchy.
 */
public class SPNote extends AbstractDataObject {
    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.INFO_NOTE;

    public static final ISPNodeInitializer<ISPObsComponent, SPNote> NI =
        new SimpleNodeInitializer<>(SP_TYPE, () -> new SPNote());

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * The name of the text property in the Note SpItem.
     */
    public static final String ATTR_NOTE_TEXT = "note";

    public static final String NOTE_TEXT_PROP = "NoteText";
    public static final String INGROUP_PROP = "inGroup";

    // The note itself.
    private CompressedString _text;

    // A String indicating if the note is in a group or not. Not in group initially.
    // The value is the name of the group the note is in.
    // Note: this is no longer used, but we need to keep it around for compatibility
    // when importing older programs.
    private String _groupName = null;

    /**
     * Default constructor.
     */
    public SPNote() {
        super(SP_TYPE);
    }

    /** for use by subclasses */
    SPNote(SPComponentType type) {
        super(type);
    }

    @Override
    public Object clone() {
        SPNote res = (SPNote) super.clone();

        // FR 10600: When cloning, be sure to create a new CompressedString.
        // We don't want two instances to share the same reference!
        if (res._text != null) res._text = new CompressedString(res._text);

        return res;
    }

    /**
     * Set the note text.
     */
    public void setNote(String newValue) {
        String oldValue = getNote();
        _setNote(newValue);
        newValue = getNote();

        if (!oldValue.equals(newValue)) {
            firePropertyChange(NOTE_TEXT_PROP, oldValue, newValue);
        }
    }

    // Set the note text without firing an event
    private void _setNote(String newValue) {
        if (_text == null) {
            if (newValue == null) return;
            _text = new CompressedString();
        }
        _text.set(newValue);
    }

    /**
     * Get the note text.
     */
    public String getNote() {
        if (_text == null) return EMPTY_STRING;
        String res = _text.get();
        return (res == null) ? EMPTY_STRING : res;
    }

    /**
     * Set the name of the group to which this observation belongs
     * (null for no group).
     */
    public void setGroup(String groupName) {
        _groupName = groupName;
    }

    /**
     * Return the name of the group to which this observation belongs, or
     * null if it does not belong to a group.
     */
    public String getGroup() {
        return _groupName;
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, NOTE_TEXT_PROP, getNote());
        if (_groupName != null) {
            Pio.addParam(factory, paramSet, INGROUP_PROP, getGroup());
        }

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, NOTE_TEXT_PROP);
        if (v != null) {
            _setNote(v);
        }
        v = Pio.getValue(paramSet, INGROUP_PROP);
        if (v != null) {
            setGroup(v);
        }
    }
}
