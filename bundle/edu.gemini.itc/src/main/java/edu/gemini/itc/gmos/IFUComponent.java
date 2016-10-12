package edu.gemini.itc.gmos;

import edu.gemini.itc.base.*;

import java.util.ArrayList;
import java.util.List;

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


    /**
     * Constructor for a user defined radial ifu set.
     */

    public IFUComponent(final String prefix, final double IFURadialMin, final double IFURadialMax) {
        super(ITCConstants.LIB + "/" + Gmos.INSTR_DIR + "/" + prefix + "ifu_trans" + Instrument.DATA_SUFFIX);

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        int Napps = new Double((IFURadialMax - IFURadialMin) / (IFU_DIAMETER + IFU_SPACING) + 1).intValue();
        if (Napps < 0) Napps = 1;

        for (int i = 0; i < Napps; i++) {
            final double Xpos = IFURadialMin + (IFU_DIAMETER + IFU_SPACING) * i;
            IFUApertures.addAperture(new HexagonalAperture(Xpos, 0, IFU_DIAMETER));
            IFUOffsets.add(Xpos);
        }
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

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();

        int numX = 20;            // number of elements in the IFU in the x-direction
        int numY = 25;            // number of elements in the IFU in the y-direction
        if (isIfu2) {
            numX = 40;            // number of elements in the IFU in the x-direction
            }

        for (int i = 0; i < numY; i++) {
            double y = (i - (numY-1)/2.) * IFU_DIAMETER;
            for (int j = 0; j < numX; j++) {
                double x = (j - (numX-1)/2.) * IFU_DIAMETER;
                double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                if (r < radius) {
                    IFUApertures.addAperture(new HexagonalAperture(x, y, IFU_DIAMETER));
                    IFUOffsets.add(x);
                    IFUOffsets.add(y);
                }
            }
        }
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
