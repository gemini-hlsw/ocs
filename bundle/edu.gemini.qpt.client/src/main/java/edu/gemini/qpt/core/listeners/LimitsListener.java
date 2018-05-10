package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.function.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Marker;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Alloc.Circumstance;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.core.util.*;
import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;

/**
 * Generates Alloc markers.
 * @author rnorris
 */
public class LimitsListener extends MarkerModelListener<Variant> {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(LimitsListener.class.getName());

	private static final DateTimeFormatter dateFormat(TimeZone zone) {
		return DateTimeFormatter.ofPattern("HH:mm").withZone(zone.toZoneId());
	};

	private static final String formatInterval(TimeZone zone, Interval interval) {
		final Function<Long, String> f =
			ms -> dateFormat(zone).format(Instant.ofEpochMilli(ms));

		return String.format("(%s, %s)", f.apply(interval.getStart()), f.apply(interval.getEnd()));
	}

    /**
     * LGS obs have a 40 deg limit by default
     */
    public static final int MIN_ELEVATION_ERROR_LIMIT = 40;
    public static final int MIN_ELEVATION_WARN_LIMIT = 42;

	public void propertyChange(PropertyChangeEvent evt) {
		Variant v = (Variant) evt.getSource();
		MarkerManager mm = v.getSchedule().getMarkerManager();
		mm.clearMarkers(this, v);

		final TimeZone zone = v.getSchedule().getSite().timezone();

		for (Alloc a: v.getAllocs()) {

			// Add a marker if better-scoring observations are unscheduled
			ScoreMarker.add(this, mm, v, a);

			// Solo in scheduling group
			Group g = a.getObs().getGroup();
			if (g != null && g.getType() == GroupType.TYPE_SCHEDULING && a.getGroupIndex() == -1 && g.getObservations().size() > 1 /* QPT-226 */)
				mm.addMarker(false, this, Severity.Warning, "Observation is part of a scheduling group, but no other members appear in plan.", v, a);

			// Below horizon!
			if (a.getMin(Circumstance.AIRMASS, true) == 0.0) {
				mm.addMarker(false, this, Severity.Error, "Target is below horizon.", v, a);
			}

			// Lower tracking limit
			final Double absoluteMinElevation = a.getMin(Circumstance.ELEVATION, true);
			if (absoluteMinElevation < 17.5) {
				mm.addMarker(false, this, Severity.Error, String.format("Tracking: target reaches %1.2f\u00B0.", absoluteMinElevation), v, a);
			} else if (absoluteMinElevation < 20.0) {
				mm.addMarker(false, this, Severity.Warning, String.format("Tracking: target reaches %1.2f\u00B0.", absoluteMinElevation), v, a);
			}

			// Upper tracking limit
			final Double maxElevation = a.getMax(Circumstance.ELEVATION, true);
			if (maxElevation > 88.0) {
				mm.addMarker(false, this, Severity.Warning, String.format("Tracking: target reaches %1.2f\u00B0.", maxElevation), v, a);
			}

			// Function to create markers that report solver intervals.
			final BiFunction<String, String, Option<Marker>> createSolverMarker =
					(cacheName, prefix) -> {
						final Map<Obs, Union<Interval>> c = v.getSchedule().getCache(cacheName);
						final Union<Interval>          ws = c.getOrDefault(a.getObs(), new Union<>());
						final ImList<Interval>         is = DefaultImList.create(ws.getIntervals());

						if (is.isEmpty()) {
							return None.instance();
						} else {
							final String m = is.map(i -> formatInterval(zone, i)).mkString(prefix + " ", ", ", ".");
							return new Some<>(new Marker(false, this, Severity.Notice, m, v, a));
						}
					};

			// Timing window
			int percentVisible = (int) (100 * a.getMean(Circumstance.TIMING_WINDOW_OPEN, false));
			switch (percentVisible) {
			case 100: // good!
				if (a.getObs().getTimingWindows().size() > 0) {
					mm.addMarker(true, this, Severity.Info, "Timing constraint is met.", v, a);

					// Report timing windows
					createSolverMarker.apply(Variant.TIMING_UNION_CACHE, "Must be in the timing window").foreach(m -> mm.addMarker(m));
				}
                break;
			case 0:
				mm.addMarker(false, this, Severity.Error, "Timing constraint is violated for entire scheduled visit.", v, a);
				break;
			default:
				mm.addMarker(false, this, Severity.Error, String.format("Timing constraint is violated for %d%% of scheduled visit.", 100 - percentVisible), v, a);
				break;

			}

			// LCH Laser Shutter Warnings
            String msg = LttsServicesClient.getInstance().getShutterWindowWarningMessage(a);
            if (msg != null) {
                mm.addMarker(false, this, Severity.Warning, msg, v, a);
            }

            // Elevation Constraint
            // Elevation Constraint
			final Double maxAirmass = a.getMax(Circumstance.AIRMASS, false);
			final Double minAirmass = a.getMin(Circumstance.AIRMASS, false);

			switch (a.getObs().getElevationConstraintType()) {

			case NONE:


				// [QPT-225]
//				if (a.getObs().getOptions().contains(AltairParams.GuideStarType.LGS)) {
                if (a.getObs().getLGS()) {

					// LGS obs have a 40 deg limit by default
                    // LCH-190: copy "\u00B0" pastes as "°" in Idea, but that didn't display correctly on user's machine for some reason...
					final Double minElevation = a.getMin(Circumstance.ELEVATION, false);
					if (minElevation < MIN_ELEVATION_ERROR_LIMIT) {
						mm.addMarker(false, this, Severity.Error, String.format("LGS observation reaches elevation %1.2f\u00B0.", minElevation), v, a);
					} else if (minElevation < MIN_ELEVATION_WARN_LIMIT) {
						mm.addMarker(false, this, Severity.Warning, String.format("LGS observation reaches elevation %1.2f\u00B0.", minElevation), v, a);
					}

				} else {

					// Legacy behavior; airmass 2.0 limit
					if (maxAirmass > 2.0) {
						mm.addMarker(false, this, Severity.Error, String.format("Observation reaches airmass %1.2f.", maxAirmass), v, a);
					} else if (maxAirmass > 1.75) {
						mm.addMarker(false, this, Severity.Warning, String.format("Observation reaches airmass %1.2f.", maxAirmass), v, a);
					}
				}

				break;

			case AIRMASS:

				final double airmassLimitMin = a.getObs().getElevationConstraintMin();
				final double airmassLimitMax = a.getObs().getElevationConstraintMax();

				Option<Marker> airmassMarker = None.instance();
				if (maxAirmass > airmassLimitMax) {
					airmassMarker = new Some<>(new Marker(false, this, Severity.Error, String.format("Airmass constraint violated (%1.2f > %1.2f).", maxAirmass, airmassLimitMax), v, a));
				} else if (maxAirmass > airmassLimitMax * 0.975) {
					airmassMarker = new Some<>(new Marker(false, this, Severity.Warning, String.format("Observation reaches airmass %1.2f.", maxAirmass), v, a));
				}
				if (airmassLimitMin > 1.0) {
					if (minAirmass < airmassLimitMin) {
						airmassMarker = new Some<>(new Marker(false, this, Severity.Error, String.format("Airmass constraint violated (%1.2f < %1.2f).", minAirmass, airmassLimitMin), v, a));
					} else if (maxAirmass < airmassLimitMin * 1.025) {
						airmassMarker = new Some<>(new Marker(false, this, Severity.Warning, String.format("Observation reaches airmass %1.2f.", maxAirmass), v, a));
					}
				}

				airmassMarker.orElse(() -> {
					final String m = String.format("Must be observed in the airmass range %1.2f - %1.2f", airmassLimitMin, airmassLimitMax);
					return createSolverMarker.apply(Variant.VISIBLE_UNION_CACHE, m);
				}).foreach(m -> mm.addMarker(m));

				break;


			case HOUR_ANGLE:

				double haLimitMin = a.getObs().getElevationConstraintMin();
				double haLimitMax = a.getObs().getElevationConstraintMax();
				double minHA = a.getMin(Circumstance.HOUR_ANGLE, false);
				double maxHA = a.getMax(Circumstance.HOUR_ANGLE, false);

				double minDelta = Math.abs(haLimitMin - minHA);
				double maxDelta = Math.abs(haLimitMax - maxHA);

				Option<Marker> haMarker = None.instance();
				if (minHA < haLimitMin) {
					haMarker = new Some<>(new Marker(false, this, Severity.Error, String.format("Hour angle constraint violated (%s < %s).", TimeUtils.hoursToHHMMSS(minHA), TimeUtils.hoursToHHMMSS(haLimitMin)), v, a));
				} else if (minDelta < 1. / 15.) {
					haMarker = new Some<>(new Marker(false, this, Severity.Warning, "Target comes within 1\u00B0 of lower HA constraint.", v, a));
				}

				if (maxHA > haLimitMax) {
					haMarker = new Some<>(new Marker(false, this, Severity.Error, String.format("Hour angle constraint violated (%s > %s).", TimeUtils.hoursToHHMMSS(maxHA), TimeUtils.hoursToHHMMSS(haLimitMax)), v, a));
				} else if (maxDelta < 1. / 15.) {
					haMarker = new Some<>(new Marker(false, this, Severity.Warning, "Target comes within 1\u00B0 of upper HA constraint.", v, a));
				}

				haMarker.orElse(() -> {
					final String m = String.format("Must be observed in the hour angle range %1.2f - %1.2f", haLimitMin, haLimitMax);
					return createSolverMarker.apply(Variant.VISIBLE_UNION_CACHE, m);
				}).foreach(m -> mm.addMarker(m));

				break;
			}

			// Lunar proximity warnings
			double obj_moon = a.getMin(Circumstance.LUNAR_DISTANCE, false);
			if (obj_moon < 5.) {
				mm.addMarker(false, this, Severity.Error, String.format("Target approaches within %1.2f\u00B0 of the moon.", obj_moon), v, a);
			} else if (obj_moon < 15.) {
				mm.addMarker(false, this, Severity.Warning, String.format("Target approaches within %1.2f\u00B0 of the moon.", obj_moon), v, a);
			}

			// Sky brightness warnings

			Double sb = a.getMin(Circumstance.TOTAL_SKY_BRIGHTNESS, false);

			if (sb != null && !a.getObs().getConditions().containsSkyBrightness(sb)) {
				mm.addMarker(false, this, Severity.Error, "Sky brightness constraint violated.", v, a);
			} else  {
				createSolverMarker.apply(Variant.DARK_UNION_CACHE, "Background constraints are met between").foreach(m -> mm.addMarker(m));
			}


			// Variant Obs Flags
			for (Flag flag: v.getFlags(a.getObs())) {
				switch (flag) {

                case LGS_UNAVAILABLE:
                    mm.addMarker(false, this, Severity.Error, "LGS observation is not allowed in non-LGS variant.", v, a);
                    break;

				case CONFIG_UNAVAILABLE:
					mm.addMarker(false, this, Severity.Error, "Required instrument configuration is unavailable.", v, a);
					break;

				case MASK_IN_CABINET:
					mm.addMarker(false, this, Severity.Error, "Required custom mask is in cabinet.", v, a);
					break;

				case MASK_UNAVAILABLE:
					mm.addMarker(false, this, Severity.Error, "Required custom mask is unavailable.", v, a);
					break;

				case INSTRUMENT_UNAVAILABLE:
					mm.addMarker(false, this, Severity.Error, "Required instrument is unavailable.", v, a);
					break;

				case CC_UQUAL:
					mm.addMarker(false, this, Severity.Error, "Variant CC is under-qualified for this observation.", v, a);
					break;

				case WV_UQUAL:
					mm.addMarker(false, this, Severity.Error, "Variant WV is under-qualified for this observation.", v, a);
					break;

				case IQ_UQUAL:
					mm.addMarker(false, this, Severity.Error, "Variant IQ is under-qualified for this observation.", v, a);
					break;

				case INACTIVE:
					mm.addMarker(false, this, Severity.Error, "Science program is inactive.", v, a);
					break;

				case OVER_QUALIFIED:
					mm.addMarker(true, this, Severity.Info, "Variant conditions are better than necessary.", v, a);
					break;

				case IN_PROGRESS:
				case ELEVATION_CNS:
				case SCHEDULED:
				case BLOCKED:
					// ...
					// No markers for these guys.
					break;

				}
			}
		}

	}

	@Override
	protected MarkerManager getMarkerManager(Variant t) {
		return t.getSchedule().getMarkerManager();
	}

}


// REL-397: warn if another unscheduled obs in the same program has a higher score and is within 1.5h in RA.
class ScoreMarker {

	private static final double RA_LIMIT_H = 1.5; // we only care about other observations within this distance in RA
	private static final double RA_LIMIT_DEG = RA_LIMIT_H * 15; // 15 degrees per hour
	private static final String PREFIX = String.format("Higher-scoring observation(s) within %2.1fH: ", RA_LIMIT_H);

	public static void add(Object owner, MarkerManager mm, Variant v, Alloc a) {

		// unscheduled observations
		final Predicate<Obs> unscheduled = obs ->
			!v.getFlags(obs).contains(Flag.SCHEDULED);

		// observations within RA_LIMIT_DEG of `a`
		final Predicate<Obs> nearby = otherObs -> {
			final double myRaDeg    = a.getObs().getRa(a.getStart()); // My RA at scheduling time
			final double otherRaDeg = otherObs.getRa(a.getStart());
			final double raDiffDeg  = Math.abs(myRaDeg - otherRaDeg);
			return raDiffDeg <= RA_LIMIT_DEG;
		};

		// observations with a better score than `a.getObs`
		final Predicate<Obs> higherScoring = otherObs -> {
			final double myScore    = v.getScore(a.getObs());
			final double otherScore = v.getScore(otherObs);
			return otherScore > myScore;
		};

		// all of the above, ordered by cost to compute
		final Predicate<Obs> all =
			unscheduled.and(nearby).and(higherScoring);

		// observations in the same program as `a` that meet all filter conditions
		final List<Obs> alternatives =
			a.getObs().getProg().getFullObsSet().stream().filter(all).collect(Collectors.toList());

		// if there are any, add a marker
		if (!alternatives.isEmpty()) {
			final String msg =
				alternatives.stream().map(Obs::getObsId).collect(Collectors.joining(" ", PREFIX, ""));
			mm.addMarker(true, owner, Severity.Warning, msg, v, a);
		}

	}

}