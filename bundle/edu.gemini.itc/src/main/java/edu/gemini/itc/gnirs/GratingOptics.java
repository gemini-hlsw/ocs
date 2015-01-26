package edu.gemini.itc.gnirs;

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

    private List<Integer> _resolvingPowerArray;
    private List<Double> _dispersionArray;
    private List<Integer> _blazeArray;
    private List<Double> _resolutionArray;
    private List<String> _gratingNameArray;
    private String _gratingName;
    private double _centralWavelength;
    private int _detectorPixels;
    private int _spectralBinning;

    public GratingOptics(String directory, String prefix, String gratingName,
                         String stringSlitWidth,
                         double centralWavelength,
                         int detectorPixels,
                         int spectralBinning)
            throws Exception {

        super(directory + Gnirs.getPrefix() +
                gratingName + Instrument.getSuffix());

        _spectralBinning = spectralBinning;
        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;
        _gratingName = gratingName;

        //New read of Grating Proporties
        final String file = directory + prefix + "gratings" + Instrument.getSuffix();
        try (final Scanner scan = DatFile.scan(file)) {
            _resolvingPowerArray = new ArrayList<>();
            _gratingNameArray = new ArrayList<>();
            _blazeArray = new ArrayList<>();
            _resolutionArray = new ArrayList<>();
            _dispersionArray = new ArrayList<>();

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
        return _centralWavelength - (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2);//*_spectralBinning;
    }

    public double getEnd() {
        return _centralWavelength + (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2);//*_spectralBinning;
    }

    public double getEffectiveWavelength() {
        return _centralWavelength;
    }

    public double getPixelWidth() {
        return _dispersionArray.get(getGratingNumber()) * _spectralBinning;

    }


    public int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(GnirsParameters.G10)) {
            grating_num = GnirsParameters.G10_N;
        } else if (_gratingName.equals(GnirsParameters.G32)) {
            grating_num = GnirsParameters.G32_N;
        } else if (_gratingName.equals(GnirsParameters.G110)) {
            grating_num = GnirsParameters.G110_N;
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
