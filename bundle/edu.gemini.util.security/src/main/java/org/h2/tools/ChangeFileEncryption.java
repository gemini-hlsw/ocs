/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.tools;

import java.sql.SQLException;
import java.util.ArrayList;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.store.FileLister;
import org.h2.store.FileStore;
import org.h2.store.fs.FileUtils;
import org.h2.util.Tool;

/**
 * Allows changing the database file encryption password or algorithm.
 * <br />
 * This tool can not be used to change a password of a user.
 * The database must be closed before using this tool.
 * @h2.resource
 */
public class ChangeFileEncryption extends Tool {

    private String directory;
    private String cipherType;
    private byte[] decrypt;
    private byte[] encrypt;

    /**
     * Options are case sensitive. Supported options are:
     * <table>
     * <tr><td>[-help] or [-?]</td>
     * <td>Print the list of options</td></tr>
     * <tr><td>[-cipher type]</td>
     * <td>The encryption type (AES or XTEA)</td></tr>
     * <tr><td>[-dir &lt;dir&gt;]</td>
     * <td>The database directory (default: .)</td></tr>
     * <tr><td>[-db &lt;database&gt;]</td>
     * <td>Database name (all databases if not set)</td></tr>
     * <tr><td>[-decrypt &lt;pwd&gt;]</td>
     * <td>The decryption password (if not set: not yet encrypted)</td></tr>
     * <tr><td>[-encrypt &lt;pwd&gt;]</td>
     * <td>The encryption password (if not set: do not encrypt)</td></tr>
     * <tr><td>[-quiet]</td>
     * <td>Do not print progress information</td></tr>
     * </table>
     * @h2.resource
     *
     * @param args the command line arguments
     */
    public static void main(String... args) throws SQLException {
        new ChangeFileEncryption().runTool(args);
    }

    public void runTool(String... args) throws SQLException {
        String dir = ".";
        String cipher = null;
        char[] decryptPassword = null;
        char[] encryptPassword = null;
        String db = null;
        boolean quiet = false;
        for (int i = 0; args != null && i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-dir")) {
                dir = args[++i];
            } else if (arg.equals("-cipher")) {
                cipher = args[++i];
            } else if (arg.equals("-db")) {
                db = args[++i];
            } else if (arg.equals("-decrypt")) {
                decryptPassword = args[++i].toCharArray();
            } else if (arg.equals("-encrypt")) {
                encryptPassword = args[++i].toCharArray();
            } else if (arg.equals("-quiet")) {
                quiet = true;
            } else if (arg.equals("-help") || arg.equals("-?")) {
                showUsage();
                return;
            } else {
                showUsageAndThrowUnsupportedOption(arg);
            }
        }
        if ((encryptPassword == null && decryptPassword == null) || cipher == null) {
            showUsage();
            throw new SQLException("Encryption or decryption password not set, or cipher not set");
        }
        try {
            process(dir, db, cipher, decryptPassword, encryptPassword, quiet);
        } catch (Exception e) {
            throw DbException.toSQLException(e);
        }
    }

    /**
     * Get the file encryption key for a given password.
     * The password must be supplied as char arrays and is cleaned in this method.
     *
     * @param password the password as a char array
     * @return the encryption key
     */
    private static byte[] getFileEncryptionKey(char[] password) {
        if (password == null) {
            return null;
        }
        return SHA256.getKeyPasswordHash("file", password);
    }

    /**
     * Changes the password for a database.
     * The passwords must be supplied as char arrays and are cleaned in this method.
     * The database must be closed before calling this method.
     *
     * @param dir the directory (. for the current directory)
     * @param db the database name (null for all databases)
     * @param cipher the cipher (AES, XTEA)
     * @param decryptPassword the decryption password as a char array
     * @param encryptPassword the encryption password as a char array
     * @param quiet don't print progress information
     * @throws SQLException
     */
    public static void execute(String dir, String db, String cipher,
            char[] decryptPassword, char[] encryptPassword, boolean quiet) throws SQLException {
        try {
            new ChangeFileEncryption().process(dir, db, cipher, decryptPassword, encryptPassword, quiet);
        } catch (Exception e) {
            throw DbException.toSQLException(e);
        }
    }

    private void process(String dir, String db, String cipher,
            char[] decryptPassword, char[] encryptPassword, boolean quiet) throws SQLException {
        dir = FileLister.getDir(dir);
        ChangeFileEncryption change = new ChangeFileEncryption();
        if (encryptPassword != null) {
            for (char c : encryptPassword) {
                if (c == ' ') {
                    throw new SQLException("The file password may not contain spaces");
                }
            }
        }
        change.out = out;
        change.directory = dir;
        change.cipherType = cipher;
        change.decrypt = getFileEncryptionKey(decryptPassword);
        change.encrypt = getFileEncryptionKey(encryptPassword);

        ArrayList<String> files = FileLister.getDatabaseFiles(dir, db, true);
        FileLister.tryUnlockDatabase(files, "encryption");
        files = FileLister.getDatabaseFiles(dir, db, false);
        if (files.size() == 0 && !quiet) {
            printNoDatabaseFilesFound(dir, db);
        }
        // first, test only if the file can be renamed
        // (to find errors with locked files early)
        for (String fileName : files) {
            String temp = dir + "/temp.db";
            FileUtils.delete(temp);
            FileUtils.moveTo(fileName, temp);
            FileUtils.moveTo(temp, fileName);
        }
        // if this worked, the operation will (hopefully) be successful
        // TODO changeFileEncryption: this is a workaround!
        // make the operation atomic (all files or none)
        for (String fileName : files) {
            // Don't process a lob directory, just the files in the directory.
            if (!FileUtils.isDirectory(fileName)) {
                change.process(fileName);
            }
        }
    }

    private void process(String fileName) {
        FileStore in;
        if (decrypt == null) {
            in = FileStore.open(null, fileName, "r");
        } else {
            in = FileStore.open(null, fileName, "r", cipherType, decrypt);
        }
        in.init();
        copy(fileName, in, encrypt);
    }

    private void copy(String fileName, FileStore in, byte[] key) {
        if (FileUtils.isDirectory(fileName)) {
            return;
        }
        String temp = directory + "/temp.db";
        FileUtils.delete(temp);
        FileStore fileOut;
        if (key == null) {
            fileOut = FileStore.open(null, temp, "rw");
        } else {
            fileOut = FileStore.open(null, temp, "rw", cipherType, key);
        }
        fileOut.init();
        byte[] buffer = new byte[4 * 1024];
        long remaining = in.length() - FileStore.HEADER_LENGTH;
        long total = remaining;
        in.seek(FileStore.HEADER_LENGTH);
        fileOut.seek(FileStore.HEADER_LENGTH);
        long time = System.currentTimeMillis();
        while (remaining > 0) {
            if (System.currentTimeMillis() - time > 1000) {
                out.println(fileName + ": " + (100 - 100 * remaining / total) + "%");
                time = System.currentTimeMillis();
            }
            int len = (int) Math.min(buffer.length, remaining);
            in.readFully(buffer, 0, len);
            fileOut.write(buffer, 0, len);
            remaining -= len;
        }
        in.close();
        fileOut.close();
        FileUtils.delete(fileName);
        FileUtils.moveTo(temp, fileName);
    }

}
