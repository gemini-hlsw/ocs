package edu.gemini.itc.shared;

/**
 * Filter
 */
public class Filter extends TransmissionElement {
    public static final int GET_EFFECTIVE_WAVELEN_FROM_FILE = 0;
    public static final int CALC_EFFECTIVE_WAVELEN = 1;

    private final String _File, _Filter;
    private final double _effectiveWavelength;

    public Filter(String prefix, String Filter, String dir, int effectiveWavelengthMethod) throws Exception {
        super(dir + prefix + Filter + Instrument.getSuffix());
        _Filter = Filter;
        _File = dir + prefix + Filter + Instrument.getSuffix();

        if (effectiveWavelengthMethod == GET_EFFECTIVE_WAVELEN_FROM_FILE) {
            // TODO: the file is now read twice (one more time in the constructor of the base class)
            // we read only the very first double here (which is the effective wavelength)
            TextFileReader dfr = new TextFileReader(_File);
            _effectiveWavelength = dfr.readDouble();
        } else {
            _effectiveWavelength = get_trans().getX(get_trans().getLength() / 2);
        }

    }

    public double getStart() {
        return get_trans().getX(2);
    }

    public double getEnd() {
        return get_trans().getX(get_trans().getLength() - 3);
    }

    // for some instruments effective wavelen will just be the mid pt of the filter
    // For Gmos this has changed the Effective wavelength is now the first
    // double value in the file.
    public double getEffectiveWavelength() {
        return _effectiveWavelength;
    }

    public String toString() {
        return "Filter: " + _Filter;
    }

}
