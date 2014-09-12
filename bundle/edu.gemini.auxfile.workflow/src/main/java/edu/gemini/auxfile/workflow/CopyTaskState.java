//
// $Id: CopyTaskState.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.auxfile.copier.AuxFileCopier;

import java.io.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.text.ParseException;

/**
 * Manages a file containing CopyTasks.  Provides for reading, modifying the
 * state file.
 */
public final class CopyTaskState {
    private static final Logger LOG = Logger.getLogger(CopyTaskState.class.getName());

    private final AuxFileCopier _copier;
    private final File _stateFile;

    public CopyTaskState(AuxFileCopier copier, File stateFile) {
        _copier    = copier;
        _stateFile = stateFile;
    }

    public AuxFileCopier getCopier() {
        return _copier;
    }

    public synchronized Collection<CopyTask> getTasks() {
        Collection<CopyTask> res = new ArrayList<CopyTask>();

        BufferedReader br = null;
        try {
            final FileReader fr = new FileReader(_stateFile);
            br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                res.add(CopyTask.parse(this, line));
            }
        } catch (FileNotFoundException ex) {
            // no state

        } catch (ParseException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            try { if (br != null) br.close(); } catch (Exception ex) {/*empty*/}
        }

        return res;
    }

    synchronized void setTasks(Collection<CopyTask> state) {
        BufferedWriter bw = null;
        try {
            final FileWriter fw = new FileWriter(_stateFile);
            bw = new BufferedWriter(fw);

            for (CopyTask task : state) {
                bw.write(task.format());
                bw.newLine();
            }

            bw.flush();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);

        } finally {
            try { if (bw != null) bw.close(); } catch (Exception ex) {/*empty*/}
        }
    }

    public synchronized void addTask(CopyTask task) {
        Collection<CopyTask> state = getTasks();
        for (Iterator<CopyTask> it = state.iterator(); it.hasNext(); ) {
            AuxFileTask curTask = it.next();
            if (curTask.getFile().equals(task.getFile())) {
                it.remove();
                break;
            }
        }
        state.add(task);
        setTasks(state);
    }

    public synchronized void removeTask(AuxFileTask task) {
        Collection<CopyTask> state = getTasks();
        boolean modified = false;
        for (Iterator<CopyTask> it = state.iterator(); it.hasNext(); ) {
            AuxFileTask curTask = it.next();
            if (curTask.equals(task)) {
                it.remove();
                modified = true;
                break;
            }
        }
        if (modified) setTasks(state);
    }
}
