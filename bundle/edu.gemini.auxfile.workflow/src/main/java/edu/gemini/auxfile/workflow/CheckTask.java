package edu.gemini.auxfile.workflow;

import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.spModel.core.SPProgramID;

import java.io.File;

public class CheckTask extends AuxFileTask {

	private final boolean _checked;

	public CheckTask(SPProgramID progId, File file, boolean checked) {
		super(progId, file);
		_checked = checked;
	}

	@Override
	public void execute(Workflow wf) throws Exception {
        final AuxFileType type = AuxFileType.getFileType(getFile());

        // Try to send out emails if notification for this file type is desired.
        if (type.sendNotification()) {
			wf.getMailer().notifyChecked(getProgId(), type, getFile().getName(), _checked);
		}
	}

}
