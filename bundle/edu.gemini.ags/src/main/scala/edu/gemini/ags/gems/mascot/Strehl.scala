package edu.gemini.ags.gems.mascot

import breeze.linalg._

import MascotUtils._
import MascotConf._
import Amoeba._
import util.Spline._
import util.YUtils._
import scala.collection.JavaConverters._

// mascot Strehl compute/optimize using distortion modes
// instead of quadratic/tt phase (original method).

// todo:
// - introduce wind velocity in zernike spectra (done)
// - make sure ftcb is not sqrt (done, ok)
// - make sure it works with 2 stars and 1 star only (done)
// - do I need to isolate TT from quads? (done: not needed)
// - make the thing faster (compute what takes time)
// - transform Cn2 profile into distortion amplitude (done for
//   tt, approximated for quads).
// - clean up code implementation

// checks
// * when putting wind=10 for all layers, knee for tt is 0.63Hz
// and for quad at 1.10Hz. That matches well with the formula
// 0.3*(n+1)(v/D), which would give 0.3*2*10/8.= 0.75 and 1.12Hz.
// so there seems to be no issue with null_modes_spectra (v>=0.5)
// * changing the number of point npt in over which ftcb is
// computed doesn't change significantly the results (lowering
// from 512 to 256 seems to reduce strehl from 1% to 2 % when @ 60%
// settling on 256
// * changing optim_npt, the number of point on which the strehl is estimated
// for the minimization, does not change the result (tried 5, 11, 21,
// settling on 5).

// Authors, Francois Rigaut,  2010, for this file.
//          Damien Gratadour, 2008-2010.
//          Allan Brighton,   2011 Scala port


/**
 * Holds the results of the computations done by the Strehl object below.
 * Usage: val s = Strehl(starList)
 */
case class Strehl private (avgstrehl: Double,
             rmsstrehl: Double,
             minstrehl: Double,
             maxstrehl: Double,
             halffield: Double,
             strehl_map: DenseMatrix[Double],
             strehl_map_halffield: DenseMatrix[Double],
             tiperr: DenseMatrix[Double],
             tilterr: DenseMatrix[Double],
             stars: List[Star]) {

  def getStars : java.util.List[Star] = {
    stars.asJava
  }

  override def toString = "Strehl: avgstrehl=" + avgstrehl +
    "\nrmsstrehl = " + rmsstrehl +
    "\nminstrehl = " + minstrehl +
    "\nmaxstrehl = " + maxstrehl +
    "\nhalffield = " + halffield +
    "\nstrehl_map =  Matrix: " + strehl_map.rows + " x " + strehl_map.cols +
    "\nstrehl_map_halffield = Matrix: " + strehl_map_halffield.rows + " x " + strehl_map_halffield.cols +
    "\tiperr = Matrix: " + tiperr.rows + " x " + tiperr.cols +
    "\ntilterr = Matrix: " + tilterr.rows + " x " + tilterr.cols +
    "\nstars = " + stars
}

/**
 * Scala port of the Mascot/Yorick Strehl routines (mascot_strehl.i).
 */
object Strehl {

  val nmodes = 5
  val sp = nullModesSpectra()
  var spv = vibSpectra()
  val novibs = false

  //  func mascot_compute_strehl(void)
  ///* DOCUMENT mascot_compute_strehl(void)
  //   Main routine. originally from Damien Gratadour.
  //
  //   SEE ALSO:
  // */
  //  {
  //    ns = dimsof(starlist)(0);
  //
  //    gso = array(double,[2,2,ns]);
  //    mag = ra = dec = array(double,ns);
  //    for (i=1;i<=ns;i++) {
  //      gso(,i) = starlist(1:2,i);
  //      mag(i)  = starlist(5,i);
  //      ra(i)   = starlist(10,i);
  //      dec(i)  = starlist(11,i);
  //    }
  //
  //    sdata = mascot_optimize(gso,mag);
  //    sdata.starra  = &ra;
  //    sdata.stardec = &dec;
  //
  //    return sdata;
  //  }

  /**
   * Computes the strehl values for the given stars
   * @param starList a list of 1, 2 or 3 stars
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   */
  def apply(starList: List[Star], factor: Double = 1.0): Strehl = {
    optimize(starList, factor)
  }

  /**
   * Optimizes the modes & mode gains.
   * Computes the modal cmat, etc.
   * Call amoeba that maximizes a criteria (strehl avg/rms).
   * Compute final performance: Strehl map.
   *
   * @param starList a list of 1, 2 or 3 stars
   * @param bandpass determines which magnitudes are used in the calculations: (one of "B", "V", "R", "J", "H", "K")
   * @param factor multiply strehl min, max and average by this value (depends on instrument filter: See REL-426)
   */
  def optimize(starList: List[Star], factor: Double): Strehl = {
    val nstars = starList.size
    val mag = (for (s <- starList) yield s.r).toArray
    val starx = (for (s <- starList) yield s.x).toArray
    val stary = (for (s <- starList) yield s.y).toArray

    // first we recenter the asterism.
    // We do that because we want to keep the segregation between TT and
    // quadratic modes. If we didn't do that, in case we have for instance,
    // 3 stars almost aligned off-axis, then we will necessarily have
    // some TT in the mode with the lowest eigenvalue, which means
    // we will also have some quadratic in the first 2 modes. This has several
    // drawbacks: (1) the turbulence covariance matrix of these eigenmodes is
    // not diagonal anymore. So we have to take into account extra-diagonal terms.
    // (2) each mode being a mix between TT and quadratics, they are not ranked
    // from highest to lowest variance, thus the modal gain optimization will be
    // less efficient (I feel), and (3) it is more difficult to introduce
    // vibrations + windshake.
    // So, we recenter here, do our stuff, and then estimate the perf on a shifted
    // area.

    //  gso_off = gso(,avg);
    //  gso = gso - gso_off(,-);
    val gso_off = DenseVector(stary.sum / nstars, starx.sum / nstars)
//    val gso = DenseMatrix(stary - gso_off(0), starx - gso_off(1))
    val gso = DenseMatrix((DenseVector(stary) - gso_off(0)).toArray, (DenseVector(starx) - gso_off(1)).toArray)

    // compute imat:
    //  imat = array(0.,[2,6,nmodes]);
    //  for (mn=1;mn<=nmodes;mn++) {
    //    signal = [];
    //    for (i=1;i<=nstars;i++) grow,signal,create_distortion(mn,gso(1,i),gso(2,i));
    //    imat(1:2*nstars,mn) = signal;
    //  }

    val imat = DenseMatrix.zeros[Double](6, nmodes)
    for (mn <- 0 until nmodes) {
      val signal = for (i <- 0 until nstars) yield createDistortion(mn + 1, gso(0, i), gso(1, i))
      imat(0 until 2 * nstars, mn) := DenseVector(signal.flatMap(_.toArray).toArray)
    }

    // prepare imat inversion
    //  ev = SVdec(imat,u,vt);
    //  nmodes_cont = sum(ev!=0); // number of controlled modes
    //  mta = transpose(vt);      // modes to act transfer matrix

    val (u, ev, vt) = util.MatrixUtil.svd(imat)
    val nmodes_cont = ev.toArray.count(_ != 0.0) // number of controlled modes
    val mta = vt.t // modes to act transfer matrix

    // Build modal cmat:
    //  eem1 = unit(nmodes)*0.;
    //  for (i=1;i<=nmodes;i++) if (ev(i)>0) eem1(i,i) = 1./ev(i);
    //  mcmat = eem1(,+) * u(,+);

    val eem1 = DenseMatrix.zeros[Double](nmodes, nmodes)
    for (i <- 0 until nmodes) {
      if (ev(i) > 0) {
        eem1(i, i) = 1.0/ ev (i)
      }
    }
    val mcmat = eem1 * u.t

    // noise propagation
    //  nca = unit(6)*0.;
    //  ttnv = wfs_noise(mag,verbose=(debug!=0)); // noise per WFS in arcsec rms
    //  for (i=1;i<=nstars*2;i++) nca(i,i) = ttnv(i);
    //  mprop = u(+,) * nca(,+);
    //  mprop = eem1(,+) * mprop(+,);
    //  noise propagation matrix on eigenmodes:
    //  mprop = mprop(,+) * mprop(,+);
    // it is indeed covariance (diagonal = variance)

    val nca = DenseMatrix.eye[Double](6)
    val ttnv = wfsNoise(mag) // noise per WFS in arcsec rms
    for (i <- 0 until nstars * 2) nca(i, i) = ttnv(i)
    val mprop0 = eem1 * (u.t * nca.t)
    val mprop = mprop0 * mprop0.t

    // pre-compute distortion fields:
    //  dfields = array(0.,[4,nmodes,optim_npt,optim_npt,2]);
    //  // first index: mode #
    //  // second and third: spatial X and Y
    //  // fourth index: local distortion value (1:x or 2:y)
    //  // so that dfields(2,,,1)
    //  // is the X component of the distortion field created by 2nd mode.
    //  // this is a 2D array covering a field = 2*halffield with
    //  // optim_npt x optim_npt points.
    //  for (i=1;i<=nmodes;i++)
    //    dfields(i,,,) = get_distortion_vfield(mta(,i),optim_npt,halffield,xloc,yloc);

    // pre-compute distortion fields:
    val dfields = Array.ofDim[Array[DenseMatrix[Double]]](nmodes)
    for (i <- 0 until nmodes) {
      dfields(i) = getDistortionVfield(mta(::, i), optim_npt, halffield)
    }

    // call minimization routine. This should return the best gains
    //  bg = amoeba(0.01, get_strehl_map,nc, fval, nMax=1000,
    //              p0=-0.7*array(1.,nmodes_cont),scale=0.2);
    val ftol = 0.01
    // Porting Note: make a partially applied function for getStrehlMap containing any formerly external values
    val f = getStrehlMap2(mprop, dfields, nmodes_cont, _: DenseVector[Double])
    val nMax = 1000
    val p0 = DenseVector.ones[Double](nmodes_cont) * -0.7
    val scale = 0.2
    val (bg, nc, fval) = amoeba(ftol, f, nMax, p0, scale)

    // now compute the final Strehl map given the gains we just found.
    // pre-compute distortion fields:
    //  dfields = array(0.,[4,nmodes,smap_npt,smap_npt,2]);
    //  for (i=1;i<=nmodes;i++)
    //    dfields(i,,,) = get_distortion_vfield(mta(,i),smap_npt,halffield,\
    //                                          xloc,yloc,offset=gso_off);

    for (i <- 0 until nmodes) {
      dfields(i) = getDistortionVfield(mta(::, i), smap_npt, halffield, gso_off)
    }

    // get the strehl map:
    //  get_strehl_map,bg,smap,tiperr,tilterr;
    //  sdata.avgstrehl = avg(smap);
    //  sdata.rmsstrehl = (smap)(*)(rms);
    //  sdata.minstrehl = min(smap);
    //  sdata.maxstrehl = max(smap);
    //  sdata.strehl_map_halffield = &smap;

    val (ret, smap, tiperr, tilterr) = getStrehlMap(mprop, dfields, nmodes_cont, bg)

    val avgstrehl = avg(smap) * factor
    val rmsstrehl = rms(smap)
    val minstrehl = smap.min * factor
    val maxstrehl = smap.max * factor
    val strehl_map_halffield = smap

    //  write,format="Strehl over %.1f\": avg=%.1f  rms=%.1f  min=%.1f  max=%.1f\n",
    //    halffield*2, sdata.avgstrehl*100, sdata.rmsstrehl*100,
    //    sdata.minstrehl*100, sdata.maxstrehl*100;

    //    println("Strehl over %.1f\": avg=%.1f  rms=%.1f  min=%.1f  max=%.1f\n" format (
    //      halffield * 2, (avgstrehl * 100), (rmsstrehl * 100), (minstrehl * 100), (maxstrehl * 100)))

    // Finaly compute the final strehl map, but now over the whole 60"x60" FoV
    // pre-compute distortion fields:
    //  dfields = array(0.,[4,nmodes,smap_npt,smap_npt,2]);
    //  for (i=1;i<=nmodes;i++)
    //    dfields(i,,,) = get_distortion_vfield(mta(,i),smap_npt,60.,\
    //                                          xloc,yloc,offset=gso_off);
    //
    //  // get the strehl map:
    //  get_strehl_map,bg,smap,tiperr,tilterr,stop=(debug>10);
    //  sdata.strehl_map = &smap;
    //  sdata.tiperr     = &tiperr;
    //  sdata.tilterr    = &tilterr;

    for (i <- 0 until nmodes) {
      dfields(i) = getDistortionVfield(mta(::, i), smap_npt, 60.0, gso_off)
    }

    // get the strehl map:
    val (ret2, strehl_map, tiperr2, tilterr2) = getStrehlMap(mprop, dfields, nmodes_cont, bg)

    Strehl(avgstrehl, rmsstrehl, minstrehl, maxstrehl, halffield, strehl_map, strehl_map_halffield,
      tiperr2, tilterr2, starList)
  }


  //func get_strehl_map(lgains,&strehl,&tiperr,&tilterr,stop=)
  ///* DOCUMENT get_strehl_map(lgains,&strehlmap,&tiperr,&tilterr,stop=)
  //   Given a noise propagation matrix mprop (passed in extern) and
  //   the modes distortion vector fields dfields (passed in extern),
  //   compute a strehl map for the mode gains lgains (input). Take
  //   into account the turbulence residuals and the propagated noise.
  //   This function is normally called by amoeba (find best gains
  //   by minimizing a criteria), but for convenience, can also be
  //   called directly, in which case it returns the strehl maps
  //   and the tip/tilt errors.
  //   SEE ALSO:
  // */
  // Porting Note: Instead of externals, we use leading parameters and
  // then pass this function to amoeba as a partially applied function.
  // Instead of return parameters, a tuple is returned: (ret, strehl, tiperr, tilterr),
  // where ret is the original return value.
  def getStrehlMap(mprop: DenseMatrix[Double],
                   dfields: Array[Array[DenseMatrix[Double]]],
                   nmodes_cont: Int,
                   lgains: DenseVector[Double])
  : (Double, DenseMatrix[Double], DenseMatrix[Double], DenseMatrix[Double]) = {

    //    val xxx = new Date().getTime()

    //  turb = nois = g = array(0.,nmodes);
    //  g(1:nmodes_cont) = 10.^lgains;
    //  freq  = sp(,1);

    val turb = DenseVector.zeros[Double](nmodes)
    val nois0 = DenseVector.zeros[Double](nmodes)
    val g = DenseVector.zeros[Double](nmodes)
    g(0 until nmodes_cont) := util.YUtils.pow(10.0, lgains)
    val freq = sp(::, 0)

    // limits upper freq range for spline:
    //  if (max(spv(,1))>sampfreq) {
    //    w = where(spv(,1)<sampfreq)(0);
    //    spv = spv(1:w,);
    //  }
    //  freqv = spv(,1);
    //
    //  rmsvib = array(0.,2);
    //  rmsvib(1) = sum((*tipvibrms)^2.);
    //  rmsvib(2) = sum((*tiltvibrms)^2.);


    if (spv(::, 0).max > sampfreq) {
      val tmp = where(spv(::, 0), _ < sampfreq)
      val w = tmp(tmp.length - 1)
      spv = spv(0 to w, ::)
    }
    val freqv = spv(::, 0)

    val rmsvib = DenseVector.zeros[Double](2)
    rmsvib(0) = (tipvibrms :^ 2.0).sum
    rmsvib(1) = (tiltvibrms :^ 2.0).sum


    // compute transfer functions for said gains.
    //  for (i=1;i<=nmodes;i++) {
    //    npt = 512;
    //    hs      = array(0.,[2,npt+1,4]);
    //    hs(2:,) = ftcb(1./sampfreq,0.3e-3,2e-3,g(i),npt);
    //    // add missing values for freq=0
    //    if (g(i)==0) hs(1,) = [0.,0.,1.,0.];
    //    else hs(1,) = [0.,0.,0.,1.];
    //    // above these are to be applied on PSD.
    //    // computed in ftcb as h*conj(h), so OK.
    //    // servolag for turb + vibrations
    //    herror = clip(spline(hs(,3),hs(,1),freq),0,);
    //    turb(i) = sum( rmsmodes(i)^2 * sp(,i+1) * herror );
    //    if (i<=2) {
    //      // vibrations + windshake:
    //      // NO ! we can't be sure 2 first modes are Tip and Tilt.
    //      // let's add this separately. TO BE DONE.
    //      // YES. now with re-centered asterism, we have clean
    //      // TT isolation as modes 1 and 2. DONE.
    //      herror = clip(spline(hs(,3),hs(,1),freqv),0,);
    //      if (!novibs) {
    //        turb(i) += sum( rmsvib(i)^2. * spv(,i+1) * herror );
    //      }
    //    }
    //    // noise
    //    hnoise = hs(,4)/npt;
    //    nois(i) = sum( hnoise );
    //  }
    //  nois = nois * (g>0.); // just to make sure nois=0 if gain=0
    //
    //  // servolag (variance):
    //  // turb_var = turb * rmsmodes^2.; // compensated rms / mode
    //  turb_var = turb; // compensated rms / mode

    for (i <- 0 until nmodes) {
      val npt = 512
      val hs = DenseMatrix.zeros[Double](npt + 1, 4)
      hs(1 until hs.rows, ::) := ftcb(1.0 / sampfreq, 0.3e-3, 2e-3, g(i), npt)
      // add missing values for freq=0
      hs(0, ::) := (if (g(i) == 0.0) DenseVector(0.0, 0.0, 1.0, 0.0) else DenseVector(0.0, 0.0, 0.0, 1.0))
      // above these are to be applied on PSD.
      // computed in ftcb as h*conj(h), so OK.
      // servolag for turb + vibrations

      //      val xxx2 = new Date().getTime()
      val herror = splineMax(hs(::, 2), hs(::, 0), freq, 0.0)
      turb(i) = ((sp(::, i + 1) * math.pow(rmsmodes(i), 2.0)) :* herror).sum
      if (i <= 1) {
        // vibrations + windshake:
        // NO ! we can't be sure 2 first modes are Tip and Tilt.
        // let's add this separately. TO BE DONE.
        // YES. now with re-centered asterism, we have clean
        // TT isolation as modes 1 and 2. DONE.
        val herror2 = splineMax(hs(::, 2), hs(::, 0), freqv, 0.0)
        if (!novibs) {
          turb(i) += ((spv(::, i + 1) * math.pow(rmsvib(i), 2.0)) :* herror2).sum
        }
      }
      //println("XXX spline: " + ((new Date().getTime() - xxx2)/1000.) + " sec")

      // noise
      val hnoise = hs(::, 3) / (npt * 1.0)
      nois0(i) = hnoise.sum
    }
    val nois = nois0 :* g.mapValues(a => if (a > 0.0) 1.0 else 0.0)


    // servolag (variance):
    val turb_var = turb; // compensated rms / mode

    // noise (variance)
    //  nois_cov = (nois(-,))*mprop*(nois(,-));

    val nois_cov = yMultiply(nois, yMultiply(nois, mprop).t)

    // map of tt error in field of view, turbulence residuals:
    //  tiperr  = turb_var(+) * (dfields^2.)(+,,,1);
    //  tilterr = turb_var(+) * (dfields^2.)(+,,,2);

    val dfields2 = util.YUtils.pow(dfields, 2.0)
    var tiperr = yMultiply4d(turb_var, dfields2, 0)
    var tilterr = yMultiply4d(turb_var, dfields2, 1)


    // noise contribution, covariance:
    //  for (i=1;i<=nmodes_cont;i++) {
    //    for (j=1;j<=nmodes_cont;j++) {
    //      tiperr  += nois_cov(i,j) * dfields(i,,,1) * dfields(j,,,1);
    //      tilterr += nois_cov(i,j) * dfields(i,,,2) * dfields(j,,,2);
    //    }
    //  }
    //

    for (i <- 0 until nmodes_cont; j <- 0 until nmodes_cont) {
      //      tiperr += yGet4d(dfields, i, 0) :* yGet4d(dfields, j, 0) :* nois_cov(i, j);
      //      tilterr += yGet4d(dfields, i, 1) :* yGet4d(dfields, j, 1) :* nois_cov(i, j);
      tiperr += dfields(i)(0) :* dfields(j)(0) :* nois_cov(i, j)
      tilterr += dfields(i)(1) :* dfields(j)(1) :* nois_cov(i, j)
    }

    //  tiperr  = sqrt(tiperr); // in arcsec
    //  tilterr = sqrt(tilterr);
    //
    //  tiperr_rd = tiperr*4.848e-6*tel_diam*2*pi/(lambdaim*1e-6)/4.;
    //  tilterr_rd = tilterr*4.848e-6*tel_diam*2*pi/(lambdaim*1e-6)/4.;


    tiperr = sqrt(tiperr); // in arcsec
    tilterr = sqrt(tilterr)

    val tiperr_rd = tiperr * 4.848e-6 * tel_diam * 2.0 * math.Pi / (lambdaim * 1e-6) / 4.0
    val tilterr_rd = tilterr * 4.848e-6 * tel_diam * 2.0 * math.Pi / (lambdaim * 1e-6) / 4.0

    // see strehl_vs_ttrms() below, this is it, fairly good approximation:
    //    strehl = sqrt(1./(1.+2.*tiperr_rd^2.))*sqrt(1./(1.+2.*tilterr_rd^2.));

    val strehl = sqrt(divide(1.0, (tiperr_rd :^ 2.0) * 2.0 + 1.0)) :* sqrt(divide(1.0, (tilterr_rd :^ 2.0) * 2.0 + 1.0))

    //  if (stop) error;
    //
    //  return -log(avg(strehl));

    val ret = -math.log(avg(strehl))
    //    println("XXX getStrehlMap: " + ((new Date().getTime() - xxx)/1000.) + " sec")

    // Porting note: return value includes the output parameters from the yorick version (which only returned ret)
    (ret, strehl, tiperr, tilterr)
  }

  /**
   * This function is passed to amoeba instead of the above. It returns just the one double value, like the original
   * Yorick version.
   */
  def getStrehlMap2(mprop: DenseMatrix[Double], dfields: Array[Array[DenseMatrix[Double]]], nmodes_cont: Int,
                    lgains: DenseVector[Double]): Double = {
    getStrehlMap(mprop, dfields, nmodes_cont, lgains)._1
  }


  //  Returns local distortion at (x,y) for mode N in arcsec
  //
  //  mn = mode n (out of 5)
  //  x and y = coordinates in the fov (say in arcsec). can be arrays.
  //  Modes normalized so that modes 1 & 2 (TT) return always 1
  //  Modes 3,4,5 return 1 for a distance of 100 arcsec for mode3.
  //  4 and 5 normalized as 3 (that is considering quadratic have same
  //  variance in turb and taking into account Z4,5,6 norm factor).
  //
  //  if (mn==1) return [x*0+1.,x*0.];
  //  else if (mn==2) return [x*0.,x*0+1.];
  //  else if (mn==3) return [x,y]/100.;
  //  else if (mn==4) return 2*sqrt(6)/4/sqrt(3.)*[y,x]/100.;
  //  else if (mn==5) return 2*sqrt(6)/4/sqrt(3.)*[x,-y]/100.;
  //  else error,"mn out of range";
  def createDistortion(mn: Int, x: Double, y: Double): DenseVector[Double] = {
    mn match {
      case 1 => DenseVector(x * 0 + 1.0, x * 0.0)
      case 2 => DenseVector(x * 0.0, x * 0 + 1.0)
      case 3 => DenseVector(x / 100.0, y / 100.0)
      case 4 => DenseVector(y, x) / 100.0 * (2.0 * math.sqrt(6.0) / 4.0 / math.sqrt(3.0))
      case 5 => DenseVector(x, -y) / 100.0 * (2.0 * math.sqrt(6.0) / 4.0 / math.sqrt(3.0))
      case _ => throw new IllegalArgumentException("Expected 1 to 5")
    }
  }

  /**
   * A version of createDistortion() where x and y are type Matrix
   * (In the Yorick version, one function worked for any number of dimensions)
   */
  def createDistortion(mn: Int, x: DenseMatrix[Double], y: DenseMatrix[Double]): Array[DenseMatrix[Double]] = {
    mn match {
      case 1 => Array(x * 0.0 + 1.0, x * 0.0)
      case 2 => Array(x * 0.0, x * 0.0 + 1.0)
      case 3 => Array(x / 100.0, y / 100.0)
      case 4 =>
        val tmp = (2.0 * math.sqrt(6.0) / 4.0 / math.sqrt(3.0)) / 100.0
        Array(y * tmp, x * tmp)
      case 5 =>
        val tmp = (2.0 * math.sqrt(6.0) / 4.0 / math.sqrt(3.0)) / 100.0
        Array(x * tmp, y * -tmp)
      case _ => throw new IllegalArgumentException("Expected 1 to 5")
    }
  }


  // DOCUMENT get_distortion_vfield(mnv,npt,halffield,&xloc,&yloc,offset=,x=,y=,plot=)
  //
  //   Return a distortion vector field (from modes with input coefficients) at
  //   given points in the field of view.
  //
  //   mnv = vector of nmodes mode coefficients, e.g. [1,2,0,0,0]
  //   x & y = specific coordinates at which to compute the distortions
  //   if x is not set, then this routine returns the distortion vector field
  //   at points defined on a square grid, with npt x npt points covering
  //   a field of view = 2 * halffield.
  //   if square grid is requested, xloc & yloc on ouput are the grid location
  //   at which the distortion were computed.
  //   plot= as it says. Plots the resulting vector field.
  // */
  def getDistortionVfield(mnv: DenseVector[Double], npt: Int, halffield: Double, offset: DenseVector[Double] = DenseVector(0.0, 0.0)): Array[DenseMatrix[Double]] = {
    //  if (x==[]) square=1; // no specific coordinates are supplied (x & y)
    //
    //  if (offset==[]) offset=[0.,0.];
    //
    //  if (square) { // compute grid locations, covering [-halffield,halffield]
    //    xy = indices(npt)-(npt+1)/2.;
    //    xy = xy/max(xy)*halffield-offset(-,-,);
    //    x = xloc = xy(,,1);
    //    y = yloc = xy(,,2);
    //  }
    //
    //  // get distortion vector field for first mode.
    //  d = mnv(1)*create_distortion(1,x,y);
    //
    //  // add other modes.
    //  for (i=2;i<=numberof(mnv);i++) d += mnv(i)*create_distortion(i,x,y);
    //
    //  // possible plot if requested.
    //  if (square && plot) {
    //    fma;
    //    plvf,d(*,2),d(*,1),xy(*,2),xy(*,1),autoscale=1;
    //    plmargin;
    //  }
    //
    //  return d;

    // Porting notes:
    // 1. Assume square for now, since x and y are never specified in Mascot code.
    // 2. Since scalala does not support math on multidimensional arrays, we use an array of matrix here (see xy)
    // 3. xloc, yloc are not used in Mascot code either, so they are ignored here
    // 4. No plotting is done in this version

    // compute grid locations, covering [-halffield,halffield]

    val xy = indices(npt)
    val tmp = (npt + 1) / 2.0
    xy(0) -= tmp
    xy(1) -= tmp

    xy(0) = xy(0) / xy(0).max * halffield - offset(0)
    xy(1) = xy(1) / xy(1).max * halffield - offset(1)
    val x = xy(0)
    val y = xy(1)

    // get distortion vector field for first mode.
    val d = createDistortion(1, x, y)
    d(0) *= mnv(0)
    d(1) *= mnv(0)

    // add other modes.
    for (i <- 1 until mnv.size) {
      val dist = createDistortion(i + 1, x, y)
      d(0) += (dist(0) * mnv(i))
      d(1) += (dist(1) * mnv(i))
    }

    d
  }
}
