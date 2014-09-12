// Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: ExportXML.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.spModel.io.app;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.DBProgramKeyAndId;
import edu.gemini.pot.spdb.DBSlaveSegregatedListFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpExportFunctor;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExportXmlApp {
    private static final Logger LOG = Logger.getLogger(ExportXmlApp.class.getName());

    static class SimpleEmailer {
        static private List<String> lines = new ArrayList<String>();

        static synchronized public void appendLine(String line) {
            lines.add(line);
        }

        static synchronized public List<String> getLines() {
            return lines;
        }

        static synchronized public void send() {
            if (lines.isEmpty()) return;
            // Recipient's email ID needs to be mentioned.
            String[] to = {"nbarriga@gemini.edu", "swalker@gemini.edu", "anunez@gemini.edu"};

            // Sender's email ID needs to be mentioned
            String from = "Don't reply to this message <noreply@gemini.edu>";

            // Get system properties
            Properties properties = System.getProperties();

            String smtp = properties.getProperty("mail.smtp.host");
            if (smtp == null || smtp.equals("")) {
                LOG.severe("Not sending email: 'mail.smtp.host' system property is not set.");
                return;
            }

            // Get the default Session object.
            Session session = Session.getDefaultInstance(properties);

            try {
                // Create a default MimeMessage object.
                MimeMessage message = new MimeMessage(session);

                // Set From: header field of the header.
                message.setFrom(new InternetAddress(from));

                // Set To: header field of the header.
                for (String recipient : to) {
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                }
                // Set Subject: header field
                message.setSubject("Problems exporting XMLs");

                // Now set the actual message
                StringBuilder text = new StringBuilder();
                text.append("List of Problems:\n");
                for (String line : lines) {
                    text.append(line);
                    text.append("\n");
                }
                message.setText(text.toString());

                // Send message
                Transport.send(message);
                System.out.println("Sent message successfully....");
            } catch (MessagingException mex) {
                mex.printStackTrace();
            }
        }
    }

    enum NodeType {
        plan() {
            public ISPNode lookup(IDBDatabaseService db, SPNodeKey key)  {
                return db.lookupNightlyPlan(key);
            }
        },
        program() {
            public ISPNode lookup(IDBDatabaseService db, SPNodeKey key)  {
                return db.lookupProgram(key);
            }
        };

        public abstract ISPNode lookup(IDBDatabaseService db, SPNodeKey key) ;

    }

    private IDBDatabaseService _database;
    private Set<Principal> _user;

    // Initialize the database connection using a remote or
    // local database.
    private static IDBDatabaseService initDatabase(String localDB) {
        if (localDB == null) throw new IllegalArgumentException("localDB = null");
        IDBDatabaseService db = null;
        try {
            db = DBLocalDatabase.create(new File(localDB));
        } catch (IOException ex) {
            System.out.println("Failed to open a database.");
            ex.printStackTrace();
            System.exit(1);
        }
        return db;
    }

    private static class ExportWorker implements Runnable {
        private final IDBDatabaseService _db;
        private final File _destDir;
        private final DBProgramKeyAndId _key;
        private final int _dbNum;
        private final NodeType _type;
        private final Set<Principal> _user;

        ExportWorker(IDBDatabaseService db, int dbNum, File destDir, DBProgramKeyAndId key, NodeType type, final Set<Principal> user) {
            _db = db;
            _destDir = destDir;
            _key = key;
            _dbNum = dbNum;
            _type = type;
            _user = user;
        }

        public void run() {
            SPNodeKey progKey = _key.getKey();
            SPProgramID progId = _key.getId();
            String fileName = (progId == null) ? progKey.toString() : progId.toString();

            StringBuilder buf = new StringBuilder();
            buf.append(progKey);
            if (progId != null) {
                buf.append(" (").append(progId).append(")");
            }
            String humanReadable = buf.toString();

            try {
                ISPNode node = _type.lookup(_db, progKey);
                if (node == null) {
                    LOG.warning(String.format("No %s was found for the key: %s", _type.name(), progKey));
                    return;
                }
                File dest = new File(_destDir, fileName + ".xml");
                System.out.println(String.format("Exporting (db %2d) %s to %s", _dbNum, humanReadable, dest));


                SpExportFunctor functor = new SpExportFunctor();
                functor = _db.getQueryRunner(_user).execute(functor, node);

                String msg = functor.getProblem();
                String xml = functor.getXmlProgram();
                if (msg != null || xml == null) {
                    LOG.warning("Error writing " + dest + ": " + msg);
                    return;
                }

                FileOutputStream fout = new FileOutputStream(dest);
                BufferedOutputStream bos = new BufferedOutputStream(fout);
                try {
                    bos.write(xml.getBytes(Charset.forName("UTF-8")));
                } finally {
                    try {
                        bos.flush();
                    } catch (Exception ex) { 
                        LOG.log(Level.SEVERE, "Trouble flushing output to " + dest, ex);
                    }
                    try {
                        bos.close();
                    } catch (Exception ex) { 
                        LOG.log(Level.SEVERE, "Trouble closing output to " + dest, ex);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Export of " + humanReadable + " failed", e);
                SimpleEmailer.appendLine("Export of " + humanReadable + " failed: " + e);
            } catch (Error e) {
                LOG.log(Level.SEVERE, "Export of " + humanReadable + " failed", e);
                SimpleEmailer.appendLine("Export of " + humanReadable + " failed: " + e);
            }
        }
    }

    public ExportXmlApp(IDBDatabaseService db, Set<Principal> user) {
        _database = db;
        _user = user;
    }

    public int exportAll(File dest)  {
        return exportAllProgs(dest) + exportAllPlans(dest);
    }

    public int exportAllProgs(File dest)  {
        Collection<Collection<DBProgramKeyAndId>> allProgs;
        allProgs = DBSlaveSegregatedListFunctor.getProgramList(_database, _user);
        return _exportAll(dest, allProgs, NodeType.program);
    }

    public int exportAllPlans(File dest)  {
        Collection<Collection<DBProgramKeyAndId>> allPlans;
        allPlans = DBSlaveSegregatedListFunctor.getNightlyPlanList(_database, _user);
        return _exportAll(dest, allPlans, NodeType.plan);
    }


    private int _exportAll(File dest, Collection<Collection<DBProgramKeyAndId>> all, NodeType type) {
        ExecutorService[] execs = new ExecutorService[all.size()];
        for (int i = 0; i < execs.length; ++i) {
            execs[i] = Executors.newSingleThreadExecutor();
        }
        if (execs.length > 1) {
            System.out.println("*** " + execs.length + "-way parallel export.");
        }

        int sum = 0;
        int i = 0;
        for (Collection<DBProgramKeyAndId> slaveProgs : all) {
            for (DBProgramKeyAndId key : slaveProgs) {
                execs[i].execute(new ExportWorker(_database, i, dest, key, type, _user));
            }
            sum += slaveProgs.size();
            ++i;
        }

        for (ExecutorService exec : execs) {
            exec.shutdown();
            try {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // empty
            }
        }
        return sum;
    }

}

