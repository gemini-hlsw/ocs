//
// $Id: HttpTooUpdate.java 313 2006-03-27 17:35:02Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spdb.rapidtoo.*;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;

import java.time.Duration;
import javax.servlet.http.HttpServletRequest;

/**
 * A {@link TooUpdate} implementation based upon HTTP parameters.  See
 * {@link HttpTooIdentity} and {@link HttpTooTarget} for parameters related to
 * these items.
 *
 * <ul>
 * <li>posAngle - position angle in degrees</li>
 * <li>note - text of the note to add to the cloned template observation</li>
 * <li>ready - whether to mark the cloned template observation as ready
 * (e.g., <code>true</code> or <code>false</code>)</li>
 * </ul>
 */
public final class HttpTooUpdate implements TooUpdate {
    public static final String EXPOSURE_TIME_PARAM  = "exptime";
    public static final String POSITION_ANGLE_PARAM = "posangle";
    public static final String NOTE_PARAM           = "note";
    public static final String GROUP_PARAM          = "group";
    public static final String READY_PARAM          = "ready";

    public static final Duration MIN_EXPOSURE_TIME  = Duration.ofSeconds(1L);
    public static final Duration MAX_EXPOSURE_TIME  = Duration.ofSeconds(300L);

    private final TooIdentity _id;
    private final TooTarget _target;
    private final Option<TooGuideTarget> _guide;
    private final Double _posAngle;
    private final Option<Duration> _exposureTime;
    private final String _note;
    private final TooElevationConstraint _elevation;
    private final TooTimingWindow _timingWindow;
    private final String _group;
    private final boolean _isReady;

    /**
     * Constructs with the servlet request.
     *
     * @throws BadRequestException if the request parameters cannot be parsed
     * into a valid {@link TooUpdate}
     */
    public HttpTooUpdate(HttpServletRequest req) throws BadRequestException {
        _id     = new HttpTooIdentity(req);
        _target = new HttpTooTarget(req);
        _guide  = HttpTooGuideTarget.parse(req);

        double posAngle = 0.0;
        String val = req.getParameter(POSITION_ANGLE_PARAM);
        if (val != null) {
            try {
                posAngle = new Double(val);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("cannot parse position angle \"" +
                                     val + "\"");
            }
        }
        _posAngle = posAngle;


        Duration expTime = null;
        final String expTimeString = req.getParameter(EXPOSURE_TIME_PARAM);
        if (expTimeString != null) {

            try {
                expTime = Duration.ofSeconds(Integer.parseInt(expTimeString));
            } catch (NumberFormatException ex) {
                throw new BadRequestException("cannot parse " + EXPOSURE_TIME_PARAM + " value `" + expTimeString + "`");
            }

            if (expTime.compareTo(MIN_EXPOSURE_TIME) < 0) {
                throw new BadRequestException("minimum " + EXPOSURE_TIME_PARAM + " is " + MIN_EXPOSURE_TIME.getSeconds() + " sec");
            }

            if (expTime.compareTo(MAX_EXPOSURE_TIME) > 0) {
                throw new BadRequestException("maximum " + EXPOSURE_TIME_PARAM + " is " + MAX_EXPOSURE_TIME.getSeconds() + " sec");
            }

        }
        _exposureTime = ImOption.apply(expTime);

        _note  = req.getParameter(NOTE_PARAM);
        _elevation    = HttpTooElevationConstraint.parse(req);
        _timingWindow = HttpTooTimingWindow.parse(req);

        String group = req.getParameter(GROUP_PARAM);
        if (group != null) {
            group = group.trim();
            if ("".equals(group)) group = null;
        }
        _group = group;

        val = req.getParameter(READY_PARAM);
        boolean isReady = false;
        if (val != null) {
            isReady = Boolean.parseBoolean(val);
        }
        _isReady = isReady;
    }

    public TooIdentity getIdentity() {
        return _id;
    }

    public TooTarget getBasePosition() {
        return _target;
    }

    public Option<TooGuideTarget> getGuideStar() {
        return _guide;
    }

    public Double getPositionAngle() {
        return _posAngle;
    }

    public Option<Duration> getExposureTime() {
        return _exposureTime;
    }

    public String getNote() {
        return _note;
    }

    public TooElevationConstraint getElevationConstraint() {
        return _elevation;
    }

    public TooTimingWindow getTimingWindow() {
        return _timingWindow;
    }

    public String getGroup() {
        return _group;
    }

    public boolean isReady() {
        return _isReady;
    }
}
