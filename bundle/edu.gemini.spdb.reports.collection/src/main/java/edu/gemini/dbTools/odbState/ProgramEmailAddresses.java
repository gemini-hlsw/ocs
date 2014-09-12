//
// $Id: ProgramEmailAddresses.java 4336 2004-01-20 07:57:42Z gillies $
//
package edu.gemini.dbTools.odbState;

import java.util.logging.Logger;
import java.util.logging.Level;

//import javax.mail.internet.InternetAddress;
//import javax.mail.internet.AddressException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;



/**
 * A class that extracts and holds the relevant email addresses from a
 * science program.
 */
public final class ProgramEmailAddresses implements Serializable {
    static final long serialVersionUID = 1;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final String[] _pi;
    private final String[] _ngo;
    private final String[] _gemContact;

    public ProgramEmailAddresses(final String[] pi, final String[] ngo, final String[] gemContact) {
        _pi = _copy(pi);
        _ngo = _copy(ngo);
        _gemContact = _copy(gemContact);
    }

    public ProgramEmailAddresses(final String piList, final String ngoList, final String gemContactList) {
        _pi = parseAddressList(piList);
        _ngo = parseAddressList(ngoList);
        _gemContact = parseAddressList(gemContactList);
    }

    /**
     * Parses a comma, space, tab, or semicolon separated list of addresses
     * into a list of individual addresses.
     *
     * @param addresses list of addresses to parse, may be <code>null</code>
     *
     * @return individual addresses in the list
     */
    private static String[] parseAddressList(final String addresses) {
        if (addresses == null) return EMPTY_STRING_ARRAY;

        final StringTokenizer st = new StringTokenizer(addresses, " \t,;", false);
        final List<String> lst = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            lst.add(st.nextToken());
        }
        return lst.toArray(EMPTY_STRING_ARRAY);
    }

//    /**
//     * Converts a list of String email addresses into
//     * <tt>javax.mail.InternetAddress</tt>es.
//     *
//     * @param strAddresses string email addresses to convert; any addresses
//     * which cannot be parsed as valid InternetAddresses will be left out
//     * of the return value
//     *
//     * @return array of converted email addresses
//     */
//    public static InternetAddress[] toInternetAddresses(String[] strAddresses) {
//        if (strAddresses == null) return EMPTY_ADDRESSES;
//        if (strAddresses.length == 0) return EMPTY_ADDRESSES;
//
//        List lst = new ArrayList();
//        for (int i=0; i<strAddresses.length; ++i) {
//            String strAddress = strAddresses[i];
//
//            InternetAddress ia = toInternetAddress(strAddress);
//            if (ia != null) lst.add(ia);
//        }
//
//        return (InternetAddress[]) lst.toArray(EMPTY_ADDRESSES);
//    }

// --Commented out by Inspection START (8/12/13 3:05 PM):
//    /**
//     * Converts a single string email address into a
//     * <tt>javax.mail.InternetAddress</tt> if possible.  If the string address
//     * is not valid, then <code>null</code> is returned.
//     */
////    public static InternetAddress toInternetAddress(String address) {
////        InternetAddress ia = null;
////        try {
////            ia = new InternetAddress(address);
////            ia.validate();
////        } catch (AddressException e) {
////            LOG.log(Level.WARNING, "Could not parse email address: " + address);
////            ia = null;
////        }
////        return ia;
////    }
//
//    public String getFirstPiAddress() {
//        if (_pi == null) return null;
//        if (_pi.length == 0) return null;
//        return _pi[0];
//    }
// --Commented out by Inspection STOP (8/12/13 3:05 PM)

//    public InternetAddress getFirstPiInternetAddress() {
//        String addr = getFirstPiAddress();
//        if (addr == null) return null;
//        return toInternetAddress(addr);
//    }

    private static String[] _copy(final String[] addresses) {
        if (addresses == null) return EMPTY_STRING_ARRAY;
        if (addresses.length == 0) return EMPTY_STRING_ARRAY;
        final String[] res = new String[addresses.length];
        System.arraycopy(addresses, 0, res, 0, addresses.length);
        return res;
    }

    public String[] getPiAddresses() {
        return _copy(_pi);
    }

//    public InternetAddress[] getPiInternetAddresses() {
//        return toInternetAddresses(_pi);
//    }

    public String[] getNgoAddresses() {
        return _copy(_ngo);
    }

//    public InternetAddress[] getNgoInternetAddresses() {
//        return toInternetAddresses(_ngo);
//    }

    public String[] getGemContactAddresses() {
        return _copy(_gemContact);
    }

//    public InternetAddress[] getGemContactInternetAddresses() {
//        return toInternetAddresses(_gemContact);
//    }

    /*
    public int compareTo(Object other) {
        ProgramEmailAddresses that = (ProgramEmailAddresses) other;

        int res;

        res = compareArrays(_pi, that._pi);
        if (res != 0) return res;

        res = compareArrays(_ngo, that._ngo);
        if (res != 0) return res;

        res = compareArrays(_gemContact, that._gemContact);
        if (res != 0) return res;

        return 0;
    }

    public boolean equals(Object other) {
        if (other == null) return false;
        if (getClass() != other.getClass()) return false;

        ProgramEmailAddresses that = (ProgramEmailAddresses) other;
        if (compareArrays(_pi, that._pi) != 0) return false;
        if (compareArrays(_ngo, that._ngo) != 0) return false;
        if (compareArrays(_gemContact, that._gemContact) != 0) return false;

        return true;
    }

    public int hashCode() {
        int res = hashCode(_pi);
        res = 37*res + hashCode(_ngo);
        res = 37*res + hashCode(_gemContact);
        return res;
    }

    private int compareArrays(String[] a1, String[] a2) {
        if (a1 == null) {
            if (a2 == null) return 0;
            return -1;
        }
        if (a2 == null) return 1;

        int len = a1.length < a2.length ? a1.length : a2.length;
        for (int i=0; i<len; ++i) {
            int res = a1[i].compareTo(a2[i]);
            if (res != 0) return res;
        }

        return a1.length - a2.length;
    }

    private int hashCode(String[] a) {
        if (a == null) return 0;
        int res = 37;
        for (int i=0; i<a.length; ++i) {
            res = 37*res + a[i].hashCode();
        }
        return res;
    }
    */
}
