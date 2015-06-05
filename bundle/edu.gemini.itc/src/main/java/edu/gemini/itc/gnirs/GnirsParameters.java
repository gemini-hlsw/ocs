package edu.gemini.itc.gnirs;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;

/**
 * This class holds the information from the Gnirs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GnirsParameters implements InstrumentDetails {

    // Data members
    private final Disperser     grating;
    private final PixelScale    pixelScale;
    private final ReadMode      readMode;
    private final Wavelength    instrumentCentralWavelength;
    private final SlitWidth     slitWidth;
    private final CrossDispersed xDisp;

    /**
     * Constructs a GnirsParameters.
     */
    public GnirsParameters(final PixelScale     pixelScale,
                           final Disperser      grating,
                           final ReadMode       readMode,
                           final CrossDispersed xDisp,
                           final Wavelength     instrumentCentralWavelength,
                           final SlitWidth      slitWidth) {
        this.grating                        = grating;
        this.pixelScale                     = pixelScale;
        this.xDisp                          = xDisp;
        this.readMode                       = readMode;
        this.instrumentCentralWavelength    = instrumentCentralWavelength;
        this.slitWidth                      = slitWidth;
    }

    public Disperser getGrating() {
        return grating;
    }

    public PixelScale getPixelScale() {
        return pixelScale;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public Wavelength getCentralWavelength() {
        return instrumentCentralWavelength;
    }

    public SlitWidth getSlitWidth() {
        return slitWidth;
    }

    public CrossDispersed getCrossDispersed() {
        return xDisp;
    }


}
