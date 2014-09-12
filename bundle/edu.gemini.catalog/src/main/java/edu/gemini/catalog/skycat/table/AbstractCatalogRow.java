//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import edu.gemini.skycalc.DDMMSS;
import edu.gemini.skycalc.HHMMSS;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import java.text.ParseException;
import java.util.logging.Logger;

/**
 * An abstract implementation of {@link CatalogRow} which implements methods
 * to convert the values to expected types based upon the {@link #get} method,
 * which is left abstract.  To make a concrete CatalogRow, extend this class
 * and implement the {@link #get} method.
 */
public abstract class AbstractCatalogRow implements CatalogRow {
    private static final Logger LOG = Logger.getLogger(AbstractCatalogRow.class.getName());

    // Would like to pass around parse methods but since Java does not have
    // first class methods, this class is used instead.  Static implementations
    // for each of the types are provided below.
    private interface Parser<C> {
        Option<C> parse(Object val) throws CatalogException;
    }

    private static String message(Object val, String type) {
        return "Could not convert '" + val + "' to " + type + ".";
    }

    private static Parser<Double> DOUBLE_PARSER = new Parser<Double>() {
        @Override
        public Option<Double> parse(Object val) throws CatalogException {
            if (val instanceof Number) {
                return new Some<Double>(((Number) val).doubleValue());
            }
            if (val instanceof String) {
                String sval = ((String) val).trim();
                if ("".equals(sval) || "null".equals(sval)) return None.instance();
                try {
                    return new Some<Double>(Double.parseDouble((String) val));
                } catch (NumberFormatException ex) {
                    throw new CatalogException(message(sval, "double"), ex);
                }
            }
            throw new CatalogException(message(val, "double"));
        }
    };

    private static Parser<Integer> INTEGER_PARSER = new Parser<Integer>() {
        @Override
        public Option<Integer> parse(Object val) throws CatalogException {
            if (val instanceof Number) {
                return new Some<Integer>(((Number) val).intValue());
            }
            if (val instanceof String) {
                String sval = ((String) val).trim();
                if ("".equals(sval)) return None.instance();
                try {
                    return new Some<Integer>(Integer.parseInt((String) val));
                } catch (NumberFormatException ex) {
                    throw new CatalogException(message(sval, "integer"), ex);
                }
            }
            throw new CatalogException(message(val, "integer"));
        }
    };

    private static Parser<String> STRING_PARSER = new Parser<String>() {
        @Override
        public Option<String> parse(Object val) { return new Some<String>(val.toString()); }
    };


    private static Parser<Angle> DEGREES_PARSER = new Parser<Angle>() {
        @Override
        public Option<Angle> parse(Object val) throws CatalogException {
            if (val instanceof Angle) return new Some<Angle>((Angle) val);
            Option<Double> d = DOUBLE_PARSER.parse(val);
            if (!d.isEmpty()) return new Some<Angle>(new Angle(d.getValue(), DEGREES));
            return None.instance();
        }
    };

    private static Parser<Angle> RA_PARSER = new Parser<Angle>() {
        @Override
        public Option<Angle> parse(Object val) throws CatalogException {
            if (val instanceof String) {
                try {
                    return new Some<Angle>(HHMMSS.parse((String)val));
                } catch (ParseException ex) {
                    LOG.warning("Could not parse '" + val + "' as an RA.");
                }
            }
            return DEGREES_PARSER.parse(val);
        }
    };

    private static Parser<Angle> DEC_PARSER = new Parser<Angle>() {
        @Override
        public Option<Angle> parse(Object val) throws CatalogException {
            if (val instanceof String) {
                try {
                    return new Some<Angle>(DDMMSS.parse((String)val));
                } catch (ParseException ex) {
                    LOG.warning("Could not parse '" + val + "' as a dec.");
                }
            }
            return DEGREES_PARSER.parse(val);
        }
    };


    private <C> Option<C> parse(int columnIndex, Parser<C> parser, String typeStr) throws CatalogException {
        Option<Object> obj = get(columnIndex);
        if (obj.isEmpty()) return None.instance();
        Object val = obj.getValue();
        return parser.parse(val);
    }

    @Override
    public Option<Angle> getRa(int columnIndex) throws CatalogException {
        return parse(columnIndex, RA_PARSER, "RA");
    }

    @Override
    public Option<Angle> getDec(int columnIndex) throws CatalogException {
        return parse(columnIndex, DEC_PARSER, "dec");
    }

    @Override
    public Option<Angle> getDegrees(int columnIndex) throws CatalogException {
        return parse(columnIndex, DEGREES_PARSER, "degrees");
    }

    @Override
    public Option<Double> getDouble(int columnIndex) throws CatalogException {
        return parse(columnIndex, DOUBLE_PARSER, "double");
    }

    @Override
    public Option<Integer> getInteger(int columnIndex) throws CatalogException {
        return parse(columnIndex, INTEGER_PARSER, "integer");
    }

    @Override
    public Option<String> getString(int columnIndex) throws CatalogException{
        return parse(columnIndex, STRING_PARSER, "string");
    }
}
