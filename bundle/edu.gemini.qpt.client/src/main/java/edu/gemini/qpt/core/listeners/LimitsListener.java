package edu.gemini.qpt.core.listeners;

import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Alloc.Circumstance;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Group;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.core.util.MarkerManager;
import edu.gemini.shared.util.DateTimeUtils;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;

/**
 * Generates Alloc markers if the alloc reaches elevation or airmass limits.
 * @author rnorris
 */
public class LimitsListener extends MarkerModelListener<Variant> {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(LimitsListener.class.getName());

    /**
     * LGS obs have a 40 deg limit by default
     */
    public static final int MIN_ELEVATION_ERROR_LIMIT = 40;
    public static final int MIN_ELEVATION_WARN_LIMIT = 42;

	public void propertyChange(PropertyChangeEvent evt) {
		Variant v = (Variant) evt.getSource();
		MarkerManager mm = v.getSchedule().getMarkerManager();
		mm.clearMarkers(this, v);

		for (Alloc a: v.getAllocs()) {

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

			// Timing window
			int percentVisible = (int) (100 * a.getMean(Circumstance.TIMING_WINDOW_OPEN, false));
			switch (percentVisible) {
			case 100: // good!
				if (a.getObs().getTimingWindows().size() > 0)
					mm.addMarker(true, this, Severity.Info, "Timing constraint is met.", v, a);
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
				
				double airmassLimitMin = a.getObs().getElevationConstraintMin();
				double airmassLimitMax = a.getObs().getElevationConstraintMax();
				
				if (maxAirmass > airmassLimitMax) {
					mm.addMarker(false, this, Severity.Error, String.format("Airmass constraint violated (%1.2f > %1.2f).", maxAirmass, airmassLimitMax), v, a);
				} else if (maxAirmass > airmassLimitMax * 0.975) {
					mm.addMarker(false, this, Severity.Warning, String.format("Observation reaches airmass %1.2f.", maxAirmass), v, a);
				}
				if (airmassLimitMin > 1.0) {
					if (minAirmass < airmassLimitMin) {
						mm.addMarker(false, this, Severity.Error, String.format("Airmass constraint violated (%1.2f < %1.2f).", minAirmass, airmassLimitMin), v, a);
					} else if (maxAirmass < airmassLimitMin * 1.025) {
						mm.addMarker(false, this, Severity.Warning, String.format("Observation reaches airmass %1.2f.", maxAirmass), v, a);
					}
				}
				break;
				
				
			case HOUR_ANGLE:				
				
				double haLimitMin = a.getObs().getElevationConstraintMin();
				double haLimitMax = a.getObs().getElevationConstraintMax();
				double minHA = a.getMin(Circumstance.HOUR_ANGLE, false);
				double maxHA = a.getMax(Circumstance.HOUR_ANGLE, false);
				
				double minDelta = Math.abs(haLimitMin - minHA);
				double maxDelta = Math.abs(haLimitMax - maxHA);
				
				if (minHA < haLimitMin) {
					mm.addMarker(false, this, Severity.Error, String.format("Hour angle constraint violated (%s < %s).", DateTimeUtils.hoursToHMMSS(minHA), DateTimeUtils.hoursToHMMSS(haLimitMin)), v, a);
				} else if (minDelta < 1. / 15.) {
					mm.addMarker(false, this, Severity.Warning, "Target comes within 1\u00B0 of lower HA constraint.", v, a);
				}

				if (maxHA > haLimitMax) {
					mm.addMarker(false, this, Severity.Error, String.format("Hour angle constraint violated (%s > %s).", DateTimeUtils.hoursToHMMSS(maxHA), DateTimeUtils.hoursToHMMSS(haLimitMax)), v, a);
				} else if (maxDelta < 1. / 15.) {
					mm.addMarker(false, this, Severity.Warning, "Target comes within 1\u00B0 of upper HA constraint.", v, a);
				}

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
			if (sb != null && !a.getObs().getConditions().containsSkyBrightness(sb)) 
				mm.addMarker(false, this, Severity.Error, "Sky brightness constraint violated.", v, a);

			
			// Variant Obs Flags
			for (Flag flag: v.getFlags(a.getObs())) {
				switch (flag) {

                case LGS_UNAVAILABLE:
                    mm.addMarker(false, this, Severity.Error, "LGS observation is not allowed in non-LGS variant.", v, a);
                    break;

				case CONFIG_UNAVAILABLE:
					mm.addMarker(false, this, Severity.Error, "Required instrument configuration is unavailable.", v, a);
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
