package edu.gemini.itc.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement {

    protected final List<Integer> _resolvingPowerArray;
    protected final List<Double> _dispersionArray;
    protected final List<Integer> _blazeArray;
    protected final List<Double> _resolutionArray;
    protected final String _gratingName;
    protected final double _centralWavelength;
    protected final int _detectorPixels;
    protected final int _spectralBinning;

    public GratingOptics(final String directory,
                         final String gratingName,
                         final String gratingsName,
                         final double centralWavelength,
                         final int detectorPixels,
                         final int spectralBinning) throws Exception {

        super(directory + gratingName + Instrument.getSuffix());

        _gratingName = gratingName;
        _spectralBinning = spectralBinning;
        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;

        final String file = directory + gratingsName + Instrument.getSuffix();
        try (final Scanner scan = DatFile.scan(file)) {
            _resolvingPowerArray = new ArrayList<>();
            _blazeArray = new ArrayList<>();
            _resolutionArray = new ArrayList<>();
            _dispersionArray = new ArrayList<>();

            while (scan.hasNext()) {
                String skipName = scan.next(); // skip grating name TODO: should we use this name for lookup in getGratingNum (instead of using the hardcoded numbers?)
                _blazeArray.add(scan.nextInt());
                _resolvingPowerArray.add(scan.nextInt());
                _resolutionArray.add(scan.nextDouble());
                _dispersionArray.add(scan.nextDouble());
            }
        }
    }

    protected abstract int getGratingNumber();

    public double getStart() {
        return _centralWavelength - (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2);
    }

    public double getEnd() {
        return _centralWavelength + (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return _centralWavelength;
    }

    public double getPixelWidth() {
        return _dispersionArray.get(getGratingNumber()) * _spectralBinning;

    }

    public double getGratingResolution() {
        return _resolvingPowerArray.get(getGratingNumber());
    }

    public double getGratingBlaze() {
        return _blazeArray.get(getGratingNumber());
    }

    public double getGratingDispersion_nm() {
        return _resolutionArray.get(getGratingNumber());
    }

    public double getGratingDispersion_nmppix() {
        return _dispersionArray.get(getGratingNumber()) * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + _gratingName;
    }

}
