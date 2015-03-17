package edu.gemini.itc.operation;

import edu.gemini.itc.service.SourceDefinition;
import edu.gemini.itc.shared.SampledSpectrum;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.service.WavebandDefinition;
import edu.gemini.itc.shared.ZeroMagnitudeStar;

/**
 * The NormalizeVisitor class is used to perform Normalization to the SED.
 * Normalization rescales the SED so that the average flux in a
 * specified waveband is equal to a specified value.
 * This is where unit conversion happens.
 */
public class NormalizeVisitor implements SampledSpectrumVisitor {
    private final WavebandDefinition _band; // String description of waveband (A, B, R, etc.)
    private final double _user_norm; // Brightness of the object (flux) as an average
    private final SourceDefinition.BrightnessUnit _units;  // mag, abmag, ...

    /**
     * Constructs a Normalizer
     *
     * @param waveband  The waveband to normalize over
     * @param user_norm The average flux in the waveband
     * @param units     The code for the units chosen by user
     */
    public NormalizeVisitor(final WavebandDefinition waveband, final double user_norm, final SourceDefinition.BrightnessUnit units) {
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
        final double zeropoint;
        final double norm;

        // Here is where you do unit conversions.
        switch (_units) {
            case MAG:
                zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
                norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));
                break;
            case JY:
                norm = _user_norm * 1.509e7 / _band.getCenter();
                break;
            case WATTS:
                norm = _user_norm * _band.getCenter() / 1.988e-13;
                break;
            case ERGS_WAVELENGTH:
                norm = _user_norm * _band.getCenter() / 1.988e-14;
                break;
            case ERGS_FREQUENCY:
                norm = _user_norm * 1.509e30 / _band.getCenter();
                break;
            case ABMAG:
                norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) / _band.getCenter();
                break;
            case MAG_PSA:
                zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
                norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));
                break;
            case JY_PSA:
                norm = _user_norm * 1.509e7 / _band.getCenter();
                break;
            case WATTS_PSA:
                norm = _user_norm * _band.getCenter() / 1.988e-13;
                break;
            case ERGS_WAVELENGTH_PSA:
                norm = _user_norm * _band.getCenter() / 1.988e-14;
                break;
            case ERGS_FREQUENCY_PSA:
                norm = _user_norm * 1.509e30 / _band.getCenter();
                break;
            case ABMAG_PSA:
                norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) / _band.getCenter();
                break;
            default:
                throw new IllegalArgumentException("Unit code " + _units + " not supported.");
        }

        // Calculate avg flux density in chosen normalization band.
        double average = sed.getAverage(_band.getStart(), _band.getEnd());

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
