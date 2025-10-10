/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.h2.compress.CompressLZF;
import org.h2.constant.ErrorCode;
import org.h2.message.DbException;
import org.h2.util.MathUtils;
import org.h2.util.New;

/**
 * This file system keeps files fully in memory. There is an option to compress
 * file blocks to safe memory.
 */
public class FilePathMem extends FilePath {

    private static final TreeMap<String, FileMemData> MEMORY_FILES = new TreeMap<String, FileMemData>();

    public FilePathMem getPath(String path) {
        FilePathMem p = new FilePathMem();
        p.name = getCanonicalPath(path);
        return p;
    }

    public long size() {
        return getMemoryFile().length();
    }

    public void moveTo(FilePath newName) {
        synchronized (MEMORY_FILES) {
            FileMemData f = getMemoryFile();
            f.setName(newName.name);
            MEMORY_FILES.remove(name);
            MEMORY_FILES.put(newName.name, f);
        }
    }

    public boolean createFile() {
        synchronized (MEMORY_FILES) {
            if (exists()) {
                return false;
            }
            getMemoryFile();
        }
        return true;
    }

    public boolean exists() {
        if (isRoot()) {
            return true;
        }
        synchronized (MEMORY_FILES) {
            return MEMORY_FILES.get(name) != null;
        }
    }

    public void delete() {
        if (isRoot()) {
            return;
        }
        synchronized (MEMORY_FILES) {
            MEMORY_FILES.remove(name);
        }
    }

    public List<FilePath> newDirectoryStream() {
        ArrayList<FilePath> list = New.arrayList();
        synchronized (MEMORY_FILES) {
            for (String n : MEMORY_FILES.tailMap(name).keySet()) {
                if (n.startsWith(name)) {
                    list.add(getPath(n));
                } else {
                    break;
                }
            }
            return list;
        }
    }

    public boolean setReadOnly() {
        return getMemoryFile().setReadOnly();
    }

    public boolean canWrite() {
        return getMemoryFile().canWrite();
    }

    public FilePathMem getParent() {
        int idx = name.lastIndexOf('/');
        return idx < 0 ? null : getPath(name.substring(0, idx));
    }

    public boolean isDirectory() {
        if (isRoot()) {
            return true;
        }
        // TODO in memory file system currently
        // does not really support directories
        synchronized (MEMORY_FILES) {
            return MEMORY_FILES.get(name) == null;
        }
    }

    public boolean isAbsolute() {
        // TODO relative files are not supported
        return true;
    }

    public FilePathMem toRealPath() {
        return this;
    }

    public long lastModified() {
        return getMemoryFile().getLastModified();
    }

    public void createDirectory() {
        if (exists() && isDirectory()) {
            throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1, name + " (a file with this name already exists)");
        }
        // TODO directories are not really supported
    }

    public OutputStream newOutputStream(boolean append) {
        try {
            FileMemData obj = getMemoryFile();
            FileMem m = new FileMem(obj, false);
            return new FileChannelOutputStream(m, append);
        } catch (IOException e) {
            throw DbException.convertIOException(e, name);
        }
    }

    public InputStream newInputStream() {
        FileMemData obj = getMemoryFile();
        FileMem m = new FileMem(obj, true);
        return new FileChannelInputStream(m);
    }

    public FileChannel open(String mode) {
        FileMemData obj = getMemoryFile();
        return new FileMem(obj, "r".equals(mode));
    }

    private FileMemData getMemoryFile() {
        synchronized (MEMORY_FILES) {
            FileMemData m = MEMORY_FILES.get(name);
            if (m == null) {
                m = new FileMemData(name, compressed());
                MEMORY_FILES.put(name, m);
            }
            return m;
        }
    }

    private boolean isRoot() {
        return name.equals(getScheme());
    }

    private static String getCanonicalPath(String fileName) {
        fileName = fileName.replace('\\', '/');
        int idx = fileName.indexOf(':') + 1;
        if (fileName.length() > idx && fileName.charAt(idx) != '/') {
            fileName = fileName.substring(0, idx) + "/" + fileName.substring(idx);
        }
        return fileName;
    }

    public String getScheme() {
        return "memFS";
    }

    /**
     * Whether the file should be compressed.
     *
     * @return if it should be compressed.
     */
    boolean compressed() {
        return false;
    }

}

/**
 * A memory file system that compresses blocks to conserve memory.
 */
class FilePathMemLZF extends FilePathMem {

    boolean compressed() {
        return true;
    }

    public String getScheme() {
        return "memLZF";
    }

}

/**
 * This class represents an in-memory file.
 */
class FileMem extends FileBase {

    /**
     * The file data.
     */
    final FileMemData data;

    private final boolean readOnly;
    private long pos;

    FileMem(FileMemData data, boolean readOnly) {
        this.data = data;
        this.readOnly = readOnly;
    }

    public long size() {
        return data.length();
    }

    public FileChannel truncate(long newLength) throws IOException {
        if (newLength < size()) {
            data.touch(readOnly);
            pos = Math.min(pos, newLength);
            data.truncate(newLength);
        }
        return this;
    }

    public FileChannel position(long newPos) {
        this.pos = (int) newPos;
        return this;
    }

    public int write(ByteBuffer src) throws IOException {
        int len = src.remaining();
        if (len == 0) {
            return 0;
        }
        data.touch(readOnly);
        pos = data.readWrite(pos, src.array(), src.position(), len, true);
        src.position(src.position() + len);
        return len;
    }

    public int read(ByteBuffer dst) throws IOException {
        int len = dst.remaining();
        if (len == 0) {
            return 0;
        }
        long newPos = data.readWrite(pos, dst.array(), dst.position(), len, false);
        len = (int) (newPos - pos);
        if (len <= 0) {
            return -1;
        }
        dst.position(dst.position() + len);
        pos = newPos;
        return len;
    }

    public long position() {
        return pos;
    }

    public void implCloseChannel() throws IOException {
        pos = 0;
    }

    public void force(boolean metaData) throws IOException {
        // do nothing
    }

    public synchronized FileLock tryLock(long position, long size, boolean shared) throws IOException {
        if (shared) {
            if (!data.lockShared()) {
                return null;
            }
        } else {
            if (!data.lockExclusive()) {
                return null;
            }
        }
        FileLock lock = new FileLock((FileChannel) null, position, size, shared) {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public void release() throws IOException {
                data.unlock();
            }
        };
        return lock;
    }

    public String toString() {
        return data.getName();
    }

}

/**
 * This class contains the data of an in-memory random access file.
 * Data compression using the LZF algorithm is supported as well.
 */
class FileMemData {

    private static final int CACHE_SIZE = 8;
    private static final int BLOCK_SIZE_SHIFT = 10;
    private static final int BLOCK_SIZE = 1 << BLOCK_SIZE_SHIFT;
    private static final int BLOCK_SIZE_MASK = BLOCK_SIZE - 1;
    private static final CompressLZF LZF = new CompressLZF();
    private static final byte[] BUFFER = new byte[BLOCK_SIZE * 2];
    private static final byte[] COMPRESSED_EMPTY_BLOCK;

    private static final Cache<CompressItem, CompressItem> COMPRESS_LATER =
        new Cache<CompressItem, CompressItem>(CACHE_SIZE);

    private String name;
    private final boolean compress;
    private long length;
    private byte[][] data;
    private long lastModified;
    private boolean isReadOnly;
    private boolean isLockedExclusive;
    private int sharedLockCount;

    static {
        byte[] n = new byte[BLOCK_SIZE];
        int len = LZF.compress(n, BLOCK_SIZE, BUFFER, 0);
        COMPRESSED_EMPTY_BLOCK = new byte[len];
        System.arraycopy(BUFFER, 0, COMPRESSED_EMPTY_BLOCK, 0, len);
    }

    FileMemData(String name, boolean compress) {
        this.name = name;
        this.compress = compress;
        data = new byte[0][];
        lastModified = System.currentTimeMillis();
    }

    /**
     * Lock the file in exclusive mode if possible.
     *
     * @return if locking was successful
     */
    synchronized boolean lockExclusive() {
        if (sharedLockCount > 0 || isLockedExclusive) {
            return false;
        }
        isLockedExclusive = true;
        return true;
    }

    /**
     * Lock the file in shared mode if possible.
     *
     * @return if locking was successful
     */
    synchronized boolean lockShared() {
        if (isLockedExclusive) {
            return false;
        }
        sharedLockCount++;
        return true;
    }

    /**
     * Unlock the file.
     */
    synchronized void unlock() {
        if (isLockedExclusive) {
            isLockedExclusive = false;
        } else {
            sharedLockCount = Math.max(0, sharedLockCount - 1);
        }
    }

    /**
     * This small cache compresses the data if an element leaves the cache.
     */
    static class Cache<K, V> extends LinkedHashMap<K, V> {

        private static final long serialVersionUID = 1L;
        private final int size;

        Cache(int size) {
            super(size, (float) 0.75, true);
            this.size = size;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            if (size() < size) {
                return false;
            }
            CompressItem c = (CompressItem) eldest.getKey();
            compress(c.data, c.page);
            return true;
        }
    }

    /**
     * Represents a compressed item.
     */
    static class CompressItem {

        /**
         * The file data.
         */
        byte[][] data;

        /**
         * The page to compress.
         */
        int page;

        public int hashCode() {
            return page;
        }

        public boolean equals(Object o) {
            if (o instanceof CompressItem) {
                CompressItem c = (CompressItem) o;
                return c.data == data && c.page == page;
            }
            return false;
        }

    }

    private static void compressLater(byte[][] data, int page) {
        CompressItem c = new CompressItem();
        c.data = data;
        c.page = page;
        synchronized (LZF) {
            COMPRESS_LATER.put(c, c);
        }
    }

    private static void expand(byte[][] data, int page) {
        byte[] d = data[page];
        if (d.length == BLOCK_SIZE) {
            return;
        }
        byte[] out = new byte[BLOCK_SIZE];
        if (d != COMPRESSED_EMPTY_BLOCK) {
            synchronized (LZF) {
                LZF.expand(d, 0, d.length, out, 0, BLOCK_SIZE);
            }
        }
        data[page] = out;
    }

    /**
     * Compress the data in a byte array.
     *
     * @param data the page array
     * @param page which page to compress
     */
    static void compress(byte[][] data, int page) {
        byte[] d = data[page];
        synchronized (LZF) {
            int len = LZF.compress(d, BLOCK_SIZE, BUFFER, 0);
            if (len <= BLOCK_SIZE) {
                d = new byte[len];
                System.arraycopy(BUFFER, 0, d, 0, len);
                data[page] = d;
            }
        }
    }

    /**
     * Update the last modified time.
     *
     * @param openReadOnly if the file was opened in read-only mode
     */
    void touch(boolean openReadOnly) throws IOException {
        if (isReadOnly || openReadOnly) {
            throw new IOException("Read only");
        }
        lastModified = System.currentTimeMillis();
    }

    /**
     * Get the file length.
     *
     * @return the length
     */
    long length() {
        return length;
    }

    /**
     * Truncate the file.
     *
     * @param newLength the new length
     */
    void truncate(long newLength) {
        changeLength(newLength);
        long end = MathUtils.roundUpLong(newLength, BLOCK_SIZE);
        if (end != newLength) {
            int lastPage = (int) (newLength >>> BLOCK_SIZE_SHIFT);
            expand(data, lastPage);
            byte[] d = data[lastPage];
            for (int i = (int) (newLength & BLOCK_SIZE_MASK); i < BLOCK_SIZE; i++) {
                d[i] = 0;
            }
            if (compress) {
                compressLater(data, lastPage);
            }
        }
    }

    private void changeLength(long len) {
        length = len;
        len = MathUtils.roundUpLong(len, BLOCK_SIZE);
        int blocks = (int) (len >>> BLOCK_SIZE_SHIFT);
        if (blocks != data.length) {
            byte[][] n = new byte[blocks][];
            System.arraycopy(data, 0, n, 0, Math.min(data.length, n.length));
            for (int i = data.length; i < blocks; i++) {
                n[i] = COMPRESSED_EMPTY_BLOCK;
            }
            data = n;
        }
    }

    /**
     * Read or write.
     *
     * @param pos the position
     * @param b the byte array
     * @param off the offset within the byte array
     * @param len the number of bytes
     * @param write true for writing
     * @return the new position
     */
    long readWrite(long pos, byte[] b, int off, int len, boolean write) {
        long end = pos + len;
        if (end > length) {
            if (write) {
                changeLength(end);
            } else {
                len = (int) (length - pos);
            }
        }
        while (len > 0) {
            int l = (int) Math.min(len, BLOCK_SIZE - (pos & BLOCK_SIZE_MASK));
            int page = (int) (pos >>> BLOCK_SIZE_SHIFT);
            expand(data, page);
            byte[] block = data[page];
            int blockOffset = (int) (pos & BLOCK_SIZE_MASK);
            if (write) {
                System.arraycopy(b, off, block, blockOffset, l);
            } else {
                System.arraycopy(block, blockOffset, b, off, l);
            }
            if (compress) {
                compressLater(data, page);
            }
            off += l;
            pos += l;
            len -= l;
        }
        return pos;
    }

    /**
     * Set the file name.
     *
     * @param name the name
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Get the file name
     *
     * @return the name
     */
    String getName() {
        return name;
    }

    /**
     * Get the last modified time.
     *
     * @return the time
     */
    long getLastModified() {
        return lastModified;
    }

    /**
     * Check whether writing is allowed.
     *
     * @return true if it is
     */
    boolean canWrite() {
        return !isReadOnly;
    }

    /**
     * Set the read-only flag.
     *
     * @return true
     */
    boolean setReadOnly() {
        isReadOnly = true;
        return true;
    }

}


