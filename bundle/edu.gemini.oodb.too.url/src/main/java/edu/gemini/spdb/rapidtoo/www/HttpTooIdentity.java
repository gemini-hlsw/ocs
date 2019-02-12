//
// $Id: HttpTooIdentity.java 532 2006-08-25 21:23:34Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spdb.rapidtoo.TooIdentity;

import javax.servlet.http.HttpServletRequest;

/**
 * A {@link TooIdentity} implementation based upon HTTP request parameters.
 *
 * <ul>
 * <li>prog - program id (e.g., GS-2006A-Q-23)</li>
 * <li>password - program password (e.g., 77482)</li>
 * <li>obs - name of the template observation to update (e.g.,
 * &quot;K twilight X&quot;</li>
 * <li>obsnum - number of the template observation to update (e.g., 5)</li>
 * </ul>
 */
public final class HttpTooIdentity implements TooIdentity {
    public static final String PROGRAM_ID_PARAM = "prog";
    public static final String EMAIL_PARAM   = "email";
    public static final String PASSWORD_PARAM   = "password";
    public static final String OBS_NAME_PARAM   = "obs";
    public static final String OBS_NUMBER_PARAM = "obsnum";

    private SPProgramID _progId;
    private String _email;
    private String _password;
    private String _obsName;
    private int _obsNumber = -1;

    /**
     * Constructs with the servlet request.
     *
     * @throws BadRequestException if the request parameters cannot be
     * parsed into a valid {@link TooIdentity}
     */
    public HttpTooIdentity(HttpServletRequest req) throws BadRequestException {

        // Set the program id.
        String val = req.getParameter(PROGRAM_ID_PARAM);
        if (val == null) {
            throw new BadRequestException("missing \"" + PROGRAM_ID_PARAM +
                                          "\" param");
        }
        try {
            _progId = SPProgramID.toProgramID(val);
        } catch (SPBadIDException e) {
            throw new BadRequestException("illegal program id \"" +
                                          PROGRAM_ID_PARAM + "\"");
        }

        // Set the email address.
        _email = req.getParameter(EMAIL_PARAM);
        if (_email == null) {
            throw new BadRequestException("missing \"" + EMAIL_PARAM + "\"");
        }

        // Set the password.
        _password = req.getParameter(PASSWORD_PARAM);
        if (_password == null) {
            throw new BadRequestException("missing \"" + PASSWORD_PARAM + "\"");
        }

        // Set the template observation name and/or number
        _obsName = req.getParameter(OBS_NAME_PARAM);
        val = req.getParameter(OBS_NUMBER_PARAM);
        if (val == null) {
            if (_obsName == null) {
                throw new BadRequestException("one of \"" + OBS_NAME_PARAM +
                                  "\" or \"" +
                                   OBS_NUMBER_PARAM + "\" must be specified");
            }
        } else {
            try {
                _obsNumber = Integer.parseInt(val);
            } catch (Exception ex) {
                throw new BadRequestException("\"" + OBS_NUMBER_PARAM +
                                              "\" must be a positive integer");
            }
            if (_obsNumber <= 0) {
                throw new BadRequestException("\"" + OBS_NUMBER_PARAM +
                                              "\" must be a positive integer");
            }
        }
    }

    public SPProgramID getProgramId() {
        return _progId;
    }

    public String getPassword() {
        return _password;
    }

    public String getTemplateObsName() {
        return _obsName;
    }

    public int getTemplateObsNumber() {
        return _obsNumber;
    }

    @Override
    public String getEmail() {
        return _email;
    }
}
