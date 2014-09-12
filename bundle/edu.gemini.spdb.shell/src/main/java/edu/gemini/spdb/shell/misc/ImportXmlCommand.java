package edu.gemini.spdb.shell.misc;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpImportService;
import edu.gemini.spdb.shell.migrate.Migrate2014B;
import scala.util.Failure;
import scala.util.Try;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.security.Principal;

/**
 * Adapted from ImportXML in spModel-io
 */
public class ImportXmlCommand {

    private final IDBDatabaseService database;
    private final SpImportService imp;
    private final SpImportService.ImportDirective impDirective;
    private final File path;

    public ImportXmlCommand(IDBDatabaseService database, File path, SpImportService.ImportDirective impDirective) {
        if (impDirective == null) impDirective = SpImportService.Skip$.MODULE$;
        this.database = database;
        this.imp  = new SpImportService(database);
		this.path = path;
        this.impDirective = impDirective;
	}

    private List<File> filesToImport(List filesAndDirs) {
        final List<File> res = new ArrayList<File>();
        for (Iterator it=filesAndDirs.iterator(); it.hasNext(); ) {
            final File input = (File) it.next();
            if (!input.isDirectory()) {
                res.add(input);
            } else {
                final File[] lst = input.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".xml");
                    }
                });
                for (int i=0; i<lst.length; ++i) res.add(lst[i]);
            }
        }
        Collections.sort(res);
        return res;
    }

    // Import the given XML files or directories containing XML files.
    // The argument is a list of File objects.
    private void importFiles(List<File> files) {
        final List<File> inputFiles = filesToImport(files);
        System.out.println("Importing " + inputFiles.size() + " file(s).");
        for (File f : inputFiles) importFile(f);
    }

    public void importXML() {
    	importFiles(Collections.singletonList(path));
    }

    private class Dup implements SpImportService.DuplicateQuery<ISPRootNode> {
        private boolean duplicate = false;
        public SpImportService.ImportDirective ask(ISPRootNode im, ISPRootNode ex) {
            duplicate = true;
            return impDirective;
        }

        String importAction() {
            return duplicate ? impDirective.toString() : "Add";
        }
    }

    // Import the given XML file.
    private void importFile(File file) {
        System.out.println("Importing: " + file.getName() + " ...");
        try {
            final String xml = Charset.forName("UTF-8")
                    .decode(new FileInputStream(file).getChannel().map(MapMode.READ_ONLY, 0, file.length()))
                    .toString();

            final Dup dup = new Dup();
            final Try<ISPRootNode> t = imp.importRootNodeXml(new StringReader(xml), dup);
            if (t.isFailure()) throw ((Failure<ISPRootNode>) t).exception();

            final ISPRootNode root = t.get();
            if (root instanceof ISPProgram) {
                Migrate2014B.migrateOne(database, (ISPProgram) root, Collections.<Principal>emptySet());
            }

            final SPProgramID pid = root.getProgramID();
            final String idStr = pid == null ? root.getProgramKey().toString() : pid.stringValue();
            System.out.println(String.format("=> %s: %s", idStr, dup.importAction()));
        } catch (Throwable e) {
            e.printStackTrace();
            System.out.println("Import failed for: " + file + ": " + e.toString());
        }
    }
}