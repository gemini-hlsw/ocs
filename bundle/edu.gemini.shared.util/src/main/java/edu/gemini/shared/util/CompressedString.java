package edu.gemini.shared.util;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressedString implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final int BUF_SIZE = 1024;
	private static final String ENCODING = "UTF-8";
	private static final Logger LOGGER = Logger.getLogger(CompressedString.class.getName());

	private byte[] data = null;

	public CompressedString() {
	}

	public CompressedString(String string) {
		set(string);
	}

    public CompressedString(CompressedString that) {
        byte[] thatdata = that.data;
        if (thatdata == null) return;
        
        data = new byte[thatdata.length];
        System.arraycopy(thatdata, 0, data, 0, data.length);
    }

    public void set(String string) {
		if (string == null) {
			data = null;
		} else {
			try {
				Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
				deflater.setInput(string.getBytes(ENCODING));
				deflater.finish();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[BUF_SIZE];
				while (!deflater.finished()) {
					int len = deflater.deflate(buf);
					baos.write(buf, 0, len);
				}

				// If you don't call end(), the deflater leaks system heap on Linux.
				// This is documented in 6293787. If you do call it, it still seems to
				// leak, just very slowly. Running finalizers seems to fix it.
				deflater.end();
				System.runFinalization();

				data = baos.toByteArray(); // Atomic write; don't need to synchronize.
				if (data.length > string.length() * 2)
					LOGGER.fine("Compressed string is larger than source: " + string);
			} catch (UnsupportedEncodingException uee) {
				throw new Error(uee);
			}
		}
	}

	public String get() {
		if (data == null) {
			return null;
		} else {
			try {
				Inflater inflater = new Inflater();
				inflater.setInput(data);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[BUF_SIZE];
				while (!inflater.needsInput()) {
					int len = inflater.inflate(buf);
					baos.write(buf, 0, len);
				}
				return new String(baos.toByteArray(), ENCODING);
			} catch (UnsupportedEncodingException uee) {
				throw new Error(uee);
			} catch (DataFormatException dfe) {
				throw new Error(dfe);
			}
		}
	}

	public int size() {
		return data == null ? 0 : data.length;
	}

    public byte[] md5() {
        byte[] tmp = data;
        MessageDigest dig;
        try {
            dig = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Bad digest algorithm: MD5");
        }
        if (tmp != null) dig.update(tmp);
        return dig.digest();
    }

    public static void main(String[] args) {
		System.out.println("Creating many compressed strings...");
		for (int i = 0; ; i++) {
			new CompressedString("foobar");
			if (i % 1000 == 0)
				System.out.println(i);
		}
	}



}
