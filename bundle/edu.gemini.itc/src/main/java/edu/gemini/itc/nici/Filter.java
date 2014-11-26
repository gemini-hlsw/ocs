// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
// $Id: Filter.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.nici;

import edu.gemini.itc.shared.TextFileReader;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.Instrument;

import java.util.List;
import java.util.ArrayList;

import java.text.ParseException;

import java.io.IOException;

/**
 * Filter
 * This class exists so that the client can specify a filter number
 * instead of specifying the data file name specifically.
 */
public class Filter extends TransmissionElement {
    private static final String FILENAME = Nici.getPrefix();
    private String _File, _Filter;
    private List _x_values;
    
    /**
     * @param ndFilter Should be one of <ul>
     * <li> clear
     * <li> NDa </li>
     * <li> NDb </li>
     * <li> NDc </li>
     * <li> NDd </li>
     * </ul>
     */
    public Filter(String Filter, String dir) throws Exception {
        super(dir + FILENAME + Filter + Instrument.getSuffix());
        _Filter = Filter;
        _File = dir + FILENAME + Filter + Instrument.getSuffix();
        
        
        TextFileReader dfr = new TextFileReader(_File);
        _x_values = new ArrayList();
        
        double x = 0;
        double y = 0;
        
        try {
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
    
    // for right now effective wavelen will just be the mid pt of the filter
    
    public double getEffectiveWavelength() {
        return ((Double) _x_values.get((int) _x_values.size() / 2)).doubleValue();
    }
    
    public String getFilter() {
        return _Filter;
    }
    
    public String toString() {
        return "Filter: " + getFilter() + "\n";
    }
    
}
