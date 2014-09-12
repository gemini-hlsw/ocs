//
// $Id: FileUtil.java 6724 2005-11-09 18:16:29Z shane $
//

package edu.gemini.shared.util;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for working with files and directories.  See also
 * {@link FileFilterDecorator}.
 */
public final class FileUtil {

    private static final Logger LOG = Logger.getLogger(FileUtil.class.getName());

    /**
     * Return the suffix portion of the file's name .
     * The suffix is converted to lower case.
     */
    public static String getSuffix(File f) {
        if (f != null) {
            String filename = f.getName();
            return getSuffix(filename);
        }
        return null;
    }

    /**
     * Return the suffix portion of the file's name .
     * The suffix is converted to lower case.
     */
    public static String getSuffix(String filename) {
        if (filename != null) {
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    /**
     * Return the prefix portion of the file's name.
     */
    public static String getPrefix(File f) {
        if (f != null) {
            String filename = f.getName();
            return getPrefix(filename);
        }
        return null;
    }

    /**
     * Return the prefix portion of the file's name.
     */
    public static String getPrefix(String filename) {
        if (filename != null) {
            int i = filename.lastIndexOf('.');
            if (i > 0) return filename.substring(0, i);
        }
        return null;

    }

    /**
     * Sets the suffix of the file path.  If the path already
     * has a suffix (an extension), it will be removed and the specified
     * suffix added.
     *
     * @return The path with the requested suffix added.
     */
    public static String setSuffix(String path, String suffix) {
        if (suffix == null) return removeSuffix(path);
        if (path != null) {
            return (removeSuffix(path) + "." + suffix);
        }
        return null;
    }

    /**
     * Removes any suffix from specified file path.
     *
     * @return The path with the suffix if any removed.
     */
    public static String removeSuffix(String path) {
        if (path != null) {
             int i = path.lastIndexOf('.');
            if (i > 0 && i < path.length() - 1) {
                return path.substring(0, i);
            }
            return path;
        }
        return null;
    }


    /**
     * Figures out the common path prefix, if any, of all the files in the given
     * <code>files</code> array.  The common path prefix is part of their path
     * that they all have in common.
     *
     * @param files collection of files that should be examined to extract the
     * common path prefix
     *
     * @return common prefix that all the files in <code>files</code> have in
     * common
     */
    public static String getCommonPrefix(File[] files) {
        if (files.length == 0) return "";

        // Get the path separator as a regular expression.  A backslash must
        // be quoted.
        String pathEx = File.separator;
        if ("\\".equals(pathEx)) pathEx = "\\\\";

        // Set the initial common path prefix.
        List commonPrefix = new ArrayList();
        File parent = files[0].getParentFile();
        if (parent == null) return "";

        String[] path = parent.getPath().split(pathEx);
        for (int i=0; i<path.length; ++i) {
            commonPrefix.add(path[i]);
        }

        // Figure out the common prefix for the remainder of the files.
        for (int i=1; (i<files.length) && (commonPrefix.size() > 0); ++i) {
            parent = files[i].getParentFile();
            if (parent == null) return "";

            String[] pathElements = parent.getPath().split(pathEx);
            int index = -1;
            for (int j=0; (j<pathElements.length) && (j<commonPrefix.size()); ++j) {
                String pathElement = pathElements[j];
                if (!pathElement.equals(commonPrefix.get(j))) {
                    index = j;
                    break;
                }
            }

            if (index != -1) {
                commonPrefix = commonPrefix.subList(0, index);
            } else if (pathElements.length < commonPrefix.size()) {
                commonPrefix = commonPrefix.subList(0, pathElements.length);
            }
        }

        // Reconstitute the common prefix into a string.
        StringBuffer buf = new StringBuffer();
        for (Iterator it=commonPrefix.iterator(); it.hasNext(); ) {
            buf.append(it.next()).append(File.separator);
        }
        return buf.toString();
    }

    /**
     * Gets a File with the shortest path to the given <code>file</code>,
     * choosing between the files's existing path and a relative path to file
     * from the reference point of <code>absoluteDir</code>.
     *
     * <p>For example if:<pre>
     *  absoluteDir = "/home/swalker/tmp"
     *  file        = "/home/swalker/xyz/id"
     * </pre>
     * then the return value is <pre>"../xyz/id"</pre>.  However, if:<pre>
     *  absoluteDir = "/usr/local/bin"
     *  file        = "/home/swalker/xyz/id"
     * </pre>
     * then the return value is <pre>"/home/swalker/xyz/id"</pre>.
     *
     * @param absoluteDir directory reference point from which the relative
     * path to <code>file</code> will be created
     * @param file file to which a shortest path should be calcualted
     */
    public static File getShortestPath(File absoluteDir, File file) {
        File relFile = getRelativeFile(absoluteDir, file);
        if (file.getPath().length() <= relFile.getPath().length()) {
            return file;
        }
        return relFile;
    }

    /**
     * Gets a File representing the path to <code>file</code> relative to
     * <code>absoluteDir</code>.  If <code>file</code> does not already
     * represent an absolute path, it will first be converted into an
     * absolute path relative to the current working directory.  It's probably
     * best to only use this code with absolute file names.
     *
     * <p>For example if:<pre>
     *  absoluteDir = "/home/swalker/tmp"
     *  file        = "/home/swalker/xyz/id"
     * </pre>
     *
     * then the return value is <pre>"../xyz/id"</pre>.
     *
     * <p><em>WARNING</em>: This code only works on UNIX platforms because I am
     * lazy.
     *
     * @param absoluteDir directory reference point from which the relative
     * path to <code>file</code> will be created
     * @param file file to which an absolute path should be calcualted
     *
     * @return File with a path that is relative to the given
     * <code>absoluteDir</code>
     */
    public static File getRelativeFile(File absoluteDir, File file) {
        String filePath = file.getAbsolutePath();
        String dirPath = absoluteDir.getAbsolutePath();

        // Special case root -- only works on UNIX of course...
        if (dirPath.equals(File.separator)) return file;
        StringBuffer buf = new StringBuffer();
        while (!filePath.startsWith(dirPath)) {
            absoluteDir = absoluteDir.getParentFile();
            dirPath = absoluteDir.getPath();
            buf.append("..").append(File.separatorChar);
        }
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath + File.separator;
        }
        filePath = filePath.substring(dirPath.length());
        buf.append(filePath);
        return new File(buf.toString());
    }

    /**
     * Deletes a directory and all of its contents.  Symlinks are not
     * followed.
     *
     * @return <code>true</code> only if completely successful
     */
    public static boolean deleteDir(File dir) {
        // Work with the canonical path.  In testing whether a file is a
        // symbolic link, we compare the absolute path to the canonical
        // path.  If the are different, we assume the file is a symlink.
        // But if the path contains a symlink in its ancestry, then we
        // don't want this to falsely make us believe the file to be
        // deleted is a symlink.  Hence, we work with canonical path.
        try {
            dir = dir.getCanonicalFile();
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Problem get canonical path", ex);
            return false;
        }
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        });
        boolean res = true;
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                if (!files[i].delete()) {
                    LOG.log(Level.WARNING, "Could not delete file: " + files[i].getAbsolutePath());
                    res = false;
                }
            }
        }
        files = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                File f = files[i];

                // Determine if the file is a symbolic link.
                boolean isSymlink;
                try {
                    isSymlink = isSymbolicLink(f);
                } catch (IOException e) {
                    res = false;
                    LOG.log(Level.WARNING, "Problem checking for symlink", e);
                    continue;
                }
                if (isSymlink) {
                    // This is a symlink, so just delete it (don't follow).
                    if (!f.delete()) {
                        LOG.log(Level.WARNING, "Could not delete symlink: " + f.getAbsolutePath());
                        res = false;
                    }
                } else if (!deleteDir(f)) {
                    LOG.log(Level.WARNING, "Could not delete directory: " + f.getAbsolutePath());
                    res = false;
                }
            }
        }
        if (!dir.delete()) res = false;
        return res;
    }

    /**
     * Determine whether the given file is a symbolic link.
     *
     * @param file file to test
     *
     * @return <code>true</code> if the file is a symbolic link,
     * <code>false</code> otherwise
     */
    public static boolean isSymbolicLink(File file) throws IOException {
        String canonicalPath = file.getCanonicalPath();
        String absolutePath = file.getAbsolutePath();
        return !canonicalPath.equals(absolutePath);
    }

    /**
     * Makes the given directory and its parents.  This method uses the
     * existing File.mkdirs() method, but tries a specified number of times
     * upon failure.  For some reason, the mkdirs() method has failed on
     * my Linux box once or twice for no apparent reason.
     *
     * @param dir directory to create, along with any missing parent directories
     * @param tryCount number of times to try to create the directory, should
     * the first attempt fail
     * @param sleep how long to sleep between retry attempts
     *
     * @return <code>true</code> if the directory is actually created,
     * <code>false</code> otherwise
     */
    public static boolean mkdirs(File dir, int tryCount, long sleep) throws InterruptedException {
        int curTry = 0;
        while (!dir.exists() && !dir.mkdirs()) {
            LOG.log(Level.WARNING, "Couldn't make the directory: " + dir.getPath());
            if (++curTry == tryCount) return false;
            Thread.sleep(sleep);
        }
        return true;
    }

    /**
     * Copies the given file to the given destination.
     *
     * @param srcFile file to copy
     * @param dstFile destination file or directory into which the source will
     * be written
     * @param append if dstFile is not a directory and does exist, then if
     * <code>append</code> is <code>true</code>, the source file will be
     * appended to the destination rather than replacing it
     */
    public static void copy(File srcFile, File dstFile, boolean append) throws IOException {
        if (dstFile.isDirectory()) {
            if (append) {
                throw new IOException("Cannot append to a directory.");
            }
            dstFile = new File(dstFile, srcFile.getName());
        }
        FileInputStream fis = new FileInputStream(srcFile);
        FileOutputStream fos = new FileOutputStream(dstFile.getPath(), append);
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        byte[] buf = new byte[8192]; // 8K
        try {
            bis = new BufferedInputStream(fis);
            bos = new BufferedOutputStream(fos);
            int res = bis.read(buf, 0, buf.length);
            while (res >= 0) {
                bos.write(buf, 0, res);
                res = bis.read(buf, 0, buf.length);
            }
        } finally {
            if (bis != null) try { bis.close(); } catch (Exception ex) {}
            if (bos != null) try { bos.close(); } catch (Exception ex) {}
        }
    }

    /**
     * Reads the given file's content into a String.
     */
    public static String readFile(File f) throws IOException {
        return readFile(f, "UTF-8");
    }

    /**
     * Reads the given file's content into a String using the specified
     * character set encoding.
     */
    public static String readFile(File f, String charset) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Read file: " + f + " using charset: " + charset);
        }

        FileInputStream fis = new FileInputStream(f);
        return readInputStream(fis, charset);
    }

    public static String readInputStream(InputStream fis, String charset) throws IOException {
        StringBuffer buf = new StringBuffer();
        InputStreamReader isr = new InputStreamReader(fis, charset);
        BufferedReader br = new BufferedReader(isr);
        try {
            String line = br.readLine();
            while (line != null) {
                buf.append(line).append("\n");
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return buf.toString();
    }

    /**
     * Writes the string to the given file.
     */
//    public static void writeString(File file, String fileContent)
//            throws IOException {
//        writeString(file, fileContent, Charset.forName("US-ASCII"));
//    }

    /**
     * Writes the string to the given file.
     */
//    public static void writeString(File file, String fileContent, Charset cset)
//            throws IOException {
//        FileOutputStream fos = new FileOutputStream(file);
//        OutputStreamWriter osw = new OutputStreamWriter(fos, cset);
//        BufferedWriter bw = new BufferedWriter(osw);
//        try {
//            bw.write(fileContent);
//        } finally {
//            bw.close();
//        }
//    }

    /**
     * Writes the string to the given file.
     */
    public static void writeString(File file, String script) throws IOException {
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(file);
            fwriter.write(script);
        } finally {
            if (fwriter != null) try { fwriter.close(); } catch (Exception ignore) {}
        }
    }
    /**
     * Concatentates the given files into one file.
     *
     * @param files input files to be concatenated
     * @param out output file into which the inputs should be concatenated
     * @param append whether to append to the output file
     * (if <code>true</code>) or overwrite (if <code>false</code>)
     */
    public static void concat(File[] files, File out, boolean append)
            throws IOException {

        BufferedOutputStream bos = new BufferedOutputStream(
                                new FileOutputStream(out.getPath(), append));

        byte[] buf = new byte[8096];
        try {
            for (int i=0; i<files.length; ++i) {
                BufferedInputStream bis = new BufferedInputStream(
                                             new FileInputStream(files[i]));
                try {
                    int res;
                    while ((res = bis.read(buf, 0, buf.length)) >= 0) {
                        bos.write(buf, 0, res);
                    }
                } finally {
                    bis.close();
                }
            }
            bos.flush();

        } finally {
            bos.close();
        }
    }


    // -- taken fron jsky/util/FileUtil --

    /**
     * Copy the given input stream to the given output stream.
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[8 * 1024];
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) break;
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }


    /** Return the contents of the URL as a String */
    public static String getURL(URL url) throws IOException {
        InputStream in = makeURLStream(url);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            copy(in, out);
            return out.toString();
        } finally {
            in.close();
        }
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
}

