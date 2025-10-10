/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.security;

import org.h2.engine.Constants;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.util.MathUtils;

/**
 * A file store that encrypts all data before writing, and decrypts all data
 * after reading. Areas that were never written to (for example after calling
 * setLength to enlarge the file) are not encrypted (contains 0 bytes).
 */
public class SecureFileStore extends FileStore {

    private byte[] key;
    private final BlockCipher cipher;
    private final BlockCipher cipherForInitVector;
    private byte[] buffer = new byte[4];
    private long pos;
    private final byte[] bufferForInitVector;
    private final int keyIterations;

    public SecureFileStore(DataHandler handler, String name, String mode, String cipher, byte[] key, int keyIterations) {
        super(handler, name, mode);
        this.key = key;
        this.cipher = CipherFactory.getBlockCipher(cipher);
        this.cipherForInitVector = CipherFactory.getBlockCipher(cipher);
        this.keyIterations = keyIterations;
        bufferForInitVector = new byte[Constants.FILE_BLOCK_SIZE];
    }

    protected byte[] generateSalt() {
        return MathUtils.secureRandomBytes(Constants.FILE_BLOCK_SIZE);
    }

    protected void initKey(byte[] salt) {
        key = SHA256.getHashWithSalt(key, salt);
        for (int i = 0; i < keyIterations; i++) {
            key = SHA256.getHash(key, true);
        }
        cipher.setKey(key);
        key = SHA256.getHash(key, true);
        cipherForInitVector.setKey(key);
    }

    protected void writeDirect(byte[] b, int off, int len) {
        super.write(b, off, len);
        pos += len;
    }

    public void write(byte[] b, int off, int len) {
        if (buffer.length < b.length) {
            buffer = new byte[len];
        }
        System.arraycopy(b, off, buffer, 0, len);
        xorInitVector(buffer, 0, len, pos);
        cipher.encrypt(buffer, 0, len);
        super.write(buffer, 0, len);
        pos += len;
    }

    protected void readFullyDirect(byte[] b, int off, int len) {
        super.readFully(b, off, len);
        pos += len;
    }

    public void readFully(byte[] b, int off, int len) {
        super.readFully(b, off, len);
        for (int i = 0; i < len; i++) {
            if (b[i] != 0) {
                cipher.decrypt(b, off, len);
                xorInitVector(b, off, len, pos);
                break;
            }
        }
        pos += len;
    }

    public void seek(long x) {
        this.pos = x;
        super.seek(x);
    }

    private void xorInitVector(byte[] b, int off, int len, long p) {
        byte[] iv = bufferForInitVector;
        while (len > 0) {
            for (int i = 0; i < Constants.FILE_BLOCK_SIZE; i += 8) {
                long block = (p + i) >>> 3;
                iv[i] = (byte) (block >> 56);
                iv[i + 1] = (byte) (block >> 48);
                iv[i + 2] = (byte) (block >> 40);
                iv[i + 3] = (byte) (block >> 32);
                iv[i + 4] = (byte) (block >> 24);
                iv[i + 5] = (byte) (block >> 16);
                iv[i + 6] = (byte) (block >> 8);
                iv[i + 7] = (byte) block;
            }
            cipherForInitVector.encrypt(iv, 0, Constants.FILE_BLOCK_SIZE);
            for (int i = 0; i < Constants.FILE_BLOCK_SIZE; i++) {
                b[off + i] ^= iv[i];
            }
            p += Constants.FILE_BLOCK_SIZE;
            off += Constants.FILE_BLOCK_SIZE;
            len -= Constants.FILE_BLOCK_SIZE;
        }
    }

}
