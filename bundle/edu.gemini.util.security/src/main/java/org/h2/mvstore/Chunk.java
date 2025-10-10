/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * A chunk of data, containing one or multiple pages.
 * <p>
 * Chunks are page aligned (each page is usually 4096 bytes).
 * There are at most 67 million (2^26) chunks,
 * each chunk is at most 2 GB large.
 * File format:
 * 1 byte: 'c'
 * 4 bytes: length
 * 4 bytes: chunk id (an incrementing number)
 * 4 bytes: pageCount
 * 8 bytes: metaRootPos
 * 8 bytes: maxLengthLive
 * [ Page ] *
 */
public class Chunk {

    /**
     * The chunk id.
     */
    final int id;

    /**
     * The start position within the file.
     */
    long start;

    /**
     * The length in bytes.
     */
    int length;

    /**
     * The number of pages.
     */
    int pageCount;

    /**
     * The sum of the max length of all pages.
     */
    long maxLength;

    /**
     * The sum of the max length of all pages that are in use.
     */
    long maxLengthLive;

    /**
     * The garbage collection priority.
     */
    int collectPriority;

    /**
     * The position of the meta root.
     */
    long metaRootPos;

    /**
     * The version stored in this chunk.
     */
    long version;

    Chunk(int id) {
        this.id = id;
    }

    /**
     * Read the header from the byte buffer.
     *
     * @param buff the source buffer
     * @param start the start of the chunk in the file
     * @return the chunk
     */
    static Chunk fromHeader(ByteBuffer buff, long start) {
        if (buff.get() != 'c') {
            throw new RuntimeException("File corrupt");
        }
        int length = buff.getInt();
        int chunkId = buff.getInt();
        int pageCount = buff.getInt();
        long metaRootPos = buff.getLong();
        long maxLength = buff.getLong();
        long maxLengthLive = buff.getLong();
        Chunk c = new Chunk(chunkId);
        c.length = length;
        c.pageCount = pageCount;
        c.start = start;
        c.metaRootPos = metaRootPos;
        c.maxLength = maxLength;
        c.maxLengthLive = maxLengthLive;
        return c;
    }

    /**
     * Write the header.
     *
     * @param buff the target buffer
     */
    void writeHeader(ByteBuffer buff) {
        buff.put((byte) 'c');
        buff.putInt(length);
        buff.putInt(id);
        buff.putInt(pageCount);
        buff.putLong(metaRootPos);
        buff.putLong(maxLength);
        buff.putLong(maxLengthLive);
    }

    /**
     * Build a block from the given string.
     *
     * @param s the string
     * @return the block
     */
    public static Chunk fromString(String s) {
        HashMap<String, String> map = DataUtils.parseMap(s);
        int id = Integer.parseInt(map.get("id"));
        Chunk c = new Chunk(id);
        c.start = Long.parseLong(map.get("start"));
        c.length = Integer.parseInt(map.get("length"));
        c.pageCount = Integer.parseInt(map.get("pageCount"));
        c.maxLength = Long.parseLong(map.get("maxLength"));
        c.maxLengthLive = Long.parseLong(map.get("maxLengthLive"));
        c.metaRootPos = Long.parseLong(map.get("metaRoot"));
        c.version = Long.parseLong(map.get("version"));
        return c;
    }

    public int getFillRate() {
        return (int) (maxLength == 0 ? 0 : 100 * maxLengthLive / maxLength);
    }

    public int hashCode() {
        return id;
    }

    public boolean equals(Object o) {
        return o instanceof Chunk && ((Chunk) o).id == id;
    }

    /**
     * Get the chunk data as a string.
     *
     * @return the string
     */
    public String asString() {
        return
                "id:" + id + "," +
                "start:" + start + "," +
                "length:" + length + "," +
                "pageCount:" + pageCount + "," +
                "maxLength:" + maxLength + "," +
                "maxLengthLive:" + maxLengthLive + "," +
                "metaRoot:" + metaRootPos + "," +
                "version:" + version;
    }

}

