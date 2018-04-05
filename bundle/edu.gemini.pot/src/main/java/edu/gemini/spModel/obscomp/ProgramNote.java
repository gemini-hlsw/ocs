/**
 * $Id$
 */

package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPStaffOnlyFieldProtected;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.Encrypted;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.util.ObjectUtil;

/**
 * Private (Gemini-internal) note.
 */
public final class ProgramNote extends SPNote implements Encrypted, ISPStaffOnlyFieldProtected {
    public static final SPComponentType SP_TYPE = SPComponentType.INFO_PROGRAMNOTE;
    private static final long serialVersionUID = 1L;

    public static final ISPNodeInitializer<ISPObsComponent, SPNote> NI =
        new SimpleNodeInitializer<>(SP_TYPE, () -> new ProgramNote());

    public ProgramNote() {
        super(SP_TYPE);
    }

    @Override public boolean staffOnlyFieldsEqual(ISPDataObject to) {
        final ProgramNote that = (ProgramNote) to;
        return ObjectUtil.equals(getTitle(), that.getTitle()) &&
               ObjectUtil.equals(getNote(), that.getNote());
    }

    @Override public boolean staffOnlyFieldsDefaulted() {
        return staffOnlyFieldsEqual(new ProgramNote());
    }

    @Override public void setStaffOnlyFieldsFrom(ISPDataObject to) {
        final ProgramNote that = (ProgramNote) to;
        setTitle(that.getTitle());
        setNote(that.getNote());
    }

    @Override public void resetStaffOnlyFieldsToDefaults() {
        setStaffOnlyFieldsFrom(new ProgramNote());
    }
}
