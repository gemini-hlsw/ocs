//
// $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spdb.rapidtoo.TooTimingWindow;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A {@link TooTimingWindow} implementation based upon HTTP parameters.
 *
 * <ul>
 * <li>windowDate - start date of the timing window in the format YYYY-MM-DD</li>
 * <li>windowTime - start time of the timing window in the format HH:MM</li>
 * <li>windowDuration - time (in hours) that the timing window should last</li>
 * </ul>
 */
public class HttpTooTimingWindow implements TooTimingWindow {
    public static final String START_DATE_PARAM = "windowDate";
    public static final String START_TIME_PARAM = "windowTime";
    public static final String DURATION_PARAM   = "windowDuration";

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
//    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final String FORMAT = "yyyy-MM-dd HH:mm";


    public static HttpTooTimingWindow parse(HttpServletRequest req) throws BadRequestException {
        String dateStr = req.getParameter(START_DATE_PARAM);
        String timeStr = req.getParameter(START_TIME_PARAM);
        String durationStr = req.getParameter(DURATION_PARAM);

        if ((dateStr == null) && (timeStr == null) && (durationStr == null)) {
            return null;
        } else if ((dateStr == null) || (timeStr == null) || (durationStr == null)) {
            throw new BadRequestException("all timing window parameters must be specified if one is specified");
        }

        // ready to parse what we were given
        try {
            DateFormat format = new SimpleDateFormat(FORMAT);
            format.setTimeZone(UTC);
            Date start = format.parse(String.format("%s %s", dateStr, timeStr));
            long duration = Integer.parseInt(durationStr) * 60l * 60 * 1000;
            if (duration < 0) {
                throw new BadRequestException("timing window duration should be a positive integer");
            }
            return new HttpTooTimingWindow(start, duration);
        } catch (ParseException ex) {
            throw new BadRequestException("timing window date format is YYYY-MM-DD and time format is HH:mm");
        } catch (NumberFormatException ex) {
            throw new BadRequestException("timing window duration should be a positive integer");
        }
    }


    private final Date _start;
    private final long _duration;


    public HttpTooTimingWindow(Date start, long duration) {
        _start = start;
        _duration = duration;
    }

    public Date getDate() {
        return new Date(_start.getTime());
    }

    public long getDuration() {
        return _duration;
    }
}
