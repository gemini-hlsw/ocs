package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.WavebandDefinition;
import edu.gemini.itc.shared.ZeroMagnitudeStar;

// for units

/**
 * The NormalizeVisitor class is used to perform Normalization to the SED.
 * Normalization rescales the SED so that the average flux in a
 * specified waveband is equal to a specified value.
 * This is where unit conversion happens.
 */
public class NormalizeVisitor implements SampledSpectrumVisitor {
    private final WavebandDefinition _band; // String description of waveband (A, B, R, etc.)
    private final double _user_norm; // Brightness of the object (flux) as an average
    private final String _units;  // mag, abmag, ...

    /**
     * Constructs a Normalizer
     *
     * @param waveband  The waveband to normalize over
     * @param user_norm The average flux in the waveband
     * @param units     The code for the units chosen by user
     */
    public NormalizeVisitor(final WavebandDefinition waveband, final double user_norm, final String units) {
        _band = waveband;
        _user_norm = user_norm;
        _units = units;
    }

    /**
     * Implements the visitor interface.
     * Performs the normalization.
     */
    public void visit(SampledSpectrum sed) {
        // obtain normalization value
        double norm = -1;

        // This is hard-wired to use the flux of a zero-magnitude star.
        // Flux is in magnitudes
        if (_units == null) throw new IllegalStateException("null units");
        // Here is where you do unit conversions.
        if (_units.equals(SourceDefinitionParameters.MAG)) {
            double zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
            norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));
        } else if (_units.equals(SourceDefinitionParameters.JY)) {
            norm = _user_norm * 1.509e7 / _band.getCenter();
        } else if (_units.equals(SourceDefinitionParameters.WATTS)) {
            norm = _user_norm * _band.getCenter() / 1.988e-13;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_WAVELENGTH)) {
            norm = _user_norm * _band.getCenter() / 1.988e-14;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_FREQUENCY)) {
            norm = _user_norm * 1.509e30 / _band.getCenter();
        } else if (_units.equals(SourceDefinitionParameters.ABMAG)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) / _band.getCenter();
        } else if (_units.equals(SourceDefinitionParameters.MAG_PSA)) {
            double zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
            norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));
        } else if (_units.equals(SourceDefinitionParameters.JY_PSA)) {
            norm = _user_norm * 1.509e7 / _band.getCenter();
        } else if (_units.equals(SourceDefinitionParameters.WATTS_PSA)) {
            norm = _user_norm * _band.getCenter() / 1.988e-13;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_WAVELENGTH_PSA)) {
            norm = _user_norm * _band.getCenter() / 1.988e-14;
        } else if (_units.equals(SourceDefinitionParameters.ERGS_FREQUENCY_PSA)) {
            norm = _user_norm * 1.509e30 / _band.getCenter();
        } else if (_units.equals(SourceDefinitionParameters.ABMAG_PSA)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) / _band.getCenter();
        } else {
            throw new IllegalArgumentException("Unit code " + _units + " not supported.");
        }

        // Calculate avg flux density in chosen normalization band.
        double average = sed.getAverage(
                (double) _band.getStart(),
                (double) _band.getEnd());

        // Calculate multiplier.
        double multiplier = norm / average;

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
