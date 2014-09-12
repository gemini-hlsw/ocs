package edu.gemini.pot.sp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates a rather expensive way of checking whether two arbitrary
 * objects are the same.  We need a way of determining when two data objects
 * actually have the same content.
 */
public final class DataObjectBlob {
    private static final DataObjectBlob EMPTY = new DataObjectBlob(new byte[0], 0);
    private static final Logger LOG = Logger.getLogger(DataObjectBlob.class.getName());

    private final byte[] bytes;
    private final int size;

    private DataObjectBlob(byte[] bytes, int size) {
        this.bytes = bytes;
        this.size  = size;
    }

    public static boolean same(Object o1, Object o2) {
        if (o1 == o2) return true;
        if ((o1 == null) || (o2 == null)) return false;
        return same(serialize(o1), serialize(o2));
    }

    private static boolean same(final DataObjectBlob dob1, final DataObjectBlob dob2) {
        if (dob1.size != dob2.size) return false;
        for (int i=0; i<dob1.size; ++i) {
            if (dob1.bytes[i] != dob2.bytes[i]) return false;
        }
        return true;
    }

    private static DataObjectBlob serialize(Object o) {
        // Provides direct access to the internal buffer, avoiding a copy.
        class SketchyByteArrayOutputStream extends ByteArrayOutputStream {
            SketchyByteArrayOutputStream() { super(2048); }
            public byte[] rawBuffer() { return buf; }
        }

        final SketchyByteArrayOutputStream baos = new SketchyByteArrayOutputStream();
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Problem serializing: " + o, ex);
            return DataObjectBlob.EMPTY;
        }

        return new DataObjectBlob(baos.rawBuffer(), baos.size());
    }
}
