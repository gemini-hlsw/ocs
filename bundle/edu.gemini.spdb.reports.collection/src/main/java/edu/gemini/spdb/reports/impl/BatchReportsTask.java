package edu.gemini.spdb.reports.impl;

import edu.gemini.util.ssh.*;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.cron.CronJob;
import edu.gemini.spdb.cron.CronStorage;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.IReport;
import edu.gemini.spdb.reports.IRow;
import edu.gemini.spdb.reports.ITable;

import java.io.*;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.gemini.spdb.cron.util.Props;

import scala.util.Try;
import scala.util.Failure;
import scala.runtime.BoxedUnit;

/** Task to generate batch reports. This task is invoked by the cron bundle. */
public class BatchReportsTask implements CronJob {

    private final File rootDir;

    public BatchReportsTask(File rootDir) {
        this.rootDir = rootDir;
    }

    public void run(final CronStorage store, final Logger log, final Map<String, String> env, Set<Principal> user) {

        // Get our config
        final Props props = new Props(env);
        final String username = props.getString("edu.gemini.spdb.reports.public.username");
        final String password = props.getString("edu.gemini.spdb.reports.public.password");
        final String remoteHost = props.getString("edu.gemini.spdb.reports.public.host");
        final String remoteDirectory = props.getString("edu.gemini.spdb.reports.public.remotedir");

        // And away we go...
        log.info("Starting batch reports; cleaning batch root...");
        clear(rootDir);
        log.info("Done cleaning batch root.");

        for (final ReportManager.ReportRegistration reg : ReportManager.getInstance()) {
            final ITable t = TableManager.getInstance().get(reg.tableId);
            if (t != null) {

                // The report
                final IReport rep = reg.report;

                // Create and configure the query.
                final IQuery q = new QueryManager(user).createQuery(t);
                rep.configureQuery(q);

                // Run it once for each database we know about, collecting results.
                final IDBDatabaseService db = SPDB.get();
                final Map<IDBDatabaseService, List<IRow>> results = new HashMap<>();
                if (db != null) {
                    // Run the query, collecting the reports.
                    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    try {
                        log.info("Executing " + reg.id);
                        Thread.currentThread().setContextClassLoader(QueryManager.class.getClassLoader());
                        final List<IRow> rows = new QueryManager(user).runQuery(q, db);
                        results.put(db, rows);
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "Trouble creating report.", e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(loader);
                    }
                }

                try {

                    // And write out the files.
                    final List<File> files = rep.execute(q, results, rootDir);

                    // Now try to FTP the public ones to the public server
                    if (rep.isPublic()) {
                        for (final File f : files) {
                            log.info("Public report: " + f.getName() + " => " + remoteHost);
                            try {
                                sftp(log, f, username, password, remoteHost, remoteDirectory);
                            } catch (Exception ioe) {
                                log.log(Level.SEVERE, "Trouble ftp'ing public report: " + f.getName(), ioe);
                            }
                        }
                    } else {
                        for (final File f : files)
                            log.info("Private report: " + f.getName());
                    }

                    reg.setFiles(files);

                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Trouble writing report(s).", ioe);
                }


            } else {
                log.warning("Could not find table " + reg.tableId);
            }

        }

        log.info("Done with batch reports.");
    }

    /** rm -r */
    private static void clear(final File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File f : files) {
                if (f.isDirectory()) clear(f);
                f.delete();
            }
        }
    }

    /**
     * This method copies a file via sftp.
     * @param f               the file to copy
     * @param username        the remote username
     * @param password        the remote password
     * @param host            the remote host
     * @param remoteDirectory the directory to copy the file to in the remote host
     * @throws IOException if anything goes wrong during the copy(i.e. file not found, wrong password, etc...)
     */
    private static void sftp(final Logger log, final File f, final String username, final String password, final String host, final String remoteDirectory) throws Exception {
        DefaultSshConfig config = new DefaultSshConfig(host, username, password, SshConfig$.MODULE$.DEFAULT_TIMEOUT());
        log.info(String.format("sftp %s starting", config.toString()));
        long startTime = System.currentTimeMillis();
        Try<SftpSession> sftpSessionTry = SftpSession$.MODULE$.connect(config);
        if (sftpSessionTry.isSuccess()) {
            SftpSession sftpSession = sftpSessionTry.get();
            try {
                Try<BoxedUnit> bu1 = sftpSession.remoteCd(remoteDirectory);
                if (bu1.isSuccess()) {
                    Try<BoxedUnit> bu2 = sftpSession.copyLocalToRemote(f, ".", true);
                    if (!bu2.isSuccess()) throw ((Exception) ((Failure<BoxedUnit>) bu2).exception());
                } else {
                   throw ((Exception) ((Failure<BoxedUnit>) bu1).exception());
                }
            } finally {
                sftpSession.disconnect();
            }
        } else {
            throw ((Exception) ((Failure<SftpSession>) sftpSessionTry).exception());
        }
        long endTime = System.currentTimeMillis();
        log.info(String.format("ssh %s transfer done in %d ms:", config.toString(), (endTime - startTime)));

    }
}
