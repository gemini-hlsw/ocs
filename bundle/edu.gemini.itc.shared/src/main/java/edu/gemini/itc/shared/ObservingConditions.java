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

    public ImageQuality  iq()      { return iq; }
    public WaterVapor    wv()      { return wv; }
    public CloudCover    cc()      { return cc; }
    public SkyBackground sb()      { return sb; }
    public double        airmass() { return airmass; }

}
