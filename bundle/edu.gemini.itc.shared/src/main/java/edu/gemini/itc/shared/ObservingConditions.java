package edu.gemini.itc.shared;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.CloudCover;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.SkyBackground;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.WaterVapor;

import java.io.Serializable;

/**
 * Container for observing condition parameters.
 */
public final class ObservingConditions implements Serializable {

    private final ImageQuality  iq;
    private final CloudCover    cc;
    private final WaterVapor    wv;
    private final SkyBackground sb;
    private final double        airmass;

    /**
     * Constructs a ObservingConditionParameters from a servlet request
     */
    public ObservingConditions(
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

    public ImageQuality  iq() { return iq; }
    public WaterVapor    wv() { return wv; }
    public CloudCover    cc() { return cc; }
    public SkyBackground sb() { return sb; }


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

}
