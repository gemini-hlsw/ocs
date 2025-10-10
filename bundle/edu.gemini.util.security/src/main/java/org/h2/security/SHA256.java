/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.security;

import java.util.Arrays;

/**
 * This class implements the cryptographic hash function SHA-256.
 */
public class SHA256 {

    /**
     * The first 32 bits of the fractional parts of the cube roots of the first
     * sixty-four prime numbers.
     */
    private static final int[] K = { 0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5, 0xd807aa98,
            0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe,
            0x9bdc06a7, 0xc19bf174, 0xe49b69c1, 0xefbe4786, 0x0fc19dc6,
            0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3,
            0xd5a79147, 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138,
            0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e,
            0x92722c85, 0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, 0x19a4c116,
            0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a,
            0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f, 0x84c87814,
            0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2 };

    /**
     * Calculate the hash code by using the given salt. The salt is appended
     * after the data before the hash code is calculated. After generating the
     * hash code, the data and all internal buffers are filled with zeros to avoid
     * keeping insecure data in memory longer than required (and possibly
     * swapped to disk).
     *
     * @param data the data to hash
     * @param salt the salt to use
     * @return the hash code
     */
    public static byte[] getHashWithSalt(byte[] data, byte[] salt) {
        byte[] buff = new byte[data.length + salt.length];
        System.arraycopy(data, 0, buff, 0, data.length);
        System.arraycopy(salt, 0, buff, data.length, salt.length);
        return getHash(buff, true);
    }

    /**
     * Calculate the hash of a password by prepending the user name and a '@'
     * character. Both the user name and the password are encoded to a byte
     * array using UTF-16. After generating the hash code, the password array
     * and all internal buffers are filled with zeros to avoid keeping the plain text
     * password in memory longer than required (and possibly swapped to disk).
     *
     * @param userName the user name
     * @param password the password
     * @return the hash code
     */
    public static byte[] getKeyPasswordHash(String userName, char[] password) {
        String user = userName + "@";
        byte[] buff = new byte[2 * (user.length() + password.length)];
        int n = 0;
        for (int i = 0, length = user.length(); i < length; i++) {
            char c = user.charAt(i);
            buff[n++] = (byte) (c >> 8);
            buff[n++] = (byte) c;
        }
        for (char c : password) {
            buff[n++] = (byte) (c >> 8);
            buff[n++] = (byte) c;
        }
        Arrays.fill(password, (char) 0);
        return getHash(buff, true);
    }

    /**
     * Calculate the hash code for the given data.
     *
     * @param data the data to hash
     * @param nullData if the data should be filled with zeros after calculating
     *            the hash code
     * @return the hash code
     */
    public static byte[] getHash(byte[] data, boolean nullData) {
        int byteLen = data.length;
        int intLen = ((byteLen + 9 + 63) / 64) * 16;
        byte[] bytes = new byte[intLen * 4];
        System.arraycopy(data, 0, bytes, 0, byteLen);
        if (nullData) {
            Arrays.fill(data, (byte) 0);
        }
        bytes[byteLen] = (byte) 0x80;
        int[] buff = new int[intLen];
        for (int i = 0, j = 0; j < intLen; i += 4, j++) {
            buff[j] = readInt(bytes, i);
        }
        buff[intLen - 2] = byteLen >>> 29;
        buff[intLen - 1] = byteLen << 3;
        int[] w = new int[64];
        int[] hh = { 0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
                0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19 };
        for (int block = 0; block < intLen; block += 16) {
            for (int i = 0; i < 16; i++) {
                w[i] = buff[block + i];
            }
            for (int i = 16; i < 64; i++) {
                int x = w[i - 2];
                int theta1 = rot(x, 17) ^ rot(x, 19) ^ (x >>> 10);
                x = w[i - 15];
                int theta0 = rot(x, 7) ^ rot(x, 18) ^ (x >>> 3);
                w[i] = theta1 + w[i - 7] + theta0 + w[i - 16];
            }

            int a = hh[0], b = hh[1], c = hh[2], d = hh[3];
            int e = hh[4], f = hh[5], g = hh[6], h = hh[7];

            for (int i = 0; i < 64; i++) {
                int t1 = h + (rot(e, 6) ^ rot(e, 11) ^ rot(e, 25))
                        + ((e & f) ^ ((~e) & g)) + K[i] + w[i];
                int t2 = (rot(a, 2) ^ rot(a, 13) ^ rot(a, 22))
                        + ((a & b) ^ (a & c) ^ (b & c));
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
            }
            hh[0] += a;
            hh[1] += b;
            hh[2] += c;
            hh[3] += d;
            hh[4] += e;
            hh[5] += f;
            hh[6] += g;
            hh[7] += h;
        }
        byte[] result = new byte[32];
        for (int i = 0; i < 8; i++) {
            writeInt(result, i * 4, hh[i]);
        }
        Arrays.fill(w, 0);
        Arrays.fill(buff, 0);
        Arrays.fill(hh, 0);
        Arrays.fill(bytes, (byte) 0);
        return result;
    }

    private static int rot(int i, int count) {
        return (i << (32 - count)) | (i >>> count);
    }

    private static int readInt(byte[] b, int i) {
        return ((b[i] & 0xff) << 24) + ((b[i + 1] & 0xff) << 16)
                + ((b[i + 2] & 0xff) << 8) + (b[i + 3] & 0xff);
    }

    private static void writeInt(byte[] b, int i, int value) {
        b[i] = (byte) (value >> 24);
        b[i + 1] = (byte) (value >> 16);
        b[i + 2] = (byte) (value >> 8);
        b[i + 3] = (byte) value;
    }

}
