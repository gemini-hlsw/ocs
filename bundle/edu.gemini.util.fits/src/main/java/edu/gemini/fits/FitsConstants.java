//
// $Id: FitsConstants.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

import java.nio.charset.Charset;

/**
 * Commonly used constants when dealing with FITS files.
 */
public final class FitsConstants {

    private FitsConstants() {
        // defeat instantiation
    }

    /**
     * The size of a logical FITS record in bytes.  Every FITS file contains
     * an integral number of records of this size.
     */
    public static final int RECORD_SIZE      = 2880;

    /**
     * The size of a single header "card image" in bytes.  That is, the number
     * of bytes that each header value occupies.
     */
    public static final int HEADER_ITEM_SIZE  =   80;

    /**
     * The number of header items that fit on a record.
     */
    public static final int ITEMS_PER_RECORD = RECORD_SIZE/HEADER_ITEM_SIZE;

    /**
     * Name of the standard FITS character set used for header information.
     */
    public static final String CHARSET_NAME = "US-ASCII";

    /**
     * Standard FITS character set used for header information.
     */
    public static final Charset CHARSET = Charset.forName(CHARSET_NAME);
}
