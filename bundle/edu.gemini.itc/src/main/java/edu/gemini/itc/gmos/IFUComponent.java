package edu.gemini.itc.gmos;

import edu.gemini.itc.base.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is the specific setup for the IFU of GMOS.
 * It uses Hexagonal Apertures that can be in a line radialy
 * from an arbitrary point in a gaussian.
 */

public final class IFUComponent extends TransmissionElement {

    public static final double IFU_DIAMETER = 0.186;  //earlier calc showed D = 0.206
    public static final double IFU_SPACING = 0.014;
    private final ApertureComposite IFUApertures;
    private final List<Double> IFUOffsets;
    private static final Logger Log = Logger.getLogger( IFUComponent.class.getName() );

    /**
     * Constructor for a user defined radial ifu set.
     */

    public IFUComponent(final String prefix, final double IFURadialMin, final double IFURadialMax) {
        super(ITCConstants.LIB + "/" + Gmos.INSTR_DIR + "/" + prefix + "ifu_trans" + Instrument.DATA_SUFFIX);

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        int Napps = new Double((IFURadialMax - IFURadialMin) / (IFU_DIAMETER + IFU_SPACING) + 1).intValue();
        if (Napps < 0) Napps = 1;
        Log.fine("Number of apertures & offsets = " + Napps);

        for (int i = 0; i < Napps; i++) {
            final double Xpos = IFURadialMin + (IFU_DIAMETER + IFU_SPACING) * i;
            IFUApertures.addAperture(new HexagonalAperture(Xpos, 0, IFU_DIAMETER));
            IFUOffsets.add(Xpos);
        }
        Log.fine("IFU Offsets = " + IFUOffsets);
    }

    public IFUComponent(final String prefix, final double IFUOffsetX) {
        super(ITCConstants.LIB + "/" + Gmos.INSTR_DIR + "/" + prefix + "ifu_trans" + Instrument.DATA_SUFFIX);

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        IFUApertures.addAperture(new HexagonalAperture(IFUOffsetX, 0, IFU_DIAMETER));
        IFUOffsets.add(IFUOffsetX);
    }

    /**
     * Constructor for a user-defined centered circular aperture set.
     *
     * @param  radius       Radius of summation
     * @throws java.lang.Exception Thrown if IFU cannot be created
     */

    public IFUComponent(final String prefix, final double radius, final Boolean isIfu2) {
        super(ITCConstants.LIB + "/" + Gmos.INSTR_DIR + "/" + prefix + "ifu_trans" + Instrument.DATA_SUFFIX);
        Log.fine("Calculating IFU elements to sum within " + radius + " arcsec of center (IFU2 = " + isIfu2 + ")");

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        int numY = 25;            // number of elements in the IFU in the y-direction
        int numX = 20;            // number of elements in the IFU-1 in the x-direction
        if (isIfu2) numX = 40;    // number of elements in the IFU-2 in the x-direction

        int Nelements = 0;
        double distX = (Math.sqrt(3)/2.0)*IFU_DIAMETER + IFU_SPACING;
        double distY = (IFU_DIAMETER + IFU_SPACING)/2.0;
        for (int i = 0; i < numX; i++) {
            double x = (i - (numX/2.0)) * distX;
            for (int j = 0; j < numY; j++) {
                double y = (2*j - (numY - Math.abs(i)%2 + 1)) * distY;
                double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                if (r < radius) {
                    IFUApertures.addAperture(new HexagonalAperture(x, y, IFU_DIAMETER));
                    IFUOffsets.add(r);
                    Nelements++;
                }
            }
        }
        Log.fine("-> will sum " + Nelements + " IFU elements");

    }

    public ApertureComponent getAperture() {
        return IFUApertures;
    }

    public List<Double> getFractionOfSourceInAperture() {
        return IFUApertures.getFractionOfSourceInAperture();
    }

    public List<Double> getApertureOffsetList() {
        return IFUOffsets;
    }

    public String toString() {
        return "IFU Transmission";
    }

}
