package edu.gemini.itc.shared;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement {

    protected final String gratingName;
    protected final double centralWavelength;
    protected final int detectorPixels;
    protected final int _spectralBinning;
    protected final DatFile.Grating[] data;

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

    protected abstract int getGratingNumber();

    public double getStart() {
        return centralWavelength - (data[getGratingNumber()].dispersion() * detectorPixels / 2);
    }

    public double getEnd() {
        return centralWavelength + (data[getGratingNumber()].dispersion() * detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return centralWavelength;
    }

    public double getPixelWidth() {
        return data[getGratingNumber()].dispersion() * _spectralBinning;

    }

    public double getGratingResolution() {
        return data[getGratingNumber()].resolvingPower();
    }

    public double getGratingBlaze() {
        return data[getGratingNumber()].blaze();
    }

    public double getGratingDispersion_nm() {
        return data[getGratingNumber()].resolution();
    }

    public double getGratingDispersion_nmppix() {
        return data[getGratingNumber()].dispersion() * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + gratingName;
    }

}
