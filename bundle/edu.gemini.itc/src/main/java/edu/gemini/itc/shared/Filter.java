package edu.gemini.itc.shared;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter
 * This class exists so that the client can specify a filter number
 * instead of specifying the data file name specifically.
 */
public class Filter extends TransmissionElement {
    //private static final String FILENAME = Gmos.getPrefix();
    public static final int GET_EFFECTIVE_WAVELEN_FROM_FILE = 0;
    public static final int CALC_EFFECTIVE_WAVELEN = 1;

    private String _File, _Filter;
    private List _x_values;
    private double _effectiveWavelength;
    private int _effectiveWavelengthMethod;

    public Filter(String prefix, String Filter, String dir, int effectiveWavelengthMethod) throws Exception {
        super(dir + prefix + Filter + Instrument.getSuffix());
        _Filter = Filter;
        _File = dir + prefix + Filter + Instrument.getSuffix();

        _effectiveWavelengthMethod = effectiveWavelengthMethod;

        TextFileReader dfr = new TextFileReader(_File);
        _x_values = new ArrayList();

        double x = 0;
        double y = 0;

        try {

            if (_effectiveWavelengthMethod == GET_EFFECTIVE_WAVELEN_FROM_FILE)
                _effectiveWavelength = dfr.readDouble();

            while (true) {
                x = dfr.readDouble();
                _x_values.add(new Double(x));
                y = dfr.readDouble();
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // normal eof
        }

    }

    public double getStart() {
        return ((Double) _x_values.get(2)).doubleValue();
    }

    public double getEnd() {
        return ((Double) _x_values.get(_x_values.size() - 3)).doubleValue();
    }

    // for some instruments effective wavelen will just be the mid pt of the filter
    // For Gmos this has changed the Effective wavelength is now the first
    // double value in the file.

    public double getEffectiveWavelength() {
        switch (_effectiveWavelengthMethod) {
            case GET_EFFECTIVE_WAVELEN_FROM_FILE:
                return _effectiveWavelength;
            case CALC_EFFECTIVE_WAVELEN:
                return ((Double) _x_values.get((int) _x_values.size() / 2)).doubleValue();
            default:
                return ((Double) _x_values.get((int) _x_values.size() / 2)).doubleValue();
        }
    }

    public String getFilter() {
        return _Filter;
    }

    public String toString() {
        return "Filter: " + getFilter();
    }

}
