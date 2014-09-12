//
// $Id: TestProgram.java 4336 2004-01-20 07:57:42Z gillies $
//
package edu.gemini.dbTools.mail.test;

import edu.gemini.dbTools.mail.OdbMailConfig;

final class TestProgram extends TestProgramBase {
    TestProgram() {
        super("GS-2003B-EMAIL-000");
    }

    protected String getPiAddressesStr() {
        return "jastro@fakeaddress.edu";
    }

    protected String getNgoAddressesStr() {
        return "ngo1@fakeaddress.edu, ngo2@fakeaddress.edu";
    }

    protected String getGeminiAddressesStr() {
        return "geminiContact@fakeaddress.edu";
    }

    protected OdbMailConfig getMailConfig() {
        throw new Error("Not implemented, sorry."); // TODO
    }

}