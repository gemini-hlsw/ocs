package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.*;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.SlitWidth;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This defines GNIRS IFU apertures.
 */

public class IFUComponent extends TransmissionElement {

    private static final Logger Log = Logger.getLogger(IFUComponent.class.getName());

    public static final double IFU_LR_SIZE = 0.15;  // LR-IFU aperture size (both directions, arcsec)
    public static final double IFU_HR_SIZE = 0.05;  // HR-IFU aperture size (both directions, arcsec)
    public static double ifuElementSize;

    private ApertureComposite IFUApertures;
    private List<Double> IFUOffsets;


    /**
     * Constructor for a radial set of IFU elements.
     *
     * @param slitwidth  Selects either the LR-IFU or the HR-IFU
     * @param IFURadialMin First IFU element when the radial set is selected
     * @param IFURadialMax Last IFU element when the radial set is selected
     * @throws java.lang.Exception Thrown if IFUComponent cannot be created
     */
    public IFUComponent(SlitWidth slitwidth, double IFURadialMin, double IFURadialMax) {
        super(ITCConstants.LIB + "/" + Gnirs.INSTR_DIR + "/" + Gnirs.INSTR_PREFIX + slitwidth.toString() + Instrument.DATA_SUFFIX);
        Log.fine("Constructing a radial set of IFU apertures...");

        if (slitwidth.equals(SlitWidth.LR_IFU)) {
           ifuElementSize = IFU_LR_SIZE;
        } else if (slitwidth.equals(SlitWidth.HR_IFU)) {
           ifuElementSize = IFU_HR_SIZE;
        } else {
           throw new RuntimeException("Unknown IFU");  // This should never happen
        }
        Log.fine("ifuElementSize = " + ifuElementSize);

        int Napps = new Double((IFURadialMax - IFURadialMin) / (ifuElementSize) + 1).intValue();

        if (Napps < 0) Napps = 1;

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        for (int i = 0; i < Napps; i++) {
            double Xpos = IFURadialMin + ifuElementSize * i;
            IFUApertures.addAperture(new RectangularAperture(ifuElementSize, ifuElementSize, Xpos, 0));
            IFUOffsets.add(Xpos);
        }
        Log.fine("IFUApertures = " + IFUApertures);
        Log.fine("IFUOffsets = " + IFUOffsets);
    }

    /**
     * Constructor for a rectangular set of IFU elements at an arbitrary center.
     *
     * @param slitwidth  Selects either the LR-IFU or the HR-IFU
     * @param numX       Number of IFU elements in the X direction
     * @param numY       Number of IFU elements in the Y direction
     * @param centerX    X center for the IFU elements
     * @param centerY    Y Center for the IFU elements
     * @throws java.lang.Exception Thrown if IFU cannot be created
     */
    public IFUComponent(SlitWidth slitwidth, int numX, int numY, double centerX, double centerY) {

        super(ITCConstants.LIB + "/" + Gnirs.INSTR_DIR + "/" + Gnirs.INSTR_PREFIX + slitwidth.toString() + Instrument.DATA_SUFFIX);
        Log.fine("Constructing a rectangular set of IFU apertures...");

        // Make sure there is at least one IFU element:
        numX = Math.max(numX, 1);
        numY = Math.max(numY, 1);

        double xStart;
        double yStart;

        if (slitwidth.equals(SlitWidth.LR_IFU)) {
           ifuElementSize = IFU_LR_SIZE;
        } else if (slitwidth.equals(SlitWidth.HR_IFU)) {
           ifuElementSize = IFU_HR_SIZE;
        } else {
           throw new RuntimeException("Unknown IFU");  // This should never happen
        }
        Log.fine("ifuElementSize = " + ifuElementSize);

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        xStart = centerX - Math.floor(numX / 2.0) * ifuElementSize;
        yStart = centerY - Math.floor(numY / 2.0) * ifuElementSize;

        for (int i = 0; i < numY; i++) {
            double yPos = yStart + ifuElementSize * i;
            for (int j = 0; j < numX; j++) {
                double xPos = xStart + ifuElementSize * j;
                IFUApertures.addAperture(new RectangularAperture(ifuElementSize, ifuElementSize, xPos, yPos));
                IFUOffsets.add(xPos);
                IFUOffsets.add(yPos);
            }
        }
        Log.fine("IFUOffsets = " + IFUOffsets);
    }

    /**
     * Constructor for a single IFU element at some distance from the center
     * @param slitwidth  Selects either the LR-IFU or the HR-IFU
     * @param offset     The X position of the IFU element
     * @throws java.lang.Exception Thrown if IFU cannot be created
     */
    public IFUComponent(SlitWidth slitwidth, double offset) {
        super(ITCConstants.LIB + "/" + Gnirs.INSTR_DIR + "/" + Gnirs.INSTR_PREFIX + slitwidth.toString() + Instrument.DATA_SUFFIX);
        Log.fine("Constructing a single IFU aperture...");

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        if (slitwidth.equals(SlitWidth.LR_IFU)) {
           ifuElementSize = IFU_LR_SIZE;
        } else if (slitwidth.equals(SlitWidth.HR_IFU)) {
           ifuElementSize = IFU_HR_SIZE;
        } else {
           throw new RuntimeException("Unknown IFU");  // This should never happen
        }
        Log.fine("ifuElementSize = " + ifuElementSize);

        IFUApertures.addAperture(new RectangularAperture(ifuElementSize, ifuElementSize, offset, 0));
        IFUOffsets.add(offset);
        Log.fine("IFUOffsets = " + IFUOffsets);
    }

    /**
     * Method to get the Aperture Composite
     *
     * @return Returns the composite of all apertures constructed
     */
    public ApertureComponent getAperture() {
        return IFUApertures;
    }

    /**
     * Method to get a list of the fraction of source contained in each aperture
     * defined in the aperture composite.
     *
     * @return List of source fractions
     */
    public List<Double> getFractionOfSourceInAperture() {
        return IFUApertures.getFractionOfSourceInAperture();
    }

    /**
     * Method to clear all of the source fractions from each aperture.
     */
    public void clearFractionOfSourceInAperture() {
        IFUApertures.clearFractionOfSourceInAperture();
    }

    /**
     * Get a list of each of the apertures and their offsets
     *
     * @return List of the apertures and offsets.  If the mode is single or radial then there
     * will be one offset per aperture.  If it is summed there will be two.. one for
     * X and one for Y.
     */
    public List<Double> getApertureOffsetList() {
        return IFUOffsets;
    }

    /**
     * Human readable string for optical components list.
     *
     * @return String for Optical components
     */
    public String toString() {
        return "IFU Transmission";
    }

}
