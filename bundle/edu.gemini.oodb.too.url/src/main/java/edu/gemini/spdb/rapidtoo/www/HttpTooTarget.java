//
// $Id: HttpTooTarget.java 309 2006-03-17 12:21:09Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spdb.rapidtoo.TooTarget;

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
public class HttpTooTarget extends HttpTarget {

    /**
     * Constructs with the servlet request.
     *
     * @throws BadRequestException if the request parameters cannot be parsed
     * into a valid {@link HttpTooTarget}
     */
    public HttpTooTarget(HttpServletRequest req) throws BadRequestException {
        super("", null, req);
    }

}
