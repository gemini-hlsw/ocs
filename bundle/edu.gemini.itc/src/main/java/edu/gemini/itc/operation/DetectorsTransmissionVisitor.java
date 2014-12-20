// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SEDFactory;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.TextFileReader;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

/**
 * For Gmos Spectroscopy the spectrum will be spread across 3 CCD's
 * The Three CCD's are not continous.  The Gaps will be represented in
 * a file read in.
 */
public class DetectorsTransmissionVisitor implements SampledSpectrumVisitor {
    private int spectralBinning;
    private VisitableSampledSpectrum detectorsTransmissionValues;
    List<Integer> detectorCcdIndexes;

    public DetectorsTransmissionVisitor(int spectralBinning, String filename) throws Exception {

        TextFileReader dfr = new TextFileReader(filename);
        this.spectralBinning = spectralBinning;

        List<Double> x_values = new ArrayList<Double>();
        List<Double> y_values = new ArrayList<Double>();
        double x;

        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                x = dfr.readDouble();
                x_values.add(x);
                y_values.add(dfr.readDouble());
            }
        } catch (IOException e) {
            // normal eof
        }
        initialize(x_values, y_values);
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

    private void initialize(List<Double> x_values, List<Double> y_values) throws Exception {
        // need to sample the file at regular interval pixel values
        int finalPixelValue = x_values.get(x_values.size() - 1).intValue();
        double[] pixelData = new double[finalPixelValue];
        pixelData[0] = 1.0; // XXX previously defaulted to 0
        int j = 0;
        detectorCcdIndexes = new ArrayList<Integer>(6);
        detectorCcdIndexes.add(0);
        int prevY = 1;
        for (int i = 1; i < finalPixelValue; i++) {
            int x = x_values.get(j).intValue();
            int y = y_values.get(j).intValue();
            if (prevY != y) {
                detectorCcdIndexes.add(x - prevY - 1);
                prevY = y;
            }
            if (x == i) {
                pixelData[i] = y;
                j++;
            } else {
                // Apply the last transmission pixel value and go on without incrementing
                pixelData[i] = y_values.get(j - 1);
            }
        }
        detectorCcdIndexes.add(finalPixelValue - 1);

        SEDFactory sedfac = new SEDFactory();
        detectorsTransmissionValues = (VisitableSampledSpectrum) sedfac.getSED(pixelData, 1, 1);

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
        return detectorCcdIndexes.get(detectorIndex*2) / spectralBinning;
    }

    /**
     * Returns the last pixel index for the given detector CCD index (when there are multiple detectors
     * separated by gaps).
     */
    public int getDetectorCcdEndIndex(int detectorIndex, int detectorCount) {
        if (detectorCount == 1) {
            // REL-478: For EEV use only one with the whole range
            return detectorCcdIndexes.get(detectorCcdIndexes.size()-1) / spectralBinning;
        }
        return detectorCcdIndexes.get(detectorIndex*2+1) / spectralBinning;
    }

    /**
     * @return Human-readable representation of this class.
     */
    public String toString() {
        return "Application of the Detector Gaps Transmission";
    }
}
