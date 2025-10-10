/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.api;

/**
 * Custom serialization mechanism for java objects being stored in column of
 * type OTHER.
 *
 * @author Sergi Vladykin
 */
public interface JavaObjectSerializer {

    /**
     * Serialize object to byte array.
     *
     * @param obj the object to serialize
     * @return the byte array of the serialized object
     */
    byte[] serialize(Object obj) throws Exception;

    /**
     * Deserialize object from byte array.
     *
     * @param bytes the byte array of the serialized object
     * @return the object
     */
    Object deserialize(byte[] bytes) throws Exception;

}
