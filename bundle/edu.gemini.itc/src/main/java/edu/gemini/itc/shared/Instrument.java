// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.


package edu.gemini.itc.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The Instrument class is the class that any instrument should extend.
 * It defines the common properties of any given Instrumnet.
 *
 * The important piece of data is the _list. This is a linked list
 * that contains all of the Components that make up the instrument.
 */
public abstract class Instrument {
    public static final String DATA_SUFFIX = ITCConstants.DATA_SUFFIX;

    private String _name;

    // The range and sampling allowed by this instrument.
    private double _sub_start;
    private double _sub_end;
    private double _sampling;

    // List of Components
    private List _components;

    // Each Instrument adds its own background.
    private ArraySpectrum _background;

    private String _background_file_name;

    // Transformation between arcsec and pixels on detector
    private double _plate_scale;
    // Binning is not a fixed property, can choose both x and y binning.
    // Doesn't yet support different binning in x,y.
    // Leave placeholder for y binning, but will just use x for now.
    //private int                _xBinning = 1;
    //private int                _yBinning = 1;

    private double _pixel_size;  // _plate_scale * binning

    // read noise is independent of binning
    private double _read_noise;  // electrons/pixel

    private double _dc_per_pixel; // electrons/s/pixel
    private double _dark_current;

    // analog to digital unit, conversion between electrons and counts.
    // not used yet, but here so we don't forget.
    private double _adu = 1.0;

    protected Instrument(String name, double sub_start, double sub_end,
                         double sampling, String background_file,
                         double plate_scale,
                         double read_noise, int dc_per_pixel) throws Exception {
        _name = name;
        _sub_start = sub_start;
        _sub_end = sub_end;
        _sampling = sampling;
        _components = new LinkedList();

        _background = new
                DefaultArraySpectrum(background_file + ITCConstants.DATA_SUFFIX);

        _plate_scale = plate_scale;
        _pixel_size = _plate_scale;//*getXBinning();
        _read_noise = read_noise;
        _dc_per_pixel = dc_per_pixel;
        _dark_current = _dc_per_pixel;//*getXBinning();
    }

    /**
     * All instruments have data files of the same format.
     * Note that one instrument accesses two files.
     * One gives instrument info, the other has transmission curve.
     * @param subdir The subdirectory under lib where files are located
     * @param filename The filename of the instrument data file
     */
    // Automatically loads the background data.
    protected Instrument(String subdir, String filename) throws Exception {
        String dir = ITCConstants.LIB + "/" + subdir + "/";
        TextFileReader in = new TextFileReader(dir + filename);

        _components = new LinkedList();
        _name = in.readLine();
        _sub_start = in.readDouble();
        _sub_end = in.readDouble();
        _sampling = in.readDouble();
        _background_file_name = in.readString();
        _background = new
                DefaultArraySpectrum(dir + _background_file_name);
        _plate_scale = in.readDouble();
        _pixel_size = _plate_scale;//*getXBinning();
        _read_noise = in.readDouble();
        _dc_per_pixel = in.readDouble();
        _dark_current = _dc_per_pixel;//*getXBinning();
    }

    /**
     * Method adds the instrument background flux to the specified spectrum.
     */
    public void addBackground(ArraySpectrum sky) {
        double val = 0;
        for (int i = 0; i < sky.getLength(); i++) {
            val = _background.getY(sky.getX(i)) + sky.getY(i);
            sky.setY(i, val);
        }

    }

    /**
     * Method to iterate through the Components list and apply the
     * accept method of each component to a sed.
     */
    public void convolveComponents(VisitableSampledSpectrum sed)
            throws Exception {
        for (Iterator itr = _components.iterator(); itr.hasNext();) {
            TransmissionElement te = (TransmissionElement) itr.next();
            sed.accept(te);
        }

    }

    protected void addComponent(TransmissionElement c) {
        _components.add(c);
    }

    // Accessor methods
    public String getName() {
        return _name;
    }

    public double getStart() {
        return _sub_start;
    }

    public double getEnd() {
        return _sub_end;
    }

    public abstract double getObservingStart();

    public abstract double getObservingEnd();

    public double getSampling() {
        return _sampling;
    }

    public double getPixelSize() {
        return _pixel_size;
    }

    public double getReadNoise() {
        return _read_noise;
    }

    public double getDarkCurrent() {
        return _dark_current;
    }

    protected void resetBackGround(String subdir, String filename_prefix) throws Exception {
        String dir = ITCConstants.LIB + "/" + subdir + "/";

        _background = new DefaultArraySpectrum(dir + filename_prefix + _background_file_name);
    }

    //public int getXBinning() {return _xBinning;}
    //public void setXBinning(int i)
    //{
    //   _xBinning = i;
    //   _pixel_size = _plate_scale * getXBinning();
    //}

    //public int getYBinning() {return _yBinning;}
    //public void setYBinning(int i)
    //{
    //   _yBinning = i;
    // eventually may have both x and y pixel size, but not now.
    //}

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     * @return Effective wavelength in nm
     */
    public abstract int getEffectiveWavelength();

    /** Returns the subdirectory where this instrument's data files are. */
    public abstract String getDirectory();

    /** The suffix on instrument data files. */
    public static String getSuffix() {
        return DATA_SUFFIX;
    }

    public String opticalComponentsToString() {
        String s = "Optical Components: <BR>";
        for (Iterator itr = _components.iterator(); itr.hasNext();) {
            s += "<LI>" + itr.next().toString() + "<BR>";
        }
        return s;
    }

    public String toString() {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (Iterator itr = _components.iterator(); itr.hasNext();) {
            s += "<LI>" + itr.next().toString() + "<BR>";
        }
        s += "<BR>";
        s += "Pixel Size: " + getPixelSize() + "<BR>" + "<BR>";

        return s;
    }

	public double get_plate_scale() {
		return _plate_scale;
	}

    protected List getComponents() {
        return new ArrayList(_components);
    }
}


