// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public abstract class ImagingS2NCalculation implements ImagingS2NCalculatable {
    double var_source, var_background, var_dark, var_readout,
            noise, sourceless_noise, signal, sed_integral, source_fraction,
            Npix, sky_integral, read_noise, dark_current, pixel_size,
            exposure_time, noiseFactor;

    double secondary_integral = 0;
    double secondary_source_fraction = 0;

    double skyAper = 1;

    //Extra Low frequency noise.  Default:  Has no effect.
    int elfinParam = 1;

    public void calculate() throws Exception {
        noiseFactor = 1 + (1 / skyAper);
        var_source = sed_integral * source_fraction * exposure_time;
        var_source = var_source + secondary_integral * secondary_source_fraction * exposure_time;

        //Multiply source by Extra-Low Frequency Noise
        var_source = var_source * elfinParam;
        var_background = sky_integral * exposure_time * pixel_size *
                pixel_size * Npix;
        //Multiply background by Extra-Low Frequency Noise
        var_background = var_background * elfinParam;
        var_dark = dark_current * Npix * exposure_time;
        var_readout = read_noise * read_noise * Npix;

        noise = Math.sqrt(var_source + var_background + var_dark +
                var_readout);
        sourceless_noise = Math.sqrt(var_background + var_dark +
                var_readout);
        signal = sed_integral * source_fraction * exposure_time +
                secondary_integral * secondary_source_fraction * exposure_time;
    }

    public void setSedIntegral(double sed_integral) {
        this.sed_integral = sed_integral;
    }

    public void setSecondaryIntegral(double secondary_integral) {
        this.secondary_integral = secondary_integral;
    }

    public void setSecondarySourceFraction(double secondary_source_fraction) {
        this.secondary_source_fraction = secondary_source_fraction;
    }

    public void setSourceFraction(double source_fraction) {
        this.source_fraction = source_fraction;
    }

    public void setDarkCurrent(double dark_current) {
        this.dark_current = dark_current;
    }

    public void setNpix(double Npix) {
        this.Npix = Npix;
    }

    public void setSkyAperture(double skyAper) {
        this.skyAper = skyAper;
    }

    public void setSkyIntegral(double sky_integral) {
        this.sky_integral = sky_integral;
    }

    //method to set the extra low freq noise.
    public void setExtraLowFreqNoise(int extraLowFreqNoise) {
        this.elfinParam = extraLowFreqNoise;
    }

    public String getTextResult(FormatStringWriter device) {
        StringBuffer sb = new StringBuffer();
        sb.append("Contributions to total noise (e-) in aperture (per exposure):\n");
        sb.append("Source noise = " +
                device.toString(Math.sqrt(var_source)) + "\n");
        sb.append("Background noise = " +
                device.toString(Math.sqrt(var_background)) + "\n");
        sb.append("Dark current noise = " +
                device.toString(Math.sqrt(var_dark)) + "\n");
        sb.append("Readout noise = " +
                device.toString(Math.sqrt(var_readout)) + "\n\n");


        sb.append("Total noise per exposure = " +
                device.toString(noise) + "\n");
        sb.append("Total signal per exposure = " +
                device.toString(signal) + "\n\n");

        return sb.toString();
    }

    public String getBackgroundLimitResult() {
        if (Math.sqrt(var_source + var_dark + var_readout) > Math.sqrt(var_background))
            return "Warning: observation is NOT background noise limited";
        else return "Observation is background noise limited.";
    }


}
