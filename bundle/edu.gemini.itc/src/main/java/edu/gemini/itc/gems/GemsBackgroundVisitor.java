package edu.gemini.itc.gems;

import edu.gemini.itc.base.*;

/**
 * The GemsBackgroundVisitor class is designed to adjust the SED for the
 * background given off by gems.
 */
public class GemsBackgroundVisitor implements SampledSpectrumVisitor {

    private ArraySpectrum _gemsBack = null;

    /**
     * Constructs GemsBackgroundVisitor.
     */
    public GemsBackgroundVisitor() {

        _gemsBack = new DefaultArraySpectrum(
                Gems.GEMS_LIB + "/" +
                        Gems.GEMS_PREFIX +
                        Gems.GEMS_BACKGROUND_FILENAME +
                        ITCConstants.DATA_SUFFIX);
    }


    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, _gemsBack.getY(sed.getX(i)) + sed.getY(i));
        }
    }


    public String toString() {
        return "GemsBackgroundVisitor ";
    }
}
