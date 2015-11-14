package edu.gemini.itc.operation;

import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.base.VisitableSampledSpectrum;
import edu.gemini.itc.shared.*;

/**
 * The SpecS2NLargeSlitVisitor is used to calculate the s2n of an observation using
 * a larger slit set.
 */
public class SpecS2NLargeSlitVisitor implements SampledSpectrumVisitor, SpecS2N {

    private VisitableSampledSpectrum source_flux;
    private VisitableSampledSpectrum halo_flux;
    private VisitableSampledSpectrum background_flux;
    private VisitableSampledSpectrum spec_signal;
    private VisitableSampledSpectrum sqrt_spec_var_background;
    private VisitableSampledSpectrum spec_exp_s2n;
    private VisitableSampledSpectrum spec_final_s2n;

    private final double spec_Npix;
    private final double spec_frac_with_source;
    private final double slit_width;
    private final double pixel_size;
    private final double spec_source_fraction;
    private final double spec_exp_time;
    private final double im_qual;
    private final double dark_current;
    private final double read_noise;
    private final double skyAper;
    private final int spec_number_exposures;

    private double pix_width;
    private double obs_wave_low;
    private double obs_wave_high;
    private double gratingDispersion_nm;
    private double gratingDispersion_nmppix;
    private double spec_halo_source_fraction;
    private double uncorrected_im_qual;

    private int _firstCcdPixel = 0;
    private int _lastCcdPixel = -1;
    private boolean haloIsUsed = false;


    /**
     * Constructs SpecS2NVisitor with specified slit_width,
     * pixel_size, Smoothing Element, SlitThroughput, spec_Npix(sw aperture
     * size), ExpNum, frac_with_source, ExpTime .
     */
    public SpecS2NLargeSlitVisitor(final double slit_width,
                                   final double pixel_size,
                                   final double pix_width,
                                   final double obs_wave_low,
                                   final double obs_wave_high,
                                   final double gratingDispersion_nm,
                                   final double gratingDispersion_nmppix,
                                   final double spec_source_fraction,
                                   final double im_qual,
                                   final double spec_Npix,
                                   final double read_noise,
                                   final double dark_current,
                                   final ObservationDetails odp) {
        this.slit_width             = slit_width;
        this.pixel_size             = pixel_size;
        this.pix_width              = pix_width;
        this.spec_source_fraction   = spec_source_fraction;
        this.spec_Npix              = spec_Npix;
        this.obs_wave_low           = obs_wave_low;
        this.obs_wave_high          = obs_wave_high;
        this.gratingDispersion_nm   = gratingDispersion_nm;
        this.gratingDispersion_nmppix = gratingDispersion_nmppix;
        this.im_qual                = im_qual;
        this.dark_current           = dark_current;
        this.read_noise             = read_noise;

        final AnalysisMethod analysisMethod = odp.analysisMethod();
        if (!(analysisMethod instanceof ApertureMethod)) throw new Error("Unsuported analysis method");
        this.skyAper                = ((ApertureMethod) analysisMethod).skyAperture();

        // Currently SpectroscopySN is the only supported calculation method for spectroscopy.
        final CalculationMethod calcMethod = odp.calculationMethod();
        if (!(calcMethod instanceof SpectroscopyS2N)) throw new Error("Unsupported calculation method");
        this.spec_number_exposures  = ((SpectroscopyS2N) calcMethod).exposures();
        this.spec_frac_with_source  = calcMethod.sourceFraction();
        this.spec_exp_time          = calcMethod.exposureTime();

    }

    // Return index of last CCD pixel, if defined and in range
    private int lastCcdPixel(final int n) {
        if (_lastCcdPixel == -1 || _lastCcdPixel >= n) return n - 1;
        return _lastCcdPixel;
    }

    public double getImageQuality() {
        return im_qual;
    }

    public double getSpecNpix() {
        return spec_Npix;
    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(final SampledSpectrum sed) {
        //this.obs_wave = (obs_wave_low+obs_wave_high)/2;


        double width, background_width;
        //if image size is less than the slit width it will determine resolution
        // For source:
        if (im_qual < slit_width)
            width = im_qual;
        else width = slit_width;
        // For background:
        background_width = slit_width;

        //calc the width of a spectral resolution element in nm
        //double res_element = obs_wave/grism_res;
        double res_element = gratingDispersion_nm * width / 0.5; // gratingDispersion_nm is the spectral resolution in nm for a 0.5-arcsec slit
        double background_res_element = gratingDispersion_nm * background_width / 0.5;
        //and the data size in the spectral domain
        double res_element_data = res_element / source_flux.getSampling(); // /gratingDispersion_nmppix;
        double background_res_element_data = background_res_element / background_flux.getSampling(); // /gratingDispersion_nmppix;
        //use the int value of spectral_pix as a smoothing element
        int smoothing_element = new Double(res_element_data + 0.5).intValue();
        int background_smoothing_element = new Double(background_res_element_data + 0.5).intValue();
        if (smoothing_element < 1) smoothing_element = 1;
        if (background_smoothing_element < 1) background_smoothing_element = 1;
        ///////////////////////////////////////////////////////////////////////////////////////
        //  We Don't know why but using just the smoothing element is not enough to create the resolution
        //     that we expect.  Using a smoothing element of  =smoothing_element +1
        //     May need to take this out in the future.
        ///////////////////////////////////////////////////////////////////////////////////////
        smoothing_element = smoothing_element + 1;                       // Left in on 04/23/2014 (SLP)
        background_smoothing_element = background_smoothing_element + 1; // Added in on 04/23/2014 (SLP)
        //System.out.println("SpecS2NLargeSlit function:");
        //System.out.println("Source Smoothing Element: " + smoothing_element + " " + res_element_data + " res el: " + res_element + " Sample : " + source_flux.getSampling());
        //System.out.println("Background Smoothing Element: " + background_smoothing_element + " " + background_res_element_data + " res el: " + background_res_element + " Sample : " + background_flux.getSampling());
        //System.out.println("gratingDispersion_nmppix: " + gratingDispersion_nmppix + "gratingDispersion_nm" + gratingDispersion_nm);
        // on the source and background

        source_flux.smoothY(smoothing_element);
        background_flux.smoothY(background_smoothing_element);       // Uncommented and decoupled from IQ on 04/08/2014 (SLP)

        if (haloIsUsed) {
            if (uncorrected_im_qual < slit_width)
                width = im_qual;
            else width = slit_width;
            //calc the width of a spectral resolution element in nm
            //double res_element = obs_wave/grism_res;
            res_element = gratingDispersion_nm * width / 0.5;  // gratingDispersion_nm is the spectral resolution in nm for a 0.5-arcsec slit
            //and the data size in the spectral domain
            res_element_data = res_element / source_flux.getSampling(); // /gratingDispersion_nmppix;
            //use the int value of spectral_pix as a smoothing element
            smoothing_element = new Double(res_element_data + 0.5).intValue();
            if (smoothing_element < 1) smoothing_element = 1;
            ///////////////////////////////////////////////////////////////////////////////////////
            //  We Don't know why but using just the smoothing element is not enough to create the resolution
            //     that we expect.  Using a smoothing element of  =smoothing_element +1
            //     May need to take this out in the future.
            ///////////////////////////////////////////////////////////////////////////////////////
            smoothing_element = smoothing_element + 1;

            halo_flux.smoothY(smoothing_element);

            SampledSpectrumVisitor halo_resample = new ResampleWithPaddingVisitor(
                    obs_wave_low, obs_wave_high - 1,
                    //source_flux.getStart(), source_flux.getEnd(),
                    pix_width, 0);
            halo_flux.accept(halo_resample);
        }


        // resample both sky and SED
        SampledSpectrumVisitor source_resample = new ResampleWithPaddingVisitor(
                obs_wave_low, obs_wave_high - 1,
                //source_flux.getStart(), source_flux.getEnd(),
                pix_width, 0);
        SampledSpectrumVisitor background_resample = new ResampleWithPaddingVisitor(
                obs_wave_low, obs_wave_high - 1,
                //source_flux.getStart(), source_flux.getEnd(),
                pix_width, 0);


        source_flux.accept(source_resample);
        background_flux.accept(background_resample);

        // the number of exposures measuring the source flux is
        double spec_number_source_exposures = spec_number_exposures * spec_frac_with_source;

        final VisitableSampledSpectrum spec_var_source = (VisitableSampledSpectrum) source_flux.clone();
        final VisitableSampledSpectrum spec_var_background = (VisitableSampledSpectrum) background_flux.clone();

        //Shot noise on the source flux in aperture
        int source_flux_last = lastCcdPixel(source_flux.getLength());
        if (haloIsUsed) {
            for (int i = _firstCcdPixel; i <= source_flux_last; ++i)
                spec_var_source.setY(i, source_flux.getY(i) * spec_source_fraction *
                        spec_exp_time * gratingDispersion_nmppix + halo_flux.getY(i) * spec_halo_source_fraction *
                        spec_exp_time * gratingDispersion_nmppix);
        } else {
            for (int i = _firstCcdPixel; i <= source_flux_last; ++i)
                spec_var_source.setY(i, source_flux.getY(i) * spec_source_fraction *
                        spec_exp_time * gratingDispersion_nmppix);
        }
        //Shot noise on background flux in aperture
        int spec_var_background_last = lastCcdPixel(spec_var_background.getLength());
        for (int i = _firstCcdPixel; i <= spec_var_background_last; ++i)
            spec_var_background.setY(i, background_flux.getY(i) * slit_width *
                    pixel_size * spec_Npix * spec_exp_time * gratingDispersion_nmppix);

        //Shot noise on dark current flux in aperture
        double spec_var_dark = dark_current * spec_Npix * spec_exp_time;

        //Readout noise in aperture
        double spec_var_readout = read_noise * read_noise * spec_Npix;

        //Create a container for the total and sourceless noise in the aperture
        final VisitableSampledSpectrum spec_noise = (VisitableSampledSpectrum) source_flux.clone();
        final VisitableSampledSpectrum spec_sourceless_noise = (VisitableSampledSpectrum) source_flux.clone();

        spec_signal = (VisitableSampledSpectrum) source_flux.clone();
        spec_exp_s2n = (VisitableSampledSpectrum) source_flux.clone();
        spec_final_s2n = (VisitableSampledSpectrum) source_flux.clone();
        sqrt_spec_var_background = (VisitableSampledSpectrum) spec_var_background.clone();

        // Total noise in the aperture is ...
        int spec_noise_last = lastCcdPixel(spec_noise.getLength());
        for (int i = _firstCcdPixel; i <= spec_noise_last; ++i)
            spec_noise.setY(i, Math.sqrt(spec_var_source.getY(i) + spec_var_background.getY(i) + spec_var_dark + spec_var_readout));

        // and ...
        int spec_sourceless_noise_last = lastCcdPixel(spec_sourceless_noise.getLength());
        for (int i = _firstCcdPixel; i <= spec_sourceless_noise_last; ++i)
            spec_sourceless_noise.setY(i, Math.sqrt(spec_var_background.getY(i) +
                    spec_var_dark + spec_var_readout));

        //total source flux in the aperture
        int spec_signal_last = lastCcdPixel(spec_signal.getLength());
        if (haloIsUsed) {
            for (int i = _firstCcdPixel; i <= spec_signal_last; ++i)
                spec_signal.setY(i, source_flux.getY(i) *
                        spec_source_fraction * spec_exp_time * gratingDispersion_nmppix + halo_flux.getY(i) *
                        spec_halo_source_fraction * spec_exp_time * gratingDispersion_nmppix);
        } else {
            for (int i = _firstCcdPixel; i <= spec_signal_last; ++i)
                spec_signal.setY(i, source_flux.getY(i) *
                        spec_source_fraction * spec_exp_time * gratingDispersion_nmppix);
        }
        //S2N for one exposure
        int spec_exp_s2n_last = lastCcdPixel(spec_exp_s2n.getLength());
        for (int i = _firstCcdPixel; i <= spec_exp_s2n_last; ++i)
            spec_exp_s2n.setY(i, spec_signal.getY(i) / spec_noise.getY(i));

        double noiseFactor = 1 + (1 / skyAper);
        //S2N for the observation
        int spec_final_s2n_last = lastCcdPixel(spec_final_s2n.getLength());
        for (int i = _firstCcdPixel; i <= spec_final_s2n_last; ++i)
            spec_final_s2n.setY(i, Math.sqrt(spec_number_source_exposures) *
                    spec_signal.getY(i) /
                    Math.sqrt(spec_signal.getY(i) + noiseFactor *
                            spec_sourceless_noise.getY(i) *
                            spec_sourceless_noise.getY(i)));

        //Finally create the Sqrt(Background) sed for plotting
        for (int i = _firstCcdPixel; i <= spec_var_background_last; ++i)
            sqrt_spec_var_background.setY(i, Math.sqrt(spec_var_background.getY(i)));
    }

    public void setSourceSpectrum(final VisitableSampledSpectrum sed) {
        source_flux = sed;
    }

    public void setHaloSpectrum(final VisitableSampledSpectrum sed) {
        halo_flux = sed;
        haloIsUsed = true;
    }

    public void setSpecHaloSourceFraction(final double spec_halo_source_fraction) {
        this.spec_halo_source_fraction = spec_halo_source_fraction;
    }

    public void setHaloImageQuality(final double uncorrected_im_qual) {
        this.uncorrected_im_qual = uncorrected_im_qual;
    }

    public void setBackgroundSpectrum(final VisitableSampledSpectrum sed) {
        background_flux = sed;
    }

    public void setCcdPixelRange(final int first, final int last) {
        _firstCcdPixel = first;
        _lastCcdPixel = last;
    }

    public void setGratingDispersion_nmppix(final double gratingDispersion_nmppix) {
        this.gratingDispersion_nmppix = gratingDispersion_nmppix;
    }

    public void setGratingDispersion_nm(final double gratingDispersion_nm) {
        this.gratingDispersion_nm = gratingDispersion_nm;
    }

    public void setSpectralPixelWidth(final double pix_width) {
        this.pix_width = pix_width;
    }

    public void setStartWavelength(final double obs_wave_low) {
        this.obs_wave_low = obs_wave_low;
    }

    public void setEndWavelength(final double obs_wave_high) {
        this.obs_wave_high = obs_wave_high;
    }

    public VisitableSampledSpectrum getSignalSpectrum() {
        return spec_signal;
    }

    public VisitableSampledSpectrum getBackgroundSpectrum() {
        return sqrt_spec_var_background;
    }

    public VisitableSampledSpectrum getExpS2NSpectrum() {
        return spec_exp_s2n;
    }

    public VisitableSampledSpectrum getFinalS2NSpectrum() {
        return spec_final_s2n;
    }


    public String toString() {
        return "SpecS2NVisitor ";
    }
}
