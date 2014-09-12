package edu.gemini.spModel.conflict;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;

/**
 *
 */
public class ConflictFolder extends AbstractDataObject {
    public static final String DEFAULT_TITLE   = "Conflicts";
    public static final String VERSION         = "2013A-1";

    public static final SPComponentType SP_TYPE =
            SPComponentType.CONFLICT_FOLDER;

    public ConflictFolder() {
        setTitle(DEFAULT_TITLE);
        setType(SP_TYPE);
        setVersion(VERSION);
    }
}
