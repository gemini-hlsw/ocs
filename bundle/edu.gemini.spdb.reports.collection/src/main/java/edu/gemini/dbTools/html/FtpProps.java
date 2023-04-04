//
// $Id: FtpProps.java 4336 2004-01-20 07:57:42Z gillies $
//
package edu.gemini.dbTools.html;

import edu.gemini.util.ssh.SshConfig;
import edu.gemini.util.ssh.SshConfig$;
import edu.gemini.util.ssh.DefaultSshConfig;
import edu.gemini.spdb.cron.util.Props;

import java.util.Map;

/** Common FTP properties used when FTPing files to/from the web server. */
//final public class FtpProps extends Props {
//    private static final String FTP_HOST_PROP     = "edu.gemini.dbTools.html.ftpHost";
//    private static final String FTP_USER_PROP     = "edu.gemini.dbTools.html.ftpAccount";
//    private static final String FTP_PASSWORD_PROP = "edu.gemini.dbTools.html.ftpPassword";
//    private static final String FTP_DEST_DIR_PROP = "edu.gemini.dbTools.html.ftpDestDir";
//    private static final String FTP_TIMEOUT_PROP = "edu.gemini.dbTools.html.ftpTimeout";
//
//    private DefaultSshConfig config;
//    public final String dir;

//    public FtpProps(final Map<String, String> props) {
//        super(props);
//
//        String host     = getString(FTP_HOST_PROP);
//        String user     = getString(FTP_USER_PROP);
//        String password = getString(FTP_PASSWORD_PROP);
//        int timeout     = getInt(FTP_TIMEOUT_PROP, SshConfig$.MODULE$.DEFAULT_TIMEOUT());
//        config = new DefaultSshConfig(host, user, password, timeout);
//
//        dir = getString(FTP_DEST_DIR_PROP);
//    }
//
//}

final public class FtpProps extends Props {
    private static final String FTP_HOST_PROP     = "edu.gemini.dbTools.html.ftpHost";
    private static final String FTP_USER_PROP     = "edu.gemini.dbTools.html.ftpAccount";
    private static final String FTP_PASSWORD_PROP = "edu.gemini.dbTools.html.ftpPassword";
    private static final String FTP_DEST_DIR_PROP = "edu.gemini.dbTools.html.ftpDestDir";
    private static final String FTP_TIMEOUT_PROP = "edu.gemini.dbTools.html.ftpTimeout";

    public final String dir;
    DefaultSshConfig sshConfig;

    public FtpProps(final Map<String, String> props) {
        super(props);

        String host     = getString(FTP_HOST_PROP);
        String user     = getString(FTP_USER_PROP);
        String password = getString(FTP_PASSWORD_PROP);
        int timeout     = getInt(FTP_TIMEOUT_PROP, SshConfig$.MODULE$.DEFAULT_TIMEOUT());
        sshConfig = new DefaultSshConfig(host, user, password, timeout, true);
        dir = getString(FTP_DEST_DIR_PROP);
    }

   public SshConfig getConfig() { return sshConfig; }
}
