//
// $Id: FileUtil.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Utility class for working with FITS files.
 */
public final class FileUtil {
    private static final int BUF_SIZE = 8 * 1024;

    /**
     * Inserts the bytes contained in <code>buf</code> into the FITS file at
     * the given position.
     *
     * @param channel channel associated with the file to update
     * @param buf buffer containing the bytes to be inserted in the file
     * @param pos position at which the bytes should be inserted
     *
     * @throws IOException if there is a problem writing to the channel
     */
    public static void insert(FileChannel channel, ByteBuffer buf, long pos)
            throws IOException {

        ByteBuffer tmp = ByteBuffer.allocate(BUF_SIZE);

        long end   = channel.size();
        long start = end - BUF_SIZE;
        int  size  = BUF_SIZE;

        if (start < pos) {
            start = pos;
            size  = (int) (end - start);
        }

        while (size > 0) {
            tmp.position(0);
            tmp.limit(size);

            _shift(channel, tmp, start, buf.remaining());

            end   = start;
            start = end - BUF_SIZE;
            if (start < pos) start = pos;
            size  = (int) (end - start);
        }

        // Write in the new bit
        writeBuf(channel, buf, pos);
    }

    /**
     * Reads from the given <code>channel</code> into the given buffer, starting
     * at file position <code>pos</code>.  Completely fills the buffer unless
     * the EOF is encountered.
     *
     * @param channel file channel from which to read
     * @param buf buffer to fill with data from the channel
     * @param pos file position from which to read
     *
     * @return number of bytes read
     *
     * @throws IOException if there is a problem reading from the channel
     */
    public static int readBuf(FileChannel channel, ByteBuffer buf, long pos)
            throws IOException {

        int bufsize = buf.remaining();

        // Fill the buffer completely, or until EOF.
        int totalRead = 0;
        while (totalRead < bufsize) {
            int read = channel.read(buf, pos+totalRead);
            if (read == -1) break;
            totalRead += read;
        }
        return totalRead;
    }

    /**
     * Writes to the given <code>channel</code> from the given buffer, starting
     * at file position <code>pos</code>.  Completely drains the buffer,
     * extending the size of the file if necessary.
     *
     * @param channel channel to which the data in the buffer should be written
     * @param buf buffer containing the data to write
     * @param pos file position to which to write
     *
     * @throws IOException if there is a problem writing to the channel
     */
    public static void writeBuf(FileChannel channel, ByteBuffer buf, long pos)
            throws IOException {

        int totalWritten = 0;
        while (buf.hasRemaining()) {
            totalWritten += channel.write(buf, pos+totalWritten);
        }
    }

    private static void _shift(FileChannel channel, ByteBuffer tmp, long pos, long shift)
            throws IOException {

        // Fill the buffer completely.
        int totalRead = readBuf(channel, tmp, pos);

        // Flip.
        tmp.position(0).limit(totalRead);

        // Write it out completely.
        writeBuf(channel, tmp, pos+shift);
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/Users/swalker/Desktop/tmp.fits");

        byte[] line = new byte[80];
        Arrays.fill(line, (byte) ' ');

        // Create a FITS record in a byte buffer.
        ByteBuffer buf = ByteBuffer.allocate(FitsConstants.RECORD_SIZE);
        for (int i=0; i<36; ++i) {
            String s = String.format("%02d", i);
            line[0] = (byte) s.charAt(0);
            line[1] = (byte) s.charAt(1);
            buf.put(line);
        }

        buf.flip();

        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        FileChannel chan = raf.getChannel();
        try {
            insert(chan, buf, FitsConstants.RECORD_SIZE);
        } finally {
            chan.close();
        }
    }
}
