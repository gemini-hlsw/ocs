//
// $Id: AddressFetcher.java 893 2007-07-19 19:43:20Z swalker $
//

package edu.gemini.auxfile.workflow;

import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.gemini.obscomp.SPProgram;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class AddressFetcher {
    private static final Logger LOG = Logger.getLogger(AddressFetcher.class.getName());

    public enum Role {
        PI,
        NGO,
        CS,
    }

    public static final AddressFetcher INSTANCE = new AddressFetcher();

    private AddressFetcher() {
    }

    private static final List<InternetAddress> EMPTY_ADDRESS_LIST = Collections.emptyList();
    private static final Map<Role, List<InternetAddress>> EMPTY_ADDRESS_MAP;

    static {
        Map<Role, List<InternetAddress>> tmp = new TreeMap<Role, List<InternetAddress>>();
        for (Role r : Role.values()) {
            tmp.put(r, EMPTY_ADDRESS_LIST);
        }
        EMPTY_ADDRESS_MAP = Collections.unmodifiableMap(tmp);
    }

    public static List<InternetAddress> parseAddresses(String addrListStr, SPProgramID progId) {
        if ((addrListStr == null) || "".equals(addrListStr.trim())) {
            return EMPTY_ADDRESS_LIST;
        }

        List<InternetAddress> res = new ArrayList<InternetAddress>();
        StringTokenizer st = new StringTokenizer(addrListStr, " \t,;", false);

        while (st.hasMoreTokens()) {
            String addrStr = st.nextToken();

            try {
                InternetAddress ia = new InternetAddress(addrStr);
                ia.validate();
                res.add(ia);
            } catch (AddressException ex) {
                String msg = "Invalid address";
                if (progId != null) {
                    msg += " in program " + progId.toString();
                }
                msg += ": " + addrStr;
                LOG.log(Level.WARNING, msg, ex);
            }
        }

        return res;
    }

    public Map<Role, List<InternetAddress>> getProgramEmails(SPProgramID progId, IDBDatabaseService db) {
        ISPProgram prog = db.lookupProgramByID(progId);
        if (prog == null) return EMPTY_ADDRESS_MAP;

        SPProgram dataObj = (SPProgram) prog.getDataObject();

        Map<Role, List<InternetAddress>> res = new TreeMap<Role, List<InternetAddress>>();


        // add NGO
        res.put(Role.NGO, Collections.unmodifiableList(parseAddresses(dataObj.getPrimaryContactEmail(), progId)));

        // add contact scientist
        res.put(Role.CS, Collections.unmodifiableList(parseAddresses(dataObj.getContactPerson(), progId)));

        // add PI
        SPProgram.PIInfo piInfo = dataObj.getPIInfo();
        if (piInfo == null) {
            res.put(Role.PI, EMPTY_ADDRESS_LIST);
        } else {
            res.put(Role.PI, Collections.unmodifiableList(parseAddresses(piInfo.getEmail(), progId)));
        }

        return res;
    }
}
