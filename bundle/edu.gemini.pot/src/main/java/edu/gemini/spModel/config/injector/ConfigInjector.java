package edu.gemini.spModel.config.injector;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Injects a value into the configuration at a particular step based on the
 * current values of one or more other parameters.
 */
public final class ConfigInjector<R> implements Serializable {

    /**
     * Interface that adapts the ConfigInjectorCalc? for generic usage by the
     * ConfigInjector.
     */
    private interface CalcAdapter<R> extends Serializable {
        List<PropertyDescriptor> list();
        String resultPropertyName();
        R apply(ISysConfig cur, ISysConfig prev);
    }

    /**
     * Adapter for single argument calculators.
     */
    private static final class Adapter1<A, R> implements CalcAdapter<R> {
        private final ConfigInjectorCalc1<A, R> calc;
        private final List<PropertyDescriptor> props;
        private final String result;

        Adapter1(ConfigInjectorCalc1<A, R> calc) {
            this.calc = calc;

            List<PropertyDescriptor> tmp = new ArrayList<>();
            tmp.add(calc.descriptor1());
            props = Collections.unmodifiableList(tmp);
            result = calc.resultPropertyName();
        }

        public List<PropertyDescriptor> list() { return props; }
        public String resultPropertyName() { return result; }

        @SuppressWarnings({"unchecked"})
        public R apply(ISysConfig cur, ISysConfig prev) {
            A aVal = (A) getValue(calc.descriptor1(), cur, prev);
            if (aVal == null) return null;
            return calc.apply(aVal);
        }
    }


    /**
     * Adapter for two argument calculators.
     */
    private static final class Adapter2<A, B, R> implements CalcAdapter<R> {
        private final ConfigInjectorCalc2<A, B, R> calc;
        private final List<PropertyDescriptor> props;
        private final String result;

        Adapter2(ConfigInjectorCalc2<A, B, R> calc) {
            this.calc = calc;

            List<PropertyDescriptor> tmp = new ArrayList<>();
            tmp.add(calc.descriptor1());
            tmp.add(calc.descriptor2());
            props = Collections.unmodifiableList(tmp);
            result = calc.resultPropertyName();
        }

        public List<PropertyDescriptor> list() { return props; }
        public String resultPropertyName() { return result; }

        @SuppressWarnings({"unchecked"})
        public R apply(ISysConfig cur, ISysConfig prev) {
            A aVal = (A) getValue(calc.descriptor1(), cur, prev);
            if (aVal == null) return null;
            B bVal = (B) getValue(calc.descriptor2(), cur, prev);
            if (bVal == null) return null;

            return calc.apply(aVal, bVal);
        }
    }

    /**
     * Adapter for three argument calculators.
     */
    private static final class Adapter3<A, B, C, R> implements CalcAdapter<R> {
        private final ConfigInjectorCalc3<A, B, C, R> calc;
        private final List<PropertyDescriptor> props;
        private final String result;

        Adapter3(ConfigInjectorCalc3<A, B, C, R> calc) {
            this.calc = calc;

            List<PropertyDescriptor> tmp = new ArrayList<>();
            tmp.add(calc.descriptor1());
            tmp.add(calc.descriptor2());
            tmp.add(calc.descriptor3());
            props = Collections.unmodifiableList(tmp);
            result = calc.resultPropertyName();
        }

        public List<PropertyDescriptor> list() { return props; }
        public String resultPropertyName() { return result; }

        @SuppressWarnings({"unchecked"})
        public R apply(ISysConfig cur, ISysConfig prev) {
            A aVal = (A) getValue(calc.descriptor1(), cur, prev);
            if (aVal == null) return null;
            B bVal = (B) getValue(calc.descriptor2(), cur, prev);
            if (bVal == null) return null;
            C cVal = (C) getValue(calc.descriptor3(), cur, prev);
            if (cVal == null) return null;

            return calc.apply(aVal, bVal, cVal);
        }
    }

    /**
     * Adapter for four argument calculators.
     */
    private static final class Adapter4<A, B, C, D, R> implements CalcAdapter<R> {
        private final ConfigInjectorCalc4<A, B, C, D, R> calc;
        private final List<PropertyDescriptor> props;
        private final String result;

        Adapter4(ConfigInjectorCalc4<A, B, C, D, R> calc) {
            this.calc = calc;

            List<PropertyDescriptor> tmp = new ArrayList<>();
            tmp.add(calc.descriptor1());
            tmp.add(calc.descriptor2());
            tmp.add(calc.descriptor3());
            tmp.add(calc.descriptor4());
            props = Collections.unmodifiableList(tmp);
            result = calc.resultPropertyName();
        }

        public List<PropertyDescriptor> list() { return props; }
        public String resultPropertyName() { return result; }

        @SuppressWarnings({"unchecked"})
        public R apply(ISysConfig cur, ISysConfig prev) {
            A aVal = (A) getValue(calc.descriptor1(), cur, prev);
            if (aVal == null) return null;
            B bVal = (B) getValue(calc.descriptor2(), cur, prev);
            if (bVal == null) return null;
            C cVal = (C) getValue(calc.descriptor3(), cur, prev);
            if (cVal == null) return null;
            D dVal = (D) getValue(calc.descriptor4(), cur, prev);
            if (dVal == null) return null;

            return calc.apply(aVal, bVal, cVal, dVal);
        }
    }

    /**
     * Creates a ConfigInjector for a calculator that requires one argument.
     *
     * @param calc calculator for a derived sequence property
     */
    public static <A, R> ConfigInjector<R> create(ConfigInjectorCalc1<A, R> calc) {
        return new ConfigInjector<>(new Adapter1<>(calc));
    }

    /**
     * Creates a ConfigInjector for a calculator that requires two arguments.
     *
     * @param calc calculator for a derived sequence property
     */
    public static <A, B, R> ConfigInjector<R> create(ConfigInjectorCalc2<A, B, R> calc) {
        return new ConfigInjector<>(new Adapter2<>(calc));
    }

    /**
     * Creates a ConfigInjector for a calculator that requires three arguments.
     *
     * @param calc calculator for a derived sequence property
     */
    public static <A, B, C, R> ConfigInjector<R> create(ConfigInjectorCalc3<A, B, C, R> calc) {
        return new ConfigInjector<>(new Adapter3<>(calc));
    }

    /**
     * Creates a ConfigInjector for a calculator that requires four arguments.
     *
     * @param calc calculator for a derived sequence property
     */
    public static <A, B, C, D, R> ConfigInjector<R> create(ConfigInjectorCalc4<A, B, C, D, R> calc) {
        return new ConfigInjector<>(new Adapter4<>(calc));
    }


    private final CalcAdapter<R> adapter;

    private ConfigInjector(CalcAdapter<R> adapt) {
        adapter = adapt;
    }

    private static Object getValue(PropertyDescriptor pd, ISysConfig cur, ISysConfig prev) {
        return ConfigInjectorUtil.instance.getValue(pd, cur, prev);
    }

    private boolean propertyUpdated(ISysConfig cur, ISysConfig fullPrev) {
        for (PropertyDescriptor pd : adapter.list()) {
            if (ConfigInjectorUtil.instance.differs(pd, cur, fullPrev)) return true;
        }
        return false;
    }

    /**
     * Injects the observing wavelength into the current configuration, using
     * values from the previous and fully defined configuration for reference
     * to determine which values have changed and to supply values not defined
     * in the current config.
     *
     * @param cur current configuration containing values that have changed
     * since the last step
     *
     * @param fullPrev previous, fully defined configuration containing the
     * set of values for all parameters as they existed in the last step
     */
    public void inject(IConfig cur, IConfig fullPrev) {
        final ISysConfig curSys  = cur.getSysConfig(INSTRUMENT_CONFIG_NAME);
        final ISysConfig prevSys = fullPrev.getSysConfig(INSTRUMENT_CONFIG_NAME);

        final String propName = adapter.resultPropertyName();
        if (curSys != null) {
            curSys.removeParameter(propName); // may not exist, but if it does, remove it
        }
        if (propertyUpdated(curSys, prevSys)) {
            final R newValue = adapter.apply(curSys, prevSys);

            if (newValue != null) {
                final Object oldValue = ConfigInjectorUtil.instance.lookup(propName, prevSys);
                if ((oldValue == null) || !oldValue.equals(newValue)) {
                    cur.putParameter(INSTRUMENT_CONFIG_NAME,
                            DefaultParameter.getInstance(propName, newValue));
                }
            }
        }
    }
}
