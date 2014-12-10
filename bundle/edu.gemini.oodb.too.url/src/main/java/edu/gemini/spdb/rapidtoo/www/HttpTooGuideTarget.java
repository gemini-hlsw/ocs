//
// $Id: HttpTooGuideTarget.java 325 2006-04-26 13:40:04Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spdb.rapidtoo.TooGuideTarget;

import javax.servlet.http.HttpServletRequest;

/**
 * A {@link edu.gemini.spdb.rapidtoo.TooGuideTarget} implementation based upon
 * HTTP request parameters.
 *
 * <ul>
 * <li>gstarget - name of the target</li>
 * <li>gsra - RA of the target specified in degrees or as HH:MM:SS (e.g.,
 * 03:02:58.940), J2000</li>
 * <li>gsdec - declination of the target specified in degrees or as DD:MM:SS
 * (e.g., 00:10:16.30), J2000</li>
 * <li>gsprobe - name of the guide star probe (e.g., PWFS1, PWFS2, OIWFS,
 * AOWFS)</li>
 * <li>gsmag - magnitude of the guide star</li>
 * </ul>
 */
public class HttpTooGuideTarget extends HttpTarget implements TooGuideTarget {

    public static final String TARGET_GUIDE_PROBE_PARAM = "probe";

    private TooGuideTarget.GuideProbe _probe;

    public HttpTooGuideTarget(HttpServletRequest req) throws BadRequestException {
        super("gs", "GS", req);
        String probeStr = getParameter(req, TARGET_GUIDE_PROBE_PARAM, null);
        try {
            _probe = TooGuideTarget.GuideProbe.valueOf(probeStr);
        } catch (Exception ex) {
            throw new BadRequestException("cannot parse the guide probe \"" + probeStr + "\"");
        }
    }

    public TooGuideTarget.GuideProbe getGuideProbe() {
        return _probe;
    }

}
