package edu.gemini.ags.gems.mascot

import breeze.linalg._

import edu.gemini.ags.gems.mascot.util.YUtils._

object Amoeba {

  //func amoeba(ftol,funcName,&nCalls,&y,nMax=,p0=,scale=,p=)
  //{
  //  if (scale != []) {    //If set, then p0 is initial starting pnt
  //    ndim = numberof(p0);
  //    p = p0(,-::ndim);
  //    for (i=1;i<=ndim;i++) p(i,i+1) = p0(i) + scale(clip(i,,numberof(scale)));
  //  }
  //
  //  s = dimsof(p);
  //  if (s(1) != 2) write, "Either (scale,p0) or p must be initialized";
  //  ndim = s(2);			//Dimensionality of simplex
  //  mpts = ndim+1;			//# of points in simplex
  //  if (nMax == [])  nMax = long(5000);
  //
  //  val = funcName(p(,1));
  //  y = array(val, mpts);  //Init Y to proper type
  //  for (i=2;i<=ndim+1;i++) y(i) = funcName(p(,i));   //Fill in rest of the vals
  //  nCalls = 0;
  //  psumm = p(,sum);
  //
  //  do { //Each iteration
  //    s = sort(y);
  //    ilo = s(1);		//Lowest point
  //    ihi = s(ndim+1);		//Highest point
  //    inhi = s(ndim);	//Next highest point
  //    d = abs(y(ihi)) + abs(y(ilo)); //Denominator = interval
  //    if (d != 0.0) rtol = 2.0 * abs(y(ihi)-y(ilo))/d;
  //    else rtol = ftol / 2.;         //Terminate if interval is 0
  //
  //    if (rtol < ftol) {//Done?
  //      t = y(1);
  //      y(1) = y(ilo);
  //      y(ilo) = t;   //Sort so fcn min is 0th elem
  //      t = p(,ilo);
  //      p(,ilo) = p(,1);
  //      p(,1) = t;
  //      return t;                 //params for fcn min
  //    }
  //
  //    nCalls = nCalls + 2;
  //    ytry = amotry(p, y, psumm, funcName, ihi, -1.0);
  //    if (ytry <= y(ilo)) ytry = amotry(p,y,psumm, funcName,ihi,2.0);
  //    else if (ytry >= y(inhi)) {
  //      ysave = y(ihi);
  //      ytry = amotry(p,y,psumm,funcName, ihi, 0.5);
  //      if (ytry >= ysave) {
  //        for (i=1;i<=ndim+1;i++) {
  //          if (i != ilo) {
  //            psumm = 0.5 * (p(,i) + p(,ilo));
  //            p(,i) = psumm;
  //            y(i) = funcName(psumm);
  //          }
  //        }
  //        nCalls += ndim;
  //        psumm = p(,sum);
  //      }		//ytry ge ysave
  //    } else nCalls -= 1;
  //  } while (nCalls < nMax);
  //
  //  return -1;		//Here, the function failed to converge.
  //}

  def amoeba(ftol: Double,
             funcName: DenseVector[Double] => Double,
             nMax: Int,
             p0: DenseVector[Double],
             scale: Double)
  : (DenseVector[Double], Int, DenseVector[Double]) = {
    // Porting note: out params &nCalls and &y are included in the return tuple, p is not passed anywhere.
    // The scala version returns a 3-tuple: (t, nCalls, y): The original returned just t.

    val ndim = p0.size
    val p = yMultiply(p0, DenseMatrix.ones[Double](ndim, ndim + 1))
    for (i <- 0 until ndim) p(i, i + 1) = p0(i) + scale

    val mpts = ndim + 1 //# of points in simplex

    val value = funcName(p(::, 0))
    val y = DenseVector.fill(mpts)(value) //Init Y to proper type
    for (i <- 1 to ndim) y(i) = funcName(p(::, i)) //Fill in rest of the values
    var nCalls = 0
    var psumm = rowSum(p)

    do {
      //Each iteration
      // Porting note: yorick sort returns the indexes of the sorted items!
      val s = sort(y)
      val ilo = s(0); //Lowest point
      val ihi = s(ndim); //Highest point
      val inhi = s(ndim - 1); //Next highest point
      val d = math.abs(y(ihi)) + math.abs(y(ilo)); //Denominator = interval
      //Terminate if interval is 0
      val rtol = if (d != 0.0) 2.0 * math.abs(y(ihi) - y(ilo)) / d else ftol / 2.0

      if (rtol < ftol) {
        //Done?
        val t = y(0)
        y(0) = y(ilo)
        y(ilo) = t; //Sort so fcn min is 0th elem
        val t2 = p(::, ilo).copy
        p(::, ilo) := p(::, 0)
        p(::, 0) := t2
        return (t2, nCalls, y) //params for fcn min
      }
      nCalls = nCalls + 2
      var ytry = amotry(p, y, psumm, funcName, ihi, -1.0)
      if (ytry <= y(ilo)) {
        ytry = amotry(p, y, psumm, funcName, ihi, 2.0)
      } else if (ytry >= y(inhi)) {
        val ysave = y(ihi)
        ytry = amotry(p, y, psumm, funcName, ihi, 0.5)
        if (ytry >= ysave) {
          for (i <- 0 to ndim) {
            if (i != ilo) {
              psumm = (p(::, i) + p(::, ilo)) * 0.5
              p(::, i) := psumm
              y(i) = funcName(psumm)
            }
          }
          nCalls += ndim
          psumm = rowSum(p)
        } //ytry ge ysave
      } else {
        nCalls -= 1
      }
    } while (nCalls < nMax)

    // return value if the function failed to converge
    throw new IllegalArgumentException("failed to converge")
  }


  //func amotry(&p,y,&psumm,funcName,ihi,fac)
  //{
  //  /* Extrapolates by a factor fac through the face of the simplex, across
  //     from the high point, tries it and replaces the high point if the new
  //     point is better.
  //  */
  def amotry(p: DenseMatrix[Double],
             y: DenseVector[Double],
             psumm: DenseVector[Double],
             funcName: DenseVector[Double] => Double,
             ihi: Int,
             fac: Double): Double = {

    // Porting note: the values in p, y and psumm are modified (as in the original)

    //  fac1 = (1.0 - fac) / numberof(psumm);
    //  fac2 = fac1  - fac;
    //  ptry = psumm * fac1 - p(,ihi) * fac2;
    //  ytry = funcName(ptry); //Eval fcn at trial point
    //  if (ytry < y(ihi)) {   //If its better than highest, replace highest
    //    y(ihi) = ytry;
    //    psumm = psumm + ptry - p(,ihi);
    //    p(1:,ihi) = ptry;
    //  }
    //  return ytry;

    val fac1 = (1.0 - fac) / psumm.size
    val fac2 = fac1 - fac
    val ptry = psumm * fac1 - p(::, ihi) * fac2
    val ytry = funcName(ptry); //Eval fcn at trial point
    if (ytry < y(ihi)) {
      //If its better than highest, replace highest
      y(ihi) = ytry
      psumm += ptry - p(::, ihi)
      p(::, ihi) := ptry
    }
    ytry
  }
}
