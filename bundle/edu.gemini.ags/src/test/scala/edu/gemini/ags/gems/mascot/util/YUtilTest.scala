package edu.gemini.ags.gems.mascot.util

import org.junit.Assert._
import breeze.linalg._
import YUtils._
import org.junit.{Ignore, Test}

/**
 * Tests methods in the YUtil class.
 */

class YUtilTest {

  val A =
    Array(
      Array(
        DenseMatrix(
          (1.0, 3.0),
          (2.0, 4.0)
        ),
        DenseMatrix(
          (5.0, 7.0),
          (6.0, 8.0)
        )
      ),
      Array(
        DenseMatrix(
          (9.0, 11.0),
          (10.0, 12.0)
        ),
        DenseMatrix(
          (13.0, 15.0),
          (14.0, 16.0)
        )
      )
    )


//  @Test def testYFormat() {
//    assertEquals("[1.0,2.0,3.0]", yFormat(Vector(1.0, 2.0, 3.0)))
//    assertEquals("[[1.0,4.0],[2.0,5.0],[3.0,6.0]]", yFormat(DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0))))
//  }

  @Test def testYmultiply() {
    assertEquals(DenseMatrix((1.0, 2.0, 3.0), (8.0, 10.0, 12.0), (21.0, 24.0, 27.0)), yMultiply(DenseVector(1.0, 2.0, 3.0), DenseMatrix((1.0, 2.0, 3.0), (4.0, 5.0, 6.0), (7.0, 8.0, 9.0))))
    assertEquals(DenseMatrix((1.0, 4.0, 7.0), (4.0, 10.0, 16.0), (9.0, 18.0, 27.0)), yMultiply(DenseVector(1.0, 2.0, 3.0), DenseMatrix((1.0, 4.0, 7.0), (2.0, 5.0, 8.0), (3.0, 6.0, 9.0))))
  }



  @Test def testDivide() {
    assertEquals(DenseVector(2.0, 1.0, 0.5, 0.25), divide(2.0, DenseVector(1.0, 2.0, 4.0, 8.0)))
  }

  @Test def testPow() {
    assertEquals(DenseVector(2.0, 4.0, 16.0, 256.0, 2048.0), YUtils.pow(2.0, DenseVector(1.0, 2.0, 4.0, 8.0, 11.0)))
  }

  @Test def testPowArray() {
    val a =
      Array(
        Array(
          DenseMatrix(
            (1.0, 9.0),
            (4.0, 16.0)
          ),
          DenseMatrix(
            (25.0, 49.0),
            (36.0, 64.0)
          )
        ),
        Array(
          DenseMatrix(
            (81.0, 121.0),
            (100.0, 144.0)
          ),
          DenseMatrix(
            (169.0, 225.0),
            (196.0, 256.0)
          )
        )
      )
    val result = YUtils.pow(A, 2.0)
    assertArrayMatricesEqual(a(0), result(0), 0.00001)
    assertArrayMatricesEqual(a(1), result(1), 0.00001)
    assertEquals(2.0, A(0)(0)(1, 0), 0.0001)
  }

  @Test def testDif() {
    assertEquals(DenseVector(1.0, 2.0, 4.0, 3.0), dif(DenseVector(1.0, 2.0, 4.0, 8.0, 11.0)))
  }

  @Test def testPcen() {
    assertEquals(DenseVector(2.0, 4.0, 7.5, 6.0, 4.0, 5.0), pcen(DenseVector(2.0, 6.0, 9.0, 3.0, 5.0)))
  }

  @Test def testGrow() {
    assertEquals(DenseVector(1.0, 2.0, 3.0, 4.0), grow(DenseVector(1.0, 2.0, 3.0), 4.0))
    assertEquals(DenseVector(0.0, 1.0, 2.0, 3.0, 4.0), grow(0.0, DenseVector(1.0, 2.0, 3.0), 4.0))
  }

  /**
   * Test the digitize function.
   * Example Yorick output:
   * <p><pre>
   * xp=[3.5e-07,4.33333e-07,5.16667e-07,6e-07,6.83333e-07,7.66667e-07,8.5e-07,9.33333e-07,1.01667e-06,1.1e-06]
   * x=[-4e-07,3.5e-07,4e-07,5e-07,6e-07,7e-07,8e-07,9e-07,1e-06,1.1e-06,1.85e-06]
   * digitize(xp,x)
   * [3,4,5,6,6,7,8,9,10,11]
   * </pre>
   */
  @Test def testDigitize() {
    val xp = DenseVector(3.5e-07, 4.33333e-07, 5.16667e-07, 6e-07, 6.83333e-07, 7.66667e-07, 8.5e-07, 9.33333e-07, 1.01667e-06, 1.1e-06)
    val x = DenseVector(-4e-07, 3.5e-07, 4e-07, 5e-07, 6e-07, 7e-07, 8e-07, 9e-07, 1e-06, 1.1e-06, 1.85e-06)
    val a = digitize(xp, x)
    // Yorick indexes start at 1, so subtract 1 from the Yorick result to get the correct result for Scala
    assertEquals(DenseVector(3, 4, 5, 6, 6, 7, 8, 9, 10, 11) - 1, DenseVector(a))
  }


  /**
   * Test the poly function:
   * poly(x, a0, a1, a2, ...0, aN)
   * returns the polynomial  A0 + A1*x + A2*x^2 + ... + AN*X^N
   * <p>
   *   Example:<br>
   * poly([1.0,2.0,3.],[2.0,4.0,6.]) = [2,4,6]
   * poly([1.0,2.0,3.],[2.0,4.0,6.],[3.0,6.0,9.]) = [5,16,33]
   * poly([1.0,2.0,3.],[2.0,4.0,6.],[3.0,6.0,9.],[5.0,7.0,9.]) = [10,44,114]
   * <p><pre>
   * </pre>
   */
  @Test def testPoly() {
    assertEquals(DenseVector(2.0, 4.0, 6.0), poly(DenseVector(1.0, 2.0, 3.0), DenseVector(2.0, 4.0, 6.0)))
    assertEquals(DenseVector(5.0, 16.0, 33.0), poly(DenseVector(1.0, 2.0, 3.0), DenseVector(2.0, 4.0, 6.0), DenseVector(3.0, 6.0, 9.0)))
    assertEquals(DenseVector(10.0, 44.0, 114.0), poly(DenseVector(1.0, 2.0, 3.0), DenseVector(2.0, 4.0, 6.0), DenseVector(3.0, 6.0, 9.0), DenseVector(5.0, 7.0, 9.0)))
  }

  @Test def testSpan() {
    assertVectorsEqual(DenseVector(1.0, 2.0, 3.0, 4.0, 5.0), span(1.0, 5.0, 5), 0.00001)
    assertVectorsEqual(DenseVector(1, 1.8, 2.6, 3.4, 4.2, 5), span(1.0, 5.0, 6), 0.00001)
  }

  @Test def testWhere() {
    assertEquals(DenseVector(3, 4, 5), DenseVector(where(DenseVector(1.0, 1.8, 2.6, 3.4, 4.2, 5.0), _ > 3.0)))
  }

  @Test def testMinMax() {
    val m1 = DenseMatrix((1.0, 2.0, 3.0), (8.0, 10.0, 12.0), (21.0, 24.0, 27.0))
    val m2 = DenseMatrix((2.0, 1.0, 5.0), (9.0, 1.0, 16.0), (22.0, -24.0, -23.0))
    assertEquals(DenseMatrix((1.0, 1.0, 3.0), (8.0, 1.0, 12.0), (21.0, -24.0, -23.0)), YUtils.min(m1, m2))
    assertEquals(DenseMatrix((2.0, 2.0, 5.0), (9.0, 10.0, 16.0), (22.0, 24.0, 27.0)), YUtils.max(m1, m2))

    assertEquals(DenseMatrix((0.0, 0.0, 0.0), (0.0, 0.0, 0.0), (0.0, -24.0, -23.0)), YUtils.min(m2, 0.0))
    assertEquals(DenseMatrix((2.0, 1.0, 5.0), (9.0, 1.0, 16.0), (22.0, 0.0, 0.0)), YUtils.max(m2, 0.0))
    assertEquals(YUtils.min(0, m2), YUtils.min(m2, 0.0))
    assertEquals(YUtils.max(0, m2), YUtils.max(m2, 0.0))

    assertEquals(DenseMatrix((2.0, 1.0, 5.0), (9.0, 1.0, 10.0), (10.0, 0.0, 0.0)), clip(m2, 0, 10))


    val v2 = DenseVector(2.0, 1.0, 5.0, 9.0, 1.0, 16.0, 22.0, -24.0, -23.0)
    assertEquals(DenseVector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -24.0, -23.0), YUtils.min(v2, 0.0))
    assertEquals(DenseVector(2.0, 1.0, 5.0, 9.0, 1.0, 16.0, 22.0, 0.0, 0.0), YUtils.max(v2, 0.0))
    assertEquals(YUtils.min(0, v2), YUtils.min(v2, 0.0))
    assertEquals(YUtils.max(0, v2), YUtils.max(v2, 0.0))
  }


  @Test def testAvg() {
    //[[1,2],[3,4],[5,6],[7,8]]
    //> m(,avg)
    //[4,5]
    //> m(avg,)
    //[1.5,3.5,5.5,7.5]
    val m1 = DenseMatrix((1.0, 2.0), (3.0, 4.0), (5.0, 6.0), (7.0, 8.0)).t
    assertEquals(DenseVector(4.0, 5.0), rowAvg(m1))
    assertEquals(DenseVector(1.5, 3.5, 5.5, 7.5), colAvg(m1))

    //> m=[[1,2,3,4,5],[6,7,8,9,0]]
    //> m(,avg)
    //[3.5,4.5,5.5,6.5,2.5]
    //> m(avg,)
    //[3,6]
    val m2 = DenseMatrix((1.0, 2.0, 3.0, 4.0, 5.0), (6.0, 7.0, 8.0, 9.0, 0.0)).t
    assertEquals(DenseVector(3.5, 4.5, 5.5, 6.5, 2.5), rowAvg(m2))
    assertEquals(DenseVector(3.0, 6.0), colAvg(m2))

    //      [[1,2,3,4,5],[6,7,8,9,0]]
    //      > avg(m)
    //      4.5
    assertEquals(4.5, avg(DenseMatrix((1.0, 2.0, 3.0, 4.0, 5.0), (6.0, 7.0, 8.0, 9.0, 0.0)).t), 0.0001)
  }


  @Test def testSum() {
    //    > m=[[1,2],[3,4],[5,6],[7,8]]
    //    > m(,sum)
    //    [16,20]
    //    > m(sum,)
    //    [3,7,11,15]
    val m1 = DenseMatrix((1.0, 2.0), (3.0, 4.0), (5.0, 6.0), (7.0, 8.0)).t
    assertEquals(DenseVector(16.0, 20.0), rowSum(m1))
    assertEquals(DenseVector(3.0, 7.0, 11.0, 15.0), colSum(m1))

    //    > m=[[1,2,3,4,5],[6,7,8,9,0]]
    //    > m(,sum)
    //    [7,9,11,13,5]
    //    > m(sum,)
    //    [15,30]
    val m2 = DenseMatrix((1.0, 2.0, 3.0, 4.0, 5.0), (6.0, 7.0, 8.0, 9.0, 0.0)).t
    assertEquals(DenseVector(7.0, 9.0, 11.0, 13.0, 5.0), rowSum(m2))
    assertEquals(DenseVector(15.0, 30.0), colSum(m2))
  }


  @Test def testSort() {
    // > sort([2.0,6.0,3.0,1.0,4.0,9.]) = [4,1,3,5,2,6] (-1 for scala 0 based indexes)
    assertArrayEquals(Array(3, 0, 2, 4, 1, 5), sort(DenseVector(2.0, 6.0, 3.0, 1.0, 4.0, 9.0)))
  }


  @Test def testRms() {
    //  > v=[1,2,3,4,5,6,7,8,9,10]
    //  > v(rms)
    //  2.87228
    assertEquals(2.87228, rms(DenseVector(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)), 0.0001)

    //      [[1,2,3,4,5],[6,7,8,9,0]]
    //      > m(*)(rms)
    //      2.87228
    assertEquals(2.87228, rms(DenseMatrix((1.0, 2.0, 3.0, 4.0, 5.0), (6.0, 7.0, 8.0, 9.0, 0.0)).t), 0.0001)
    assertEquals(2.87228, rms(DenseMatrix((1.0, 2.0, 3.0, 4.0, 5.0), (6.0, 7.0, 8.0, 9.0, 0.0))), 0.0001)
  }


}