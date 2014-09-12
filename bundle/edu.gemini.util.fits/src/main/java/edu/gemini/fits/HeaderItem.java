//
// $Id: HeaderItem.java 37 2005-08-20 17:46:18Z shane $
//

package edu.gemini.fits;

/**
 * A single header "card image" of 80 bytes.  Header items always have an
 * associated keyword and optionally have a value and/or comment.
 */
public interface HeaderItem {

    /**
     * Gets the name of the header item.
     */
    String getKeyword();

    /**
     * Returns <code>true</code> if this header item should be treated as a
     * quoted String.  Strings in FITS headers are quoted with single quote
     * characters: <code>'</code>.  Embedded single quote characters are
     * encoded as two consecutive single quotes: <code>''</code>.  This
     * method is used by the {@link HeaderItemFormat#format} method to
     * determine how to write the header item.
     *
     * @return <code>true</code> if the value contained in this header item
     * should be viewed as a String; <code>false</code> if there is no value
     * or if it should not be treated as a String
     */
    boolean isStringValue();

    /**
     * Gets the value (if any) of the header item as a String.
     *
     * @return value of the header item as a String; <code>null</code> if there
     * is no value for this header item
     */
    String getValue();

    /**
     * Gets the value of this header item as an int.
     *
     * @return value of this header card as an integer
     *
     * @throws NumberFormatException if the value cannot be parsed as an
     * integer
     * @throws NullPointerException if the value is <code>null</code>
     */
    int getIntValue();

    /**
     * Gets the value of this header item as a double.
     *
     * @return value of this header item as a double
     *
     * @throws NumberFormatException if the value cannot be parsed as a
     * double
     * @throws NullPointerException if the value is <code>null</code>
     */
    double getDoubleValue();

    /**
     * Gets the value of this header item as a boolean.
     *
     * @return value of this header item as a boolean; <code>true</code> if
     * the value is not <code>null</code> and equals the String "T"
     */
    boolean getBooleanValue();

    /**
     * Gets the comment associated with this header item, if any.
     *
     * @return associated comment, if any; <code>null</code> if none
     */
    String getComment();

    /**
     * Defines <code>equals()</code> based upon the keyword, value and comment
     * of this header item.  Two HeaderItem implementations, regardless of
     * concrete class, must be equivalent if their keyword, value and comment
     * are equivalent according to a <code>String.equals()</code> comparison
     * of like non-<code>null</code> values.
     *
     * @return <code>true</code> if two HeaderItems have the same keyword,
     * comment, and value
     */
    boolean equals(Object o);

    /**
     * Calculates the hash code to agree with the definition of
     * <code>equals()</code> above.  It is defined to be the result of this
     * calculation:
     * <pre>
     *   hashCode = keyword.hashCode();
     *   if (value != null) hashCode = 31*hashCode + value.hashCode();
     *   if (comment != null) hashCode = 31*hashCode + value.hashCode();
     *   hashCode = 31*hashCode + (isString ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode());
     * </pre>
     * This ensures that <code>headerItem1.equals(headerItem2)</code> implies
     * that <code>headerItem1.hashCode() == headerItem2.hashCode()</code> for
     * any two HeaderItems, <code>headerItem1</code> and
     * <code>headerItem2</code> as required by the general contract of
     * <code>Object.hashCode</code>.
     */
    int hashCode();
}
