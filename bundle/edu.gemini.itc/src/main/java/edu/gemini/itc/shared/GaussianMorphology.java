package edu.gemini.itc.shared;

/**
 * The GaussianMorphology concreate class implements the operations that all
 * that are defined in it's abstract parent class Morphology2d.
 */
public class GaussianMorphology extends Morphology3D {

    private double sigma;

    /**
     * Constructor for the Gaussian Morphology
     *
     * @param sourceSize Allows the source size in arcsec to be set at constuction
     */

    public GaussianMorphology(double sourceSize) {
        sigma = sourceSize / 2.355;
    }

    /**
     * We should provide methods that allow the calculation of
     * integrals for square, circular.
     */

    //////////SQUARE//////////////

    // Values of a 2-D gaussian integral over a square region.
    private double[] _2D_SQUARE_INTEGRAL =
            {0, 0.031, 0.118, 0.245, 0.393, 0.542, 0.675, 0.784, 0.865, 0.920, 0.956};

    // This is a table of the integral of a 2-D gaussian over a square region.
    // X is the length of the square as a fraction of sigma.
    // Y is the value of the double integral.
    private DefaultSampledSpectrum _2d_square_integralTable = new
            DefaultSampledSpectrum(_2D_SQUARE_INTEGRAL, 0, 0.5);

    /**
     * This method returns the value of a 2-D circularly-symmetric gaussian
     * over a square region centered about the mean.
     *
     * @param sigmaFraction The length of the square as a fraction of sigma.
     */
    public double get2DSquareIntegral(double sigmaFraction) {
        // Use linear interpolation on the table of pre-calculated results.
        return _2d_square_integralTable.getY(sigmaFraction);
    }

    /**
     * This Method returns the value of a 2-D circularly-symmetric gaussian
     * over a square region that is arbitrarily placed on the gaussian.
     *
     * @param xMin The coordinate of the minimum X position.
     * @param xMax The coordinate of the maximum X position.
     * @param yMin The coordinate of the minimum Y position.
     * @param yMax The coordinate of the maximum Y position.
     */

    public double get2DSquareIntegral(double xMin, double xMax, double yMin, double yMax) {
        double sgConst = Math.sqrt(2) / (2 * sigma);
        try {
            ErrorFunction erf = new ErrorFunction();

            return .25 * erf.getERF(yMax * sgConst) * erf.getERF(xMax * sgConst) -
                    .25 * erf.getERF(yMax * sgConst) * erf.getERF(xMin * sgConst) -
                    .25 * erf.getERF(yMin * sgConst) * erf.getERF(xMax * sgConst) +
                    .25 * erf.getERF(yMin * sgConst) * erf.getERF(xMin * sgConst);
        } catch (Exception e) {
            System.out.println("Could not Create Error Function;");
            return 0;
        }
    }


    //////////END SQUARE//////////////////


    //////////BEGIN CIRCULAR/////////////

    /**
     * Use ErrorFunction to calculate the value of a 2-D circularly-symmetric gaussian
     * over a circular region that is centered on the mean of the gaussian.
     *
     * @param radius The radius of the aperture in arcsec.
     *               <p/>
     *               Use the following equations.
     *               use aperture to find sigmaFraction
     *               then use following cumulitive distribution function  def f(x,u,sigma): return .5*(1+pygsl.sf.erf((x-u)/(sigma*math.sqrt(2)))[0])
     *               in the form f(sigmaFraction,0,1) - f(-sigmaFraction,0,1)
     *               to calculate the fraction of light in the aperture.
     */


    /////////END CIRCULAR///////////////
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    public double getSigma() {
        return this.sigma;
    }

    public void accept(MorphologyVisitor v) {
        v.visitGaussian(this);
    }


}
