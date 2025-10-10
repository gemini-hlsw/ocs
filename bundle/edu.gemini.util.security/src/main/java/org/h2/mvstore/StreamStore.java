/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.h2.util.IOUtils;

/**
 * A facility to store streams in a map. Streams are split into blocks, which
 * are stored in a map. Very small streams are inlined in the stream id.
 * <p>
 * The key of the map is a long (incremented for each stored block). The default
 * initial value is 0. Before storing blocks into the map, the stream store
 * checks if there is already a block with the next key, and if necessary
 * searches the next free entry using a binary search (0 to Long.MAX_VALUE).
 * <p>
 * The format of the binary id is: An empty id represents 0 bytes of data.
 * In-place data is encoded as 0, the size (a variable size int), then the data.
 * A stored block is encoded as 1, the length of the block (a variable size
 * int), then the key (a variable size long). Multiple ids can be concatenated
 * to concatenate the data. If the id is large, it is stored itself, which is
 * encoded as 2, the total length (a variable size long), and the key of the
 * block that contains the id (a variable size long).
 */
public class StreamStore {

    private final Map<Long, byte[]> map;
    private int minBlockSize = 256;
    private int maxBlockSize = 256 * 1024;
    private final AtomicLong nextKey = new AtomicLong();

    /**
     * Create a stream store instance.
     *
     * @param map the map to store blocks of data
     */
    public StreamStore(Map<Long, byte[]> map) {
        this.map = map;
    }

    public Map<Long, byte[]> getMap() {
        return map;
    }

    public void setNextKey(long nextKey) {
        this.nextKey.set(nextKey);
    }

    public long getNextKey() {
        return nextKey.get();
    }

    public void setMinBlockSize(int minBlockSize) {
        this.minBlockSize = minBlockSize;
    }

    public int getMinBlockSize() {
        return minBlockSize;
    }

    public void setMaxBlockSize(int maxBlockSize) {
        this.maxBlockSize = maxBlockSize;
    }

    public long getMaxBlockSize() {
        return maxBlockSize;
    }

    /**
     * Store the stream, and return the id.
     *
     * @param in the stream
     * @return the id (potentially an empty array)
     */
    public byte[] put(InputStream in) throws IOException {
        ByteArrayOutputStream id = new ByteArrayOutputStream();
        int level = 0;
        while (true) {
            if (put(id, in, level)) {
                break;
            }
            if (id.size() > maxBlockSize / 2) {
                id = putIndirectId(id);
                level++;
            }
        }
        if (id.size() > minBlockSize * 2) {
            id = putIndirectId(id);
        }
        return id.toByteArray();
    }

    private boolean put(ByteArrayOutputStream id, InputStream in, int level) throws IOException {
        if (level > 0) {
            ByteArrayOutputStream id2 = new ByteArrayOutputStream();
            while (true) {
                boolean eof = put(id2, in, level - 1);
                if (id2.size() > maxBlockSize / 2) {
                    id2 = putIndirectId(id2);
                    id2.writeTo(id);
                    return eof;
                } else if (eof) {
                    id2.writeTo(id);
                    return true;
                }
            }
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int len = (int) IOUtils.copy(in, buffer, maxBlockSize);
        if (len == 0) {
            return true;
        }
        boolean eof = len < maxBlockSize;
        byte[] data = buffer.toByteArray();
        if (len < minBlockSize) {
            id.write(0);
            DataUtils.writeVarInt(id, len);
            id.write(data);
        } else {
            id.write(1);
            DataUtils.writeVarInt(id, len);
            DataUtils.writeVarLong(id, writeBlock(data));
        }
        return eof;
    }

    private ByteArrayOutputStream putIndirectId(ByteArrayOutputStream id) throws IOException {
        byte[] data = id.toByteArray();
        id = new ByteArrayOutputStream();
        id.write(2);
        DataUtils.writeVarLong(id, length(data));
        DataUtils.writeVarLong(id, writeBlock(data));
        return id;
    }

    private long writeBlock(byte[] data) {
        long key = getAndIncrementNextKey();
        map.put(key, data);
        return key;
    }

    private long getAndIncrementNextKey() {
        long key = nextKey.getAndIncrement();
        if (!map.containsKey(key)) {
            return key;
        }
        // search the next free id using binary search
        synchronized (this) {
            long low = key, high = Long.MAX_VALUE;
            while (low < high) {
                long x = (low + high) >>> 1;
                if (map.containsKey(x)) {
                    low = x + 1;
                } else {
                    high = x;
                }
            }
            key = low;
            nextKey.set(key + 1);
            return key;
        }
    }

    /**
     * Remove all stored blocks for the given id.
     *
     * @param id the id
     */
    public void remove(byte[] id) {
        ByteBuffer idBuffer = ByteBuffer.wrap(id);
        while (idBuffer.hasRemaining()) {
            switch (idBuffer.get()) {
            case 0:
                int len = DataUtils.readVarInt(idBuffer);
                idBuffer.position(idBuffer.position() + len);
                break;
            case 1:
                DataUtils.readVarInt(idBuffer);
                long k = DataUtils.readVarLong(idBuffer);
                map.remove(k);
                break;
            case 2:
                DataUtils.readVarLong(idBuffer);
                long k2 = DataUtils.readVarLong(idBuffer);
                // recurse
                remove(map.get(k2));
                map.remove(k2);
                break;
            default:
                throw new IllegalArgumentException("Unsupported id");
            }
        }
    }

    /**
     * Calculate the number of data bytes for the given id. As the length is
     * encoded in the id, this operation does not cause any reads in the map.
     *
     * @param id the id
     * @return the length
     */
    public long length(byte[] id) {
        ByteBuffer idBuffer = ByteBuffer.wrap(id);
        long length = 0;
        while (idBuffer.hasRemaining()) {
            switch (idBuffer.get()) {
            case 0:
                int len = DataUtils.readVarInt(idBuffer);
                idBuffer.position(idBuffer.position() + len);
                length += len;
                break;
            case 1:
                length += DataUtils.readVarInt(idBuffer);
                DataUtils.readVarLong(idBuffer);
                break;
            case 2:
                length += DataUtils.readVarLong(idBuffer);
                DataUtils.readVarLong(idBuffer);
                break;
            default:
                throw new IllegalArgumentException("Unsupported id");
            }
        }
        return length;
    }

    /**
     * Check whether the id itself contains all the data. This operation does
     * not cause any reads in the map.
     *
     * @param id the id
     * @return if the id contains the data
     */
    public boolean isInPlace(byte[] id) {
        ByteBuffer idBuffer = ByteBuffer.wrap(id);
        while (idBuffer.hasRemaining()) {
            if (idBuffer.get() != 0) {
                return false;
            }
            int len = DataUtils.readVarInt(idBuffer);
            idBuffer.position(idBuffer.position() + len);
        }
        return true;
    }

    /**
     * Open an input stream to read data.
     *
     * @param id the id
     * @return the stream
     */
    public InputStream get(byte[] id) {
        return new Stream(this, id);
    }

    /**
     * Get the block.
     *
     * @param key the key
     * @return the block
     */
    byte[] getBlock(long key) {
        return map.get(key);
    }

    /**
     * A stream backed by a map.
     */
    static class Stream extends InputStream {

        private final StreamStore store;
        private byte[] oneByteBuffer;
        private ByteBuffer idBuffer;
        private ByteArrayInputStream buffer;
        private long skip;
        private final long length;
        private long pos;

        Stream(StreamStore store, byte[] id) {
            this.store = store;
            this.length = store.length(id);
            this.idBuffer = ByteBuffer.wrap(id);
        }

        @Override
        public int read() {
            byte[] buffer = oneByteBuffer;
            if (buffer == null) {
                buffer = oneByteBuffer = new byte[1];
            }
            int len = read(buffer, 0, 1);
            return len == -1 ? -1 : (buffer[0] & 255);
        }

        @Override
        public long skip(long n) {
            n = Math.min(length - pos, n);
            if (n == 0) {
                return 0;
            }
            if (buffer != null) {
                long s = buffer.skip(n);
                if (s > 0) {
                    n = s;
                } else {
                    buffer = null;
                    skip += n;
                }
            } else {
                skip += n;
            }
            pos += n;
            return n;
        }

        @Override
        public void close() {
            buffer = null;
            idBuffer.position(idBuffer.limit());
            pos = length;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            while (true) {
                if (buffer == null) {
                    buffer = nextBuffer();
                    if (buffer == null) {
                        return -1;
                    }
                }
                int result = buffer.read(b, off, len);
                if (result > 0) {
                    pos += result;
                    return result;
                }
                buffer = null;
            }
        }

        private ByteArrayInputStream nextBuffer() {
            while (idBuffer.hasRemaining()) {
                switch (idBuffer.get()) {
                case 0: {
                    int len = DataUtils.readVarInt(idBuffer);
                    if (skip >= len) {
                        skip -= len;
                        idBuffer.position(idBuffer.position() + len);
                        continue;
                    }
                    int p = (int) (idBuffer.position() + skip);
                    int l = (int) (len - skip);
                    idBuffer.position(p + l);
                    return new ByteArrayInputStream(idBuffer.array(), p, l);
                }
                case 1: {
                    int len = DataUtils.readVarInt(idBuffer);
                    long key = DataUtils.readVarLong(idBuffer);
                    if (skip >= len) {
                        skip -= len;
                        continue;
                    }
                    byte[] data = store.getBlock(key);
                    int s = (int) skip;
                    skip = 0;
                    return new ByteArrayInputStream(data, s, data.length - s);
                }
                case 2: {
                    long len = DataUtils.readVarInt(idBuffer);
                    long key = DataUtils.readVarLong(idBuffer);
                    if (skip >= len) {
                        skip -= len;
                        continue;
                    }
                    byte[] k = store.getBlock(key);
                    ByteBuffer newBuffer = ByteBuffer.allocate(k.length + idBuffer.limit() - idBuffer.position());
                    newBuffer.put(k);
                    newBuffer.put(idBuffer);
                    newBuffer.flip();
                    idBuffer = newBuffer;
                    return nextBuffer();
                }
                default:
                    throw new IllegalArgumentException("Unsupported id");
                }
            }
            return null;
        }

    }

}
