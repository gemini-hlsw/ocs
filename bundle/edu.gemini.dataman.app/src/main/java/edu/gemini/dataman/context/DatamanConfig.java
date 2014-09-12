//
// $Id: DatamanConfig.java 245 2006-01-03 18:52:39Z shane $
//

package edu.gemini.dataman.context;

import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.net.URL;

/**
 * Simple Dataman configuration properties.
 */
public interface DatamanConfig {

    String RAW_DIR_PROP      = "edu.gemini.dataman.rawDir";
    String WORK_DIR_PROP     = "edu.gemini.dataman.workDir";
    String GSA_CRC_URL_PROP  = "edu.gemini.dataman.gsaCrcUrl";
    String GSA_XFER_URL_PROP = "edu.gemini.dataman.gsaXferUrl";
    String SMTP_HOST_PROP    = "edu.gemini.dataman.smtpHost";
    String CHECK_MAIL_CC     = "edu.gemini.dataman.checkMailCc";
    String SCAN_TIME_PROP    = "edu.gemini.dataman.dbScanMinutes";

    /**
     * Gets the directory to which raw datasets are written by the instruments.
     */
    File getRawDir();

    /**
     * Gets the "working" directory where header updates are made to datasets.
     */
    File getWorkDir();

    /**
     * Gets the FileFilter used to select files in the working storage for a
     * dataset sync.
     */
    FileFilter getFileFilter();

    /**
     * Gets the configuration required to transfer datasets to the base
     * facility "testing" copy.
     */
    XferConfig getBaseXferConfig();

    /**
     * Gets the configuration required to transfer datasets to the GSA transfer
     * directory.
     */
    GsaXferConfig getGsaXferConfig();

    /**
     * URL for checking for dataset existence/CRC in the GSA.  For a dataset
     * that has been accepted, the CRC that is returned
     */
    GsaUrl getGsaCrcUrl();

    /**
     * URL for checking on the transfer state of a file.
     */
     GsaUrl getGsaXferStatusUrl();

    /**
     * URL for checking on the transfer state of all files in the GSA transfer
     * system.
     */
    URL getGsaAllXferStatusUrl();

    /**
     * The SMTP host to use when sending emails to contact scientists.
     */
    String getSmtpHost();

    /**
     * Gets any email addresses that should be cc'ed when the contact
     * scientist is notified.
     */
    Collection<InternetAddress> getCheckMailCc();

    /**
     * Gets the amount of time (in milliseconds) to wait before checking the
     * ODB for datasets with {@link edu.gemini.spModel.dataset.GsaState}s that
     * need to be updated.  See {@link edu.gemini.dataman.gsa.GsaVigilante} for
     * more information.
     */
    long getOdbScanTime();
}
