package edu.gemini.ags.gems.mascot

import org.junit.Test
import breeze.linalg._
import edu.gemini.ags.gems.mascot.util.YUtils.{abs, assertVectorsEqual}
import Amoeba.amoeba

class AmoebaTest {

  //func my_func(p) {
  //  x=(float(indgen(17))-1.)*5.;
  //  y=[ 12.0, 24.3, 39.6, 51.0, 66.5, 78.4, 92.7, 107.8, 120.0, 135.5, 147.5, 161.0, 175.4, 187.4, 202.5, 215.4, 229.9];
  //  return max(abs(y-(p(1)+p(2)*x)));
  //}
  def myFunc(p: DenseVector[Double]): Double = {
    val x = DenseVector.tabulate(17)(_ * 5.0)
    val y = DenseVector(12.0, 24.3, 39.6, 51.0, 66.5, 78.4, 92.7, 107.8, 120.0, 135.5, 147.5, 161.0, 175.4, 187.4, 202.5, 215.4, 229.9)
    max(abs(y - (x * p(1) + p(0))))
  }


  //func test_amoeba(void)
  //// test function similar to the idl one
  ////IDL prints:
  ////Intercept, Slope:      11.4100      2.72800
  ////Function value:       1.33000
  //{
  //  r=amoeba(1.e-5,my_func,nc,fval,p0=[0.,0.],scale=1.e2);
  //  r;
  //  fval;
  //}
  @Test def testAmoeba() {
    val (r, nc, fval) = amoeba(1.0e-5, myFunc, 5000, DenseVector(0.0, 0.0), 1.0e2)
    //    [11.41,2.728]
    //    [1.33002,1.33003,1.33002]

    assertVectorsEqual(DenseVector(11.41, 2.728), r, 0.01)
    assertVectorsEqual(DenseVector(1.33002, 1.33003, 1.33002), fval, 0.0001)
  }

}
