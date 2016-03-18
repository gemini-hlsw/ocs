package edu.gemini.spModel.target;

import edu.gemini.spModel.core.Target;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SPTargetPio {

    private static final Logger LOGGER = Logger.getLogger(SPTarget.class.getName());

    public static final String PARAM_SET_NAME = "spTarget";

    private static final String _TARGET = "target";

    public static final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static synchronized Date parseDate(final String dateStr) {
        if (dateStr == null) return null;

        DateFormat format = formatter;
        if (!dateStr.contains("UTC")) {
            // OT-755: we didn't used to store the time zone, which
            // led to bugs when exporting in one time zone and importing
            // in another -- say when the program is stored.
            // If the date doesn't include "UTC", then assume it is in
            // the old style and import in the local time zone so that
            // at least the behavior when reading in existing programs for
            // the first time won't change.
            format = DateFormat.getInstance();
        }

        try {
            return format.parse(dateStr);
        } catch (final ParseException e) {
            LOGGER.log(Level.WARNING, " Invalid date found " + dateStr);
            return null;
        }
    }

    public static ParamSet getParamSet(final SPTarget spt, final PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        paramSet.addParamSet(TargetParamSetCodecs.TargetParamSetCodec().encode(_TARGET, spt.getNewTarget()));
        return paramSet;
    }

    public static void setParamSet(final ParamSet paramSet, final SPTarget spt) {
        if (paramSet == null) return;
        final ParamSet ntps = paramSet.getParamSet(_TARGET);
        if (ntps != null) {
            final Target t = TargetParamSetCodecs.TargetParamSetCodec().decode(ntps).toOption().get();
            spt.setNewTarget(t);
        }
    }

    public static SPTarget fromParamSet(final ParamSet pset) {
        final SPTarget res = new SPTarget();
        res.setParamSet(pset);
        return res;
    }

}
