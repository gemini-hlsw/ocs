package edu.gemini.itc.niri;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.DatFile;
import edu.gemini.itc.shared.TransmissionElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This represents the transmission of the Grism optics.
 */
public class GrismOptics extends TransmissionElement {


    private List<Double> _spectralCoverageArray;
    private List<Double> _spectralPixelWidthArray;
    private List<Integer> _resolvingPowerArray;
    private String _grismName;

    public GrismOptics(String directory, String grismName,
                       String cameraName, String focalPlaneMaskOffset,
                       String stringSlitWidth)
            throws Exception {

        super(directory + Niri.getPrefix() +
                grismName + "_" + cameraName + Instrument.getSuffix());

        _grismName = grismName;

        //New read of Grism Resolving Power
        _resolvingPowerArray = new ArrayList<>();
        final String grismFile = directory + Niri.getPrefix() + "grism-resolution-" + stringSlitWidth + "_" + cameraName + Instrument.getSuffix();
        try (final Scanner scan = DatFile.scan(grismFile)) {
            while (scan.hasNext()) {
                _resolvingPowerArray.add(scan.nextInt());
            }
        }

        final String coverageFile = directory + Niri.getPrefix() + "grism-coverage-" + focalPlaneMaskOffset + Instrument.getSuffix();
        _spectralCoverageArray = new ArrayList<>();
        _spectralPixelWidthArray = new ArrayList<>();
        try (final Scanner grismCoverage =  DatFile.scan(coverageFile)) {
            while (grismCoverage.hasNext()) {
                _spectralCoverageArray.add(grismCoverage.nextDouble());
                _spectralCoverageArray.add(grismCoverage.nextDouble());
                _spectralPixelWidthArray.add(grismCoverage.nextDouble());
            }
        }
    }

    public double getStart() {
        return _spectralCoverageArray.get(getGrismNumber() * 2);
    }

    public double getEnd() {
        return _spectralCoverageArray.get(getGrismNumber() * 2 + 1);
    }

    public double getEffectiveWavelength() {
        return (getStart() + getEnd()) / 2;
    }

    public double getPixelWidth() {
        return _spectralPixelWidthArray.get(getGrismNumber());
    }

    public int getGrismNumber() {
        int grism_num = 0;

        if (_grismName.equals(NiriParameters.JGRISM)) {
            grism_num = NiriParameters.J;
        } else if (_grismName.equals(NiriParameters.HGRISM)) {
            grism_num = NiriParameters.H;
        } else if (_grismName.equals(NiriParameters.KGRISM)) {
            grism_num = NiriParameters.K;
        } else if (_grismName.equals(NiriParameters.LGRISM)) {
            grism_num = NiriParameters.L;
        } else if (_grismName.equals(NiriParameters.MGRISM)) {
            grism_num = NiriParameters.M;
        }
        return grism_num;
    }

    public double getGrismResolution() {
        return _resolvingPowerArray.get(getGrismNumber());
    }


    public String toString() {
        return "Grism Optics: " + _grismName;
    }

}
