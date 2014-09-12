//
// $Id: DatasetProblemLogger.java 534 2006-08-25 22:15:33Z shane $
//

package edu.gemini.dataman.util;

import edu.gemini.spModel.dataset.DatasetLabel;

import java.util.*;
import java.util.logging.Level;

/**
 *
 */
public class DatasetProblemLogger {
    private static Timer _timer = new Timer("DatasetProblemLoggerTimer", true);

    private static class LogTask extends TimerTask {
        public void run() {
        }
    }

    private String _message;
    private Level _level;

    private Set<DatasetLabel> _datasets;

    public DatasetProblemLogger(String message, Level level) {
        _message = message;
        _level   = level;
        _datasets = new HashSet<DatasetLabel>();
    }

    public void addDataset(DatasetLabel label) {
        _datasets.add(label);
    }
}
