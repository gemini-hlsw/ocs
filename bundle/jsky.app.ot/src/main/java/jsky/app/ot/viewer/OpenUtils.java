// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: OpenUtils.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpExportFunctor;
import edu.gemini.spModel.util.*;
import jsky.app.ot.OT;
import jsky.app.ot.vcs.VcsOtClient;
import jsky.app.ot.viewer.open.OpenDialog$;
import jsky.util.Preferences;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.ExampleFileFilter;
import jsky.util.gui.GridBagUtil;

import javax.swing.*;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class providing methods for handling import/output of
 * various Science Programs from files and databases.
 * <p/>
 * Methods for importing, exporting, and parsing of XML Science
 * Programs are provided.  Methods for opening the database are
 * provided.
 * <p/>
 * This class is meant to be part of the JSky OT user interface, thus
 * it provides file opening capabilities using JFileChooser and uses
 * the DialogUtil for posting errors or problems for the user.
 *
 * @author Kim Gillies, Allan Brighton
 */
public final class OpenUtils {

    // Values for the dialogType parameter to the getDatabaseEntries() method
    private static final int OPEN = 0;
    private static final int REMOVE = 1;
    private static final int EXPORT = 2;

    // Preferences keys used to remember the user's selected dirs for XML import/export
    private static final String IMPORT_DIR_KEY = OpenUtils.class.getName() + ".importDir";
    private static final String EXPORT_DIR_KEY = OpenUtils.class.getName() + ".exportDir";

    // Import and export file choosers
    private static final JFileChooser _importFileChooser = _makeFileChooser(IMPORT_DIR_KEY);
    private static final JFileChooser _exportFileChooser = _makeFileChooser(EXPORT_DIR_KEY);

    // JFileChooser Accessory panels to add options to the import/export dialogs
    private static XMLExportAccessoryPanel _xmlExportAccessoryPanel;
    private static XMLImportAccessoryPanel _xmlImportAccessoryPanel;

    // Construct a file chooser with an "xml" file filter, initialized to the directory indicated by the provided
    // preference key.
    private static JFileChooser _makeFileChooser(final String key) {
        final JFileChooser fc = new JFileChooser(new File("."));
        final ExampleFileFilter spFilter = new ExampleFileFilter(new String[]{"xml"}, "Science Program");
        fc.addChoosableFileFilter(spFilter);
        fc.setFileFilter(spFilter);
        final String dir = Preferences.get(key);
        if (dir != null)
            fc.setCurrentDirectory(new File(dir));
        return fc;
    }

    /**
     * Uses the JFileChooser to select one or more XML-based science programs to import.
     * @param parent the parent component for the file chooser (optional)
     * @return an array of selected files or null if the user cancelled.
     */
    public static File[] getXmlFiles(final Component parent) {
        final JFileChooser fc = _importFileChooser;
        final XMLImportAccessoryPanel panel = _getXMLImportAccessoryPanel();
        fc.setAccessory(panel);
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Preferences.set(IMPORT_DIR_KEY, fc.getCurrentDirectory().toString());
            if (panel.bulkImportButton.isSelected()) {
                return fc.getSelectedFile().listFiles(new FileFilter() {
                    public boolean accept(final File pathname) {
                        return pathname.getName().endsWith(".xml");
                    }
                });
            } else {
                return fc.getSelectedFiles();
            }
        }
        return null;
    }

    /**
     * Uses the JFileChooser to select a place to save an XML science program or plan.
     * @param root   the science program or nightly plan to be saved (may be null)
     * @param parent optional parent component for the file chooser
     * @return a selected File or null if the user cancelled.
     */
    private static File getSaveAsFile(final ISPNode root, final Component parent) {
        // Generate a default filename from the program reference
        File defaultFile = null;
        if (root != null) {
            final SPProgramID progId = root.getProgramID();
            if (progId != null) {
                final String progRef = progId.stringValue();
                defaultFile = new File(progRef + ".xml");
            }
        }

        final JFileChooser fc = _exportFileChooser;
        if (defaultFile != null) {
            fc.setSelectedFile(defaultFile);
        }
        fc.setMultiSelectionEnabled(false);

        final XMLExportAccessoryPanel panel = _getXMLExportAccessoryPanel();
        if (root == null) {
            panel.exportProgButton.setEnabled(false);
            panel.bulkExportButton.doClick();
        } else {
            panel.exportProgButton.setEnabled(true);
            panel.exportProgButton.doClick();
        }
        fc.setAccessory(panel);

        final int option = fc.showSaveDialog(parent);
        if (option != JFileChooser.APPROVE_OPTION) return null;

        final File file = fc.getSelectedFile();
        if (file == null) return null;  // the user canceled

        // remember the selected dir for the next session
        Preferences.set(EXPORT_DIR_KEY, fc.getCurrentDirectory().toString());

        if (checkOverwrite(file)) return file;
        return null;

    }

    /**
     * Get confirmation if a file needs to be overwritten and return true
     * if the operation should proceed.
     */
    public static boolean checkOverwrite(File file) {
        if (!file.isDirectory()) {
            final String fileName = file.getPath();

            // make sure the file has the .xml suffix
            if (!fileName.endsWith(".xml")) {
                file = new File(fileName + ".xml");
            }

            if (file.exists()) {
                final int ans = DialogUtil.confirm("The file " + file.getName()
                        + " already exists. Do you want to overwrite it?");
                if (ans != JOptionPane.YES_OPTION) return false;
            }
        }
        return true;
    }


    /**
     * Export an ISPProgram as an XML file.
     * @return true if the operation completed successfully
     */
    private static void exportProgAsXml(final ISPNode root, final String xmlFileName) {
        if (root == null || xmlFileName == null) return;

        try {
            final IDBDatabaseService db = SPDB.get();
            SpExportFunctor functor = new SpExportFunctor();
            // be sure to assign functor to the result here since a copy is returned with the results
            functor = db.getQueryRunner(OT.getUser()).execute(functor, root);
            final String msg = functor.getProblem();
            final String xml = functor.getXmlProgram();
            if (msg != null || xml == null) {
                DialogUtil.error("Error exporting " + xmlFileName + ": " + msg);
                return;
            }
            final FileOutputStream fout;
            fout = new FileOutputStream(xmlFileName);
            try {
                fout.write(xml.getBytes(Charset.forName("UTF-8")));
            } finally {
                fout.close();
            }
        } catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /**
     * Export an ISPProgram as an XML file.
     * @param root        the science program or nightly plan (may be null ir filename is null)
     * @param xmlFileName if xmlFileName is null, the chooser is used.
     * @param parent      the parent component for the file chooser (optional)
     */
    public static void exportProgAsXml(final ISPNode root, final String xmlFileName, final Component parent) {
        if (xmlFileName != null && root != null) {
            exportProgAsXml(root, xmlFileName);
        }

        File xmlFile = getSaveAsFile(root, parent);
        if (xmlFile == null) return;

        if (root == null || xmlFile.isDirectory()) {
            if (!xmlFile.isDirectory()) {
                xmlFile = xmlFile.getParentFile();
            }
            exportSelectedProgsAsXml(xmlFile, parent);
        } else {
            exportProgAsXml(root, xmlFile.getPath());
        }
    }


    /**
     * Export the selected science programs from the database to XML files in the
     * given directory.
     * @param dir    the directory in which to save the science programs
     * @param parent the parent component for the file chooser (optional)
     */
    private static void exportSelectedProgsAsXml(final File dir, final Component parent) {
        if (dir == null) return;

        final IDBDatabaseService db = SPDB.get();
        if (db == null) {
            DialogUtil.error("Failed to get a database.");
            return;
        }

        final List list = getDatabaseEntries(db, EXPORT);
        if (list == null || list.size() == 0) return;

        final ExportWorker worker = new ExportWorker(list, dir, parent);
        worker.start();
    }


    /**
     * Popup a dialog to choose one or more science programs or nightly plans
     * from the local database.
     * @param db         the database object to use
     * @param dialogType determines the type of dialog used: one or OPEN, REMOVE, EXPORT
     * @return a list of DBProgramInfo objects for the selected science programs
     */
    private static List<DBProgramInfo> getDatabaseEntries(final IDBDatabaseService db, final int dialogType) {

        // Open the chooser with the list of programs
        if (dialogType == OPEN) {
            return OpenDialog$.MODULE$.open(
                    db,
                    OT.getKeyChain(),
                    VcsOtClient.unsafeGetRegistrar(),
                    (JComponent) null);
        }

        final DBProgramChooser chooser;
        final DBProgramChooserFilter filter = new DBProgramChooserFilter(DBProgramChooserFilter.Mode.localOnly);
        if (dialogType == REMOVE) {
            chooser = DBProgramChooser.getRemoveDialog(filter);
        } else if (dialogType == EXPORT) {
            chooser = DBProgramChooser.getExportDialog(filter);
        } else {
            return null;
        }

        // Waits until cancel or okay
        chooser.refresh();
        chooser.show();
        return chooser.getSelectedPrograms();
    }

    /**
     * Returns a list of the program names and ids contained in the specified database.
     * @return a list of DBProgramInfo objects
     */
    public static List<DBProgramInfo> getProgramList(final IDBDatabaseService db) {
        try {
            final IDBQueryRunner runner = db.getQueryRunner(OT.getUser());
            return runner.queryPrograms(new DBProgramListFunctor()).getList();
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogUtil.message("The database query failed.");
            return new ArrayList<DBProgramInfo>();
        }
    }

    /**
     * Open a new science program from the database.
     */
    public static ISPProgram openDBProgram(final IDBDatabaseService db, final SPNodeKey key) {
        if ((key == null) || (db == null)) return null;

        final ISPProgram prog;
        prog = db.lookupProgram(key);

        if (prog == null) {
            DialogUtil.error("No science program found for id: " + key);
            return null;
        }

        return prog;
    }

    /**
     * Display a dialog for opening one or more science programs from the database.
     */
    public static ISPProgram[] openDBPrograms() {
        final IDBDatabaseService db = SPDB.get();
        if (db == null) {
            DialogUtil.error("Failed to get a database.");
            return null;
        }

        final List<DBProgramInfo> list = getDatabaseEntries(db, OPEN);
        if (list == null) return null;
        final int n = list.size();
        if (n == 0) return null;

        final ISPProgram[] progs = new ISPProgram[n];
        for (int i = 0; i < n; i++) {
            final DBProgramInfo pi = list.get(i);
            final SPNodeKey key = pi.nodeKey;
            if (pi.isNightlyPlan()) {
                throw new RuntimeException("Nightly plans are not opened in the OT.");
            } else {
                progs[i] = openDBProgram(db, key);
            }
        }
        return progs;
    }

    // Return a JFileChooser accessory panel with options to save one or all
    // science programs to XML.
    private static XMLExportAccessoryPanel _getXMLExportAccessoryPanel() {
        if (_xmlExportAccessoryPanel == null) {
            _xmlExportAccessoryPanel = new XMLExportAccessoryPanel(_exportFileChooser);
        }
        return _xmlExportAccessoryPanel;
    }


    // Return a JFileChooser accessory panel with options to import one or more
    // science programs from XML files.
    private static XMLImportAccessoryPanel _getXMLImportAccessoryPanel() {
        if (_xmlImportAccessoryPanel == null) {
            _xmlImportAccessoryPanel = new XMLImportAccessoryPanel(_importFileChooser);
        }
        return _xmlImportAccessoryPanel;
    }


    /**
     * A JFileChooser accessory to add an option to the export dialog
     * to export the entire program database, or selected science programs in a directory.
     */
    private static class XMLExportAccessoryPanel extends JPanel {

        // file chooser dialog
        final JFileChooser _fileChooser;
        final JLabel label;
        final JRadioButton exportProgButton;
        final JRadioButton bulkExportButton;

        public XMLExportAccessoryPanel(final JFileChooser fileChooser) {
            _fileChooser = fileChooser;
            label = new JLabel("Options:");
            exportProgButton = new JRadioButton("Export Current Science Program");
            bulkExportButton = new JRadioButton("Bulk Export to a Selected Directory");

            final ButtonGroup group = new ButtonGroup();
            group.add(exportProgButton);
            group.add(bulkExportButton);
            exportProgButton.setSelected(true);

            exportProgButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    _fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                }
            });

            bulkExportButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    _fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
            });

            final GridBagUtil layout = new GridBagUtil(this);
            layout.add(label, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NONE, GridBagConstraints.WEST, new Insets(11, 11, 0, 0), 0, 0);
            layout.add(exportProgButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NONE, GridBagConstraints.WEST, new Insets(11, 11, 0, 0), 0, 0);
            layout.add(bulkExportButton, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NONE, GridBagConstraints.WEST, new Insets(11, 11, 0, 0), 0, 0);
        }
    }

    /**
     * A JFileChooser accessory to add an option to the import dialog
     * to import the contents of an entire directory, or selected science programs
     * in a directory.
     */
    private static class XMLImportAccessoryPanel extends JPanel {

        // file chooser dialog
        final JFileChooser _fileChooser;
        final JLabel label;
        final JRadioButton importProgButton;
        final JRadioButton bulkImportButton;

        public XMLImportAccessoryPanel(final JFileChooser fileChooser) {
            _fileChooser = fileChooser;
            label = new JLabel("Options:");
            importProgButton = new JRadioButton("Import selected Science Program XML Files");
            bulkImportButton = new JRadioButton("Bulk Import from a Selected Directory");

            final ButtonGroup group = new ButtonGroup();
            group.add(importProgButton);
            group.add(bulkImportButton);

            importProgButton.setSelected(true);
            _fileChooser.setMultiSelectionEnabled(true);
            _fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            importProgButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    _fileChooser.setMultiSelectionEnabled(true);
                    _fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                }
            });

            bulkImportButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    _fileChooser.setMultiSelectionEnabled(false);
                    _fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                }
            });

            final GridBagUtil layout = new GridBagUtil(this);
            layout.add(label, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NONE, GridBagConstraints.WEST, new Insets(11, 11, 0, 0), 0, 0);
            layout.add(importProgButton, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NONE, GridBagConstraints.WEST, new Insets(11, 11, 0, 0), 0, 0);
            layout.add(bulkImportButton, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NONE, GridBagConstraints.WEST, new Insets(11, 11, 0, 0), 0, 0);
        }
    }
}

