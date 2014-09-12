//
// $Id: Record.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.nio.ByteBuffer;

/**
 * Representation of a section of a FITS file called a "logical record".
 * Every logical record contains {@link FitsConstants.RECORD_SIZE 2880}
 * bytes.
 */
public interface Record {

    /**
     * Gets the position of the record in the FITS file.  Since each record
     * is a fixed size, this should be an integral number of 2880 byte blocks.
     *
     * @return offset from the start of the file at which this block is found
     */
    long position();

    /**
     * Gets the bytes that compose this record.
     *
     * @return bytes contained in this record; a 2880 byte array
     */
    ByteBuffer getBuffer();
}
