//
// $
//

package jsky.app.ot.log;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

/**
 * Configures Java Logging to send emails for WARNING (or worse) log messages
 * in the background.
 */
public final class JavaLoggingEmailer extends Handler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(JavaLoggingEmailer.class.getName());

    private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<LogRecord>();
    private final Thread worker = new Thread(this, "JavaLoggingEmailer");
    private final Properties sessionProps = new Properties();
    private final Address[] to;

    boolean open = true;

    public JavaLoggingEmailer() {
        to = addresses(EmailConfig.instance.to());
        setLevel(Level.WARNING);
        setFormatter(new SimpleFormatter());
        sessionProps.put("mail.host", EmailConfig.instance.smtp());
        sessionProps.put("mail.from", EmailConfig.instance.from());
        worker.setDaemon(true);
    }

    public void open() {
        worker.setPriority(Thread.NORM_PRIORITY-1);
        worker.start();
    }

    public void close() {
        try {
            open = false;
            queue.clear();
            worker.interrupt();
        } catch (Exception ex) {
            // ignore
        }
    }

    @Override
    public void publish(LogRecord rec) {
        if (open && isLoggable(rec)) queue.add(rec);
    }

    @Override
    public boolean isLoggable(LogRecord rec) {
        return super.isLoggable(rec) && !LOGGER.getName().equals(rec.getLoggerName());
    }


    @Override
    public void flush() {
        // Actually doing a flush would take too long, so just ignore
        // this event. This means that we could lose some messages on
        // VM shutdown. Hm.
    }

    private String getText(LogRecord rec) {
        String ctx = EmailConfig.instance.context();
        String body = getFormatter().format(rec);
        return ctx == null ? body : ctx + "\n" + body;
    }

    public void run() {
        for (; open;) {
            try {
                LogRecord rec = queue.take();
                Session session = Session.getInstance(sessionProps);
                Message message = new MimeMessage(session);
                message.setRecipients(Message.RecipientType.TO, to);
                message.setSubject(EmailConfig.instance.getSubject(rec.getThrown()));
                message.setText(getText(rec));
                Transport.send(message);
            } catch (InterruptedException ie) {
            } catch (MessagingException e) {
                LOGGER.log(Level.WARNING, "Trouble sending message.", e);
            }
        }
    }

    private static Address[] addresses(String list) {
        ArrayList<Address> ret = new ArrayList<Address>();
        if (list != null) {
            for (String a: list.split("\\s*,\\s*")) {
                try {
                    ret.add(new InternetAddress(a));
                } catch (AddressException e) {
                    LOGGER.log(Level.WARNING, "Could not add address: " + a, e);
                }
            }
        }
        return ret.toArray(new Address[ret.size()]);
    }

    private static Logger getRoot(Logger logger) {
        Logger parent = logger.getParent();
        return parent == null ? logger : getRoot(parent);
    }

    public static Logger getRoot() {
        return getRoot(Logger.getLogger(Logger.GLOBAL_LOGGER_NAME));
    }

    public static void install() {
        if (!EmailConfig.instance.shouldInstall()) return;
        JavaLoggingEmailer h = new JavaLoggingEmailer();
        h.open();
        getRoot().addHandler(h);
    }
}
