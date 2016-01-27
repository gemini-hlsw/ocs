package edu.gemini.shared.util;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.*;

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
        File[] files = dir.listFiles(file -> !file.isDirectory());
        boolean res = true;
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    LOG.log(Level.WARNING, "Could not delete file: " + file.getAbsolutePath());
                    res = false;
                }
            }
        }
        files = dir.listFiles(File::isDirectory);
        if (files != null) {
            for (File f : files) {
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
    private static boolean isSymbolicLink(File file) throws IOException {
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
    private static String readFile(File f, String charset) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Read file: " + f + " using charset: " + charset);
        }

        FileInputStream fis = new FileInputStream(f);
        return readInputStream(fis, charset);
    }

    private static String readInputStream(InputStream fis, String charset) throws IOException {
        StringBuilder buf = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(fis, charset);
        try (BufferedReader br = new BufferedReader(isr)) {
            String line = br.readLine();
            while (line != null) {
                buf.append(line).append("\n");
                line = br.readLine();
            }
        }
        return buf.toString();
    }

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

}

