package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.spModel.gemini.ghost.Detector;
import edu.gemini.spModel.target.env.ResolutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is the specific setup for the IFU of Ghost.
 * It uses Hexagonal Apertures that can be in a line radius
 * from an arbitrary point in a gaussian.
 */

public final class IFUComponent extends TransmissionElement {
    public static final double FIBER_DIAMETER_HR = 0.16628;   // The apothem value is 0.12 mm.
    public static final double FIBER_DIAMETER_SR = 0.27713;
    public static final double IFU_SPACING_HR = 0.0;   // TODO. Venu has to confirm the correct value
    public static final double IFU_SPACING_SR = 0.0;   // Using 0 and 0.27713 long diameter, the all fiber area is
                                                       // equal to matlab code (0.349122546103 mm2).
    private final ApertureComposite IFUApertures;
    private final List<Double> IFUOffsets;
    private static final Logger Log = Logger.getLogger( IFUComponent.class.getName() );
    private final double RADIUS_HR=0.36226;
    private final double LONG_IFU_DIAMETER_HR= 0.7245;
    private final double RADIUS_SR=0.3666;
    private final double LONG_IFU_DIAMETER_SR= 0.7332; //mm
    private ResolutionMode _res;
    public IFUComponent(ResolutionMode res) {

        super(ITCConstants.LIB + "/" + Ghost.INSTR_DIR + "/" +Ghost.INSTR_PREFIX + "ifu" + Instrument.DATA_SUFFIX);
        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList<>();
        double ifu_diameter = FIBER_DIAMETER_HR;
        double ifu_spacing =  IFU_SPACING_HR;
        double radius= RADIUS_HR;
        if (res == ResolutionMode.GhostStandard) {
            ifu_diameter = FIBER_DIAMETER_SR;
            ifu_spacing =  IFU_SPACING_SR;
            radius = RADIUS_SR;
        }
        int numX=8;
        int numY=9;
        double distX = (Math.sqrt(3)/2.0)* (ifu_diameter + ifu_spacing);  // The sqrt(3)/2 it is gotten using the Pythagoras theorem between the centers
                                                                       // of three microlens.
        double distY = (ifu_diameter + ifu_spacing)/2.0;
        for (int i = 0; i < numX; i++) {
            double x = (i - (numX/2.0)) * distX;
            for (int j = 0; j < numY; j++) {
                double y = (2*j - numY + Math.abs(i)%2 - 1) * distY;
                double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
                if (r < radius) {
                    IFUApertures.addAperture(new HexagonalAperture(x, y, ifu_diameter));
                    IFUOffsets.add(r);
                }
            }
        }
        List<ApertureComponent> list = IFUApertures.getApertureList();
        for( ApertureComponent ac : list) {
            HexagonalAperture h = (HexagonalAperture) ac;
        }
        this._res = res;
    }

    public ApertureComponent getAperture() {
        return IFUApertures;
    }

    public List<Double> getFractionOfSourceInAperture() {
        return IFUApertures.getFractionOfSourceInAperture();
    }

    public String toString() {
        String infoIfu = "IFU";
        double ifu_diameter = (_res == ResolutionMode.Standard) ? LONG_IFU_DIAMETER_SR : LONG_IFU_DIAMETER_HR ;
        infoIfu += " resolution: " + _res.displayValue()  + ". ifu-diameter: " + (ifu_diameter * Ghost.PLATE_SCALE) + " arcsec";
        return infoIfu;
    }

}
