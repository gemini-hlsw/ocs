// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public class ImagingS2NMethodACalculation extends ImagingS2NCalculation {

    int number_exposures;
    double frac_with_source, exp_s2n, final_s2n, number_source_exposures;

    public ImagingS2NMethodACalculation(int number_exposures,
                                        double frac_with_source,
                                        double exposure_time, double read_noise,
                                        double pixel_size) {

        this.number_exposures = number_exposures;
        this.frac_with_source = frac_with_source;
        this.exposure_time = exposure_time;
        this.read_noise = read_noise;
        this.pixel_size = pixel_size;
    }

    public void calculate() throws Exception {
        super.calculate();

        double epsilon = 0.2;
        double number_source_exposures = number_exposures * frac_with_source;
        int iNumExposures = (int) (number_source_exposures + 0.5);
        double diff = number_source_exposures - iNumExposures;
        if (Math.abs(diff) > epsilon) {
            throw new Exception(
                    "Fraction with source value produces non-integral number of source exposures with source (" +
                            number_source_exposures + " vs. " + iNumExposures + ").");
        }

        exp_s2n = signal / noise;


        final_s2n = Math.sqrt(number_source_exposures) * signal /
                Math.sqrt(signal + noiseFactor * sourceless_noise *
                        sourceless_noise);

    }

    public String getTextResult(FormatStringWriter device) {
        StringBuffer sb = new StringBuffer(super.getTextResult(device));
        sb.append("Intermediate S/N for one exposure = " +
                device.toString(exp_s2n) + "\n\n");
        sb.append("S/N for the whole observation = "
                + device.toString(final_s2n) +
                " (including sky subtraction)\n\n");

        sb.append("Requested total integration time = " +
                device.toString(exposure_time * number_exposures) +
                " secs, of which " + device.toString(exposure_time *
                number_exposures *
                frac_with_source) +
                " secs is on source.\n");
        return sb.toString();
    }


}

