//
// $Id: MailTemplate.java 5677 2004-12-09 14:59:48Z anunez $
//
package edu.gemini.shared.mail;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An email template, which can be used to substitute variables of the form
 * <code>@VARIABLE_NAME@</code> for their corresponding values.  A few
 * "standard" variables are known by the Template class itself, and can be
 * obtained with their current value by calling {@link #addPluralProperties}
 * method.
 */
public class MailTemplate {
    /** Current date formatted for the platform's default locale. */
//    public static final String DATE_VAR = "DATE";

    /** Current time formatted for the platform's default locale. */
//    public static final String TIME_VAR = "TIME";

    /** Current date and time formated for the platform's default locale. */
//    public static final String DATE_TIME_VAR = "DATE_TIME";

    /** "is" or "are" */
    public static final String IS_ARE_VAR = "IS_ARE";

    /** "was" or "were" */
    public static final String WAS_WERE_VAR = "WAS_WERE";

    /** "have" or "has" */
    public static final String HAS_HAVE_VAR = "HAS_HAVE";

    /** "s" or "" */
    public static final String S_OR_EMPTY_VAR = "S_OR_EMPTY";

    private String _tmplText;

//    private static final DateFormat _dateFormatInstance     = DateFormat.getDateInstance();
//    private static final DateFormat _timeFormatInstance     = DateFormat.getTimeInstance();
//    private static final DateFormat _dateTimeFormatInstance = DateFormat.getDateTimeInstance();

    public MailTemplate(String tmplText) {
        _tmplText = tmplText;
    }

//    public static void addDateProperties(Properties props, Calendar cal) {
//        Date date = new Date();
//        String dateStr     = _dateFormatInstance.format(date);
//        String timeStr     = _timeFormatInstance.format(date);
//        String dateAndTime = _dateTimeFormatInstance.format(date);
//
//        props.put(DATE_VAR, dateStr);
//        props.put(TIME_VAR, timeStr);
//        props.put(DATE_TIME_VAR, dateAndTime);
//    }

    /**
     * Adds values for all the singular vs plural properties.  For example,
     * "IS_ARE" and "WAS_WERE".
     *
     * @param props the Properties object to which the properties will be
     * added
     * @param isPlural if <code>true</code> choose values appropriate for
     * a plural subject; otherwise values appropriate for a singular subject
     */
    public static void addPluralProperties(Properties props, boolean isPlural) {
        if (isPlural) {
            props.put(IS_ARE_VAR, "are");
            props.put(WAS_WERE_VAR, "were");
            props.put(S_OR_EMPTY_VAR, "s");
            props.put(HAS_HAVE_VAR, "have");
        } else {
            props.put(IS_ARE_VAR, "is");
            props.put(WAS_WERE_VAR, "was");
            props.put(S_OR_EMPTY_VAR, "");
            props.put(HAS_HAVE_VAR, "has");
        }
    }

    public String subsitute(Properties props) {
        StringBuffer buf = new StringBuffer();

        Pattern pat = Pattern.compile("@([A-Z_]*)@");
        Matcher mat = pat.matcher(_tmplText);

        for (boolean notDone = mat.find(); notDone; notDone = mat.find()) {
            String var = mat.group(1);
            String val = props.getProperty(var);
            //TODO: Escape dollar signs, using something like .replaceAll("\\$","\\\\\\$");
            if (val == null) {
                // Just write the variable back in.
                val = "@" + var + "@";
            }
            mat.appendReplacement(buf, val);
        }
        mat.appendTail(buf);

        return buf.toString();
    }
}
