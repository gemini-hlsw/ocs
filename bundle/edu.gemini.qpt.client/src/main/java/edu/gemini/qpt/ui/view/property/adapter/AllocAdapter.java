package edu.gemini.qpt.ui.view.property.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.gemini.qpt.core.Alloc;
import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Alloc.Circumstance;
import edu.gemini.qpt.shared.sp.Conds;
import edu.gemini.qpt.shared.sp.Obs;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.qpt.ui.view.property.PropertyTable;
import edu.gemini.qpt.ui.view.property.PropertyTable.Adapter;

public class AllocAdapter implements Adapter<Alloc> {

	public void setProperties(Variant variant, Alloc target, PropertyTable table) {

		// Many properties are directly from the obs
		Obs obs = target.getObs();
		ObsAdapter.addObsProperties(variant, obs, table);

		table.put(PROP_TYPE, obs.getObsClass() + " Visit " + obs.getObsId() + " (Band " + obs.getProg().getBand() + ")");
		table.put(PROP_SEQUENCE_PLANNED, toSequenceString(target));
		table.put(PROP_AIRMASS, minMaxMean(target, Alloc.Circumstance.AIRMASS, false));
		table.put(PROP_ELEVATION, minMaxMean(target, Alloc.Circumstance.ELEVATION, "\u00B0", false));
		table.put(PROP_HOUR_ANGLE, minMaxMeanHHMMSS(target, Alloc.Circumstance.HOUR_ANGLE, false));
		table.put(PROP_PARALLACTIC_ANGLE, minMaxMeanParallacticAngle(target, "\u00B0", false));
		table.put(PROP_LUNAR_RANGE, minMaxMean(target, Alloc.Circumstance.LUNAR_DISTANCE, "\u00B0", false));
		table.put(PROP_COORDINATES, obs.getCoords(target.getMiddlePoint()));

		if (variant != null) {
			DateFormat df = new SimpleDateFormat("HH:mm");
			df.setTimeZone(variant.getSchedule().getSite().timezone());
			table.put(PROP_TIME_SLOT, df.format(new Date(target.getStart())) + " - " + df.format(new Date(target.getEnd())));
		}

		Double brightest = target.getMin(Alloc.Circumstance.TOTAL_SKY_BRIGHTNESS, false);
		table.put(PROP_TOTAL_BRIGHTNESS, minMaxMean(target, Alloc.Circumstance.TOTAL_SKY_BRIGHTNESS, "", "Dark", false) + 
				" (" + Conds.getPercentileForSkyBrightness(brightest) + "%)");
				
	}

	private Object minMaxMeanHHMMSS(Alloc target, Circumstance circ, boolean includeSetup) {
		long min = (long) (TimeUtils.MS_PER_HOUR * target.getMin(circ, includeSetup));
		long mean = (long) (TimeUtils.MS_PER_HOUR * target.getMean(circ, includeSetup));
		long max = (long) (TimeUtils.MS_PER_HOUR * target.getMax(circ, includeSetup));
		return 
			"min: " + TimeUtils.msToHHMMSS(min) + " \u2022 " +
			"mean: " + TimeUtils.msToHHMMSS(mean) + " \u2022 " +
			"max: " + TimeUtils.msToHHMMSS(max);
	}

	private String toSequenceString(Alloc alloc) {
		return ObsAdapter.toSequenceString(alloc.getObs(), new Alloc[]{alloc}, alloc.getFirstStep(), alloc.getLastStep(), alloc.getSetupType());
	}

    private String minMaxMean(Alloc a, Alloc.Circumstance circ, boolean includeSetup) {
		return minMaxMean(a, circ, "", includeSetup);
	}

    private String minMaxMean(Alloc a, Alloc.Circumstance circ, String units, boolean includeSetup) {
		return minMaxMean(a, circ, units, "n/a", includeSetup);
	}

    private String minMaxMean(Alloc a, Alloc.Circumstance circ, String units, String na, boolean includeSetup) {
		return 
			"min: " + doubleOrNA(a.getMin(circ, includeSetup), na) + units + " \u2022 " +
			"mean: " + doubleOrNA(a.getMean(circ, includeSetup), na) +units + " \u2022 " +
			"max: " + doubleOrNA(a.getMax(circ, includeSetup), na) + units;
	}

    private String minMaxMeanParallacticAngle(Alloc a, String units, boolean includeSetup) {
        // REL-1441: calculate min, mean and max parallactic angle based on a corrected (continuous) function
        double min = a.getMinParallacticAngle(includeSetup);
        double mean = a.getMeanParallacticAngle(includeSetup);
        double max = a.getMaxParallacticAngle(includeSetup);
        // correct min, mean and max values so that the majority of them lies inside the desired range (-180,180]
        if (mean > 180. && max > 180.) {
            min = min - 360.;
            max = max - 360.;
            mean = mean - 360.;
        } else if (min <= -180. && mean <= -180.) {
            min = min + 360.;
            max = max + 360.;
            mean = mean + 360.;
        }
        return
            "min: " + doubleOrNA(min, "n/a") + units + " \u2022 " +
            "mean: " + doubleOrNA(mean, "n/a") +units + " \u2022 " +
            "max: " + doubleOrNA(max, "n/a") + units;
    }

    private String doubleOrNA(Double d, String na) {
		return d == null ? na : String.format("%1.2f", d);
	}


}
