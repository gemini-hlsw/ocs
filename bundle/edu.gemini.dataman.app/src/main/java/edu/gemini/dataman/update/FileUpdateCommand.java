//
// $Id: FileUpdateCommand.java 137 2005-09-22 17:02:41Z shane $
//

package edu.gemini.dataman.update;

import edu.gemini.dataman.util.DatamanLoggers;
import edu.gemini.dataman.context.DatamanServices;
import edu.gemini.dataman.util.DatasetCommand;
import edu.gemini.dataman.util.DatasetCommandProcessor;
import edu.gemini.datasetfile.DatasetFile;
import edu.gemini.datasetfile.DatasetFileException;
import edu.gemini.datasetfile.DatasetFileService;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.dataset.DatasetQaState;
import edu.gemini.spModel.dataset.DatasetFileState;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A command that performs a dataset file update.
 */
public final class FileUpdateCommand implements DatasetCommand {
    private static final Logger LOG = Logger.getLogger(FileUpdateCommand.class.getName());

    private DatamanServices _services;
    private DatasetLabel _label;
    private String _filename;
    private DatasetQaState _qaState;

    private DatasetFile _result;

    public FileUpdateCommand(DatamanServices services, DatasetLabel label,
                             String filename, DatasetQaState qaState) {
        if (services == null) {
            throw new NullPointerException("Missing DatamanServices");
        }
        if (label == null) {
            throw new NullPointerException("Missing DatasetLabel");
        }
        if (qaState == null) {
            throw new NullPointerException("Missing qaState");
        }

        if (!filename.endsWith(".fits")) {
            filename = filename + ".fits";
        }
        _services = services;
        _label    = label;
        _filename = filename;
        _qaState  = qaState;
    }

    public DatasetLabel getLabel() {
        return _label;
    }

    public Boolean call() throws IOException, InterruptedException {
        DatasetFileService service = _services.getDatasetFileService();
        if (service == null) {
            LOG.warning("Received dataset file update but DatasetFileService is down.");
            return false;
        }

        DatasetFile file;
        try {
            file = service.updateQaState(_filename, _qaState);
        } catch (DatasetFileException ex) {
            RecordUpdateCommand.scheduleUpdate(_services, _label, DatasetFileState.BAD);
            String msg = "Problem updating file '" + _filename +
                         "', for dataset '" + _label + ": " + ex.getMessage();
            DatamanLoggers.DATASET_PROBLEM_LOGGER.log(Level.SEVERE, msg);
            return false;
        }

        if (file == null) {
            RecordUpdateCommand.scheduleUpdate(_services, _label, DatasetFileState.MISSING);
            String msg = "Missing dataset file '" + _filename +
                    "' for dataset '" + _label;
            DatamanLoggers.DATASET_PROBLEM_LOGGER.log(Level.SEVERE, msg);
            return false;
        }
        _setResult(file);
        return true;
    }

    /**
     * Performs a synchronous update, returning the newly updated
     * {@link DatasetFile} if successful, or <code>null</code> if not.  This
     * method simply wraps a call to the {@link #call} method, catching any
     * exceptions and retrieving the {@link DatasetFile} result.
     *
     * @return newly updated DatasetFile if successful, <code>null</code>
     * if not
     */
    public DatasetFile doUpdate() {
        try {
            return (call()) ? getResult() : null;
        } catch (InterruptedException ex) {
            String msg = "Interrupted while updating file '" + _filename +
                         "', for dataset '" + _label + "'";
            LOG.log(Level.WARNING, msg, ex);
        } catch (IOException ex) {
            String msg = "IOException updating file '" + _filename +
                         "', for dataset '" + _label + "'";
            LOG.log(Level.WARNING, msg, ex);
        }
        return null;
    }

    /**
     * Schedules an update for the indicated dataset file.  It will execute
     * asynchronously in a separate thread.  Commands associated with the
     * same dataset are serialized however.  This method deposits the
     * command in the {@link DatasetCommandProcessor} to be executed at some
     * point in the future.
     *
     * @return <code>true</code> if the update command is scheduled,
     * <code>false</code> if it could not be scheduled because the necessary
     * services are not available
     */
    public boolean scheduleUpdate() {
        DatasetFileService service = _services.getDatasetFileService();
        if (service == null) {
            LOG.warning("Received dataset file update but DatasetFileService is down.");
            return false;
        }
        return DatasetCommandProcessor.INSTANCE.add(this);
    }

    /**
     * Performs a synchronous update, returning the newly updated
     * {@link DatasetFile} if successful, or <code>null</code> if not.  This
     * is a convenience method that creates and executes the command in one
     * step.
     *
     * @param services contains a reference to the service used to perform the
     * update (namely, the {@link DatasetFileService})
     *
     * @param label label of the dataset to update
     *
     * @param filename name of the file to update
     *
     * @param qaState the new dataset QA state to apply
     *
     * @return newly updated DatasetFile if successful, <code>null</code> if not
     */
    public static DatasetFile doUpdate(DatamanServices services,
                 DatasetLabel label, String filename, DatasetQaState qaState) {

        FileUpdateCommand upd;
        upd = new FileUpdateCommand(services, label, filename, qaState);
        return upd.doUpdate();
    }

    /**
     * Schedules an update for the indicated file.  It will execute
     * asynchronously in a separate thread.  Commands associated with the same
     * dataset are serialized however.   This is a convenience method that
     * creates and schedules the FileUpdateCommand in a single method call and
     * is equivalent to the following sequence:
     *
     * <pre>
     * FileUpdateCommand cmd;
     * cmd = new FileUpdateCommand(services, label, filename, qaState);
     * return cmd.scheduleUpdate();
     * </pre>
     *
     * @param services contains references to the service used to perform the
     * update (namely, the {@link DatasetFileService})
     *
     * @return <code>true</code> if the update command is scheduled,
     * <code>false</code> if it could not be scheduled because the necessary
     * services are not available
     */
    public static boolean scheduleUpdate(DatamanServices services,
                 DatasetLabel label, String filename, DatasetQaState qaState) {
        FileUpdateCommand cmd;
        cmd = new FileUpdateCommand(services, label, filename, qaState);
        return cmd.scheduleUpdate();
    }

    /**
     * Obtains the DatasetFile that is produced after a successful call to
     * the {@link #call} method.  Will return <code>null</code> before the
     * {@link #call} method completes successfully or if it fails for some
     * reason.
     */
    public synchronized DatasetFile getResult() {
        return _result;
    }

    private synchronized void _setResult(DatasetFile record) {
        _result = record;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("FileUpdateCommand [dataset=").append(_label);
        buf.append(", filename=").append(_filename);
        buf.append(", qa=").append(_qaState);
        buf.append(']');
        return buf.toString();
    }

}
