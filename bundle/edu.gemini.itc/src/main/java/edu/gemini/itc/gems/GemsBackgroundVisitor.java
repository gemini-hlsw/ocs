package edu.gemini.itc.gems;

import edu.gemini.itc.base.*;

/**
 * The GemsBackgroundVisitor class is designed to adjust the SED for the
 * background given off by gems.
 */
public class GemsBackgroundVisitor implements SampledSpectrumVisitor {

    private DefaultArraySpectrum _gemsBack = null;

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
        final DefaultArraySpectrum.Interpolator back = _gemsBack.interpolator();
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, back.at(sed.getX(i)) + sed.getY(i));
        }
    }


    public String toString() {
        return "GemsBackgroundVisitor ";
    }
}
