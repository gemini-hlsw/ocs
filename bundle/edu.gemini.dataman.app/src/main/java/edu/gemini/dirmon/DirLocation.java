//
// $Id: DirLocation.java 39 2005-08-20 22:40:25Z shane $
//

package edu.gemini.dirmon;

import java.io.Serializable;

/**
 * An interface that describes the directory that a {@link DirListener} wishes
 * to monitor.  The properties associated with the location are registered
 * with the {@link DirListener} service registration and used by the
 * implementation to determine which host and directory to examine.
 */
public interface DirLocation extends Serializable {

    /**
     * A property that identifies the host that contains the
     * filesystem with the directory to be monitored.
     */
    String HOST_PROP = "dirmon.HOST";

    /**
     * A property that identifies the directory path to monitor.
     */
    String DIR_PATH_PROP  = "dirmon.DIR_PATH";

    /**
     * Gets the host IP address that contains the filesystem with the directory
     * to be monitored.
     */
    String getHostAddress();

    /**
     * Gets the directory path to monitor.
     */
    String getDirPath();
}
