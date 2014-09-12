package edu.gemini.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public class RetryQueue extends Thread implements FilenameFilter {

	private static final Logger LOGGER = Logger.getLogger(RetryQueue.class.getName());

	private static final long INTERVAL = 1000 * 60; // 1 minute
	private static final long TIMEOUT  = 1000 * 60 * 60 * 24; // 1 day

	private static final String MESSAGE_SUFFIX = ".message";
	private static final String SESSION_SUFFIX = ".session";

	private static final String HEADER_ENQUEUED = "X-Gemini-Enqueued";

	private final File dir;

	private volatile boolean trouble;
	private volatile boolean done;

	public RetryQueue(File dir) {
		this.dir = dir;
        if (!dir.isDirectory()) {
            LOGGER.log(Level.WARNING, dir.getAbsolutePath() + " is not a directory.");
        }
		setDaemon(true);
	}

	public void open() {
		LOGGER.info("Starting SMTP retry queue on " + dir);
		done = false;
		start();
	}

	public void close() {
		LOGGER.info("Closing SMTP retry queue...");
		done = true;
		while (isAlive()) {
			try {
				interrupt();
				join();
			} catch (InterruptedException ie) {
			}
		}
		LOGGER.info("Done.");
	}

	public synchronized void put(Session session, Message _message) throws MessagingException {

		if (!(_message instanceof MimeMessage))
			throw new MessagingException("Can't queue a non-MimeMessage, sorry");

		MimeMessage message = (MimeMessage) _message;

		LOGGER.fine("Queueing " + message.getMessageID());
		File messageFile = null, sessionFile = null;
		try {

			// By setting trouble to true, we are indicating that a failure
			// happened recently. This will cause the retry loop to early-exit
			// and sleep again; otherwise the loop will spin, re-queueing over
			// and over until the server comes back.
			trouble = true;

			// Set the enqueued header if it's not there already.
			if (message.getHeader(HEADER_ENQUEUED) == null)
				message.setHeader(HEADER_ENQUEUED, Long.toString(System.currentTimeMillis()));

//			// Add a header to record this retry attempt. If there are several
//			// retries, there will be several of these headers.
//			message.addHeader(HEADER_ATTEMPT, new Date().toString());
//			message.saveChanges();

			// Store the message.
			messageFile = new File(dir, message.getMessageID() + MESSAGE_SUFFIX);
			OutputStream os = new FileOutputStream(messageFile);
			message.writeTo(os);
			os.close();

			// Store the session.
			sessionFile = new File(dir, message.getMessageID() + SESSION_SUFFIX);
			OutputStream os2 = new FileOutputStream(sessionFile);
			session.getProperties().store(os2, "");
			os2.close();

		} catch (IOException ioe) {

			// Clean up the files if they exist.
			if (messageFile != null && messageFile.exists()) messageFile.delete();
			if (sessionFile != null && sessionFile.exists()) sessionFile.delete();

			// And re-throw as a MessagingException
			throw new MessagingException("Problem queueing message.", ioe);

		}
	}

	public synchronized MimeMessage take() {

		File[] all = dir.listFiles(this);
		if (all != null && all.length > 0) {

			LOGGER.fine("Retry queue size is " + all.length);

			File messageFile = null, sessionFile = null;
			try {

				// The file filter ensures that we will only get .message files,
				// but we also need to get the session file. This is pretty
				// lame but on the other hand it's very straightforward. We want
				// to take the oldest file. This way we guarantee churning;
				// eventually every file will get looked at.
				for (File f: all) {
					if (messageFile == null || messageFile.lastModified() > f.lastModified()) {
						messageFile = f;
					}
				}
				sessionFile = sessionFile(messageFile);

				// Reconstitute the session.
				Properties sessionProps = new Properties();
				sessionProps.load(new FileInputStream(sessionFile));
				Session session = Session.getInstance(sessionProps);

				// And the message.
				MimeMessage msg = new MimeMessage(session, new FileInputStream(messageFile));
				LOGGER.fine("Dequeued " + msg.getMessageID());
				return msg;

			} catch (Exception e) {

				// Not much we can do here. If we can't reconstitute the message
				// from disk, we're screwed. Just punt.
				LOGGER.log(Level.SEVERE, "Message was corrupted, sorry.", e);

			} finally {

				// Clean up the files, whether it worked or not. We don't want
				// them hanging around in either case.
				if (messageFile != null && messageFile.exists()) messageFile.delete();
				if (sessionFile != null && sessionFile.exists()) sessionFile.delete();

			}

		}
		return null;

	}

	public boolean accept(File dir, String name) {
		return name.endsWith(MESSAGE_SUFFIX);
	}

	public File sessionFile(File messageFile) {
		String messageFileName = messageFile.getName();
		String sessionFileName = messageFileName.substring(0, messageFileName.length() - MESSAGE_SUFFIX.length()) + SESSION_SUFFIX;
		return new File(messageFile.getParentFile(), sessionFileName);
	}

	@Override
	public void run() {
		while (!done) {
			try {
				Thread.sleep(INTERVAL);
				for (trouble = false; !trouble; ) {
					MimeMessage msg = take();
					if (msg != null) {

						// See if the message should be timed out.
						String[] senq = msg.getHeader(HEADER_ENQUEUED);
						if (senq != null) {
							long enq = Long.parseLong(senq[0]);
							if (System.currentTimeMillis() - enq > TIMEOUT) {
								LOGGER.warning("Timeout. Discarding " + msg.getMessageID());
								continue;
							}
						}

						Transport.send(msg);
					}
					else
						break;
				}
			} catch (InterruptedException e) {
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Trouble in retry loop.", t);
			}
		}
	}

}
