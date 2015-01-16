package edu.gemini.ags.gems;

import edu.gemini.ags.gems.mascot.Star;
import edu.gemini.ags.gems.mascot.Strehl;
import edu.gemini.ags.gems.mascot.MascotCat;
import edu.gemini.ags.gems.mascot.MascotProgress;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Target;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.ValidatableGuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;

/**
 * Analyzes the results of the given catalog queries to find the best guide star asterisms for
 * the given observation context. The Mascot Strehl algorithm is used to get a list of asterisms
 * for the stars found.
 *
 * See OT-27
 */
public class GemsCatalogResults {

    /**
     * Analyze the given position angles and search results to select tip tilt asterisms and flexure stars.
     * @param obsContext observation context
     * @param posAngles position angles to try (should contain at least one element: the current pos angle)
     * @param results results of catalog search
     * @param progress used to report progress of Mascot Strehl calculations and interrupt if requested
     * @return a sorted List of GemsGuideStars
     */
    public List<GemsGuideStars> analyze(final ObsContext obsContext, final Set<Angle> posAngles,
                                        final List<GemsCatalogSearchResults> results, final MascotProgress progress) {

        final Coordinates base = obsContext.getBaseCoordinates();
        final List<GemsGuideStars> result = new ArrayList<>();

        for (TiptiltFlexurePair pair : TiptiltFlexurePair.pairs(results)) {
            final GemsGuideProbeGroup tiptiltGroup = pair.getTiptiltResults().criterion().key().group();
            final GemsGuideProbeGroup flexureGroup = pair.getFlexureResults().criterion().key().group();
            final List<Target.SiderealTarget> tiptiltTargetsList = filter(obsContext, pair.getTiptiltResults().resultsAsJava(),
                    tiptiltGroup, posAngles);
            final List<Target.SiderealTarget> flexureTargetsList = filter(obsContext, pair.getFlexureResults().resultsAsJava(),
                    flexureGroup, posAngles);
            if (tiptiltTargetsList.size() != 0 && flexureTargetsList.size() != 0) {
                if (progress != null) {
                    progress.setProgressTitle("Finding asterisms for " + tiptiltGroup.getKey());
                }
                final Magnitude.Band bandpass = getBandpass(tiptiltGroup, obsContext.getInstrument());
                final double factor = getStrehlFactor(new Some<>(obsContext));
                final MascotCat.StrehlResults strehlResults = MascotCat.javaFindBestAsterismInSkyObjectList(
                        tiptiltTargetsList, base.getRaDeg(), base.getDecDeg(), bandpass.name(), factor, progress);
                for (Strehl strehl : strehlResults.strehlList()) {
                    result.addAll(analyzeAtAngles(obsContext, posAngles, strehl, flexureTargetsList, flexureGroup,
                            tiptiltGroup));
                }
            }
        }

        return sortResultsByRanking(result);
    }


    /**
     * Analyze the given position angles and search results to select tip tilt asterisms and flexure stars.
     * This version allows the progress argument to stop the strehl algorithm when a "good enough"
     * asterism has been found and use the results up until that point.
     *
     * @param obsContext observation context
     * @param posAngles position angles to try (should contain at least one element: 0. deg)
     * @param results results of catalog search
     * @param progress used to report progress of Mascot Strehl calculations and interrupt if requested
     *        (using the results up until that point)
     * @return a sorted List of GemsGuideStars
     */
    public List<GemsGuideStars> analyzeGoodEnough(final ObsContext obsContext, final Set<Angle> posAngles,
                                        final List<GemsCatalogSearchResults> results, final MascotProgress progress) {

        final Coordinates base = obsContext.getBaseCoordinates();
        final List<GemsGuideStars> result = new ArrayList<>();

        for (TiptiltFlexurePair pair : TiptiltFlexurePair.pairs(results)) {
            final GemsGuideProbeGroup tiptiltGroup = pair.getTiptiltResults().criterion().key().group();
            final GemsGuideProbeGroup flexureGroup = pair.getFlexureResults().criterion().key().group();
            final List<Target.SiderealTarget> tiptiltTargetsList = filter(obsContext, pair.getTiptiltResults().resultsAsJava(),
                    tiptiltGroup, posAngles);
            final List<Target.SiderealTarget> flexureTargetsList = filter(obsContext, pair.getFlexureResults().resultsAsJava(),
                    flexureGroup, posAngles);
            if (tiptiltTargetsList.size() != 0 && flexureTargetsList.size() != 0) {
                if (progress != null) {
                    progress.setProgressTitle("Finding asterisms for " + tiptiltGroup.getKey());
                }
                final Magnitude.Band bandpass = getBandpass(tiptiltGroup, obsContext.getInstrument());
                final MascotProgress strehlHandler = new MascotProgress() {
                    @Override
                    public boolean progress(Strehl strehl, int count, int total, boolean usable) {
                        final List<GemsGuideStars> subResult = analyzeAtAngles(obsContext, posAngles, strehl,
                                flexureTargetsList, flexureGroup, tiptiltGroup);
                        boolean used = subResult.size() != 0;
                        if (used) result.addAll(subResult);
                        return progress == null || progress.progress(strehl, count, total, used);
                    }

                    @Override
                    public void setProgressTitle(String s) {
                        progress.setProgressTitle(s);
                    }
                };

                final double factor = getStrehlFactor(new Some<>(obsContext));
                try {
                    MascotCat.javaFindBestAsterismInSkyObjectList(
                            tiptiltTargetsList, base.getRaDeg(), base.getDecDeg(), bandpass.name(), factor, strehlHandler);
                } catch (CancellationException e) {
                    // continue on with results so far?
                }
            }
        }

        return sortResultsByRanking(result);
    }

    // Tries the given asterism and flexure star at the given position angles and returns a list of
    // combinations that work.
    private List<GemsGuideStars> analyzeAtAngles(final ObsContext obsContext, final Set<Angle> posAngles, final Strehl strehl,
                                                 final List<Target.SiderealTarget> flexureSkyObjectList,
                                                 final GemsGuideProbeGroup flexureGroup,
                                                 final GemsGuideProbeGroup tiptiltGroup) {
        final List<GemsGuideStars> result = new ArrayList<>();
        for (Angle posAngle : posAngles) {
            List<Target.SiderealTarget> flexureList = filter(obsContext, flexureSkyObjectList, flexureGroup, posAngle);
            List<Target.SiderealTarget> flexureStars = GemsUtils4Java.sortTargetsByBrightness(flexureList);
            result.addAll(analyzeStrehl(obsContext, strehl, posAngle, tiptiltGroup, flexureGroup, flexureStars, true));
            if ("CWFS".equals(tiptiltGroup.getKey())) {
                // try different order of cwfs1 and cwfs2
                result.addAll(analyzeStrehl(obsContext, strehl, posAngle, tiptiltGroup, flexureGroup, flexureStars, false));
            }
        }
        return result;
    }


    // Analyzes the given strehl object at the given position angle and returns a list of
    // GemsGuideStars objects, each containing a 1 to 3 star asterism from the given tiptiltGroup group and
    // one star from the flexure group. If any of the stars in the asterism is not valid at the position
    // angle or if no flexure star can be found, an empty list is returned.
    //
    // If reverseOrder is true, reverse the order in which guide probes are tried (to make sure to get all
    // combinations of cwfs1 and cwfs2, since cwfs3 is otherwise fixed)
    private List<GemsGuideStars> analyzeStrehl(final ObsContext obsContext, final Strehl strehl, final Angle posAngle,
                                               final GemsGuideProbeGroup tiptiltGroup, final GemsGuideProbeGroup flexureGroup,
                                               final List<Target.SiderealTarget> flexureStars, final boolean reverseOrder) {
        final List<GemsGuideStars> result = new ArrayList<>();
        final List<Target.SiderealTarget> tiptiltTargetList = getTargetListFromStrehl(strehl);

        // XXX The TPE assumes canopus tiptilt if there are only 2 stars (one of each ODGW and CWFS),
        // So don't add any items to the list that have only 2 stars and GSAOI as tiptilt.
        if (tiptiltGroup == GsaoiOdgw.Group.instance && tiptiltTargetList.size() == 1) {
            return result;
        }

        if (validate(obsContext, tiptiltTargetList, tiptiltGroup, posAngle)) {
            final List<GuideProbeTargets> guideProbeTargets = assignGuideProbeTargets(obsContext, posAngle,
                    tiptiltGroup, tiptiltTargetList, flexureGroup, flexureStars, reverseOrder);
            if (!guideProbeTargets.isEmpty()) {
                final GuideGroup guideGroup = GuideGroup.create(None.<String>instance(), DefaultImList.create(guideProbeTargets));
                final GemsStrehl gemsStrehl = new GemsStrehl(strehl.avgstrehl(), strehl.rmsstrehl(), strehl.minstrehl(), strehl.maxstrehl());
                final GemsGuideStars gemsGuideStars = new GemsGuideStars(GemsUtils4Java.toNewAngle(posAngle), tiptiltGroup, gemsStrehl, guideGroup);
                result.add(gemsGuideStars);
            }
        }
        return result;
    }


    // Returns a list of GuideProbeTargets for the given tiptilt targets and flexure star.
    //
    // If reverseOrder is true, reverse the order in which guide probes are tried (to make sure to get all
    // combinations of cwfs1 and cwfs2, since cwfs3 is otherwise fixed)
    private List<GuideProbeTargets> assignGuideProbeTargets(ObsContext obsContext, final Angle posAngle,
                                                            final GemsGuideProbeGroup tiptiltGroup,
                                                            final List<Target.SiderealTarget> tiptiltTargetList,
                                                            final GemsGuideProbeGroup flexureGroup,
                                                            final List<Target.SiderealTarget> flexureStars,
                                                            final boolean reverseOrder) {
        final List<GuideProbeTargets> result = new ArrayList<>(tiptiltTargetList.size() + 1);

        // assign guide probes for tiptilt asterism
        for(Target.SiderealTarget target : tiptiltTargetList) {
            final Option<GuideProbeTargets> guideProbeTargets = assignGuideProbeTarget(obsContext, posAngle, tiptiltGroup,
                    target, tiptiltGroup, result, tiptiltTargetList, reverseOrder);
            if (guideProbeTargets.isEmpty()) return new ArrayList<>();
            result.add(guideProbeTargets.getValue());
            // Update the ObsContext, since validation of the following targets may depend on it
            obsContext = obsContext.withTargets(obsContext.getTargets().putPrimaryGuideProbeTargets(guideProbeTargets.getValue()));
        }

        // assign guide probe for flexure star
        for (Target.SiderealTarget flexureStar : flexureStars) {
            final Option<GuideProbeTargets> guideProbeTargets = assignGuideProbeTarget(obsContext, posAngle, flexureGroup,
                    flexureStar, tiptiltGroup, result, tiptiltTargetList, false);
            if (!guideProbeTargets.isEmpty()) {
                result.add(guideProbeTargets.getValue());
                break;
            }
        }

        if (result.size() == tiptiltTargetList.size() + 1) {
            return result;
        } else {
            return new ArrayList<>();
        }
    }


    // Returns the GuideProbeTargets object for the given tiptilt target.
    //
    // If reverseOrder is true, reverse the order in which guide probes are tried (to make sure to get all
    // combinations of cwfs1 and cwfs2, since cwfs3 is otherwise fixed)
    private Option<GuideProbeTargets> assignGuideProbeTarget(final ObsContext obsContext, final Angle posAngle,
                                                             final GemsGuideProbeGroup group, final Target.SiderealTarget target,
                                                             final GemsGuideProbeGroup tiptiltGroup,
                                                             final List<GuideProbeTargets> otherTargets,
                                                             final List<Target.SiderealTarget> tiptiltTargetList,
                                                             final boolean reverseOrder) {
        // First try to assign cwfs3 to the brightest star, if applicable (assignCwfs3ToBrightest arg = true)
        Option<GuideProbe> guideProbe = getGuideProbe(obsContext, target, group, posAngle, tiptiltGroup,
                otherTargets, tiptiltTargetList, true, reverseOrder);

        if (guideProbe.isEmpty() && "CWFS".equals(tiptiltGroup.getKey())) {
            // if that didn't work, try to assign cwfs3 to the second brightest star (assignCwfs3ToBrightest arg = false)
            guideProbe = getGuideProbe(obsContext, target, group, posAngle, tiptiltGroup,
                    otherTargets, tiptiltTargetList, false, reverseOrder);
        }

        if (guideProbe.isEmpty()) {
            return None.instance();
        } else {
            return new Some<>(GuideProbeTargets.create(guideProbe.getValue(), GemsUtils4Java.toSPTarget(target)));
        }
    }


    // Returns the given targets list with any objects removed that are not valid in at least one of the
    // given position angles.
    private List<Target.SiderealTarget> filter(final ObsContext obsContext, final List<Target.SiderealTarget> targetsList, final GemsGuideProbeGroup group,
                                   final Set<Angle> posAngles) {
        final List<Target.SiderealTarget> result = new ArrayList<>(targetsList.size());
        for (Angle posAngle : posAngles) {
            result.addAll(filter(obsContext, targetsList, group, posAngle));
        }
        return result;
    }

    // Returns the given targets list with any objects removed that are not valid in the
    // given position angle.
    private List<Target.SiderealTarget> filter(final ObsContext obsContext, final List<Target.SiderealTarget> targetsList,
                                   final GemsGuideProbeGroup group, final Angle posAngle) {
        final List<Target.SiderealTarget> result = new ArrayList<>(targetsList.size());
        for (Target.SiderealTarget siderealTarget : targetsList) {
            if (validate(obsContext, siderealTarget, group, posAngle)) {
                result.add(siderealTarget);
            }
        }
        return result;
    }

    // Returns the input list sorted by ranking. See OT-27 and GemsGuideStars.compareTo
    private List<GemsGuideStars> sortResultsByRanking(final List<GemsGuideStars> list) {
        // Sort by ranking and remove duplicates
        final Set<GemsGuideStars> set = new TreeSet<>(list);
        final List<GemsGuideStars> result = new ArrayList<>(set);
        Collections.reverse(result); // put highest ranking first in list
        printResults(result);
        return result;
    }

    private void printResults(final List<GemsGuideStars> result) {
        System.out.println("Results:");
        int i = 0;
        for(GemsGuideStars gemsGuideStars : result) {
            i++;
            System.out.println("result #" + i + ": " + gemsGuideStars);
        }
    }

    // Returns true if all the stars in the given target list are valid for the given group
    private boolean validate(final ObsContext obsContext, final List<Target.SiderealTarget> targetList, final GemsGuideProbeGroup group, final Angle posAngle) {
        for(Target.SiderealTarget target : targetList) {
            if (!validate(obsContext, target, group, posAngle)) {
                System.out.println("Target " + target + " is not valid for " + group.getKey() + " at pos angle " + posAngle);
                return false;
            }
        }
        return true;
    }

    // Returns true if the given target is valid for the given group
    private boolean validate(final ObsContext obsContext, final Target.SiderealTarget target, final GemsGuideProbeGroup group, final Angle posAngle) {
        final ObsContext ctx = obsContext.withPositionAngle(posAngle);
        for (GuideProbe guideProbe : group.getMembers()) {
            if (guideProbe instanceof ValidatableGuideProbe) {
                final ValidatableGuideProbe v = (ValidatableGuideProbe) guideProbe;
                if (v.validate(GemsUtils4Java.toSPTarget(target), ctx)) {
                    return true;
                }
            } else {
                return true; // validation not available
            }
        }
        return false;
    }

    // Returns the first valid guide probe for the given target in the given guide probe group at the given
    // position angle. Note that if tiptiltGroup != group, we're looking for a flexure star, otherwise a
    // tiptilt star.
    // If assignCwfs3ToBrightest is true, the brightest star (in tiptiltTargetList) is assigned to cwfs3,
    // otherwise the second brightest (OT-27).
    // If reverseOrder is true, reverse the order in which guide probes are tried (to make sure to get all
    // combinations of cwfs1 and cwfs2, since cwfs3 is otherwise fixed)
    private Option<GuideProbe> getGuideProbe(final ObsContext obsContext, final Target.SiderealTarget target, final GemsGuideProbeGroup group,
                                             final Angle posAngle, final GemsGuideProbeGroup tiptiltGroup,
                                             final List<GuideProbeTargets> otherTargets, final List<Target.SiderealTarget> tiptiltTargetList,
                                             final boolean assignCwfs3ToBrightest, final boolean reverseOrder) {
        final ObsContext ctx = obsContext.withPositionAngle(posAngle);

        final boolean isFlexure = (tiptiltGroup != group);
        final boolean isTiptilt = !isFlexure;

        if (isFlexure && "ODGW".equals(tiptiltGroup.getKey())) {
            // Special case:
            // If the tip tilt asterism is assigned to the GSAOI ODGW group, then the flexure star must be assigned to CWFS3.
            if (Canopus.Wfs.cwfs3.validate(GemsUtils4Java.toSPTarget(target), ctx)) {
                return new Some<GuideProbe>(Canopus.Wfs.cwfs3);
            }
        } else {
            final List<GuideProbe> members = new ArrayList<GuideProbe>(group.getMembers());
            if (reverseOrder) {
                Collections.reverse(members);
            }
            for (GuideProbe guideProbe : members) {
                boolean valid = validate(ctx, target, guideProbe);
                if (valid) {
                    if (isTiptilt) {
                        valid = checkOtherTargets(guideProbe, otherTargets);
                        if (valid && "CWFS".equals(tiptiltGroup.getKey())) {
                            valid = checkCwfs3Rule(guideProbe, target, tiptiltTargetList, assignCwfs3ToBrightest);
                        }
                    }
                    if (valid) {
                        return new Some<>(guideProbe);
                    }
                }
            }
        }
        return None.instance();
    }

    // Returns true if the given target is valid for the given guide probe
    private boolean validate(final ObsContext ctx, final Target.SiderealTarget target, final GuideProbe guideProbe) {
        boolean valid = !(guideProbe instanceof ValidatableGuideProbe) || ((ValidatableGuideProbe) guideProbe).validate(GemsUtils4Java.toSPTarget(target), ctx);

        // Additional check for mag range (for cwfs1 and cwfs2, since different than cwfs3 and group range)
        if (valid && guideProbe instanceof Canopus.Wfs) {
            final Canopus.Wfs wfs = (Canopus.Wfs) guideProbe;
            final GemsMagnitudeTable.CanopusWfsCalculator canopusWfsCalculator = GemsMagnitudeTable.CanopusWfsMagnitudeLimitsCalculator();
            valid = GemsUtils4Java.containsMagnitudeInLimits(target, canopusWfsCalculator.getNominalMagnitudeConstraints(wfs));
        }
        return valid;
    }

    // Returns true if none of the other targets are assigned the given guide probe.
    //
    // From OT-27: Only one star per GSAOI ODGW is allowed -- for example, if an asterism is formed
    // of two guide stars destined for ODGW2, then it cannot be used.
    //
    // Also for Canopus: only assign one star per cwfs
    private boolean checkOtherTargets(final GuideProbe guideProbe, final List<GuideProbeTargets> otherTargets) {
        for (GuideProbeTargets otherTarget : otherTargets) {
            if (otherTarget.getGuider() == guideProbe) {
                return false;
            }
        }
        return true;
    }

    // Returns true if the given cwfs guide probe can be assigned to the given target according to the rules in OT-27.
    // If assignCwfs3ToBrightest is true, the brightest star in the asterism (in tiptiltTargetList) is assigned to cwfs3,
    // otherwise the second brightest (OT-27).
    private boolean checkCwfs3Rule(final GuideProbe guideProbe, final Target.SiderealTarget target, final List<Target.SiderealTarget> tiptiltTargetList,
                               final boolean assignCwfs3ToBrightest) {
        final boolean isCwfs3 = guideProbe == Canopus.Wfs.cwfs3;
        if (tiptiltTargetList.size() <= 1) {
            return isCwfs3; // single star asterism must be cwfs3
        }

        // sort, put brightest stars first
        final List<Target.SiderealTarget> ar = GemsUtils4Java.sortTargetsByBrightness(tiptiltTargetList);
        // TODO This is a risky deallocation, move it to Scala for safety
        final boolean targetIsBrightest = target.equals(ar.get(0));
        final boolean targetIsSecondBrightest = target.equals(ar.get(1));

        if (isCwfs3) {
            if (assignCwfs3ToBrightest) return targetIsBrightest;
            return targetIsSecondBrightest;
        } else {
            if (assignCwfs3ToBrightest) return !targetIsBrightest;
            return !targetIsSecondBrightest;
        }
    }

    // Returns the stars in the given asterism as a SPTarget list, sorted by R mag, brightest first.
    private List<Target.SiderealTarget> getTargetListFromStrehl(final Strehl strehl) {
        final List<Target.SiderealTarget> targetList = new ArrayList<>();
        for(Star star : strehl.getStars()) {
            targetList.add(GemsUtils4Java.starToSiderealTarget(star));
        }
        return GemsUtils4Java.sortTargetsByBrightness(targetList);
    }

    // OT-33: If the asterism is a Canopus asterism, use R. If an ODGW asterism,
    // see OT-22 for a mapping of GSAOI filters to J, H, and K.
    // If iterating over filters, I think we can assume the filter in
    // the static component as a first pass at least.
    private Magnitude.Band getBandpass(final GemsGuideProbeGroup group, final SPInstObsComp inst) {
        if (group == GsaoiOdgw.Group.instance) {
            if (inst instanceof Gsaoi) {
                final Gsaoi gsaoi = (Gsaoi) inst;
                final Option<Magnitude.Band> band = gsaoi.getFilter().getCatalogBand();
                if (!band.isEmpty()) {
                    return band.getValue();
                }
            }
        }
        return Magnitude.Band.R;
    }

    // REL-426: Multiply the average, min, and max Strehl values reported by Mascot by the following scale
    // factors depending on the filter used in the instrument component of the observation (GSAOI, F2 in the future):
    //   0.2 in J,
    //   0.3 in H,
    //   0.4 in K
    // See OT-22 for the mapping of GSAOI filters to JHK equivalent
    //
    // Update for REL-1321:
    // Multiply the average, min, and max Strehl values reported by Mascot by the following scale factors depending
    // on the filter used in the instrument component of the observation (GSAOI, F2 and GMOS-S in the future) and
    // the conditions:
    //  J: IQ20=0.12 IQ70=0.06 IQ85=0.024 IQAny=0.01
    //  H: IQ20=0.18 IQ70=0.14 IQ85=0.06 IQAny=0.01
    //  K: IQ20=0.35 IQ70=0.18 IQ85=0.12 IQAny=0.01
    public static double getStrehlFactor(final Option<ObsContext> obsContextOption) {
        if (!obsContextOption.isEmpty()) {
            final ObsContext obsContext = obsContextOption.getValue();
            final SPInstObsComp inst = obsContext.getInstrument();
            if (inst instanceof Gsaoi) {
                final Gsaoi gsaoi = (Gsaoi) inst;
                final Option<Magnitude.Band> band = gsaoi.getFilter().getCatalogBand();
                if (!band.isEmpty()) {
                    final String s = band.getValue().name();
                    final SPSiteQuality.Conditions conditions = obsContext.getConditions();
                    if ("J".equals(s)) {
                        if (conditions != null) {
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_20) return 0.12;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_70) return 0.06;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_85) return 0.024;
                        }
                        return 0.01;
                    }
                    if ("H".equals(s)) {
                        if (conditions != null) {
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_20) return 0.18;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_70) return 0.14;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_85) return 0.06;

                        }
                        return 0.01;
                    }
                    if ("K".equals(s)) {
                        if (conditions != null) {
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_20) return 0.35;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_70) return 0.18;
                            if (conditions.iq == SPSiteQuality.ImageQuality.PERCENT_85) return 0.12;
                        }
                        return 0.01;
                    }
                }
            }
        }
        return 0.3;
    }
}
