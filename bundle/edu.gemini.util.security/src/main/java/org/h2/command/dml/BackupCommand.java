/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.h2.api.DatabaseEventListener;
import org.h2.command.CommandInterface;
import org.h2.command.Prepared;
import org.h2.constant.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.message.DbException;
import org.h2.result.ResultInterface;
import org.h2.store.FileLister;
import org.h2.store.PageStore;
import org.h2.store.fs.FileUtils;
import org.h2.util.IOUtils;

/**
 * This class represents the statement
 * BACKUP
 */
public class BackupCommand extends Prepared {

    private Expression fileNameExpr;

    public BackupCommand(Session session) {
        super(session);
    }

    public void setFileName(Expression fileName) {
        this.fileNameExpr = fileName;
    }

    public int update() {
        String name = fileNameExpr.getValue(session).getString();
        session.getUser().checkAdmin();
        backupTo(name);
        return 0;
    }

    private void backupTo(String fileName) {
        Database db = session.getDatabase();
        if (!db.isPersistent()) {
            throw DbException.get(ErrorCode.DATABASE_IS_NOT_PERSISTENT);
        }
        try {
            String name = db.getName();
            name = FileUtils.getName(name);
            OutputStream zip = FileUtils.newOutputStream(fileName, false);
            ZipOutputStream out = new ZipOutputStream(zip);
            db.flush();
            String fn = db.getName() + Constants.SUFFIX_PAGE_FILE;
            backupPageStore(out, fn, db.getPageStore());
            // synchronize on the database, to avoid concurrent temp file
            // creation / deletion / backup
            String base = FileUtils.getParent(fn);
            synchronized (db.getLobSyncObject()) {
                String prefix = db.getDatabasePath();
                String dir = FileUtils.getParent(prefix);
                dir = FileLister.getDir(dir);
                ArrayList<String> fileList = FileLister.getDatabaseFiles(dir, name, true);
                for (String n : fileList) {
                    if (n.endsWith(Constants.SUFFIX_LOB_FILE)) {
                        backupFile(out, base, n);
                    }
                }
            }
            out.close();
            zip.close();
        } catch (IOException e) {
            throw DbException.convertIOException(e, fileName);
        }
    }

    private void backupPageStore(ZipOutputStream out, String fileName, PageStore store) throws IOException {
        Database db = session.getDatabase();
        fileName = FileUtils.getName(fileName);
        out.putNextEntry(new ZipEntry(fileName));
        int pos = 0;
        try {
            store.setBackup(true);
            while (true) {
                pos = store.copyDirect(pos, out);
                if (pos < 0) {
                    break;
                }
                int max = store.getPageCount();
                db.setProgress(DatabaseEventListener.STATE_BACKUP_FILE, fileName, pos, max);
            }
        } finally {
            store.setBackup(false);
        }
        out.closeEntry();
    }

    private static void backupFile(ZipOutputStream out, String base, String fn) throws IOException {
        String f = FileUtils.toRealPath(fn);
        base = FileUtils.toRealPath(base);
        if (!f.startsWith(base)) {
            DbException.throwInternalError(f + " does not start with " + base);
        }
        f = f.substring(base.length());
        f = correctFileName(f);
        out.putNextEntry(new ZipEntry(f));
        InputStream in = FileUtils.newInputStream(fn);
        IOUtils.copyAndCloseInput(in, out);
        out.closeEntry();
    }

    public boolean isTransactional() {
        return true;
    }

    /**
     * Fix the file name, replacing backslash with slash.
     *
     * @param f the file name
     * @return the corrected file name
     */
    public static String correctFileName(String f) {
        f = f.replace('\\', '/');
        if (f.startsWith("/")) {
            f = f.substring(1);
        }
        return f;
    }

    public boolean needRecompile() {
        return false;
    }

    public ResultInterface queryMeta() {
        return null;
    }

    public int getType() {
        return CommandInterface.BACKUP;
    }

}
