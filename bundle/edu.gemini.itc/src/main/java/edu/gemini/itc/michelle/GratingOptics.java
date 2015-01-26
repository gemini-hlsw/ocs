package edu.gemini.itc.michelle;

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
    private final List <Double>_dispersionArray;
    private final List<Integer> _blazeArray;
    private final List <Double> _resolutionArray;
    private final List <String> _gratingNameArray;
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

        super(directory + Michelle.getPrefix() +
                gratingName + Instrument.getSuffix());

        _spectralBinning = spectralBinning;

        _detectorPixels = detectorPixels;
        _centralWavelength = centralWavelength;
        _gratingName = gratingName;

        _resolvingPowerArray = new ArrayList<>();
        _gratingNameArray = new ArrayList<>();
        _blazeArray = new ArrayList<>();
        _resolutionArray = new ArrayList<>();
        _dispersionArray = new ArrayList<>();

        //New read of Grating Properties
        final String file = directory + Michelle.getPrefix() + "gratings" + Instrument.getSuffix();
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

        if (_gratingName.equals(MichelleParameters.LOW_N)) {
            grating_num = MichelleParameters.LOWN;
        } else if (_gratingName.equals(MichelleParameters.LOW_Q)) {
            grating_num = MichelleParameters.LOWQ;
        } else if (_gratingName.equals(MichelleParameters.MED_N1)) {
            grating_num = MichelleParameters.MEDN1;
        } else if (_gratingName.equals(MichelleParameters.MED_N2)) {
            grating_num = MichelleParameters.MEDN2;
        } else if (_gratingName.equals(MichelleParameters.ECHELLE_N)) {
            grating_num = MichelleParameters.ECHELLEN;
        } else if (_gratingName.equals(MichelleParameters.ECHELLE_Q)) {
            grating_num = MichelleParameters.ECHELLEQ;
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
