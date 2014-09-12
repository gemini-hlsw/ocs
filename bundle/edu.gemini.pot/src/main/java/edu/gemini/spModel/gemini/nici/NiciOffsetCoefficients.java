//
// $
//

package edu.gemini.spModel.gemini.nici;

/**
 * Coefficients used for calculating a p,q offset given a distance along the
 * arc (in arcseconds) tracked by the focal plane mask.  See SCT-232.
 */
public final class NiciOffsetCoefficients {

    static final double DEGREES_PER_RADIAN = 180.0 / Math.PI;

    public static final NiciOffsetCoefficients DEFAULT = new NiciOffsetCoefficients(
            65.7442, // p1
            66.4827, // q1
            93.5,    // r1
            45.32,   // theta1
            1.0      // s1
    );

    /**
     * Lookup the NiciOffsetCoefficients that should be used with the given
     * focal plane mask.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static NiciOffsetCoefficients lookup(NICIParams.FocalPlaneMask fpm) {
        return DEFAULT;
    }

    private final double p1;
    private final double q1;
    private final double r1;
    private final double theta1;
    private final double s1;

    private final double pCenter;
    private final double qCenter;

    public NiciOffsetCoefficients(double p1, double q1, double r1, double theta1, double s1) {
        this.p1 = p1;
        this.q1 = q1;
        this.r1 = r1;
        this.theta1 = theta1;
        this.s1 = s1;

        // compute center of the circle
        double p0 = p1 - r1*Math.cos((theta1)/DEGREES_PER_RADIAN);
        double q0 = q1 - r1*Math.sin((theta1)/DEGREES_PER_RADIAN);
        double p180 = p1 - r1*Math.cos((theta1 + 180.0*s1)/DEGREES_PER_RADIAN);
        double q180 = q1 - r1*Math.sin((theta1 + 180.0*s1)/DEGREES_PER_RADIAN);
        pCenter = (p180 - p0)/2;
        qCenter = (q180 - q0)/2;
    }

    public double getP1() {
        return p1;
    }

    public double getQ1() {
        return q1;
    }

    public double getR1() {
        return r1;
    }

    public double getTheta1() {
        return theta1;
    }

    public double getS1() {
        return s1;
    }

    public double computeP(double d) {
        double thetaDegrees = DEGREES_PER_RADIAN * d / r1;
        double res = p1 - r1*Math.cos((theta1 + thetaDegrees*s1)/DEGREES_PER_RADIAN);
        return round(res);
    }

    public double computeQ(double d) {
        double thetaDegrees = DEGREES_PER_RADIAN * d / r1;
        double res = q1 - r1*Math.sin((theta1 + thetaDegrees*s1)/DEGREES_PER_RADIAN);
        return round(res);
    }

    public double computeD(double p, double q) {
        // Get the p and q relative to the base of the circle.
        q = q - qCenter;
        p = p - pCenter;

        // Compute the distance from the point to the center.
        double h = Math.sqrt(q*q + p*p);

        // Figure out the angle that the line connecting the point to
        // the center makes.
        double thetaR = Math.asin(q/h);
        double thetaD = Math.toDegrees(thetaR);

        if (q >= 0) {
            if (p > 0) {
                thetaD = 180 - thetaD;
            }
        } else {
            thetaD *= -1;
            if (p >= 0) {
                thetaD += 180;
            } else {
                thetaD = 360 - thetaD;
            }
        }
        thetaD = 360 - thetaD - theta1;
        return r1 * thetaD/DEGREES_PER_RADIAN;
    }

    static double round(double res) {
        // Throw away the extra precision.
        res = res * 10000;
        long tmp = Math.round(res);
        return tmp / 10000.0;
    }
}
