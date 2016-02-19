package edu.gemini.ags.gems.mascot.util

import breeze.linalg._

/**
 * Support routines for the Yorick to Scala port
 */

object YUtils {

  /*
   * Returns a Yorick formatted string for the array
   */
  def yFormat(a: Array[Array[DenseMatrix[Double]]]): String = {
    val sb = new StringBuilder
    sb.append("[")
    for (i <- a.indices) {
      sb.append(yFormat(a(i)))
      if (i != a.length - 1) {
        sb.append(",")
      }
    }
    sb.append("]")
    sb.toString()
  }

  /*
   * Returns a Yorick formatted string for the array
   */
  def yFormat(a: Array[DenseMatrix[Double]]): String = {
    val sb = new StringBuilder
    sb.append("[")
    for (i <- a.indices) {
      sb.append(yFormat(a(i)))
      if (i != a.length - 1) {
        sb.append(",")
      }
    }
    sb.append("]")
    sb.toString()
  }

  /*
   * Returns a Yorick formatted matrix string
   */
  def yFormat(m: DenseMatrix[Double]): String = {
    val sb = new StringBuilder
    sb.append("[")
    for (col <- 0 until m.cols) {
      //      sb.append("[")
      //      for (row <- 0 until m.numRows) {
      //        sb.append(m(row, col).floatValue)
      //        if (row != m.numRows - 1) {
      //          sb.append(",")
      //        }
      //      }
      //      sb.append("]")
      sb.append(yFormat(m(::, col)))
      if (col != m.cols - 1) {
        sb.append(",")
      }
    }
    sb.append("]")
    sb.toString()
  }

  /*
   * Returns a Yorick formatted vector string
   */
  def yFormat(v: DenseVector[Double]): String = {
    val sb = new StringBuilder
    sb.append("[")
    for (i <- 0 until v.size) {
      sb.append(v(i))
      if (i != v.size - 1) {
        sb.append(",")
      }
    }
    sb.append("]")
    sb.toString()
  }

  /*
   * Returns a Yorick formatted array string
   */
  def yFormat(v: Array[Int]): String = {
    val sb = new StringBuilder
    sb.append("[")
    for (i <- v.indices) {
      sb.append(v(i))
      if (i != v.length - 1) {
        sb.append(",")
      }
    }
    sb.append("]")
    sb.toString()
  }


  /**
   * Yorick style multiply of a vector and a matrix.
   * <p>
   * Example:
   * <p>
   * [[1,2,3],[4,5,6]]*[1,2,3] = [[1,4,9],[4,10,18]]
   */
  def yMultiply(v: DenseVector[Double], m: DenseMatrix[Double]): DenseMatrix[Double] = {
    assert(v.size == m.rows)
    val result = DenseMatrix.zeros[Double](m.rows, m.cols)
    for (i <- 0 until m.rows; j <- 0 until m.cols) {
      result(i, j) = m(i, j) * v(i)
    }
    result
  }

  def yMultiply4d(v: DenseVector[Double], a: Array[Array[DenseMatrix[Double]]], n: Int): DenseMatrix[Double] = {
    var m = DenseMatrix.zeros[Double](a(0)(0).rows, a(0)(0).cols)
    for (j <- a.indices) {
      m += a(j)(n) * v(j)
    }
    m
  }


  /**
   * Returns a DenseVector containing the differences of the consecutive elements
   * (In Yorick: v(dif)
   */
  def dif(v: DenseVector[Double]): DenseVector[Double] = {
    //    val a = v.toArray
    //    (a.zip(a.tail).map(a => a._2 - a._1)).asVector
    val result = DenseVector.zeros[Double](v.size - 1)
    for (i <- 0 until v.size - 1) {
      result(i) = v(i + 1) - v(i)
    }
    result
  }


  /**
   * Temporary, until Scalala implements this:
   * Returns d/v (Scalala currently only supports v/d)
   */
  def divide(d: Double, v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(d / _)
  }

  /**
   * Temporary, until Scalala implements this:
   * Returns d/m (Scalala currently only supports m/d)
   */
  def divide(d: Double, m: DenseMatrix[Double]): DenseMatrix[Double] = {
    m.mapValues(d / _)
  }


//  /**
//   * Temporary, until Scalala implements this:
//   * Returns d/v (Scalala currently only supports v/d)
//   */
//  def divideComplex(d: Double, v: DenseVector[Complex]): DenseVector[Complex] = {
//    v.mapValues(d / _)
//  }


  /**
   * Temporary, until Scalala implements this:
   * Returns d:^v (Scalala currently only supports v:^d)
   */
  def pow(d: Double, v: DenseVector[Double]): DenseVector[Double] = {
//    v.mapValues(d :^ _)
    v.mapValues(math.pow(d, _))
  }

  /**
   * Returns a to the power of d for each value in a
   */
  def pow(a: Array[Array[DenseMatrix[Double]]], d: Double): Array[Array[DenseMatrix[Double]]] = {
    val result = Array.ofDim[Array[DenseMatrix[Double]]](a.length)
    for (i <- a.indices) {
      result(i) = Array.ofDim[DenseMatrix[Double]](a(i).length)
      for (j <- a(i).indices) {
        result(i)(j) = a(i)(j) :^ d
      }
    }
    result
  }

  /**
   * Returns math.exp(v) for each value in v
   */
  def exp(v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(math.exp)
  }

//  /**
//   * Returns math.exp(v) for each value in the complex vector v.
//   * Uses Euler's formula, since not supported by math.exp directly.
//   */
//  def expComplex(v: Vector[Complex]): Vector[Complex] = {
//    v.mapValues(a => (math.exp(a.real) * (math.cos(a.imag) + i * math.sin(a.imag))))
//  }
//
//  /**
//   * Returns the conjugate of each value in the complex vector v
//   */
//  def conj(v: Vector[Complex]): Vector[Complex] = {
//    v.mapValues(_.conjugate)
//  }
//
//  /**
//   * Returns (v * conj(v)) as a vector of doubles (all imaginary values
//   * are 0 after the multiply)
//   */
//  def conjMultiply(v: Vector[Complex]): Vector[Double] = {
//    val cv = v :* conj(v)
//    cv.mapValues(_.real)
//  }


  /**
   * Returns the pairwise average between successive elements. However, the two
   * endpoints are copied unchanged, so that the result dimension has a length one
   * greater than the dimension of the subscripted array. The name is short for
   * “point center”.
   */
  def pcen(v: DenseVector[Double]): DenseVector[Double] = {
    //    val l = v.toList
    //    (l.head :: l.zip(l.tail).map(x => (x._1 + x._2) / 2.) ::: List(l.last)).toArray.asVector
    val result = DenseVector.zeros[Double](v.size + 1)
    result(0) = v(0)
    for (i <- 0 until v.size - 1) {
      result(i + 1) = (v(i) + v(i + 1)) / 2.0
    }
    result(v.size) = v(v.size - 1)
    result
  }

  /**
   * Applies math.sinh to each element of the vector and returns the result
   */
  def sinh(v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(math.sinh)
  }

  /**
   * Applies math.cosh to each element of the vector and returns the result
   */
  def cosh(v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(math.cosh)
  }

  /**
   * Applies math.sqrt to each element of the vector and returns the result
   */
  def sqrt(v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(math.sqrt)
  }

  /**
   * Applies math.sqrt to each element of the matrix and returns the result
   */
  def sqrt(m: DenseMatrix[Double]): DenseMatrix[Double] = {
    m.mapValues(math.sqrt)
  }

  /**
   * Returns the vector v with d1 inserted at the start and d2 at the end.
   */
  def grow(d1: Double, v: DenseVector[Double], d2: Double): DenseVector[Double] = {
    val result = DenseVector.zeros[Double](v.size + 2)
    result(0) = d1
    result(1 to v.size) := v
    result(result.size - 1) = d2
    result
  }

  /**
   * Returns the vector v with d appended.
   */
  def grow(v: DenseVector[Double], d: Double): DenseVector[Double] = {
    //    v.toArray.padTo(v.size + 1, d).asVector
    val result = DenseVector.zeros[Double](v.size + 1)
    result(0 until v.size) := v
    result(result.size - 1) = d
    result
  }


  /**
   * From C routine: digitize(x, bins):
   * <p>
   * Returns an array of longs with dimsof(X), and values i such that
   * BINS(i-1) <= X < BINS(i) if BINS is monotonically increasing, or
   * BINS(i-1) > X >= BINS(i) if BINS is monotonically decreasing.
   * Beyond the bounds of BINS, returns either i=1 or i=numberof(BINS)+1
   * as appropriate.
   * <p>
   * SEE ALSO: histogram, interp, integ, sort, where, where2
   * <p>
   * Example:
   * <p><pre>
   * xp=[3.5e-07,4.33333e-07,5.16667e-07,6e-07,6.83333e-07,7.66667e-07,8.5e-07,9.33333e-07,1.01667e-06,1.1e-06]
   * x=[-4e-07,3.5e-07,4e-07,5e-07,6e-07,7e-07,8e-07,9e-07,1e-06,1.1e-06,1.85e-06]
   * digitize(xp,x)
   * [3,4,5,6,6,7,8,9,10,11]
   * </pre>
   */
  def digitize(x: DenseVector[Double], bins: DenseVector[Double]): Array[Int] = {
    //    void Y_digitize(int nArgs)
    //    {
    //      long number, origin, nbins, i, ip;
    //      double *x, *bins;
    //      Dimension *dimsx, *dimsb;
    //      long *ibin;
    //      if (nArgs!=2) YError("digitize takes exactly two arguments");
    //
    //      bins= YGet_D(sp, 0, &dimsb);
    //      x= YGet_D(sp-1, 0, &dimsx);
    //
    //      if (!dimsb || dimsb->number<2 || dimsb->next)
    //        YError("2nd argument to digitize must be 1D with >=2 elements");
    //      nbins= dimsb->number;
    //      origin= dimsb->origin;
    //      number= TotalNumber(dimsx);
    //
    //      if (dimsx) {
    //        Array *array= PushDataBlock(NewArray(&longStruct, dimsx));
    //        ibin= array->value.l;
    //      } else {
    //        PushLongValue(0L);
    //        ibin= &sp->value.l;
    //      }
    //      ip= 0;
    //      for (i=0 ; i<number ; i++)
    //        ibin[i]= ip= origin+hunt(bins, nbins, x[i], ip);
    //    }

    // XXX For now assume only one dimension
    val ibin = Array.ofDim[Int](x.size)
    var ip = 0
    for (i <- 0 until x.size) {
      ibin(i) = hunt(bins, bins.size, x(i), ip)
      ip = ibin(i)
    }
    ibin
  }

  /**
   * From C routine: hunt(double *x, long n, double xp, long ip):
   * <p>
   * Based on the hunt routine given in Numerical Recipes (Press, et al.,
   * Cambridge University Press, 1988), section 3.4.
   * <p>
   * Here, x[n] is a monotone array and, if xp lies in the interval
   * from x[0] to x[n-1], then<br>
   *    x[h-1] <= xp < x[h]  (h is the value returned by hunt), or<br>
   *    x[h-1] >= xp > x[h], as x is ascending or descending<br>
   *  The value 0 or n will be returned if xp lies outside the interval.
   */
  def hunt(x: DenseVector[Double], n: Int, xp: Double, ip: Int): Int = {
    // static long hunt(double *x, long n, double xp, long ip)
    //{
    //  int ascend= x[n-1]>x[0];
    //  long jl, ju;
    //
    //  if (ip<1 || ip>n-1) {
    //    /* caller has declined to make an initial guess, so fall back to
    //       garden variety bisection method */
    //    if ((xp>=x[n-1]) == ascend) return n;
    //    if ((xp<x[0]) == ascend) return 0;
    //    jl= 0;
    //    ju= n-1;
    //
    //  } else {
    //    /* search from initial guess ip in ever increasing steps to bracket xp */
    //    int inc= 1;
    //    jl= ip;
    //    if ((xp>=x[ip]) == ascend) { /* search toward larger index values */
    //      if (ip==n-1) return n;
    //      jl= ip;
    //      ju= ip+inc;
    //      while ((xp>=x[ju]) == ascend) {
    //        jl= ju;
    //        inc+= inc;
    //        ju+= inc;
    //        if (ju>=n) {
    //          if ((xp>=x[n-1]) == ascend) return n;
    //          ju= n;
    //          break;
    //        }
    //      }
    //    } else {                     /* search toward smaller index values */
    //      if (ip==0) return 0;
    //      ju= ip;
    //      jl= ip-inc;
    //      while ((xp<x[jl]) == ascend) {
    //        ju= jl;
    //        inc+= inc;
    //        jl-= inc;
    //        if (jl<0) {
    //          if ((xp<x[0]) == ascend) return 0;
    //          jl= 0;
    //          break;
    //        }
    //      }
    //    }
    //  }
    //
    //  /* have x[jl]<=xp<x[ju] for ascend, x[jl]>=xp>x[ju] for !ascend */
    //  while (ju-jl > 1) {
    //    ip= (jl+ju)>>1;
    //    if ((xp>=x[ip]) == ascend) jl= ip;
    //    else ju= ip;
    //  }
    //
    //  return ju;
    //}

    val ascend = x(n - 1) > x(0)
    var jl = 0
    var ju = 0

    if (ip < 1 || ip > n - 1) {
      // caller has declined to make an initial guess, so fall back to garden variety bisection method
      if ((xp >= x(n - 1)) == ascend) return n
      if ((xp < x(0)) == ascend) return 0
      jl = 0
      ju = n - 1
    } else {
      // search from initial guess ip in ever increasing steps to bracket xp
      var inc = 1
      jl = ip
      var break = false
      if ((xp >= x(ip)) == ascend) {
        // search toward larger index values
        if (ip == n - 1) return n
        jl = ip
        ju = ip + inc
        while ((xp >= x(ju)) == ascend && !break) {
          jl = ju
          inc += inc
          ju += inc
          if (ju >= n) {
            if ((xp >= x(n - 1)) == ascend) return n
            ju = n
            break = true
          }
        }
      } else {
        // search toward smaller index values
        if (ip == 0) return 0
        ju = ip
        jl = ip - inc
        while ((xp < x(jl)) == ascend && !break) {
          ju = jl
          inc += inc
          jl -= inc
          if (jl < 0) {
            if ((xp < x(0)) == ascend) return 0
            jl = 0
            break = true
          }
        }
      }
    }

    // have x(jl)<=xp<x(ju) for ascend, x(jl)>=xp>x(ju) for !ascend
    while (ju - jl > 1) {
      val ip2 = (jl + ju) >> 1
      if ((xp >= x(ip2)) == ascend) jl = ip2
      else ju = ip2
    }

    ju
  }

  /**
   * From the C routine: poly(x, a0, a1, a2, ..., aN)
   * returns the polynomial  A0 + A1*x + A2*x^2 + ... + AN*X^N
   * The data type and dimensions of the result, and conformability rules
   * for the inputs are identical to those for the expression.
   */
  def poly(x: DenseVector[Double], a: DenseVector[Double]*): DenseVector[Double] = {
    var result = DenseVector.zeros[Double](x.size)
    for (i <- 0 until a.size) {
      result += (a(i) :* (x :^ i.toDouble))
    }
    result
  }


  /**
   * Returns a vector containing n equally spaced values starting with 'start' and
   * ending with 'end'.
   */
  def span(start: Double, stop: Double, n: Int): DenseVector[Double] = {
    val inc = (stop - start) / (n - 1.0)
    DenseVector.tabulate[Double](n)(start + _ * inc)
  }


  /**
   * Returns an array containing the indexes of the items in v for which the
   * function f(v(i)) returns true
   */
  def where(v: DenseVector[Double], f: Double => Boolean): Array[Int] = {
    v.toArray.zipWithIndex collect {
      case (d, j) if f(d) => j
    }
  }

  /**
   * Returns a matrix containing the max values of each matrix
   */
  def max(m1: DenseMatrix[Double], m2: DenseMatrix[Double]): DenseMatrix[Double] = {
    // Note: k is a tuple with the (i,j) indexes
    m1.mapPairs((k,a) => if (m1(k) > m2(k)) m1(k) else m2(k))
  }

  /**
   * Returns a matrix containing the min values of each matrix
   */
  def min(m1: DenseMatrix[Double], m2: DenseMatrix[Double]): DenseMatrix[Double] = {
    // Note: k is a tuple with the (i,j) indexes
    m1.mapPairs((k,a) => if (m1(k) < m2(k)) m1(k) else m2(k))
  }

  /**
   * Returns a matrix containing the max values of the matrix and the given value
   */
  def max(m: DenseMatrix[Double], d: Double): DenseMatrix[Double] = {
    m.mapValues(a => if (a > d) a else d)
  }

  /**
   * Returns a matrix containing the min values of the matrix and the given value
   */
  def min(m: DenseMatrix[Double], d: Double): DenseMatrix[Double] = {
    m.mapValues(a => if (a < d) a else d)
  }

  /**
   * Returns a matrix containing the max values of the matrix and the given value
   */
  def max(d: Double, m: DenseMatrix[Double]): DenseMatrix[Double] = {
    m.mapValues(a => if (a > d) a else d)
  }

  /**
   * Returns a matrix containing the min values of the matrix and the given value
   */
  def min(d: Double, m: DenseMatrix[Double]): DenseMatrix[Double] = {
    m.mapValues(a => if (a < d) a else d)
  }

  /**
   * Returns a vector containing the max values of the vector and the given value
   */
  def max(v: DenseVector[Double], d: Double): DenseVector[Double] = {
    v.mapValues(a => if (a > d) a else d)
  }

  /**
   * Returns a vector containing the min values of the vector and the given value
   */
  def min(v: DenseVector[Double], d: Double): DenseVector[Double] = {
    v.mapValues(a => if (a < d) a else d)
  }

  /**
   * Returns a vector containing the max values of the vector and the given value
   */
  def max(d: Double, v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(a => if (a > d) a else d)
  }

  /**
   * Returns a vector containing the min values of the vector and the given value
   */
  def min(d: Double, v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(a => if (a < d) a else d)
  }

  /**
   * Returns a vector containing the max values of each vector
   */
  def max(v1: DenseVector[Double], v2: DenseVector[Double]): DenseVector[Double] = {
    //    v1.join(v2)((a, b) => if (a > b) a else b).asInstanceOf[DenseVector[Double]]  // XXX doesn't work now: try again after scalala update?
    //    v1.toArray.zip(v2.toArray).map(a => if (a._1 > a._2) a._1 else a._2).asVector
    val result = DenseVector.zeros[Double](v1.size)
    for (i <- 0 until v1.size) {
      result(i) = math.max(v1(i), v2(i))
    }
    result
  }

  /**
   * Returns a vector containing the min values of each vector
   */
  def min(v1: DenseVector[Double], v2: DenseVector[Double]): DenseVector[Double] = {
    //    v1.toArray.zip(v2.toArray).map(a => if (a._1 < a._2) a._1 else a._2).asVector
    val result = DenseVector.zeros[Double](v1.size)
    for (i <- 0 until v1.size) {
      result(i) = math.min(v1(i), v2(i))
    }
    result

  }

  /**
   * Returns the argument, which has been "clipped" to mini
   * and maxi, i.e. in which all elements lower than "mini"
   * have been replaced by "mini" and all elements greater
   * than "maxi" by "maxi".
   */
  def clip(m: DenseMatrix[Double], lt: Double, ht: Double): DenseMatrix[Double] = {
    //    if (lt != []) arg = max(arg,lt);
    //    if (ht != []) arg = min(arg,ht);
    //    return arg;
    min(max(m, lt), ht)
  }


  /**
   * Returns the matrix m with absolute values
   */
  def abs(m: DenseMatrix[Double]): DenseMatrix[Double] = {
    m.mapValues(math.abs)
  }

  /**
   * Returns the vector v with absolute values
   */
  def abs(v: DenseVector[Double]): DenseVector[Double] = {
    v.mapValues(math.abs)
  }

  /**
   * Returns sqrt(v1^2 + v2^2), as in the yorick version
   */
  def abs(v1: DenseVector[Double], v2: DenseVector[Double]): DenseVector[Double] = {
    sqrt((v1 :^ 2.0) + (v2 :^ 2.0))
  }

  /**
   * asserts that 2 double vectors are about equal (difference < err)
   */
  def assertVectorsEqual(expect: DenseVector[Double], v: DenseVector[Double], err: Double) {
    val a = abs(expect - v)
    val eq = a.forall(_ < err)
    if (!eq) {
      println("Assertion failed. Expected:\n" + expect + "\nbut got:\n" + v)
    }
    assert(eq)
  }

  /**
   * asserts that 2 double matrices are about equal (difference < err)
   */
  def assertMatricesEqual(expect: DenseMatrix[Double], m: DenseMatrix[Double], err: Double) {
    val a = abs(expect - m)
    val eq = a.forall(_ < err)
    if (!eq) {
      println("Assertion failed. Expected:\n" + expect + "\nbut got:\n" + m)
    }
    assert(eq)
  }

  /**
   * asserts that 2 double matrices are about equal (difference < err)
   */
  def assertArrayMatricesEqual(expect: Array[DenseMatrix[Double]], a: Array[DenseMatrix[Double]], err: Double) {
    if (expect.length != a.length) {
      println("Assertion failed. Array sizes do not match: Expected" + expect.length + " but got: " + a.length)
      assert(false)
    }
    for (i <- a.indices) {
      assertMatricesEqual(expect(i), a(i), err)
    }
  }

  /**
   * Returns a vector with the average of all the values in each row (in Yorick: m(,avg)
   */
  def rowAvg(m: DenseMatrix[Double]): DenseVector[Double] = {
    //    val v = DenseVector.zeros[Double](m.numRows)
    //    for (i <- 0 until m.numRows) {
    //      v(i) = m(i, ::).sum / m.numCols
    //    }
    //    v
    rowSum(m) / m.cols.toDouble
  }

  /**
   * Returns a vector with the average of all the values in each column (in Yorick: m(avg,)
   */
  def colAvg(m: DenseMatrix[Double]): DenseVector[Double] = {
    //    val v = DenseVector.zeros[Double](m.numCols)
    //    for (i <- 0 until m.numCols) {
    //      v(i) = m(::, i).sum / m.numRows
    //    }
    //    v
    colSum(m) / m.rows.toDouble
  }

  /**
   * Returns the average of all the values (in Yorick: avg(m))
   */
  def avg(m: DenseMatrix[Double]): Double = {
    sum(m) / (m.rows * m.cols)
  }

  /**
   * Returns a vector with the sum of all the values in each column (in Yorick: m(sum,)
   */
  def colSum(m: DenseMatrix[Double]): DenseVector[Double] = {
    val v = DenseVector.zeros[Double](m.cols)
    for (i <- 0 until m.cols) {
      v(i) = sum(m(::, i))
    }
    v
  }

  /**
   * Returns a vector with the sum of all the values in each row (in Yorick: m(,sum)
   */
  def rowSum(m: DenseMatrix[Double]): DenseVector[Double] = {
    val v = DenseVector.zeros[Double](m.rows)
    for (i <- 0 until m.rows) {
      v(i) = sum(m(i, ::).t.toDenseVector)
    }
    v
  }

  /**
   * Returns the indexes of the sorted vector v
   */
  def sort(v: DenseVector[Double]): Array[Int] = {
    v.toArray.zipWithIndex.sortBy(_._1).map(_._2)
  }

  /**
   * Returns the root mean square deviation from the arithmetic mean of the values
   */
  def rms(v: DenseVector[Double]): Double = {
    math.sqrt(sum((v - (sum(v) / v.size)).mapValues(math.pow(_, 2.0))) / v.size)
  }

  /**
   * Returns the root mean square deviation from the arithmetic mean of the values
   */
  def rms(m: DenseMatrix[Double]): Double = {
    //    if (m.isInstanceOf[DenseMatrix[Double]]) {
    //      rms((m.asInstanceOf[DenseMatrix[Double]]).data.asVector)
    //    } else {
    //      rms((for (j <- 0 until m.numCols; i <- 0 until m.numRows) yield m(i, j)).toArray.asVector)
    //    }
    val size = m.rows * m.cols
    math.sqrt(sum((m - (sum(m) / size)).mapValues(math.pow(_, 2.0))) / size)
  }

  /**
   * Temp: Add missing support for Array addition
   */
  def add(a: Seq[Int], d: Int) : Seq[Int]  = {
    a.map(_ + d)
  }
}
