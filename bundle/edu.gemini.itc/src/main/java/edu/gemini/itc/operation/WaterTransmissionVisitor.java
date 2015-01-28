package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.TransmissionElement;

/**
 * The WaterTransmissionVisitor is designed to adjust the SED for
 * water in the atmosphere.
 */
public final class WaterTransmissionVisitor {
    private static final String FILENAME = "water_trans";

    private WaterTransmissionVisitor() {
    }

    /**
     * Constructs transmission visitor for water vapor.
     */
    public static SampledSpectrumVisitor create(int skyTransparencyWater) throws Exception {
        return new TransmissionElement(ITCConstants.TRANSMISSION_LIB + "/" + FILENAME + skyTransparencyWater + ITCConstants.DATA_SUFFIX);
    }

    public static SampledSpectrumVisitor create(int skyTransparencyWater, double airMass, String file_name, String site, String wavelenRange) throws Exception {

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

            name = "/HI-Res/" + site + wavelenRange + ITCConstants.TRANSMISSION_LIB + "/"
                    + file_name + _transmissionCategory + _airmassCategory + ITCConstants.DATA_SUFFIX;
        }

        return new TransmissionElement(name);


    }

}
