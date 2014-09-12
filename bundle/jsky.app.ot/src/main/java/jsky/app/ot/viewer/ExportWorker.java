// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ExportWorker.java 47001 2012-07-26 19:40:02Z swalker $
//
package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.util.DBProgramInfo;
import edu.gemini.spModel.io.SpExportFunctor;
import jsky.app.ot.OT;
import jsky.util.gui.SwingWorker;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


/**
 * A utility class for exporting science programs to XML files in a background thread, while
 * displaying a progress bar.
 */
class ExportWorker extends SwingWorker {

    // Duplicates dialog button indexes
    private static final int OVERWRITE = 0;
    private static final int NO_OVERWRITE = 1;
    private static final int CANCEL = 2;

    // Button labels
    private static final String OVERWRITE_MSG = "Overwrite";
    private static final String NO_OVERWRITE_MSG = "Don't Overwrite";
    private static final String CANCEL_MSG = "Cancel";

    // Displayed while the files are being exported
    private ProgressMonitor _progressMonitor;

    // list of DBProgramInfo objects describing the programs to export
    private List _list;

    // The directory name to hold the XML files
    private File _dir;

    // The parent component for popup dialogs
    private Component _parent;

    // Return value from checkOverwrite in event dispatching thread: One of
    // (OVERWRITE, NO_OVERWRITE, or CANCEL).
    int _overwrite;

    // If true, apply the value of _overwriteOk rather than popping up multiple
    // confirm overwrite dialogs
    private boolean _applyAll;

    // The last choice made in the overwrite dialog. One of (OVERWRITE,
    // NO_OVERWRITE, or CANCEL).
    private int _choice;


    /**
     * Export the given list of programs in a background thread.
     *
     * @param list   a list of {@link DBProgramInfo} objects describing the programs to export
     * @param dir    the directory in which to write the XML files
     * @param parent used as the parent of the progress bar
     */
    public ExportWorker(List list, File dir, Component parent) {
        BusyWin.setBusy(true);
        _list = list;
        _dir = dir;
        _parent = parent;

        _progressMonitor = new ProgressMonitor(parent, "", "", 0, 100);
        _progressMonitor.setMillisToDecideToPopup(0);
        _progressMonitor.setMillisToPopup(0);
        _progressMonitor.setNote("Exporting programs...");
        _progressMonitor.setProgress(5);
    }

    /**
     * Called in background thread
     */
    public Object construct() {
        try {
            final IDBDatabaseService db = SPDB.get();
            if (db == null) throw new RuntimeException("Failed to get a database.");

            final String sep = System.getProperty("file.separator");
            final int numProgs = _list.size();
            int n = 0;
            for (int i = 0; i < numProgs; i++) {
                final DBProgramInfo info = (DBProgramInfo) _list.get(i);
                String s = info.getProgramIDAsString();
                if (s == null || s.length() == 0)
                    s = info.nodeKey.toString();
                ISPNode root = null;
                SPNodeKey key = info.nodeKey;
                if (info.isNightlyPlan()) {
                    throw new RuntimeException("Not expecting nightly plans.");
                } else {
                    root = db.lookupProgram(key);
                }
                if (root != null) {
                    String xmlFileName = _dir + sep + s + ".xml";
                    File file = new File(xmlFileName);
                    int val = checkOverwrite(file);
                    if (val == OVERWRITE) {
                        if (_progressMonitor.isCanceled()) {
                            break;
                        }
                        _progressMonitor.setNote("Writing " + xmlFileName);
                        _progressMonitor.setProgress((i * 100) / numProgs);
                        _exportAsXml(db, root, xmlFileName);
                        n++;
                    } else if (val == CANCEL) {
                        break;
                    } else if (val == NO_OVERWRITE) {
                        // do nothing...
                    }
                }
            }
            return "Done. Saved " + n + " science programs in " + _dir;
        } catch (Exception e) {
            return e;
        }
    }

    /**
     * Called in event thread
     */
    public void finished() {
        BusyWin.setBusy(false);
        _progressMonitor.setProgress(100);

        Object o = getValue();
        if (o instanceof String) {
            DialogUtil.message((String) o);
        } else if (o instanceof Exception) {
            DialogUtil.error((Exception) o);
        }
    }

    // Pop up a confirm dialog in the event dispatching thread to check before
    // overwriting an existing XML file.
    private int checkOverwrite(final File file)
            throws InterruptedException, InvocationTargetException {

        if (!file.exists()) {
            return OVERWRITE;
        }

        // If user checked the checkbox, return the previously selected value.
        if (_applyAll) {
            return _choice;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                _overwrite = _showOverwriteDialog(file);
            }
        });
        return _overwrite;
    }

    // Display a dialog to get the user to confirm overwriting the given science program
    // file. (Called in the event dispatching thread.)
    private int _showOverwriteDialog(File file) {
        String title = "Confirm Overwrite File";
        String msg = "The file " + file.getName()
                + " already exists. Do you want to overwrite it?";

        final JCheckBox b = new JCheckBox("Apply choice to all other existing files");
        b.setToolTipText("Remember the button that was pressed, if there are more "
                + "existing files to be overwritten");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _applyAll = b.isSelected();
            }
        });
        Object[] array = new Object[]{msg, b};
        String[] choices = new String[]{
                OVERWRITE_MSG,
                NO_OVERWRITE_MSG,
                CANCEL_MSG,
        };

        int val = 0;
        String defaultChoice = choices[val];
        BusyWin.setBusy(false);
        val = JOptionPane.showOptionDialog(_parent,
                array,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                defaultChoice);

        BusyWin.setBusy(true);
        _choice = val;
        return val;
    }

    // Export an ISPProgram as an XML file.
    private void _exportAsXml(IDBDatabaseService db, ISPNode root, String xmlFileName)
            throws IOException, SPNodeNotLocalException {

        // be sure to assign functor to the result here since a copy is returned with the results
        final SpExportFunctor functor = db.getQueryRunner(OT.getUser()).execute(new SpExportFunctor(), root);
        final String msg = functor.getProblem();
        final String xml = functor.getXmlProgram();
        if (msg != null || xml == null) {
            throw new RuntimeException("Error parsing " + xmlFileName + ": " + msg);
        }
        final FileOutputStream fout = new FileOutputStream(xmlFileName);
        try {
            fout.write(xml.getBytes(Charset.forName("UTF-8")));
        } finally {
            fout.close();
        }

    }
}
