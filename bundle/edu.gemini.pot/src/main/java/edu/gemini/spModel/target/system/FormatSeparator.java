// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: FormatSeparator.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

/**
 * <code>FormatSeparator</code> is an enumerated type for keepting
 * track of the symbol that appears within a coordinate value.
 */
public final class FormatSeparator extends TypeBase {
    /**
     * FormatSeparator character constants.
     **/
    public static final char SPACE_SEPARATOR = ' ';
    public static final char COLON_SEPARATOR = ':';
    public static final char HOUR_LETTER_SEPARATOR = 'h';
    public static final char MINUTE_LETTER_SEPARATOR = 'm';
    public static final char SECOND_LETTER_SEPARATOR = 's';
    public static final char DEGREE_LETTER_SEPARATOR = 'd';

    public static final int _COLON = 0;
    public static final int _SPACES = 1;
    public static final int _LETTERS = 2;

    public static final FormatSeparator COLON =
            new FormatSeparator(_COLON, "colon separator");

    public static final FormatSeparator SPACES =
            new FormatSeparator(_SPACES, "space separator");

    public static final FormatSeparator LETTERS =
            new FormatSeparator(_LETTERS, "letter separator");

    public static final FormatSeparator[] TYPES = new FormatSeparator[]{
        COLON,
        SPACES,
        LETTERS,
    };

    private FormatSeparator(int type, String name) {
        super(type, name);
    }

}
