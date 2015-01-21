// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.TextFileReader;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SlitThroughput {

    private double slit_spatial_ratio, slit_spec_ratio;
    private double im_qual, pixel_size, slit_width, slit_ap;
    private double[][] _data;
    private double[][] _xy_axis_values;
    private List x_axis = new ArrayList();
    private List y_axis = new ArrayList();
    private List y_values = new ArrayList();
    private String fileName = ITCConstants.CALC_LIB +
            ITCConstants.SLIT_THROUGHPUT_FILENAME + ITCConstants.DATA_SUFFIX;

    // constructor for the optimum aperture case
    public SlitThroughput(double im_qual, double pixel_size,
                          double slit_width)
            throws Exception {
        this.slit_ap = 1.4 * im_qual;
        readSlitThroughputFile();
        _initialize(x_axis, y_axis, y_values);  // throws Exception


        //stick a copy into our private vars
        this.im_qual = im_qual;
        this.pixel_size = pixel_size;
        this.slit_width = slit_width;

    }

    //constructor for the user defined aperture case.
    public SlitThroughput(double im_qual, double user_def_ap, double pixel_size,
                          double slit_width)
            throws Exception {
        this.slit_ap = user_def_ap;
        readSlitThroughputFile();
        _initialize(x_axis, y_axis, y_values);  // throws Exception


        //stick a copy into our private vars
        this.im_qual = im_qual;
        this.pixel_size = pixel_size;
        this.slit_width = slit_width;

        //System.out.println("im"+im_qual+"uda"+user_def_ap+"pixel"+pixel_size+"sw"+slit_width);
    }


    public void readSlitThroughputFile() throws Exception {
        //set up the text file reader need to change this to read in a table.
        TextFileReader dfr = new TextFileReader(fileName);
        //	List x_values = new ArrayList();

        //	List x_axis = new ArrayList();
        //	List y_axis = new ArrayList();

        double x = 0;
        double y = 0;

        try {

            for (int i = 0; i < dfr.countTokens(); ++i) {
                x = dfr.readDouble();
                x_axis.add(new Double(x));
            }
            for (int i = 0; i < dfr.countTokens(); ++i) {
                y = dfr.readDouble();
                y_axis.add(new Double(y));

            }


        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // This is normal and happens at the end of file
        }


        try {
            while (true) {
                //x = dfr.readDouble();
                //x_values.add(new Double(x));
                y = dfr.readDouble();
                y_values.add(new Double(y));
                //System.out.print(" " + y +" ");
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            // This is normal and happens at the end of file
        }

// 	if (y_values.size() != x_values.size())
// 	    {
// 	  throw new Exception("Error in file " + fileName + ", not same number of x and y data points.");
// 	    }

        if (y_values.size() < 1) {
            throw new Exception("No values found in file " + fileName);
        }
    }

    public double getSlitThroughput() {
        // find the x value

        double spatial_pix = slit_ap / pixel_size;
        int int_spatial_pix = new Double(spatial_pix + .5).intValue();
        slit_spatial_ratio = int_spatial_pix * pixel_size / slit_width;
        // find the y value
        double sigma = im_qual / 2.355;
        slit_spec_ratio = slit_width / sigma;

        //trap large values
        if (slit_spatial_ratio > 5.5)
            slit_spatial_ratio = 5.499; //Slide in under the max

        if (slit_spec_ratio > 8)
            return 1;
        // Do a 2D interpolation to finde the return value using x= slit_spatial_ratio and y= slit_spec_ratio

        //double slitThroughput = getSTvalue(slit_spec_ratio,slit_spatial_ratio);  //Correct version
        double slitThroughput = getSTvalue(slit_spatial_ratio, slit_spec_ratio);
        //double slitThroughput = getSTvalue(8,4.1);
        //System.out.println("Bilinear interp: x:"+ slit_spec_ratio+ " y: " +slit_spatial_ratio+ " slitTP: " +slitThroughput);

        // Do a 2D interpolation to finde the return value using x= slit_patial_ratio and y= slit_spec_ratio
        if (slitThroughput > 1.0) return 1;
        else return slitThroughput;
    }

    public double getSpatialPix() {
        //double slit_ap = 1.4* im_qual;
        double spatial_pix =
                new Integer(new Double(slit_ap / pixel_size + 0.5).intValue()
                ).doubleValue();
        // System.out.println("SPatial pix: " +slit_ap/pixel_size+ " " + spatial_pix);
        return spatial_pix;
    }
    //  public double getUSBSlitThroughput() {
    //    }

    public double getSlitSpatialRatio() {
        return slit_spatial_ratio;
    }

    public double getSlitSpecRatio() {
        return slit_spec_ratio;
    }

    private void _initialize(List x_axis, List y_axis, List y_values) throws Exception {
        if ((x_axis.size() * y_axis.size()) != y_values.size()) {
            throw new Exception("SlitThroughput data invalid, " + x_axis.size() * y_axis.size()
                    + " x*y axis size " + y_values.size()
                    + " y values");
        }
        try {
            _data = new double[y_axis.size()][x_axis.size()];
            for (int j = 0; j < y_axis.size(); j++) {
                for (int i = 0; i < x_axis.size(); i++) {
                    _data[j][i] = ((Double) y_values.get((i) + ((x_axis.size()) * j))).doubleValue();
                    //System.out.print(" " + _data[j][i] +" ");
                }
                //System.out.println("");
            }

            //  for (int i = 0; i < x_axis.size(); ++i) {
            //	for (int j = 0; j < y_axis.size(); ++j) {
            //	    _data[j][i] = ((Double)y_values.get((j)+(y_axis.size()*i))).doubleValue();
            //	System.out.print(" " + _data[j][i] +" ");
            //}
            //System.out.println("");
            // }
        } catch (Exception e) {
            throw new Exception("Slit Throughput data invalid");
        }
    }

    /**
     * @return y value at specified x using linear interpolation.
     * Silently returns zero if x is out of spectrum range.
     */
    public double getSTvalue(double x, double y) {
        //System.out.println("Hi x: " +x+" : "+ getStartX()+" : "+ getEndX()+"  y:"+ y+" : "+getStartY()+" : " +getEndY()  );
        if (x < getStartX() || y < getStartY()) {
            return 0;
        }
        if (y > getEndY()) return 1;

        int low_index_x = getLowerXIndex(x);
        int low_index_y = getLowerYIndex(y);

        int high_index_x;
        int high_index_y;
        if (low_index_y == getYAxisSize() - 1) {
            return 1.0;
        } else if (low_index_x == getXAxisSize() - 1) {
            return getValue(low_index_x, low_index_y) * (y / getYAxisValue(low_index_y));
            //high_index_x = low_index_x;
            //high_index_y = low_index_y;
// 	    low_index_x--;
// 	    low_index_y--;
        } else {
            high_index_x = low_index_x + 1;
            high_index_y = low_index_y + 1;
        }
        //System.out.println("Lowx"+low_index_x+" HX " + high_index_x+" lowy "+ low_index_y + " HY " + high_index_y);
        double y1 = getValue(low_index_x, low_index_y);
        double y2 = getValue(high_index_x, low_index_y);
        double y3 = getValue(high_index_x, high_index_y);
        double y4 = getValue(low_index_x, high_index_y);

        //System.out.println(" y vals" + y1 + " " + y2 + "  "+ y3+" " + y4);
        double t = (x - getXAxisValue(low_index_x)) / (getXAxisValue(high_index_x) - getXAxisValue(low_index_x));
        double u = (y - getYAxisValue(low_index_y)) / (getYAxisValue(high_index_y) - getYAxisValue(low_index_y));
        //double slope = (y2 - y1) / (x2 - x1);
        //System.out.println(" t and u " + t + " " + u);
        return ((1.0 - t) * (1.0 - u) * y1 + t * (1.0 - u) * y2 + t * u * y3 + (1.0 - t) * u * y4);
    }

    /**
     * @return starting x value
     */
    public double getStartX() {
        return ((Double) x_axis.get(0)).doubleValue();
    }

    /**
     * @return ending x value
     */
    public double getEndX() {
        return ((Double) x_axis.get(x_axis.size() - 1)).doubleValue();
    }

    public double getXAxisValue(int index) {
        return ((Double) x_axis.get(index)).doubleValue();
    }

    public double getYAxisValue(int index) {
        return ((Double) y_axis.get(index)).doubleValue();
    }

    public int getXAxisSize() {
        return x_axis.size();
    }

    public int getYAxisSize() {
        return y_axis.size();
    }

    /**
     * @return starting y value
     */
    public double getStartY() {
        return ((Double) y_axis.get(0)).doubleValue();
    }

    /**
     * @return ending y value
     */
    public double getEndY() {
        return ((Double) y_axis.get(y_axis.size() - 1)).doubleValue();
    }

    /**
     * Returns x value of specified data point.
     */
    public double getValue(int index_x, int index_y) {
        return _data[index_y][index_x];
    }

    /** Returns y value of specified data point. */
    // public double getY(int index) {return _data[1][index];}


    /**
     * Returns the index of the data point with largest x value less than x
     */
    public int getLowerXIndex(double x) {
        // x value is in.  The only solution is to search for it.
        // Small amt of data so just walk through.

        int low_index = 0;
        for (int i = 0; i < x_axis.size(); ++i) {
            if (x >= (((Double) x_axis.get(i)).doubleValue()))
                low_index = i;
            //  System.out.println("x :" + x + " XAvalue: " + (((Double)x_axis.get(i)).doubleValue()) + " i: " + i + " lowin: " +low_index);
        }

//       int high_index = _data[0].length;
//       if (high_index - low_index <= 1) return low_index;
//       while (high_index - low_index > 1) {
// 	 int index = (high_index + low_index) / 2;
// 	 if (getX(index) < x) low_index = index;
// 	 else high_index = index;
//       }
        return low_index;
    }

    public int getLowerYIndex(double x) {
        // x value is in.  The only solution is to search for it.
        // Small amt of data so just walk through.
        int low_index = 0;
        for (int i = 0; i < y_axis.size(); ++i) {
            if (x >= (((Double) y_axis.get(i)).doubleValue()))
                low_index = i;
        }
        return low_index;
    }


}
