package edu.gemini.itc.operation;

import edu.gemini.itc.gsaoi.Gsaoi;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.service.ObservationDetails;
import edu.gemini.itc.shared.BinningProvider;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.Instrument;

public abstract class ImagingS2NCalculation implements ImagingS2NCalculatable {

    final double Npix;
    final double source_fraction;
    final double dark_current;
    final double sed_integral;
    final double sky_integral;
    final double skyAper;

    double var_source, var_background, var_dark, var_readout,
            noise, sourceless_noise, signal,
            read_noise, pixel_size,
            exposure_time, noiseFactor;

    double secondary_integral = 0;
    double secondary_source_fraction = 0;

    //Extra Low frequency noise.  Default:  Has no effect.
    int elfinParam = 1;

    public ImagingS2NCalculation( final ObservationDetails obs, final Instrument instrument,final SourceFraction sourceFrac, final double sed_integral, final double sky_integral) {
        this.sed_integral    = sed_integral;
        this.sky_integral    = sky_integral;
        this.source_fraction = sourceFrac.getSourceFraction();
        this.Npix            = sourceFrac.getNPix();
        this.dark_current    = (instrument instanceof BinningProvider) ?
                instrument.getDarkCurrent() * ((BinningProvider) instrument).getSpatialBinning() * ((BinningProvider) instrument).getSpectralBinning() :
                instrument.getDarkCurrent();
        // TODO: Why 1 for NIRI/GSAOI?? Is this a bug or is there a reason why in the original code those instruments did not
        // TODO: set the aperture and used a (default) value of 1?
        this.skyAper         = (instrument instanceof Niri || instrument instanceof Gsaoi) ? 1 : obs.getSkyApertureDiameter();
    }

    public void calculate() {
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

    public void setSecondaryIntegral(double secondary_integral) {
        this.secondary_integral = secondary_integral;
    }

    public void setSecondarySourceFraction(double secondary_source_fraction) {
        this.secondary_source_fraction = secondary_source_fraction;
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
