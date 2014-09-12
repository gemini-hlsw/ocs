package edu.gemini.mail;

import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.URLName;

import com.sun.mail.smtp.SMTPTransport;

public class RetrySMTPTransport extends SMTPTransport {
	
	private static final Logger LOGGER = Logger.getLogger(RetrySMTPTransport.class.getName());
	private static RetryQueue queue;
		
	private boolean connected = false;

	public RetrySMTPTransport(Session session, URLName urlName) {
		super(session, urlName);
	}

	@Override
	public synchronized void sendMessage(Message msg, Address[] addrs) throws MessagingException, SendFailedException {
		if (connected) {			
			super.sendMessage(msg, addrs);
		} else {
			synchronized (RetrySMTPTransport.class) {
				if (queue != null) {
					queue.put(session, msg);
				} else {
					
					// Very unlikely that this would happen. The connect() will
					// fail in most cases. This is a very tight race condition.
					throw new MessagingException("Retry queue is not available.");
					
				}
			}
		}
	}
	
	@Override
	public synchronized void connect() throws MessagingException {
		try {
			super.connect();
			connected = true;
		} catch (MessagingException me) {
			if (RetrySMTPTransport.queue != null) {
				LOGGER.warning("Connect failed. Messages will be queued for retry.");
			} else {
				LOGGER.warning("Retry queue is not available.");
				throw me;
			}
		}
	}
	
	@Override
	public synchronized void close() throws MessagingException {
		connected = false;
		super.close();
	}
		
	public static void setQueue(RetryQueue queue) {
		RetrySMTPTransport.queue = queue;
	}
	
}
