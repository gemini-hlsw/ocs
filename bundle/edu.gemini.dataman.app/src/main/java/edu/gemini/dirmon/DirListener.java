//
// $Id: DirListener.java 53 2005-08-31 04:10:57Z shane $
//

package edu.gemini.dirmon;

import java.util.EventListener;



/**
 * An interface implemented by clients that wish to receive directory events.
 * DirListeners may be added directly to MonitoredDirImpl instances, but the
 * intended approach is that the client register the DirListener implementation
 * as an OSGi service.  The registration should be accompanied by properties
 * properties that describe the desired directory and the environment in which
 * the client is expected to run.
 *
 * <p>The dirmon bundles offer client and server bundles.  The client and
 * server can run in the same application, in different applications on the
 * same machine, or on different machines.  In either of the later cases, the
 * JiniDriver optional OSGi service must be running.  The DirListener is
 * published as a Jini service in this case, and the remote dirmon server is
 * notified of the service registration via the JiniDriver.
 *
 * <ul>
 * <li><b>{@link DirLocation.DIR_PATH_PROP}</b> - (required)
 * Contains the directory path that the listener is interested in watching.
 * This should be a directory local to the machine on which the application
 * containing this listener is running, or else the <code>HOST_PROP</code>
 * and <code>DEVICE_CATEGORY</code> properties defined below must be set as
 * well.
 * </li>
 *
 * <li><b>{@link DirLocation.HOST_PROP}</b> - (optional)
 * The name of the host which should contain a dirmon server with access to the
 * specified directory.
 * </li>
 *
 * <li><b>DEVICE_CATEGORY</b> - (optional)
 * Should be set to JiniDriver.DEVICE_CATEGORY if the dirmon listener is
 * listening to a remote server.
 * </li>
 *
 * <li><b>JiniDriver.EXPORT</b> - (optional)
 * Should be set to any value (including an empty string) if the dirmon
 * listener is listening to a remote server. This property is used to signal
 * the JiniDriver that this listener should be exported as a Jini service.
 * </li>
 * </ul>
 *
 * An easy way to obtain the host and dir path properties is to construct a
 * {@link edu.gemini.dirmon.util.DefaultDirLocation} instance and call
 * its {@link edu.gemini.dirmon.util.DefaultDirLocation#toDictionary()}
 * method.
 */
public interface DirListener extends EventListener {

    /**
     * Called when the directory being watched has completed its initial scan
     * of the directory and notices the listener.  Allows the DirListener
     * implementation to perform any required initialization.
     *
     * @param dir the MonitoredDir that will send {@link #dirModified(DirEvent)
     * events} should any changes to the directory happen
     *
     * @param lastModified time at which the last update to the directory was
     * made; the listener will not be notified of updates before this time
     */
    void init(MonitoredDir dir, long lastModified) ;

    /**
     * Called whetever the directory being monitored is modified.  In other
     * words, when a file is added, deleted, or updated.
     *
     * @param evt event describing the modification(s) to the directory that
     * have taken place
     */
    void dirModified(DirEvent evt) ;
}
