package edu.gemini.util.logging;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailHandler extends NamedHandler implements PropertyChangeListener, Runnable {

	public static final String PROP_TO = "to";
	public static final String PROP_CC = "cc";
	public static final String PROP_BCC = "bcc";
	public static final String PROP_LEVEL = "level";
	public static final String PROP_FORMATTER = "formatter";
	public static final String PROP_HOST = "host";
	public static final String PROP_FROM = "from";
	public static final String PROP_DEBUG = "debug";

	private static final Logger LOGGER = Logger.getLogger("blah"); // WTF, can't log otherwise. hmm
	private final BlockingQueue<LogRecord> queue = new LinkedBlockingQueue<LogRecord>();
	private final Thread worker = new Thread(this);
	private final Properties sessionProps = new Properties();
	
	private Address[] to, cc, bcc;

	boolean open = true;
	
	public EmailHandler() {
		this("");
	}
	
	public EmailHandler(String name) {
		super(name);
		setLevel(Logger.getLogger("").getLevel());		
		init();
		LogManager.getLogManager().addPropertyChangeListener(this);
		worker.setDaemon(true);
		worker.start();
	}
	
	@Override
	public void publish(LogRecord rec) {
		if (open && isLoggable(rec)) {
			queue.add(rec);
		}
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

	@Override
	public void close() throws SecurityException {
		try {
			open = false;
			LogManager.getLogManager().removePropertyChangeListener(this);
			queue.clear();
			worker.interrupt();
			worker.join();
		} catch (InterruptedException e) {
			LOGGER.log(Level.WARNING, "Trouble shutting down mail queue, sorry.", e);
		}
	}

	public void propertyChange(PropertyChangeEvent pce) {
		init();
	}
	
	private Address[] addresses(String list) {
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
	
	private void init() {

		// Addressees
		to = addresses(getProperty(PROP_TO));
		cc = addresses(getProperty(PROP_CC));
		bcc = addresses(getProperty(PROP_BCC));

		LOGGER.fine(this + ".to == " + Arrays.toString(to));
		LOGGER.fine(this + ".cc == " + Arrays.toString(cc));
		LOGGER.fine(this + ".bcc == " + Arrays.toString(bcc));
		
		// Level
		String level = getProperty(PROP_LEVEL);
		if (level != null) {
			try {
				setLevel(Level.parse(level));
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Could not set level: " + level);
			}
		}

		LOGGER.fine(this + ".level == " + getLevel());

		// Formatter
		String formatter = getProperty(PROP_FORMATTER);
		if (formatter != null && formatter.length() != 0) {
			try {
				setFormatter((Formatter) Class.forName(formatter).newInstance());
			} catch (Exception  e) {
				LOGGER.log(Level.WARNING, "Can't set formatter to " + formatter + "; using default.", e);
			}
		}
		setFormatter(new SimpleFormatter());
		LOGGER.fine(this + ".formatter == " + getFormatter().getClass().getName());

		// Mail properties
		setSessionProp("mail.debug", PROP_DEBUG);
		setSessionProp("mail.host", PROP_HOST);
		setSessionProp("mail.from", PROP_FROM);

		LOGGER.fine(this + ".debug == " + getProperty("debug"));
		LOGGER.fine(this + ".host == " + getProperty("host"));
		LOGGER.fine(this + ".from == " + getProperty("from"));

		
	}
	
	private void setSessionProp(String sessionProp, String configProp) {
		Object c = getProperty(configProp);
		if (c != null)
			sessionProps.put(sessionProp, c);
	}
	 
	public void run() {
		for (; open;) {
			try {
				LogRecord rec = queue.take();								
				Session session = Session.getInstance(sessionProps);				
				Message message = new MimeMessage(session);
				message.setRecipients(Message.RecipientType.TO, to);
				message.setRecipients(Message.RecipientType.CC, cc);
				message.setRecipients(Message.RecipientType.BCC, bcc);
				message.setSubject(rec.getLevel() + ": " + rec.getMessage());
				message.setText(getFormatter().format(rec));
				Transport.send(message);				
			} catch (InterruptedException ie) {			
			} catch (MessagingException e) {
				LOGGER.log(Level.WARNING, "Trouble sending message.", e);
			}
		}
	}
	
}
