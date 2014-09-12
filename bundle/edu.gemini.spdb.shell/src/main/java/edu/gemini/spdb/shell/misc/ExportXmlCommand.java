package edu.gemini.spdb.shell.misc;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpExportFunctor;
import edu.gemini.spModel.util.DBProgramInfo;
import edu.gemini.spModel.util.DBProgramListFunctor;

import java.io.File;
import java.io.FileOutputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Adapted from ExportXML in spModel-io.
 */
public final class ExportXmlCommand {
    private final IDBDatabaseService db;
    private final File path;
    private final Set<Principal> user;

    public ExportXmlCommand(final IDBDatabaseService db, final File path, Set<Principal> user) {
		super();
		this.db = db;
		this.path = path;
        this.user = user;
	}

    public void exportXML() {
    	_exportRoots(path, Collections.<SPProgramID>emptyList());
    }

    public void exportXML(List<SPProgramID> progIds) {
        _exportRoots(path, progIds);
    }

    // Export the given programs to the given dest dir.
    // If the progIds list is empty, all programs and plans are exported.
    private void _exportRoots(File dest, List<SPProgramID> progIds) {
        if (progIds.size() == 0) {
            final DBProgramListFunctor progFunc = db.getQueryRunner(user).queryPrograms(new DBProgramListFunctor());
            final List<DBProgramInfo> dbProgInfoList = progFunc.getList();
            for (DBProgramInfo pi : dbProgInfoList) _exportRootAsXml(dest, db.lookupProgram(pi.nodeKey));

            final DBProgramListFunctor planFunc = db.getQueryRunner(user).queryNightlyPlans(new DBProgramListFunctor());
            final List<DBProgramInfo> dbPlanInfoList = planFunc.getList();
            for (DBProgramInfo pi : dbPlanInfoList) _exportRootAsXml(dest, db.lookupNightlyPlan(pi.nodeKey));
        } else {
            for (SPProgramID id : progIds) {
                final ISPRootNode root = lookup(id);
                if (root != null) _exportRootAsXml(dest, root);
            }
        }
    }

    private final ISPRootNode lookup(SPProgramID id) {
        final ISPRootNode prog = db.lookupProgramByID(id);
        if (prog != null) return prog;

        final ISPRootNode plan = db.lookupNightlyRecordByID(id);
        if (plan != null) return plan;

        System.out.println(id + ": not found");
        return null;
    }

    private static final String fileName(ISPRootNode root) {
        final SPProgramID progId  = root.getProgramID();
        final SPNodeKey   progKey = root.getProgramKey();
        return (progId == null) ? progKey.toString() : progId.stringValue();
    }

    /**
     * Export an ISPProgram as an XML file.
     */
    private void _exportRootAsXml(File dest, ISPRootNode root) {
        final File file = new File(dest, fileName(root) + ".xml");
        System.out.println("Exporting to " + file);
        try {
            final SpExportFunctor functor = db.getQueryRunner(user).execute(new SpExportFunctor(), root);
            final String msg = functor.getProblem();
            final String xml = functor.getXmlProgram();
            if (msg != null || xml == null) {
                System.out.println("Error writing " + file + ": " + msg);
                return;
            }
            final FileOutputStream fOut = new FileOutputStream(file);
            try {
                fOut.write(xml.getBytes());
            } finally {
                fOut.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

