//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.util.immutable.Option;

/**
 * A catalog row represents a single catalog result from a tabular catalog data
 * source.  There are convenience methods for interpreting the data in the
 * row in various expected formats.
 */
public interface CatalogRow {

    /**
     * Extracts the value in the given column without trying to interpret it.
     * For some columns in some catalogs, there is no
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row wrapped in a
     * <code>{@link edu.gemini.shared.util.immutable.Some}</code>, if any;
     * <code>{@link edu.gemini.shared.util.immutable.None}</code> otherwise
     */
    public Option<Object> get(int columnIndex);

    /**
     * Extracts the value in the indicated column and attempts to interpret and
     * return it as an {@link Angle} representing a right ascension.  String
     * values in HH:MM:SS format such as <code>12:34:56.78</code> must be
     * supported. Numeric values and String values that can be interpreted as
     * a number will be treated as an angle value in degrees.
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row interpreted as an
     * {@link Angle} representing a right ascension and wrapped in an
     * {@link Option}; see {@link #get}
     *
     * @throws CatalogException if there is a problem interpreting this value
     * as an RA
     */
    public Option<Angle> getRa(int columnIndex) throws CatalogException;

    /**
     * Extracts the value in the indicated column and attempts to interpret and
     * return it as an {@link Angle} representing a declination.  String
     * values in DD:MM:SS format such as <code>12:34:56.78</code> must be
     * supported.  Numeric values and String values that can be interpreted as
     * a number will be treated as an angle value in degrees.
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row interpreted as an
     * {@link Angle} representing a declination and wrapped in an
     * {@link Option}; see {@link #get}
     *
     * @throws CatalogException if there is a problem interpreting this value
     * as a declination
     */
    public Option<Angle> getDec(int columnIndex) throws CatalogException;

    /**
     * Extracts the value in the indicated column and attempts to interpret and
     * return it as an {@link Angle}.  Numeric values and String values that
     * can be parsed as a number will be treated as an angle value in degrees.
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row interpreted as an
     * {@link Angle} and wrapped in an {@link Option}; see {@link #get}
     *
     * @throws CatalogException if there is a problem interpreting this value
     * as an angle
     */
    public Option<Angle> getDegrees(int columnIndex) throws CatalogException;

    /**
     * Extracts the value in the indicated column and attempts to interpret and
     * return it as a double.  Numeric values and String values that
     * can be parsed as a number must be supporetd.
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row interpreted as a double value
     * and wrapped in an {@link Option}; see {@link #get}
     *
     * @throws CatalogException if there is a problem interpreting this value
     * as a double
     */
    public Option<Double> getDouble(int columnIndex) throws CatalogException;

    /**
     * Extracts the value in the indicated column and attempts to interpret and
     * return it as an integer.  Numeric values and String values that
     * can be parsed as a number must be supporetd.
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row interpreted as an integer value
     * and wrapped in an {@link Option}; see {@link #get}
     *
     * @throws CatalogException if there is a problem interpreting this value
     * as an integer
     */
    public Option<Integer> getInteger(int columnIndex) throws CatalogException;

    /**
     * Extracts the value in the indicated column and attempts to interpret and
     * return it as a string.
     *
     * @param columnIndex index of the column of interest
     *
     * @return corresponding value for this row interpreted as a string
     * and wrapped in an {@link Option}; see {@link #get}
     *
     * @throws CatalogException if there is a problem interpreting this value
     * as a String
     */
    public Option<String> getString(int columnIndex) throws CatalogException;
}
