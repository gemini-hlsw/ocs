//
// $Id: DefaultRecord.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.nio.ByteBuffer;

/**
 * A default implementation of the {@link Record} interface.
 */
public final class DefaultRecord implements Record {
    private long _pos;
    private ByteBuffer _buf;

    /**
     * Constructs with the file offset (position) and the bytes of which this
     * record is comprised.
     *
     * @param pos offset in the corresponding FITS file where this record
     * starts
     *
     * @param buf bytes that make up this record; no copy of the given
     * buffer is made so subsequent modification of <code>buf</code> will
     * be visible to this instance
     */
    public DefaultRecord(long pos, ByteBuffer buf) {
        _pos = pos;
        _buf = buf;
    }

    /**
     * Gets the offset in the corresponding FITS file where this record begins.
     *
     * @return offset from the start of the corresponding FITS file where this
     * record begins
     */
    public long position() {
        return _pos;
    }

    /**
     * Gets a reference to the bytes contained in this record.  A simple
     * reference is returned so any modifications will be visible to this
     * instance.  However there is no guarantee that modifications
     * made will be reflected to the FITS file on disk.  Generally this will
     * not be the case.
     *
     * @return a reference to the bytes in this record in a ByteBuffer
     */
    public ByteBuffer getBuffer() {
        return _buf;
    }
}
