package edu.gemini.itc.parameters;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.SkyBackground;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.WaterVapor;

/**
 * This class holds the information from the Observing Condition section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class ObservingConditionParameters {

    private final ImageQuality  iq;
    private final CloudCover    cc;
    private final WaterVapor    wv;
    private final SkyBackground sb;
    private final double        airmass;

    /**
     * Constructs a ObservingConditionParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public ObservingConditionParameters(
            final ImageQuality iq,
            final CloudCover cc,
            final WaterVapor wv,
            final SkyBackground sb,
            final double airmass) {

        this.iq      = iq;
        this.cc      = cc;
        this.wv      = wv;
        this.sb      = sb;
        this.airmass = airmass;
    }

    // == IQ
    public int getImageQuality() {
        return iq.ordinal() + 1;
    }

    public double getImageQualityPercentile() {
        return iq.getPercentage() / 100.0;
    }

    // == CC

    // TODO: currently ITC does not support 90% for CC and a total of 4 values (1..4) for CC
    // TODO: in order to deal with this we translate 90% to ANY
    // TODO: Note: values is used for file lookup like cloud_trans<nr>.dat

    public int getSkyTransparencyCloud() {
        switch (cc) {
            case ANY:           return cc.ordinal();
            default:            return cc.ordinal() + 1;
        }
    }

    public double getSkyTransparencyCloudPercentile() {
        switch (cc) {
            case PERCENT_90:    return 1.0;
            default:            return cc.getPercentage() / 100.0;
        }
    }

    // == WV
    public int getSkyTransparencyWater() {
        return wv.ordinal() + 1;
    }

    public double getSkyTransparencyWaterPercentile() {
        return wv.getPercentage() / 100.0;
    }

    public String getSkyTransparencyWaterCategory() {
        return wv.sequenceValue();
    }

    // == SB
    public int getSkyBackground() {
        return sb.ordinal() + 1;
    }

    public double getSkyBackgroundPercentile() {
        return sb.getPercentage() / 100.0;
    }

    public String getSkyBackgroundCategory() {
        return sb.sequenceValue();
    }

    // == Airmass
    public double getAirmass() {
        return airmass;
    }

    public String getAirmassCategory() {
        if (airmass <= 1.25)
            return "10";
        else if (airmass > 1.25 && airmass <= 1.75)
            return "15";
        else
            return "20";
    }


    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Image Quality:\t" + getImageQuality() + "\n");
        sb.append("Cloud Trans:\t" + getSkyTransparencyCloud() + "\n");
        sb.append("Water Trans:\t" + getSkyTransparencyWater() + "\n");
        sb.append("Sky Background:\t" + getSkyBackground() + "\n");
        sb.append("Airmass:\t" + getAirmass() + "\n");
        sb.append("\n");
        return sb.toString();
    }

}
