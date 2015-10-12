package edu.gemini.itc.operation;

import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.base.ZeroMagnitudeStar;
import edu.gemini.spModel.core.BrightnessUnit;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.MagnitudeSystem;
import edu.gemini.spModel.core.SurfaceBrightness;

/**
 * The NormalizeVisitor class is used to perform Normalization to the SED.
 * Normalization rescales the SED so that the average flux in a
 * specified waveband is equal to a specified value.
 * This is where unit conversion happens.
 */
public class NormalizeVisitor implements SampledSpectrumVisitor {
    private final MagnitudeBand _band;    // waveband (A, B, R, etc.)
    private final double _user_norm;      // Brightness of the object (flux) as an average
    private final BrightnessUnit _units;  // mag, abmag, ...

    /**
     * Constructs a Normalizer
     *
     * @param waveband  The waveband to normalize over
     * @param user_norm The average flux in the waveband
     * @param units     The code for the units chosen by user
     */
    public NormalizeVisitor(final MagnitudeBand waveband, final double user_norm, final BrightnessUnit units) {
        _band = waveband;
        _user_norm = user_norm;
        _units = units;
    }

    /**
     * Implements the visitor interface.
     * Performs the normalization.
     */
    public void visit(final SampledSpectrum sed) {
        // obtain normalization value
        final double norm;

        // Here is where you do unit conversions.
        if (_units.equals(MagnitudeSystem.Vega$.MODULE$) || _units.equals(SurfaceBrightness.Vega$.MODULE$)) {
            final double zeropoint = ZeroMagnitudeStar.getAverageFlux(_band);
            norm = zeropoint * (java.lang.Math.pow(10.0, -0.4 * _user_norm));

        } else if (_units.equals(MagnitudeSystem.Jy$.MODULE$) || _units.equals(SurfaceBrightness.Jy$.MODULE$)) {
            norm = _user_norm * 1.509e7 / _band.center().toNanometers();

        } else if (_units.equals(MagnitudeSystem.Watts$.MODULE$) || _units.equals(SurfaceBrightness.Watts$.MODULE$)) {
            norm = _user_norm * _band.center().toNanometers() / 1.988e-13;

        } else if (_units.equals(MagnitudeSystem.ErgsWavelength$.MODULE$) || _units.equals(SurfaceBrightness.ErgsWavelength$.MODULE$)) {
            norm = _user_norm * _band.center().toNanometers() / 1.988e-14;

        } else if (_units.equals(MagnitudeSystem.ErgsFrequency$.MODULE$) || _units.equals(SurfaceBrightness.ErgsFrequency$.MODULE$)) {
            norm = _user_norm * 1.509e30 / _band.center().toNanometers();

        } else if (_units.equals(MagnitudeSystem.AB$.MODULE$) || _units.equals(SurfaceBrightness.AB$.MODULE$)) {
            norm = 5.632e10 * java.lang.Math.pow(10, -0.4 * _user_norm) / _band.center().toNanometers();

        } else {
            throw new IllegalArgumentException("Unit code " + _units + " not supported.");
        }

        // Calculate avg flux density in chosen normalization band.
        double average = sed.getAverage(_band.start().toNanometers(), _band.end().toNanometers());

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
