//
// $Id: CopyTask.java 855 2007-05-22 02:52:46Z rnorris $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.auxfile.copier.AuxFileType;

import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * A task that encapsulates the file to be copied and the algorithm for doing
 * the copy.
 */
public final class CopyTask extends AuxFileTask {
    private final CopyTaskState _state;
	private final long _timestamp;

    public CopyTask(CopyTaskState state, SPProgramID progId, File file, long timestamp) {
    	super(progId, file);
        _state     = state;
    	_timestamp = timestamp;
    }

    public long getTimestamp() {
	    return _timestamp;
	}

    public boolean equals(Object other) {
        if (!(other instanceof CopyTask)) return false;
        CopyTask that = (CopyTask) other;
        if (!_progId.equals(that._progId)) return false;
        if (!_file.equals(that._file)) return false;
        return (_timestamp == that._timestamp);
    }

    public int hashCode() {
        int res = _progId.hashCode();
        res = res*37 + _file.hashCode();
        return res*37 + (int)(_timestamp^(_timestamp>>>32));
    }

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    // Parses a line of the task state file
    public static CopyTask parse(CopyTaskState state, String text) throws ParseException {
        String[] parts = text.split("\t");
        if (parts.length != 3) {
            throw new ParseException("Could not parse CopyTaskState: " + text, 0);
        }

        SPProgramID progId;
        try {
            progId = SPProgramID.toProgramID(parts[0]);
        } catch (SPBadIDException ex) {
            throw new ParseException("Could not parse program id: " + parts[0], 0);
        }

        Date d = DATE_FORMAT.parse(parts[1]);
        File f = new File(parts[2]);

        return new CopyTask(state, progId, f, d.getTime());
    }

    // Formats the CopyTask object into a line suitable for inclusion in the
    // copy task state file.
    public String format() {
        StringBuilder buf = new StringBuilder();
        buf.append(_progId.stringValue());
        buf.append('\t');
        buf.append(DATE_FORMAT.format(new Date(_timestamp)));
        buf.append('\t');
        buf.append(_file.getPath());
        return buf.toString();
    }

    public String toString() {
        return format();
    }

    @Override public void execute(Workflow wf) throws Exception {

        // Figure out which copy configuration to use, the one for
        // finder charts or the one for ODF files.
        final File f = getFile();
        final AuxFileType type = AuxFileType.getFileType(f);

        // Do the copy, and if successful remove it from the state file
        // so it won't be tried again.
        final SPProgramID progId = getProgId();
        if (_state.getCopier().copy(progId, f)) {
            _state.removeTask(this);

            // Try to send out emails if notification for this file type is desired.
            if (type.sendNotification()) {
                wf.getMailer().notifyStored(progId, type, f.getName());
            }
        }
    }
}
