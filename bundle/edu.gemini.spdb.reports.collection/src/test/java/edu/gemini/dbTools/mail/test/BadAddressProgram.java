//
// $Id: BadAddressProgram.java 4336 2004-01-20 07:57:42Z gillies $
//
package edu.gemini.dbTools.mail.test;

import edu.gemini.dbTools.mail.OdbMailConfig;

final class BadAddressProgram extends TestProgramBase {

    private final OdbMailConfig config;

    BadAddressProgram(OdbMailConfig mailConfig) {
        super("GS-2003B-EMAIL-001");
        config = mailConfig;
    }

    // No pi address
    protected String getPiAddressesStr() {
        return null;
    }

    // A bad ngo address
    protected String getNgoAddressesStr() {
        return "swalker@@gemini.edu";
    }

    // one bad gemini address, one good one
    protected String getGeminiAddressesStr() {
        return "geminiContact@fakeaddress.edu, illegal^char#s*(in)address!";
    }

    protected OdbMailConfig getMailConfig() {
        return config;
    }
}
