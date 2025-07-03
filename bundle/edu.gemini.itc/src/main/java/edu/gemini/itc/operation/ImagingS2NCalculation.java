package edu.gemini.itc.operation;

import edu.gemini.itc.base.BinningProvider;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.gsaoi.Gsaoi;
import edu.gemini.itc.niri.Niri;
import edu.gemini.itc.shared.ApertureMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.trecs.TRecs;

public abstract class ImagingS2NCalculation implements ImagingS2NCalculatable {
    protected final double Npix;
    protected final double source_fraction;
    protected final double dark_current;
    protected final double sed_integral;
    protected final double sky_integral;
    protected final double skyAper;
    protected final int elfinParam;

    protected double var_source;
    protected double var_background;
    protected double var_dark;
    protected double var_readout;
    protected double noise;
    protected double sourceless_noise;
    protected double signal;
    protected double noiseFactor;
    protected double read_noise;
    protected double pixel_size;
    protected double exposure_time;
    protected int    coadds;

    protected double secondary_integral = 0;
    protected double secondary_source_fraction = 0;

    public ImagingS2NCalculation( final ObservationDetails obs, final Instrument instrument, final SourceFraction sourceFrac, final double sed_integral, final double sky_integral) {
        this.sed_integral    = sed_integral;
        this.sky_integral    = sky_integral;
        this.source_fraction = sourceFrac.getSourceFraction();
        this.Npix            = sourceFrac.getNPix();
        this.dark_current    = (instrument instanceof BinningProvider) ?
                instrument.getDarkCurrent() * ((BinningProvider) instrument).getSpatialBinning() * ((BinningProvider) instrument).getSpectralBinning() :
                instrument.getDarkCurrent();
        // TODO: Why 1 for NIRI/GSAOI?? Is this a bug or is there a reason why in the original code those instruments did not
        // TODO: set the aperture and used a (default) value of 1?
        this.skyAper         = (instrument instanceof Niri || instrument instanceof Gsaoi) ? 1 : ((ApertureMethod) obs.analysisMethod()).skyAperture();
        // TODO: marker interface like for binning?
        this.elfinParam      = (instrument instanceof TRecs) ? ((TRecs) instrument).getExtraLowFreqNoise() : 1; // default 1 will have no effect
        coadds = obs.calculationMethod().coaddsOrElse(1);
    }

    public void calculate() {
        noiseFactor = 1 + (1 / skyAper);
        var_source = sed_integral * source_fraction * exposure_time;
        var_source = var_source + secondary_integral * secondary_source_fraction * exposure_time;

        //Multiply source by Extra-Low Frequency Noise
        var_source = var_source * elfinParam;
        var_background = sky_integral * exposure_time * pixel_size * pixel_size * Npix;

        //Multiply background by Extra-Low Frequency Noise
        var_background = var_background * elfinParam;
        var_dark = dark_current * Npix * exposure_time;
        var_readout = read_noise * read_noise * Npix;

        noise = Math.sqrt(var_source + var_background + var_dark + var_readout);
        sourceless_noise = Math.sqrt(var_background + var_dark + var_readout);
        signal = sed_integral * source_fraction * exposure_time +
                secondary_integral * secondary_source_fraction * exposure_time;
    }

    public void setSecondaryIntegral(double secondary_integral) {
        this.secondary_integral = secondary_integral;
    }

    public void setSecondarySourceFraction(double secondary_source_fraction) {
        this.secondary_source_fraction = secondary_source_fraction;
    }

    public double getVarSource() { return var_source; }
    public double getVarBackground() { return var_background; }
    public double getVarDark() { return var_dark; }
    public double getVarReadout() { return var_readout; }
    public double getNoise() { return noise; }
    public double getSignal() { return signal; }

    @Override public double totalSNRatio() {
        return Math.sqrt(numberSourceExposures()) * signal / Math.sqrt(signal + noiseFactor * sourceless_noise * sourceless_noise);
    }

    public double singleSNRatio() {
        return Math.sqrt(coadds) * signal / noise;
    }

    @Override public double getExposureTime() { return exposure_time; }

}
