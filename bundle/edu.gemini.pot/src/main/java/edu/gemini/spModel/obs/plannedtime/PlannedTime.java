package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.time.ChargeClass;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Step-by-step detailed planned time accounting information for an observation.
 */
public final class PlannedTime implements Serializable {
    /**
     * Determines whether any of the given items have been updated in the
     * configuration (as opposed to their values in the previous configuration).
     *
     * @param cur current configuration
     * @param prevOpt previous configuration wrapped in an Option
     * @param keys item keys to examine
     *
     * @return <code>true</code> if any item has been updated in <code>cur</code>,
     * <code>false</code> otherwise
     */
    public static boolean isUpdated(Config cur, Option<Config> prevOpt, ItemKey... keys) {
        if (prevOpt.isEmpty()) return false;
        Config prev = prevOpt.getValue();

        for (ItemKey key : keys) {
            Object curValue  = cur.getItemValue(key);
            Object prevValue = prev.getItemValue(key);

            if (curValue == null) {
                if (prevValue != null) return true;
            } else {
                if (!curValue.equals(prevValue)) return true;
            }
        }
        return false;
    }

    public interface StepCalculator {
        CategorizedTimeGroup calc(Config stepConfig, Option<Config> prevStepConfig);
    }

    private static long toMillsec(double sec) {
        return Math.round(1000.0 * sec);
    }

    public enum Category {
        CONFIG_CHANGE("Config Change"),
        EXPOSURE("Exposure"),
        READOUT("Readout"),
        DHS_WRITE("DHS Write"),
        ;

        public final String display;

        Category(String display) {
            this.display = display;
        }

        public final CategorizedTime ZERO = new CategorizedTime(this, 0, ImOption.empty());

    }

    public static final class CategorizedTime implements Comparable<CategorizedTime>, Serializable {
        public final Category category;

        /** Time in milliseconds. */
        public final long time;

        public final Option<String> detail;

        private CategorizedTime(Category cat, long time, Option<String> detail) {
            if (cat == null) throw new IllegalArgumentException("Category is null");
            this.category = cat;
            this.time     = time;
            this.detail   = detail;
        }

        public Duration timeDuration() {
            return Duration.ofMillis(time);
        }

        public double timeSeconds() {
            return time / 1000.0;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CategorizedTime that = (CategorizedTime) o;

            if (time != that.time) return false;
            if (category != that.category) return false;
            return java.util.Objects.equals(detail, that.detail);
        }

        @Override public int hashCode() {
            int result = category.hashCode();
            result = 31 * result + (int) (time ^ (time >>> 32));
            result = 31 * result + java.util.Objects.hashCode(detail);
            return result;
        }

        public CategorizedTime add(long time) {
            return (time == 0) ? this : new CategorizedTime(category, this.time + time, this.detail);
        }

        /**
         * @param sec time in seconds for compatibility with old code --
         * converted to a <code>long</code> in millisec
         */
        public static CategorizedTime fromSeconds(Category cat, double sec) {
            long time = toMillsec(sec);
            return (time == 0) ? cat.ZERO : new CategorizedTime(cat, time, ImOption.empty());
        }

        public static CategorizedTime fromSeconds(Category cat, double sec, String detail) {
            return (detail == null) ? fromSeconds(cat, sec) : new CategorizedTime(cat, toMillsec(sec), ImOption.apply(detail));
        }

        public static CategorizedTime apply(Category cat, long time) {
            return (time == 0) ? cat.ZERO : new CategorizedTime(cat, time, ImOption.empty());
        }

        public static CategorizedTime apply(Category cat, long time, String detail) {
            return new CategorizedTime(cat, time, ImOption.apply(detail));
        }

        @Override public int compareTo(CategorizedTime that) {
            int res = category.compareTo(that.category);
            if (res != 0) return res;

            if (time < that.time) return -1;
            if (time > that.time) return 1;

            return detail.getOrElse("").compareTo(that.detail.getOrElse(""));
        }

        @Override public String toString() {
            return String.format("%15s - %7d - %s", category.name(), time, detail.getOrElse(""));
        }
    }

    public static final class CategorizedTimeGroup implements Iterable<CategorizedTime>, Serializable {
        public static final CategorizedTimeGroup EMPTY = new CategorizedTimeGroup(Collections.emptySet());

        public final Set<CategorizedTime> times;

        private CategorizedTimeGroup(Set<CategorizedTime> times) {
            this.times = times;
        }

        /**
         * Filters all the contained CategorizedTimes on a particular category,
         * returning only those in this category.
         */
        public Set<CategorizedTime> times(Category cat) {
            return times.stream().filter(ct -> ct.category == cat).collect(Collectors.toCollection(TreeSet::new));
        }

        public CategorizedTimeGroup filter(PredicateOp<CategorizedTime> op) {
            Set<CategorizedTime> res = times.stream().filter(op::apply).collect(Collectors.toCollection(TreeSet::new));
            return (times.size() == res.size()) ? this : new CategorizedTimeGroup(res);
        }

        public CategorizedTimeGroup add(CategorizedTime ct) {
            Set<CategorizedTime> s = new TreeSet<>(times);
            s.add(ct);
            return new CategorizedTimeGroup(Collections.unmodifiableSet(s));
        }

        public CategorizedTimeGroup add(CategorizedTimeGroup ctg) {
            if (times.size() == 0) return ctg;
            if (ctg.times.size() == 0) return this;

            Set<CategorizedTime> s = new TreeSet<>(times);
            s.addAll(ctg.times);
            return new CategorizedTimeGroup(Collections.unmodifiableSet(s));
        }

        public CategorizedTimeGroup addAll(Collection<CategorizedTime> times) {
            if (times.size() == 0) return this;
            Set<CategorizedTime> s = new TreeSet<>(this.times);
            s.addAll(times);
            return new CategorizedTimeGroup(Collections.unmodifiableSet(s));
        }

        public CategorizedTimeGroup addAll(CategorizedTime... times) {
            if (times.length == 0) return this;
            return addAll(Arrays.asList(times));
        }

        public static CategorizedTimeGroup apply(CategorizedTime ct) {
            return EMPTY.add(ct);
        }

        public static CategorizedTimeGroup apply(Collection<CategorizedTime> col) {
            return EMPTY.addAll(col);
        }

        public static CategorizedTimeGroup apply(CategorizedTime... col) {
            return EMPTY.addAll(col);
        }

        @Override public Iterator<CategorizedTime> iterator() {
            return times.iterator();
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CategorizedTimeGroup that = (CategorizedTimeGroup) o;
            return times.equals(that.times);
        }

        @Override public int hashCode() {
            return times.hashCode();
        }

        public HashMap<Category, ImList<CategorizedTime>> groupTimes() {
            return DefaultImList.create(times).groupBy(t -> t.category);
        }

        public long totalTime() {
            if (times.size() == 0) return 0;

            // Times are stored sorted.  All CategorizedTime instances in the
            // same category except the last (biggest) should be ignored
            // because they represent concurrent actions.
            Iterator<CategorizedTime> it = times.iterator();
            long res = 0;
            CategorizedTime prev = it.next();
            while (it.hasNext()) {
                CategorizedTime cur = it.next();
                if (cur.category != prev.category) res += prev.time;
                prev = cur;
            }
            res += prev.time;
            return res;
        }
    }

    public static final class Setup implements Serializable {
        public final SetupTime time;
        public final ChargeClass chargeClass;

        private Setup(SetupTime time, ChargeClass chargeClass) {
            if (time        == null) throw new IllegalArgumentException("time is null");
            if (chargeClass == null) throw new IllegalArgumentException("chargeClass is null");
            this.time        = time;
            this.chargeClass = chargeClass;
        }

        public static Setup apply(SetupTime time, ChargeClass chargeClass) {
            return new Setup(time, chargeClass);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Setup setup = (Setup) o;
            return Objects.equals(time, setup.time) &&
                    Objects.equals(chargeClass, setup.chargeClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(time, chargeClass);
        }
    }

    public static final class Step implements Serializable {
        public final CategorizedTimeGroup times;
        public final ChargeClass chargeClass;
        public final boolean executed;
        public final String obsType;


        private Step(CategorizedTimeGroup times, ChargeClass chargeClass, boolean executed, String obsType) {
            if (times == null) throw new IllegalArgumentException("times are null");
            if (chargeClass == null) throw new IllegalArgumentException("chargeClass is null");
            if (obsType == null) throw new IllegalArgumentException("obsType is null");

            this.times       = times;
            this.chargeClass = chargeClass;
            this.executed    = executed;
            this.obsType     = obsType;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Step step = (Step) o;

            if (executed != step.executed) return false;
            if (chargeClass != step.chargeClass) return false;
            if (!obsType.equals(step.obsType)) return false;
            return times.equals(step.times);
        }

        private Step(CategorizedTimeGroup times) {
            this(times, ChargeClass.PROGRAM, false, "OBJECT");
        }

        @Override public int hashCode() {
            int result = times.hashCode();
            result = 31 * result + chargeClass.hashCode();
            result = 31 * result + (executed ? 1 : 0);
            result = 31 * result + obsType.hashCode();
            return result;
        }

        public long totalTime() {
            return times.totalTime();
        }

        public long totalTime(ChargeClass chargeClass) {
            return (this.chargeClass == chargeClass) ? totalTime() : 0;
        }

        public static Step apply(CategorizedTimeGroup times, ChargeClass chargeClass, boolean executed, String obsType) {
            return new Step(times, chargeClass, executed, obsType);
        }

        public static Step apply(CategorizedTimeGroup times) {
            return new Step(times);
        }

    }

    public final Setup setup;
    public final List<Step> steps;
    public final ConfigSequence sequence;

    private PlannedTime(Setup setup, List<Step> steps, ConfigSequence sequence) {
        if (setup == null) throw new IllegalArgumentException("setup is null");
        if (steps == null) throw new IllegalArgumentException("steps are null");

        this.setup    = setup;
        this.steps    = steps;
        this.sequence = sequence;
    }

    public <T> T foldSteps(T init, BiFunction<T, Step, T> sum) {
        T res = init;
        for (Step s : steps) res = sum.apply(res, s);
        return res;
    }

    public <T> T fold(Function<Setup, T> init, BiFunction<T, Step, T> sum) {
        return foldSteps(init.apply(setup), sum);
    }

    public long totalTime() {
        return fold(s -> s.time.toDuration(), (d, s) -> d.plusMillis(s.totalTime())).toMillis();
    }

    public long totalTime(ChargeClass chargeClass) {
        return fold(
                 s -> (s.chargeClass == chargeClass) ? s.time.toDuration() : Duration.ZERO,
                 (d, s) -> d.plusMillis(s.totalTime(chargeClass))
               ).toMillis();
    }

    public PlannedTimeSummary toPlannedTimeSummary() {
        return new PlannedTimeSummary(totalTime(ChargeClass.PROGRAM), totalTime());
    }

    public PlannedStepSummary toPlannedStepSummary() {

        long[] stepTimes   = new long[steps.size()];
        boolean[] executed = new boolean[steps.size()];
        String[] obsTypes  = new String[steps.size()];

        int i = 0;
        for (Step step : steps) {
            stepTimes[i] = step.totalTime();
            executed[i]  = step.executed;
            obsTypes[i]  = step.obsType;
            ++i;
         }

        return new PlannedStepSummary(setup.time, stepTimes, executed, obsTypes);
    }

    public static PlannedTime apply(Setup setup) {
        return new PlannedTime(setup, Collections.emptyList(), ConfigSequence.EMPTY);
    }

    public static PlannedTime apply(Setup setup, List<Step> steps, ConfigSequence sequence) {
        steps = Collections.unmodifiableList(new ArrayList<>(steps));
        return new PlannedTime(setup, steps, sequence);
    }

}
