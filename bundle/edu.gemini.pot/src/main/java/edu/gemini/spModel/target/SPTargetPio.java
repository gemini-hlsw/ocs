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

    // DateFormat is not synchronized so it must be kept private and accessed
    // through synchronized methods.
    private static final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static synchronized String formatDate(final Date date) {
        return formatter.format(date);
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
            try {

                // At some point during 16A the server started writing out times with o'clock in
                // them ... unclear why this happened. Workaround...
                return format.parse(dateStr.replace("o'clock ", ""));

            } catch (final ParseException ee) {
                LOGGER.log(Level.WARNING, " Invalid date found " + dateStr);
                return null;
            }
        }
    }

    public static ParamSet getParamSet(final SPTarget spt, final PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        paramSet.addParamSet(TargetParamSetCodecs.TargetParamSetCodec().encode(_TARGET, spt.getTarget()));
        return paramSet;
    }

    public static void setParamSet(final ParamSet paramSet, final SPTarget spt) {
        if (paramSet == null) return;
        final ParamSet ntps = paramSet.getParamSet(_TARGET);
        if (ntps != null) {
            final Target t = TargetParamSetCodecs.TargetParamSetCodec().decode(ntps).toOption().get();
            spt.setTarget(t);
        }
    }

    public static SPTarget fromParamSet(final ParamSet pset) {
        final SPTarget res = new SPTarget();
        res.setParamSet(pset);
        return res;
    }

}
