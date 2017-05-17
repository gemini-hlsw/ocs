package edu.gemini.dbTools.odbState;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.gemini.obscomp.SPProgram;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that extracts and holds the relevant email addresses from a
 * science program.
 */
public class ProgramContact implements Serializable {
    static final long serialVersionUID = 1;


    private static final String EMPTY_STRING[] = {};

    public static final String XML_CONTACT_ELEMENT = "contact";
    private static final String XML_PI_ELEMENT = "pi";
    private static final String XML_PI_FNAME_ELEMENT = "fname";
    private static final String XML_PI_LNAME_ELEMENT = "lname";
    private static final String XML_PI_EMAIL_ELEMENT = "email";
    private static final String XML_NGO_EMAIL_ELEMENT = "ngoEmail";
    private static final String XML_GEM_EMAIL_ELEMENT = "gemEmail";

    private String _firstName;
    private String _lastName;
    private final ProgramEmailAddresses _emailAddresses;

    public ProgramContact(final ISPProgram prog)  {
        final SPProgram obj = (SPProgram) prog.getDataObject();
        final SPProgram.PIInfo piInfo = obj.getPIInfo();

        _firstName = piInfo.getFirstName();
        _lastName = piInfo.getLastName();

        final String piAddrs;
        piAddrs = piInfo.getEmail();
        final String ngoAddrs = obj.getPrimaryContactEmail();
        final String gemContactAddrs = obj.getContactPerson();

        _emailAddresses = new ProgramEmailAddresses(piAddrs, ngoAddrs, gemContactAddrs);
    }

    /**
     * Parses the email addresses from the "contact" element
     */
    public ProgramContact(final Element contact) {
        String[] piAddrs = null;
        final Element pi = contact.element(XML_PI_ELEMENT);
        if (pi != null) {
            final Element fname = pi.element(XML_PI_FNAME_ELEMENT);
            if (fname != null) _firstName = fname.getTextTrim();
            final Element lname = pi.element(XML_PI_LNAME_ELEMENT);
            if (lname != null) _lastName = lname.getTextTrim();
            piAddrs = _parseEmails(pi, XML_PI_EMAIL_ELEMENT);
        }

        final String[] ngoAddrs = _parseEmails(contact, XML_NGO_EMAIL_ELEMENT);
        final String[] gemAddrs = _parseEmails(contact, XML_GEM_EMAIL_ELEMENT);

        _emailAddresses = new ProgramEmailAddresses(piAddrs, ngoAddrs, gemAddrs);
    }

    public Element toElement(final DocumentFactory fact) {
        final Element contact = fact.createElement(XML_CONTACT_ELEMENT);

        final Element pi = contact.addElement(XML_PI_ELEMENT);

        final Element fname = pi.addElement(XML_PI_FNAME_ELEMENT);
        if (fname != null) fname.setText(_firstName);

        final Element lname = pi.addElement(XML_PI_LNAME_ELEMENT);
        if (lname != null) lname.setText(_lastName);

        _addAddrElements(pi, XML_PI_EMAIL_ELEMENT, _emailAddresses.getPiAddresses());
        _addAddrElements(contact, XML_NGO_EMAIL_ELEMENT, _emailAddresses.getNgoAddresses());
        _addAddrElements(contact, XML_GEM_EMAIL_ELEMENT, _emailAddresses.getGemContactAddresses());

        return contact;
    }

    private static void _addAddrElements(final Element parent, final String emailElementName, final String[] addrs) {
        if (addrs == null) return;
        for (final String addr : addrs) {
            final Element e = parent.addElement(emailElementName);
            e.setText(addr);
        }
    }

    private static String[] _parseEmails(final Element parent, final String emailElementName) {
        List<String> res = null;

        final List<Element> addrElements = parent.elements(emailElementName);
        if (addrElements != null) {
            res = new ArrayList<String>(addrElements.size());
            for (final Element addrElement : addrElements) {
                final String addr = addrElement.getTextTrim();
                if (addr != null) res.add(addr);
            }
        }

        if (res == null) return EMPTY_STRING;
        return res.toArray(EMPTY_STRING);
    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String getPiFirstName() {
//        return _firstName;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    public String getPiLastName() {
//        return _lastName;
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

    public ProgramEmailAddresses getEmailAddresses() {
        return _emailAddresses;
    }

    /*
    public int compareTo(Object other) {
        ProgramContact that = (ProgramContact) other;

        int res = 0;
        if (_lastName == null) {
            if (that._lastName != null) return -1;
        } else {
            if (that._lastName == null) return 1;
            res = _lastName.compareTo(that._lastName);
        }
        if (res != 0) return res;

        if (_firstName == null) {
            if (that._firstName != null) return -1;
        } else {
            if (that._firstName == null) return 1;
            res = _firstName.compareTo(that._firstName);
        }
        if (res != 0) return res;
        return _emailAddresses.compareTo(that._emailAddresses);
    }
    */
}
