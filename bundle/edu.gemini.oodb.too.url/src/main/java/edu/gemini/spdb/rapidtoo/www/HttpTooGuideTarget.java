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
public class HttpTooGuideTarget implements TooGuideTarget {
    public static final String TARGET_NAME_PARAM = "gstarget";
    public static final String TARGET_RA_PARAM   = "gsra";
    public static final String TARGET_DEC_PARAM  = "gsdec";

    public static final String TARGET_GUIDE_PROBE_PARAM = "gsprobe";
    public static final String TARGET_MAGNITUDE_PARAM   = "gsmag";

    /**
     * Creates an HttpTooGuideTarget, if the appropriate parameters are in the
     * supplied request.  Guide star specifications are required (including the
     * probe, ra, and dec). If the name is missing, it will default to "GS".
     * If the magnitude is missing, it will be ignored.
     *
     * @return the corresponding HttpTooGuideTarget
     *
     * @throws edu.gemini.spdb.rapidtoo.www.BadRequestException if the request
     * parameters cannot be parsed into a valid
     * {@link edu.gemini.spdb.rapidtoo.www.HttpTooGuideTarget}
     */
    public static HttpTooGuideTarget create(HttpServletRequest req)
            throws BadRequestException {
        Double ra  = HttpTooTarget.parseRa(req, TARGET_RA_PARAM);
        if (ra == null) {
            throw new BadRequestException("missing '" + TARGET_RA_PARAM + "'");
        }
        Double dec = HttpTooTarget.parseDec(req, TARGET_DEC_PARAM);
        if (dec == null) {
            throw new BadRequestException("missing '" + TARGET_DEC_PARAM + "'");
        }

        String probeStr = req.getParameter(TARGET_GUIDE_PROBE_PARAM);
        if (probeStr == null) {
            throw new BadRequestException("missing '" + TARGET_GUIDE_PROBE_PARAM + "'");
        }
        TooGuideTarget.GuideProbe probe;
        try {
            probe = TooGuideTarget.GuideProbe.valueOf(probeStr);
        } catch (Exception ex) {
            throw new BadRequestException("cannot parse the guide probe \"" +
                                           probeStr + "\"");
        }

        String name = req.getParameter(TARGET_NAME_PARAM);
        if (name == null) name = "GS";  // default to GS
        name = name.trim();
        if ("".equals(name)) name = "GS";

        String mag  = req.getParameter(TARGET_MAGNITUDE_PARAM);

        return new HttpTooGuideTarget(probe, name, ra, dec, mag);
    }

    private String _name;
    private double _ra;
    private double _dec;
    private String _mag;
    private TooGuideTarget.GuideProbe _probe;

    /**
     * Constructs with the servlet request.
     *
     */
    private HttpTooGuideTarget(TooGuideTarget.GuideProbe probe, String name,
                               double ra, double dec, String mag) {
        _probe = probe;
        _name  = name;
        _ra    = ra;
        _dec   = dec;
        _mag   = mag;
    }

    public String getName() {
        return _name;
    }

    public double getRa() {
        return _ra;
    }

    public double getDec() {
        return _dec;
    }

    public TooGuideTarget.GuideProbe getGuideProbe() {
        return _probe;
    }

    public String getMagnitude() {
        return _mag;
    }
}
