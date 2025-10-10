/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store.fs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Allows to read from a file channel like an input stream.
 */
public class FileChannelInputStream extends InputStream {

    private final FileChannel channel;
    private final byte[] buffer = { 0 };

    /**
     * Create a new file object input stream from the file channel.
     *
     * @param channel the file channel
     */
    public FileChannelInputStream(FileChannel channel) {
        this.channel = channel;
    }

    public int read() throws IOException {
        if (channel.position() >= channel.size()) {
            return -1;
        }
        FileUtils.readFully(channel, ByteBuffer.wrap(buffer));
        return buffer[0] & 0xff;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (channel.position() + len < channel.size()) {
            FileUtils.readFully(channel, ByteBuffer.wrap(b, off, len));
            return len;
        }
        return super.read(b, off, len);
    }

    public void close() throws IOException {
        channel.close();
    }

}
