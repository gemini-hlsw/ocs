// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may
// only be used or copied as described in the license set out in the
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
//import edu.gemini.itc.gmos.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.ITCConstants;

/**
 * The SpecS2NVisitor is used to calculate the s2n of an observation using
 * the niri grism set.
 */
public class SpecS2NVisitor implements SampledSpectrumVisitor {
    // private ArraySpectrum _telescopeBack = null;
    private VisitableSampledSpectrum source_flux,halo_flux,background_flux,spec_noise,
        spec_sourceless_noise, spec_signal, spec_var_source, spec_var_background,
        sqrt_spec_var_background, spec_exp_s2n, spec_final_s2n;
    private double slit_width, pixel_size, spec_source_fraction, spec_halo_source_fraction,
        pix_width, spec_Npix, spec_frac_with_source, spec_exp_time, im_qual, uncorrected_im_qual,
        dark_current, read_noise, obs_wave, obs_wave_low, obs_wave_high, grism_res;
    private int spec_number_exposures;
    private boolean haloIsUsed = false;

    
    
    
    /**
     * Constructs SpecS2NVisitor with specified slit_width,
     * pixel_size, Smoothing Element, SlitThroughput, spec_Npix(sw aperture
     * size), ExpNum, frac_with_source, ExpTime .
     */
    public SpecS2NVisitor(double slit_width, double pixel_size,
                          double pix_width, double obs_wave_low,
                          double obs_wave_high, double grism_res,
                          double spec_source_fraction, double im_qual,
                          double spec_Npix, int spec_number_exposures,
                          double spec_frac_with_source, double spec_exp_time,
                          double dark_current, double read_noise)
            throws Exception {
        this.slit_width = slit_width;
        this.pixel_size = pixel_size;
        this.pix_width = pix_width;
        this.spec_source_fraction = spec_source_fraction;
        this.spec_Npix = spec_Npix;
        this.spec_frac_with_source = spec_frac_with_source;
        this.spec_exp_time = spec_exp_time;
        this.obs_wave_low = obs_wave_low;
        this.obs_wave_high = obs_wave_high;
        this.grism_res = grism_res;
        this.im_qual = im_qual;
        this.dark_current = dark_current;
        this.read_noise = read_noise;
        this.spec_number_exposures = spec_number_exposures;

    }

    /**
     * Implements the SampledSpectrumVisitor interface
     */
    public void visit(SampledSpectrum sed) throws Exception {
        this.obs_wave = (obs_wave_low + obs_wave_high) / 2;

  
	//double width;
	//if image size is less than the slit width it will determine resolution
        //if (im_qual<slit_width)
        //    width = im_qual;
        //else width = slit_width; 
        //calc the width of a spectral resolution element in nm
        double res_element = obs_wave / grism_res;  // 2-pixel resolution element [nm]
        double background_res_element = res_element;  // OLD: background_res_element = (res_element / 2.) * (slit_width / pixel_size);   // slit-width [nm]  (edited on 10/10/2014)

        //and the data size in the spectral domain
        double res_element_data = res_element / source_flux.getSampling(); // /pix_width;
        double background_res_element_data = background_res_element / background_flux.getSampling();
        
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
        smoothing_element = smoothing_element + 1;
        background_smoothing_element = background_smoothing_element + 1;
        //System.out.println("Smoothing Element: " + smoothing_element+ " "+ res_element_data+ "res el" + res_element);
        // on the source and background

        source_flux.smoothY(smoothing_element);
        background_flux.smoothY(background_smoothing_element);

	if (haloIsUsed){
		//if (uncorrected_im_qual<slit_width)
		//	width = uncorrected_im_qual;
		//else width = slit_width;
	//calc the width of a spectral resolution element in nm
        res_element = obs_wave / grism_res;
        //and the data size in the spectral domain
        res_element_data = res_element / source_flux.getSampling(); // /pix_width;
        //use the int value of spectral_pix as a smoothing element
        smoothing_element = new Double(res_element_data + 0.5).intValue();
        if (smoothing_element < 1) smoothing_element = 1;
        ///////////////////////////////////////////////////////////////////////////////////////
        //  We Don't know why but using just the smoothing element is not enough to create the resolution
        //     that we expect.  Using a smoothing element of  =smoothing_element +1
        //     May need to take this out in the future.
        ///////////////////////////////////////////////////////////////////////////////////////
        smoothing_element = smoothing_element + 1; // REL-557: F2 ITC: S/N does not appear to change with disperser
        //System.out.println("Smoothing Element: " + smoothing_element+ " "+ res_element_data+ "res el" + res_element);
        // on the source and background

	halo_flux.smoothY(smoothing_element);

	SampledSpectrumVisitor halo_resample = new ResampleWithPaddingVisitor(
                obs_wave_low, obs_wave_high,
                pix_width,0);

	halo_flux.accept(halo_resample);

       } 
        // resample both sky and SED
        SampledSpectrumVisitor resample = new ResampleWithPaddingVisitor(
                obs_wave_low, obs_wave_high,
                pix_width,0);
        
        source_flux.accept(resample);
        background_flux.accept(resample);
        
        // the number of exposures measuring the source flux is
        double spec_number_source_exposures =
                spec_number_exposures * spec_frac_with_source;

        spec_var_source = (VisitableSampledSpectrum) source_flux.clone();
        spec_var_background = (VisitableSampledSpectrum) background_flux.clone();

        //Shot noise on the source flux in aperture
	if (haloIsUsed){
		for (int i = 0; i < source_flux.getLength(); ++i)
            spec_var_source.setY(i, source_flux.getY(i) * spec_source_fraction *
                                    spec_exp_time * pix_width+ halo_flux.getY(i)*spec_halo_source_fraction*
				    spec_exp_time * pix_width);
 	}else{
        for (int i = 0; i < source_flux.getLength(); ++i)
            spec_var_source.setY(i, source_flux.getY(i) * spec_source_fraction *
                                    spec_exp_time * pix_width);
	}
        //Shot noise on background flux in aperture
        for (int i = 0; i < spec_var_background.getLength(); ++i)
            spec_var_background.setY(i, background_flux.getY(i) * slit_width *
                                        pixel_size * spec_Npix * spec_exp_time * pix_width);

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
        sqrt_spec_var_background =
                (VisitableSampledSpectrum) spec_var_background.clone();

        // Total noise in the aperture is ...
        for (int i = 0; i < spec_noise.getLength(); ++i)
            spec_noise.setY(i, Math.sqrt(spec_var_source.getY(i) +
                                         spec_var_background.getY(i) +
                                         spec_var_dark + spec_var_readout));
        // and ...
        for (int i = 0; i < spec_sourceless_noise.getLength(); ++i)
            spec_sourceless_noise.setY(i, Math.sqrt(spec_var_background.getY(i) +
                                                    spec_var_dark + spec_var_readout));

        //total source flux in the aperture

	if (haloIsUsed){
		for (int i = 0; i < spec_signal.getLength(); ++i)
            		spec_signal.setY(i, source_flux.getY(i) *
                                spec_source_fraction * spec_exp_time * pix_width+ halo_flux.getY(i)*
                        spec_halo_source_fraction*spec_exp_time * pix_width);
	}else{
        	for (int i = 0; i < spec_signal.getLength(); ++i)
            		spec_signal.setY(i, source_flux.getY(i) *
                                spec_source_fraction * spec_exp_time * pix_width);
 	}
        //S2N for one exposure
        for (int i = 0; i < spec_exp_s2n.getLength(); ++i)
            spec_exp_s2n.setY(i, spec_signal.getY(i) / spec_noise.getY(i));

        //S2N for the observation
        for (int i = 0; i < spec_final_s2n.getLength(); ++i)
            spec_final_s2n.setY(i, Math.sqrt(spec_number_source_exposures) *
                                   spec_signal.getY(i) /
                                   Math.sqrt(spec_signal.getY(i) + 2 *
                                                                   spec_sourceless_noise.getY(i) *
                                                                   spec_sourceless_noise.getY(i)));

        //Finally create the Sqrt(Background) sed for plotting
        for (int i = 0; i < spec_var_background.getLength(); ++i)
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

    public VisitableSampledSpectrum getSignalSpectrum() {
        return spec_signal;
    }

    public VisitableSampledSpectrum getBackgroundSpectrum() {
        return sqrt_spec_var_background;
    }

    public VisitableSampledSpectrum getBackgroundSpectrum_wo_sqrt() {
        return spec_var_background;
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
