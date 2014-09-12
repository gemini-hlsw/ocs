/*
 * ESO Archive
 *
 * $Id: FileUtil.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/24  Created
 */

package jsky.util;

import java.io.*;
import java.net.*;


/**
 * Contains static utility methods for dealing with files and URLs.
 */
public final class FileUtil {

    /**
     * Given a URL context (for resolving relative path names) and
     * a string, which may be either a file or a URL string, return a
     * new URL made from the string.
     *
     * @param context the base URL, used to resolve relative path names (may be null)
     * @param fileOrUrlStr a file name or URL string (may be relative)
     */
    public static URL makeURL(URL context, String fileOrUrlStr) {
        final URL url;
        try {
            if (context != null
                    || fileOrUrlStr.startsWith("http:")
                    || fileOrUrlStr.startsWith("file:")
                    || fileOrUrlStr.startsWith("jar:")
                    || fileOrUrlStr.startsWith("ftp:")
                    || fileOrUrlStr.startsWith("bundle:")) {
                if (context != null)
                    url = new URL(context, fileOrUrlStr);
                else
                    url = new URL(fileOrUrlStr);
            } else {
                File file = new File(fileOrUrlStr);
                url = file.getAbsoluteFile().toURI().toURL();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return url;
    }


    /**
     * This method returns an InputStream for the given URL, and also wraps it
     * in a BufferedInputStream, if necessary.
     *
     * @param url the URL to read
     */
    public static InputStream makeURLStream(URL url) {
        try {
            InputStream stream = url.openStream();
            if (!(stream instanceof BufferedInputStream))
                stream = new BufferedInputStream(stream);
            return stream;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Copy the given input stream to the given output stream.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[8 * 1024];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) break;
            out.write(buffer, 0, bytesRead);
        }
    }


    /** Return the contents of the URL as a String */
    public static String getURL(URL url) throws IOException {
        final InputStream in = makeURLStream(url);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            copy(in, out);
            return out.toString();
        } finally {
            in.close();
        }
    }
}


