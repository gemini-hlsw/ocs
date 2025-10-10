/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;

/**
 * This file system stores files on disk and uses java.nio to access the files.
 * This class uses FileChannel.
 */
public class FilePathNio extends FilePathWrapper {

    public FileChannel open(String mode) throws IOException {
        return new FileNio(name.substring(getScheme().length() + 1), mode);
    }

    public String getScheme() {
        return "nio";
    }

}

/**
 * File which uses NIO FileChannel.
 */
class FileNio extends FileBase {

    private final String name;
    private final FileChannel channel;

    FileNio(String fileName, String mode) throws IOException {
        this.name = fileName;
        channel = new RandomAccessFile(fileName, mode).getChannel();
    }

    public void implCloseChannel() throws IOException {
        channel.close();
    }

    public long position() throws IOException {
        return channel.position();
    }

    public long size() throws IOException {
        return channel.size();
    }

    public int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    public FileChannel position(long pos) throws IOException {
        channel.position(pos);
        return this;
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        return channel.read(dst, position);
    }

    public int write(ByteBuffer src, long position) throws IOException {
        return channel.write(src, position);
    }

    public FileChannel truncate(long newLength) throws IOException {
        try {
            channel.truncate(newLength);
            if (channel.position() > newLength) {
                // looks like a bug in this FileChannel implementation, as the
                // documentation says the position needs to be changed
                channel.position(newLength);
            }
            return this;
        } catch (NonWritableChannelException e) {
            throw new IOException("read only");
        }
    }

    public void force(boolean metaData) throws IOException {
        channel.force(metaData);
    }

    public int write(ByteBuffer src) throws IOException {
        try {
            return channel.write(src);
        } catch (NonWritableChannelException e) {
            throw new IOException("read only");
        }
    }

    public synchronized FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return channel.tryLock(position, size, shared);
    }

    public String toString() {
        return "nio:" + name;
    }

}
