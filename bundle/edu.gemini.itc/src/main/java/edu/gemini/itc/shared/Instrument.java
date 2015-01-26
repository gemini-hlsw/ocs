package edu.gemini.itc.shared;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * The Instrument class is the class that any instrument should extend.
 * It defines the common properties of any given Instrumnet.
 * <p/>
 * The important piece of data is the _list. This is a linked list
 * that contains all of the Components that make up the instrument.
 */
public abstract class Instrument {
    public static final String DATA_SUFFIX = ITCConstants.DATA_SUFFIX;

    private final String _name;

    // The range and sampling allowed by this instrument.
    private final double _sub_start;
    private final double _sub_end;
    private final double _sampling;

    // List of Components
    private final List<TransmissionElement> _components;

    // Each Instrument adds its own background.
    private ArraySpectrum _background;
    private String _background_file_name;

    // Transformation between arcsec and pixels on detector
    private final double _plate_scale;
    // Binning is not a fixed property, can choose both x and y binning.
    // Doesn't yet support different binning in x,y.
    // Leave placeholder for y binning, but will just use x for now.
    //private int                _xBinning = 1;
    //private int                _yBinning = 1;

    private final double _pixel_size;  // _plate_scale * binning

    // read noise is independent of binning
    private final double _read_noise;  // electrons/pixel

    private final double _dark_current;

    /**
     * All instruments have data files of the same format.
     * Note that one instrument accesses two files.
     * One gives instrument info, the other has transmission curve.
     *
     * @param subdir   The subdirectory under lib where files are located
     * @param filename The filename of the instrument data file
     */
    // Automatically loads the background data.
    protected Instrument(String subdir, String filename) throws Exception {
        final String dir = ITCConstants.LIB + "/" + subdir + "/";
        // == TODO: static
        try (final Scanner in = DatFile.scan(dir + filename)) {
            _name = in.next(); // TODO: can't parse "Flamingos 2" like this, do those really need to be read from dat file?
            _sub_start = in.nextDouble();
            _sub_end = in.nextDouble();
            _sampling = in.nextDouble();
            _background_file_name = in.next();
            _plate_scale = in.nextDouble();
            _read_noise = in.nextDouble();      // electrons/pixel
            _dark_current = in.nextDouble();    // electrons/s/pixel
            _pixel_size = _plate_scale;
        }
        // === non-static
        _components = new LinkedList<>();
        _background = new DefaultArraySpectrum(dir + _background_file_name);
    }

    /**
     * Method adds the instrument background flux to the specified spectrum.
     */
    public void addBackground(ArraySpectrum sky) {
        for (int i = 0; i < sky.getLength(); i++) {
            sky.setY(i, _background.getY(sky.getX(i)) + sky.getY(i));
        }

    }

    /**
     * Method to iterate through the Components list and apply the
     * accept method of each component to a sed.
     */
    public void convolveComponents(VisitableSampledSpectrum sed) throws Exception {
        for (final TransmissionElement te : _components) {
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

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public abstract int getEffectiveWavelength();

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public abstract String getDirectory();

    /**
     * The suffix on instrument data files.
     */
    public static String getSuffix() {
        return DATA_SUFFIX;
    }

    public String opticalComponentsToString() {
        String s = "Optical Components: <BR>";
        for (final TransmissionElement te : _components) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        return s;
    }

    public String toString() {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : _components) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        s += "<BR>";
        s += "Pixel Size: " + getPixelSize() + "<BR>" + "<BR>";

        return s;
    }

    protected List<TransmissionElement> getComponents() {
        return new ArrayList<>(_components);
    }
}


