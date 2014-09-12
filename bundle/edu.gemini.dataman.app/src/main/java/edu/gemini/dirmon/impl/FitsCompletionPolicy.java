//
// $Id: FitsCompletionPolicy.java 54 2005-08-31 16:25:13Z shane $
//

package edu.gemini.dirmon.impl;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 */
public class FitsCompletionPolicy extends IdleTimeCompletionPolicy {
    private static final Logger LOG = Logger.getLogger(FitsCompletionPolicy.class.getName());

    // The size of a complete FITS file block.
    private static final long FITS_FILE_BLOCK = 2880;

    public FitsCompletionPolicy() {
    }

    public FitsCompletionPolicy(long maxWriteIdleTime) {
        super(maxWriteIdleTime);
    }

    public boolean isComplete(File f, long idleTime) {
        // See if the file has been idle long enough to make it a candidate for
        // completion.
        if (!super.isComplete(f, idleTime)) return false;

        // If this isn't a fits file, then assume it is complete.
        if (!f.getName().endsWith(".fits")) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(f+ " is not a FITS file and is " +
                          idleTime + " ms old, so it is considered complete.");
            }
            return true;
        }

        // Return true if the file size is an expected FITS file size.
        boolean evenBlockSize = (f.length() % FITS_FILE_BLOCK) == 0;

        if (evenBlockSize  && LOG.isLoggable(Level.FINE)) {
            LOG.fine(f + " is " + idleTime +
                      " ms old, and contains an even number of blocks.");
        }

        return evenBlockSize;
    }
}
