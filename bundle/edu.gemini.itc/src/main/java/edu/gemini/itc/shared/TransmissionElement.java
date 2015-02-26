package edu.gemini.itc.shared;

/**
 * A TransmissionElement has a transmission spectrum that can
 * be convolved with a spectrum.
 * Examples are water in the atmosphere and instrument filters.
 * This class can be used directly if the client knows the path
 * to the data file.  But usually a class will be derived from this
 * class that knows something about the data file naming conventions
 * for that type of element.
 */
public class TransmissionElement implements SampledSpectrumVisitor {

    private final ArraySpectrum _trans;

    /**
     * Constructs a TransmissionElement
     */
    public TransmissionElement(final ArraySpectrum transmission) {
        _trans = (ArraySpectrum) transmission.clone();
    }

    /**
     * Constructs a TransmissionElement using specified transmission data file
     */
    public TransmissionElement(final String resourceName) {
        _trans = new DefaultArraySpectrum(resourceName);
    }

    /**
     * Apply the transmission convolution for this component.
     */
    public void visit(final SampledSpectrum sed) {
        for (int i = 0; i < sed.getLength(); i++) {
            final double startval = sed.getX(i);
            final double multiplier = _trans.getY(startval);
            sed.setY(i, sed.getY(i) * multiplier);
        }
    }

    public ArraySpectrum get_trans() {
        return _trans;
    }
}
