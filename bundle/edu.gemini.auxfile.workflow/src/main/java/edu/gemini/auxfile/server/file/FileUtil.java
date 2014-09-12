//
// $Id: FileUtil.java 411 2006-06-11 22:46:31Z shane $
//

package edu.gemini.auxfile.server.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with files and directories.
 */
public final class FileUtil {
    private static final Logger LOG = Logger.getLogger(FileUtil.class.getName());

    private static final int CHUNK_BUFFER_SIZE = 16 * 1024;

    private FileUtil() {
    }

    public static boolean deleteDir(File dir) {
        File[] ls = dir.listFiles();
        for (File f : ls) {
            if (f.isDirectory()) {
                deleteDir(f);
            } else {
                f.delete();
            }
        }
        return dir.delete();
    }

    private static void _closeChannel(FileChannel c, File f) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not close file: " + f.getPath(), ex);
        }
    }


    public static String readString(File f) throws IOException {
        FileChannel fcin = null;

        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            fcin = raf.getChannel();

            ByteBuffer bbuf = ByteBuffer.allocateDirect(2048);
            StringBuilder sbuf = new StringBuilder();
            while (fcin.read(bbuf) != -1) {
                bbuf.flip();
                byte[] bytes = new byte[bbuf.remaining()];
                bbuf.get(bytes);
                sbuf.append(new String(bytes));
                bbuf.clear();
            }
            return sbuf.toString();
        } finally {
            _closeChannel(fcin, f);
        }
    }

    public static void writeString(File f, String s) throws IOException {
        FileChannel fcout = null;

        try {
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            fcout = raf.getChannel();

            final int maxSubString = 2048;

            ByteBuffer bbuf = ByteBuffer.allocateDirect(maxSubString);

            int startPos = 0;
            int endPos   = Math.min(maxSubString, s.length());
            while (startPos < s.length()) {
                String sub = s.substring(startPos, endPos);
                bbuf.put(sub.getBytes("UTF-8"));
                bbuf.flip();
                startPos = endPos;
                endPos   = Math.min(startPos + maxSubString, s.length());

                do {
                    fcout.write(bbuf);
                } while (bbuf.hasRemaining());
                bbuf.clear();
            }
            fcout.force(true);
            fcout.truncate(s.length());

        } finally {
            _closeChannel(fcout, f);
        }
    }

    public static byte[] readChunk(File f, long startPos, int chunkSize)
            throws IOException {

        // Figure out how far to read, and adjust the chunk size if necessary to
        // not go beyond the end of the file.
        long endPos = Math.min(startPos + chunkSize, f.length());
        chunkSize = (int) (endPos - startPos);

        int bufSize = Math.min(chunkSize, CHUNK_BUFFER_SIZE);

        // Read the data from the chunk into a ByteBuffer.
        FileChannel fcin = null;
        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(bufSize);

            // Create the result byte array.
            byte[] res = new byte[chunkSize];

            // Open the file and position to start where reading should begin
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            fcin = raf.getChannel();
            fcin.position(startPos);

            long pos = startPos;
            while (pos < endPos) {
                int bytesRead = fcin.read(buf);
                if (bytesRead == -1) {
                    // shouldn't get here, unless the file was modified while
                    // being read
                    throw new IOException("unexpected EOF");
                }

                buf.flip();
                int offset = (int) (pos - startPos);
                buf.get(res, offset, buf.remaining());
                pos += bytesRead;

                buf.clear();
            }

            return res;
        } finally {
            _closeChannel(fcin, f);
        }
    }

    public static void writeChunk(File f, byte[] chunk)
            throws IOException {

        int bufSize = Math.min(chunk.length, CHUNK_BUFFER_SIZE);

        FileChannel fcout = null;
        try {
            ByteBuffer buf = ByteBuffer.allocateDirect(bufSize);

            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            fcout = raf.getChannel();
            fcout.position(raf.length());

            int pos = 0;
            while (pos < chunk.length) {
                int readLength = Math.min(bufSize, chunk.length-pos);
                buf.put(chunk, pos, readLength);
                buf.flip();

                while (buf.hasRemaining()) {
                    fcout.write(buf);
                }
                buf.clear();
                pos += readLength;
            }
            fcout.force(true);
        } finally {
            _closeChannel(fcout, f);
        }
    }


}
