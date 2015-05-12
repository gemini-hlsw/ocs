package edu.gemini.itc.base;

/**
 * Filter
 */
public final class Filter extends TransmissionElement {
    private final String _filter;
    private final double _effectiveWavelength;

    public static Filter fromFile(final String prefix, final String filter, final String dir) {
        final String file = dir + prefix + filter + Instrument.getSuffix();
        final double[][] data = DatFile.arrays().apply(file);
        final double wl = data[0][data[0].length / 2];
        return new Filter(filter, data, wl);
    }

    public static Filter fromWLFile(final String prefix, final String filter, final String dir) {
        final String file = dir + prefix + filter + Instrument.getSuffix();
        final DatFile.Filter f = DatFile.filters().apply(file);
        return new Filter(filter, f.data(), f.wavelength());
    }

    private Filter(final String filter, final double[][] data, final double wl) {
        super(new DefaultArraySpectrum(data));
        _filter = filter;
        _effectiveWavelength = wl;
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
        return "Filter: " + _filter;
    }

}
