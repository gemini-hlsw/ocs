// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ObsCatalogQueryArgs.java 6987 2006-05-01 18:05:34Z shane $

package jsky.app.ot.gemini.obscat;

import jsky.app.ot.shared.gemini.obscat.ObsCatalogInfo;
import jsky.catalog.*;
import jsky.util.NameValue;

import java.util.ArrayList;
import java.util.List;


/**
 * Adds instrument specific parameters to the basic query arguments.
 *
 * @author Allan Brighton
 */
public final class ObsCatalogQueryArgs extends BasicQueryArgs {

    // The names of the selected instruments, or null if none were selected
    private String[] _instruments;

    // Array of instrument specific parameters, for each selected instrument
    private FieldDesc[][] _instParams;

    // Array of parameter values for each selected instrument, corresponding to the
    // instrument specific catalog parameters
    private Object[][] _instValues;


    /**
     * Constructor: Initialize null instrument specific values.
     */
    public ObsCatalogQueryArgs(ObsCatalog catalog) {
        super(catalog);
    }

    /**
     * Get the value of the named parameter as a String Array (cast from Object[]).
     *
     * @param label the parameter label
     * @return array of parameter values
     */
    public String[] getParamValueAsStringArray(String label) {
        final Object pv = getParamValue(ObsCatalogInfo.INSTRUMENT);
        if (!(pv instanceof Object[])) return null;

        final Object[] objAr = (Object[]) pv;
        final String[] ar = new String[objAr.length];
        for (int i = 0; i < objAr.length; i++) {
            final Object o = objAr[i];
            if (o instanceof NameValue) {
                ar[i] = ((NameValue) o).getValue().toString();
            } else {
                ar[i] = o.toString();
            }
        }
        return ar;
    }

    /**
     * Return the names of the selected instruments, or null if none are selected
     */
    public String[] getInstruments() {
        return _instruments;
    }

    /**
     * Set the names of the selected instruments, or null if none are selected
     */
    public void setInstruments(String[] instruments) {
        _instruments = instruments;
        if (_instruments != null) {
            _instParams = new FieldDesc[_instruments.length][];
            _instValues = new Object[_instruments.length][];
            for (int i = 0; i < _instruments.length; i++) {
                for (int j = 0; j < ObsCatalogInfo.INSTRUMENTS.length; j++) {
                    if (_instruments[i].equals(ObsCatalogInfo.INSTRUMENTS[j])) {
                        _instParams[i] = ObsCatalog.getInstrumentParamDesc(_instruments[i]);
                        _instValues[i] = new Object[_instParams[i].length];
                        break;
                    }
                }
            }
        }
    }


    /**
     * Set the value for the given instrument parameter.
     *
     * @param inst  the instrument name
     * @param index the parameter index (in ObsCatalog.getInstrumentParamDesc())
     * @param value the value the user entered or selected in the widget
     */
    public void setInstParamValue(String inst, int index, Object value) {
        if (_instruments != null) {
            for (int i = 0; i < _instruments.length; i++) {
                if (_instruments[i].equals(inst)) {
                    _instValues[i][index] = value;
                    break;
                }
            }
        }
    }

    /**
     * Return an array of instrument specific SearchCondition objects, for each instrument,
     * indicating the values or range of values to search for.
     *
     * @return an array indexed by [selected inst index][inst parameter index]
     */
    public SearchCondition[][] getInstConditions() {

        if (_instValues == null)
            return null;

        final SearchCondition[][] result = new SearchCondition[_instruments.length][];
        for (int i = 0; i < _instruments.length; i++) {
            int n = _instParams[i].length;
            final List<SearchCondition> v = new ArrayList<SearchCondition>(n);
            for (int j = 0; j < n; j++) {
                if (_instValues[i][j] != null) {
                    final FieldDesc p = _instParams[i][j];
                    if (_instValues[i][j] instanceof ValueRange) {
                        final ValueRange r = (ValueRange) _instValues[i][j];
                        v.add(new RangeSearchCondition(p, r.getMinValue(), r.isMinInclusive(),
                                r.getMaxValue(), r.isMaxInclusive()));
                    } else if (_instValues[i][j] instanceof Object[]) {
                        final Object[] nvArray = (Object[]) _instValues[i][j];
                        final String[] strArray = new String[nvArray.length];
                        for (int k = 0; k < nvArray.length; ++k) {
                            strArray[k] = ((NameValue) nvArray[k]).getValue().toString();
                        }
                        v.add(new ArraySearchCondition(p, strArray));
//                    } else if (_instValues[i][j] instanceof Object[]) {
//                        v.add(new ArraySearchCondition(p, (Object[]) _instValues[i][j]));
                    } else if (_instValues[i][j] instanceof Comparable) {
                        v.add(new ValueSearchCondition(p, (Comparable) _instValues[i][j]));
                    }
                }
            }

            // convert result vector to array for return
            n = v.size();
            if (n == 0) {
                result[i] = new SearchCondition[0];
            } else {
                result[i] = new SearchCondition[n];
                v.toArray(result[i]);
            }
        }

        return result;
    }
}

