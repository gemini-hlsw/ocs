//
// $Id: TestProgramBase.java 47005 2012-07-26 22:35:47Z swalker $
//
package edu.gemini.dbTools.mail.test;

import edu.gemini.dbTools.mail.OdbMailConfig;
import edu.gemini.dbTools.mail.OdbMailTemplate;
import edu.gemini.dbTools.mail.PrepareMessageAction;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.shared.mail.MailTemplate;
import edu.gemini.shared.mail.MailUtil;
import edu.gemini.shared.util.GeminiRuntimeException;
import edu.gemini.spModel.core.Affiliate;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obs.SPObservation;

import javax.mail.Address;
import javax.mail.Message;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Base class for test programs used in testing the email sending agent.
 */
abstract class TestProgramBase {
    private final SPProgramID _progId;

    TestProgramBase(final String progId) {
        try {
            _progId = SPProgramID.toProgramID(progId);
        } catch (SPBadIDException ex) {
            throw GeminiRuntimeException.newException(ex);
        }
    }

    protected abstract String getPiAddressesStr();

    protected abstract String getNgoAddressesStr();

    protected abstract String getGeminiAddressesStr();

    protected abstract OdbMailConfig getMailConfig();

    Address[] getPiAddresses() {
        final String adrsStr = getPiAddressesStr();
        if (adrsStr == null) return MailUtil.EMPTY_ADDRESS_ARRAY;
        return MailUtil.parseAddresses(adrsStr);
    }

    Address[] getNgoAddresses() {
        final String adrsStr = getNgoAddressesStr();
        if (adrsStr == null) return MailUtil.EMPTY_ADDRESS_ARRAY;
        return MailUtil.parseAddresses(adrsStr);
    }

    Address[] getGeminiAddresses() {
        final String adrsStr = getGeminiAddressesStr();
        if (adrsStr == null) return MailUtil.EMPTY_ADDRESS_ARRAY;
        return MailUtil.parseAddresses(adrsStr);
    }

    // Creates a science program with three observations in the
    // "for review" state
    ISPProgram create(final IDBDatabaseService db) throws Exception {

        // First make sure this program doesn't already exist.
        ISPProgram prog = db.lookupProgramByID(_progId);
        if (prog != null) db.removeProgram(prog.getNodeKey());

        // Now create it from scratch.
        final ISPFactory fact = db.getFactory();
        prog = fact.createProgram(null, _progId);

        // Set the email addresses.
        final SPProgram progObj = (SPProgram) prog.getDataObject();
        final SPProgram.PIInfo piInfo;
        piInfo = new SPProgram.PIInfo("Joe", "Astronomer",
                                      getPiAddressesStr(), "666", Affiliate.UNITED_STATES);
        progObj.setPIInfo(piInfo);
        progObj.setPrimaryContactEmail(getNgoAddressesStr());
        progObj.setContactPerson(getGeminiAddressesStr());

        prog.setDataObject(progObj);


        // Create one observation in each of the following statuses.
        final ObservationStatus[] statusA =
                new ObservationStatus[]{
                    ObservationStatus.PHASE2,
                    ObservationStatus.FOR_REVIEW,
                    ObservationStatus.FOR_ACTIVATION,
                    ObservationStatus.READY,
                };

        final List<ISPObservation> obsList = new ArrayList<ISPObservation>();
        for (final ObservationStatus status : statusA) {
            for (int j = 0; j < 1; ++j) {
                final ISPObservation obs = fact.createObservation(prog, null);
                final SPObservation obsObj = (SPObservation) obs.getDataObject();
                obsObj.setPhase2Status(status.phase2());
                obs.setDataObject(obsObj);
                obsList.add(obs);
            }
        }

        prog.setObservations(obsList);

        return prog;
    }

    private static Address[] concat(Address[] a, Address[] b) {
        final Address[] res = new Address[a.length + b.length];
        System.arraycopy(a, 0, res, 0,        a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    Message createDown_ForReview(final List obsList) throws Exception {
        final String subject = PrepareMessageAction.getSubject_Down_ForReview(_progId);
        Address[] toAddresses = getNgoAddresses();
        Address[] ccAddresses = concat(getGeminiAddresses(), getPiAddresses());
        if (toAddresses.length == 0) {
            toAddresses = ccAddresses;
            ccAddresses = MailUtil.EMPTY_ADDRESS_ARRAY;
        }

        return createTestMessage(OdbMailTemplate.DOWN_FOR_REVIEW,
                                 toAddresses, ccAddresses, subject, obsList);
    }

    Message createDown_Phase2(final List obsList) throws Exception {
        final String subject = PrepareMessageAction.getSubject_Down_Phase2(_progId);
        final Address[] toAddresses = getPiAddresses();
        final Address[] ccAddresses = concat(getGeminiAddresses(), getNgoAddresses());

        return createTestMessage(OdbMailTemplate.DOWN_PHASE2,
                                 toAddresses, ccAddresses, subject, obsList);
    }

    Message createUp_ForActivation(final List obsList) throws Exception {
        final String subject = PrepareMessageAction.getSubject_Up_ForActivation(_progId);
        final Address[] toAddresses = getGeminiAddresses();
        final Address[] ccAddresses = concat(getPiAddresses(), getNgoAddresses());

        return createTestMessage(OdbMailTemplate.UP_FOR_ACTIVATION,
                                 toAddresses, ccAddresses, subject, obsList);
    }

    Message createUp_ForReview(final List obsList) throws Exception {
        final String subject = PrepareMessageAction.getSubject_Up_ForReview(_progId);
        Address[] toAddresses = getNgoAddresses();
        Address[] ccAddresses = concat(getGeminiAddresses(), getPiAddresses());
        if (toAddresses.length == 0) {
            toAddresses = ccAddresses;
            ccAddresses = MailUtil.EMPTY_ADDRESS_ARRAY;
        }

        return createTestMessage(OdbMailTemplate.UP_FOR_REVIEW,
                                 toAddresses, ccAddresses, subject, obsList);
    }

    Message createAny_On_Hold(final List obsList) throws Exception {
        final String subject = PrepareMessageAction.getSubject_On_Hold(_progId);
        final Address[] toAddresses = getPiAddresses();
        final Address[] ccAddresses = concat(getGeminiAddresses(), getNgoAddresses());

        return createTestMessage(OdbMailTemplate.ON_HOLD,
                                 toAddresses, ccAddresses, subject, obsList);
    }

    Message createUp_Ready(final List obsList) throws Exception {
        final String subject = PrepareMessageAction.getSubject_Up_Ready(_progId);
        final Address[] toAddresses = getPiAddresses();
        final Address[] ccAddresses = concat(getGeminiAddresses(), getNgoAddresses());

        return createTestMessage(OdbMailTemplate.UP_READY,
                                 toAddresses, ccAddresses, subject, obsList);
    }

    Message createTestMessage(final MailTemplate tmpl,
                              final Address[] toAddresses, final Address[] ccAddresses,
                              final String subject, final List obsIdList)
            throws Exception {

        final Message msg = getMailConfig().createMessage();
        msg.setFrom(getMailConfig().sender);
        msg.setSubject(subject);
        msg.setRecipients(Message.RecipientType.TO, toAddresses);
        msg.setRecipients(Message.RecipientType.CC, ccAddresses);

        final Properties props = new Properties();
        props.put(OdbMailTemplate.PROG_ID_VAR, _progId.toString());

        final boolean plural = obsIdList.size() > 1;
        MailTemplate.addPluralProperties(props, plural);

        final StringBuilder buf = new StringBuilder();
        for (Iterator it = obsIdList.iterator(); it.hasNext();) {
            final String obsIdStr = it.next().toString();
            buf.append("\t").append(obsIdStr);
            if (it.hasNext()) buf.append("\n");
        }
        props.put(OdbMailTemplate.OBS_ID_LIST_VAR, buf.toString());

        final String text = tmpl.subsitute(props);
        msg.setText(text);

        return msg;
    }
}
