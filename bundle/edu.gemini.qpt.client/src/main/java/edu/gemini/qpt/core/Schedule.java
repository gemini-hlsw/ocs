package edu.gemini.qpt.core;

import edu.gemini.spModel.ictd.CustomMaskKey;
import edu.gemini.spModel.ictd.IctdSummary;
import edu.gemini.qpt.core.listeners.Listeners;
import edu.gemini.qpt.core.util.*;
import edu.gemini.qpt.core.util.Variants.EditException;
import edu.gemini.qpt.shared.sp.Conds;
import edu.gemini.qpt.shared.sp.Inst;
import edu.gemini.qpt.shared.sp.MiniModel;
import edu.gemini.qpt.shared.util.EnumPio;
import edu.gemini.qpt.shared.util.Ictd;
import edu.gemini.qpt.shared.util.PioSerializable;
import edu.gemini.qpt.shared.util.TimeUtils;
import edu.gemini.skycalc.TwilightBoundedNight;
import static edu.gemini.skycalc.TwilightBoundType.CIVIL;
import static edu.gemini.skycalc.TwilightBoundType.NAUTICAL;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.PreImagingType;
import edu.gemini.spModel.ictd.Availability;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.nici.NICIParams;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.texes.TexesParams;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import jsky.coords.WorldCoordinates;
import jsky.util.DateUtil;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.qpt.shared.util.TimeUtils.MS_PER_DAY;

/**
 * Top-level model object representing a queue plan.
 * @author rnorris
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class Schedule extends BaseMutableBean implements PioSerializable, Commentable {

    private static final Logger LOGGER = Logger.getLogger(Schedule.class.getName());

    // Property Constants
    public static final String PROP_SITE = "site";
    public static final String PROP_VARIANTS = "variants";
    public static final String PROP_CURRENT_VARIANT = "currentVariant";
    public static final String PROP_BLOCKS = "blocks";
    public static final String PROP_DIRTY = "dirty";
    public static final String PROP_FILE = "file";
    public static final String PROP_FACILITIES = "facilities";
    public static final String PROP_EXTRA_SEMESTERS = "extraSemesters";
    public static final String PROP_MINI_MODEL = "miniModel";
    public static final String PROP_ICTD = "ictd";

    // Persistent Members
    private final BlockUnion blocks;
    private final VariantList variants;
    private String comment;
    private StringSet extraSemesters;

    // Transient Members
    private Variant currentVariant;
    private MixedEnumSet facilities = new MixedEnumSet();
    private MarkerManager markerManager = new MarkerManager();
    private File file;
    private Map<WorldCoordinates, Union<Interval>> intervalCache = new HashMap<>();
    private MiniModel miniModel;
    private Option<IctdSummary> ictdSummary;

    /**
     * Constructs an empty Schedule.
     * @param model
     */
    public Schedule(MiniModel model, Option<IctdSummary> ictdSummary) {
        assert model != null;
        assert ictdSummary != null;
        this.miniModel = model;
        this.ictdSummary = ictdSummary;
        this.blocks = new BlockUnion();
        this.variants = new VariantList();
        this.extraSemesters = new StringSet();
        init(true, ictdSummary.map(i -> i.featureAvailabilityJava()));
    }

    /**
     * Constructs a Schedule from the specified ParamSet.
     * @param model
     * @param params
     */
    public Schedule(MiniModel model, ParamSet params, int upgradingFrom) {
        this.miniModel = model;

        // Initialize the Facilities collection. After deserializing, we want to
        // add any new facilities that didn't exist in the previous version if
        // we're upgrading.
        this.facilities = new MixedEnumSet(params.getParamSet(PROP_FACILITIES));
        switch (upgradingFrom) {

            // MIGRATE PRE-104 to 104
            case ScheduleIO.VERSION_PRE_104:

                LOGGER.info("Upgrading serial version " + ScheduleIO.VERSION_PRE_104 + " to " + ScheduleIO.VERSION_104);
                switch (model.getSite()) {
                    case GS:
                        facilities.addAll(Arrays.asList(FilterSouth.values()));           // GMOS South Filters
                        facilities.addAll(Arrays.asList(GNIRSParams.Disperser.values())); // GNIRS Dispersers
                        facilities.addAll(Arrays.asList(GNIRSParams.SlitWidth.values())); // GNIRS Slits
                        facilities.addAll(Arrays.asList(TReCSParams.Disperser.values())); // TReCS Dispersers
                        facilities.addAll(Arrays.asList(TReCSParams.Mask.values()));      // TReCS Slits
                        break;

                    case GN:
                        facilities.addAll(Arrays.asList(FilterNorth.values()));  // GMOS North Filters
                        break;
                }

                // Intentionally no break here, versions may need more than one migration step!

            // MIGRATE 104 TO 1030
            case ScheduleIO.VERSION_104:

                LOGGER.info("Upgrading serial version " + ScheduleIO.VERSION_104 + " to " + ScheduleIO.VERSION_1030);
                migrateSetupRequired(params);

                // Intentionally no break here, versions may need more than one migration step!

            // MIGRATE 1030 TO 1031
            case ScheduleIO.VERSION_1030:

                LOGGER.info("Upgrading serial version " + ScheduleIO.VERSION_1030 + " to " + ScheduleIO.VERSION_1031);
                // adding options as part of the QV project means we need to reflect them here, too, even
                // though all those new options are hidden (at least for now)
                switch (model.getSite()) {
                    case GS:
                        facilities.addAll(Arrays.asList(Flamingos2.Disperser.values()));
                        facilities.addAll(Arrays.asList(Flamingos2.Filter.values()));
                        facilities.addAll(Arrays.asList(GmosCommonType.UseNS.values()));
                        facilities.addAll(Arrays.asList(PreImagingType.values()));          // GMOS & F2
                        facilities.addAll(Arrays.asList(GNIRSParams.CrossDispersed.values()));
                        facilities.addAll(Arrays.asList(GNIRSParams.Filter.values()));
                        facilities.addAll(Arrays.asList(GNIRSParams.Camera.values()));
                        facilities.addAll(Arrays.asList(Gpi.Disperser.values()));
                        facilities.addAll(Arrays.asList(Gpi.Filter.values()));
                        facilities.addAll(Arrays.asList(Gsaoi.Filter.values()));
                        facilities.addAll(Arrays.asList(NICIParams.FocalPlaneMask.values()));
                        facilities.addAll(Arrays.asList(NICIParams.DichroicWheel.values()));
                        facilities.addAll(Arrays.asList(NICIParams.Channel1FW.values()));
                        facilities.addAll(Arrays.asList(NICIParams.Channel2FW.values()));
                        facilities.addAll(Arrays.asList(TexesParams.Disperser.values()));
                        break;

                    case GN:
                        facilities.addAll(Arrays.asList(GmosCommonType.UseNS.values()));
                        facilities.addAll(Arrays.asList(PreImagingType.values()));          // GMOS
                        facilities.addAll(Arrays.asList(NIFSParams.Disperser.values()));
                        facilities.addAll(Arrays.asList(NIFSParams.Filter.values()));
                        facilities.addAll(Arrays.asList(NIFSParams.Mask.values()));
                        facilities.addAll(Arrays.asList(Niri.Mask.values()));
                        facilities.addAll(Arrays.asList(Niri.Disperser.values()));
                        facilities.addAll(Arrays.asList(Niri.Camera.values()));
                        break;
                }

                // Intentionally no break here, versions may need more than one migration step!

            // MIGRATE 1031 to 1032
            case ScheduleIO.VERSION_1031:
                LOGGER.info("Upgrading serial version " + ScheduleIO.VERSION_1031 + " to " + ScheduleIO.VERSION_1032);

                // Here the block boundaries are set at nautical twilight, which
                // clips all the caches (like VISIBLE_UNION_CACHE) at nautical
                // twilight. We expect blocks trimmed by 12 degree twilight
                // bounds.  If we find one, swap it for one trimmed by 6 degree
                // twilight.

                final Site s = model.getSite();

                ImOption.apply(params.getParamSet("blocks")).foreach(bs -> {
                    DefaultImList.create(bs.getParamSets("block")).foreach(b -> {
                        final Param ps = b.getParam("start");
                        final Param pe = b.getParam("end");

                        final Long ts = Long.valueOf(ps.getValue());
                        final Long te = Long.valueOf(pe.getValue());

                        final TwilightBoundedNight n = TwilightBoundedNight.forTime(NAUTICAL, ts, s);

                        if (ts == n.getStartTime() && te == n.getEndTime()) {
                            final TwilightBoundedNight c = TwilightBoundedNight.forTime(CIVIL, ts, s);
                            ps.setValue(String.valueOf(c.getStartTime()));
                            pe.setValue(String.valueOf(c.getEndTime()));
                        }
                    });
                });

                // Intentionally no break here, versions may need more than one migration step!

            // ADD MORE MIGRATION STEPS HERE AS NEEDED
            // .....
        }

        // ok, now the data should be up to date and ready to be processed
        this.ictdSummary = ImOption.apply(params.getParamSet(PROP_ICTD)).map(Ictd::decode);
        this.blocks = getBlockUnion(params);
        this.variants = new VariantList(this, params.getParamSet(PROP_VARIANTS));
        this.extraSemesters = getExtraSemesters(params);
        this.comment = Pio.getValue(params, PROP_COMMENT);
        init(false, ImOption.empty());
    }

    private void init(boolean isNew, Option<Map<Enum<?>, Availability>> oam) {
        if (isNew) initFacilities();
        if (!variants.isEmpty())
            setCurrentVariant(variants.getFirst());
        Listeners.attach(this);
        for (Variant v: variants)
            Listeners.attach(v);
        addHiddenFacilities();
        setDirty(false);
        oam.foreach(this::matchFacilitiesToIctd);
    }

    @Override
    public void setDirty(boolean dirty) {
        super.setDirty(dirty);
        LOGGER.fine("dirty => " + dirty);
    }

    ///
    /// MINI-MODEL
    ///

    public MiniModel getMiniModel() {
        return miniModel;
    }

    public void setMiniModel(MiniModel miniModel) {
        if (this.miniModel.getSite() != miniModel.getSite())
            throw new IllegalArgumentException("Sites do not match, sorry.");

        MiniModel prev = this.miniModel;

        invalidateAllCaches();

        this.miniModel = miniModel;
        for (Variant v: variants)
            v.miniModelChanged(miniModel);

        firePropertyChange(PROP_MINI_MODEL, prev, miniModel);

    }

    public Option<IctdSummary> getIctdSummary() {
        return ictdSummary;
    }

    @SuppressWarnings("unchecked")
    public void setIctdSummary(Option<IctdSummary> ictd) {
        final Option<IctdSummary> prev = this.ictdSummary;

        invalidateAllCaches();

        this.ictdSummary = ictd;
        doPublicFacilitiesUpdate(() -> ictd.foreach(i -> matchFacilitiesToIctd(i.featureAvailabilityJava())));

        firePropertyChange(PROP_ICTD, prev, ictd);
    }

    /**
     * Determines the availability of the indicated custom mask.  Note, if there
     * is no ICTD data, we assume that the mask is available.
     */
    public Availability maskAvailability(CustomMaskKey key) {
        return getIctdSummary().map(i -> i.maskAvailabilityJava().getOrDefault(key, Availability.Missing))
                        .getOrElse(Availability.Installed); // no ICTD => assume installed
    }

    public void moveAllocs(long offset) {
        try {
            // Move every alloc forward (or back ... :-/)
            for (Variant v : getVariants()) {
                v.setFlagUpdatesEnabled(false);
                SortedSet<Alloc> allocs = v.getAllocs();
                for (Alloc a : allocs) {
                    a.move(a.getStart() + offset);
                }
            }

        } finally {
            invalidateAllCaches();
            for (Variant v : getVariants()) {
                v.invalidateAllCaches();
                v.setFlagUpdatesEnabled(true);
            }
        }
    }

    ///
    /// PIO
    ///

    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = factory.createParamSet(name);
        params.addParamSet(facilities.getParamSet(factory, PROP_FACILITIES));
        params.addParamSet(blocks.getParamSet(factory, PROP_BLOCKS));
        params.addParamSet(variants.getParamSet(factory, PROP_VARIANTS));
        params.addParamSet(extraSemesters.getParamSet(factory, PROP_EXTRA_SEMESTERS));
        ictdSummary.foreach(i -> params.addParamSet(Ictd.encode(factory, PROP_ICTD, i)));
        Pio.addParam(factory, params, PROP_COMMENT, comment);
        return params;
    }

    // package-private, needed for ScheduleIO
    static BlockUnion getBlockUnion(ParamSet params) {
        return new BlockUnion(params.getParamSet(PROP_BLOCKS));
    }

    static StringSet getExtraSemesters(ParamSet params) {
        return new StringSet(params.getParamSet(PROP_EXTRA_SEMESTERS));
    }

    ///
    /// FACILITIES
    ///

    // Initialize our facilities collection with the default instruments
    // (or default options for instruments with changeable configurations).
    @SuppressWarnings("unchecked")
    private synchronized void initFacilities() {
        for (Inst inst : Inst.values()) {
            if (inst.existsAtSite(miniModel.getSite())) {
                if (inst.isNormallyAvailable()) {
                    facilities.add(inst.getValue());
                }
                for (Enum option : inst.getOptions())
                    if (inst.isNormallyAvailable(option))
                        facilities.add(option);
            }
        }
    }

    // Adds/removes facilities based upon the availability data from the ICTD.
    private synchronized void matchFacilitiesToIctd(Map<Enum<?>, Availability> am) {
        for (Map.Entry<Enum<?>, Availability> me: am.entrySet()) {
            if (me.getValue() == Availability.Installed) facilities.add(me.getKey());
            else facilities.remove(me.getKey());
        }
    }

    private synchronized void addHiddenFacilities() {
        for (Inst inst : Inst.values()) {
            facilities.addAll(Arrays.asList(inst.getHiddenOptions()));
        }
    }

    private void doPublicFacilitiesUpdate(Runnable action) {
        Set<Enum> prev = getFacilities();
        action.run();
        firePropertyChange(PROP_FACILITIES, prev, getFacilities());
        setDirty(true);
        for (Variant v: variants)
            v.facilitiesChanged();
    }

    @SuppressWarnings("unchecked")
    public void addFacility(Enum o) {
        assert o instanceof Serializable;
        doPublicFacilitiesUpdate(() -> facilities.add(o));
    }

    @SuppressWarnings("unchecked")
    public void removeFacility(Enum o) {
        doPublicFacilitiesUpdate(() -> facilities.remove(o));
    }

    @SuppressWarnings("unchecked")
    public void setFacilities(Collection<Enum> newFacilities) {
        doPublicFacilitiesUpdate(() -> {
            facilities.clear();
            facilities.addAll(newFacilities);
            addHiddenFacilities();
        });
    }

    @SuppressWarnings("unchecked")
    public boolean hasFacility(Enum o) {
        return facilities.contains(o);
    }

    @SuppressWarnings("unchecked")
    public Set<Enum> getFacilities() {
        return new HashSet<Enum>(facilities);
    }

    ///
    /// PLANNER COMMENTS
    ///

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        setDirty(true);
    }

    ///
    /// VARIANTS
    ///

    public Variant addVariant(String name, byte cc, byte iq, byte wv, ApproximateAngle windConstraint, Boolean lgsConstraint) {
        return addVariant(name, new Conds((byte) 0 /* Any sky brightness. */, cc, iq, wv), windConstraint, lgsConstraint);
    }

    public Variant addVariant(String name, Conds conds, ApproximateAngle windConstraint, Boolean lgsConstraint) {
        List<Variant> prev = Collections.unmodifiableList(new ArrayList<Variant>());
        Variant v = new Variant(this, name, conds, windConstraint, lgsConstraint);
        variants.add(v);
        firePropertyChange(PROP_VARIANTS, prev, getVariants());
        if (currentVariant == null) setCurrentVariant(v);
        setDirty(true);
        Listeners.attach(v);
        return v;
    }

    /**
     * Removes the specified Variant from this Schedule.
     */
    public void removeVariant(Variant v) {
        List<Variant> prev = Collections.unmodifiableList(new ArrayList<Variant>());
        if (currentVariant == v) setCurrentVariant(null);
        variants.remove(v);
        firePropertyChange(PROP_VARIANTS, prev, getVariants());
        setDirty(true);
        Listeners.detach(v);
    }

    /**
     * Moves the specified Variant up or down.
     * @throws ArrayIndexOutOfBoundsException if delta would push the variant out of the list.
     */
    public void moveVariant(Variant v, int delta) {
        List<Variant> prev = Collections.unmodifiableList(new ArrayList<Variant>());
        int newPos = variants.indexOf(v) + delta;
        variants.remove(v);
        variants.add(newPos, v);
        firePropertyChange(PROP_VARIANTS, prev, getVariants());
        setDirty(true);
    }

    /**
     * Duplicates the specified variant, adding it next in the list (or to the end
     * if the variant is from another plan) and making it current. This method handles
     * some of the subtle issues related to deep-cloning the variant, hooking up
     * listeners. etc.
     * <p>
     * Note that if the supplied variant is from another plan, the destination's
     * mini-model should be replaced (but first be sure to add any extra semesters
     * referred to by the imported variant). See MergeAction for an example.
     * @param old
     * @return
     */
    public Variant duplicateVariant(Variant old) {
        List<Variant> prev = Collections.unmodifiableList(new ArrayList<Variant>());

        Variant v = new Variant(this, old.getName(), old.getConditions(), old.getWindConstraint(), old.getLgsConstraint());
        for (Alloc a: old.getAllocs()) {
            try {
                v.addAlloc(a.getObs(), a.getStart(), a.getFirstStep(), a.getLastStep(), a.getSetupType(), a.getComment());
            } catch (EditException pe) {
                // This will never happen; we can always copy the allocs first to last without
                // breaking any rules.
                LOGGER.log(Level.SEVERE, "Problem duplicating variant " + old, pe);
                throw new RuntimeException(pe); // ?
            }
        }

        v.setComment(old.getComment());
        int newPos = variants.indexOf(old) + 1;
        if (newPos == 0)
            variants.add(v);
        else
            variants.add(newPos, v);

        firePropertyChange(PROP_VARIANTS, prev, getVariants());
        setCurrentVariant(v);
        setDirty(true);
        Listeners.attach(v);
        return v;
    }

    /**
     * Returns this Schedule's list of Variants (read-only).
     * @return the Variants collection
     */
    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public void setCurrentVariant(Variant v) {
        assert v == null || variants.contains(v);
        Variant prev = currentVariant;
        currentVariant = v;
        firePropertyChange(PROP_CURRENT_VARIANT, prev, currentVariant);
//        setDirty(true);
    }

    public Variant getCurrentVariant() {
        return currentVariant;
    }

    ///
    /// BLOCKS
    ///

    public void addBlock(long start, long end) {
        SortedSet<Block> prev = Collections.unmodifiableSortedSet(new TreeSet<Block>(getBlocks()));
        blocks.add(new Block(start, end));
        synchronized (intervalCache) {
            intervalCache.clear();
        }
        firePropertyChange(PROP_BLOCKS, prev, getBlocks());
        setDirty(true);
    }

    public void removeBlock(long start, long end) {
        SortedSet<Block> prev = Collections.unmodifiableSortedSet(new TreeSet<Block>(getBlocks()));
        blocks.remove(new Block(start, end));
        synchronized (intervalCache) {
            intervalCache.clear();
        }
        firePropertyChange(PROP_BLOCKS, prev, getBlocks());
        setDirty(true);
    }

    public void addObservingNights(long start, long end) {
        for (long i = start; i <= end; i += MS_PER_DAY) {
            final TwilightBoundedNight night = Twilight.startingOnDate(i, miniModel.getSite());
            addBlock(night.getStartTime(), night.getEndTime());
        }
    }

    public void addObservingNights(int count) {
        if (count < 1) throw new IllegalArgumentException("You must add at least one night.");
        long now = System.currentTimeMillis();
        addObservingNights(now, now + (count - 1) * TimeUtils.MS_PER_DAY);
    }

    public SortedSet<Block> getBlocks() {
        return blocks.getIntervals();
    }

    public SortedSet<Interval> getBlockIntervals() {
        final SortedSet<Interval> ts = new TreeSet<>();
        for (Block b : blocks) {
            ts.add(new Interval(b.getStart(), b.getEnd()));
        }
        return ts;
    }


    ///
    /// TRIVIAL GETTERS
    ///

    @Override
    public String toString() {
        return getName(); // getClass().getSimpleName() + ":" + miniModel.getSite();
    }

    public Site getSite() {
        return miniModel.getSite();
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    ///
    /// Extra Semesters
    ///

    public SortedSet<String> getExtraSemesters() {
        return Collections.unmodifiableSortedSet(extraSemesters);
    }

    public void addExtraSemester(String semester) {
        if (!miniModel.getAllSemesters().contains(semester))
            throw new NoSuchElementException(semester);
        SortedSet<String> prev = new TreeSet<>(extraSemesters);
        if (extraSemesters.add(semester))
            firePropertyChange(PROP_EXTRA_SEMESTERS, prev, getExtraSemesters());
    }

    public void removeExtraSemester(String semester) {
        if (!extraSemesters.contains(semester))
            throw new NoSuchElementException(semester);
        for (Variant v: variants) {
            for (Alloc a: v.getAllocs()) {
                if (a.getObs().getProg().getSemesterAsJava().map(s -> s.toString()).contains(semester)) {
                    throw new IllegalStateException("Can't remove. Plan contains observations from " + semester + ".");
                }
            }
        }
        SortedSet<String> prev = new TreeSet<>(extraSemesters);
        extraSemesters.remove(semester);
        firePropertyChange(PROP_EXTRA_SEMESTERS, prev, getExtraSemesters());
    }

    ///
    /// I/O
    ///

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        File prev = this.file;
        this.file = file;
        firePropertyChange(PROP_FILE, prev, file);
    }

    ///
    /// DERIVED PROPERTIES
    ///

    /**
     * The schedule if empty if there are no blocks AND all variants are empty.
     */
    public boolean isEmpty() {
        if (!blocks.isEmpty()) return false;
        for (Variant v: variants) {
            if (!v.isEmpty()) return false;
        }
        return true;
    }

    public long getStart() {
        if (isEmpty()) throw new IllegalStateException("Schedule is empty.");
        long start = Long.MAX_VALUE;
        if (!blocks.isEmpty()) start = blocks.getIntervals().first().getStart();
        if (variants != null) {
            for (Variant v: variants) {
                if (!v.isEmpty()) start = Math.min(start, v.getStart());
            }
        }
        return start;
    }

    public long getEnd() {
        if (isEmpty()) throw new IllegalStateException("Schedule is empty.");
        long end = Long.MIN_VALUE;
        if (!blocks.isEmpty()) end = blocks.getIntervals().last().getEnd();
        if (variants != null) {
            for (Variant v: variants) {
                if (!v.isEmpty()) end = Math.max(end, v.getEnd());
            }
        }
        return end;
    }

    public long getSpan() {
        return getEnd() - getStart();
    }

    public long getMiddlePoint() { return (getStart() + getEnd()) / 2; }

    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (getFile() != null) {
            sb.append(getFile().getName());
            sb.append(" - ");
        }
        sb.append(getSite().displayName);
        if (!isEmpty()) {
            sb.append(" - ");
            sb.append(DateUtil.formatUTCyyyymmdd(getEnd()));
        }
        return sb.toString();
    }

    public static class BlockUnion extends Union<Block> implements PioSerializable {

        public static final String PROP_MEMBER = "block";

        public BlockUnion() {
        }

        public BlockUnion(ParamSet params) {
            for (ParamSet blockParams: params.getParamSets(PROP_MEMBER)) {
                add(new Block(blockParams));
            }
        }

        public ParamSet getParamSet(PioFactory factory, String name) {
            ParamSet params = factory.createParamSet(name);
            for (Block block: getIntervals())
                params.addParamSet(block.getParamSet(factory, PROP_MEMBER));
            return params;
        }

    }

    //
    // MIGRATIONS
    //

    /**
     * Migrate boolean value "setupRequired" to enum "setupType" (REL-1346).
     * This is part of the migration from 104 to 1030.
     */
    private void migrateSetupRequired(ParamSet params) {

        // descend into child structures
        for (Object c : params.getChildren()) {
            if (c instanceof ParamSet) {
                migrateSetupRequired((ParamSet) c);
            }
        }

        // for all "setupRequired" boolean values add the corresponding new "setupType" enum
        for (ParamSet alloc: params.getParamSets(AllocSet.PROP_MEMBER)) {
            if (alloc.getParam("setupRequired") != null) {
                boolean setupRequired = Pio.getBooleanValue(alloc, "setupRequired", true);
                String setupType = setupRequired ? Alloc.SetupType.FULL.toString() : Alloc.SetupType.NONE.toString();
                alloc.getParam("setupRequired").setValue(setupType);  // in-place replacement of old with new value
                alloc.getParam("setupRequired").setName("setupType"); // change value name to reflect its new type
                LOGGER.info("migrating setupRequired " + setupRequired + " to " + setupType);
            }
        }
    }

}




@SuppressWarnings("serial")
class VariantList extends LinkedList<Variant> implements PioSerializable {

    public static final String PROP_MEMBER = "variant";

    public VariantList() {
    }

    public VariantList(Schedule schedule, ParamSet paramSet) {
        if (paramSet != null) {
            for (ParamSet variantParams: paramSet.getParamSets(PROP_MEMBER))
                add(new Variant(schedule, variantParams));
        }
    }

    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = factory.createParamSet(name);
        for (Variant v: this)
            params.addParamSet(v.getParamSet(factory, PROP_MEMBER));
        return params;
    }

}


@SuppressWarnings("serial")
class StringSet extends TreeSet<String> implements PioSerializable {

    public static final String PROP_MEMBER = "item";

    public StringSet() {
    }

    @SuppressWarnings("unchecked")
    public StringSet(ParamSet paramSet) {
        if (paramSet != null) {
            for (Param p: (List<Param>) paramSet.getParams(PROP_MEMBER)) {
                add(p.getValue());
            }
        }
    }

    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = factory.createParamSet(name);
        for (String s: this) {
            Pio.addParam(factory, params, PROP_MEMBER, s);
        }
        return params;
    }

}

@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
class MixedEnumSet extends HashSet<Enum> implements PioSerializable {

    private static final Logger LOGGER = Logger.getLogger(MixedEnumSet.class.getName());

    public static final String PROP_MEMBER = "member";

    public MixedEnumSet() {
    }

    public MixedEnumSet(ParamSet params) {
        if (params != null) {
            for (ParamSet ps: params.getParamSets(PROP_MEMBER)) {
                Enum e = EnumPio.getEnum(ps);
                if (e != null)
                    add(e);
            }
        }
    }

    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet params = factory.createParamSet(name);
        for (Enum e: this)
            params.addParamSet(EnumPio.getEnumParamSet(factory, PROP_MEMBER, e));
        return params;
    }

}


