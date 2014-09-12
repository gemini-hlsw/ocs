//
// $Id: RecordUpdateCommand.java 617 2006-11-22 21:39:46Z shane $
//

package edu.gemini.dataman.update;

import edu.gemini.dataman.context.DatamanServices;
import edu.gemini.dataman.util.DatasetCommand;
import edu.gemini.dataman.util.DatasetCommandProcessor;
import edu.gemini.datasetfile.DatasetFile;
import edu.gemini.datasetrecord.DatasetRecordService;
import edu.gemini.datasetrecord.DatasetRecordTemplate;
import edu.gemini.datasetrecord.util.DefaultTemplate;
import edu.gemini.spModel.dataset.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A command that performs a dataset record update.
 */
public final class RecordUpdateCommand implements DatasetCommand {
    private static final Logger LOG = Logger.getLogger(RecordUpdateCommand.class.getName());

    private final DatamanServices _services;
    private final DatasetLabel _label;
    private final Dataset _dataset;
    private final DefaultTemplate _update;
    private DefaultTemplate _precond;
    private DatasetRecord _result;

    /**
     * Constructs with the DatasetLabel of the dataset to be updated.  Choose
     * this constructor if the command should only attempt to update an existing
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord} (and not try to create it).
     */
    public RecordUpdateCommand(DatamanServices services, DatasetLabel label) {
        if ((services == null) || (label == null)) {
            throw new NullPointerException();
        }
        _services = services;
        _dataset  = null;
        _label    = label;
        _update   = new DefaultTemplate();
    }

    /**
     * Constructs with the Dataset object for the dataset to be created or
     * updated.  Choose this constructor if the command should attempt to
     * create a {@link edu.gemini.spModel.dataset.DatasetExecRecord} if a matching one cannot be found.
     */
//    public RecordUpdateCommand(DatamanServices services, Dataset dataset) {
//        if (services == null) throw new NullPointerException();
//        _services = services;
//        _dataset  = dataset;
//        _label    = dataset.getLabel();
//        _update   = new DefaultTemplate();
//    }

    /**
     * Constructs using information in the given {@link DatasetFile} object.
     * Will extract the dataset, QA state, and sync time from the
     * DatasetFile. Choose this constructor if the command should attempt to
     * create a {@link edu.gemini.spModel.dataset.DatasetExecRecord} if a matching one cannot be found.
     */
    public RecordUpdateCommand(DatamanServices services, DatasetFile dsetFile) {
        if (services == null) throw new NullPointerException();
        _services = services;
        _dataset  = dsetFile.getDataset();
        _label    = _dataset.getLabel();
        _update   = new DefaultTemplate();
        _update.setQaState(dsetFile.getQaState());
        _update.setSyncTime(dsetFile.lastModified());
        _update.setDatasetFileState(DatasetFileState.OK);
    }

    public DatasetLabel getLabel() {
        return _label;
    }

    public synchronized void setQaState(DatasetQaState state) {
        _update.setQaState(state);
    }

    public synchronized void setQaStatePrecond(DatasetQaState state) {
        if (_precond == null) _precond = new DefaultTemplate();
        _precond.setQaState(state);
    }

    public synchronized void setSyncTime(Long syncTime) {
        _update.setSyncTime(syncTime);
    }

    public synchronized void setSyncTimePrecond(Long syncTime) {
        if (_precond == null) _precond = new DefaultTemplate();
        _precond.setSyncTime(syncTime);
    }

    public synchronized void setDatasetFileState(DatasetFileState state) {
        _update.setDatasetFileState(state);
    }

    public synchronized void setDatasetFileStatePrecond(DatasetFileState state) {
        if (_precond == null) _precond = new DefaultTemplate();
        _precond.setDatasetFileState(state);
    }

    public synchronized void setGsaState(GsaState state) {
        _update.setGsaState(state);
    }

    public synchronized void setGsaStatePrecond(GsaState state) {
        if (_precond == null) _precond = new DefaultTemplate();
        _precond.setGsaState(state);
    }

    private synchronized DatasetRecordTemplate _getRecordUpdate() {
        return new DefaultTemplate(_update);
    }

    public Boolean call() throws InterruptedException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("update dataset record: " + this);
        }

        DatasetRecordService service = _services.getDatasetRecordService();
        if (service == null) {
            LOG.warning("Received dataset modification but DatasetRecordService is down.");
            return false;
        }

        DatasetRecord res;
        if (_dataset == null) {
            res = service.update(_label, _getRecordUpdate(), _precond);
        } else {
            res = service.updateOrCreate(_dataset, _getRecordUpdate(), _precond);
        }
        if (res == null) {
            String msg = "Could not find or create the dataset (check for duplicate dataset labels): " + _label;
            LOG.info(msg);
            return false;
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("updated dataset record: " + _label);
        }
        _setResult(res);
        return true;
    }

    /**
     * Performs a synchronous update, returning the newly updated
     * {@link edu.gemini.spModel.dataset.DatasetExecRecord} if successful, or <code>null</code> if not.  This
     * method simply wraps a call to the {@link #call} method, catching any
     * RemoteException and retrieving the {@link edu.gemini.spModel.dataset.DatasetExecRecord} result.
     *
     * @return newly updated DatasetRecord if successful, <code>null</code>
     * if not
     */
    public DatasetRecord doUpdate() {
        try {
            return (call()) ? getResult() : null;
        } catch (InterruptedException ex) {
            LOG.log(Level.WARNING, "Could not update record: " + _label, ex);
        }
        return null;
    }

    /**
     * Schedules an update for the indicated record.  It will execute
     * asynchronously in a separate thread.  Commands associated with the same
     * dataset are serialized however.  This method deposits the command
     * in the {@link DatasetCommandProcessor} to be executed at some point
     * in the future.
     *
     * @return <code>true</code> if the update command is scheduled,
     * <code>false</code> if it could not be scheduled because the necessary
     * services are not available
     */
    public boolean scheduleUpdate() {
        DatasetRecordService service = _services.getDatasetRecordService();
        if (service == null) {
            LOG.warning("Could not schedule record update because DatasetRecordService is down.");
            return false;
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Scheduling record update: " + this);
        }
        return DatasetCommandProcessor.INSTANCE.add(this);
    }

    /**
     * Creates and schedules a RecordUpdateCommand that will update the
     * QA state and sync time of the record in the database to match the given
     * <code>file</code>.  This is a convenience method that accomplishes in
     * one method call the following sequence:
     *
     * <pre>
     * RecordUpdateCommand cmd = new RecordUpdateCommand(services, file.getDataset());
     * cmd.setFromDatasetFile(file);
     * return cmd.scheduleUpdate();
     * </pre>
     *
     * @return <code>true</code> if the update command is scheduled,
     * <code>false</code> if it could not be scheduled because the necessary
     * services are not available
     */
    public static boolean scheduleUpdate(DatamanServices services, DatasetFile file) {
        return (new RecordUpdateCommand(services, file)).scheduleUpdate();
    }

    /**
     * Creates and schedules a RecordUpdateCommand that will update the
     * {@link GsaState} of the record in the database to match the given
     * state.  This is a convenience method that accomplishes in one method
     * call the following sequence:
     *
     * <pre>
     * RecordUpdateCommand cmd;
     * cmd = new RecordUpdateCommand(services, label);
     * cmd.setGsaState(state);
     * return cmd.scheduleUpdate();
     * </pre>
     *
     * @return <code>true</code> if the update command is scheduled,
     * <code>false</code> if it could not be scheduled because the necessary
     * services are not available
     */
    public static boolean scheduleUpdate(DatamanServices services,
                                     DatasetLabel label, GsaState state) {
        RecordUpdateCommand cmd;
        cmd = new RecordUpdateCommand(services, label);
        cmd.setGsaState(state);
        return cmd.scheduleUpdate();
    }

    /**
     * Creates and schedules a RecordUpdateCommand that will update the
     * {@link DatasetFileState} of the record in the database to match the
     * given state.  This is a convenience method that accomplishes in one
     * method call the following sequence:
     *
     * <pre>
     * RecordUpdateCommand cmd;
     * cmd = new RecordUpdateCommand(services, label);
     * cmd.setDatasetFileState(state);
     * return cmd.scheduleUpdate();
     * </pre>
     *
     * @return <code>true</code> if the update command is scheduled,
     * <code>false</code> if it could not be scheduled because the necessary
     * services are not available
     */
    public static boolean scheduleUpdate(DatamanServices services,
                          DatasetLabel label, DatasetFileState fileState) {
        RecordUpdateCommand cmd;
        cmd = new RecordUpdateCommand(services, label);
        cmd.setDatasetFileState(fileState);
        return cmd.scheduleUpdate();
    }

    /**
     * Obtains the DatasetRecord that is produced after a successful call to
     * the {@link #call} method.  Will return <code>null</code> before the
     * {@link #call} method completes successfully or if it fails for some
     * reason.
     */
    public synchronized DatasetRecord getResult() {
        return _result;
    }

    private synchronized void _setResult(DatasetRecord record) {
        _result = record;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("RecordUpdateCommand [dataset=").append(_label);
        buf.append(", sync=").append(_update.getSyncTime());
        buf.append(", qa=").append(_update.getQaState());
        buf.append(", fileState=").append(_update.getDatasetFileState());
        buf.append(", dataflow=").append(_update.getGsaState());
        buf.append(']');
        return buf.toString();
    }
}