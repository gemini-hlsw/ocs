package edu.gemini.itc.altair;

import edu.gemini.itc.base.*;

/**
 * The AltairBackgroundVisitor class is designed to adjust the SED for the
 * background given off by altair.
 */
public class AltairBackgroundVisitor implements SampledSpectrumVisitor {

    private DefaultArraySpectrum _altairBack = null;

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
        final DefaultArraySpectrum.Interpolator back = _altairBack.interpolator();
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, back.at(sed.getX(i)) + sed.getY(i));
        }
    }


    public String toString() {
        return "AltairBackgroundVisitor ";
    }
}
