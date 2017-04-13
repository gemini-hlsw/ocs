//
// $Id: TestMailSender.java 4410 2004-02-02 00:51:35Z shane $
//
package edu.gemini.dbTools.mail.test;

import edu.gemini.shared.mail.MailSender;
import edu.gemini.shared.mail.MailUtil;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.List;
import java.util.ArrayList;

final class TestMailSender implements MailSender {
    private final List<Message> _msgList = new ArrayList<>();

    public void send(final Message msg) throws MessagingException {
        _msgList.add(msg);
    }

    public void clearMessages() {
        _msgList.clear();
    }

    public int getMessageCount() {
        return _msgList.size();
    }

    public boolean matchMessage(final Message msg) throws Exception {
        boolean matched = false;
        for (final Object a_msgList : _msgList) {
            final Message cur = (Message) a_msgList;
            if (MailUtil.equals(msg, cur)) {
                matched = true;
                break;
            }
        }
        return matched;
    }
}
