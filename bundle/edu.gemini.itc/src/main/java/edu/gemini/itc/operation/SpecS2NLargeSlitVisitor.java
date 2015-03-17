package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.VisitableSampledSpectrum;

/**
 * The SpecS2NLargeSlitVisitor is used to calculate the s2n of an observation using
 * a larger slit set.
 */
public class SpecS2NLargeSlitVisitor implements SampledSpectrumVisitor {
    // private ArraySpectrum _telescopeBack = null;
    private VisitableSampledSpectrum source_flux, halo_flux, background_flux, spec_noise,
            spec_sourceless_noise, spec_signal, spec_var_source, spec_var_background,
            sqrt_spec_var_background, spec_exp_s2n, spec_final_s2n;
    private double slit_width, pixel_size, spec_source_fraction, spec_halo_source_fraction, pix_width, spec_Npix, spec_frac_with_source, spec_exp_time, im_qual, uncorrected_im_qual, dark_current, read_noise, obs_wave, obs_wave_low, obs_wave_high, grating_res,
            gratingDispersion_nm, gratingDispersion_nmppix, skyAper;
    private int spec_number_exposures;
    private boolean haloIsUsed = false;

    private edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;

    private int _firstCcdPixel = 0, _lastCcdPixel = -1;

    /**
     * Constructs SpecS2NVisitor with specified slit_width,
     * pixel_size, Smoothing Element, SlitThroughput, spec_Npix(sw aperture
     * size), ExpNum, frac_with_source, ExpTime .
     */
    public SpecS2NLargeSlitVisitor(double slit_width, double pixel_size,
                                   double pix_width, double obs_wave_low,
                                   double obs_wave_high, double gratingDispersion_nm,
                                   double gratingDispersion_nmppix, double grating_res,
                                   double spec_source_fraction, double im_qual,
                                   double spec_Npix, int spec_number_exposures,
                                   double spec_frac_with_source, double spec_exp_time,
                                   double dark_current, double read_noise,
                                   double skyAper) {
        this.slit_width = slit_width;
        this.pixel_size = pixel_size;
        this.pix_width = pix_width;
        this.spec_source_fraction = spec_source_fraction;
        this.spec_Npix = spec_Npix;
        this.spec_frac_with_source = spec_frac_with_source;
        this.spec_exp_time = spec_exp_time;
        this.obs_wave_low = obs_wave_low;
        this.obs_wave_high = obs_wave_high;
        this.gratingDispersion_nm = gratingDispersion_nm;
        this.gratingDispersion_nmppix = gratingDispersion_nmppix;
        this.grating_res = grating_res;
        this.im_qual = im_qual;
        this.dark_current = dark_current;
        this.read_noise = read_noise;
        this.spec_number_exposures = spec_number_exposures;
        this.skyAper = skyAper;

    }

    // Return index of last CCD pixel, if defined and in range
    private int lastCcdPixel(int n) {
        if (_lastCcdPixel == -1 || _lastCcdPixel >= n) return n - 1;
        return _lastCcdPixel;
    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) {
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
            halo_flux.accept(_dtv);
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

        source_flux.accept(_dtv);
        background_flux.accept(_dtv);

        // the number of exposures measuring the source flux is
        double spec_number_source_exposures = spec_number_exposures * spec_frac_with_source;

        spec_var_source = (VisitableSampledSpectrum) source_flux.clone();
        spec_var_background = (VisitableSampledSpectrum) background_flux.clone();

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
        //Create a container for the total and sourceless noise in the
        //aperture
        spec_noise = (VisitableSampledSpectrum) source_flux.clone();
        spec_sourceless_noise = (VisitableSampledSpectrum) source_flux.clone();

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

    public void setSourceSpectrum(VisitableSampledSpectrum sed) {
        source_flux = sed;
    }

    public void setHaloSpectrum(VisitableSampledSpectrum sed) {
        halo_flux = sed;
        haloIsUsed = true;
    }

    public void setSpecHaloSourceFraction(double spec_halo_source_fraction) {
        this.spec_halo_source_fraction = spec_halo_source_fraction;
    }

    public void setHaloImageQuality(double uncorrected_im_qual) {
        this.uncorrected_im_qual = uncorrected_im_qual;
    }

    public void setBackgroundSpectrum(VisitableSampledSpectrum sed) {
        background_flux = sed;
    }

    public void setDetectorTransmission(edu.gemini.itc.operation.DetectorsTransmissionVisitor dtv) {
        _dtv = dtv;
    }

    public void setCcdPixelRange(int first, int last) {
        _firstCcdPixel = first;
        _lastCcdPixel = last;
    }

    public void setGratingDispersion_nmppix(double gratingDispersion_nmppix) {
        this.gratingDispersion_nmppix = gratingDispersion_nmppix;
    }

    public void setGratingDispersion_nm(double gratingDispersion_nm) {
        this.gratingDispersion_nm = gratingDispersion_nm;
    }

    public void setSpectralPixelWidth(double pix_width) {
        this.pix_width = pix_width;
    }

    public void setStartWavelength(double obs_wave_low) {
        this.obs_wave_low = obs_wave_low;
    }

    public void setEndWavelength(double obs_wave_high) {
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
