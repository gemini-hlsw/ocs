//
// $Id: RemoteUserConfig.java 129 2005-09-14 15:40:53Z shane $
//

package edu.gemini.dataman.context;

import edu.gemini.util.ssh.SshConfig;

/**
 * Configuration properties associated with a user on another machine.
 * For example, used for executing remote commands via ssh and for transferring
 * files via ftp.
 */
public interface RemoteUserConfig extends SshConfig {
    String XFER_USER_PREFIX = "edu.gemini.dataman.xfer.user";
    String XFER_HOST_PREFIX = "edu.gemini.dataman.xfer.host";
    String XFER_PASS_PREFIX = "edu.gemini.dataman.xfer.pass";
}
