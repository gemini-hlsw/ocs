package edu.gemini.itc.nifs;

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

    public GratingOptics(String directory, String prefix, String gratingName,
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
        final String file = directory + prefix + "gratings" + Instrument.getSuffix();
        _resolvingPowerArray = new ArrayList<>();
        _gratingNameArray = new ArrayList<>();
        _blazeArray = new ArrayList<>();
        _resolutionArray = new ArrayList<>();
        _dispersionArray = new ArrayList<>();
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
        return _centralWavelength - (_dispersionArray.get(getGratingNumber()) * _detectorPixels / 2);
    }

    public double getEnd() {
        return _centralWavelength + (_dispersionArray.get(getGratingNumber())* _detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return _centralWavelength;
    }

    public double getPixelWidth() {
        return _dispersionArray.get(getGratingNumber()) * _spectralBinning;
    }


    public int getGratingNumber() {
        int grating_num = 0;

        if (_gratingName.equals(NifsParameters.Z_G5602)) {
            grating_num = NifsParameters.Z_G5602_N;
        } else if (_gratingName.equals(NifsParameters.J_G5603)) {
            grating_num = NifsParameters.J_G5603_N;
        } else if (_gratingName.equals(NifsParameters.H_G5604)) {
            grating_num = NifsParameters.H_G5604_N;
        } else if (_gratingName.equals(NifsParameters.K_G5605)) {
            grating_num = NifsParameters.K_G5605_N;
        } else if (_gratingName.equals(NifsParameters.KS_G5606)) {
            grating_num = NifsParameters.KS_G5606_N;
        } else if (_gratingName.equals(NifsParameters.KL_G5607)) {
            grating_num = NifsParameters.KL_G5607_N;
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
