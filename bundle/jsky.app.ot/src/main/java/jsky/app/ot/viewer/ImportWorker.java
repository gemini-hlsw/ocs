// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ImportWorker.java 47000 2012-07-26 19:15:10Z swalker $
//
package jsky.app.ot.viewer;

import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.util.FileUtil;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.io.SpImportService;
import jsky.app.ot.SplashDialog;
import jsky.util.gui.BusyWin;
import jsky.util.gui.DialogUtil;
import jsky.util.gui.SwingWorker;
import scala.util.Failure;
import scala.util.Try;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;


/**
 * A utility class for importing science programs from XML files in a background thread, while
 * displaying a progress bar.
 */
public class ImportWorker extends SwingWorker {

    // Duplicates dialog button indexes
    private static final int REPLACE_PROG = 0;
    private static final int COPY_PROG = 1;
    private static final int KEEP_EXISTING = 2;

    // Button labels
    private static final String REPLACE_MSG = "Replace With Imported Program";
    private static final String COPY_MSG = "Create Duplicate Program";
    private static final String KEEP_MSG = "Keep Existing Program";
    private static final String CANCEL_MSG = "Cancel";


    // An array of files to import
    private File[] _files;

    // Displayed while the files are being imported
    private ProgressMonitor _progressMonitor;

    // The viewer to display the imported program
    private SPViewer _viewer;

    // If true, don't display the duplicates dialog, but remember the last setting
    private boolean _applyAll;

    // The last choice made in the duplicates dialog.
    private Option<SpImportService.ImportDirective> _choice = None.instance();

    /**
     * Constructor
     */
    public ImportWorker(File[] files, SPViewer viewer) {
        _files = files;
        _viewer = viewer;

        BusyWin.setBusy(true);

        _progressMonitor = new ProgressMonitor(_viewer, "", "", 0, 100);
        _progressMonitor.setMillisToDecideToPopup(0);
        _progressMonitor.setMillisToPopup(0);
    }

    /**
     * Called in background thread
     */
    public Object construct() {
        final List<ISPProgram> progList = new ArrayList<ISPProgram>(_files.length);
        try {

            final IDBDatabaseService db = SPDB.get();
            if (db == null)
                throw new RuntimeException("Failed to get a database.");

            for (int i = 0; i < _files.length; i++) {
                if (_progressMonitor.isCanceled()) break;
                _progressMonitor.setNote("Reading " + _files[i].getName());
                if (_files.length == 1) {
                    _progressMonitor.setProgress(0);
                } else {
                    _progressMonitor.setProgress((i * 100) / _files.length);
                }

                // Do a quick parse of the XML file header
                final SpImportService sis = new SpImportService(db);
                final String xml = FileUtil.readFile(_files[i]);

                class Dup implements SpImportService.DuplicateQuery<ISPProgram> {
                    boolean cancelled = false;

                    @Override
                    public SpImportService.ImportDirective ask(ISPProgram importNode, ISPProgram existingNode) {
                        if (_applyAll && !_choice.isEmpty())
                            return _choice.getValue();
                        _choice = _showDuplicateDialogInEventThread(importNode);
                        final SpImportService.ImportDirective res = _choice.isEmpty() ? SpImportService.Skip$.MODULE$ : _choice.getValue();
                        cancelled = _choice.isEmpty();
                        return res;
                    }
                }

                final Dup dup = new Dup();
                final Try<ISPProgram> tryP = sis.importProgramXml(new StringReader(xml), dup);
                if (dup.cancelled) return null;
                if (tryP.isFailure())
                    throw ((Failure<ISPProgram>) tryP).exception();
                progList.add(tryP.get());
            }
        } catch (Throwable e) {
            return e;
        }
        // I don't understand the reasoning, but we seem to return just one program.
        return (progList.size() == 0) ? null : progList.get(0);
    }


    /**
     * Called in event thread
     */
    public void finished() {
        BusyWin.setBusy(false);
        _progressMonitor.setProgress(100);

        Object o = getValue();
        if (o instanceof ISPProgram) {
            if (_files.length == 1) _showViewer((ISPProgram) o);
        } else if (o instanceof Exception) {
            DialogUtil.error((Exception) o);
        }
    }

    // Call _showDuplicateDialog in the event dispatching thread
    private Option<SpImportService.ImportDirective> _showDuplicateDialogInEventThread(final ISPProgram prog) {

        class QueryUser implements Runnable {
            Option<SpImportService.ImportDirective> res = None.instance();

            public void run() {
                res = _showDuplicateDialog(prog);
            }
        }

        final QueryUser qu = new QueryUser();
        try {
            SwingUtilities.invokeAndWait(qu);
        } catch (Exception e) {
            final String s = (prog.getProgramID() == null) ?
                    ((SPProgram) prog.getDataObject()).getTitle() :
                    prog.getProgramID().stringValue();
            DialogUtil.error("Problem importing '" + s + "'");
        }
        return qu.res;
    }

    // Display a dialog to get the user to confirm overwriting the given science program.
    // (Called in the event dispatching thread.)
    private Option<SpImportService.ImportDirective> _showDuplicateDialog(ISPProgram prog) {
        SPProgramID pid = prog.getProgramID();
        String title, msg;
        if (pid == null) {
            title = "Duplicate Program Key";
            msg = "A science program with the key '"
                    + prog.getNodeKey()
                    + "' (title: "
                    + ((SPProgram) prog.getDataObject()).getTitle()
                    + ") already exists in the database.";
        } else {
            title = "Duplicate Program Id";
            msg = "A science program with the id '"
                    + pid
                    + "' already exists in the database.";
        }

        String[] choices;
        Object[] array;
        if (_files.length == 1) {
            array = new Object[]{msg};
            choices = new String[]{
                    REPLACE_MSG,
                    COPY_MSG,
                    KEEP_MSG
            };
        } else {
            final JCheckBox b = new JCheckBox("Apply choice to all other duplicates");
            b.setToolTipText("Remember the button that was pressed, if there are more duplicate program ids found");
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _applyAll = b.isSelected();
                }
            });
            array = new Object[]{msg, b};
            choices = new String[]{
                    REPLACE_MSG,
                    COPY_MSG,
                    KEEP_MSG,
                    CANCEL_MSG,
            };
        }

        int val = 0;
        String defaultChoice = choices[val];
        BusyWin.setBusy(false);
        val = JOptionPane.showOptionDialog(_viewer,
                array,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                defaultChoice);

        BusyWin.setBusy(true);

        final SpImportService.ImportDirective id;
        switch (val) {
            case REPLACE_PROG:
                id = SpImportService.Replace$.MODULE$;
                break;
            case COPY_PROG:
                id = SpImportService.Copy$.MODULE$;
                break;
            case KEEP_EXISTING:
                id = SpImportService.Skip$.MODULE$;
                break;
            default:
                id = null;
        }
        return ImOption.apply(id);
    }


    // Display the given program in the viewer
    private void _showViewer(ISPProgram prog) {
        try {
            ViewerManager.open(prog);
            SplashDialog.hideInstance();
        } catch (Exception ex) {
            DialogUtil.error(ex);
        }
    }
}
