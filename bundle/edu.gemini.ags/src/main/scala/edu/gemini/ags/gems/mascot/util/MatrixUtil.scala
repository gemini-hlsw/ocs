package edu.gemini.ags.gems.mascot.util

import breeze.linalg._
import breeze.util._
import org.netlib.lapack.LAPACK
import org.netlib.util.intW

/**
 * Matrix utilities
 */

object MatrixUtil {

  /**
   * Replacement for LinearAlgebra.svd, for compatibility with the Yorick version, which calls
   * different LAPACK routines and therefore gets somewhat different values
   */
  def svd(mat: DenseMatrix[Double]): (DenseMatrix[Double], DenseVector[Double], DenseMatrix[Double]) = {
    //    func SVdec(a, &u, &vt, full=)
    ///* DOCUMENT s= SVdec(a, u, vt)
    //         or s= SVdec(a, u, vt, full=1)
    //
    //     performs the singular value decomposition of the m-by-n matrix A:
    //        A = (U(,+) * SIGMA(+,))(,+) * VT(+,)
    //     where U is an m-by-m orthogonal matrix, VT is an n-by-n orthogonal
    //     matrix, and SIGMA is an m-by-n matrix which is zero except for its
    //     min(m,n) diagonal elements.  These diagonal elements are the return
    //     value of the function, S.  The returned S is always arranged in
    //     order of descending absolute value.  U(,1:min(m,n)) are the left
    //     singular vectors corresponding to the min(m,n) elements of S;
    //     VT(1:min(m,n),) are the right singular vectors.  (The original A
    //     matrix maps a right singular vector onto the corresponding left
    //     singular vector, stretched by a factor of the singular value.)
    //
    //     Note that U and VT are strictly outputs; if you don't need them,
    //     they need not be present in the calling sequence.
    //
    //     By default, U will be an m-by-min(m,n) matrix, and V will be
    //     a min(m,n)-by-n matrix (i.e.- only the singular vextors are returned,
    //     not the full orthogonal matrices).  Set the FULL keyword to a
    //     non-zero value to get the full m-by-m and n-by-n matrices.
    //
    //     On rare occasions, the routine may fail; if it does, the
    //     first SVinfo values of the returned S are incorrect.  Hence,
    //     the external variable SVinfo will be 0 after a successful call
    //     to SVdec.  If SVinfo>0, then external SVe contains the superdiagonal
    //     elements of the bidiagonal matrix whose diagonal is the returned
    //     S, and that bidiagonal matrix is equal to (U(+,)*A(+,))(,+) * V(+,).
    //
    //     Numerical Recipes (Press, et. al. Cambridge University Press 1988)
    //     has a good discussion of how to use the SVD -- see section 2.9.
    //
    //   SEE ALSO: SVsolve, LUsolve, QRsolve, TDsolve
    // */
    //    {
    //      /* get n, m, dims, nrhs, checking validity of a and b */
    //      { local dims, n, m, nrhs; }
    //      b= [];
    //      _get_matrix, 1;
    //
    //      if (!full) full= 0;
    //      else full= 1;
    //
    //      /* set up and perform SVD solve --
    //         first call returns optimal workspace length */
    //      work= 0.0;
    //      info= 0;
    //      s= array(0.0, min(m,n));
    //      if (full) {
    //        u= array(0.0, m, m);
    //        vt= array(0.0, n, n);
    //        ldvt= n;
    //      } else {
    //        ldvt= min(m, n);
    //        u= array(0.0, m, ldvt);
    //        vt= array(0.0, ldvt, n);
    //      }
    //      _dgesvx, full, m, n, a, m, s, u, m, vt, ldvt, work, 1, info;
    //      if (info==-13) {
    //        lwork= long(work);
    //        work= array(0.0, lwork);
    //        _dgesvx, full, m, n, a, m, s, u, m, vt, ldvt, work, lwork, info;
    //      }
    //      if (info) error, "SVD algorithm failed to converge - wow";
    //
    //      return s;
    //    }


    val m = mat.rows
    val n = mat.cols
    val ldvt = m min n
    val S = DenseVector.zeros[Double](ldvt)
    val U = DenseMatrix.zeros[Double](m, ldvt)
    val Vt = DenseMatrix.zeros[Double](ldvt, n)
//    val iwork = new Array[Int](8 * ldvt);
    val workSize = (3
      * ldvt
      * ldvt
      + math.max(math.max(m, n), 4 * ldvt
      * ldvt + 4 * ldvt)
      )
    val work = new Array[Double](workSize)
    val info = new intW(0)
    //    LAPACK.getInstance.dgesdd(
    //      "S", m, n,
    //      mat.copy.data, math.max(1,m),
    //      S.data, U.data, math.max(1,m),
    //      Vt.data, math.max(1,n),
    //      work,work.length,iwork, info);

    
    LAPACK.getInstance.dgesvd(
      "S", "S", m, n,
      mat.copy.data, math.max(1, m),
      S.data, U.data, math.max(1, m),
      Vt.data, ldvt,
      work, work.length, info)

    if (info.`val` > 0)
      throw new NotConvergedException(NotConvergedException.Iterations)
    else if (info.`val` < 0)
      throw new IllegalArgumentException()

    (U, S, Vt)
  }


  /**
   * Ported from Yorick: func TDsolve(c, d, e, b, which=)
   * <p>
   * Note: Here we only deal with vectors. The original Yorick version also dealt with multiple dimensions.
   */
  def tdSolve(pc: DenseVector[Double], pd: DenseVector[Double], pe: DenseVector[Double], pb: DenseVector[Double]): DenseVector[Double] = {
    //  /* DOCUMENT TDsolve(c, d, e, b)
    //           or TDsolve(c, d, e, b, which=which)
    //
    //       returns the solution to the tridiagonal system:
    //          D(1)*x(1)       + E(1)*x(2)                       = B(1)
    //          C(1:-1)*x(1:-2) + D(2:-1)*x(2:-1) + E(2:0)*x(3:0) = B(2:-1)
    //                            C(0)*x(-1)      + D(0)*x(0)     = B(0)
    //       (i.e.- C is the subdiagonal, D the diagonal, and E the superdiagonal;
    //       C and E have one fewer element than D, which is the same length as
    //       both B and x)
    //
    //       B may have additional dimensions, in which case the returned x
    //       will have the same additional dimensions.  The WHICH dimension of B,
    //       and of the returned x is the one of length n which participates
    //       in the matrix solve.  By default, WHICH=1, so that the equations
    //       being solved involve B(,..) and x(+,..).
    //       Non-positive WHICH counts from the final dimension (as for the
    //       sort and transpose functions), so that WHICH=0 involves B(..,)
    //       and x(..,+).
    //
    //       The C, D, and E arguments may be either scalars or vectors; they
    //       will be broadcast as appropriate.
    //
    //    SEE ALSO: LUsolve, QRsolve, SVsolve, SVdec
    //  */
    //  /* check validity of b argument */
    //  if (structof(b)==complex) error, "expecting a non-complex RHS vector";
    //  dims= dimsof(b);
    //  ndb= is_void(dims)? 0 : dims(1);
    //  if (is_void(which)) which= 1;
    //  else if (which<=0) which+= ndb;
    //  if (!ndb) error, "RHS must have at least one dimension";
    //  n= dims(1+which);
    //  b= double(b);   /* copy of RHS to be transformed into solution */
    //  nrhs= numberof(b)/n;
    //
    //  /* put first matrix dimension of b first */
    //  if (which!=1) b= transpose(b, [1,which]);
    //
    //  /* copy, force to double, and broadcast matrix diagonals
    //     -- also will blow up on conformability error */
    //  cc= ee= array(0.0, n-1);
    //  dd= array(0.0, n);
    //  cc()= c;
    //  dd()= d;
    //  ee()= e;
    //
    //  info= 0;
    //  _dgtsv, n, nrhs, cc, dd, ee, b, n, info;
    //  if (info) error, "tridiagonal element "+pr1(info)+" of became 0.0";
    //
    //  /* restore proper order of result if necessary */
    //  if (which!=1) b= transpose(b, [1,which]);
    //
    //  return b;

    val n = pb.size
    val b = pb :- 0.0 // copy of RHS to be transformed into solution
    val c = pc :- 0.0 // in/out parameters!
    val d = pd :- 0.0
    val e = pe :- 0.0
    val nrhs = 1
    val info = new intW(0)
    // Cast to DenseVector so we can access the internal data to pass to the java LAPACK routine
    val bb = b.asInstanceOf[DenseVector[Double]]
    val cc = c.asInstanceOf[DenseVector[Double]]
    val dd = d.asInstanceOf[DenseVector[Double]]
    val ee = e.asInstanceOf[DenseVector[Double]]
    LAPACK.getInstance.dgtsv(n, nrhs, cc.data, dd.data, ee.data, bb.data, n, info)
    if (info.`val` != 0) {
      throw new IllegalArgumentException("tridiagonal element became 0.0")
    }
    b;
  }

  /*
   * Returns a Scalala formatted string for the array
   */
  def sFormat(a: Array[Array[DenseMatrix[Double]]]): String = {
    val sb = new StringBuilder;
    sb.append("Array(\n")
    for (i <- 0 until a.size) {
      sb.append(sFormat(a(i)))
       if (i != a.size - 1) {
         sb.append(",\n")
       }
    }
    sb.append(")\n")
    sb.toString
  }

  /*
   * Returns a Scalala formatted string for the array
   */
  def sFormat(a: Array[DenseMatrix[Double]]): String = {
    val sb = new StringBuilder
    sb.append("Array(\n")
    for (i <- 0 until a.size) {
      sb.append(sFormat(a(i)))
       if (i != a.size - 1) {
         sb.append(",\n")
       }
    }
    sb.append(")\n")
    sb.toString
  }

  /*
   * Returns a Scalala formatted matrix string
   */
  def sFormat(m: DenseMatrix[Double]): String = {
    val sb = new StringBuilder
    sb.append("Matrix(\n")
    for (row <- 0 until m.rows) {
      sb.append("(")
      for (col <- 0 until m.cols) {
        sb.append(m(row, col))
        if (col != m.cols - 1) {
          sb.append(",")
        }
      }
      sb.append(")")
      if (row != m.cols - 1) {
        sb.append(",\n")
      }
    }
    sb.append(")\n")
    sb.toString
  }

  /*
   * Returns a Scalala formatted vector string
   */
  def sFormat(v: DenseVector[Double]): String = {
    val sb = new StringBuilder
    sb.append("Vector(")
    for (i <- 0 until v.size) {
      sb.append(v(i))
      if (i != v.size - 1) {
        sb.append(",")
      }
    }
    sb.append(")")
    sb.toString
  }


}
