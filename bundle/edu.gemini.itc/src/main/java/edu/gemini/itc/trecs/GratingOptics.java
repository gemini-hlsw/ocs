package edu.gemini.itc.trecs;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.TransmissionElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This represents the transmission and properties of the Grating optics.
 */
public class GratingOptics extends TransmissionElement {

    private final List<Integer> _resolvingPowerArray;
    private final List<Double> _dispersionArray;
    private final List<Integer> _blazeArray;
    private final List<Double> _resolutionArray;
    private final List<String> _gratingNameArray;
    private final String _gratingName;
    private final double _centralWavelength;
    private final int _detectorPixels;
    private final int _spectralBinning;

    public GratingOptics(String directory, String gratingName,
                         String stringSlitWidth,
                         double centralWavelength,
                         int detectorPixels,
                         int spectralBinning)
            throws Exception {

        super(directory + TRecs.getPrefix() +
                gratingName + Instrument.getSuffix());

        _spectralBinning = spectralBinning;
        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;
        _gratingName = gratingName;

        //New read of Grating Proporties
        _resolvingPowerArray = new ArrayList<>();
        _gratingNameArray = new ArrayList<>();
        _blazeArray = new ArrayList<>();
        _resolutionArray = new ArrayList<>();
        _dispersionArray = new ArrayList<>();

        final String file = directory + TRecs.getPrefix() + "gratings" + Instrument.getSuffix();
        try (final Scanner grismProperties = DatFile.scan(file)) {
            while (grismProperties.hasNext()) {
                _gratingNameArray.add(grismProperties.next());
                _blazeArray.add(grismProperties.nextInt());
                _resolvingPowerArray.add(grismProperties.nextInt());
                _resolutionArray.add(grismProperties.nextDouble());
                _dispersionArray.add(grismProperties.nextDouble());
            }
        }

    }


    public double getStart() {
        return _centralWavelength - (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2) * _spectralBinning;
    }

    public double getEnd() {
        return _centralWavelength + (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2) * _spectralBinning;
    }

    public double getEffectiveWavelength() {
        return _centralWavelength;
    }

    public double getPixelWidth() {
        return _dispersionArray.get(getGratingNumber()) * _spectralBinning;

    }

    public int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(TRecsParameters.LORES10_G5401)) {
            grating_num = TRecsParameters.LORES10;
        } else if (_gratingName.equals(TRecsParameters.LORES20_G5402)) {
            grating_num = TRecsParameters.LORES20;
        } else if (_gratingName.equals(TRecsParameters.HIRES10_G5403)) {
            grating_num = TRecsParameters.HIRES10;
        }
        return grating_num;
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
