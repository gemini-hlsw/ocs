//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.*;

/**
 * The CatalogValueExtractor simplifies the process of extracting information
 * from catalog results.  Constructed with a header and a row, the client can
 * access items from the row by name letting this class handle extracting the
 * data into expected types.
 */
public final class CatalogValueExtractor {

    /**
     * Groups the typical information related to a magnitude value.  In
     * particular, the band, the name of the column containing the brightness,
     * and (optionally) the name of the column containing the error of the
     * measured brightness.
     */
    public static final class MagnitudeDescriptor {
        private final Magnitude.Band band;
        private final String magColumn;
        private final Option<String> errorColumn;

        public MagnitudeDescriptor(Magnitude.Band band, String magnitudeColumn) {
            this(band, magnitudeColumn, null);
        }

        public MagnitudeDescriptor(Magnitude.Band band, String magnitudeColumn, String errorColumn) {
            this.band = band;
            this.magColumn = magnitudeColumn;
            if (errorColumn != null) {
                this.errorColumn = new Some<>(errorColumn);
            } else {
                this.errorColumn = None.instance();
            }
        }

        public Magnitude.Band getBand() {
            return band;
        }

        public String getMagnitudeColumn() {
            return magColumn;
        }

        public Option<String> getErrorColumn() {
            return errorColumn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MagnitudeDescriptor that = (MagnitudeDescriptor) o;

            return band == that.band && errorColumn.equals(that.errorColumn) && magColumn.equals(that.magColumn);
        }

        @Override
        public int hashCode() {
            int result = band.hashCode();
            result = 31 * result + magColumn.hashCode();
            result = 31 * result + errorColumn.hashCode();
            return result;
        }
    }

    // Interface for a small class that extracts a value from a row at the
    // given column and converts it to the expected type.  This is basically
    // a poor man's first class function under the limitations imposed by Java.
    private interface Extractor<T> {
        Option<T> extract(CatalogRow row, int index) throws CatalogException;
    }

    private static final Extractor<Angle> RA_EXTRACTOR = CatalogRow::getRa;

    private static final Extractor<Angle> DEC_EXTRACTOR = CatalogRow::getDec;

    private static final Extractor<Angle> DEGREES_EXTRACTOR = CatalogRow::getDegrees;

    private static final Extractor<Double> DOUBLE_EXTRACTOR = CatalogRow::getDouble;

    private static final Extractor<Integer> INTEGER_EXTRACTOR = CatalogRow::getInteger;

    private static final Extractor<String> STRING_EXTRACTOR = CatalogRow::getString;

    private final CatalogHeader header;
    private final CatalogRow row;

    /**
     * Constructs the value extractor with the catalog header and the row from
     * which values will be extracted.  The header is utilized in mapping from
     * column names to indicies and the row contains the values themselves.
     */
    public CatalogValueExtractor(CatalogHeader header, CatalogRow row) {
        this.header = header;
        this.row    = row;
    }

    // Extracts a value from the row associated with the given name, throwing
    // an exception if the name is unrecognized.  Wraps the value in Some if
    // it exists, otherwise returns None
    private <T> Option<T> extractOptional(String colName, Extractor<T> ext) throws CatalogException {
        Option<Integer> colOpt = header.columnIndex(colName);
        if (colOpt.isEmpty()) {
            throw new CatalogException("Missing '" + colName + "'");
        }
        return ext.extract(row, colOpt.getValue());
    }

    // Extracts a value from the row associated with the given name, throwing
    // an exception if the name is unrecognized or the value is not specified.
    private <T> T extract(String colName, Extractor<T> ext) throws CatalogException {
        Option<T> valOpt = extractOptional(colName, ext);
        if (valOpt.isEmpty()) {
            throw new CatalogException("Missing value for '" + colName + "'");
        }

        return valOpt.getValue();
    }

    /**
     * Gets the value associated with the given column name as an Angle
     * representing right ascension. This method can interpret Strings in
     * HH:MM:SS format, Strings containing floating point values, Numbers, and
     * Angles.
     *
     * @param colName name of the column containing the RA
     *
     * @return Angle representing the right ascension
     *
     * @throws CatalogException if there is no such column or if the value is
     * not set
     */
    public Angle getRa(String colName) throws CatalogException {
        return extract(colName, RA_EXTRACTOR);
    }

    /**
     * Gets the value associated with the given column name as an Angle
     * representing declination.  This method can interpret Strings in
     * DD:MM:SS format, Strings containing floating point values, Numbers, and
     * Angles.
     *
     * @param colName name of the column containing the declination
     *
     * @return Angle representing the declination
     *
     * @throws CatalogException if there is no such column or if the value is
     * not set
     */
    public Angle getDec(String colName) throws CatalogException {
        return extract(colName, DEC_EXTRACTOR);
    }

    /**
     * Gets the value associated with the given column name as an Angle in
     * degrees.  This method can interpret Strings in containing floating
     * point values (which are assumed to be expressed in degrees), Numbers,
     * and Angles.
     *
     * @param colName name of the column containing the angle
     *
     * @return Angle corresponding to the value interpreted as degrees
     *
     * @throws CatalogException if there is no such column or if the value is
     * not set
     */
    public Angle getDegrees(String colName) throws CatalogException {
        return extract(colName, DEGREES_EXTRACTOR);
    }

    /**
     * Works as {@link #getDegrees(String)} but does not throw an exception
     * if the value is not set.
     *
     * @param colName name of the column potentially containing the angle
     *
     * @return a {@link Some} wrapping an Angle corresponding to the value
     * interpreted as degrees, if any; {@link None} otherwise
     *
     * @throws CatalogException if there is no such column
     */
    public Option<Angle> getOptionalDegrees(String colName) throws CatalogException {
        return extractOptional(colName, DEGREES_EXTRACTOR);
    }

    /**
     * Gets the value associated with the given column name as a String.  Uses
     * the "Object.toString()" method to convert types to a String.
     *
     * @param colName name of the column containing the string
     *
     * @return String corresponding to the value
     *
     * @throws CatalogException if there is no such column or if the value is
     * not set
     */
    public String getString(String colName) throws CatalogException {
        return extract(colName, STRING_EXTRACTOR);
    }

    /**
     * Works as {@link #getString(String)} but does not throw an exception
     * if the value is not set
     *
     * @param colName name of the column potentially containing the angle
     *
     * @return a {@link Some} wrapping the String, if any; {@link None}
     * otherwise
     *
     * @throws CatalogException if there is no such column
     */
    public Option<String> getOptionalString(String colName) throws CatalogException {
        return extractOptional(colName, STRING_EXTRACTOR);
    }

    /**
     * Gets the value associated with the given column name as a Double.
     * This method can interpret Strings in containing floating
     * point values and Numbers.
     *
     * @param colName name of the column containing the floating point value
     *
     * @return Double corresponding to the value
     *
     * @throws CatalogException if there is no such column or if the value is
     * not set
     */
    public Double getDouble(String colName) throws CatalogException {
        return extract(colName, DOUBLE_EXTRACTOR);
    }

    /**
     * Works as {@link #getDouble(String)} but does not throw an exception
     * if the value is not set
     *
     * @param colName name of the column potentially containing the floating
     * point value
     *
     * @return a {@link Some} wrapping the Double, if any; {@link None}
     * otherwise
     *
     * @throws CatalogException if there is no such column
     */
    public Option<Double> getOptionalDouble(String colName) throws CatalogException {
        return extractOptional(colName, DOUBLE_EXTRACTOR);
    }

    public Integer getInteger(String colName) throws CatalogException {
        return extract(colName, INTEGER_EXTRACTOR);
    }

    public Option<Integer> getOptionalInteger(String colName) throws CatalogException {
        return extractOptional(colName, INTEGER_EXTRACTOR);
    }

    private final class MagExtractOp implements MapOp<MagnitudeDescriptor, Option<Magnitude>> {
        private CatalogException ex;

        @Override public Option<Magnitude> apply(MagnitudeDescriptor desc) {
            try {
                return getOptionalMagnitude(desc);
            } catch (CatalogException ex) {
                if (this.ex == null) this.ex = ex;
                return None.instance();
            }
        }
    }

    private static final PredicateOp<Option<Magnitude>> FILTER_OUT_EMPTY = opt -> !opt.isEmpty();

    private static final MapOp<Option<Magnitude>, Magnitude> EXTRACT_MAG = Option::getValue;

    public ImList<Magnitude> getMagnitudes(ImList<MagnitudeDescriptor> descCollection) throws CatalogException {

        // Sadly, this is awkward because of the CatalogException
        MagExtractOp op = new MagExtractOp();
        ImList<Option<Magnitude>> tmp = descCollection.map(op);
        if (op.ex != null) throw op.ex;

        return tmp.filter(FILTER_OUT_EMPTY).map(EXTRACT_MAG);
    }

    public Option<Magnitude> getOptionalMagnitude(MagnitudeDescriptor desc) throws CatalogException {
        return getOptionalMagnitude(desc.getBand(), desc.getMagnitudeColumn(), desc.getErrorColumn());
    }

    public Option<Magnitude> getOptionalMagnitude(Magnitude.Band band, String colName) throws CatalogException {
        return getOptionalMagnitude(band, colName, None.STRING);
    }

    public Option<Magnitude> getOptionalMagnitude(Magnitude.Band band, String colName, String errorColName) throws CatalogException {
        return getOptionalMagnitude(band, colName, new Some<>(errorColName));
    }

    private Option<Magnitude> getOptionalMagnitude(Magnitude.Band band, String colName, Option<String> errorColName) throws CatalogException {
        Option<Double> mag = getOptionalDouble(colName);
        if (mag.isEmpty()) return None.instance();

        Option<Double> error = None.instance();
        if (!errorColName.isEmpty()) {
            error = getOptionalDouble(errorColName.getValue());
        }

        return new Some<>(new Magnitude(band, mag.getValue(), error, band.defaultSystem));
    }
}
