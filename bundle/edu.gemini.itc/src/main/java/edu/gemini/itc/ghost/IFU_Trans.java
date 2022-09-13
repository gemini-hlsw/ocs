package edu.gemini.itc.ghost;

import edu.gemini.itc.base.SampledSpectrum;
import edu.gemini.itc.base.SampledSpectrumVisitor;
import edu.gemini.spModel.gemini.ghost.GhostType;
import org.jfree.util.Log;
import java.lang.Math;

public class IFU_Trans implements SampledSpectrumVisitor {

    private final double POLY_SR[] = {-0.601093757388945,   2.491686724776579,  -3.321623316076719, 0.954991358859481, 0.926565900249881};
    private final double POLY_SR_bad[] = {0.017680338582827,  -0.192379590733923,   0.817695279572324, -1.662347054268447,  1.461657092371379};
    private final double POLY_HR[] = {0.285444811536887,  -1.867077232744627,   4.552284297394247, -4.811391795562376, 1.405374986540268, 0.883408649616416};
    private final double POLY_HR_bad[] = {0.011858109451155,  -0.138739933878572,   0.635235598240202, -1.389283926814856, 1.306482356152507};
    private final double SR_IFU_tx = 0.93;  // SR IFU lenslet throughput factor (Ross, GVR-5124.4)
    private final double HR_IFU_tx = 0.85;  // Equivalent for HR

    private GhostType.Resolution _res;

    private double _fwhm;

    public IFU_Trans(GhostType.Resolution  res) {
        _res = res;
        _fwhm = 0;
    }

    public void setFWHM(double fwhm) {
        _fwhm = fwhm;
    }


    private double polyVal(double poly[], double p) {
        int n = poly.length;
        if (n == 0) return 0;
        double result = poly[0];
        for (int i = 1; i < n; i++)
            result = result*p + poly[i];
        return result;
    }



    public double getInjectionLoss(){
        switch (_res) {
            case STANDARD: {
                if (_fwhm < 0.28) {
                    double alpha = 0.701 * _fwhm + 5 * Math.ulp(1);  //% for best seeing use integration in equiv circle
                    return (1 - Math.pow(1 + Math.pow((0.333 / alpha), 2), -3)) * SR_IFU_tx;
                }

                if (_fwhm < 1.55)
                    return polyVal(POLY_SR, _fwhm) * SR_IFU_tx;

                if (_fwhm < 3)
                    return polyVal(POLY_SR_bad, _fwhm) * SR_IFU_tx;

                double alpha = 0.701 * _fwhm;     // for really bad seeing use integration in equiv circle
                return (1 - Math.pow(1 + Math.pow((0.333 / alpha), 2), -3)) * SR_IFU_tx;
            }
            case HIGH:
                if (_fwhm < 0.28) {
                    double alpha = 0.701 * _fwhm + 5 * Math.ulp(1);
                    return (1 - Math.pow(1 + Math.pow((0.333/alpha),2),-3)) * HR_IFU_tx;
                }

                if (_fwhm <= 1.55)
                   return polyVal(POLY_HR,_fwhm) * HR_IFU_tx;

                if (_fwhm < 3)
                    return polyVal(POLY_HR_bad,_fwhm) * HR_IFU_tx;

                double alpha = 0.701 * _fwhm;
                return (1 - Math.pow(1 + Math.pow((0.333/alpha),2),-3)) * HR_IFU_tx ;

            default:
                Log.error("Wrong resolution param provided to get the IFU throughput");
                return 0;
        }
    }

    @Override
    public void visit(SampledSpectrum sed) {
        final double loss = getInjectionLoss();
        for (int i = 0; i < sed.getLength(); i++) {
            sed.setY(i, sed.getY(i) * loss);
        }
    }
}
