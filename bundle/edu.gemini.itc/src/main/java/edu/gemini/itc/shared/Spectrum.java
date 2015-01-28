package edu.gemini.itc.shared;

/**
 * This interface represents a 2-D spectrum.  The x and y axes are the
 * real numbers (doubles).  There is no implication as to how the data
 * is stored, however the spectrum is a function (not multivalued).
 * This spectrum is "read-only".  There are no setters.
 */
public interface Spectrum extends Cloneable {
    /**
     * Overrides clone() to be public()
     */
    Object clone();

    /**
     * @return starting x value
     */
    double getStart();

    /**
     * @return ending x value
     */
    double getEnd();

    /**
     * @return y value at specified x using linear interpolation.
     */
    double getY(double x);

    /**
     * Returns the sum of entire spectrum.
     */
    double getSum();

    /**
     * Returns the integral of entire spectrum.
     */
    double getIntegral();

    /**
     * Returns the average of entire spectrum.
     */
    double getAverage();

    /**
     * Returns the Sum of y values in the spectrum in
     * the specified range.
     */
    double getSum(double x_start, double x_end);

    /**
     * Returns the average of values in the Spectrum in
     * the specified range.
     */
    double getAverage(double x_start, double x_end);
}
