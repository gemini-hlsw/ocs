package edu.gemini.itc.operation;

import edu.gemini.itc.shared.*;

import java.util.ArrayList;
import java.util.List;

/**
 * For Gmos Spectroscopy the spectrum will be spread across 3 CCD's
 * The Three CCD's are not continous.  The Gaps will be represented in
 * a file read in.
 */
public class DetectorsTransmissionVisitor implements SampledSpectrumVisitor {
    private final int spectralBinning;
    private VisitableSampledSpectrum detectorsTransmissionValues;
    private List<Integer> detectorCcdIndexes;

    public DetectorsTransmissionVisitor(int spectralBinning, String filename) throws Exception {
        this.spectralBinning = spectralBinning;
        final double[][] data = DatFile.arrays().apply(filename);
        initialize(data);
    }

    /**
     * This method performs the Detectors transmision manipulation on the SED.
     */
    public void visit(SampledSpectrum sed) throws Exception {
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i,
                    sed.getY(i * sed.getSampling() + sed.getStart()) *
                            detectorsTransmissionValues.getY(i *
                                    detectorsTransmissionValues.getSampling() +
                                    detectorsTransmissionValues.getStart()));
        }
    }

    private void initialize(final double[][] data) throws Exception {
        // need to sample the file at regular interval pixel values
        int finalPixelValue = (int) data[0][data[0].length - 1];
        double[] pixelData = new double[finalPixelValue];
        pixelData[0] = 1.0; // XXX previously defaulted to 0
        int j = 0;
        detectorCcdIndexes = new ArrayList<>(6);
        detectorCcdIndexes.add(0);
        int prevY = 1;
        for (int i = 1; i < finalPixelValue; i++) {
            int x = (int) data[0][j];
            int y = (int) data[1][j];
            if (prevY != y) {
                detectorCcdIndexes.add(x - prevY - 1);
                prevY = y;
            }
            if (x == i) {
                pixelData[i] = y;
                j++;
            } else {
                // Apply the last transmission pixel value and go on without incrementing
                pixelData[i] = data[1][j - 1];
            }
        }
        detectorCcdIndexes.add(finalPixelValue - 1);

        detectorsTransmissionValues = SEDFactory.getSED(pixelData, 1, 1);

        ResampleVisitor detectorResample = new ResampleVisitor(
                detectorsTransmissionValues.getStart(),
                detectorsTransmissionValues.getLength(),
                spectralBinning);
        detectorsTransmissionValues.accept(detectorResample);

    }

    /**
     * Returns the first pixel index for the given detector CCD index (when there are multiple detectors
     * separated by gaps).
     */
    public int getDetectorCcdStartIndex(int detectorIndex) {
        return detectorCcdIndexes.get(detectorIndex * 2) / spectralBinning;
    }

    /**
     * Returns the last pixel index for the given detector CCD index (when there are multiple detectors
     * separated by gaps).
     */
    public int getDetectorCcdEndIndex(int detectorIndex, int detectorCount) {
        if (detectorCount == 1) {
            // REL-478: For EEV use only one with the whole range
            return detectorCcdIndexes.get(detectorCcdIndexes.size() - 1) / spectralBinning;
        }
        return detectorCcdIndexes.get(detectorIndex * 2 + 1) / spectralBinning;
    }

    /**
     * @return Human-readable representation of this class.
     */
    public String toString() {
        return "Application of the Detector Gaps Transmission";
    }
}
