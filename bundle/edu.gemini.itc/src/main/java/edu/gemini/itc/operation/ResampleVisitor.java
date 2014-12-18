// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: ResampleVisitor.java,v 1.3 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SampledSpectrum;

/**
 * A resampling operation can move the start or end of the spectrum
 * and change the sampling wavelength interval.
 * The start and end can not be moved beyond the original limits
 * because there would be no data.
 *
 * Motivation of this operation is to "save time and improve performance"
 * quoting from Phil's demo ITC document.
 */
public class ResampleVisitor implements SampledSpectrumVisitor {
    private double _start;     // starting wavelength
    private double _end;       // ending wavelength
    private double _sampling;  // wavelength sampling interval

    public ResampleVisitor(double start, double end, double sampling) {
        _start = start;
        _end = end;
        _sampling = sampling;
    }

    /** @return sampling interval */
    public double getSampling() {
        return _sampling;
    }

    /** @return wavelength start */
    public double getStart() {
        return _start;
    }

    /** @return wavelength end */
    public double getEnd() {
        return _end;
    }

    /**
     * This method performs the resampling manipulation on the SED.
     */
    public void visit(SampledSpectrum sed) throws Exception {
        if (getStart() < sed.getStart()) {
            throw new Exception("Resampling start " + getStart() + " is before "
                                + " SED start " + sed.getStart());
        }
        if (getEnd() > sed.getEnd()) {
            throw new Exception("Resampling end " + getEnd() + " is after "
                                + " SED end " + sed.getEnd());
        }
        int num_elements = (int) ((getEnd() - getStart()) / getSampling());//+ 1;
        

        // Sed is going to get a new array
        double[] data = new double[num_elements];

        int startIndex = sed.getLowerIndex(getStart());
        if (sed.getSampling() == getSampling() &&
                sed.getX(startIndex) == getStart()) {
            // SED already has proper sampling interval and an interval starts
            // exactly on getStart().
            // Avoid interpolation and just copy array values.
            //System.out.println("resampling is copy operation" );
            for (int i = 0; i < num_elements; i++) {
                data[i] = sed.getY(i + startIndex);
            }
        } else {
            // Loop to go assign values to each element of the new array
            //System.out.println("resampling requires interpolation" );
            for (int i = 1; i < num_elements; i++) {
                // interpolate new values
                data[i] = sed.getAverage(getStart() + i * getSampling()-getSampling()/2, getStart() + i * getSampling()+ getSampling()/2);///(getSampling()/sed.getSampling()+1);
                //System.out.println("point:" + sed.getY(getStart() + i * getSampling())+ "next: "+ sed.getY(getStart() + i * getSampling()+ getSampling()) + "INT: " + data[i]);

            }
        }
        
        sed.reset(data, getStart(), getSampling());
    }

    /** @return Human-readable representation of this class. */
    public String toString() {
        String s = "ResampleVisitor - starting wavelength: ";
        s += getStart() + " sampling interval: " + getSampling();
        return s;
    }
}
