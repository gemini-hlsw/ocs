//
// $Id: ProgramInternetAddresses.java 4336 2004-01-20 07:57:42Z gillies $
//
package edu.gemini.dbTools.mail;

import edu.gemini.dbTools.odbState.ProgramEmailAddresses;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class converts {@link ProgramEmailAddresses} into
 * JavaMail InternetAddresses.  The reason for splitting this
 * functionality into a separate class is to avoid having the
 * database functor that extracts email addesses reference anything
 * in JavaMail -- which would mean that the database would have to
 * download these classes when the functor runs.
 */
public class ProgramInternetAddresses {

    private final InternetAddress[] EMPTY_ADDRESSES = new InternetAddress[0];

    private final InternetAddress[] _pi;
    private final InternetAddress[] _ngo;
    private final InternetAddress[] _contact;

    public ProgramInternetAddresses(final Logger log, final ProgramEmailAddresses addresses) {
        _pi = convert(log, addresses.getPiAddresses());
        _ngo = convert(log, addresses.getNgoAddresses());
        _contact = convert(log, addresses.getGemContactAddresses());
    }

    private InternetAddress[] convert(final Logger log, final String[] strAddresses) {
        if (strAddresses == null) return EMPTY_ADDRESSES;
        if (strAddresses.length == 0) return EMPTY_ADDRESSES;

        final List<InternetAddress> lst = new ArrayList<InternetAddress>();
        for (final String strAddress : strAddresses) {
            try {
                final InternetAddress ia = new InternetAddress(strAddress);
                ia.validate();
                lst.add(ia);
            } catch (AddressException e) {
                log.warning("Could not parse email address: " + strAddress);
            }
        }

        return lst.toArray(EMPTY_ADDRESSES);
    }

    public InternetAddress[] getPiAddresses() {
        return _pi;
    }

    public InternetAddress[] getNgoAddresses() {
        return _ngo;
    }

    public InternetAddress[] getContactAddresses() {
        return _contact;
    }
}
