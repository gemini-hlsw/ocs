/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.store;

import java.io.OutputStream;
import org.h2.engine.Constants;
import org.h2.tools.CompressTool;

/**
 * An output stream that is backed by a file store.
 */
public class FileStoreOutputStream extends OutputStream {
    private FileStore store;
    private final Data page;
    private final String compressionAlgorithm;
    private final CompressTool compress;
    private final byte[] buffer = { 0 };

    public FileStoreOutputStream(FileStore store, DataHandler handler, String compressionAlgorithm) {
        this.store = store;
        if (compressionAlgorithm != null) {
            this.compress = CompressTool.getInstance();
            this.compressionAlgorithm = compressionAlgorithm;
        } else {
            this.compress = null;
            this.compressionAlgorithm = null;
        }
        page = Data.create(handler, Constants.FILE_BLOCK_SIZE);
    }

    public void write(int b) {
        buffer[0] = (byte) b;
        write(buffer);
    }

    public void write(byte[] buff) {
        write(buff, 0, buff.length);
    }

    public void write(byte[] buff, int off, int len) {
        if (len > 0) {
            page.reset();
            if (compress != null) {
                if (off != 0 || len != buff.length) {
                    byte[] b2 = new byte[len];
                    System.arraycopy(buff, off, b2, 0, len);
                    buff = b2;
                    off = 0;
                }
                int uncompressed = len;
                buff = compress.compress(buff, compressionAlgorithm);
                len = buff.length;
                page.checkCapacity(2 * Data.LENGTH_INT + len);
                page.writeInt(len);
                page.writeInt(uncompressed);
                page.write(buff, off, len);
            } else {
                page.checkCapacity(Data.LENGTH_INT + len);
                page.writeInt(len);
                page.write(buff, off, len);
            }
            page.fillAligned();
            store.write(page.getBytes(), 0, page.length());
        }
    }

    public void close() {
        if (store != null) {
            try {
                store.close();
            } finally {
                store = null;
            }
        }
    }

}
