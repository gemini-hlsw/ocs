package edu.gemini.spModel.timeacct;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioParseException;

import java.time.Duration;
import java.util.*;

/**
 * Utility for converting to/from ParamSets for {@link TimeAcctAllocation}s.
 */
public final class TimeAcctPio {
    public static final String TIME_ACCT_PARAM_SET = "timeAcct";

    private static final String ALLOC_PARAM_SET = "timeAcctAlloc";
    private static final String CATEGORY_PARAM  = "category";
    private static final String PROGRAM_PARAM   = "program";
    private static final String PARTNER_PARAM   = "partner";

    private TimeAcctPio() {
    }

    public static ParamSet getParamSet(final PioFactory factory, final TimeAcctAllocation allocation) {
        final ParamSet pset = factory.createParamSet(TIME_ACCT_PARAM_SET);

        for (TimeAcctCategory cat : allocation.getCategories()) {
            final TimeAcctAward award = allocation.getAward(cat);
            if (award.isZero()) continue;

            final ParamSet allocParamSet = factory.createParamSet(ALLOC_PARAM_SET);
            Pio.addParam(    factory, allocParamSet, CATEGORY_PARAM, cat.name());
            Pio.addLongParam(factory, allocParamSet, PROGRAM_PARAM,  award.getProgramAward().toMillis());
            Pio.addLongParam(factory, allocParamSet, PARTNER_PARAM,  award.getPartnerAward().toMillis());
            pset.addParamSet(allocParamSet);
        }

        return pset;
    }

    public static TimeAcctAllocation getTimeAcctAllocation(ParamSet pset) throws PioParseException {

        final List<ParamSet> lst = pset.getParamSets(ALLOC_PARAM_SET);
        if (lst == null) return TimeAcctAllocation.EMPTY;

        final  Map<TimeAcctCategory, TimeAcctAward> allocMap = new HashMap<>();
        for (ParamSet awardPset : lst) {
            final TimeAcctCategory cat = getCategory(awardPset);
            final Duration     program = getDuration(awardPset, PROGRAM_PARAM);
            final Duration     partner = getDuration(awardPset, PARTNER_PARAM);
            final TimeAcctAward  award = new TimeAcctAward(program, partner);
            allocMap.put(cat, award);
        }

        return new TimeAcctAllocation(allocMap);
    }

    private static TimeAcctCategory getCategory(ParamSet pset) throws PioParseException {
        final String name = Pio.getValue(pset, CATEGORY_PARAM, null);
        if (name == null) {
            throw new PioParseException("missing '" + CATEGORY_PARAM + "'");
        }

        final TimeAcctCategory cat;
        try {
            cat = TimeAcctCategory.valueOf(name);
        } catch (Exception ex) {
            throw new PioParseException("unknown time accounting category '" + name + "'");
        }
        return cat;
    }

    public static Duration getDuration(ParamSet pset, String paramName) throws PioParseException {
        final long ms = Pio.getLongValue(pset, paramName, -1l);
        if (ms < 0) {
            throw new PioParseException("missing or illegal " + paramName + " time: " + ms);
        }
        return Duration.ofMillis(ms);
    }
}
