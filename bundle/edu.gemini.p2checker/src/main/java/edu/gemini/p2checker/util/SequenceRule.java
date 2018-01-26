package edu.gemini.p2checker.util;

import edu.gemini.p2checker.api.*;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.config2.ItemEntry;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.pot.sp.ISPProgramNode;
import scala.Option;

import java.util.*;
import java.beans.PropertyDescriptor;

/**
 * A rule that applies a collection of {@link IConfigRule} to the entire
 * sequence produced by an observation.
 */
public class SequenceRule implements IRule {
    private static final String INSTRUMENT_PREFIX = "instrument:";
    private static final ItemKey P_OFFSET_KEY = new ItemKey("telescope:p");
    private static final ItemKey Q_OFFSET_KEY = new ItemKey("telescope:q");
    private static final ItemKey REPEAT_COUNT_KEY = new ItemKey(String.format("%s:%s", SeqConfigNames.OBSERVE_CONFIG_NAME, InstConstants.REPEAT_COUNT_PROP));
    /**
     * This is an {@link IConfigRule rule} in name only.  It can be added
     * to an instrument's list of rules in order to print each configuration.
     * It will never generate a problem.
     */
    public static IConfigRule DUMP_CONFIG_RULE = new AbstractConfigRule() {
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            StringBuilder buf = new StringBuilder();
            for (ItemEntry ie : config.itemEntries()) {
                Object val = ie.getItemValue();
                String classStr = (val == null) ? "" : val.getClass().getName();
                buf.append(ie.getKey()).append(" -> ").append(val).append("\t[").append(classStr).append("]\n");
            }
            return null;
        }
    };


    // Seems like there must be a better way ...  The problem is that property
    // types are primitives, "double.class", but the class of the object in the
    // sequence is the wrapper, "Double.class".  double.class.isInstance(obj)
    // returns false for objs of type Double.class.
    private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = new HashMap<>();

    // Matcher for science observations only.
    public static final IConfigMatcher SCIENCE_MATCHER = (config, step, elems) -> {
        ObsClass obsClass = getObsClass(config);
        return obsClass == ObsClass.SCIENCE;
    };

    // Matcher for science observations and nighttime calibrations.
    public static final IConfigMatcher SCIENCE_NIGHTTIME_CAL_MATCHER = (config, step, elems) -> {
        ObsClass obsClass = getObsClass(config);
        if (obsClass == null)
            return false;
        switch(obsClass) {
            case SCIENCE:
            case PARTNER_CAL:
            case PROG_CAL:
                return true;
            default:
                return false;
        }
    };


    static {
        PRIMITIVE_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_MAP.put(char.class, Character.class);
        PRIMITIVE_MAP.put(byte.class, Byte.class);
        PRIMITIVE_MAP.put(short.class, Short.class);
        PRIMITIVE_MAP.put(int.class, Integer.class);
        PRIMITIVE_MAP.put(long.class, Long.class);
        PRIMITIVE_MAP.put(float.class, Float.class);
        PRIMITIVE_MAP.put(double.class, Double.class);
    }
    private static Class<?> getWrapperClass(Class<?> primitiveClass) {
        return PRIMITIVE_MAP.get(primitiveClass);
    }

    private static Object getItem(Config config, Class<?> c, String strKey) {
        ItemKey key = new ItemKey(strKey);
        return getItem(config, c, key);
    }

    public static Object getItem(Config config, Class<?> c, ItemKey key) {
        Object obj = config.getItemValue(key);
        if (c.isPrimitive()) c = getWrapperClass(c);
        if ((c != null) && c.isInstance(obj)) return obj;
        return null;
    }

    public static Object getInstrumentItem(Config config, PropertyDescriptor desc) {
        return getItem(config, desc.getPropertyType(), INSTRUMENT_PREFIX + desc.getName());
    }

    private static final ItemKey OBS_CLASS_KEY = new ItemKey("observe:class");

    public static ObsClass getObsClass(Config config) {
        String obsClassStr = (String) getItem(config, String.class, OBS_CLASS_KEY);
        if (obsClassStr == null) return null;
        return ObsClass.parseType(obsClassStr);
    }

    public static Option<Double> getPOffset(final Config config) {
        return Option.apply(
                _getDoubleValue((String) SequenceRule.getItem(config, String.class, P_OFFSET_KEY))
        );
    }

    public static Option<Double> getQOffset(final Config config) {
        return Option.apply(
                _getDoubleValue((String) SequenceRule.getItem(config, String.class, Q_OFFSET_KEY))
        );
    }

    public static Integer getStepCount(Config config) {
        return (Integer) SequenceRule.getItem(config, Integer.class, REPEAT_COUNT_KEY);
    }

    private static final ItemKey EXPOSURE_TIME_KEY = new ItemKey("observe:exposureTime");
    private static final ItemKey OBSERVE_TYPE_KEY = new ItemKey("observe:observeType");

    /**
     * Return the exposure time for the given config step. Will try to
     * get it both as a Double and String. The result is always a double
     * and will return null in case nothing can be obtained from the config.
     */
    public static Double getExposureTime(Config config) {
        Double val = (Double)getItem(config, Double.class, EXPOSURE_TIME_KEY);
        if (val != null) return val;
        //attempt to read it as string
        String strVal = (String)getItem(config, String.class, EXPOSURE_TIME_KEY);
        return _getDoubleValue(strVal);
    }

    private static final ItemKey INSTRUMENT_EXPOSURE_TIME_KEY = new ItemKey(INSTRUMENT_PREFIX + "exposureTime");
    
    /**
     * Return the exposure time for the instrument. Will try to
     * get it both as a Double and String. The result is always a double
     * and will return null in case nothing can be obtained from the config.
     */
    public static Double getInstrumentExposureTime(Config config) {
        Double val = (Double)getItem(config, Double.class, INSTRUMENT_EXPOSURE_TIME_KEY);
        if (val != null) {
            return val;
        }
        //attempt to read it as string
        String strVal = (String)getItem(config, String.class, INSTRUMENT_EXPOSURE_TIME_KEY);
        return _getDoubleValue(strVal);
    }

    private static final ItemKey COADDS_KEY = new ItemKey("observe:coadds");

    public static Integer getCoadds(Config config) {

        Integer val = (Integer)getItem(config, Integer.class, COADDS_KEY);
        if (val != null) return val;
        //attempt to read it as string
        String strVal = (String)getItem(config, String.class, COADDS_KEY);
        if (strVal == null) return null;
        try {
            return Integer.parseInt(strVal);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getObserveType(Config config) {
        return (String) getItem(config, String.class, OBSERVE_TYPE_KEY);
    }

    private static Double _getDoubleValue(String val) {
        if (val == null) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Return the Instrument or the sequence node, making the decision based on whether this is
     * the first step or not.
     */
    public static ISPProgramNode getInstrumentOrSequenceNode(int step, ObservationElements elems) {
        return (step == 0) ? elems.getInstrumentNode() : elems.getSeqComponentNode();
    }

    /**
     * Return the Instrument or the sequence node, taking into account the configuration of the step.
     * If the configuration contains a DARK or a FLAT observation, the result will be the sequence node.
     * Otherwise, it will make the decision based on whether this is the first step or not.
     * <p/>
     * This method should be used when the node to be returned has to be defined by the
     * observe type in the first place, as stated previously
     */
    public static ISPProgramNode getInstrumentOrSequenceNode(int step, ObservationElements elems, Config config) {
        String obsType = getObserveType(config);
        if (InstConstants.FLAT_OBSERVE_TYPE.equals(obsType) || InstConstants.DARK_OBSERVE_TYPE.equals(obsType)) {
            return elems.getSeqComponentNode();
        }
        return getInstrumentOrSequenceNode(step, elems);
    }

    private Set<IConfigRule> _instRules;
    private Object _state;

    public SequenceRule(Collection<IConfigRule> instRules, Object state) {
        _instRules = new HashSet<>(instRules);
        _state = state;
    }

    public IP2Problems check(ObservationElements elements)  {
        IP2Problems probs = new P2Problems();

        // Walk through ever config in the sequence, checking each rule.  If
        // a rule matches, remove it from the set so it won't be reported twice.
        int step = 0;
        ConfigSequence seq = elements.getSequence();
        for (Iterator<Config> it=seq.iterator(); it.hasNext(); ++step) {
            Config config = it.next();

            Map<IConfigMatcher, Boolean> valMap = new HashMap<>();

            Iterator<IConfigRule> ruleIt = _instRules.iterator();
            while (ruleIt.hasNext()) {
                IConfigRule rule = ruleIt.next();
                IConfigMatcher matcher = rule.getMatcher();
                Boolean res;
                //this can't be simplified to  matcher == null || valMap.get(matcher);
                //since if valMap.get(matcher) is null it will throw a NullPointerException
                //noinspection SimplifiableIfStatement
                if (matcher == null) {
                    res = true;
                } else {
                    res = valMap.get(matcher);
                }

                if (res == null) {
                   res = matcher.matches(config, step, elements);
                   valMap.put(matcher, res);
                }
                if (res) {
                    // rules is applicable, do it
                    Problem prob = rule.check(config, step, elements, _state);
                    if (prob != null) {
                        probs.append(prob);
                        ruleIt.remove();
                    }
                }
            }
        }

        return probs;
    }
}
