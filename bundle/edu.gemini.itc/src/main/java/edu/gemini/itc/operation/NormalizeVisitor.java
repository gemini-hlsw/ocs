// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: NormalizeVisitor.java,v 1.5 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.ZeroMagnitudeStar;
import edu.gemini.itc.shared.WavebandDefinition;

import edu.gemini.itc.parameters.SourceDefinitionParameters;

// for units

/**
 * The NormalizeVisitor class is used to perform Normalization to the SED.
 * Normalization rescales the SED so that the average flux in a
 * specified waveband is equal to a specified value.
 * This is where unit conversion happens.
 */
public class NormalizeVisitor implements SampledSpectrumVisitor {
    private String _band; // String description of waveband (A, B, R, etc.)
    private double _user_norm; // Brightness of the object (flux) as an average
    private String _units;  // mag, abmag, ...

    /**
     * Constructs a Normalizer
     * @param waveband The waveband to normalize over
     * @param user_norm The average flux in the waveband
     * @param units The code for the units chosen by user
     */
    public NormalizeVisitor(String waveband, double user_norm, String units) {
        _band = waveband;
        _user_norm = user_norm;
        _units = units;
    }

    /**
     * Implements the visitor interface.
     * Performs the normalization.
     */
    public void visit(SampledSpectrum sed) throws Exception {
        // obtain normalization value
        double norm = -1;

        // This is hard-wired to use the flux of a zero-magnitude star.
        // Flux is in magnitudes
        if (_units == null) throw new Exception("null units");
        // Here is where you do unit conversions.
        if (_units.equals(SourceDefinitionParameters.MAG)) {
            double zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
            norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));
        } else if (_units.equals(SourceDefinitionParameters.JY)) {
            norm = _user_norm * 1.509e7 / WavebandDefinition.getCenter(_band);
        } else if (_units.equals(SourceDefinitionParameters.WATTS)) {
            norm = _user_norm * WavebandDefinition.getCenter(_band) / 1.988e-13;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_WAVELENGTH)) {
            norm = _user_norm * WavebandDefinition.getCenter(_band) / 1.988e-14;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_FREQUENCY)) {
            norm = _user_norm * 1.509e30 / WavebandDefinition.getCenter(_band);
        } else if (_units.equals(SourceDefinitionParameters.ABMAG)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) /
                    WavebandDefinition.getCenter(_band);
        } else if (_units.equals(SourceDefinitionParameters.MAG_PSA)) {
            double zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
            norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));
        } else if (_units.equals(SourceDefinitionParameters.JY_PSA)) {
            norm = _user_norm * 1.509e7 / WavebandDefinition.getCenter(_band);
        } else if (_units.equals(SourceDefinitionParameters.WATTS_PSA)) {
            norm = _user_norm * WavebandDefinition.getCenter(_band) / 1.988e-13;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_WAVELENGTH_PSA)) {
            norm = _user_norm * WavebandDefinition.getCenter(_band) / 1.988e-14;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_FREQUENCY_PSA)) {
            norm = _user_norm * 1.509e30 / WavebandDefinition.getCenter(_band);
        } else if (_units.equals(SourceDefinitionParameters.ABMAG_PSA)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) /
                    WavebandDefinition.getCenter(_band);
        } else {
            throw new Exception("Unit code " + _units + " not supported.");
        }

	//System.out.println("SED inside Normalize function:");
	//for (int i = 1500; i<1800; i++)
	//    System.out.println("X val: "+ i +" Y val: "+sed.getY(i));

        // Calculate avg flux density in chosen normalization band.
        double average = sed.getAverage(
                (double) WavebandDefinition.getStart(_band),
                (double) WavebandDefinition.getEnd(_band));
        //System.out.println("Average flux over Normail:" +average+ " Norm: "+ norm);
        // Calculate multiplier.
        double multiplier = norm / average;

	System.out.println("Waveband: " + _band);
	System.out.println("Waveband Start/End: "+WavebandDefinition.getStart(_band)+ " " + WavebandDefinition.getEnd(_band));
	System.out.println("Norm: "+norm);
	System.out.println("multiplier: "+multiplier);

        // Apply normalization, multiply every value in the SED by
        // multiplier and then its average in specified band will be
        // the required amount.
        sed.rescaleY(multiplier);
    }

    public String toString() {
        return "Normalizer - band: " + _band + " user_norm: " + _user_norm +
                " unit code: " + _units;
    }
}
