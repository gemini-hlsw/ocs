package edu.gemini.itc.operation;

import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.spModel.core.Site;

/**
 * The WaterTransmissionVisitor is designed to adjust the SED for
 * water in the atmosphere.
 */
public final class WaterTransmissionVisitor {

    private WaterTransmissionVisitor() {
    }

    public static SampledSpectrumVisitor create(final int skyTransparencyWater, final double airMass, final String file_name, final Site site, final String wavelenRange) {

        final String name;

        if (wavelenRange.equals(ITCConstants.VISIBLE)) {
            switch (skyTransparencyWater) {
                case 1:
                    if (airMass <= 1.25) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "20_" + "10" +
                                ITCConstants.DATA_SUFFIX;
                    } else if (airMass > 1.25 && airMass <= 1.75) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "20_" + "15" +
                                ITCConstants.DATA_SUFFIX;
                    } else {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "20_" + "20" +
                                ITCConstants.DATA_SUFFIX;
                    }
                    break;
                case 2:
                    if (airMass <= 1.25) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "50_" + "10" +
                                ITCConstants.DATA_SUFFIX;
                    } else if (airMass > 1.25 && airMass <= 1.75) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "50_" + "15" +
                                ITCConstants.DATA_SUFFIX;
                    } else {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "50_" + "20" +
                                ITCConstants.DATA_SUFFIX;
                    }
                    break;
                case 3:
                    if (airMass <= 1.25) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "80_" + "10" +
                                ITCConstants.DATA_SUFFIX;
                    } else if (airMass > 1.25 && airMass <= 1.75) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "80_" + "15" +
                                ITCConstants.DATA_SUFFIX;
                    } else {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "80_" + "20" +
                                ITCConstants.DATA_SUFFIX;
                    }
                    break;
                case 4:
                    if (airMass <= 1.25) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "100_" + "10" +
                                ITCConstants.DATA_SUFFIX;
                    } else if (airMass > 1.25 && airMass <= 1.75) {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "100_" + "15" +
                                ITCConstants.DATA_SUFFIX;
                    } else {
                        name = ITCConstants.TRANSMISSION_LIB + "/" +
                                file_name + "100_" + "20" +
                                ITCConstants.DATA_SUFFIX;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown WV value");
            }

        } else {

            final String _airmassCategory;
            final String _transmissionCategory;

            if (airMass <= 1.25)
                _airmassCategory = "10";
            else if (airMass > 1.25 && airMass <= 1.75)
                _airmassCategory = "15";
            else
                _airmassCategory = "20";

            switch (skyTransparencyWater) {
                case 1:
                    _transmissionCategory = "20_";
                    break;
                case 2:
                    _transmissionCategory = "50_";
                    break;
                case 3:
                    _transmissionCategory = "80_";
                    break;
                case 4:
                    _transmissionCategory = "100_";
                    break;
                default:
                    throw new IllegalArgumentException("unknown WV value");
            }

            name = "/HI-Res/" + abbrForSite(site) + wavelenRange + ITCConstants.TRANSMISSION_LIB + "/"
                    + file_name + _transmissionCategory + _airmassCategory + ITCConstants.DATA_SUFFIX;
        }

        return new TransmissionElement(name);


    }

    private static String abbrForSite(Site site) {
        switch (site) {
            case GN: return "mk";
            case GS: return "cp";
            default: throw new IllegalArgumentException();
        }
    }

}
