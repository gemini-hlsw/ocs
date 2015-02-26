package edu.gemini.itc.shared;

import scala.collection.immutable.Map;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement {

    protected final String gratingName;
    protected final double centralWavelength;
    protected final int detectorPixels;
    protected final int _spectralBinning;
    protected final Map<String, DatFile.Grating> data;

    public GratingOptics(final String directory,
                         final String gratingName,
                         final String gratingsName,
                         final double centralWavelength,
                         final int detectorPixels,
                         final int spectralBinning) throws Exception {

        super(directory + gratingName + Instrument.getSuffix());

        final String file = directory + gratingsName + Instrument.getSuffix();
        this.data = DatFile.gratings().apply(file);
        this.gratingName = gratingName;
        this._spectralBinning = spectralBinning;
        this.detectorPixels = detectorPixels;
        this.centralWavelength = centralWavelength;
    }

    public double getStart() {
        return centralWavelength - (data.apply(gratingName).dispersion() * detectorPixels / 2);
    }

    public double getEnd() {
        return centralWavelength + (data.apply(gratingName).dispersion() * detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return centralWavelength;
    }

    public double getPixelWidth() {
        return data.apply(gratingName).dispersion() * _spectralBinning;

    }

    public double getGratingResolution() {
        return data.apply(gratingName).resolvingPower();
    }

    public double getGratingBlaze() {
        return data.apply(gratingName).blaze();
    }

    public double getGratingDispersion_nm() {
        return data.apply(gratingName).resolution();
    }

    public double getGratingDispersion_nmppix() {
        return data.apply(gratingName).dispersion() * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + gratingName;
    }

}
