//
// $Id: HttpTooTarget.java 309 2006-03-17 12:21:09Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spdb.rapidtoo.TooTarget;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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
public abstract class HttpTarget implements TooTarget {

    public static final String TARGET_NAME_PARAM = "target";
    public static final String TARGET_RA_PARAM   = "ra";
    public static final String TARGET_DEC_PARAM  = "dec";
    public static final String TARGET_MAGS_PARAM = "mags";

    public static Set<String> allParams(final String prefix) {
        return Arrays.asList(
            TARGET_NAME_PARAM,
            TARGET_RA_PARAM,
            TARGET_DEC_PARAM,
            TARGET_MAGS_PARAM
        ).stream()
         .map(p -> prefix + p)
         .collect(Collectors.toCollection(() -> new HashSet<String>()));
    }

    private static Double parseRa(String val) throws BadRequestException {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            try {
                return Angle$.MODULE$.parseHMS(val).toOption().get().toDegrees();
            } catch (Exception ex2) {
                throw new BadRequestException("cannot parse the ra \"" + val + "\"");
            }
        }
    }

    private static Double parseDec(String val) throws BadRequestException {
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException ex) {
            try {
                return Angle$.MODULE$.parseDMS(val).toOption().get().toSignedDegrees();
            } catch (Exception ex2) {
                throw new BadRequestException("cannot parse the dec \"" +
                        val + "\"");
            }
        }
    }

    private final String _prefix;
    private final String _name;
    private final double _ra;
    private final double _dec;
    private final ImList<Magnitude> _mags;

    protected String getParameter(HttpServletRequest req, String key, String defaultValue)
            throws BadRequestException {
        final String param = _prefix + key;
        final String value = req.getParameter(param);
        if (value == null || value.trim().length() == 0) {
            if (defaultValue != null)
                return defaultValue;
            throw new BadRequestException("missing '" + param + "'");
        }
        return value;
    }

    /**
     * Constructs with the servlet request.
     *
     * @throws BadRequestException if the request parameters cannot be parsed
     * into a valid {@link HttpTooTarget}
     */
    protected HttpTarget(String prefix, String defaultName, HttpServletRequest req) throws BadRequestException {
        _prefix = prefix;
        _name   = getParameter(req, TARGET_NAME_PARAM, defaultName);
        _ra     = parseRa(getParameter(req, TARGET_RA_PARAM, null));
        _dec    = parseDec(getParameter(req, TARGET_DEC_PARAM, null));
        _mags   = new MagParser().unsafeParse(getParameter(req, TARGET_MAGS_PARAM, ""));
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

    public ImList<Magnitude> getMagnitudes() {
        return _mags;
    }


}
