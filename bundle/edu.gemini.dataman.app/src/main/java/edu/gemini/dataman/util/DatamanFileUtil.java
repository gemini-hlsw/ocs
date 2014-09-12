//
// $Id: DatamanFileUtil.java 201 2005-10-14 20:30:52Z shane $
//

package edu.gemini.dataman.util;

import edu.gemini.file.util.LockedFileChannel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A few file utilities used by the Dataman app.
 */
public final class DatamanFileUtil {
    private static final Logger LOG = Logger.getLogger(DatamanFileUtil.class.getName());

    private static void _closeChannel(FileChannel c, File f) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not close file: " + f.getPath(), ex);
        }
    }

    private static void _releaseLock(FileLock l, File f) {
        if (l == null) return;
        try {
            l.release();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not unlock file: " + f.getPath(), ex);
        }
    }

    private static void release(LockedFileChannel lfc) {
        if (lfc == null) return;
        try {
            if (lfc.isHeldByCurrentThread()) lfc.unlock();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not unlock file: " + lfc.getFile().getPath(), ex);
        } finally {
            try {
                lfc.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Could not close file: " + lfc.getFile().getPath(), ex);
            }
        }
    }

    public static void copyFile(File src, File dest) throws IOException, InterruptedException {

        LockedFileChannel lfc_in = null;
        LockedFileChannel lfc_out= null;

        try {
            lfc_in  = new LockedFileChannel(src, LockedFileChannel.Mode.r);
            lfc_in.lock();

            lfc_out = new LockedFileChannel(dest, LockedFileChannel.Mode.rw);
            lfc_out.lock();

            FileChannel fc_out = lfc_out.getChannel();
            copy(lfc_in.getChannel(), fc_out);
            fc_out.force(true);

        } finally {
            release(lfc_out);
            release(lfc_in);
        }
    }

    public static void copy(ReadableByteChannel src, WritableByteChannel dest)
            throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(16*1024);
        while (src.read(buf) != -1) {
            buf.flip();
            while (buf.hasRemaining()) {
                dest.write(buf);
            }
            buf.clear();
        }
    }

    public static String md5HexString(File f) throws IOException, InterruptedException {
        byte[] bA = md5(f);
        StringBuilder buf = new StringBuilder();

        for (byte b : bA) {
            buf.append(String.format("%02x", b));
        }

        return buf.toString();
    }

    public static byte[] md5(File f) throws IOException, InterruptedException {
        return digest(f, "MD5");
    }

    public static byte[] digest(File f, String algorithm) throws IOException, InterruptedException {
        LockedFileChannel lfc = null;
        try {
            lfc = new LockedFileChannel(f, LockedFileChannel.Mode.r);
            lfc.lock();
            return digest(lfc.getChannel(), algorithm);
        } finally {
            release(lfc);
        }
    }

    public static byte[] digest(ReadableByteChannel c, String algorithm)
            throws IOException {
        MessageDigest dig;
        try {
            dig = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("Bad digest algorithm: " + algorithm);
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(16*1024);
        while (c.read(buf) != -1) {
            buf.flip();
            dig.update(buf);
            buf.clear();
        }
        return dig.digest();
    }

    public static String crcHexString(File f) throws IOException, InterruptedException {
        long crc = crc(f);
        return String.format("%08x", crc);
    }

    public static long crc(File f) throws IOException, InterruptedException {
        LockedFileChannel lfc = null;
        try {
            lfc = new LockedFileChannel(f, LockedFileChannel.Mode.r);
            lfc.lock();
            return crc(lfc.getChannel());
        } finally {
            release(lfc);
        }
    }

    public static long crc(ReadableByteChannel c) throws IOException {
        CRC crc = new CRC();
        ByteBuffer buf = ByteBuffer.allocate(16*1024);
        while (c.read(buf) != -1) {
            buf.flip();
            crc.update(buf.array(), 0, buf.limit());
            buf.clear();
        }
        return crc.getValue();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        File src = new File("/Users/swalker/Desktop/S20070428S0029.fits");
        System.out.println(crcHexString(src));
        /*
        File dest = new File("/tmp", src.getName());

        long t1 = System.currentTimeMillis();
        copyFile(src, dest);
        long t2 = System.currentTimeMillis();
        byte[] md5_src = md5(src);
        long t3 = System.currentTimeMillis();
        byte[] md5_dest = md5(dest);
        long t4 = System.currentTimeMillis();

        System.out.println("copy time     = " + (t2 - t1));
        System.out.println("md5 src time  = " + (t3 - t2));
        System.out.println("md5 dest time = " + (t4 - t3));

        System.out.println("md5 src       = " + Arrays.toString(md5_src));
        System.out.println("md5 dest      = " + Arrays.toString(md5_dest));
        */
    }
}

