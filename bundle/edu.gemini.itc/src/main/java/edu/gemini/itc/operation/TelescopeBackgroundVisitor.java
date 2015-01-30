package edu.gemini.itc.operation;

import edu.gemini.itc.shared.*;

/**
 * The TelescopeBackgroundVisitor class is designed to adjust the SED for the
 * background given off by the telescope.
 */
public class TelescopeBackgroundVisitor implements SampledSpectrumVisitor {

    private String _setup;
    private final ArraySpectrum _telescopeBack;

    /**
     * Constructs TelescopeBackgroundVisitor with specified port and coating.
     * We will use a different background file for different
     * ports and coatings.
     */
    public TelescopeBackgroundVisitor(String coating, String port, String site, String wavelenRange) throws Exception {

        String _fullBackgroundResource;
        if (!wavelenRange.equals(ITCConstants.VISIBLE)) {

            final String filenameBase = "/HI-Res/" + site + wavelenRange + ITCConstants.TELESCOPE_BACKGROUND_LIB + "/"
                    + ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;

            if (port.equals("up")) {
                _setup = "_2";
                if (coating.equals("aluminium"))
                    _setup = _setup + "al";
                else if (coating.equals("silver"))
                    _setup = _setup + "ag";
            } else if (port.equals("side") && coating.equals("silver")) {
                _setup = "_3ag";
            } else if (port.equals("side") && coating.equals("aluminium")) {
                _setup = "_2al+1ag";
            }

            _fullBackgroundResource = filenameBase + _setup + ITCConstants.DATA_SUFFIX;

        } else {
            String filenameBase = ITCConstants.TELESCOPE_BACKGROUND_FILENAME_BASE;
            if (port.equals("up")) {
                _setup = "_2";
                if (coating.equals("aluminium"))
                    _setup = _setup + "al_ph";
                else if (coating.equals("silver"))
                    _setup = _setup + "ag_ph";
            } else if (port.equals("side") && coating.equals("silver")) {
                _setup = "_3ag_ph";
            } else if (port.equals("side") && coating.equals("aluminium")) {
                _setup = "_2al+1ag_ph";
            } else if (port.equals("upGS") && coating.equals("silver")) {
                filenameBase = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_ag1_al1";
            } else if (port.equals("upGS") && coating.equals("aluminium")) {
                filenameBase = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_al2";
            } else if (port.equals("sideGS") && coating.equals("silver")) {
                filenameBase = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_ag1_al2";
            } else if (port.equals("sideGS") && coating.equals("aluminium")) {
                filenameBase = ITCConstants.GS_TELESCOPE_BACKGROUND_FILENAME_BASE;
                _setup = "_al3";
            }

            _fullBackgroundResource = ITCConstants.TELESCOPE_BACKGROUND_LIB + "/" +
                    filenameBase
                    + _setup +
                    ITCConstants.DATA_SUFFIX;
        }

        _telescopeBack = new DefaultArraySpectrum(_fullBackgroundResource);


    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) throws Exception {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, _telescopeBack.getY(sed.getX(i)) + sed.getY(i));
        }
    }

    public String toString() {
        return "TelescopeBackgroundVisitor using setup " + _setup;
    }
}
