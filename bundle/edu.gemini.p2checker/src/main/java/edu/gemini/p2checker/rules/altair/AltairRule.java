//
// $
//

package edu.gemini.p2checker.rules.altair;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.p2checker.util.PositionOffsetChecker;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Trio;
import edu.gemini.shared.util.immutable.Tuple3;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;

/**
 * Rules for observations containing an Altair node.
 * See REL-386.
 */
public final class AltairRule implements IRule {
    private static final String PREFIX = "AltairGuideStarRule_";

    // Error/warning messages
    private static final String ND_FILTER_WITH_LGS = "The ND filter is not commissioned for use with LGS";
    private static final String CASSEGRAIN_FIXED = "The Cassegrain Rotator is fixed so the field orientation will rotate on the sky";
    private static final String ND_FILTER_NGS_FAINT = "The ND filter is not necessary for stars fainter than R=6.5";
    private static final String ND_FILTER_NGS_BRIGHT = "The ND filter must be used for stars brighter than R=5.5";
    private static final String NO_FIELD_LENS = "The Altair field lens is recommended for off-axis targets";
    private static final String LGS_NO_FIELD_LENS = "Fieldlens should be used in the LGS mode";
//    private static final String NO_ND_FILTER_LGS_BRIGHT = "Altair ND filter should be used for LGS stars brighter than R=10.5";
    private static final String ND_FILTER_LGS_BRIGHT = "Altair LGS cannot be used for LGS stars brighter than R=2.5 even with the ND filter";
    private static final String LGS_FAINT = "Altair LGS is not commissioned for guide stars fainter than R=18.5";
    private static final String LGS_SB_FAINT = "Altair LGS with guide stars fainter than R=18.0 requires SB=50%";
    private static final String LGS_IN_RANGE = "Altair LGS with guide stars 17.5 < R < 18.0 requires SB=80%";
    private static final String LGS_SB_ANY = "SB=Any is recommended for Altair LGS guide stars brighter than R=17.5";
    private static final String LGS_PHOTOMETRIC = "Altair LGS requires photometric conditions (CC=50%)";
    private static final String LGS_BRIGHT = "Altair NGS may provide better performance and lower overheads than LGS with guide stars brighter than R=13.5";
    private static final String LGS_TOO_BRIGHT = "Stars brighter than R=10.5 cannot be done in LGS mode, use NGS instead.";

    private static final String GS_EDGE_WARN = "The AOWFS guide star falls close to the edge of the guide probe field and may not be accessible at one or more offset positions.";
    private static final String GS_RANGE_ERROR = "The AOWFS guide star falls out of the range of the guide probe field one or more offset positions.";

    public static final AltairRule INSTANCE = new AltairRule();

    private AltairRule() {
    }

    private boolean validateObs(ObservationElements elements) {
        if (!elements.hasAltair())
            return false;

        SPInstObsComp inst = elements.getInstrument();
        if (inst == null) return false;

        //SPComponentType type = inst.getType();
        //return type.equals(InstNIRI.SP_TYPE) || type.equals(InstNIFS.SP_TYPE) || type.equals(InstGNIRS.SP_TYPE);
        return true;
    }

    /**
     * Altair checks.
     * See See REL-386 for details.
     *
     * @param elements ObservationElements object that contains the observation
     *                 components needed by the rule to perform the checking.
     * @return the problems found
     * @
     */
    public IP2Problems check(ObservationElements elements)  {
        if (!validateObs(elements)) return null; // does not apply
        IP2Problems problems = new P2Problems();
        for (ISPProgramNode aoNode : elements.getAOComponentNode()) {
            for (InstAltair altair : elements.<InstAltair>getAOComponent()) {
                if (altair.getNdFilter() == AltairParams.NdFilter.IN && altair.getGuideStarType() == AltairParams.GuideStarType.LGS) {
                    // ND Filter = YES and LGS = YES (Error: The ND filter is not commissioned for use with LGS)
                    problems.addError(PREFIX + "ND_FILTER_WITH_LGS", ND_FILTER_WITH_LGS, aoNode);
                }

                if (altair.getCassRotator() == AltairParams.CassRotator.FIXED) {
                    // Altair use with Cassegrain Rotator Fixed: (Warning: The Cassegrain Rotator is fixed so the field orientation will rotate on the sky.)
                    problems.addWarning(PREFIX + "CASSEGRAIN_FIXED", CASSEGRAIN_FIXED, aoNode);
                }

                problems.append(checkNgsLgs(elements));
            }
        }

        // Check for offsets outside the guide probe FOV.
        problems.append(checkOffsets(elements));

        return problems;
    }

    // REL-393: Check for offsets outside the guide probe FOV.
    // This code largely taken from GmosOiwfsGuideProbe.
    private IP2Problems checkOffsets(ObservationElements elements) {
        Option<ObsContext> opt = elements.getObsContext();
        if (opt.isEmpty()) return null;  // nothing to check

        ObsContext ctx = opt.getValue();

        P2Problems problems = new P2Problems();
        for (ISPProgramNode targetNode : elements.getTargetObsComponentNode()) {

            // Check the OIWFS guide stars.
            if (elements.hasAltair()) {
                for (AltairAowfsGuider guider : AltairAowfsGuider.values()) {
                    TargetEnvironment env = ctx.getTargets();
                    Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
                    if (gtOpt.isEmpty()) break; // okay, no targets to check

                    GuideProbeTargets gt = gtOpt.getValue();
                    Option<SPTarget> primaryOpt = gt.getPrimary();
                    if (primaryOpt.isEmpty()) break; // okay, no target to check

                    SPTarget primary = primaryOpt.getValue();

                    guider.checkBoundaries(primary, ctx).foreach(bs -> {
                        switch (bs) {
                            case inside:
                                break;
                            case innerBoundary:
                            case outerBoundary:
                                problems.addWarning(PREFIX + "GS_EDGE_WARN", GS_EDGE_WARN, targetNode);
                                break;
                            case outside:
                                problems.addError(PREFIX + "GS_RANGE_ERROR", GS_RANGE_ERROR, targetNode);
                                break;
                        }
                    });
                }
            }
        }
        return problems;
    }
    // End REL-393.

    // Checks based on the NGS/LGS settings
    private IP2Problems checkNgsLgs(ObservationElements elements) {
        IP2Problems problems = new P2Problems();

        for (TargetObsComp target : elements.getTargetObsComp()) {
            Tuple3<Double, Double, Boolean> minMaxMag = getMinMaxMagnitudeInVorR(target, 17.5, 18.0);
            final Double minMag;
            final Double maxMag;
            final boolean inRange;
            if (minMaxMag == null) {
                minMag = null;
                maxMag = null;
                inRange = false;
            } else {
                minMag = minMaxMag._1();
                maxMag = minMaxMag._2();
                inRange = minMaxMag._3();
            }

            for (ISPProgramNode aoNode : elements.getAOComponentNode()) {
                for (InstAltair altair : elements.<InstAltair>getAOComponent()) {
                    boolean isPwfs  = (altair.getMode() == AltairParams.Mode.LGS_P1);
                    boolean isAowfs = (altair.getMode() == AltairParams.Mode.LGS);
                    if (altair.getGuideStarType() == AltairParams.GuideStarType.NGS) {
                        final boolean offAxis = altairOffAxisGuiding(target.getTargetEnvironment(), elements.getSchedulingBlockStart());
                        if (offAxis && altair.getFieldLens() == AltairParams.FieldLens.OUT) {
                            //Altair NGS without Field Lens (Warning: The Altair field lens is recommended for off-axis targets.)
                            problems.addWarning(PREFIX + "NO_FIELD_LENS", NO_FIELD_LENS, aoNode);
                        }
                        if (!isPwfs && minMag != null) {
                            if (altair.getNdFilter() == AltairParams.NdFilter.IN && maxMag > 6.5) {
                                // Altair NGS and ND Filter=In and guide star (V or R)>6.5 (Warning: The ND filter is not necessary for stars fainter than R=6.5)
                                problems.addWarning(PREFIX + "ND_FILTER_NGS_FAINT", ND_FILTER_NGS_FAINT, aoNode);
                            } else if (altair.getNdFilter() == AltairParams.NdFilter.OUT && minMag < 5.5) {
                                // Altair NGS and ND Filter=Out and guide star (V or R)<5.5 (Warning: The ND filter must be used for stars brighter than R=5.5)
                                problems.addWarning(PREFIX + "ND_FILTER_NGS_BRIGHT", ND_FILTER_NGS_BRIGHT, aoNode);
                            }
                        }
                    } else if (altair.getGuideStarType() == AltairParams.GuideStarType.LGS) {
                        // REL-778: Check the position offsets. Maximum distance from base that is allowed is 5 arcmin.
                        if (PositionOffsetChecker.hasBadOffsets(elements)) {
                            problems.addError(PREFIX + PositionOffsetChecker.PROBLEM_CODE(), PositionOffsetChecker.PROBLEM_MESSAGE(), elements.getSeqComponentNode());
                        }

                        if (altair.getFieldLens() == AltairParams.FieldLens.OUT) {
                            // Moved here from GeneralRule
                            problems.addWarning(PREFIX + "LGS_NO_FIELD_LENS", LGS_NO_FIELD_LENS, aoNode);
                        }
                        if (minMag != null) {
                            if (isAowfs) {
                                if (minMag < 13.5) {
                                    // Altair LGS with guide star (V or R)<13.5, (Warning: Altair NGS may provide better performance and lower overheads than LGS with guide stars brighter than R=13.5.
                                    problems.addWarning(PREFIX + "LGS_BRIGHT", LGS_BRIGHT, aoNode);
                                }
                            }
                            if (!isPwfs) {
                                if (minMag < 2.5) {
                                    // Altair LGS with guide star (V or R) < 2.5 and ND filter (Warning: Altair LGS cannot be used for LGS stars brighter than R=2.5 even with the ND filter)
                                    problems.addWarning(PREFIX + "ND_FILTER_LGS_BRIGHT", ND_FILTER_LGS_BRIGHT, aoNode);
                                } else if (minMag < 10.5) {
                                    // For LGS stars brighter than 10.5: Please use the following warning. "Stars brighter than R=10.5 cannot be done in LGS mode, use NGS instead."
                                    problems.addWarning(PREFIX + "LGS_TOO_BRIGHT", LGS_TOO_BRIGHT, aoNode);
                                }

                                if (maxMag > 18.5) {
                                    // Altair LGS with guide star (V or R)>18.5 (Error: Altair LGS is not commissioned for guide stars fainter than R=18.5)
                                    problems.addError(PREFIX + "LGS_FAINT", LGS_FAINT, aoNode);
                                }

                                for (ISPObsComponent siteQualityNode : elements.getSiteQualityNode()) {
                                    for (SPSiteQuality siteQuality : elements.getSiteQuality()) {
                                        if (siteQuality != null) {
                                            SPSiteQuality.SkyBackground sb = siteQuality.getSkyBackground();
                                            if (maxMag > 18.0 && (sb == SPSiteQuality.SkyBackground.PERCENT_80 || sb == SPSiteQuality.SkyBackground.ANY)) {
                                                // Altair LGS with guide star (V or R)>18.0 and SB=(80% or Any) (Error: Altair LGS with guide stars fainter than R=18.0 requires SB=50%)
                                                problems.addError(PREFIX + "LGS_SB_FAINT", LGS_SB_FAINT, siteQualityNode);
                                            }
                                            if (inRange && sb == SPSiteQuality.SkyBackground.ANY) {
                                                // Altair LGS with guide star 17.5 < (V or R) < 18.0 and SB=Any (Error: Altair LGS with guide stars 17.5 < R < 18.0 requires SB=80%)
                                                problems.addError(PREFIX + "LGS_IN_RANGE", LGS_IN_RANGE, siteQualityNode);
                                            }
                                            if (minMag < 17.5 && sb != SPSiteQuality.SkyBackground.ANY) {
                                                // Altair LGS with guide star (V or R)<17.5 and SB not equal to Any (Warning: SB=Any is recommended for Altair LGS guide stars brighter than R=17.5)
                                                problems.addWarning(PREFIX + "LGS_SB_ANY", LGS_SB_ANY, siteQualityNode);
                                            }
                                            if (siteQuality.getCloudCover() != SPSiteQuality.CloudCover.PERCENT_50) {
                                                // Altair LGS with CC not equal to 50% (Error: Altair LGS requires photometric conditions (CC=50%))
                                                problems.addError(PREFIX + "LGS_PHOTOMETRIC", LGS_PHOTOMETRIC, siteQualityNode);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return problems;
    }

    // Returns the min and max magnitudes of all the guide stars in the V or R bands as well as a boolean value
    // indicating if any of the magnitudes fall in the given range. May return null, if no values were defined.
    private static Tuple3<Double, Double, Boolean> getMinMaxMagnitudeInVorR(TargetObsComp targetObsComp, double lower, double upper) {
        GuideGroup guideGroup;
        try {
            guideGroup = targetObsComp.getTargetEnvironment().getGuideEnvironment().getPrimary();
        } catch(NullPointerException e) {
            guideGroup = null;
        }
        if (guideGroup != null) {
            Double minMag = null;
            Double maxMag = null;
            boolean inRange = false;
            MagnitudeBand[] bands = new MagnitudeBand[]{MagnitudeBand.R$.MODULE$, MagnitudeBand.V$.MODULE$};
            for (SPTarget spTarget : guideGroup.getTargets()) {
                for (MagnitudeBand band : bands) {
                    final scala.Option<Magnitude> om = spTarget.getMagnitude(band);
                    final Magnitude m = om.isDefined() ? om.get() : null;
                    if (m != null) {
                        double b = m.value();
                        if (minMag == null || b < minMag) minMag = b;
                        if (maxMag == null || b > maxMag) maxMag = b;
                        if (b > lower && b < upper) inRange = true;
                    }
                }
            }
            if (minMag != null)
                return new Trio<Double, Double, Boolean>(minMag, maxMag, inRange);
        }
        return null;
    }

    // Check if the primary guide target for Altair is off-axis (i.e. has different coordinates from base target).
    private static boolean altairOffAxisGuiding(final TargetEnvironment targets, final Option<Long> when) {
        final Option<GuideProbeTargets> altairTargets = targets.getPrimaryGuideProbeTargets(AltairAowfsGuider.instance);
        if (altairTargets.isDefined()) {
            final Option<SPTarget> primaryAltairTarget = altairTargets.getValue().getPrimary();
            if (primaryAltairTarget.isDefined()) {
                final Option<Coordinates> science = targets.getAsterism().getSkycalcCoordinates(when);
                final Option<Coordinates> altair  = primaryAltairTarget.getValue().getSkycalcCoordinates(when);
                return !science.equals(altair);
            }
        }
        return false;
    }
}
