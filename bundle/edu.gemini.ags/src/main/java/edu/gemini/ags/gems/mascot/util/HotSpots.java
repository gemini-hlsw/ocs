package edu.gemini.ags.gems.mascot.util;

import com.github.fommil.netlib.LAPACK;
import org.netlib.util.intW;

import java.util.Arrays;

/**
 * Java versions of some Scala functions that were determined to be performance bottlenecks.
 */
public class HotSpots {


    /**
     * spline(y,x) form.
     */
    public static double[] spline(double[] y, double[] x) {
        double[] dx = dif(x);
        double[] dy = dif(y);
        double[] diag = pcen(divide(2.0, dx));
        if (x.length > 2) multiplyEq(diag, 1, diag.length - 1, 2.);
        double[] rhs = pcen(divide(multiply(dy, 3.), multiply(dx, dx)));
        if (x.length > 2) multiplyEq(rhs, 1, rhs.length - 1, 2.);
        double[] dx2 = divide(1.0, dx);

        // Note: Since the Mascot code doesn't use the 2 optional parameters, the code dealing
        // with those has been left out here.
        return tdSolve(dx2, diag, dx2, rhs); /* simple natural spline */
    }


    /**
     * spline(dydx,y,x,xp) form.
     */
    public static double[] spline(double[] pdydx, double[] py, double[] px, double[] xp) {
        int[] l = digitize(xp, px); /* index of lower boundary of interval containing xp */
        int[] u = add(l, 1);

        /* extend x, y, dydx so that l and u can be used as index lists */
        double dx = px[px.length - 1] - px[0];
        double[] x = grow(px[0] - dx, px, px[px.length - 1] + dx);
        double[] y = grow(py[0] - pdydx[0] * dx, py, py[py.length - 1] + pdydx[pdydx.length - 1] * dx);
        double[] dydx = grow(pdydx[0], pdydx, pdydx[pdydx.length - 1]);

        double[] xl = xget(x, l);
        double[] dx2 = subtract(xget(x, u), xl);
        double[] yl = xget(y, l);
        double[] dy = subtract(xget(y, u), yl);
        double[] dl = xget(dydx, l);
        double[] du = xget(dydx, u);
        double[] dydx2 = divide(dy, dx2);
        return poly(subtract(xp, xl),
                new double[][]{
                        yl,
                        dl,
                        divide(subtract(subtract(multiply(dydx2, 3.0), du), multiply(dl, 2.0)), dx2),
                        divide(subtract(add(du, dl), multiply(dydx2, 2.0)), multiply(dx2, dx2))});
    }

    /**
     * Returns the max value of the spline
     */
    public static double[] splineMax(double[] pdydx, double[] py, double[] px, double[] xp, double maxValue) {
        return max(spline(pdydx, py, px, xp), maxValue);
    }


    /**
     * Returns the concatenation of the arguments
     */
    public static double[] grow(double d1, double[] a, double d2) {
        double[] result = new double[a.length + 2];
        result[0] = d1;
        System.arraycopy(a, 0, result, 1, a.length);
        result[result.length - 1] = d2;
        return result;
    }

    /**
     * Returns v(a) in scala, where a is an array of indexes into the vector v.
     *
     * @param v a vector
     * @param a an array of indexes
     * @return a new vector
     */
    public static double[] xget(double[] v, int[] a) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) result[i] = v[a[i]];
        return result;
    }

    /**
     * Returns x+y
     */
    public static double[] add(double[] x, double[] y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] + y[i];
        return result;
    }

    /**
     * Returns x+y
     */
    public static int[] add(int[] x, int y) {
        int[] result = new int[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] + y;
        return result;
    }

    /**
     * Implements x += y
     */
    public static void addEq(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) x[i] += y[i];
    }

    /**
     * Returns x-y
     */
    public static double[] subtract(double[] x, double[] y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] - y[i];
        return result;
    }

    /**
     * Returns x*y
     */
    public static double[] multiply(double[] x, double[] y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] * y[i];
        return result;
    }

    /**
     * Returns x*y
     */
    public static double[] multiply(double[] x, double y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] * y;
        return result;
    }

    /**
     * implements x *= y
     */
    public static void multiplyEq(double[] x, int start, int end, double y) {
        for (int i = start; i < end; i++) x[i] *= y;
    }

    /**
     * Returns x/y
     */
    public static double[] divide(double[] x, double[] y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] / y[i];
        return result;
    }

    /**
     * Returns x/y
     */
    public static double[] divide(double[] x, double y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i] / y;
        return result;
    }

    /**
     * Returns x/y
     */
    public static double[] divide(double x, double[] y) {
        double[] result = new double[y.length];
        for (int i = 0; i < y.length; i++) result[i] = x / y[i];
        return result;
    }

    /**
     * Returns max(x, y)
     */
    public static double[] max(double[] x, double y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = Math.max(x[i], y);
        return result;
    }

    /**
     * Returns x^y
     */
    public static double[] pow(double[] x, double y) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = Math.pow(x[i], y);
        return result;
    }

    /**
     * From the C routine: poly(x, a0, a1, a2, ..., aN)
     * returns the polynomial  A0 + A1*x + A2*x^2 + ... + AN*X^N
     * The data type and dimensions of the result, and conformability rules
     * for the inputs are identical to those for the expression.
     */
    public static double[] poly(double[] x, double[][] a) {
        double[] result = new double[x.length];
        for (int i = 0; i < a.length; i++) addEq(result, multiply(a[i], pow(x, i)));
        return result;
    }


    /**
     * From C routine: digitize(x, bins):
     * <p/>
     * Returns an array of longs with dimsof(X), and values i such that
     * BINS(i-1) <= X < BINS(i) if BINS is monotonically increasing, or
     * BINS(i-1) > X >= BINS(i) if BINS is monotonically decreasing.
     * Beyond the bounds of BINS, returns either i=1 or i=numberof(BINS)+1
     * as appropriate.
     * <p/>
     * SEE ALSO: histogram, interp, integ, sort, where, where2
     * <p/>
     * Example:
     * <p><pre>
     * xp=[3.5e-07,4.33333e-07,5.16667e-07,6e-07,6.83333e-07,7.66667e-07,8.5e-07,9.33333e-07,1.01667e-06,1.1e-06]
     * x=[-4e-07,3.5e-07,4e-07,5e-07,6e-07,7e-07,8e-07,9e-07,1e-06,1.1e-06,1.85e-06]
     * digitize(xp,x)
     * [3,4,5,6,6,7,8,9,10,11]
     * </pre>
     */
    public static int[] digitize(double[] x, double[] bins) {
        int[] ibin = new int[x.length];
        int ip = 0;
        for (int i = 0; i < x.length; i++) {
            ibin[i] = hunt(bins, bins.length, x[i], ip);
            ip = ibin[i];
        }
        return ibin;
    }

    /**
     * From C routine: hunt(double *x, long n, double xp, long ip):
     * <p/>
     * Based on the hunt routine given in Numerical Recipes (Press, et al.,
     * Cambridge University Press, 1988), section 3.4.
     * <p/>
     * Here, x[n] is a monotone array and, if xp lies in the interval
     * from x[0] to x[n-1], then<br>
     * x[h-1] <= xp < x[h]  (h is the value returned by hunt), or<br>
     * x[h-1] >= xp > x[h], as x is ascending or descending<br>
     * The value 0 or n will be returned if xp lies outside the interval.
     */
    public static int hunt(double[] x, int n, double xp, int ip) {
        boolean ascend = x[n - 1] > x[0];
        int jl, ju;

        if (ip < 1 || ip > n - 1) {
            /* caller has declined to make an initial guess, so fall back to garden variety bisection method */
            if ((xp >= x[n - 1]) == ascend) return n;
            if ((xp < x[0]) == ascend) return 0;
            jl = 0;
            ju = n - 1;
        } else {
            /* search from initial guess ip in ever increasing steps to bracket xp */
            int inc = 1;
            if ((xp >= x[ip]) == ascend) { /* search toward larger index values */
                if (ip == n - 1) return n;
                jl = ip;
                ju = ip + inc;
                while ((xp >= x[ju]) == ascend) {
                    jl = ju;
                    inc += inc;
                    ju += inc;
                    if (ju >= n) {
                        if ((xp >= x[n - 1]) == ascend) return n;
                        ju = n;
                        break;
                    }
                }
            } else {                     /* search toward smaller index values */
                if (ip == 0) return 0;
                ju = ip;
                jl = ip - inc;
                while ((xp < x[jl]) == ascend) {
                    ju = jl;
                    inc += inc;
                    jl -= inc;
                    if (jl < 0) {
                        if ((xp < x[0]) == ascend) return 0;
                        jl = 0;
                        break;
                    }
                }
            }
        }

        /* have x[jl]<=xp<x[ju] for ascend, x[jl]>=xp>x[ju] for !ascend */
        while (ju - jl > 1) {
            ip = (jl + ju) >> 1;
            if ((xp >= x[ip]) == ascend) jl = ip;
            else ju = ip;
        }

        return ju;
    }

    /**
     * Returns an Array containing the differences of the consecutive elements
     * (In Yorick: v(dif)
     */
    public static double[] dif(double[] v) {
        int n = v.length - 1;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = v[i + 1] - v[i];
        }
        return result;
    }

    /**
     * Returns the pairwise average between successive elements. However, the two
     * endpoints are copied unchanged, so that the result dimension has a length one
     * greater than the dimension of the subscripted array. The name is short for
     * “point center”.
     */
    public static double[] pcen(double[] v) {
        double[] result = new double[v.length + 1];
        result[0] = v[0];
        int n = v.length - 1;
        for (int i = 0; i < n; i++) {
            result[i + 1] = (v[i] + v[i + 1]) / 2.;
        }
        result[v.length] = v[n];
        return result;
    }

    /**
     * Ported from Yorick: func TDsolve(c, d, e, b, which=)
     * <p>
     * Note: Here we only deal with vectors. The original Yorick version also dealt with multiple dimensions.
     */
    public static double[] tdSolve(double[] c, double[] d, double[] e, double[] b) {
      int n = b.length;
      double[] bb = Arrays.copyOf(b, b.length); // copy of RHS to be transformed into solution
      double[] cc = Arrays.copyOf(c, c.length); // in/out parameters!
      double[] dd = Arrays.copyOf(d, d.length);
      double[] ee = Arrays.copyOf(e, e.length);

      int nrhs = 1;
      intW info = new intW(0);
      LAPACK.getInstance().dgtsv(n, nrhs, cc, dd, ee, bb, n, info);
      if (info.val != 0) {
        throw new IllegalArgumentException("tridiagonal element became 0.0");
      }
      return bb;
    }

  //func ftcb(te,tcal,tmir,gain,dim,x=)
  ///* DOCUMENT ftcb(te,tcal,tmir,gain,dim,x=)
  //   returns [f,hbo,hcor,hbf]
  //   AUTHOR: F.Rigaut, way back in 1996?
  //   SEE ALSO:
  // */
  public static double[][] ftcb(double te, double tcal, double tmir, double gain, int dim) {
    double[] f = divide(range(1, dim+1), (te * 2.0 * dim));
    Complex[] p = multiply(f, new Complex(0., 2.).times(Math.PI));
    Complex[] pte = multiply(p, te);
    Complex[] mpte = multiply(pte, -1.);
    Complex[] hzoh = divide(add(multiply(exp(mpte), -1.0), 1.0), pte);
    Complex[] hmir = divide(1.0, add(multiply(p, tmir), 1.0));
    Complex[] hwfs = divide(add(multiply(exp(mpte), -1.0), 1.0), pte);
    Complex[] hcal = multiply(exp(multiply(p, -tcal)), gain);

    Complex[] hbo = divide(multiply(multiply(multiply(hzoh, hmir), hwfs), hcal), add(multiply(exp(mpte), -1.0), 1.0));
    Complex[] hbop1 = add(hbo, 1.0);
    double[] hcor = conjMultiply(divide(1.0, hbop1));
    double[] hbf = conjMultiply(divide(hbo, hbop1));
    double[] hbo2 = conjMultiply(hbo);

    return new double[][]{f, hbo2, hcor, hbf};
  }

    /**
     * Returns x*y
     */
    public static Complex[] multiply(double[] x, Complex y) {
        Complex[] result = new Complex[x.length];
        for (int i = 0; i < x.length; i++) result[i] = y.times(x[i]);
        return result;
    }

    /**
     * Returns x*y
     */
    public static Complex[] multiply(Complex[] x, double y) {
        Complex[] result = new Complex[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i].times(y);
        return result;
    }

    /**
     * Returns x*y
     */
    public static Complex[] multiply(Complex[] x, Complex[] y) {
        Complex[] result = new Complex[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i].times(y[i]);
        return result;
    }

    /**
     * Returns x/y
     */
    public static Complex[] divide(Complex[] x, Complex[] y) {
        Complex[] result = new Complex[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i].divides(y[i]);
        return result;
    }

    /**
     * Returns x/y
     */
    public static Complex[] divide(double x, Complex[] y) {
        Complex[] result = new Complex[y.length];
        Complex cx = new Complex(x, 0);
        for (int i = 0; i < y.length; i++) result[i] = cx.divides(y[i]);
        return result;
    }

    /**
     * Returns x+y
     */
    public static Complex[] add(Complex[] x, double y) {
        Complex[] result = new Complex[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i].plus(y);
        return result;
    }

    /**
     * Returns exp(x)
     */
    public static Complex[] exp(Complex[] x) {
        Complex[] result = new Complex[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i].exp();
        return result;
    }

    /**
     * Returns (v * conj(v)) as a vector of doubles (all imaginary values
     * are 0 after the multiply)
     */
    public static double[] conjMultiply(Complex[] x) {
        double[] result = new double[x.length];
        for (int i = 0; i < x.length; i++) result[i] = x[i].times(x[i].conjugate()).re();
        return result;
    }

    /**
     * rReturns an array of values starting with start and ending with end-1
     */
    public static double[] range(int start, int end) {
        double[] result = new double[end-start];
        for(int i = start, j = 0; i < end; i++, j++) {
            result[j] = i;
        }
        return result;
    }
}
