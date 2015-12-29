package edu.gemini.ags.gems.mascot.util

import org.junit.Assert._
import breeze.linalg._
import MatrixUtil._
import YUtils.yMultiply
import YUtils.assertVectorsEqual
import YUtils.assertMatricesEqual
import YUtils.abs
import org.junit.Test

/**
 * Tests methods in the MascotUtil class.
 * <p>
 * Notes:
 * <p><pre>
 * Yorick         Scala
 * ------------+---------------
 * c(,+)*d(,+)    c*d.t
 * c(+,)*d(+,)    c.t*d
 * c(,+)*d(+,)    c*d
 * c(+,)*d(,+)    c.t*d.t
 * </pre>
 */

class MatrixUtilTest {


  /**
   * Test case ported from the Yorick version.
   * Tests the ported svd function.
   */
  def testSVD(m: Int, n: Int) {
    //  func testSVD(m, n)
    //  {
    //    a= random(m,n);
    //    s= SVdec(a, u, v);
    //    if (anyof(s(dif)>0.0))
    //      error, "***WARNING*** SVdec returned increasing singular values";
    //    achk= u(,+) * (s*v)(+,);
    //    sabs= max(abs(s));
    //    err= max(abs(a-achk))/sabs;
    //    if (err>1.e-9) {
    //      write, "***WARNING***  SVdec decomposition doesn't check";
    //      write, "   max relative error is "+pr1(err);
    //    }
    val a = DenseMatrix.rand(m, n)
    val (u, s, v) = MatrixUtil.svd(a)
//    assert(s.sorted.toList.reverse == s.toList)
    val achk = u * yMultiply(s, v)
    val sabs = abs(s).max
    val err = abs(a - achk).max / sabs
    assert(err <= 1.0e-9)
  }

  @Test def testSVD() {
    testSVD(3, 3)
    testSVD(4, 4)
    testSVD(5, 5)
    testSVD(6, 5)
  }

  // From the web:
  //Q. Your SVD routine gives a different answer from the SVD routine in Matlab (or Yorick...)
  //
  //A. The SVD decomposition is not completely unique. Columns in the U matrix can be swapped,
  // if those in the V matrix and the elements in the diagonal matrix, D, are swapped in the same way.
  // The signs of the elements in a column of U may be reversed if the signs in the corresponding column
  // in V are reversed. If a number of the singular values are identical one can apply an orthogonal
  // transformation to the corresponding columns of U and the corresponding columns of V.
  @Ignore
  @Test def testSVD2() {
    val a = DenseMatrix(
      (1.0, -0.0, -0.023180133333333335, -0.1359605544139689, -0.016390829468808328),
      (-0.0, 1.0, -0.19227726, -0.016390829468808328, 0.1359605544139689),
      (1.0, -0.0, -0.36523093333333334, 0.16541640967397533, -0.25825726965909185),
      (-0.0, 1.0, 0.23393413, -0.25825726965909185, -0.16541640967397533),
      (1.0, 0.0, 0.3884110666666667, -0.029455855260006443, 0.27464809912790017),
      (0.0, 1.0, -0.041656869999999985, 0.27464809912790017, 0.029455855260006443))


    val (u, s, v) = MatrixUtil.svd(a)

//    assert(s.sorted.toList.reverse == s.toList)
    val achk = u * yMultiply(s, v)
    val sabs = abs(s).max
    val err = abs(a - achk).max / sabs
    assert(err <= 1.0e-9)

    assertVectorsEqual(DenseVector(1.73205, 1.73205, 0.706012, 0.434871, 0.26246), s, 0.0001)

    assertMatricesEqual(
      abs(DenseMatrix(
        (-0.57735, 0.0, -0.150874, -0.196319, 0.733775),
        (0.0, -0.57735, 0.0361116, 0.246225, 0.322759),
        (-0.57735, 0.0, 0.332095, 0.68772, -0.251846),
        (0.0, -0.57735, -0.663096, 0.156248, -0.230775),
        (-0.57735, 0.0, -0.181221, -0.491401, -0.481929),
        (0.0, -0.57735, 0.626984, -0.402473, -0.0919834))),
      abs(u), 0.00001)

    /*assertMatricesEqual(
      abs(DenseMatrix(
        (0.0, -1.0, 2.38674E-16, -2.46195E-16, 7.84579E-16),
        (-1.0, -0.0, -0.0, -0.0, -0.0),
        (0.0, 5.11629E-16, 0.848599, -0.376948, 0.371201),
        (-0.0, -3.66514E-16, -4.1672E-16, -0.701654, -0.712518),
        (-0.0, 4.49701E-16, -0.529037, -0.604642, 0.595423))),
      abs(v), 0.0001)*/
  }

  /**
   * Test case ported from the Yorick version.
   * Tests the ported tdSolve function.
   */
  def testTD(n: Int) {
    //func testTD(n)
    //{
    //  c= random(n-1);
    //  d= random(n);
    //  e= random(n-1);
    //  b= random(n);
    //  TDcheck,c,d,e,b,TDsolve(c,d,e,b), "1D";
    //  b2= random(n);
    //  x= TDsolve(c,d,e,[b,b2])
    //  TDcheck,c,d,e,b, x(,1), "2D(1)";
    //  TDcheck,c,d,e,b2, x(,2), "2D(2)";
    //  x= TDsolve(c,d,e,transpose([b,b2]), which=2)
    //  TDcheck,c,d,e,b, x(1,), "2D(1)/which";
    //  TDcheck,c,d,e,b2, x(2,), "2D(2)/which";
    //}

    val c = DenseVector.rand(n - 1)
    val d = DenseVector.rand(n)
    val e = DenseVector.rand(n - 1)
    val b = DenseVector.rand(n)
    tdCheck(c, d, e, b, tdSolve(c, d, e, b))
    // Other tests don't apply, since only 1D is implemented here
  }

  def tdCheck(c: DenseVector[Double], d: DenseVector[Double], e: DenseVector[Double], b: DenseVector[Double], x: DenseVector[Double]) {
    //func TDcheck(c, d, e, b, x, s)
    //{
    //  check= _(   d(1)*x(1)    +    e(1)*x(2),
    //           c(1:-1)*x(1:-2) + d(2:-1)*x(2:-1) + e(2:0)*x(3:0),
    //                                c(0)*x(-1)   +   d(0)*x(0)   );
    //  if (max(abs(check-b))>1.e-9*max(abs(b))) {
    //    write, "***WARNING*** "+s+" tridiagonal solution doesn't check";
    //    write, "   max relative error is "+pr1((max(abs(check-b)))/max(abs(b)));
    //  }
    //}
    val check = DenseVector.zeros[Double](b.size)
    check(0) = d(0) * x(0) + e(0) * x(1)
    check(1 until b.size - 1) := (c(0 until c.size - 1) :* x(0 until x.size - 2)) + (d(1 until d.size - 1) :* x(1 until x.size - 1)) + (e(1 until e.size) :* x(2 until x.size))
    check(b.size - 1) = c(c.size - 1) * x(x.size - 2) + d(d.size - 1) * x(x.size - 1)
    if (abs(check - b).max > 1.0e-9 * abs(b).max) {
      println("***WARNING*** tridiagonal solution doesn't check")
      println("max relative error is " + abs(check - b).max / abs(b).max)
      assertTrue(false)
    }
  }

  @Test def testTdCheck() {
    testTD(7)
    testTD(20)
  }
}