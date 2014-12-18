// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.Gaussian;

import edu.gemini.itc.parameters.ObservationDetailsParameters;

public class PointSourceFractionCalculation implements SourceFractionCalculatable {

	double im_qual=-1;
	double ap_diam,pixel_size,ap_pix,sw_ap,Npix,source_fraction;
	String ap_type;
        boolean SFprint=true; 
        
	public PointSourceFractionCalculation(String ap_type,
		double ap_diam, double pixel_size) {
		this.ap_type = ap_type;
		this.ap_diam = ap_diam;
		this.pixel_size = pixel_size;
                
		}

	public void calculate() throws Exception {
		if (im_qual < 0)
			throw new Exception("Programming Error, Must set image quality before calling Calculate");

		if (ap_type.equals(ObservationDetailsParameters.AUTO_APER)) {
         		ap_diam = 1.18 * im_qual;
      		} else if (ap_type.equals(
				ObservationDetailsParameters.USER_APER)) {
         		// Do nothing ap_diam is correct
      		} else {
         		throw new Exception(
				"Unknown aperture type: " + ap_type);
      		}

		ap_pix = (Math.PI/4.) * (ap_diam/pixel_size)
				*(ap_diam/pixel_size);
      		Npix = (ap_pix >= 9) ? ap_pix : 9;
      		sw_ap = (ap_pix >= 9) ? ap_diam : 3.4 * pixel_size;

      // Calculate the fraction of source flux contained in this aperture.
      // Found by doing 2-d integral over assumed gaussian profile.
      		double sigma = im_qual/2.355;
      		double ap_ratio = sw_ap/sigma;
      		double ap_frac = Gaussian.get2DIntegral(ap_ratio);

      		source_fraction = (ap_ratio > 5.0) ? 1.0 : ap_frac;
                
	}

	public String getTextResult(FormatStringWriter device) {
		StringBuffer sb = new StringBuffer();
		sb.append("software aperture diameter = " +
			device.toString(sw_ap) + " arcsec\n");
                if (SFprint) {
                    sb.append("fraction of source flux in aperture = " +
                            device.toString(source_fraction) + "\n");
                }
		sb.append("enclosed pixels = " +
			device.toString(Npix) + "\n");
		return sb.toString();
	}

	// must set image quality before calculate
	public void setImageQuality( double im_qual) { this.im_qual = im_qual;}
        public void setApType(String ap_type) { this.ap_type = ap_type; }
        public void setApDiam(double ap_diam) { this.ap_diam = ap_diam; }
        public void setSFPrint(boolean SFprint) { this.SFprint = SFprint; }

	public double getSourceFraction() { return source_fraction;}
	public double getNPix() { return Npix; }
	public double getApDiam() { return ap_diam; }
	public double getApPix() { return ap_pix; }
	public double getSwAp() { return sw_ap; }

}
