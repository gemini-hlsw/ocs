//
// $Id: RecordIterator.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * Iterates over the 2880 byte records in a FITS file.
 */
public final class RecordIterator {
    private static final int NUM_BUFS = 2;

    /**
     * Iterates over the given channel.  Assumes that no bytes have been read
     * from the channel, or else that an integral number of records have been
     * read.  Otherwise, the {@link Record}s returned will not be aligned
     * correctly.
     *
     * @param channel the channel from which the records will iterated
     *
     * @return a RecordIterator ready to produce the contained FITS
     * {@link Record}s
     *
     * @throws IOException if there is a problem reading from the channel
     */
    public static RecordIterator iterateFromBeginning(ScatteringByteChannel channel)
            throws IOException {
        return new RecordIterator(channel, 0);
    }

    /**
     * Iterates over the given FileChannel.  Resets the position of the
     * FileChannel if necessary to ensure that it is aligned on an integral
     * number of FITS records.
     *
     * @param channel the channel from which the records will iterated
     *
     * @return a RecordIterator ready to produce the contained FITS
     * {@link Record}s
     *
     * @throws IOException if there is a problem reading from the channel
     */
    public static RecordIterator iterateFile(FileChannel channel)
            throws IOException {

        long filePos = channel.position();

        // Get lined up on a record boundary.
        int over = (int) (filePos % FitsConstants.RECORD_SIZE);
        if (over != 0) {
            filePos = filePos - over;
            channel.position(filePos);
        }

        return new RecordIterator(channel, filePos);
    }

    private ScatteringByteChannel _chan;
    private long _filePos;
    private ByteBuffer[] _bufs;
    private int _bufIndex;
    private int _bufLimit;

    private RecordIterator(ScatteringByteChannel channel, long pos) throws IOException {
        _chan = channel;
        _bufs = new ByteBuffer[NUM_BUFS];
        for (int i=0; i<NUM_BUFS; ++i) {
            _bufs[i] = ByteBuffer.allocateDirect(FitsConstants.RECORD_SIZE);
        }
        _filePos = pos;

        // Fill up the buffers.
        _fillBuffs();
    }

    private void _fillBuffs() throws IOException {
        for (int i=0; i<NUM_BUFS; ++i) {
            _bufs[i].clear();
        }

        //noinspection PointlessArithmeticExpression
        long cap = _bufs[0].capacity() * NUM_BUFS;

        long total = 0;
        do {
            long read = _chan.read(_bufs);
            if (read == -1) break;
            total += read;
        } while (total < cap);

        _bufIndex = 0;
        _bufLimit = (int) total / FitsConstants.RECORD_SIZE;

        for (int i=0; i<_bufLimit; ++i) {
            _bufs[i].flip();
        }
    }

    public boolean hasNext() throws IOException {
        if (_bufIndex < _bufLimit) return true;
        if (_bufLimit == 0) return false;

        _fillBuffs();
        return (_bufIndex < _bufLimit);
    }

    public Record next() {
        ByteBuffer buf = _bufs[_bufIndex++];
        Record rec = new DefaultRecord(_filePos, buf);
        _filePos += FitsConstants.RECORD_SIZE;
        return rec;
    }
}
