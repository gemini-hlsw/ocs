package edu.gemini.ags.gems;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;

import java.util.Set;

/**
 * See OT-27
 */
public class GemsGuideStars implements Comparable<GemsGuideStars> {

    // position angle that applies to the result
    private Angle pa;

    // the guide group used for tip tilt correction
    private GemsGuideProbeGroup tiptiltGroup;

    // calculated by the mascot algorithm
    private GemsStrehl strehl;

    //  Guide Group Contents
    private GuideGroup guideGroup;


    /**
     * The guideGroup should contain the designation of guide stars to guiders for the tip tilt
     * asterism computed by mascot and the flexure star.
     * There must be 1 to 3 tip tilt guide stars all designated for guiders in the same group
     * (e.g., all Canopus or all GSAOI On Detector Guide Window).
     * There must be one flexure star taken from the opposite group. For example, if a Canopus
     * asterism is used for tiptilt, then the flexure star is a GSAOI ODGW star if using GSAOI
     * or an F2 OIWFS star if using Flamingos2.
     * If the tip tilt asterism is assigned to the GSAOI ODGW group, then the flexure star must
     * be assigned to CWFS3.
     * Flamingos 2 OIWFS can only ever be used for the flexure star.
     * The GemsCatalogSearchCriterion available in the input will contain all the options that
     * need be considered (i.e., the F2 OIWFS "group" will never appear with "tiptilt" type).
     *
     * @param pa position angle that applies to the result
     * @param tiptiltGroup the guide group used for tip tilt correction
     * @param strehl calculated by the mascot algorithm
     * @param guideGroup guide group Contents
     */
    public GemsGuideStars(Angle pa, GemsGuideProbeGroup tiptiltGroup, GemsStrehl strehl, GuideGroup guideGroup) {
        this.pa = pa;
        this.tiptiltGroup = tiptiltGroup;
        this.strehl = strehl;
        this.guideGroup = guideGroup;
    }

    public Angle getPa() {
        return pa;
    }

    public GemsGuideProbeGroup getTiptiltGroup() {
        return tiptiltGroup;
    }

    public GemsStrehl getStrehl() {
        return strehl;
    }

    public GuideGroup getGuideGroup() {
        return guideGroup;
    }

    @Override
    /**
     * From OT-27: Ranking Results
     *
     * The first order ranking of results is by best (highest) average Strehl ratio. In addition:
     *
     * When searching ODGW asterisms over different PAs, among configurations that
     * give equivalent average Strehls ratios the ones that include ODGW1 stars must
     * excluded. GSAOI detector 1 has many bad pixels and must be avoided when possible.
     *
     * When searching over different PAs preference must be given to orientations along
     * the cardinal directions (PA=0,90,180,270). If all orientations are equivalent
     * then PA=0 must be selected. It is more important however to avoid ODGW1.
     *
     * In these rules, an "equivalent" average strehl is defined as anything within 2% average strehl.
     */
    public int compareTo(GemsGuideStars that) {
        Boolean thisContainsOdgw1 = this.guideGroup.contains(GsaoiOdgw.odgw1);
        Boolean thatContainsOdgw1 = that.guideGroup.contains(GsaoiOdgw.odgw1);
        if (!thisContainsOdgw1.equals(thatContainsOdgw1)) {
            return thatContainsOdgw1.compareTo(thisContainsOdgw1);
        }

        double thisStrel = this.getAverageStrehlForCompare();
        double thatStrehl = that.getAverageStrehlForCompare();
        // Check if more than 2% difference in strehl average
        if ((Math.abs(thisStrel - thatStrehl) / ((thisStrel + thatStrehl)/2.)) * 100 > 2) {
            return Double.compare(thisStrel, thatStrehl);
        }

        double thisPa = pa.toDegrees();
        double thatPa = that.pa.toDegrees();
        double[] cardinalDirections = {0.,90.,180.,270.};
        if (thisPa != thatPa) {
            for (double d : cardinalDirections) {
                if (thisPa == d) return 1;
                if (thatPa == d) return -1;
            }
        }

        return Double.compare(thisStrel, thatStrehl);
    }

    // Returns the average Strehl value, minus 20% if it is a Canopus asterism and cwfs3 is not assigned to the
    // brightest star.
    //
    // OT-27:
    // When using Canopus for tiptilt correction, the brightest star of the asterism (in R)
    // must be assigned to CWFS3 unless by not doing so the average Strehl can be improved
    // by more than 20%. In this case, the second brightest star should be assigned to CWFS3
    // as long as the faintness limit (R=17.5) is met.
    private double getAverageStrehlForCompare() {
        double avg = strehl.getAvg();
        if ("CWFS".equals(tiptiltGroup.getKey())) {
           if (!cwfs3IsBrightest()) {
               // Previous code ensures that cwfs3 is either the brightest or second brightest
               // so this assumes cwfs3 is the second brightest
               return avg - avg*0.2;
           }
        }
        return avg;
    }

    // Returns true if cwfs3 is the brightest star in the Caanopus asterism
    private boolean cwfs3IsBrightest() {
        double cwfs1 = getRLikeMag(guideGroup.get(Canopus.Wfs.cwfs1));
        double cwfs2 = getRLikeMag(guideGroup.get(Canopus.Wfs.cwfs2));
        double cwfs3 = getRLikeMag(guideGroup.get(Canopus.Wfs.cwfs3));
        return cwfs3 < cwfs2 && cwfs3 < cwfs1;
    }

    // Returns the R magnitude, if known, otherwise 99.
    private double getRLikeMag(Option<GuideProbeTargets> g) {
        return GemsUtils4Java.getRLikeMagnitude(g, 99.0);
    }


//    // Used to format strehl message
//    private static NumberFormat nf = NumberFormat.getInstance(Locale.US);
//    static {
//        nf.setMaximumFractionDigits(2);
//    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Set<GuideProbe> guiders = guideGroup.getReferencedGuiders();
        for(GuideProbe guideProbe : guiders) {
            SPTarget target = guideGroup.get(guideProbe).getValue().getPrimary().getValue();
            sb.append(guideProbe);
            sb.append("[");
            sb.append(target.getTarget().getRa());
            sb.append(",");
            sb.append(target.getTarget().getDec());
            sb.append("] ");
        }
        return "GemsGuideStars{" +
                "pa=" + pa +
                ", tiptilt=" + tiptiltGroup.getKey() +
                ", avg Strehl=" + strehl.getAvg()*100 +
                ", guiders=" + sb.toString() +
                '}';
    }
}
