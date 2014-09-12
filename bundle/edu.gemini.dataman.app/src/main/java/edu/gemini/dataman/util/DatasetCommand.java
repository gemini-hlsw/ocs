//
// $Id: DatasetCommand.java 101 2005-09-07 22:11:01Z shane $
//

package edu.gemini.dataman.util;

import edu.gemini.spModel.dataset.DatasetLabel;

import java.util.concurrent.Callable;

/**
 * An interface that may be immplented for commands associated with a particular
 * dataset.  For example, a command might be an update in the ODB, or the
 * transfer of the dataset to the GSA transfer directory.
 *
 * <p>DatasetCommands may be used with the {@link DatasetCommandProcessor} if
 * asynchronous execution is desired.  Otherwise, the call method may be
 * invoked directly for synchronous execution.
 *
 * <p>The <code>Boolean call()</code> method should return <code>true</code> if
 * the command was successful, and <code>false</code> otherwise.
 */
public interface DatasetCommand extends Callable<Boolean> {

    /**
     * Gets the label of the dataset with which this command is associated.
     */
    DatasetLabel getLabel();
}
