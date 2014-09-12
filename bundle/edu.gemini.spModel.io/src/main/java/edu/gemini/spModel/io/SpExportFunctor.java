//
// $Id: SpExportFunctor.java 46733 2012-07-12 20:43:36Z rnorris $
//
package edu.gemini.spModel.io;

import java.io.StringWriter;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.spdb.IDBDatabaseService;

import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.spModel.io.impl.PioSpXmlWriter;

/**
 * Database functor used to create an XML string for a program without
 * requiring the client to perform a series of remote method calls.
 */
public class SpExportFunctor extends DBAbstractFunctor {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(SpExportFunctor.class.getName());

    private String _xml;
    private String _problem;

    /**
     * Constructs an SpExportFunctor with no export control.
     */
    public SpExportFunctor() {
    }

    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        StringWriter writer = new StringWriter();
        PioSpXmlWriter outXml = new PioSpXmlWriter(writer);
        try {
            outXml.printDocument(node);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Problem exporting program.", ex);
            _problem = "internal error exporting program, please check database logs";
            return;
        }

        _problem = outXml.getProblemDescription();
        _xml = writer.toString();
    }

    /**
     * Gets the program document that was exported, assuming the export was
     * successful, otherwise returns <code>null</code>.
     */
    public String getXmlProgram() {
        return _xml;
    }

    /**
     * Gets the problem that was encountered while exporting the program, if
     * any; otherwise returns <code>null</code>.
     */
    public String getProblem() {
        return _problem;
    }
}
