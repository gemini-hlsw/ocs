// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SEDFactory;
import edu.gemini.itc.operation.ResampleVisitor;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.TextFileReader;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Instrument;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.text.ParseException;

/**
 * For Gmos Spectroscopy the spectrum will be spread across 3 CCD's
 * The Three CCD's are not continous.  The Gaps will be represented in
 * a file read in.
 */
public class DetectorsTransmissionVisitor implements SampledSpectrumVisitor {
    private static final String FILENAME = "ccdpix_red";
    private List x_values;
    private List y_values;
    private double maxPixelValue;
    private int spectralBinning;
    private VisitableSampledSpectrum detectorsTransmissionValues;


    public DetectorsTransmissionVisitor(int spectralBinning) throws Exception {
        TextFileReader dfr = new TextFileReader(ITCConstants.LIB + "/" +
                                                "gmos/" +
                                                Gmos.getPrefix() +
                                                FILENAME +
                                                Instrument.getSuffix());
        this.spectralBinning = spectralBinning;

        x_values = new ArrayList();
        y_values = new ArrayList();
        double x;

        try {
            while (true) {
                x = dfr.readDouble();
                x_values.add(new Double(x));
                y_values.add(new Double(dfr.readDouble()));
                maxPixelValue = x;
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
        }
        initialize(x_values, y_values);
    }

    /**
     * This method performs the Detectors transmision manipulation on the SED.
     */
    public void visit(SampledSpectrum sed) throws Exception {
        int num_elements = sed.getLength();
        //System.out.println("Ratio: " + detectorsTransmissionValues.getLength()/sed.getLength()+
        //					" dtv: " +detectorsTransmissionValues.getLength()+ " sed: "+sed.getLength());
        // resample the DetectorsTransmissionfile to the length of the sed.
        // to compensate for spectral direction binning effects.
//		ResampleVisitor detectorResample = new ResampleVisitor(
//						detectorsTransmissionValues.getStart(),
//						sed.getLength(),
//						//detectorsTransmissionValues.getLength(),
//						(new Double(detectorsTransmissionValues.getLength()/
//									sed.getLength())).intValue());

//		detectorsTransmissionValues.accept(detectorResample);

        /******  Old code without binning do not delete yet.
         if (Math.abs(maxPixelValue - num_elements) >=3)
         throw new Exception("Sed Has Not been resampled to fit the detector. "+
         "This will create an incorrect sed when the detector " +
         "transmission is applied.  Aborting.");


         double[] data = new double[num_elements];

         int j=0;
         for (int i=0; i<num_elements-1; i++) {
         // if the Transmission pixel value is equal to the counter
         // then apply that y_value and increment the counter
         // this is done because the transmission file does not
         // include all of the values 0..N but rather has
         // important subsets.
         //System.out.println("i: " + i+" Pixval: " +((Double)x_values.get(j)).doubleValue());
         if ( ((Double)x_values.get(j)).doubleValue()-1 == i) {
         data[i] = sed.getY(i*sed.getSampling()+sed.getStart())*
         ((Double)y_values.get(j)).doubleValue();
         j++;
         } else {
         // Apply the last transmission pixel value and go on
         // without incrementing
         data[i] = sed.getY(i*sed.getSampling()+sed.getStart())*
         ((Double)y_values.get(j-1)).doubleValue();
         }


         }
         *****/

        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i,
                     sed.getY(i * sed.getSampling() + sed.getStart()) *
                     detectorsTransmissionValues.getY(i *
                                                      detectorsTransmissionValues.getSampling() +
                                                      detectorsTransmissionValues.getStart()));
        }

        //sed.reset(data, sed.getStart(), sed.getSampling());
    }

    private void initialize(List x_values, List y_values) throws Exception {
        // need to sample the file at regular interval pixel values
        int finalPixelValue = ((Double) x_values.get(x_values.size() - 1)).intValue();
        //System.out.println("final: "+finalPixelValue);
        double[] pixelData = new double[finalPixelValue];
        int j = 0;
        for (int i = 1; i < finalPixelValue; i++) {
            if (((Double) x_values.get(j)).doubleValue() == i) {
                pixelData[i] = ((Double) y_values.get(j)).doubleValue();
                j++;
            } else {
                // Apply the last transmission pixel value and go on
                // without incrementing
                pixelData[i] = ((Double) y_values.get(j - 1)).doubleValue();
            }
        }
        SEDFactory sedfac = new SEDFactory();
        detectorsTransmissionValues = (VisitableSampledSpectrum) sedfac.getSED(pixelData, 1, 1);

        ResampleVisitor detectorResample = new ResampleVisitor(
                detectorsTransmissionValues.getStart(),
                //sed.getLength(),
                detectorsTransmissionValues.getLength(),
                spectralBinning);
        //(new Double(detectorsTransmissionValues.getLength()/
        //			sed.getLength())).intValue());
        detectorsTransmissionValues.accept(detectorResample);

    }


    /** @return Human-readable representation of this class. */
    public String toString() {
        return "Application of the Detector Gaps Transmission";
    }
}
