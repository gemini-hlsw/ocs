//
// $Id: HeaderItemFormat.java 354 2006-05-26 22:20:43Z shane $
//

package edu.gemini.fits;

import java.util.Formatter;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;


/**
 * A simple immutable {@link edu.gemini.fits.HeaderItem} implementation.
 */
public final class HeaderItemFormat {
    private static final Logger LOG = Logger.getLogger(HeaderItemFormat.class.getName());

    /**
     * Encodes a String value such that it is suitable for inclusion in a FITS
     * header.  In particular, all occurances of the single quote character
     * <code>'</code> are replaced with two single quote characters:
     * <code>''</code>
     *
     * @param rawStrValue string to be encoded
     *
     * @return encoded version of <code>rawStrValue</code>
     */
    public static String encodeStringValue(String rawStrValue) {
        return rawStrValue.replaceAll("'","''");
    }

    /**
     * Decodes a String value taken from a FITS header.  In particular, double
     * single quote characters <code>''</code> are replaced with a single
     * single quote character: <code>'</code>
     *
     * @param encodedStrValue encoded string value
     *
     * @return decoded String value
     */
    public static String decodeStringValue(String encodedStrValue) {
        return encodedStrValue.replaceAll("''", "'");
    }

    /**
     * Parses the given <code>itemStr</code> into a HeaderItem.  The
     * <code>itemStr</code> must be exactly 80 characters and must contain a
     * valid FITS header item.  This method may be used to parse FITS files
     * to extract the HeaderItems.
     *
     * @param itemStr 80 character String
     *
     * @return the HeaderItem represented by the given <code>itemStr</code>
     *
     * @throws FitsException if <code>itemStr</code> cannot be interpreted as
     * a valid FITS header item
     */
    public static HeaderItem parse(String itemStr) throws FitsParseException {
        if (itemStr.length() != FitsConstants.HEADER_ITEM_SIZE) {
            throw new FitsParseException("Header card too long: [" + itemStr +
                                         ']');
        }

        // Get the keyword.
        String keyword = itemStr.substring(0, 8).trim();

        // Does it contain a value?  If not return a comment header card.
        if (itemStr.charAt(8) != '=') {
            String comment = itemStr.substring(8).trim();
            return DefaultHeaderItem.createComment(keyword, comment);
        }

        // If the value starts with a single quote, then it is a string value.
        if (itemStr.charAt(10) == '\'') {
            int end = -1;
            StringBuilder buf = new StringBuilder(68); // max string size
            int s = 11;
            int i = itemStr.indexOf('\'', s);
            while (i != -1) {
                buf.append(itemStr.substring(s, i));
                if (i == 79) {
                    end = 80;
                    break;
                }

                s = i+1;
                char next = itemStr.charAt(s);
                if (next == '\'') {
                    buf.append('\'');
                    ++s;
                } else {
                    end = s;
                    break;
                }
                i = itemStr.indexOf('\'', s);
            }
            if (i == -1) {
                throw new FitsParseException("Missing closing ': [" + itemStr +
                                              ']');
            }

            String comment = null;
            if (end < 80) {
                comment = itemStr.substring(end).trim();
                if ((comment.length() > 0) && (comment.charAt(0) == '/')) {
                    comment = comment.substring(1).trim();
                }
            }
            return DefaultHeaderItem.create(keyword, buf.toString(), comment);
        }

        // Otherwise, it's some other value.  Find the comment, if there is one.
        int commentStart = itemStr.indexOf('/', 10);
        if (commentStart == -1) {
            return new DefaultHeaderItem(keyword, itemStr.substring(10).trim(),
                                         null, false);
        }

        String value   = itemStr.substring(10, commentStart).trim();
        String comment = null;
        if (commentStart < 79) {
            comment = itemStr.substring(commentStart+1).trim();
        }

        return new DefaultHeaderItem(keyword, value, comment, false);
    }

    /**
     * Formats the given HeaderItem into an 80 character String as it would
     * appear in a FITS header.  Use {@link #toBytes(HeaderItem)} or
     * {@link #toByteBuffer(HeaderItem)} for a more convenient representation
     * for IO to a FITS file.
     *
     * @return string representation of the given HeaderItem as it would appear
     * in a FITS header
     */
    public static String format(HeaderItem item) {
        StringBuilder buf = new StringBuilder(80);

        Formatter f = new Formatter(buf);
        f.format("%1$-8.8s", item.getKeyword());

        String value   = item.getValue();
        String comment = item.getComment();
        if (value == null) {
            String com = comment;
            if (com == null) com = "";
            if (com.length() <= 70) {
                buf.append("  ");
                f.format("%1$-70.70s", com);
            } else {
                f.format("%1$72.72s", com);
            }
        } else {
            // Append the equals sign and trailing space.
            buf.append("= ");

            // Append the value.
            if (item.isStringValue()) {
                // If this is a string, then the standard is to quote it with
                // single quotes, and make the the quoted string contain 8
                // characters at a minimum with trailing blanks.
                buf.append('\'');
                f.format("%1$-8.68s", encodeStringValue(value));
                buf.append('\'');
                int pos = buf.length();

                // Pad a short string value out to the comment line.
                if (pos < 30) {
                    StringBuilder spec = new StringBuilder(6);
                    spec.append("%1$").append(30-pos).append('s');
                    f.format(spec.toString(), "");
                }
            } else {
                // Not a sing, so it may take the entire 70 remaining
                // characters, but pad out to 20 if less than that with leading
                // blanks.
                f.format("%1$20.70s", value);
            }

            // Append the comment, or blanks if no comment.
            int pos = buf.length();
            if (comment == null) {
                StringBuilder spec = new StringBuilder(6);
                spec.append("%1$").append(80-pos).append('s');
                f.format(spec.toString(), "");
            } else {
                pos += 3; // for " / ";
                if (pos < 80) buf.append(" / ");
                int w = 80 - pos;
                StringBuilder spec = new StringBuilder(10);
                spec.append("%1$-").append(w).append('.').append(w).append('s');
                f.format(spec.toString(), comment);
            }
        }

        return buf.toString();
    }

    /**
     * Gets the representation of the HeaderItem in a byte array suitable for
     * writing into a FITS header.
     *
     * @return bytes representing this HeaderItem in a FITS header
     */
    public static byte[] toBytes(HeaderItem item) {
        try {
            return format(item).getBytes(FitsConstants.CHARSET_NAME);
        } catch (UnsupportedEncodingException ex) {
            // This should never happen...
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException("Unsupported charset: " +
                    FitsConstants.CHARSET_NAME, ex);
        }
    }

    /**
     * Gets the representation of the HeaderItem in a byte array suitable for
     * writing into a FITS header.
     *
     * @return bytes representing this HeaderItem in a FITS header
     */
    public static ByteBuffer toByteBuffer(HeaderItem item) {
        return ByteBuffer.wrap(toBytes(item));
    }
}
