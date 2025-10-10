/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.h2.api.JavaObjectSerializer;
import org.h2.constant.ErrorCode;
import org.h2.constant.SysProperties;
import org.h2.message.DbException;

/**
 * This utility class contains miscellaneous functions.
 */
public class Utils {

    /**
     * The serializer to use.
     */
    public static JavaObjectSerializer serializer;

    /**
     * An 0-size byte array.
     */
    public static final byte[] EMPTY_BYTES = {};

    /**
     * An 0-size int array.
     */
    public static final int[] EMPTY_INT_ARRAY = {};

    /**
     * An 0-size long array.
     */
    private static final long[] EMPTY_LONG_ARRAY = {};

    private static final int GC_DELAY = 50;
    private static final int MAX_GC = 8;
    private static long lastGC;

    private static final HashMap<String, byte[]> RESOURCES = New.hashMap();

    private static boolean allowAllClasses;
    private static HashSet<String> allowedClassNames;
    private static String[] allowedClassNamePrefixes;

    static {
        String clazz = SysProperties.JAVA_OBJECT_SERIALIZER;
        if (clazz != null) {
            try {
                serializer = (JavaObjectSerializer) loadUserClass(clazz).newInstance();
            } catch (Exception e) {
                throw DbException.convert(e);
            }
        }
    }

    private Utils() {
        // utility class
    }

    private static int readInt(byte[] buff, int pos) {
        return (buff[pos++] << 24) + ((buff[pos++] & 0xff) << 16) + ((buff[pos++] & 0xff) << 8) + (buff[pos] & 0xff);
    }

    /**
     * Write a long value to the byte array at the given position. The most
     * significant byte is written first.
     *
     * @param buff the byte array
     * @param pos the position
     * @param x the value to write
     */
    public static void writeLong(byte[] buff, int pos, long x) {
        writeInt(buff, pos, (int) (x >> 32));
        writeInt(buff, pos + 4, (int) x);
    }

    private static void writeInt(byte[] buff, int pos, int x) {
        buff[pos++] = (byte) (x >> 24);
        buff[pos++] = (byte) (x >> 16);
        buff[pos++] = (byte) (x >> 8);
        buff[pos++] = (byte) x;
    }

    /**
     * Read a long value from the byte array at the given position. The most
     * significant byte is read first.
     *
     * @param buff the byte array
     * @param pos the position
     * @return the value
     */
    public static long readLong(byte[] buff, int pos) {
        return (((long) readInt(buff, pos)) << 32) + (readInt(buff, pos + 4) & 0xffffffffL);
    }

    /**
     * Calculate the index of the first occurrence of the pattern in the byte
     * array, starting with the given index. This methods returns -1 if the
     * pattern has not been found, and the start position if the pattern is
     * empty.
     *
     * @param bytes the byte array
     * @param pattern the pattern
     * @param start the start index from where to search
     * @return the index
     */
    public static int indexOf(byte[] bytes, byte[] pattern, int start) {
        if (pattern.length == 0) {
            return start;
        }
        if (start > bytes.length) {
            return -1;
        }
        int last = bytes.length - pattern.length + 1;
        int patternLen = pattern.length;
        next:
        for (; start < last; start++) {
            for (int i = 0; i < patternLen; i++) {
                if (bytes[start + i] != pattern[i]) {
                    continue next;
                }
            }
            return start;
        }
        return -1;
    }

    /**
     * Calculate the hash code of the given byte array.
     *
     * @param value the byte array
     * @return the hash code
     */
    public static int getByteArrayHash(byte[] value) {
        int len = value.length;
        int h = len;
        if (len < 50) {
            for (int i = 0; i < len; i++) {
                h = 31 * h + value[i];
            }
        } else {
            int step = len / 16;
            for (int i = 0; i < 4; i++) {
                h = 31 * h + value[i];
                h = 31 * h + value[--len];
            }
            for (int i = 4 + step; i < len; i += step) {
                h = 31 * h + value[i];
            }
        }
        return h;
    }

    /**
     * Compare two byte arrays. This method will always loop over all bytes and
     * doesn't use conditional operations in the loop to make sure an attacker
     * can not use a timing attack when trying out passwords.
     *
     * @param test the first array
     * @param good the second array
     * @return true if both byte arrays contain the same bytes
     */
    public static boolean compareSecure(byte[] test, byte[] good) {
        if ((test == null) || (good == null)) {
            return (test == null) && (good == null);
        }
        int len = test.length;
        if (len != good.length) {
            return false;
        }
        if (len == 0) {
            return true;
        }
        // don't use conditional operations inside the loop
        int bits = 0;
        for (int i = 0; i < len; i++) {
            // this will never reset any bits
            bits |= test[i] ^ good[i];
        }
        return bits == 0;
    }

    /**
     * Compare the contents of two byte arrays. If the content or length of the
     * first array is smaller than the second array, -1 is returned. If the
     * content or length of the second array is smaller than the first array, 1
     * is returned. If the contents and lengths are the same, 0 is returned.
     *
     * @param data1 the first byte array (must not be null)
     * @param data2 the second byte array (must not be null)
     * @return the result of the comparison (-1, 1 or 0)
     */
    public static int compareNotNull(byte[] data1, byte[] data2) {
        if (data1 == data2) {
            return 0;
        }
        int len = Math.min(data1.length, data2.length);
        for (int i = 0; i < len; i++) {
            byte b = data1[i];
            byte b2 = data2[i];
            if (b != b2) {
                return b > b2 ? 1 : -1;
            }
        }
        return Integer.signum(data1.length - data2.length);
    }

    /**
     * Copy the contents of the source array to the target array. If the size if
     * the target array is too small, a larger array is created.
     *
     * @param source the source array
     * @param target the target array
     * @return the target array or a new one if the target array was too small
     */
    public static byte[] copy(byte[] source, byte[] target) {
        int len = source.length;
        if (len > target.length) {
            target = new byte[len];
        }
        System.arraycopy(source, 0, target, 0, len);
        return target;
    }

    /**
     * Create a new byte array and copy all the data. If the size of the byte
     * array is zero, the same array is returned.
     *
     * @param b the byte array (may not be null)
     * @return a new byte array
     */
    public static byte[] cloneByteArray(byte[] b) {
        if (b == null) {
            return null;
        }
        int len = b.length;
        if (len == 0) {
            return EMPTY_BYTES;
        }
        byte[] copy = new byte[len];
        System.arraycopy(b, 0, copy, 0, len);
        return copy;
    }

    /**
     * Serialize the object to a byte array.
     *
     * @param obj the object to serialize
     * @return the byte array
     */
    public static byte[] serialize(Object obj) {
        try {
            if (serializer != null) {
                return serializer.serialize(obj);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        } catch (Throwable e) {
            throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1, e, e.toString());
        }
    }

    /**
     * De-serialize the byte array to an object.
     *
     * @param data the byte array
     * @return the object
     * @throws DbException if serialization fails
     */
    public static Object deserialize(byte[] data) {
        try {
            if (serializer != null) {
                return serializer.deserialize(data);
            }
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is;
            if (SysProperties.USE_THREAD_CONTEXT_CLASS_LOADER) {
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                is = new ObjectInputStream(in) {
                    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                        try {
                            return Class.forName(desc.getName(), true, loader);
                        } catch (ClassNotFoundException e) {
                            return super.resolveClass(desc);
                        }
                    }
                };
            } else {
                is = new ObjectInputStream(in);
            }
            return is.readObject();
        } catch (Throwable e) {
            throw DbException.get(ErrorCode.DESERIALIZATION_FAILED_1, e, e.toString());
        }
    }

    /**
     * Calculate the hash code of the given object. The object may be null.
     *
     * @param o the object
     * @return the hash code, or 0 if the object is null
     */
    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    /**
     * Get the used memory in KB.
     * This method possibly calls System.gc().
     *
     * @return the used memory
     */
    public static int getMemoryUsed() {
        collectGarbage();
        Runtime rt = Runtime.getRuntime();
        long mem = rt.totalMemory() - rt.freeMemory();
        return (int) (mem >> 10);
    }

    /**
     * Get the free memory in KB.
     * This method possibly calls System.gc().
     *
     * @return the free memory
     */
    public static int getMemoryFree() {
        collectGarbage();
        Runtime rt = Runtime.getRuntime();
        long mem = rt.freeMemory();
        return (int) (mem >> 10);
    }

    /**
     * Get the maximum memory in KB.
     *
     * @return the maximum memory
     */
    public static long getMemoryMax() {
        long max = Runtime.getRuntime().maxMemory();
        return max / 1024;
    }

    private static synchronized void collectGarbage() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long time = System.currentTimeMillis();
        if (lastGC + GC_DELAY < time) {
            for (int i = 0; i < MAX_GC; i++) {
                runtime.gc();
                long now = runtime.totalMemory();
                if (now == total) {
                    lastGC = System.currentTimeMillis();
                    break;
                }
                total = now;
            }
        }
    }

    /**
     * Create an array of bytes with the given size. If this is not possible
     * because not enough memory is available, an OutOfMemoryError with the
     * requested size in the message is thrown.
     *
     * @param len the number of bytes requested
     * @return the byte array
     * @throws OutOfMemoryError
     */
    public static byte[] newBytes(int len) {
        try {
            if (len == 0) {
                return EMPTY_BYTES;
            }
            return new byte[len];
        } catch (OutOfMemoryError e) {
            Error e2 = new OutOfMemoryError("Requested memory: " + len);
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Create an int array with the given size.
     *
     * @param len the number of bytes requested
     * @return the int array
     */
    public static int[] newIntArray(int len) {
        if (len == 0) {
            return EMPTY_INT_ARRAY;
        }
        return new int[len];
    }

    /**
     * Create a long array with the given size.
     *
     * @param len the number of bytes requested
     * @return the int array
     */
    public static long[] newLongArray(int len) {
        if (len == 0) {
            return EMPTY_LONG_ARRAY;
        }
        return new long[len];
    }

    /**
     * Checks if given classes have a common Comparable superclass.
     *
     * @param c1 the first class
     * @param c2 the second class
     * @return true if they have
     */
    public static boolean haveCommonComparableSuperclass(Class<?> c1, Class<?> c2) {
        if (c1 == c2 || c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1)) {
            return true;
        }
        Class<?> top1;
        do {
            top1 = c1;
            c1 = c1.getSuperclass();
        } while (Comparable.class.isAssignableFrom(c1));

        Class<?> top2;
        do {
            top2 = c2;
            c2 = c2.getSuperclass();
        } while (Comparable.class.isAssignableFrom(c2));

        return top1 == top2;
    }

    /**
     * Load a class, but check if it is allowed to load this class first. To
     * perform access rights checking, the system property h2.allowedClasses
     * needs to be set to a list of class file name prefixes.
     *
     * @param className the name of the class
     * @return the class object
     */
    public static Class<?> loadUserClass(String className) {
        if (allowedClassNames == null) {
            // initialize the static fields
            String s = SysProperties.ALLOWED_CLASSES;
            ArrayList<String> prefixes = New.arrayList();
            boolean allowAll = false;
            HashSet<String> classNames = New.hashSet();
            for (String p : StringUtils.arraySplit(s, ',', true)) {
                if (p.equals("*")) {
                    allowAll = true;
                } else if (p.endsWith("*")) {
                    prefixes.add(p.substring(0, p.length() - 1));
                } else {
                    classNames.add(p);
                }
            }
            allowedClassNamePrefixes = new String[prefixes.size()];
            prefixes.toArray(allowedClassNamePrefixes);
            allowAllClasses = allowAll;
            allowedClassNames = classNames;
        }
        if (!allowAllClasses && !allowedClassNames.contains(className)) {
            boolean allowed = false;
            for (String s : allowedClassNamePrefixes) {
                if (className.startsWith(s)) {
                    allowed = true;
                }
            }
            if (!allowed) {
                throw DbException.get(ErrorCode.ACCESS_DENIED_TO_CLASS_1, className);
            }
        }
        // the following code might be better for OSGi (need to verify):
        /*
        try {
            return Utils.class.getClassLoader().loadClass(className);
        } catch (Throwable e) {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (Throwable e2) {
                DbException.get(ErrorCode.CLASS_NOT_FOUND_1, e, className);
            }
        }
        */
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            } catch (Exception e2) {
                throw DbException.get(ErrorCode.CLASS_NOT_FOUND_1, e, className);
            }
        } catch (NoClassDefFoundError e) {
            throw DbException.get(ErrorCode.CLASS_NOT_FOUND_1, e, className);
        } catch (Error e) {
            // UnsupportedClassVersionError
            throw DbException.get(ErrorCode.GENERAL_ERROR_1, e, className);
        }
    }

    /**
     * Get a resource from the resource map.
     *
     * @param name the name of the resource
     * @return the resource data
     */
    public static byte[] getResource(String name) throws IOException {
        byte[] data = RESOURCES.get(name);
        if (data == null) {
            data = loadResource(name);
            RESOURCES.put(name, data);
        }
        return data == null ? EMPTY_BYTES : data;
    }

    private static byte[] loadResource(String name) throws IOException {
        InputStream in = Utils.class.getResourceAsStream("data.zip");
        if (in == null) {
            in = Utils.class.getResourceAsStream(name);
            if (in == null) {
                return null;
            }
            return IOUtils.readBytesAndClose(in, 0);
        }
        ZipInputStream zipIn = new ZipInputStream(in);
        try {
            while (true) {
                ZipEntry entry = zipIn.getNextEntry();
                if (entry == null) {
                    break;
                }
                String entryName = entry.getName();
                if (!entryName.startsWith("/")) {
                    entryName = "/" + entryName;
                }
                if (entryName.equals(name)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    IOUtils.copy(zipIn, out);
                    zipIn.closeEntry();
                    return out.toByteArray();
                }
                zipIn.closeEntry();
            }
        } catch (IOException e) {
            // if this happens we have a real problem
            e.printStackTrace();
        } finally {
            zipIn.close();
        }
        return null;
    }

    /**
     * Calls a static method via reflection. This will try to use the method
     * where the most parameter classes match exactly (this algorithm is simpler
     * than the one in the Java specification, but works well for most cases).
     *
     * @param classAndMethod a string with the entire class and method name, eg.
     *            "java.lang.System.gc"
     * @param params the method parameters
     * @return the return value from this call
     */
    public static Object callStaticMethod(String classAndMethod, Object... params) throws Exception {
        int lastDot = classAndMethod.lastIndexOf('.');
        String className = classAndMethod.substring(0, lastDot);
        String methodName = classAndMethod.substring(lastDot + 1);
        return callMethod(null, Class.forName(className), methodName, params);
    }

    /**
     * Calls an instance method via reflection. This will try to use the method
     * where the most parameter classes match exactly (this algorithm is simpler
     * than the one in the Java specification, but works well for most cases).
     *
     * @param instance the instance on which the call is done
     * @param methodName a string with the method name
     * @param params the method parameters
     * @return the return value from this call
     */
    public static Object callMethod(
            Object instance,
            String methodName,
            Object... params) throws Exception {
        return callMethod(instance, instance.getClass(), methodName, params);
    }

    private static Object callMethod(
            Object instance, Class<?> clazz,
            String methodName,
            Object... params) throws Exception {
        Method best = null;
        int bestMatch = 0;
        boolean isStatic = instance == null;
        for (Method m : clazz.getMethods()) {
            if (Modifier.isStatic(m.getModifiers()) == isStatic && m.getName().equals(methodName)) {
                int p = match(m.getParameterTypes(), params);
                if (p > bestMatch) {
                    bestMatch = p;
                    best = m;
                }
            }
        }
        if (best == null) {
            throw new NoSuchMethodException(methodName);
        }
        return best.invoke(instance, params);
    }

    /**
     * Creates a new instance. This will try to use the constructor where the
     * most parameter classes match exactly (this algorithm is simpler than the
     * one in the Java specification, but works well for most cases).
     *
     * @param className a string with the entire class, eg. "java.lang.Integer"
     * @param params the constructor parameters
     * @return the newly created object
     */
    public static Object newInstance(String className, Object... params) throws Exception {
        Constructor<?> best = null;
        int bestMatch = 0;
        for (Constructor<?> c : Class.forName(className).getConstructors()) {
            int p = match(c.getParameterTypes(), params);
            if (p > bestMatch) {
                bestMatch = p;
                best = c;
            }
        }
        if (best == null) {
            throw new NoSuchMethodException(className);
        }
        return best.newInstance(params);
    }

    private static int match(Class<?>[] params, Object[] values) {
        int len = params.length;
        if (len == values.length) {
            int points = 1;
            for (int i = 0; i < len; i++) {
                Class<?> pc = getNonPrimitiveClass(params[i]);
                Object v = values[i];
                Class<?> vc = v == null ? null : v.getClass();
                if (pc == vc) {
                    points++;
                } else if (vc == null) {
                    // can't verify
                } else if (!pc.isAssignableFrom(vc)) {
                    return 0;
                }
            }
            return points;
        }
        return 0;
    }

    /**
     * Returns a static field.
     *
     * @param classAndField a string with the entire class and field name
     * @return the field value
     */
    public static Object getStaticField(String classAndField) throws Exception {
        int lastDot = classAndField.lastIndexOf('.');
        String className = classAndField.substring(0, lastDot);
        String fieldName = classAndField.substring(lastDot + 1);
        return Class.forName(className).getField(fieldName).get(null);
    }

    /**
     * Returns a static field.
     *
     * @param instance the instance on which the call is done
     * @param fieldName the field name
     * @return the field value
     */
    public static Object getField(Object instance, String fieldName) throws Exception {
        return instance.getClass().getField(fieldName).get(instance);
    }

    /**
     * Returns true if the class is present in the current class loader.
     *
     * @param fullyQualifiedClassName a string with the entire class name, eg.
     *        "java.lang.System"
     * @return true if the class is present
     */
    public static boolean isClassPresent(String fullyQualifiedClassName) {
        try {
            Class.forName(fullyQualifiedClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Convert primitive class names to java.lang.* class names.
     *
     * @param clazz the class (for example: int)
     * @return the non-primitive class (for example: java.lang.Integer)
     */
    public static Class<?> getNonPrimitiveClass(Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        } else if (clazz == boolean.class) {
            return Boolean.class;
        } else if (clazz == byte.class) {
            return Byte.class;
        } else if (clazz == char.class) {
            return Character.class;
        } else if (clazz == double.class) {
            return Double.class;
        } else if (clazz == float.class) {
            return Float.class;
        } else if (clazz == int.class) {
            return Integer.class;
        } else if (clazz == long.class) {
            return Long.class;
        } else if (clazz == short.class) {
            return Short.class;
        } else if (clazz == void.class) {
            return Void.class;
        }
        return clazz;
    }

    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value
     */
    public static String getProperty(String key, String defaultValue) {
        try {
            return System.getProperty(key, defaultValue);
        } catch (SecurityException se) {
            return defaultValue;
        }
    }

    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value
     */
    public static int getProperty(String key, int defaultValue) {
        String s = getProperty(key, null);
        if (s != null) {
            try {
                return Integer.decode(s).intValue();
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

    /**
     * Get the system property. If the system property is not set, or if a
     * security exception occurs, the default value is returned.
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value
     */
    public static boolean getProperty(String key, boolean defaultValue) {
        String s = getProperty(key, null);
        if (s != null) {
            try {
                return Boolean.parseBoolean(s);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }

}
