//
// $
//

package edu.gemini.spModel.inst;

import edu.gemini.spModel.guide.GuideProbe;

/**
 * An interface implemented by instruments that support electronic offsetting.
 * All instruments that support electronic offsetting must have an associated
 * on-instrument guide probe that is used
 */
public interface ElectronicOffsetProvider {
    /**
     * Determines whether electronic offsetting is to be used.
     * @return <code>true</code> if electronic offsetting is to be used,
     * <code>false</code> otherwise
     */
    boolean getUseElectronicOffsetting();

    /**
     * Sets the electronic offsetting property.
     * @param useElectronicOffsetting whether to use electronic offsetting
     */
    void setUseElectronicOffsetting(boolean useElectronicOffsetting);

    /**
     * Gets the OIWFS guide probe to be used to do the electronic offsetting.
     * @return associated OIWFS guider
     */
    GuideProbe getElectronicOffsetGuider();
}
