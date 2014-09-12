//
// $Id: DatamanAdminServlet.java 295 2006-02-24 20:54:15Z shane $
//

package edu.gemini.dataman.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class DatamanAdminServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(DatamanAdminServlet.class.getName());

    private static final String COMMAND_PARAM = "cmd";

    private enum Command {
        shutdown() {
            void doAction(HttpServletRequest req, HttpServletResponse res, BundleContext ctx) {
                LOG.info("Shutdown request from admin servlet.");
                Bundle b = ctx.getBundle(0);
                try {
                    b.stop();
                } catch (BundleException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        },
        status() {
            void doAction(HttpServletRequest req, HttpServletResponse res, BundleContext ctx) {
                sendResponse(res, "ok");
            }
        };

        abstract void doAction(HttpServletRequest req, HttpServletResponse res, BundleContext ctx) throws ServletException;
    }

    private BundleContext _ctx;

    DatamanAdminServlet(BundleContext context) {
        _ctx = context;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        _doCommand(req, res);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        _doCommand(req, res);
    }

    private void _doCommand(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String cmdStr = req.getParameter(COMMAND_PARAM);
        if (cmdStr == null) {
            res.sendError(400, "mising '" + COMMAND_PARAM + "' parameter");
            return;
        }

        Command cmd = Command.valueOf(cmdStr);
        if (cmd == null) {
            res.sendError(400, "unrecognized command: " + cmd);
            return;
        }

        cmd.doAction(req, res, _ctx);
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
