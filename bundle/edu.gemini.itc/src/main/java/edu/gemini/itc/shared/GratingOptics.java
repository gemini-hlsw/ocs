package edu.gemini.itc.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Base class for all grating optics elements.
 */
public abstract class GratingOptics extends TransmissionElement {

    protected static final class GratingData {
        final public String name;
        final public int    resolvingPower;
        final public int    blaze;
        final public double dispersion;
        final public double resolution;
        GratingData(final String name, final int resolvingPower, final int blaze, final double dispersion, final double resolution) {
            this.name           = name;
            this.resolvingPower = resolvingPower;
            this.blaze          = blaze;
            this.dispersion     = dispersion;
            this.resolution     = resolution;
        }
    }
    // TODO: Instead of using fixed positions in this list provided by
    // TODO: getGratingNumber() this should be changed to use a lookup by name.
    private static final HashMap<String, List<GratingData>> gratings = new HashMap<>();
    private static List<GratingData> loadData(final String file) {
        final List<GratingData> data = new ArrayList<>();
        try (final Scanner scan = DatFile.scan(file)) {
            while (scan.hasNext()) {
                final String name        = scan.next();
                final int blaze          = scan.nextInt();
                final int resolvingPower = scan.nextInt();
                final double resolution  = scan.nextDouble();
                final double dispersion  = scan.nextDouble();
                data.add(new GratingData(name, resolvingPower, blaze, dispersion, resolution));
            }
        }
        return data;
    }
    private synchronized static List<GratingData> getData(final String file) {
        if (!gratings.containsKey(file)) {
            gratings.put(file, loadData(file));
        }
        return gratings.get(file);
    }

    protected final String gratingName;
    protected final double centralWavelength;
    protected final int detectorPixels;
    protected final int _spectralBinning;
    protected final List<GratingData> data;

    public GratingOptics(final String directory,
                         final String gratingName,
                         final String gratingsName,
                         final double centralWavelength,
                         final int detectorPixels,
                         final int spectralBinning) throws Exception {

        super(directory + gratingName + Instrument.getSuffix());

        final String file = directory + gratingsName + Instrument.getSuffix();
        this.data = getData(file);
        this.gratingName = gratingName;
        this._spectralBinning = spectralBinning;
        this.detectorPixels = detectorPixels;
        this.centralWavelength = centralWavelength;
    }

    protected abstract int getGratingNumber();

    public double getStart() {
        return centralWavelength - (data.get(getGratingNumber()).dispersion * detectorPixels / 2);
    }

    public double getEnd() {
        return centralWavelength + (data.get(getGratingNumber()).dispersion * detectorPixels / 2);
    }

    public double getEffectiveWavelength() {
        return centralWavelength;
    }

    public double getPixelWidth() {
        return data.get(getGratingNumber()).dispersion * _spectralBinning;

    }

    public double getGratingResolution() {
        return data.get(getGratingNumber()).resolution;
    }

    public double getGratingBlaze() {
        return data.get(getGratingNumber()).blaze;
    }

    public double getGratingDispersion_nm() {
        return data.get(getGratingNumber()).dispersion;
    }

    public double getGratingDispersion_nmppix() {
        return data.get(getGratingNumber()).dispersion * _spectralBinning;
    }

    public String toString() {
        return "Grating Optics: " + gratingName;
    }

}
