package edu.gemini.spModel.timeacct;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioParseException;

import java.util.*;

/**
 * Utility for converting to/from ParamSets for {@link TimeAcctAllocation}s.
 */
public final class TimeAcctPio {
    public static final String TIME_ACCT_PARAM_SET = "timeAcct";

    private static final String ALLOC_PARAM_SET = "timeAcctAlloc";
    private static final String CATEGORY_PARAM  = "category";
    private static final String HOURS_PARAM = "hours";

    private TimeAcctPio() {
    }

    public static ParamSet getParamSet(final PioFactory factory, final TimeAcctAllocation allocation) {
        ParamSet pset = factory.createParamSet(TIME_ACCT_PARAM_SET);

        for (TimeAcctCategory cat : allocation.getCategories()) {
            Double hours = allocation.getHours(cat);
            if (hours == 0) continue;
            
            ParamSet ratioParamSet = factory.createParamSet(ALLOC_PARAM_SET);
            Pio.addParam(factory, ratioParamSet, CATEGORY_PARAM, cat.name());
            Pio.addDoubleParam(factory, ratioParamSet, HOURS_PARAM, hours);
            pset.addParamSet(ratioParamSet);
        }

        return pset;
    }

    public static TimeAcctAllocation getTimeAcctAllocation(ParamSet pset) throws PioParseException {

        List<ParamSet> lst = pset.getParamSets(ALLOC_PARAM_SET);
        if (lst == null) return TimeAcctAllocation.EMPTY;

        Map<TimeAcctCategory, Double> allocMap = new HashMap<TimeAcctCategory, Double>();
        for (ParamSet timeAcctRatioPset : lst) {
            TimeAcctCategory cat = getCategory(timeAcctRatioPset);
            Double hours = getHours(timeAcctRatioPset);
            allocMap.put(cat, hours);
        }

        return new TimeAcctAllocation(allocMap);
    }

    private static TimeAcctCategory getCategory(ParamSet pset) throws PioParseException {
        String name = Pio.getValue(pset, CATEGORY_PARAM, null);
        if (name == null) {
            throw new PioParseException("missing '" + CATEGORY_PARAM + "'");
        }

        TimeAcctCategory cat;
        try {
            cat = TimeAcctCategory.valueOf(name);
        } catch (Exception ex) {
            throw new PioParseException("unknown time accounting category '" + name + "'");
        }
        return cat;
    }

    public static double getHours(ParamSet pset) throws PioParseException {
        double hours = Pio.getDoubleValue(pset, HOURS_PARAM, -1.0);
        if (hours < 0) {
            throw new PioParseException("missing or illegal time accounting hours: " + hours);
        }
        return hours;
    }
}
