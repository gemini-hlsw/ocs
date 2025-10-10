/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import org.h2.command.Prepared;
import org.h2.constant.ErrorCode;
import org.h2.constant.SysProperties;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.security.SHA256;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.FileStoreInputStream;
import org.h2.store.FileStoreOutputStream;
import org.h2.store.LobStorage;
import org.h2.store.fs.FileUtils;
import org.h2.tools.CompressTool;
import org.h2.util.IOUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.TempFileDeleter;

/**
 * This class is the base for RunScriptCommand and ScriptCommand.
 */
abstract class ScriptBase extends Prepared implements DataHandler {

    /**
     * The default name of the script file if .zip compression is used.
     */
    private static final String SCRIPT_SQL = "script.sql";

    /**
     * The output stream.
     */
    protected OutputStream out;

    /**
     * The input stream.
     */
    protected InputStream in;

    /**
     * The file name (if set).
     */
    private Expression fileNameExpr;

    private Expression password;

    private String fileName;

    private String cipher;
    private FileStore store;
    private String compressionAlgorithm;

    ScriptBase(Session session) {
        super(session);
    }

    public void setCipher(String c) {
        cipher = c;
    }

    private boolean isEncrypted() {
        return cipher != null;
    }

    public void setPassword(Expression password) {
        this.password = password;
    }

    public void setFileNameExpr(Expression file) {
        this.fileNameExpr = file;
    }

    protected String getFileName() {
        if (fileNameExpr != null && fileName == null) {
            fileName = fileNameExpr.optimize(session).getValue(session).getString();
            if (fileName == null || fileName.trim().length() == 0) {
                fileName = "script.sql";
            }
            fileName = SysProperties.getScriptDirectory() + fileName;
        }
        return fileName;
    }

    public boolean isTransactional() {
        return false;
    }

    /**
     * Delete the target file.
     */
    void deleteStore() {
        String file = getFileName();
        if (file != null) {
            FileUtils.delete(file);
        }
    }

    private void initStore() {
        Database db = session.getDatabase();
        byte[] key = null;
        if (cipher != null && password != null) {
            char[] pass = password.optimize(session).getValue(session).getString().toCharArray();
            key = SHA256.getKeyPasswordHash("script", pass);
        }
        String file = getFileName();
        store = FileStore.open(db, file, "rw", cipher, key);
        store.setCheckedWriting(false);
        store.init();
    }

    /**
     * Open the output stream.
     */
    void openOutput() {
        String file = getFileName();
        if (file == null) {
            return;
        }
        if (isEncrypted()) {
            initStore();
            out = new FileStoreOutputStream(store, this, compressionAlgorithm);
            // always use a big buffer, otherwise end-of-block is written a lot
            out = new BufferedOutputStream(out, Constants.IO_BUFFER_SIZE_COMPRESS);
        } else {
            OutputStream o = FileUtils.newOutputStream(file, false);
            out = new BufferedOutputStream(o, Constants.IO_BUFFER_SIZE);
            out = CompressTool.wrapOutputStream(out, compressionAlgorithm, SCRIPT_SQL);
        }
    }

    /**
     * Open the input stream.
     */
    void openInput() {
        String file = getFileName();
        if (file == null) {
            return;
        }
        if (isEncrypted()) {
            initStore();
            in = new FileStoreInputStream(store, this, compressionAlgorithm != null, false);
        } else {
            InputStream inStream;
            try {
                inStream = FileUtils.newInputStream(file);
            } catch (IOException e) {
                throw DbException.convertIOException(e, file);
            }
            in = new BufferedInputStream(inStream, Constants.IO_BUFFER_SIZE);
            in = CompressTool.wrapInputStream(in, compressionAlgorithm, SCRIPT_SQL);
            if (in == null) {
                throw DbException.get(ErrorCode.FILE_NOT_FOUND_1, SCRIPT_SQL + " in " + file);
            }
        }
    }

    /**
     * Close input and output streams.
     */
    void closeIO() {
        IOUtils.closeSilently(out);
        out = null;
        IOUtils.closeSilently(in);
        in = null;
        if (store != null) {
            store.closeSilently();
            store = null;
        }
    }

    public boolean needRecompile() {
        return false;
    }

    public String getDatabasePath() {
        return null;
    }

    public FileStore openFile(String name, String mode, boolean mustExist) {
        return null;
    }

    public void checkPowerOff() {
        session.getDatabase().checkPowerOff();
    }

    public void checkWritingAllowed() {
        session.getDatabase().checkWritingAllowed();
    }

    public int getMaxLengthInplaceLob() {
        return session.getDatabase().getMaxLengthInplaceLob();
    }

    public TempFileDeleter getTempFileDeleter() {
        return session.getDatabase().getTempFileDeleter();
    }

    public String getLobCompressionAlgorithm(int type) {
        return session.getDatabase().getLobCompressionAlgorithm(type);
    }

    public void setCompressionAlgorithm(String algorithm) {
        this.compressionAlgorithm = algorithm;
    }

    public Object getLobSyncObject() {
        return this;
    }

    public SmallLRUCache<String, String[]> getLobFileListCache() {
        return null;
    }

    public LobStorage getLobStorage() {
        return null;
    }

    public Connection getLobConnection() {
        return null;
    }

    public int readLob(long lobId, byte[] hmac, long offset, byte[] buff, int off, int length) {
        throw DbException.throwInternalError();
    }

}
