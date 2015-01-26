package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.*;

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

    public GratingOptics(String directory, String prefix, String gratingName, Detector detector,
                         String stringSlitWidth,
                         double centralWavelength,
                         int detectorPixels,
                         int spectralBinning)
            throws Exception {

        super(directory + prefix +
                gratingName + Instrument.getSuffix());

        _spectralBinning = spectralBinning;

        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;
        _gratingName = gratingName;

        //New read of Grating Properties
        final String detectorPrefix = detector.toString().contains("EEV") ? "eev_" : ""; // REL-477
        final String file = directory + prefix + detectorPrefix + "gratings" + Instrument.getSuffix();
        _resolvingPowerArray = new ArrayList<>();
        _gratingNameArray = new ArrayList<>();
        _blazeArray = new ArrayList<>();
        _resolutionArray = new ArrayList<>();
        _dispersionArray = new ArrayList<>();

        try (final Scanner scan = DatFile.scan(file)) {
            while (scan.hasNext()) {
                _gratingNameArray.add(scan.next());
                _blazeArray.add(scan.nextInt());
                _resolvingPowerArray.add(scan.nextInt());
                _resolutionArray.add(scan.nextDouble());
                _dispersionArray.add(scan.nextDouble());
            }
        }
    }


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

    public int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(GmosParameters.B1200_G5301)) {
            grating_num = GmosParameters.B1200;
        } else if (_gratingName.equals(GmosParameters.R831_G5302)) {
            grating_num = GmosParameters.R831;
        } else if (_gratingName.equals(GmosParameters.B600_G5303)) {
            grating_num = GmosParameters.B600;
        } else if (_gratingName.equals(GmosParameters.R600_G5304)) {
            grating_num = GmosParameters.R600;
        } else if (_gratingName.equals(GmosParameters.R400_G5305)) {
            grating_num = GmosParameters.R400;
        } else if (_gratingName.equals(GmosParameters.R150_G5306)) {
            grating_num = GmosParameters.R150;
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
