/**
 * $Id: DisperserInfo.java 7064 2006-05-25 19:48:25Z shane $
 */

package edu.gemini.mask;

import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;

/**
 * Defines min/max wavelength for GMOS dispersers (north and south)
 */
class DisperserInfo {

    private static final DisperserInfo[] GRATING_INFO = new DisperserInfo[]{
        // XXX the version in EdCompInstGmos has 1100 instead of 1050
        // XXX The gmmps gratings.lut file has 1050

        // GMOS-N
        new DisperserInfo(DisperserNorth.B1200_G5301, 1200, 300, 1050),
        new DisperserInfo(DisperserNorth.R831_G5302, 831, 498, 1050),
        new DisperserInfo(DisperserNorth.B600_G5303, 600, 320, 1050),
        new DisperserInfo(DisperserNorth.R600_G5304, 600, 530, 1050),
        new DisperserInfo(DisperserNorth.R400_G5305, 400, 520, 1050),
        new DisperserInfo(DisperserNorth.R150_G5306, 150, 430, 1050),

        // GMOS-S
        new DisperserInfo(DisperserSouth.B1200_G5321, 1200, 300, 1050),
        new DisperserInfo(DisperserSouth.R831_G5322, 831, 498, 1050),
        new DisperserInfo(DisperserSouth.B600_G5323, 600, 320, 1050),
        new DisperserInfo(DisperserSouth.R600_G5324, 600, 530, 1050),
        new DisperserInfo(DisperserSouth.R400_G5325, 400, 520, 1050),
        new DisperserInfo(DisperserSouth.R150_G5326, 150, 430, 1050),
    };

    private final GmosCommonType.Disperser disperser;
    private final int grating;
    private final int lambda1;
    private final int lambda2;

    DisperserInfo(GmosCommonType.Disperser type, int grating, int lambda1, int lambda2) {
        this.grating = grating;
        this.disperser = type;
        this.lambda1 = lambda1;
        this.lambda2 = lambda2;
    }

    static DisperserInfo getDisperserInfo(GmosCommonType.Disperser d) {
        for (DisperserInfo dinfo : GRATING_INFO) {
            if (dinfo.disperser == d) return dinfo;
        }
        throw new IllegalArgumentException("The chosen disperser: " + d.name()
                + " is not supported. Please select another disperser.");
    }

    public GmosCommonType.Disperser getDisperser() {
        return disperser;
    }

    public int getGrating() {
        return grating;
    }

    public int getLambda1() {
        return lambda1;
    }

    public int getLambda2() {
        return lambda2;
    }
}
