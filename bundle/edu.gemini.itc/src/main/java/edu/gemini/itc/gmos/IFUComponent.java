// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.ApertureComponent;
import edu.gemini.itc.shared.ApertureComposite;
import edu.gemini.itc.shared.HexagonalAperture;
import edu.gemini.itc.shared.TransmissionElement;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Instrument;

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 *  This is the specific setup for the IFU of GMOS.
 *  It uses Hexagonal Apertures that can be in a line radialy
 *  from an arbitrary point in a gaussian.
 */

public class IFUComponent extends TransmissionElement {
    public static final double IFU_DIAMETER = 0.186;  //earlier calc showed D = 0.206
    public static final double IFU_SPACING = 0.014;
    private ApertureComposite IFUApertures;
    private List IFUOffsets;


    /**
     *  Constructor for a user defined radial ifu set.
     */

    public IFUComponent(double IFURadialMin, double IFURadialMax) throws Exception {

        super(ITCConstants.LIB + "/" + Gmos.INSTR_DIR + "/" + Gmos.INSTR_PREFIX + "ifu_trans" + Instrument.DATA_SUFFIX);
        //System.out.println(ITCConstants.LIB + "/" +Gmos.INSTR_DIR + "/" +Gmos.INSTR_PREFIX+ "ifu_trans"+Instrument.DATA_SUFFIX);
        int Napps;

        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList();

        // if it is a single aperture just create it

        //	if (IFURadialMin + IFUDiameter <= IFURadialMax)
        //	{
        //		double Xmin = IFURadialMin;
        //		double Xmax = IFURadialMin+ IFUDiameter;
        //		IFUApertures = new HexagonalAperture(Xmin, Xmax, 0,0);
        //	}
        //	else
        //	{
        Napps = new Double((IFURadialMax - IFURadialMin) / (IFU_DIAMETER + IFU_SPACING) + 1).intValue();

        if (Napps < 0) Napps = 1;

        for (int i = 0; i < Napps; i++) {
            //double Xmin = IFURadialMin + (IFU_DIAMETER)*i;
            //double Xmax = IFURadialMin + (IFU_DIAMETER)*(1+i);

            //double Xpos = (Xmin+Xmax)/2- ((IFU_DIAMETER)/2);
            double Xpos = IFURadialMin + (IFU_DIAMETER + IFU_SPACING) * i;

            IFUApertures.addAperture(new HexagonalAperture(Xpos, 0, IFU_DIAMETER));
            IFUOffsets.add(new Double(Xpos));
        }
    }

    public IFUComponent(double IFUOffsetX) throws Exception {
        super(ITCConstants.LIB + "/" + Gmos.INSTR_DIR + "/" + Gmos.INSTR_PREFIX + "ifu_trans" + Instrument.DATA_SUFFIX);
        IFUApertures = new ApertureComposite();
        IFUOffsets = new ArrayList();

        IFUApertures.addAperture(new HexagonalAperture(IFUOffsetX, 0, IFU_DIAMETER));
        IFUOffsets.add(new Double(IFUOffsetX));
    }


    public ApertureComponent getAperture() {
        return IFUApertures;
    }

    public List getFractionOfSourceInAperture() {
        return IFUApertures.getFractionOfSourceInAperture();
    }

    public List getApertureOffsetList() {
        return IFUOffsets;
    }

    public String toString() {
        return "IFU Transmission";
    }

}
