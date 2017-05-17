package edu.gemini.auxfile.workflow;

import edu.gemini.auxfile.copier.AuxFileType;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Mailer {
    private MailConfig _config;
    private MailTransport _transport;
    private IDBDatabaseService _odb;

    public Mailer(MailConfig config, IDBDatabaseService odb) {
        this(config, odb, Transport::send);
    }

    public Mailer(MailConfig config, IDBDatabaseService odb, MailTransport transport) {
        _config = config;
        _transport = transport;
        _odb = odb;
    }

    private List<InternetAddress> getStaticToAddresses(AuxFileType type) {
        return (type == AuxFileType.other) ? _config.getFinderRecipients() : _config.getMaskRecipients();
    }

    private void addAddresses(MimeMessage msg, Message.RecipientType recipientType, List<InternetAddress> addrs)
            throws MessagingException {
        if (addrs.size() == 0) return;
        InternetAddress[] a = addrs.toArray(new InternetAddress[addrs.size()]);
        msg.setRecipients(recipientType, a);
    }

    private MimeMessage createMessage(SPProgramID progId, AuxFileType type) throws MessagingException {
        // Create the message.
        Properties props = new Properties();
        props.setProperty("mail.host", _config.getSmtpHost());
        Session session = Session.getInstance(props);
        MimeMessage msg = new MimeMessage(session);

        // Set the sender and recipient.
        msg.setFrom(_config.getSender());

        // Send the message to the configured TO recipients, if any, plus the
        // contact scientists.
        Map<AddressFetcher.Role, List<InternetAddress>> progAddrs;
        progAddrs = AddressFetcher.INSTANCE.getProgramEmails(progId, _odb);

        List<InternetAddress> toAddrs = new ArrayList<InternetAddress>(getStaticToAddresses(type));
        toAddrs.addAll(progAddrs.get(AddressFetcher.Role.CS));

        Message.RecipientType remainingRecipientType = Message.RecipientType.TO;
        if (toAddrs.size() > 0) {
            addAddresses(msg, Message.RecipientType.TO, toAddrs);
            remainingRecipientType = Message.RecipientType.CC;
        }

        // CC the NGOs and PIs
        List<InternetAddress> ccAddrs = new ArrayList<InternetAddress>();
        ccAddrs.addAll(progAddrs.get(AddressFetcher.Role.NGO));
        ccAddrs.addAll(progAddrs.get(AddressFetcher.Role.PI));
        addAddresses(msg, remainingRecipientType, ccAddrs);

        return msg;
    }

    public void notifyStored(SPProgramID progId, AuxFileType type, String fileName)
            throws MessagingException {

        // Create the message.
        MimeMessage msg = createMessage(progId, type);

        // Set the subject.
        StringBuilder buf = new StringBuilder();
        buf.append(type.getDisplayName());
        buf.append(" file uploaded for program ");
        buf.append(progId.stringValue());
        msg.setSubject(buf.toString());

        // Set the message body.
        buf = new StringBuilder();
        buf.append("An auxiliary file (");
        buf.append(type.getDisplayName());
        buf.append(") has been uploaded or modified for program ");
        buf.append(progId.stringValue()).append(":\n\n");
        buf.append("\t").append(fileName);
        msg.setText(buf.toString());

        // Send the mail.
        _transport.send(msg);
    }

    public void notifyChecked(SPProgramID progId, AuxFileType type,
			String fileName, boolean checked) throws MessagingException {

        // Create the message.
        MimeMessage msg = createMessage(progId, type);

		// Set the subject.
		StringBuilder buf = new StringBuilder();
		buf.append(type.getDisplayName());
		buf.append(" checked => ");
		buf.append(checked);
		buf.append(" for program ");
		buf.append(progId.stringValue());
		msg.setSubject(buf.toString());

		// Set the message body.
		buf = new StringBuilder();
		buf.append("An auxiliary file (");
		buf.append(type.getDisplayName());
		buf.append(") has been marked as checked => ");
		buf.append(checked);
		buf.append(" for program ");
		buf.append(progId.stringValue()).append(":\n\n");
		buf.append("\t").append(fileName);
		msg.setText(buf.toString());

		// Send the mail.
		_transport.send(msg);
	}


}
