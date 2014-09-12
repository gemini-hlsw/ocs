//
// $Id: HttpTooTarget.java 309 2006-03-17 12:21:09Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spdb.rapidtoo.TooTarget;
import edu.gemini.spModel.target.system.HMS;
import edu.gemini.spModel.target.system.DMS;

import javax.servlet.http.HttpServletRequest;

/**
 * A {@link TooTarget} implementation based upon HTTP request parameters.
 *
 * <ul>
 * <li>target - name of the target</li>
 * <li>ra - RA of the target specified in degrees or as HH:MM:SS (e.g.,
 * 03:02:58.940), J2000</li>
 * <li>dec - declination of the target specified in degrees or as DD:MM:SS
 * (e.g., 00:10:16.30), J2000</li>
 * </ul>
 */
public class HttpTooTarget implements TooTarget {
    public static final String TARGET_NAME_PARAM = "target";
    public static final String TARGET_RA_PARAM   = "ra";
    public static final String TARGET_DEC_PARAM  = "dec";

    static Double parseRa(HttpServletRequest req, String paramName)
                                                   throws BadRequestException {
        String val = req.getParameter(paramName);
        if (val == null) return null;

        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            try {
                HMS hms = new HMS(val);
                return hms.getAs(HMS.Units.DEGREES);
            } catch (Exception ex2) {
                throw new BadRequestException("cannot parse the ra \"" +
                                          val + "\"");
            }
        }
    }

    static Double parseDec(HttpServletRequest req, String paramName)
                                                    throws BadRequestException {
        // Parse the dec.  Either specified as degrees or DD:MM:SS
        String val = req.getParameter(paramName);
        if (val == null) return null;

        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            try {
                DMS dms = new DMS(val);
                return dms.getAs(DMS.Units.DEGREES);
            } catch (Exception ex2) {
                throw new BadRequestException("cannot parse the dec \"" +
                                          val + "\"");
            }
        }
    }

    private String _name;
    private double _ra;
    private double _dec;

    /**
     * Constructs with the servlet request.
     *
     * @throws BadRequestException if the request parameters cannot be parsed
     * into a valid {@link HttpTooTarget}
     */
    public HttpTooTarget(HttpServletRequest req) throws BadRequestException {
        _name = req.getParameter(TARGET_NAME_PARAM);
        if (_name == null) {
            throw new BadRequestException("missing '" + TARGET_NAME_PARAM + "'");
        }
        _name = _name.trim();
        if ("".equals(_name)) {
            throw new BadRequestException("target name not specified");
        }

        Double ra = parseRa(req, TARGET_RA_PARAM);
        if (ra == null) {
            throw new BadRequestException("missing '" + TARGET_RA_PARAM + "'");
        }
        _ra = ra;

        Double dec = parseDec(req, TARGET_DEC_PARAM);
        if (dec == null) {
            throw new BadRequestException("missing '" + TARGET_DEC_PARAM + "'");
        }
        _dec = dec;
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
}
