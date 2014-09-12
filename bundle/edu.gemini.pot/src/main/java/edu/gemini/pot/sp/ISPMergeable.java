package edu.gemini.pot.sp;

import edu.gemini.spModel.data.ISPDataObject;

/**
 * Implemented by data objects that be merged.
 */
public interface ISPMergeable<T extends ISPDataObject> {

    /**
     * Merges this data object with <code>that</code> data object if possible.
     * @return merged data object or <code>null</code> if not possible to
     * merge
     */
    T mergeOrNull(T that);
}
