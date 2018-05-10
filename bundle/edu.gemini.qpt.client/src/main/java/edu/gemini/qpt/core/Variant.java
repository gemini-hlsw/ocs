package edu.gemini.qpt.core;

import edu.gemini.qpt.core.Marker.Severity;
import edu.gemini.qpt.core.util.*;
import edu.gemini.qpt.core.util.Interval.Overlap;
import edu.gemini.qpt.core.util.Variants.AbandonedSuccessorException;
import edu.gemini.qpt.core.util.Variants.CollisionException;
import edu.gemini.qpt.core.util.Variants.MissingPredecessorException;
import edu.gemini.qpt.core.util.Variants.OrderingException;
import edu.gemini.qpt.shared.sp.*;
import edu.gemini.qpt.shared.util.PioSerializable;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.core.ProgramId;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.spModel.obs.SPObservation.Priority;
import edu.gemini.spModel.obs.plannedtime.PlannedStepSummary;
import edu.gemini.spModel.obscomp.SPGroup.GroupType;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.too.TooType;
import jsky.coords.WorldCoords;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;


/**
 * A schedule variant consists of a set of allocations and a set of constraints.
 * @author rnorris
 */
public final class Variant extends BaseMutableBean implements PioSerializable, Commentable {

	// Property Names
	public static final String PROP_ALLOCS = "visits";
	public static final String PROP_SITE_CONDITIONS = "siteConditions";
    public static final String PROP_WIND_CONSTRAINT = "windConstraint";
    public static final String PROP_LGS_CONSTRAINT = "lgsConstraint";
	public static final String PROP_NAME = "name";
	public static final String PROP_FLAGS = "flags";
	public static final String PROP_COMMENT = "comment";

	// A Logger
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Variant.class.getName());

	// Final Members
	private final AllocSet allocs;
	private final Schedule owner;
	private final Map<Obs, EnumSet<Flag>> obsFlags = new HashMap<>();
	private final Map<Alloc, String> allocComments = new HashMap<>();

	// Mutable Members
	private Conds conditions;
	private String name = "Untitled Variant";
	private String comment;
	private ApproximateAngle windConstraint;
    private Boolean lgsConstraint;

	// Flag caches
	private Map<Obs, EnumSet<Flag>> intrinsicFlagCache;
	private Map<Obs, EnumSet<Flag>> facilitiesFlagCache;
	private Map<Obs, EnumSet<Flag>> condsFlagCache;
	private Map<Obs, Union<Interval>> visibleUnionCache;
	private Map<Obs, Union<Interval>> darkUnionCache;
	private Map<Obs, Union<Interval>> constrainedUnionCache;
	private Map<Obs, Union<Interval>> timingUnionCache;

	// Cache names
	public static final String DARK_UNION_CACHE = "darkUnionCache";
	public static final String VISIBLE_UNION_CACHE = "visibleUnionCache";
	public static final String TIMING_UNION_CACHE = "timingUnionCache";

	// The list of all groups encountered in the plan, by first appearance.
	private List<Group> groups = new ArrayList<>();
	private boolean flagUpdatesEnabled = true;

	/**
	 * Each variant keeps a set of flags for each Observation.
	 */
	public enum Flag {
		INACTIVE,
		IQ_UQUAL,
		WV_UQUAL,
		CC_UQUAL,
		OVER_QUALIFIED,
		INSTRUMENT_UNAVAILABLE,
		CONFIG_UNAVAILABLE,
		MASK_IN_CABINET,
		MASK_UNAVAILABLE,
        LGS_UNAVAILABLE,
		BLOCKED,
		ELEVATION_CNS,
		SCHEDULED,
		IN_PROGRESS,
		BACKGROUND_CNS,
		MULTI_CNS,
		SETS_EARLY,
		PARTIALLY_BLOCKED,
		SETUP_BLOCKED,
		OVER_ALLOCATED,
		TIMING_CNS,
		SCHED_GROUP,
		TIME_CONSTRAINED // intrinsic
	}

	///
	/// CONSTRUCTORS AND PIO
	///

	Variant(Schedule owner, String name, Conds conds, ApproximateAngle windConstraint, Boolean lgsConstraint) {
		this.owner = owner;
		initCaches();
		this.name = name;
		this.conditions = conds;
		this.windConstraint = windConstraint;
        this.lgsConstraint = lgsConstraint;
		this.allocs = new AllocSet();
		updateObsFlags();
	}

	Variant(Schedule owner, ParamSet params) {
		this.owner = owner;
		initCaches();
		this.name = Pio.getValue(params, PROP_NAME);
		this.conditions = new Conds(params.getParamSet(PROP_SITE_CONDITIONS));

		ParamSet windParams = params.getParamSet(PROP_WIND_CONSTRAINT);
		this.windConstraint = windParams != null ? new ApproximateAngle(windParams) : null;

        this.lgsConstraint = Boolean.parseBoolean(Pio.getValue(params,PROP_LGS_CONSTRAINT));

		this.allocs = new AllocSet(this, params.getParamSet(PROP_ALLOCS));
		this.comment = Pio.getValue(params, PROP_COMMENT);
		updateBrokenAllocs(owner.getMiniModel()); // not new, so we have to repair
		updateObsFlags();
	}

	public ParamSet getParamSet(PioFactory factory, String name) {
		ParamSet params = factory.createParamSet(name);
		params.addParamSet(allocs.getParamSet(factory, PROP_ALLOCS));
		params.addParamSet(conditions.getParamSet(factory, PROP_SITE_CONDITIONS));
		if (windConstraint != null)
			params.addParamSet(windConstraint.getParamSet(factory, PROP_WIND_CONSTRAINT));
        Pio.addParam(factory, params, PROP_LGS_CONSTRAINT, this.lgsConstraint.toString());
        Pio.addParam(factory, params, PROP_NAME, this.name);
        Pio.addParam(factory, params, PROP_COMMENT, this.comment);
		return params;
	}

	void initCaches() {

		// These schedule-level caches are shared among all Variants.
		intrinsicFlagCache = owner.getCache("intrinsicFlagCache");
		facilitiesFlagCache = owner.getCache("facilitiesFlagCache", Schedule.PROP_FACILITIES, Schedule.PROP_ICTD);
		visibleUnionCache = owner.getCache("visibleUnionCache", Schedule.PROP_BLOCKS);
		darkUnionCache = owner.getCache("darkUnionCache", Schedule.PROP_BLOCKS);
		constrainedUnionCache = owner.getCache("freeUnionCache", Schedule.PROP_BLOCKS);
		timingUnionCache = owner.getCache("timingUnionCache");

		// Private variant-level caches.
		condsFlagCache = getCache("condsFlagCache", Variant.PROP_SITE_CONDITIONS);

	}

	///
	/// OVERRIDES
	///

	@Override
	public void setDirty(boolean dirty) {
		super.setDirty(dirty);
		if (dirty) owner.setDirty(dirty); // propogate up if dirty
		// TODO: propogate down if clean. Really not an issue since the top-level
		// dirty flag is the only one anyone looks at. Need to do this on Schedule
		// also.
	}

	///
	/// MINI-MODEL
	///

	void miniModelChanged(MiniModel miniModel) {
		invalidateAllCaches(); // [SCT-355] clear condsFlagCache, which is owned by the Variant (not by the Schedule)
		updateBrokenAllocs(miniModel);
		updateObsFlags();
	}

	void facilitiesChanged() {
		updateObsFlags();
	}

	///
	/// COMMENTS
	///

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
		setDirty(true);
	}

	String getComment(Alloc alloc) {
		return allocComments.get(alloc);
	}

	void setComment(Alloc alloc, String comment) {
		allocComments.put(alloc, comment);
		setDirty(true);
	}


	///
	/// ALLOCS
	///

	public Alloc addAlloc(Obs obs, Long start, int firstStep, int lastStep, Alloc.SetupType setupRequired, String comment) throws CollisionException, MissingPredecessorException, OrderingException {
		synchronized (allocs) {
			Alloc alloc = new Alloc(this, obs, start, firstStep, lastStep, setupRequired, comment);
			SortedSet<Alloc> prev = Collections.unmodifiableSortedSet(new TreeSet<Alloc>(getAllocs()));
			Variants.tryAdd(prev, alloc);
			allocs.add(alloc);
			firePropertyChange(PROP_ALLOCS, prev, getAllocs());
			setDirty(true);
			updateObsFlags();
			return alloc;
		}
	}

	public SortedSet<Alloc> getAllocs() {
		synchronized (allocs) {
			return new TreeSet<Alloc>(allocs);
		}
	}

    public SortedSet<Interval> getAllocIntervals() {
        synchronized (allocs) {
            SortedSet<Interval> ts = new TreeSet<>();
            for (Alloc a : allocs) ts.add(a.getInterval());
            return ts;
        }
    }

    // Package-protected, called by Alloc::remove()
	void removeAlloc(Alloc alloc, boolean force) throws AbandonedSuccessorException {
		SortedSet<Alloc> prev = Collections.unmodifiableSortedSet(new TreeSet<Alloc>(getAllocs()));
		if (!force) Variants.tryRemove(prev, alloc);
		synchronized (allocs) {
			allocs.remove(alloc);
		}
		firePropertyChange(PROP_ALLOCS, prev, getAllocs());
		updateObsFlags();
		setDirty(true);
	}

	// Faster than remove + add
	// Package-protected, called by Alloc::move()
	Alloc moveAlloc(Alloc a, long newStart, Alloc.SetupType setupType) {
		synchronized (allocs) {
			Alloc alloc = new Alloc(this, a.getObs(), newStart, a.getFirstStep(), a.getLastStep(), setupType, a.getComment());
			SortedSet<Alloc> prev = Collections.unmodifiableSortedSet(new TreeSet<Alloc>(getAllocs()));
			allocs.remove(a);
			allocs.add(alloc);
			firePropertyChange(PROP_ALLOCS, prev, getAllocs());
			setDirty(true);
			updateObsFlags();
			return alloc;
		}
	}

	Alloc moveAlloc(Alloc a, long newStart) {
		return moveAlloc(a, newStart, a.getSetupType());
	}

	/**
	 * Return any allocations that are overlapped by a block. For instance
	 * getAllocs(b, Overlap.TOTAL) returns the set of allocs that are
	 * correctly scheduled within this block.
	 */
	public SortedSet<Alloc> getAllocs(Block b, Overlap olap) {
		SortedSet<Alloc> ret = new TreeSet<Alloc>();
		synchronized (allocs) {
			for (Alloc a: allocs) {
				if (b.overlaps(a.getInterval(), olap))
					ret.add(a);
			}
		}
		return ret;
	}

	///
	/// PREDECESSOR / SUCCESSOR
	///

	Alloc getPredecessor(Alloc a) {
		synchronized (allocs) {
			for (Alloc p: allocs) {
				if (a.getObs() == p.getObs() && a.getFirstStep() == p.getLastStep() + 1)
					return p;
			}
		}
		return null;
	}

	Alloc getPrevious(Alloc a) {
		synchronized (allocs) {
			Alloc prev = null;
			for (Alloc p: allocs) {
				if (p == a) return prev;
				prev = p;
			}
		}
		return null;
	}

	Alloc getNext(Alloc a) {
		synchronized (allocs) {
			Alloc prev = null;
			for (Alloc p: allocs) {
				if (prev == a) return p;
				prev = p;
			}
		}
		return null;
	}

	Alloc getSuccessor(Alloc a) {
		synchronized (allocs) {
			for (Alloc p: allocs) {
				if (a.getObs() == p.getObs() && a.getLastStep() + 1 == p.getFirstStep())
					return p;
			}
		}
		return null;
	}

	/**
	 * Returns the index of the passed alloc's group (just a unique int per group,
	 * with no special meaning) or -1 if the passed alloc is ungrouped OR
	 * is the only alloc in its group to appear in the plan. That is, the index will
	 * only be non-negative if the alloc has a friend in the plan that comes from
	 * the same group.
	 */
	int getGroupIndex(Alloc a) {
		Group g = a.getObs().getGroup();
		return g != null ? groups.indexOf(g) : -1;
	}

	///
	/// TRIVIAL PROPERTIES
	///

	public void setName(String name) {
		String prev = this.name;
		this.name = name;
		firePropertyChange(PROP_NAME, prev, name);
		setDirty(true);
	}

	public String getName() {
		return name;
	}

	public Schedule getSchedule() {
		return owner;
	}

	@Override
	public String toString() {
		return getName();
	}

	public Conds getConditions() {
		return conditions;
	}

	public void setConditions(Conds conditions) {
		Conds prev = this.conditions;
		this.conditions = conditions;
		firePropertyChange(PROP_SITE_CONDITIONS, prev, conditions);
		updateObsFlags(); // must do tis after prop change
		setDirty(true);
	}

	public ApproximateAngle getWindConstraint() {
		return windConstraint;
	}

	public void setWindConstraint(ApproximateAngle windConstraint) {
		ApproximateAngle prev = this.windConstraint;
		this.windConstraint = windConstraint;
		firePropertyChange(PROP_WIND_CONSTRAINT, prev, windConstraint);
		updateObsFlags(); // must do tis after prop change
		setDirty(true);
	}

    public Boolean getLgsConstraint() {
        return lgsConstraint;
    }

    public void setLgsConstraint(Boolean lgsConstraint) {
        Boolean prev = this.lgsConstraint;
        this.lgsConstraint = lgsConstraint;
        firePropertyChange(PROP_LGS_CONSTRAINT, prev, lgsConstraint);
        invalidateAllCaches(); //without this you need to update the variant twice for the candidate observations list to be updated, no idea why...
        updateObsFlags(); // must do this after prop change
        setDirty(true);
    }


	///
	/// DERIVED PROPERTIES
	///

	public boolean isEmpty() {
		return allocs.isEmpty();
	}

	public long getStart() {
		if (isEmpty()) throw new IllegalStateException("Variant is empty.");
		return allocs.first().getStart();
	}

	public long getEnd() {
		if (isEmpty()) throw new IllegalStateException("Variant is empty.");
		return allocs.last().getEnd();
	}

	public long getSpan() {
		return getEnd() - getStart();
	}

	public SortedSet<Marker> getMarkers(boolean transitive) {
		return owner.getMarkerManager().getMarkers(this, transitive);
	}

	/**
	 * Examines this Variant's Markers and returns the severity of the "worst" marker.
	 * @return a Severity, or null if there are no markers
	 */
	public Severity getSeverity() {
		Severity sev = null;
		for (Marker m: getMarkers(true)) {
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
	/// CANDIDATE OBSERVATIONS
	///

	private void updateBrokenAllocs(MiniModel newModel) {

		// Next, set up collections for old and new allocs.
		Set<Alloc> prev = getAllocs();
		Set<Alloc> next = new TreeSet<Alloc>();

		// Ok, what we're going to do is copy each alloc from prev to next,
		// modifying those that need repairs and deleting those that
		// don't make sense anymore because the steps have been executed or
		// the obs has been deleted. We're working with a completely new set
		// of stuff here, so the UI can continue to draw and everything should be ok
		for (Alloc a: prev) {

			// Get the old obs. If it's not there, this means that we're in the process
			// of deserializing a Variant with a new MiniModel and the obs was deleted.
			// Luckily this isn't a problem; we just skip it.
			final Obs oldObs = a.getObs();
			if (oldObs == null)
				continue;

			final Obs newObs = newModel.getObs(oldObs.getObsId());

			// If the new mini-model doesn't contain the obs, delete the alloc.
			if (newObs == null)
				continue;

			// Now look at the new execution state.
			final PlannedStepSummary newSteps = newObs.getSteps();
			final int firstUnexecuted = newObs.getFirstUnexecutedStep();

			// Delete the alloc if all steps have been executed.
			if (firstUnexecuted > a.getLastStep())
				continue;

			// Calculate the new first step. We need to push it forward if it's less
			// than the new first step. There is also the odd case where the old
			// alloc started on the first step, and in all cases we want to keep it
			// on the first step. This handles the odd case where the beginning of
			// the sequence has become un-executed. This can happen in odd cases due
			// to bugs elsewhere in OCS, so we want to handle it.
			final int newFirstStep;
			final int oldFirstStep = a.getFirstStep();
			if (oldFirstStep < firstUnexecuted || a.getFirstStep() == a.getObs().getFirstUnexecutedStep()) {
				newFirstStep = firstUnexecuted;
			} else {
				newFirstStep = oldFirstStep;
			}

			// New last step is the min of current last step and step count. So if
			// steps were deleted, we need to remove them from the visit.
			final int newLastStep = Math.min(a.getLastStep(), newSteps.size() - 1);

			// In the normal case we want to push the lower bound up
			// as steps get executed, rather than shifting the remainder to the left.
			// We'll try to do this, using the execution times from the old alloc.
			// This should be correct in the most common case.
			final long oldStartTime = a.getStart();
			final long newStartTime;
			if (newFirstStep > oldFirstStep) {

				PlannedStepSummary steps = a.getObs().getSteps();
				long accum = 0;
				for (int i = oldFirstStep; i < newFirstStep; i++)
					accum += steps.getStepTime(i);
				newStartTime = oldStartTime + accum;

			} else if (newFirstStep < oldFirstStep) {

				PlannedStepSummary steps = a.getObs().getSteps();
				long accum = 0;
				for (int i = newFirstStep; i < oldFirstStep; i++)
					accum += steps.getStepTime(i);
				newStartTime = oldStartTime - accum;

			} else {

				// No change.
				newStartTime = oldStartTime;

			}

			// Now create the new alloc or copy the old one over if there were no changes.
			// We always have to create a new one (even if the steps don't change) because
			// the existing steps may have changed in duration. The constructor will re-
			// calculate the correct execution time for us.
			a = new Alloc(this, newObs, newStartTime, newFirstStep, newLastStep, a.getSetupType(), a.getComment());
			next.add(a);


		}

		// Done. Now we can tell everyone what happened.
		synchronized (allocs) {
			allocs.clear(); allocs.addAll(next);
		}

		firePropertyChange(PROP_ALLOCS, prev, next);

	}


	/**
	 * Checks that the observation requires a custom mask and that the
	 * corresponding facility checkbox is checked.
	 */
	@SuppressWarnings("unchecked")
	private boolean shouldCheckMaskAvailability(final Obs obs) {
		boolean check = false;
		for (Enum<?> e : obs.getOptions()) {
			check = check || (Inst.isCustomMask(e) && owner.hasFacility(e));
		}
		return check;
	}

	@SuppressWarnings("unchecked")
	private void updateObsFlags() {

		if (!flagUpdatesEnabled) return;

		// Nothing to do if there are no blocks. This only happens with
		// new schedules.
		if (owner.isEmpty()) return;

		// Before we get started with flags, let's accumulate a list of all groups
		// referenced in the plan. We will use this later to color-code the group
		// associations for allocs such that all allocs in the same group get the
		// same color. Groups are considered significant only if they appear more
		// than once.
		Set<Group> seenOnce = new HashSet<Group>();
		Set<Group> seenMore = new HashSet<Group>();
		for (Alloc a: getAllocs()) {
			Group g = a.getObs().getGroup();
			if (g != null && g.getType() == GroupType.TYPE_SCHEDULING && !seenOnce.add(g))
				seenMore.add(g);
		}
		groups.clear();
		groups.addAll(seenMore);

		// Clear our last flag map.
		obsFlags.clear();

		// We need the site for a few things
		Site site = owner.getSite();

		// It's easier to create the SCHEDULED flag by iterating the
		// allocs, so we'll do that before iterating the model.
		for (Alloc a: getAllocs()) {
			getMutableFlags(a.getObs()).add(Flag.SCHEDULED);
		}

		// Now look at each Obs in the model. For each we want to associate
		// meaningful flags that we can use to annotate the Obs in the UI.
		// This is a very expensive process and we're doing aggressive caching
		// in order to keep up with it. The caches are created during variant
		// construction and are invalidated automatically based on property
		// changes. Some caches are shared among all variants.
		for (Obs obs: owner.getMiniModel().getAllObservations()) {

			EnumSet<Flag> flags = getMutableFlags(obs);

			// INACTIVE, IN_PROGRESS, OVER_ALLOCATED, TIME_CONSTRAINED
			// These flags are intrinsic properties of the Obs.
			EnumSet<Flag> intrinsicFlags = intrinsicFlagCache.get(obs);
			if (intrinsicFlags == null) {
				intrinsicFlags = EnumSet.noneOf(Flag.class);

				// Inactive programs
				if (!obs.getProg().isActive())
					intrinsicFlags.add(Flag.INACTIVE);

				// In-progress observations
				if (obs.isInProgress())
					intrinsicFlags.add(Flag.IN_PROGRESS);

				// Over-allocated observations.
				// HACK: don't check for ENG and CAL
				if (!obs.getProg().isEngOrCal() && obs.getProg().getRemainingProgramTime() <= 0)
					intrinsicFlags.add(Flag.OVER_ALLOCATED);

				// Observations in scheduling groups
				Group g = obs.getGroup();
				if (g != null && g.getType() == GroupType.TYPE_SCHEDULING)
					intrinsicFlags.add(Flag.SCHED_GROUP);

				// Observations with time constraints
				if (obs.getTooPriority() != TooType.none || obs.getTimingWindows().size() > 0)
					intrinsicFlags.add(Flag.TIME_CONSTRAINED);

				intrinsicFlagCache.put(obs, intrinsicFlags);
			}
			flags.addAll(intrinsicFlags);


			// INSTRUMENT_UNAVAILABLE, CONFIG_UNAVAILABLE
			// These flags depend on the current set of facilities.
			EnumSet<Flag> facilitiesFlags = facilitiesFlagCache.get(obs);
			if (facilitiesFlags == null) {
				facilitiesFlags = EnumSet.noneOf(Flag.class);

				// Insufficient facilities (instrument)
				for (Inst inst: obs.getInstruments()) {
					if (!owner.hasFacility(inst)) {
						facilitiesFlags.add(Flag.INSTRUMENT_UNAVAILABLE);
						break;
					}
				}

				// Insufficient facilities (option - grating, filter, etc)
				for (Enum<?> option: obs.getOptions()) {
					if (!owner.hasFacility(option)) {
						facilitiesFlags.add(Flag.CONFIG_UNAVAILABLE);
						break;
					}
				}

				// If the custom mask flag is set and the observation has a
				// custom mask, check its availability.
				final EnumSet<Flag> maskFlags = EnumSet.noneOf(Flag.class);
				ImOption.apply(obs.getCustomMask()).filter(m -> !m.trim().isEmpty() && shouldCheckMaskAvailability(obs)).foreach(m -> {
					final ProgramId pid = obs.getProg().getStructuredProgramId();
					if (pid instanceof ProgramId.Science) {
						final Availability a = owner.maskAvailability((ProgramId.Science) pid, m);
						if (a == Availability.SummitCabinet) {
							maskFlags.add(Flag.MASK_IN_CABINET);
						} else if (a != Availability.Installed) {
							maskFlags.add(Flag.MASK_UNAVAILABLE);
						}
					}
				});
				facilitiesFlags.addAll(maskFlags);

				facilitiesFlagCache.put(obs, facilitiesFlags);
			}
			flags.addAll(facilitiesFlags);


			// UNDER_QUALIFIED, OVER_QUALIFIED
			// These flags depend on this variant's conditions set.
			EnumSet<Flag> condsFlags = condsFlagCache.get(obs);
			if (condsFlags == null) {
				condsFlags = EnumSet.noneOf(Flag.class);

				// Underqualified Conditions
				if (!obs.getConditions().meetsCCConstraint(conditions))
					condsFlags.add(Flag.CC_UQUAL);
				if (!obs.getConditions().meetsWVConstraint(conditions))
					condsFlags.add(Flag.WV_UQUAL);
				if (!obs.getConditions().meetsIQConstraint(conditions))
					condsFlags.add(Flag.IQ_UQUAL);

				// Overqualified Observations
				if (obs.getConditions().meetsConstraintsEasily(conditions)) {
					condsFlags.add(Flag.OVER_QUALIFIED);
				}

                // LGS osbervations on non-LGS variant
                if(!lgsConstraint && obs.getLGS()){
                    condsFlags.add(Flag.LGS_UNAVAILABLE);
                }

				condsFlagCache.put(obs, condsFlags);
			}
			flags.addAll(condsFlags);


			// We dont want to do the blocking calculation if the obs can't be
			// scheduled even if the variant is totally empty. So keep track
			// of whether we have come up with a reason or not.
			boolean theoreticallySchedulable = !flags.contains(Flag.SCHEDULED);


			// BACKGROUND_CNS
			// This flag depends on the schedule's blocks.
			// Find the dark time for the target. If the union is empty, the observation
			// can not be scheduled at all tonight.
			Union<Interval> darkUnion = darkUnionCache.get(obs);
			if (darkUnion == null) {
				Function<Long, WorldCoords> coords = obs::getCoords;
				final double brightest = obs.getConditions().getBrightestMagnitude();
				SkyBackgroundSolver sbs = new SkyBackgroundSolver(site, coords, brightest);
				darkUnion = sbs.solve(owner.getStart(), owner.getEnd());
				darkUnionCache.put(obs, darkUnion);
			}
			if (darkUnion.isEmpty()) {
				flags.add(Flag.BACKGROUND_CNS);
				theoreticallySchedulable = false;
			}


			// ELEVATION_CNS, SETS_EARLY
			// This flag depends on the schedule's blocks.
			// Find the time that the target meets its elevation constraints. For now
			// this just means <= airmass 2.
			Union<Interval> visibleUnion = visibleUnionCache.get(obs);
			if (visibleUnion == null) {
				ElevationConstraintSolver as = ElevationConstraintSolver.forObs(site, obs);
				visibleUnion = as.solve(owner.getStart(), owner.getEnd());
				visibleUnionCache.put(obs, visibleUnion);
			}
			if (visibleUnion.isEmpty()) {
				flags.add(Flag.ELEVATION_CNS);
				theoreticallySchedulable = false;
			} else {
				long set = visibleUnion.getIntervals().last().getEnd();
				if (set - owner.getStart() < 3 * TimeUtils.MS_PER_HOUR)
					flags.add(Flag.SETS_EARLY);
			}

			// Timing windows
			Union<Interval> timingUnion = timingUnionCache.get(obs);
			if (timingUnion == null) {
				TimingWindowSolver as = new TimingWindowSolver(obs);
				timingUnion = as.solve(owner.getStart(), owner.getEnd());
				timingUnionCache.put(obs, timingUnion);
			}
			if (timingUnion.isEmpty()) {
				flags.add(Flag.TIMING_CNS);
				theoreticallySchedulable = false;
			}

			// UNSCHEDULABLE, BLOCKED
			// If the Obs is still theoretically schedulable, we now need to
			// intersect all the availability unions and see if there's enough
			// room to schedule at least one step.
			if (theoreticallySchedulable) {

				// What's the minimum amount of time we need?
				PlannedStepSummary steps = obs.getSteps();
				final long firstUnexStep;
				try {
					firstUnexStep = steps.getStepTime(obs.getFirstUnexecutedStep());
				} catch (ArrayIndexOutOfBoundsException aioobe) {
					LOGGER.severe(obs + ": TOTAL STEPS == " + steps.size() + ", FIRST UNEX IS " + obs.getFirstUnexecutedStep());
					return;
				}
				final long setupTime = steps.getSetupTime();
				final long firstUnexStepPlusSetup = firstUnexStep + setupTime;


				// First find the intersection of all constraint-based unions
				// without regard to the state of the variant itself. This will tell
				// us whether the Obs is in fact schedulable in theory.
				Union<Interval> constrainedUnion = constrainedUnionCache.get(obs);
				if (constrainedUnion == null) {
					constrainedUnion = new Union<>(owner.getBlockIntervals());
					constrainedUnion.intersect(visibleUnion);
					constrainedUnion.intersect(darkUnion);
					constrainedUnion.intersect(timingUnion);
					constrainedUnionCache.put(obs, new Union<Interval>(constrainedUnion)); // put a copy, since we change it after retrieving

					// Now, also extend each interval back in time for a length
					// equal to the setup time. This is the actual constrained
					// union, since setup time is unconstrained.
					for (Interval i: new ArrayList<Interval>(constrainedUnion.getIntervals())) {
						constrainedUnion.add(new Interval(i.getStart() - setupTime, i.getStart()));
					}

				} else {
					constrainedUnion = new Union<Interval>(constrainedUnion); // copy
				}

				// Look for a slot large enough to run the first unexecuted step
				// plus setup.
				boolean existsSlotForFirstUnexStepPlusSetup = false;
				for (Interval i: constrainedUnion) {
					if (i.getLength() >= firstUnexStepPlusSetup) {
						existsSlotForFirstUnexStepPlusSetup = true;
						break;
					}
				}

				if (!existsSlotForFirstUnexStepPlusSetup) {

					// Once we unioned all the constraints together, there was
					// no open space. So although there is no one constraint that
					// prevents scheduling, there is nowhere on the schedule where
					// we won't violate at least one hard constraint. So we flag
					// the obs as unschedulable.
					flags.add(Flag.MULTI_CNS);

				} else {

					// Ok, clone this again in case we need it down below.
					Union<Interval> constrainedUnionWithoutSetup = new Union<Interval>(constrainedUnion);

					// There is at least one slot in the schedule where we can
					// place at least one step without violating any hard constraints.
					// However all such slots may be covered by existing allocs at
					// this point, so we need to subtract them out and see if
					// there is enough space remaining.
					constrainedUnion.remove(getAllocIntervals());

					// Find largest interval
					long maxInterval = -1;
					for (Interval i: constrainedUnion)
						maxInterval = Math.max(maxInterval, i.getLength());

					if (maxInterval < firstUnexStep) {

						// Nope, all the good spots on the schedule are already taken.
						// The obs can still be scheduled, but only if we move something.
						flags.add(Flag.BLOCKED);

					} else {

						if (maxInterval < firstUnexStepPlusSetup) {

							// At least enough time for step 1, but not enough time
							// for setup. We need to recalculate the union in this
							// corner case because non-setup time is more constrained.

							// Set up constrainedUnionWithoutSetup
							for (Interval i: new ArrayList<Interval>(constrainedUnionWithoutSetup.getIntervals()))
								constrainedUnionWithoutSetup.remove(new Interval(i.getStart(), i.getStart() + setupTime));
							constrainedUnionWithoutSetup.remove(getAllocIntervals());

							// And see if there is time for step 1
							for (Interval i: constrainedUnionWithoutSetup) {
								if (i.getLength() >= firstUnexStep) {
									flags.add(Flag.SETUP_BLOCKED);
									break;
								}
							}

							if (!flags.contains(Flag.SETUP_BLOCKED))
								flags.add(Flag.BLOCKED);

						} else {

							// At least enough time for step 1 plus setup.

							// Count up all the steps
							long totalSteps = steps.getSetupTime();
							for (int i = 0; i < steps.size(); i++)
								totalSteps += steps.getStepTime(i);

							// If the largest interval isn't big enough for the whole
							// obs sequence, we're partially blocked
							if (totalSteps > maxInterval)
								flags.add(Flag.PARTIALLY_BLOCKED);

						}

					}

				}

			}

		}

		firePropertyChange(PROP_FLAGS, null, Collections.unmodifiableMap(obsFlags));

	}

	static final SimpleDateFormat df = new SimpleDateFormat("HH:mm");

	@SuppressWarnings("unused")
	private StringBuilder print(Union<? extends Interval> u) {
		if (u == null) return new StringBuilder("null");
		StringBuilder sb = new StringBuilder("" + System.identityHashCode(u));
		sb.append(": ");
		for (Interval i: u) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(df.format(new Date(i.getStart())));
			sb.append("-");
			sb.append(df.format(new Date(i.getEnd())));
		}
		return sb;
	}

	private EnumSet<Flag> getMutableFlags(Obs obs) {
		EnumSet<Flag> ret = obsFlags.get(obs);
		if (ret == null) {
			ret = EnumSet.noneOf(Flag.class);
			obsFlags.put(obs, ret);
		}
		return ret;
	}

	public Set<Flag> getFlags(Obs obs) {
		EnumSet<Flag> ret = obsFlags.get(obs);
		if (ret == null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(ret);
	}

	private static final EnumSet<Flag> AUTOMATIC_DEATH_FLAGS = EnumSet.of(
			Flag.CONFIG_UNAVAILABLE,
			Flag.MASK_IN_CABINET,
			Flag.MASK_UNAVAILABLE,
            Flag.LGS_UNAVAILABLE,
			Flag.INSTRUMENT_UNAVAILABLE,
			Flag.ELEVATION_CNS,
			Flag.BACKGROUND_CNS,
			Flag.TIMING_CNS,
			Flag.CC_UQUAL,
			Flag.WV_UQUAL,
			Flag.IQ_UQUAL,
			Flag.MULTI_CNS
		);

	// This is used to initialize the editor.
	public static final int DEFAULT_WIND_CONSTRAINT_VARIANCE = 20;

	public double getScore(Obs obs) {

		Set<Flag> flags = getFlags(obs);

		// All kinds of flags automatically give you a zero.
		if (containsAny(flags, AUTOMATIC_DEATH_FLAGS))
			return 0;

		// Initial score is 1 / science band ^ 2
		double score = 1.0 / obs.getProg().getBand();
		score *= score;

		// Decrease if no timing windows
		if (obs.getTimingWindows().isEmpty())
			score *= 0.75;

		// Decrease if doesn't set early
		if (!flags.contains(Flag.SETS_EARLY))
			score *= 0.75;

		// Decrease if over-qualified
		if (flags.contains(Flag.OVER_QUALIFIED))
			score *= 0.8;

		// Decrease lot if not started
		if (!flags.contains(Flag.IN_PROGRESS))
			score *= 0.5;

		// Decrease if can only be partially scheduled
		if (flags.contains(Flag.PARTIALLY_BLOCKED))
			score *= 0.75;

		// QPT-211: Make observations requiring dark time more important
		// than those which don't require dark time,
		score *= 1.0 - obs.getConditions().getSB() / 200.0; // 0 => 1.0, 100 => 0.5

		// QPT-202: Determine the effective PI priority, which may be higher than
		// the base priority in the model. Shift everything up if there are unused
		// non-TOO priorities in the same program (ignoring unobservable obs).
		boolean[] found = new boolean[Priority.values().length];
		for (Obs o: obs.getProg().getFullObsSet())
			if (o != obs && !containsAny(getFlags(o), AUTOMATIC_DEATH_FLAGS))
				found[o.getPriority().ordinal()] = true;
		Priority basePriority = obs.getPriority();
		int effectivePriorityOrdinal = basePriority.ordinal();
		for (int i = effectivePriorityOrdinal + 1; i < found.length - 1; i++) // length - 1 to skip TOO
			if (!found[i]) ++effectivePriorityOrdinal;
		Priority effectivePriority = Priority.values()[effectivePriorityOrdinal];
//		if (basePriority != effectivePriority)
//			LOGGER.info(obs + ": " + basePriority + " => " + effectivePriority);

		// Scale based on effective priority
                if (obs.getTooPriority() != TooType.none) {
		    switch (effectivePriority) {
		    case HIGH: score *= 0.5; break;
		    case MEDIUM: score *= 0.4; break;
		    case LOW: score *= 0.3; break;
		    }
                }

		return score;


	}

	@SuppressWarnings("unchecked")
	private boolean containsAny(Collection<?> a, Collection<?> b) {
		for (Object o: a)
			if (b.contains(o))
				return true;
		return false;
	}

	public SortedSet<Alloc> getAllocs(Obs o) {
		SortedSet<Alloc> ret = getAllocs(); // this is a new collection
		for (Iterator<Alloc> it = ret.iterator(); it.hasNext(); )
			if (!o.equals(it.next().getObs())) // TODO: why doesn't == work?
				it.remove();
		return ret;
	}

	public Alloc addAlloc(Alloc a) throws CollisionException, MissingPredecessorException, OrderingException {
		return addAlloc(a.getObs(), a.getStart(), a.getFirstStep(), a.getLastStep(), a.getSetupType(), a.getComment());
	}

	public void setFlagUpdatesEnabled(boolean b) {
		flagUpdatesEnabled  = b;
		if (flagUpdatesEnabled) updateObsFlags();
	}


}


@SuppressWarnings("serial")
final class AllocSet extends TreeSet<Alloc> implements PioSerializable {

    private static final Logger LOGGER = Logger.getLogger(AllocSet.class.getName());

    public static final String PROP_MEMBER = "visit";

    public AllocSet() {}

	public AllocSet(final Variant variant, final ParamSet paramSet) {
		for (ParamSet allocParams: paramSet.getParamSets(PROP_MEMBER)) {
            final String obsId = Pio.getValue(allocParams, "obs");
            final Obs obs = variant.getSchedule().getMiniModel().getObs(obsId);
            if (obs != null) {
                add(new Alloc(variant, obs, allocParams));
            } else {
                // Silently ignore observations that are not found in the mini model.
                // This can for example happen when an observation is changed from Active to Inactive etc.
                LOGGER.fine("No observation found in mini model with id " + obsId + "; ignoring this observation");
            }
        }
	}

	public ParamSet getParamSet(final PioFactory factory, final String name) {
		final ParamSet params = factory.createParamSet(name);
		for (Alloc a: this) {
			params.addParamSet(a.getParamSet(factory, PROP_MEMBER));
		}
		return params;
	}

}




