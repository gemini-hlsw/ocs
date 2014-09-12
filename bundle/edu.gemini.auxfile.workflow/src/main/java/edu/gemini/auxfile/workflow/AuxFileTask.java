package edu.gemini.auxfile.workflow;

import java.io.File;

import edu.gemini.spModel.core.SPProgramID;

public abstract class AuxFileTask {

	protected final SPProgramID _progId;
	protected final File _file;

	public AuxFileTask(SPProgramID progId, File file) {
        if (progId == null) throw new NullPointerException();
        if (file == null) throw new NullPointerException();
        _progId    = progId;
        _file      = file;
	}

	public SPProgramID getProgId() {
	    return _progId;
	}

	public File getFile() {
	    return _file;
	}

	public abstract void execute(Workflow wf) throws Exception;
	
}
