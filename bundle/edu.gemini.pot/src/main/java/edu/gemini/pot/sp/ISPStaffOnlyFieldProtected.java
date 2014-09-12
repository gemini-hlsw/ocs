package edu.gemini.pot.sp;

import edu.gemini.spModel.data.ISPDataObject;

/**
 * Implemented by data objects that have staff-only fields.
 */
public interface ISPStaffOnlyFieldProtected extends ISPDataObject {
    /**
     * Return <code>true</code> if all staff-only fields are the same between
     * this data object and that one. <em>Anything that triggers a <code>false</code>
     * response here had better be reset by
     * {@link #setStaffOnlyFieldsFrom(ISPDataObject)}</em>.
     */
    boolean staffOnlyFieldsEqual(ISPDataObject that);

    /**
     * Return <code>true</code> if all staff-only fields have their default
     * values.
     */
    boolean staffOnlyFieldsDefaulted();

    /**
     * Sets staff-only fields in this data object using the values from
     * <code>that</code>.
     */
    void setStaffOnlyFieldsFrom(ISPDataObject that);

    void resetStaffOnlyFieldsToDefaults();
}
