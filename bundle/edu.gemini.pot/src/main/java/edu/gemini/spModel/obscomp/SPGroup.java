/**
 * $Id: SPGroup.java 44536 2012-04-15 16:42:55Z swalker $
 */

package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.ISPGroup;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.type.DisplayableSpType;

/**
 * Defines a data object that holds the name of a science program group.
 */
public class SPGroup extends AbstractDataObject {
    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.GROUP_GROUP;

    public static final ISPNodeInitializer<ISPGroup, SPGroup> NI =
            new SimpleNodeInitializer<>(SP_TYPE, () -> new SPGroup("Group"));

    // for serialization
    private static final long serialVersionUID = 2L;

    public static final String LIBRARY_ID_PROP = "libraryId";
    public static final String PROP_GROUP_TYPE = "GroupType";

    public enum GroupType implements DisplayableSpType {

    	TYPE_SCHEDULING("Scheduling Group"),
    	TYPE_FOLDER("Organizational Folder"),
    	;

    	public static final GroupType DEFAULT = TYPE_SCHEDULING;

    	final String displayName;

		private GroupType(String displayName) {
			this.displayName = displayName;
		}

    	public String displayValue() {
    		return displayName;
    	}

    }

    private GroupType groupType = GroupType.DEFAULT;
    private String libraryId;

    /**
     * Constructor.
     */
    public SPGroup(String groupName) {
        super(SP_TYPE);
        setTitle(groupName);
    }

    /**
     * Set the group name.
     */
    public void setGroup(String newValue) {
        if (newValue == null) newValue = EMPTY_STRING;
        setTitle(newValue);
    }

    /**
     * Get the group text.
     */
    public String getGroup() {
        return getTitle();
    }

    /**
     * Set the group type.
     * @param groupType, not null
     */
    public void setGroupType(GroupType groupType) {
    	if (groupType == null) throw new IllegalArgumentException("Group type may not be null.");
    	GroupType prev = this.groupType;
		this.groupType = groupType;
		firePropertyChange(PROP_GROUP_TYPE, prev, groupType);
	}

    /**
     * Returns the group type.
     * @return a GroupType, never null.
     */
    public GroupType getGroupType() {
		return groupType;
	}

    public String getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(String libraryId) {
        if (libraryId == this.libraryId) return;
        if ((libraryId != null) && libraryId.equals(this.libraryId)) return;
        String prev = this.libraryId;
        this.libraryId = libraryId;
        firePropertyChange(LIBRARY_ID_PROP, prev, libraryId);
    }

	@Override
	public ParamSet getParamSet(PioFactory factory) {
		final ParamSet params = super.getParamSet(factory);
		Pio.addEnumParam(factory, params, PROP_GROUP_TYPE, groupType);
        Pio.addParam(factory, params, LIBRARY_ID_PROP, libraryId);
		return params;
	}

	@Override
	public void setParamSet(ParamSet paramSet) {
		super.setParamSet(paramSet);
		setGroupType(Pio.getEnumValue(paramSet, PROP_GROUP_TYPE, GroupType.DEFAULT));
        setLibraryId(Pio.getValue(paramSet, LIBRARY_ID_PROP, null));
	}

}
