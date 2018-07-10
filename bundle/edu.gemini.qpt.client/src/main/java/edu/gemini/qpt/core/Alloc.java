package edu.gemini.qpt.core;

import java.util.Date;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Logger;

import edu.gemini.qpt.core.util.LttsServicesClient;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import jsky.coords.WorldCoords;
import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.shared.sp.*;
import edu.gemini.qpt.core.util.Commentable;
import edu.gemini.qpt.core.util.ImprovedSkyCalc;
import edu.gemini.qpt.core.util.Interval;
import edu.gemini.qpt.shared.util.*;
import edu.gemini.qpt.core.util.TimingWindowSolver;
import edu.gemini.qpt.core.util.Variants.AbandonedSuccessorException;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

/**
 * An interval representing a scheduled observation. Instances are immutable value types.
 * The comment appears to be mutable,  but it's actually stored in the variant. This allows
 * us to change the comment without newing up another Alloc.
 * <p>
 * Instances are constructed via Variant::addAlloc().
 * @see Variant#addAlloc(Obs, Long, Long)
 * @author rnorris
 */
public final class Alloc implements Comparable<Alloc>, Commentable, PioSerializable {

    private static final Logger LOGGER = Logger.getLogger(Alloc.class.getName());

    public enum Grouping {
        NONE,
        FIRST,
        MIDDLE,
        LAST,
        SOLO
    }

    public enum Circumstance {
        AZIMUTH,
        ELEVATION,
        AIRMASS,
        LUNAR_DISTANCE,
        PARALLACTIC_ANGLE,
        TOTAL_SKY_BRIGHTNESS,
        HOUR_ANGLE,
        TIMING_WINDOW_OPEN,
    }

    public enum SetupType {
        NONE,
        REACQUISITION,
        FULL
    }

    private static final long QUANTUM = 30 * TimeUtils.MS_PER_SECOND;
    private static final long serialVersionUID = 1L;

    // Members
    private final Interval interval; // The interval
    private final Obs obs;            // Associated Obs
    private final Variant variant;    // Owning Variant
    private final int firstStep;    // First step to execute
    private final int lastStep;    // Last step to execute
    private final SetupType setupType;    // Do we need to run setup?
    private final long shutterTime; // LCH-193: Extra time added to Alloc for laser shutter

    // Transients
    private Map<Circumstance, Double[]> circV, circS;

    // Package-protected constructor called by Variant#addAlloc()
    Alloc(final Variant variant, final Obs obs, final long low, final int firstStep, final int lastStep, final SetupType setup, final String comment) {
        this(variant, obs, low, firstStep, lastStep, setup, comment, span(obs, firstStep, lastStep, setup));
    }

    Alloc(final Variant variant, final Obs obs, final long low, final int firstStep, final int lastStep, final SetupType setup, final String comment, final long span) {
        this.interval = new Interval(low, addLchOverlap(obs, low, low + span));
        this.shutterTime = getEnd() - (low + span);
        this.variant = variant;
        this.obs = obs;
        this.firstStep = firstStep;
        this.lastStep = lastStep;
        this.setupType = setup;
        setComment(comment);
    }

    Alloc(final Variant variant, final Obs obs, final ParamSet params) {
        this.interval = new Interval(params);
        this.shutterTime = 0;
        this.variant = variant;
        this.obs = obs;
        this.firstStep = Pio.getIntValue(params, "firstStep", 0);
        this.lastStep = Pio.getIntValue(params, "lastStep", 0);
        this.setupType = Pio.getEnumValue(params, "setupType", SetupType.FULL);
        setComment(Pio.getValue(params, "comment"));
    }

    /**
     * Returns an Alloc for dragging (i.e., with a null obs and meaningless start time). The max length
     * hint specifies that the Alloc should be no longer than the specified size (however at least setup plus
     * the first step are always returned, so the hint may not be honored in all cases).
     */
    public static Alloc forDragging(Obs obs, long maxLengthHint) {

        // Minimum is setup plus first unexecuted step
        int first = obs.getFirstUnexecutedStep();
        int last = first;
        long dur = obs.getSteps().getSetupTime() + obs.getSteps().getStepTime(first);

        // While there are more steps and the next one doesn't go over our limit, add the step and repeat.
        while (last < obs.getSteps().size() - 1) {
            long next = obs.getSteps().getStepTime(last + 1);
            if (dur + next > maxLengthHint)
                break;
            dur += next;
            last++;
        }

        return new Alloc(null, obs, 0L, first, last, SetupType.FULL, null);
    }

    ///
    /// PIO
    ///

    @Override
    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = interval.getParamSet(factory, name);
        Pio.addIntParam(factory, params, "firstStep", firstStep);
        Pio.addIntParam(factory, params, "lastStep", lastStep);
        Pio.addParam(factory, params, "obs", getObs().getObsId());
        Pio.addParam(factory, params, "comment", getComment());
        Pio.addEnumParam(factory, params, "setupType", setupType);
        return params;
    }

    ///
    /// INTERVAL BEHAVIOR
    ///

    public Interval getInterval() { return interval; }
    public long getStart() { return interval.getStart(); }
    public long getMiddlePoint() { return interval.getMiddlePoint(); }
    public long getEnd() { return interval.getEnd(); }
    public long getLength() { return interval.getLength(); }
    public boolean abuts(Alloc a) { return a != null && interval.abuts(a.interval); }
    public boolean contains(long t) { return interval.contains(t); }
    public boolean overlaps(Alloc a, Interval.Overlap o) { return interval.overlaps(a.interval, o); }
    public boolean overlaps(Interval i, Interval.Overlap o) { return interval.overlaps(i, o); }

    ///
    /// COMMENTS
    ///

    public String getComment() {
        return variant == null ? null : variant.getComment(this);
    }

    public void setComment(String comment) {
        if (variant != null)
            variant.setComment(this, comment);
    }

    ///
    /// CIRCUMSTANCES
    ///

    private synchronized void initCircumstances() {
        if (circV == null) {

            // Calculate visit circumstances, which includes setup time.
            Function<Long, WorldCoords> coords = getObs()::getCoords;
            int size = Math.max(1, (int) (interval.getLength() / QUANTUM));
            circV = new TreeMap<Circumstance, Double[]>();
            for (Circumstance c: Circumstance.values()) {
                circV.put(c, new Double[size]);
            }
            TimingWindowSolver tws = new TimingWindowSolver(obs);
            ImprovedSkyCalc calc = new ImprovedSkyCalc(variant.getSchedule().getSite());
            for (int i = 0; i < size; i++) {
                long t = interval.getStart() + QUANTUM * i;
                calc.calculate(coords.apply(t), new Date(t), true);
                for (Circumstance c: Circumstance.values()) {
                    Double[] vals = circV.get(c);
                    switch (c) {
                    case AIRMASS:                 vals[i] = calc.getAirmass(); break;
                    case AZIMUTH:                 vals[i] = calc.getAzimuth(); break;
                    case ELEVATION:             vals[i] = calc.getAltitude(); break;
                    case LUNAR_DISTANCE:         vals[i] = calc.getLunarDistance(); break;
                    case PARALLACTIC_ANGLE:    vals[i] = calc.getParallacticAngle(); break;
                    case TOTAL_SKY_BRIGHTNESS:    vals[i] = calc.getTotalSkyBrightness(); break;
                    case HOUR_ANGLE:           vals[i] = calc.getHourAngle(); break;
                    case TIMING_WINDOW_OPEN:    vals[i] = tws.includes(t) ? 1. : 0.; break;
                    default: throw new Error("Missing switch case for " + c);
                    }
                }
            }

            // Now calculate circumstances for science.
            if (getSetupType() != SetupType.NONE) {

                circS = new TreeMap<Circumstance, Double[]>();
                long setupTime = getSetupTime();

                for (Circumstance c: Circumstance.values()) {

                    Double[] valsV = circV.get(c);
                    Double[] valsS = new Double[valsV.length];

                    // We want to copy the range n ... length where n is the first
                    // quantum that's not during setup time, or the last sample, so
                    // we guarantee at least one.
                    int n = Math.min((int) (setupTime / QUANTUM), valsV.length - 1);
                    System.arraycopy(valsV, n, valsS, n, valsV.length - n);

                    circS.put(c, valsS);

                }

            } else {

                // Science and visit circumstances are the same if there is no setup.
                circS = circV;

            }

        }
    }

    public Double getMin(Circumstance circ, boolean includeSetup) {
        initCircumstances();
        return getMin((includeSetup ? circV : circS).get(circ));
    }

    public Double getMax(Circumstance circ, boolean includeSetup) {
        initCircumstances();
        return getMax((includeSetup ? circV : circS).get(circ));
    }

    public Double getMean(Circumstance circ, boolean includeSetup) {
        initCircumstances();
        return getMean((includeSetup ? circV : circS).get(circ));
    }

    /**
     * Gets minimum for parallactic angle taking care of possible discontinuities in function.
     * @param includeSetup
     * @return
     */
    //REL-1441
    public Double getMinParallacticAngle(boolean includeSetup) {
        initCircumstances();
        return getMin(continuousPa((includeSetup ? circV : circS).get(Circumstance.PARALLACTIC_ANGLE)));
    }

    /**
     * Gets maximum for parallactic angle taking care of possible discontinuities in function.
     * @param includeSetup
     * @return
     */
    //REL-1441
    public Double getMaxParallacticAngle(boolean includeSetup) {
        initCircumstances();
        return getMax(continuousPa((includeSetup ? circV : circS).get(Circumstance.PARALLACTIC_ANGLE)));
    }

    /**
     * Gets mean for parallactic angle taking care of possible discontinuities in function.
     * @param includeSetup
     * @return
     */
    //REL-1441
    public Double getMeanParallacticAngle(boolean includeSetup) {
        initCircumstances();
        return getMean(continuousPa((includeSetup ? circV : circS).get(Circumstance.PARALLACTIC_ANGLE)));
    }

    /**
     * Gets the minimum of all values ignoring <code>null</code> values.
     * @param values
     * @return
     */
    private Double getMin(Double[] values) {
        Double ret = null;
        for (Double d: values) {
            if (d != null) ret = (ret == null) ? d : Math.min(d, ret);
        }
        return ret;
    }

    /**
     * Gets the maximum of all values ignoring <code>null</code> values.
     * @param values
     * @return
     */
    private Double getMax(Double[] values) {
        Double ret = null;
        for (Double d: values) {
            if (d != null) ret = (ret == null) ? d : Math.max(d, ret);
        }
        return ret;
    }

    /**
     * Gets the average of all values ignoring <code>null</code> values.
     * @param values
     * @return
     */
    private Double getMean(Double[] values) {
        double sum = 0;
        int count = 0;
        for (Double d: values) {
            if (d != null) {
                sum += d;
                ++count;
            }
        }
        return count == 0 ? null : (sum / count);
    }

    /**
     * Gets the values for the parallactic angle as a continuous function.
     * Note that the {@link ImprovedSkyCalc} calculates par. angles in the range from (-180,180] and that there can
     * be discontinuities where the values jump from -179.9.. to +179.9.. This will result in wrong values for the
     * min, max and mean values which can be avoided by correcting the values in a way to make them represent a
     * continuous function. This is done by adding a correction factor (+360 or -360) to the part of the function
     * after the discontinuity happens so that the jump in the values can be avoided. See also REL-1441.
     * @param values
     * @return
     */
    // NOTE: This is a bit of a hack, but I did not find a better way to deal with the discontinuity of the
    // function for parallactic angles.
    private Double[] continuousPa(Double[] values) {
        Double[] cor = new Double[values.length];
        // after detection of discontinuity this correction will be added to values
        // to make function/values continuous and allow correct calculation of min, max and mean
        double correction = 0;
        for (int i = 0; i < values.length; i++) {
            Double v = values[i];
            if (v != null) {
                if (i > 0 && values[i-1] != null) {
                    double last = values[i - 1];
                    // a jump by more than 357 degrees is considered to be the discontinuity
                    if (Math.abs(last - v) > 357.) {
                        // if the last value was > 0 (i.e. 179+) the function will continue at -179 and needs
                        // to be shifted upwards by 360 to become continuous (and vice versa for last value < 0)
                        correction = last > 0 ? +360.0 : -360.0;
                    }
                }
                v += correction;
            }
            cor[i] = v;
        }
        return cor;
    }

    ///
    /// MARKER MANAGER DELEGATE METHODS
    ///

    public SortedSet<Marker> getMarkers() {
        return variant.getSchedule().getMarkerManager().getMarkers(this);
    }

    public Severity getSeverity() {
        return getSeverity(true);
    }

    public Severity getSeverity(boolean qcOnly) {
        Severity sev = null;
        for (Marker m: getMarkers()) {
            if (m.isQcOnly() && !qcOnly) continue; // QC-only markers are not necessarily INFO level anymore (SCT-336)
            Severity ms = m.getSeverity();
            if (sev == null) {
                sev = m.getSeverity();
            } else {
                if (sev.ordinal() > ms.ordinal())
                    sev = ms;
            }
            if (sev == Severity.Error)
                break;
        }
        return sev;
    }


    ///
    /// TRIVIAL ACCESSORS
    ///

    public Obs getObs() { return obs; }

    public int getLastStep() {
        return lastStep;
    }

    public int getFirstStep() {
        return firstStep;
    }

    public Variant getVariant() {
        return variant;
    }

    public SetupType getSetupType() {
        return setupType;
    }

    ///
    /// VARIANT DELEGATE METHODS
    ///

    public Alloc getPredecessor() {
        return variant == null ? null : variant.getPredecessor(this);
    }

    public Alloc getPrevious() {
        return variant.getPrevious(this);
    }

    public Alloc getSuccessor() {
        return  variant == null ? null : variant.getSuccessor(this);
    }

    public Alloc getNext() {
        return variant.getNext(this);
    }

    public void remove() throws AbandonedSuccessorException {
        variant.removeAlloc(this, false);
    }

    /**
     * You can almost always avoid calling this method, so if you are using this
     * method it's probably because you're lazy.
     */
    @Deprecated
    public void forceRemove() {
        try {
            variant.removeAlloc(this, true);
        } catch (AbandonedSuccessorException e) {
            // This will never happen
        }
    }

    public Grouping getGrouping() {
        int g = getGroupIndex();
        if (g != -1) {
            Alloc prev = getPrevious();
            Alloc next = getNext();
            boolean gup = prev != null && g == prev.getGroupIndex();
            boolean gdn = next != null && g == next.getGroupIndex();
            if (gup && gdn) {
                return Grouping.MIDDLE;
            } else if (gup) {
                return Grouping.LAST;
            } else if (gdn) {
                return Grouping.FIRST;
            } else {
                return Grouping.SOLO;
            }
        } else {
            return Grouping.NONE;
        }

    }

    public int getGroupIndex() {
        return variant.getGroupIndex(this);
    }

    /**
     * Removes the current alloc and creates/returns a new one with the specified
     * start time.
     * @param newStart
     * @return
     */
    public Alloc move(long newStart) {
        return move(newStart, getSetupType());
    }

    public Alloc move(long newStart, SetupType setupType) {
        return variant.moveAlloc(this, newStart, setupType);
    }

    /**
     * Toggles between different setup types.
     * @return
     */
    public Alloc toggleSetupTime() {
        long fullSetup = obs.getSteps().getSetupTime();
        long racqSetup = obs.getSteps().getReacquisitionTime();
        long start;
        switch (getSetupType()) {
            case NONE:
                start = constrainStartTime(getStart() - fullSetup, getLength() + fullSetup);
                return variant.moveAlloc(this, start, SetupType.FULL);
            case FULL:
                // if there is a reacquisition time then use that and set type to "REACQUISITION"
                if (racqSetup > 0) {
                    start = constrainStartTime(getStart() + fullSetup - racqSetup, getLength() - fullSetup + racqSetup);
                    return variant.moveAlloc(this, start, SetupType.REACQUISITION);
                // if not, toggle directly to setupType "NONE"
                } else {
                    start = constrainStartTime(getStart() + fullSetup, getLength() - fullSetup);
                    return variant.moveAlloc(this, start, SetupType.NONE);
                }
            case REACQUISITION:
                start = getStart() + racqSetup;
                return variant.moveAlloc(this, start, SetupType.NONE);
            default:
                throw new IllegalArgumentException();
        }
    }

    public long getSetupTime() {
        switch (getSetupType()) {
            case NONE: return 0;
            case FULL: return getObs().getSteps().getSetupTime();
            case REACQUISITION: return getObs().getSteps().getReacquisitionTime();
            default: throw new IllegalArgumentException();
        }
    }

    public long constrainStartTime(long proposed, long length) {
        // If we have a success or predecessor, this will constrain the start time.
        Alloc pred = getPredecessor();
        Alloc succ = getSuccessor();
        if (pred != null) proposed = Math.max(pred.getEnd(), proposed);
        if (succ != null) proposed = Math.min(proposed, succ.getStart() - length);
        return proposed;
    }

    ///
    /// OBJECT OVERRIDES
    ///

    @Override
    public String toString() {
        // TODO: this, statically
        StringBuilder builder = new StringBuilder(getObs().toString());
        builder.append(" S");
        builder.append(firstStep + 1);
        if (lastStep > firstStep) {
            builder.append("-");
            builder.append(lastStep + 1);
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + interval.hashCode();
        hash = hash * 37 + obs.hashCode();
        hash = hash * 37 + variant.hashCode();
        hash = hash * 37 + firstStep;
        hash = hash * 37 + lastStep;
        hash = hash * 37 + setupType.hashCode();
        hash = hash * 37 + (int) shutterTime;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Alloc && super.equals(obj)) {
            Alloc a = (Alloc) obj;
            return
                a.interval.equals(interval) &&
                a.obs.equals(obs) &&
                a.variant.equals(variant) &&
                a.firstStep == firstStep &&
                a.lastStep == lastStep &&
                a.setupType.equals(setupType) &&
                a.shutterTime == shutterTime;
        }
        return false;
    }

    @Override
    public int compareTo(Alloc a) {
        int diff = interval.compareTo(a.interval);
        if (diff == 0) diff = obs.compareTo(a.obs);
        if (diff == 0) diff = variant.getName().compareTo(a.variant.getName());
        if (diff == 0) diff = firstStep - a.firstStep;
        if (diff == 0) diff = lastStep - a.lastStep;
        if (diff == 0) diff = setupType.compareTo(a.setupType);
        if (diff == 0) diff = (int) (shutterTime - a.shutterTime);
        return diff;
    }

    ///
    /// CTOR HELPER
    ///

    private static long span(Obs obs, int firstStep, int lastStep, SetupType setup) {
        assert lastStep >= firstStep;
        PlannedStepSummary steps = obs.getSteps();
        assert lastStep < steps.size();
        long accum;
        switch (setup) {
            case NONE: accum = 0; break;
            case FULL: accum = steps.getSetupTime(); break;
            case REACQUISITION: accum = steps.getReacquisitionTime(); break;
            default: throw new IllegalArgumentException();
        }
        for (int i = firstStep; i <= lastStep; i++) {
            if (!steps.isStepExecuted(i)) accum += steps.getStepTime(i);
        }
        return accum;
    }

    // If the observation when allocated at the given time overlaps with laser shutters, add
    // the amount of overlap to the end time and return it, otherwise return the original end time.
    private static long addLchOverlap(Obs obs, long start, long end) {
        return LttsServicesClient.getInstance().getShutterOverlap(obs, new Interval(start, end));
    }

    public String getSkyBrightnessBin(boolean includeSetup) {
        return Conds.getPercentileForSkyBrightness(getMin(Circumstance.TOTAL_SKY_BRIGHTNESS, includeSetup)) + "%";
    }

    public boolean isSuccessor(Alloc a) {
        return getObs() == a.getObs() && getLastStep() + 1 == a.getFirstStep();
    }

    public long getShutterTime() {
        return shutterTime;
    }
}
