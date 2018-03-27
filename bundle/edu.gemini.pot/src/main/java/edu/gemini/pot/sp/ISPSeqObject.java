package edu.gemini.pot.sp;

import edu.gemini.spModel.data.ISPDataObject;

/**
 * Marks a data object that is involved in sequence calculation and provides
 * a method to determine how many steps the node contributes on its own.
 */
public interface ISPSeqObject extends ISPDataObject {
    int getStepCount();
}
