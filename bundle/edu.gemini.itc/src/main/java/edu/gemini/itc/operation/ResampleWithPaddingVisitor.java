package edu.gemini.itc.operation;

import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Timestamp;

/**
 * A resampling operation can move the start or end of the spectrum
 * and change the sampling wavelength interval.
 * In this resampling visitor the start and end can be moved beyond the
 * original limits. It will just pad the new data with a user specified
 * Value.
 * <p/>
 * Motivation of this operation is to "save time and improve performance"
 * quoting from Phil's demo ITC document.
 */
public class ResampleWithPaddingVisitor implements SampledSpectrumVisitor {
    private double _start;     // starting wavelength
    private double _end;       // ending wavelength
    private double _sampling;  // wavelength sampling interval
    private double _padding;

    public ResampleWithPaddingVisitor(double start, double end, double sampling,
                                      double padding) {
        _start = start;
        _end = end;
        _sampling = sampling;
        _padding = padding;

    }

    /**
     * @return sampling interval
     */
    public double getSampling() {
        return _sampling;
    }

    /**
     * @return wavelength start
     */
    public double getStart() {
        return _start;
    }

    /**
     * @return wavelength end
     */
    public double getEnd() {
        return _end;
    }

    /**
     * @return padding value
     */
    public double getPadding() {
        return _padding;
    }

    /**
     * This method performs the resampling manipulation on the SED.
     */
    public void visit(SampledSpectrum sed) {

        int num_elements = (int) ((getEnd() - getStart()) / getSampling());//+ 1;

        // Sed is going to get a new array
        double[] data = new double[num_elements];

        //System.out.println("sed.getStart(): " + sed.getStart() + "  getStart: " +  getStart() + " sed.getEnd(): " + sed.getEnd() + " getEnd(): " + getEnd());
        // If No padding is needed revert back to the old code for
        // resample visitor.
        if ((sed.getStart() <= getStart()) && (sed.getEnd() >= getEnd())) {
            int startIndex = sed.getLowerIndex(getStart());

            //System.out.println("sed.getSampling(): " + sed.getSampling() + "  getSampling(): " +  getSampling() + " sed.getX(startIndex): " + sed.getX(startIndex) + " getStart(): " + getStart());

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
                //System.out.println("resampling requires interpolation " +  num_elements);
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                //String fileName = "ResampleWithPadding_" + timestamp.toString().replace(' ', '_') +".dat";
                try {
                    //BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                    for (int i = 1; i < num_elements; i++) {
                        // interpolate new values
                        //data[i] = sed.getY(getStart() + i * getSampling());
                        //data[i] = sed.getSum(getStart() + i * getSampling(), getStart() + i * getSampling()+ getSampling())/(getSampling()/sed.getSampling()+1);

                        data[i] = sed.getAverage(getStart() + i * getSampling() - getSampling(), getStart() + i * getSampling() + getSampling());

                        /*writer.append(i + "\txstar: " + (getStart() + i * getSampling() - getSampling())
                                         + "\txEnd: " + (getStart() + i * getSampling() + getSampling())
                                         + "\tinitInt: "+ sed.getLowerIndex((getStart() + i * getSampling() - getSampling()))
                                         + "\tendInt: "+ sed.getLowerIndex(getStart() + i * getSampling() + getSampling())
                                         + "\tnewVal: " + data[i] + "\n");

                         */

                    }
                    //writer.close();
                }
                catch (Exception e) {
                    //System.out.println("error creating the file: " + fileName);
                }

                    //System.out.println("point:" + sed.getY(getStart() + i * getSampling())+ "next: "+ sed.getY(getStart() + i * getSampling()+ getSampling()) + "INT: " + data[i]);



            }
        } else {

            for (int i = 1; i < num_elements; i++) {
                // if in the range of the sed use those vals
                if ((sed.getStart() <= (getStart() + i * getSampling() - getSampling())) &&
                        (sed.getEnd() >= (getStart() + i * getSampling() + getSampling())))
                //data[i] = sed.getY(getStart() + i * getSampling());
                {
                    if (sed.getSampling() != getSampling())
                        // data[i] = sed.getSum(getStart() + i * getSampling(), getStart() + i * getSampling()+ getSampling())/(getSampling()/sed.getSampling()+1);
                        data[i] = sed.getAverage(getStart() + i * getSampling() - getSampling(), getStart() + i * getSampling() + getSampling());
                    else
                        data[i] = sed.getY(getStart() + i * getSampling());
                }//-getSampling());
                //System.out.println("point:" + sed.getY(getStart() + i * getSampling())+ "next: "+ sed.getY(getStart() + i * getSampling()+ getSampling()) + "INT: " + data[i]);
                else  //else use padding val
                    data[i] = getPadding();
            }
        }


        sed.reset(data, getStart(), getSampling());
    }

    /**
     * @return Human-readable representation of this class.
     */
    public String toString() {
        String s = "ResampleVisitor - starting wavelength: ";
        s += getStart() + " sampling interval: " + getSampling();
        return s;
    }
}
