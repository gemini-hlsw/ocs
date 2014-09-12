package edu.gemini.spModel.io;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPRootNode;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBFunctor;

import scala.util.Failure;
import scala.util.Try;

import java.io.StringReader;
import java.security.Principal;
import java.util.Set;


/**
 * Imports a program from XML.  This is only useful for importing a program into
 * a remote database.  To import something locally, just use
 * {@link SpImportService}.
 */
public class SpImportFunctor extends DBAbstractFunctor implements IDBFunctor {
    private String _xml;

    private ISPRootNode _rootNode;
    private Throwable _problem;

    /**
     * Constructs with the XML program to import.
     * @param xml program to import
     */
    public SpImportFunctor(String xml) {
        if (xml == null) throw new NullPointerException();
        _xml = xml;
    }

    public void execute(IDBDatabaseService database, ISPNode node, Set<Principal> principals) {
        if (_xml == null) {
            _problem = new RuntimeException("There was no program to read.");
            return;
        }

        _problem = new RuntimeException("Disabled for now.");

        final SpImportService sis = new SpImportService(database);
        final Try<ISPRootNode> tryRoot = sis.importRootNodeXml(new StringReader(_xml), SpImportService.alwaysAnswer(SpImportService.Skip$.MODULE$));

        if (tryRoot.isFailure()) {
            _problem = ((Failure<ISPRootNode>) tryRoot).exception();
        } else {
            _rootNode = tryRoot.get();
        }
    }

    /**
     * Gets the root nod that was imported, assuming the import was successful,
     * otherwise returns <code>null</code>.
     */
    public ISPNode getRootNode() {
        return _rootNode;
    }

    /**
     * Gets the problem that was encountered while importing the program, if
     * any; otherwise returns <code>null</code>.
     */
    public Throwable getProblem() {
        return _problem;
    }
}
