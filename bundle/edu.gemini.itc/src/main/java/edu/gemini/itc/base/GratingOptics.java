package edu.gemini.itc.base;

import scala.collection.immutable.Map;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement implements Disperser {

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
                         final int spectralBinning) {

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

    public double getGratingResolvingPower() {
        System.out.println("resolvingPower, "+ gratingName + "  blaze: " + data.apply(gratingName).resolvingPower());
        return data.apply(gratingName).resolvingPower();
    }

    public double getGratingBlaze() {
        System.out.println("getGratingBlaze, "+ gratingName + "  blaze: " + data.apply(gratingName).blaze());
        return data.apply(gratingName).blaze();
    }

    public double resolutionHalfArcsecSlit() {
        System.out.println("resolutionHalfArcsecSlit, "+ gratingName + "  resolution: " + data.apply(gratingName).resolution());
        return data.apply(gratingName).resolution();
    }

    public double dispersion() {
        return data.apply(gratingName).dispersion() * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + gratingName;
    }

}
