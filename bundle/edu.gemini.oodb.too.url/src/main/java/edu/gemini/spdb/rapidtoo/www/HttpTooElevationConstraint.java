//
// $
//

package edu.gemini.spdb.rapidtoo.www;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ElevationConstraintType;
import edu.gemini.spdb.rapidtoo.TooElevationConstraint;

import javax.servlet.http.HttpServletRequest;

/**
 * A {@link edu.gemini.spdb.rapidtoo.TooElevationConstraint} implementation
 * based upon HTTP parameters.
 *
 * <ul>
 * <li>elevationType - type of elevation constraint (none, hourAngle, airmass)</li>
 * <li>elevationMin  - minimum elevation value (hour angle or airmass)</li>
 * <li>elevationMax  - maximum elevation value (hour angle or airmass</li>
 * </ul>
 */
public final class HttpTooElevationConstraint implements TooElevationConstraint {
    public static final String TYPE_PARAM = "elevationType";
    public static final String MIN_PARAM  = "elevationMin";
    public static final String MAX_PARAM  = "elevationMax";

    private static final HttpTooElevationConstraint NONE =new HttpTooElevationConstraint(ElevationConstraintType.NONE, 0, 0);

    public static HttpTooElevationConstraint parse(HttpServletRequest req) throws BadRequestException {
        String typeStr = req.getParameter(TYPE_PARAM);
        String minStr  = req.getParameter(MIN_PARAM);
        String maxStr  = req.getParameter(MAX_PARAM);

        if ((typeStr == null) && (minStr == null) && (maxStr == null)) {
            return NONE;
        } else if (ElevationConstraintType.NONE.displayValue().equalsIgnoreCase(typeStr)) {
            return NONE;
        } else if ((typeStr == null) || (minStr == null) || (maxStr == null)) {
            throw new BadRequestException("all elevation constraint parameters must be specified if one is specified");
        }

        // ready to parse what we were given
        try {
            ElevationConstraintType type;
            if ("hourAngle".equalsIgnoreCase(typeStr)) {
                type = ElevationConstraintType.HOUR_ANGLE;
            } else if ("airmass".equalsIgnoreCase(typeStr)) {
                type = ElevationConstraintType.AIRMASS;
            } else {
                throw new BadRequestException("unrecognized elevation constraint type: " + typeStr);
            }

            double min = Double.parseDouble(minStr);
            double max = Double.parseDouble(maxStr);

            return new HttpTooElevationConstraint(type, min, max);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("elevation constraint min and max must be floating point values");
        }
    }


    private final ElevationConstraintType type;
    private final double min;
    private final double max;


    public HttpTooElevationConstraint(ElevationConstraintType type, double min, double max) {
        this.type = type;
        this.min  = min;
        this.max  = max;
    }

    public SPSiteQuality.ElevationConstraintType getType() {
        return type;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}