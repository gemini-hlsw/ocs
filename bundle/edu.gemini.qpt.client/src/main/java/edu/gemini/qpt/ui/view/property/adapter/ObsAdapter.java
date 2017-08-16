package edu.gemini.qpt.ui.view.property.adapter;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.gemini.lch.services.model.Observation;
import edu.gemini.lch.services.model.Visibility;
import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import edu.gemini.spModel.obscomp.InstConstants;

public class ObsAdapter implements Adapter<Obs> {

    private static final Logger LOGGER = Logger.getLogger(ObsAdapter.class.getName());
    private static final String LASER_SHUTTER = "laserShutter";

	public void setProperties(Variant variant, Obs target, PropertyTable table) {
		table.put(PROP_TYPE, target.getObsClass() + " " + target.getObsId() + " (Band " + target.getProg().getBand() + ")");
		addObsProperties(variant, target, table);
	}

	static SimpleDateFormat TW_DF = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
	static {
		TW_DF.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	static void addObsProperties(Variant variant, Obs target, PropertyTable table) {
		table.put(PROP_TITLE, target.getTitle());
		table.put(PROP_INSTRUMENT, target.getInstrumentStringWithConfig());
		table.put(PROP_CONSTRAINTS, target.getConstraintsString());
		table.put(PROP_WFS, target.getWavefrontSensors());
		if (variant != null) {
            Set<Alloc> allocs = variant.getAllocs(target);
            if (allocs != null && allocs.size() != 0) { // LCH-194: when multiple allocs for Obs?
                table.put(PROP_SEQUENCE_FULL, toSequenceString(target, allocs.toArray(new Alloc[]{})));
            }

			table.put(PROP_FLAGS, variant.getFlags(target));
        }
		table.put(PROP_REMAINING_PROGRAM_TIME, TimeUtils.msToHHMMSS(target.getProg().getRemainingProgramTime()));

		if (target.getTimingWindows().isEmpty()) {
			table.put(PROP_TIMING, "\u00ABnone\u00BB");
		} else {
			StringBuilder sb = new StringBuilder();
			for (TimingWindow tw: target.getTimingWindows()) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(TW_DF.format(new Date(tw.getStart())));
				sb.append(" UTC ");
				if (tw.getDuration() == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) {
					sb.append("and remains open forever");
				} else {
					sb.append("+ ");
					sb.append(TimeUtils.msToHMM(tw.getDuration()));
					if (tw.getRepeat() != TimingWindow.REPEAT_NEVER) {
						sb.append(" every ");
						sb.append(TimeUtils.msToHMM(tw.getPeriod()));
						if (tw.getRepeat() == TimingWindow.REPEAT_FOREVER) {
							sb.append(" forever");
						} else {
							sb.append(" x ");
							sb.append(tw.getRepeat());
						}
					}
				}
			}
			table.put(PROP_TIMING, sb);
		}

        Observation observation = LttsServicesClient.getInstance().getObservation(target);
        if (observation != null) {
            try {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for(Visibility.Interval visibility : observation.getScienceTarget().getLaserTarget().getVisibility().getAboveLaserLimit()) {
                    Date start = visibility.getStart();
                    Date end = visibility.getEnd();
                    long duration = end.getTime() - start.getTime();
                    if (i++ != 0) sb.append(", ");
                    sb.append(TW_DF.format(start));
                    sb.append(" - ");
                    sb.append(TW_DF.format(end));
                    sb.append(" UTC ");
                    sb.append(" (+");
                    sb.append(TimeUtils.msToHMM(duration));
                    sb.append(")");
                }
                table.put(PROP_LASER_TIMING, sb);
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Missing laser visibility interval information from LCH server", e);
            }


        }
	}


	static String toSequenceString(Obs obs, Alloc[] a, int firstStep, int lastStep, Alloc.SetupType setupType) {
		PlannedStepSummary steps = obs.getSteps();
		int readySteps = 0;
		StringBuilder builder = new StringBuilder();
		switch (setupType) {
            case NONE: builder.append("No"); break;
            case FULL: builder.append(TimeUtils.msToMMSS(steps.getSetupTime())); break;
            case REACQUISITION: builder.append(TimeUtils.msToMMSS(steps.getReacquisitionTime())); break;
		}
		builder.append(" setup");
		int repeatCount = 0;
		long prevLength = -1;
		String prevType = null;
		long total = steps.getSetupTime();
		for (int i = firstStep; i <= lastStep; i++) {
//			if (steps.isStepExecuted(i)) continue;
			++readySteps;
			long thisLength = steps.getStepTime(i);
			String thisType = steps.getObsType(i);
			total += thisLength;
			if (thisLength == prevLength && equiv(thisType, prevType)) {
				++repeatCount;
			} else {
				append(builder, prevLength, repeatCount, prevType);
				repeatCount = 1;
				prevLength = thisLength;
				prevType = thisType;
			}
		}

        // LCH-194: Show additional time taken into account for shuttering windows in properties
        long shutterTime = 0L;
        for(Alloc alloc : a) {
            shutterTime += alloc.getShutterTime();
        }
        if (shutterTime != 0L) {
            total += shutterTime;
            append(builder, shutterTime, 1, LASER_SHUTTER);
        }

        append(builder, prevLength, repeatCount, prevType);
		builder.append(" = ").append(TimeUtils.msToHHMMSS(total));
		builder.append(" in ").append(readySteps);
		builder.append(readySteps == 1 ? " step" : " steps");
		builder.append(readySteps == 1 ? " (" + (firstStep + 1) + ")" : " (" + (firstStep + 1) + "-" + (lastStep + 1) + ")");
		return builder.toString();
	}

	private static boolean equiv(Object a, Object b) {
		return a == null ? b == null : a.equals(b);
	}


	static String toSequenceString(Obs obs, Alloc[] a) {
		return toSequenceString(obs, a, obs.getFirstUnexecutedStep(), obs.getSteps().size() - 1, Alloc.SetupType.FULL);
	}

	static void append(StringBuilder builder, long length, int repeatCount, String type) {
		if (type != null) {
			type = STEP_TYPE_DISPLAY_NAME.get(type);

		}
		if (repeatCount > 0) {
			builder.append(" + ");
			if (repeatCount > 1) {
				builder.append(repeatCount);
				builder.append(" x ");
			}
			builder.append(TimeUtils.msToMMSS(length));
			if (type != null)
				builder.append(" ").append(type).append(" ");
		}
	}

	private static final Map<String, String> STEP_TYPE_DISPLAY_NAME = new TreeMap<String, String>();
	static {
		// this is terrible
		STEP_TYPE_DISPLAY_NAME.put(InstConstants.ARC_OBSERVE_TYPE, "arc");
		STEP_TYPE_DISPLAY_NAME.put(InstConstants.BIAS_OBSERVE_TYPE, "bias");
		STEP_TYPE_DISPLAY_NAME.put(InstConstants.CAL_OBSERVE_TYPE, "cal");
		STEP_TYPE_DISPLAY_NAME.put(InstConstants.DARK_OBSERVE_TYPE, "dark");
		STEP_TYPE_DISPLAY_NAME.put(InstConstants.FLAT_OBSERVE_TYPE, "flat");
		STEP_TYPE_DISPLAY_NAME.put(InstConstants.SCIENCE_OBSERVE_TYPE, "obs");
        STEP_TYPE_DISPLAY_NAME.put(LASER_SHUTTER, "laser shutter");
	}

}
