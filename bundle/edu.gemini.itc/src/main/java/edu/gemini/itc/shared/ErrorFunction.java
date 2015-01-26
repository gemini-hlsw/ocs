package edu.gemini.itc.shared;

/**
 * ErrorFunction
 * This class exists so that the client can calculate a value
 * from the Error function given a z.
 */
public class ErrorFunction {

    private static final String DEFAULT_ERROR_FILENAME = ITCConstants.CALC_LIB + "/"
            + "Error_Function" + ITCConstants.DATA_SUFFIX;
    private static final double MAX_Z_VALUE = 2.9;
    private static final double MIN_Z_VALUE = -3.0;

    private final DefaultArraySpectrum errorFunction;

    public ErrorFunction() throws Exception {
        this(ErrorFunction.DEFAULT_ERROR_FILENAME);
    }

    public ErrorFunction(String file) throws Exception {
        errorFunction = new DefaultArraySpectrum(file);
    }

    public double getERF(double z) {
        if (z > ErrorFunction.MAX_Z_VALUE)
            return 1;
        else if (z <= ErrorFunction.MIN_Z_VALUE)
            return -1;
        else
            return errorFunction.getY(z);
    }


    public String toString() {
        final StringBuilder sb = new StringBuilder("ErrorFuntion:\n");
        final double[][] data = errorFunction.getData();
        for (int i = 0; i < data[0].length; i++) {
            sb.append(data[0][i]);
            sb.append("\t");
            sb.append(data[1][i]);
            sb.append("\n");
        }
        return sb.toString();
    }


}
