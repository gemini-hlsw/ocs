//
// $Id: AuxFileTransferEvent.java 314 2006-03-31 20:31:51Z shane $
//

package edu.gemini.auxfile.api;

import edu.gemini.spModel.core.SPProgramID;

import java.util.EventObject;

/**
 * An event object containing details about the progress of a file transfer.
 */
public class AuxFileTransferEvent extends EventObject {
    private SPProgramID _programId;
    private String _fileName;

    private long _bytesTransferred;
    private long _totalBytes;

    public AuxFileTransferEvent(Object source, SPProgramID programId,
                                String fileName, long bytesTransferred, long totalBytes) {
        super(source);

        if (programId == null) throw new NullPointerException("null programId");
        if (fileName  == null) throw new NullPointerException("null fileName");
        if (totalBytes < 0) {
            throw new IllegalArgumentException("negative totalBytes: " + totalBytes);
        }
        if ((bytesTransferred < 0) || (bytesTransferred > totalBytes)) {
            throw new IllegalArgumentException("bytes transferred = " +
                    bytesTransferred + ", total bytes = " + totalBytes);
        }

        _programId        = programId;
        _fileName         = fileName;
        _bytesTransferred = bytesTransferred;
        _totalBytes       = totalBytes;
    }


    public SPProgramID getProgramId() {
        return _programId;
    }

    public String getFileName() {
        return _fileName;
    }

    public long getBytesTransferred() {
        return _bytesTransferred;
    }

    public long getTotalBytes() {
        return _totalBytes;
    }

    public double getPercentageTransferred() {
        return (((double)_bytesTransferred) / _totalBytes * 100.0);
    }
}
