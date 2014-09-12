//
// $Id: TransferDirCleaner.java 319 2006-04-11 20:01:12Z shane $
//

package edu.gemini.auxfile.server.file;

import edu.gemini.spModel.core.SPProgramID;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.FileFilter;

/**
 *
 */
public final class TransferDirCleaner {
    private static final long HOUR = 1000 * 60 * 60;

    private static void _sweep() {
        FileManager fman = FileManager.instance();

        Collection<SPProgramID> progIds = fman.getAllProgramIds();

        for (SPProgramID progId : progIds) {
            File dir = fman.getTransferDir(progId);

            final long now = System.currentTimeMillis();
            File[] delList = dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    long lastModified = file.lastModified();
                    return ((now - lastModified) > HOUR);
                }
            });
            if (delList == null) continue;

            for (File f : delList) {
                f.delete();
            }
        }
    }

    private static class CleanupTask extends TimerTask {
        public void run() {
            _sweep();
        }
    }

    private static Timer CLEANUP_TIMER;

    public static synchronized void start() {
        if (CLEANUP_TIMER != null) return;
        CLEANUP_TIMER = new Timer();
        CLEANUP_TIMER.schedule(new CleanupTask(), HOUR, HOUR);
    }

    public static synchronized void stop() {
        if (CLEANUP_TIMER == null) return;
        CLEANUP_TIMER.cancel();
        CLEANUP_TIMER = null;
    }
}
