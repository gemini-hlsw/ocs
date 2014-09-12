//
// $Id: TooUpdateServlet.java 306 2006-03-16 15:40:03Z shane $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.sp.SPObservationID;
import edu.gemini.spdb.rapidtoo.TooUpdate;
import edu.gemini.spdb.rapidtoo.TooUpdateException;
import edu.gemini.spdb.rapidtoo.InactiveProgramException;
import edu.gemini.spdb.rapidtoo.impl.TooUpdateFunctor;
import edu.gemini.util.security.auth.keychain.KeyService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A servlet that receives update requests, creates a {@link TooUpdate} object,
 * and applies it to the indicated program.
 */
public class TooUpdateServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(TooUpdateServlet.class.getName());

    private final KeyService _ks;
    private final IDBDatabaseService _db;
    private final Set<Principal> _user;

    public TooUpdateServlet(IDBDatabaseService db, KeyService ks, Set<Principal> user) {
        this._ks = ks;
        this._db = db;
        this._user = user;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        _doTooUpdate(req, res);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        _doTooUpdate(req, res);
    }

    private void _doTooUpdate(HttpServletRequest req, HttpServletResponse res) throws IOException {
        TooUpdate update;
        try {
            update = new HttpTooUpdate(req);
        } catch (BadRequestException ex) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            return;
        }

        String obsIdStr = null;

        int code = 503;
        String error = "Database unavailable.";
        Exception fex = null;

        try {
            TooUpdateFunctor func = TooUpdateFunctor.update(_db, _ks, update, _user);
            SPObservationID obsId = func.getObservationId();
            if (obsId == null) {
                fex = func.getException();
            } else {
                obsIdStr = obsId.toString();
            }
        } catch (InactiveProgramException ex) {
            code = 403;
            error = "Program is not active.";
        } catch (TooUpdateException ex) {
            code = 404;
            error = ex.getMessage();
        }

        if (obsIdStr != null) {
            sendResponse(res, obsIdStr);
        } else if (fex != null) {
            LOG.log(Level.INFO, "Bad request", fex);
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, fex.getMessage());
        } else {
            // Just send the last error we encountered, if any.
            res.sendError(code, error);
        }
    }

    public static void sendResponse(HttpServletResponse res, String data) {

        res.setContentType("text/html");

        BufferedOutputStream bos = null;
        try {
            OutputStream out = res.getOutputStream();
            bos = new BufferedOutputStream(out);
            bos.write(data.getBytes());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Problem sending response", ex);
        } finally {
            try {
                if (bos != null) bos.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Problem closing response", ex);
            }
        }
    }
}
