// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

// $Id: NumericIntegration.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

interface Integrable {
    double function(double x);
}

/**
 * This class is used as a utility to create a table of numerical
 * integration results.  Just recode the function() method and
 * code up the limits of integration and the interval in main(),
 * recompile and run.
 */
public class NumericIntegration {
    public final static int NUM_INTERVALS = 250;

    public static void main(String[] args) {
        /** This is the function to be integrated. */
        Integrable gaussian = new Integrable() {
            // Could separate this out into an interface and have clients
            // pass an instance of the interface that calculates the function.
            public double function(double x) {
                // Here is a gaussian.
                // Let mu = 0 (choose mean to be zero)
                // Through variable substitution, sigma drops out.
                // (x is a fraction of sigma)
                return Math.exp(-x * x / 2);
            }
        };

        double increment = .5;
        for (double x = 0; x < 6; x += increment) {
            double integral = trapezoidIntegration(-x / 2, x / 2, NUM_INTERVALS, gaussian);
            // Factor out 1/sqrt(2PI)
            //integral /= Math.sqrt(2 * Math.PI);
            integral *= integral;
            integral /= (2 * Math.PI);
            System.out.println("x: " + x + "   integral: " + integral);
        }

        Integrable rgaussian = new Integrable() {
            // Could separate this out into an interface and have clients
            // pass an instance of the interface that calculates the function.
            public double function(double x) {
                // Here is a gaussian.
                // Let mu = 0 (choose mean to be zero)
                // Through variable substitution, sigma drops out.
                // (x is a fraction of sigma)
                return Math.exp(-x * x / 2) * x;
            }
        };
        System.out.println("");
        for (double x = 0; x < 12; x += increment) {
            double integral = trapezoidIntegration(0, x / 2, NUM_INTERVALS, rgaussian);
            // Factor out 1/sqrt(2PI)
            //integral /= Math.sqrt(2 * Math.PI);
            //integral *= Math.sqrt(2 * Math.PI);
            System.out.println("x: " + x + "   integral: " + integral);
        }
    }

    /** Integrate with primitive trapezoid method. */
    public static double trapezoidIntegration(double xStart, double xEnd,
                                              int numIntervals, Integrable integrand) {
        // let fi = function evaluated at ith sampling
        // I = [(f0+f1)/2 + (f1+f2)/2 + ... + (fn-1 + fn)/2 ] * interval
        // collect terms
        // I = [f0/2 + f1 + f2 + ... + fn-1 + fn/2] * interval
        double interval = (xEnd - xStart) / numIntervals;
        double area = 0.0;
        area += integrand.function(xStart) / 2.0;
        for (int i = 1; i < numIntervals; ++i) {
            area += integrand.function(i * interval + xStart);
        }
        area += integrand.function(xEnd) / 2.0;
        area *= interval;
        return area;
    }
}


