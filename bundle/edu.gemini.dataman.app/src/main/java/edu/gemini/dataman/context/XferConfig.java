//
// $Id: XferConfig.java 129 2005-09-14 15:40:53Z shane $
//

package edu.gemini.dataman.context;

/**
 * Configuration properties associated with transfering files to remote
 * machines.
 */
public interface XferConfig extends RemoteUserConfig {
    String XFER_TEMP_DIR_PREFIX = "edu.gemini.dataman.xfer.tempDir";
    String XFER_DEST_DIR_PREFIX = "edu.gemini.dataman.xfer.destDir";

    String getTempDir();
    String getDestDir();
}
