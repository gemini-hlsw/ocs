package edu.gemini.itc.base;

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
    public  String _file;

    private String _description="";

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
        _file = resourceName;
    }

    /**
     * Apply the transmission convolution for this component.
     */
    public void visit(final SampledSpectrum sed) {
        final int n = sed.getLength();
        if (_trans instanceof DefaultArraySpectrum) {
            final DefaultArraySpectrum.Interpolator trans = ((DefaultArraySpectrum) _trans).interpolator();
            for (int i = 0; i < n; i++) {
                sed.setY(i, sed.getY(i) * trans.at(sed.getX(i)));
            }
            return;
        }
        for (int i = 0; i < n; i++) {
            final double startval = sed.getX(i);
            final double multiplier = _trans.getY(startval);
            sed.setY(i, sed.getY(i) * multiplier);
        }
    }

    public ArraySpectrum get_trans() {
        return _trans;
    }

    public String getDescription(){
        return _description;
    }
    public void setDescription(String desc) {
        this._description = desc;
    }
    public String toString() {
        if (!_description.equals("")) {
            return _description;
        }
        else
            return "Element defined by the " + _file + " file" ;
    }
}
