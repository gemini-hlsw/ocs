package edu.gemini.itc.altair;

import edu.gemini.itc.shared.*;

/**
 * The AltairBackgroundVisitor class is designed to adjust the SED for the
 * background given off by altair.
 */
public class AltairBackgroundVisitor implements SampledSpectrumVisitor {

    private ArraySpectrum _altairBack = null;

    /**
     * Constructs AltairBackgroundVisitor.
     */
    public AltairBackgroundVisitor() {

        _altairBack = new DefaultArraySpectrum(
                Altair.ALTAIR_LIB + "/" +
                        Altair.ALTAIR_PREFIX +
                        Altair.ALTAIR_BACKGROUND_FILENAME +
                        ITCConstants.DATA_SUFFIX);
    }


    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, _altairBack.getY(sed.getX(i)) + sed.getY(i));
        }
    }


    public String toString() {
        return "AltairBackgroundVisitor ";
    }
}
