package edu.gemini.itc.operation;

import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;

/**
 * The WaterTransmissionVisitor is designed to adjust the SED for
 * water in the atmosphere.
 */
public final class WaterTransmissionVisitor {

    private WaterTransmissionVisitor() {
    }

    public static SampledSpectrumVisitor create(final Instrument instrument, final SPSiteQuality.WaterVapor wv, final double airMass, final String file_name) {

        final String name;

        switch (instrument.getBands()) {
            case VISIBLE:
                switch (wv) {
                    case PERCENT_20:
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
                    case PERCENT_50:
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
                    case PERCENT_80:
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
                    case ANY:
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

                break;

            default:

                final String _airmassCategory;
                final String _transmissionCategory;

                if (airMass <= 1.25)
                    _airmassCategory = "10";
                else if (airMass > 1.25 && airMass <= 1.75)
                    _airmassCategory = "15";
                else
                    _airmassCategory = "20";

                switch (wv) {
                    case PERCENT_20: _transmissionCategory = "20_"; break;
                    case PERCENT_50: _transmissionCategory = "50_"; break;
                    case PERCENT_80: _transmissionCategory = "80_"; break;
                    case ANY: _transmissionCategory = "100_"; break;
                    default:
                        throw new IllegalArgumentException("unknown WV value");
                }

                name = "/HI-Res/" + abbrForSite(instrument.getSite()) + instrument.getBands().getDirectory() + ITCConstants.TRANSMISSION_LIB + "/"
                        + file_name + _transmissionCategory + _airmassCategory + ITCConstants.DATA_SUFFIX;
        }

        System.out.println("***** ***** Water name " + name);
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
