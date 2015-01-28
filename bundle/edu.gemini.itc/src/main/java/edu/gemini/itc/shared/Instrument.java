package edu.gemini.itc.shared;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The Instrument class is the class that any instrument should extend.
 * It defines the common properties of any given Instrumnet.
 * <p/>
 * The important piece of data is the _list. This is a linked list
 * that contains all of the Components that make up the instrument.
 */
public abstract class Instrument {

    public static final String DATA_SUFFIX = ITCConstants.DATA_SUFFIX;

    // Instrument parameters from dat file
    private final DatFile.Instrument params;
    // List of Components
    private final List<TransmissionElement> components;
    // Each Instrument adds its own background.
    private ArraySpectrum background;

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
        params = DatFile.parseInstrument(dir + filename);
        components = new LinkedList<>();
        background = new DefaultArraySpectrum(dir + params.backgroundFile());
    }

    /**
     * Method adds the instrument background flux to the specified spectrum.
     */
    public void addBackground(ArraySpectrum sky) {
        for (int i = 0; i < sky.getLength(); i++) {
            sky.setY(i, background.getY(sky.getX(i)) + sky.getY(i));
        }
    }

    /**
     * Method to iterate through the Components list and apply the
     * accept method of each component to a sed.
     */
    public void convolveComponents(VisitableSampledSpectrum sed) throws Exception {
        for (final TransmissionElement te : components) {
            sed.accept(te);
        }
    }

    protected void addComponent(TransmissionElement c) {
        components.add(c);
    }

    // Accessor methods
    public String getName() {
        return params.name();
    }

    public double getStart() {
        return params.start();
    }

    public double getEnd() {
        return params.end();
    }

    public abstract double getObservingStart();

    public abstract double getObservingEnd();

    public double getSampling() {
        return params.sampling();
    }

    public double getPixelSize() {
        return params.plateScale(); // pixelSize = plateScale * binning
    }

    public double getReadNoise() {
        return params.readNoise();
    }

    public double getDarkCurrent() {
        return params.darkCurrent();
    }

    protected void resetBackGround(String subdir, String filename_prefix) throws Exception {
        final String dir = ITCConstants.LIB + "/" + subdir + "/";
        background = new DefaultArraySpectrum(dir + filename_prefix + params.backgroundFile());
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
        for (final TransmissionElement te : components) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        return s;
    }

    public String toString() {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (final TransmissionElement te : components) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        s += "<BR>";
        s += "Pixel Size: " + getPixelSize() + "<BR>" + "<BR>";

        return s;
    }

    protected List<TransmissionElement> getComponents() {
        return new ArrayList<>(components);
    }
}


