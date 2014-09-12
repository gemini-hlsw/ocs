//
// $Id: ProgIdHash.java 7456 2006-11-24 13:45:54Z shane $
//

package edu.gemini.util.security.auth;

import edu.gemini.spModel.core.SPProgramID;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides a password based upon the program id. This provides legacy program keys
 * that can be used instead of a PI email key. Note that these are generated independently
 * by ITAC, so the passed key needs to be the same (!).
 */
public class ProgIdHash {

    static final String HASH = "MD5";
    static final String ENC = "UTF-8";
    static final int LENGTH = 5;

    private final MessageDigest md;
    private final char[] key;

    public ProgIdHash(String key) {
        if (key == null)
            throw new IllegalArgumentException("ProgIdHash cannot be null.");
        this.key = key.toCharArray();
        try {
            md = MessageDigest.getInstance(HASH);
        } catch (NoSuchAlgorithmException e) {
            throw new Error(HASH + " Unavailable.");
        }
    }

    public String pass(SPProgramID progId) {
        return pass(progId.toString());
    }

    public String pass(String progId) {
        try {
            byte[] digest = md.digest(progId.getBytes(ENC));
            char[] pass = new char[LENGTH];
            for (int i = 0; i < pass.length; i++) {
                pass[i] = key[Math.abs(digest[i]) % key.length];
            }
            return new String(pass);
        } catch (UnsupportedEncodingException uee) {
            throw new Error(ENC + " Unavailable.");
        }
    }

}
