/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore.type;

import java.nio.ByteBuffer;

/**
 * A data type.
 */
public interface DataType {

    /**
     * Compare two keys.
     *
     * @param a the first key
     * @param b the second key
     * @return -1 if the first key is smaller, 1 if larger, and 0 if equal
     */
    int compare(Object a, Object b);

    /**
     * Get the maximum length in bytes used to store an object. In many cases,
     * this method can be faster than calculating the exact length.
     *
     * @param obj the object
     * @return the maximum length
     */
    int getMaxLength(Object obj);

    /**
     * Estimate the used memory in bytes.
     *
     * @param obj the object
     * @return the used memory
     */
    int getMemory(Object obj);

    /**
     * Write the object.
     *
     * @param buff the target buffer
     * @param obj the value
     */
    void write(ByteBuffer buff, Object obj);

    /**
     * Read an object.
     *
     * @param buff the source buffer
     * @return the object
     */
    Object read(ByteBuffer buff);

    /**
     * Get the stable string representation that is used to build this data
     * type.
     * <p>
     * To avoid conflict with the default factory, the returned string should
     * start with the package name of the type factory.
     *
     * @return the string representation
     */
    String asString();

}

