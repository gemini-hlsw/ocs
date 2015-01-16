package edu.gemini.ags.gems.mascot

import MascotConf._
import util.Spline._
import util.YUtils
import util.YUtils._
import scala.math
import nom.tam.fits.{ImageData, Fits}
import java.io.IOException
import breeze.linalg._
import edu.gemini.ags.gems.mascot.util.HotSpots

/**
 * Ported from the Yorick version, mascot_utils.i
 */
object MascotUtils {

  // Returns an array whose elements contain the distance to (xc,yc).
  def dist(dim: Int, xc: Double, yc: Double): DenseMatrix[Double] = {
    //  x	= float(span(1,dim,dim)(,-:1:dim));
    val x = DenseMatrix.tabulate(dim, dim)((i, j) => i + 1.0)
    //  y	= transpose(x);
    val y = x.t
    //  d	= float(sqrt((x-xc)^2.+(y-yc)^2.));
    val d = ((x - xc) :^ 2.0) + ((y - yc) :^ 2.0) :^ 0.5
    //  d	= clip(d,1e-5,);
    //  return d;

    util.YUtils.max(d, 1e-5)
  }

  /* DOCUMENT indices(dim)
  * Return a dimxdimx2 array. First plane is the X indices of the pixels
  * in the dimxdim array. Second plane contains the Y indices.
  * Inspired by the Python scipy routine of the same name.
  * dim is a single number N (e.g. 128) and the returned array are square (NxN)
  * F.Rigaut 2002/04/03
  */
  def indices(dim: Int): Array[DenseMatrix[Double]] = {
    //    x  = span(1,dim,dim)(,-:1:dim);
    //    y  = transpose(x);
    //    return [x,y];
    val x = DenseMatrix.tabulate[Double](dim, dim)((i, j) => i + 1.0)
    val y = x.copy.t
    Array(x, y)
  }


  // Returns true if the 3 (or at least 2) positions can be used
  def doesItFit(n1: Star, n2: Star, n3: Option[Star] = None): Boolean = {
    n3.map{ v3 =>
            //  d = array(0.,[3,300,300,nstars]);
      //  for (ns=1;ns<=3;ns++) {
      //    d(,,ns) = dist(300,xc=150+slist(1,ns),yc=150+slist(2,ns));
      //  }
      //  dmin = min(d(,,max));
      //  if (dmin<=(60-edge_margin)) return 1;
      //  else return 0;
      val size = 300
      val r = size / 2
      val d1 = dist(size, r + n1.y, r + n1.x)
      val d2 = dist(size, r + n2.y, r + n2.x)
      val d3 = dist(size, r + v3.y, r + v3.x)
      val dmin = util.YUtils.max(util.YUtils.max(d1, d2), d3).min

      dmin < 60 - edge_margin
    }.getOrElse {
      //      d = slist(1:2,1)-slist(1:2,2);
      //      d = sqrt(sum(d^2.));
      val d = math.sqrt((DenseVector(n1.y - n2.y, n1.x - n2.x) :^ 2.0).sum)
      //      if (d<=(120-2*edge_margin)) return 1;
      //      else return 0;
      d <= (120 - 2 * edge_margin)
    }
  }


  //   Returns the noise on the TT sensors in arcsec rms.
  def wfsNoise(mag: Array[Double]): DenseVector[Double] = {
    //    nstars = numberof(mag);
    //    ttnpde  = array(0.,nstars);
    //    for (i=1;i<=numberof(mag);i++) ttnpde(i) = magstar(mag(i));
    //    ttnpde *= detqe*thrup/sampfreq;

    val nstars = mag.size
    val ttnpde = DenseVector.zeros[Double](nstars)
    for (i <- 0 until nstars) {
      ttnpde(i) = magstar(mag(i)) * (detqe * thrup / sampfreq)
    }

    //    ttnsky  = (magsky())(case_sky)*detqe*thrup/sampfreq/4.; // 4-> per channel
    //    ttdc    = dark_current/sampfreq;
    //    ttsnr   = ttnpde/sqrt(ttnpde+4*ttron^2.+4*ttnsky+4.*ttdc);
    //    if (correct_for_poisson) {
    //      ttsnr = ttsnr/(1.+(0.7+0.4*sqrt(ttnsky+ttdc))/ttnpde)
    //    }

    // 4-> per channel
    val ttnsky = magsky()(case_sky - 1) * (detqe * thrup / sampfreq / 4.0)
    val ttdc = dark_current / sampfreq
    val ttsnr_0 = ttnpde :/ sqrt(ttnpde + 4 * math.pow(ttron, 2.0) + 4.0 * ttnsky + 4.0 * ttdc)
    val ttsnr = if (correct_for_poisson) {
      ttsnr_0 :/ (divide(0.7 + 0.4 * math.sqrt(ttnsky + ttdc), ttnpde) + 1.0)
    } else {
      ttsnr_0
    }


    // FIX ME ! don't know what gain FwhmCloseLoop is ...
    //    gainfwhmcloseloop = 1.; // let's be conservative and assume no gain !
    //    r0wfs = r0vis * (lambdawfs/0.5)^1.2;
    //

    val gainfwhmcloseloop = 1.0; // let's be conservative and assume no gain !
    val r0wfs = r0vis * math.pow(lambdawfs / 0.5, 1.2)

    // rms TT noise in as on WFSs: the 0.533 is for the CG (CG=FWHM*0.533)
    //    ttnoise= 0.533*(lambdawfs*1.0e-6/r0wfs/gainfwhmcloseloop/4.848e-6)/ttsnr;
    //
    //    if (verbose!=0) {
    //      write,format="%s","TT npde[ph/frame] = "; write,ttnpde;
    //      write,format="TT Nsky[ph/channel/frame] = %.2f  Dark/channel/frame = %.2f\n",ttnsky,ttdc;
    //      write,format="%s","TT noise[as] = "; write,ttnoise;
    //    }
    //
    //    ttnv = [];
    //    for (i=1;i<=nstars;i++) grow,ttnv,_(ttnoise(i),ttnoise(i));
    //    return ttnv; // 2 * nstars vector, sqrt(diagonal of noise covar mat).

    val ttnoise = divide(0.533 * (lambdawfs * 1.0e-6 / r0wfs / gainfwhmcloseloop / 4.848e-6), ttsnr)

//    if (verbose) {
//      println("TT npde[ph/frame] = " + yFormat(ttnpde))
//      println("TT Nsky[ph/channel/frame] = " + ttnsky + " Dark/channel/frame = " + ttdc)
//      println("TT noise[as] = " + yFormat(ttnoise));
//    }

    // 2 * nstars vector, sqrt(diagonal of noise covar mat).
    val ttnv = DenseVector.zeros[Double](nstars * 2)
    for (i <- 0 until nstars) {
      ttnv(i * 2) = ttnoise(i)
      ttnv(i * 2 + 1) = ttnoise(i)
    }
    ttnv
  }


  /**
   * Returns a matrix, given an array of arrays of floats
   * (Replacement fro scalala RichArrayMatrix)
   */
  def matrixFromArray(data: Array[Array[Float]]): DenseMatrix[Double] = {
    // Need to convert float array to double
    new DenseMatrix(data(0).length, data.flatten.map(_ * 1.0))
  }

  /**
   * Reads the data from the primary HDU of the named FITS file resource
   * and returns a matrix for it. (The file must be a resource on the classpath.)
   */
  def fitsRead(name: String): DenseMatrix[Double] = {
    val in = getClass.getResourceAsStream(name)
    if (in == null) throw new IOException("Could not find resource: " + name)
    val fits = new Fits(in)
    val hdu = fits.read()(0)
    in.close()
    val data = hdu.getData.asInstanceOf[ImageData]
    // transpose result to get correct order and multiply by 1.0 to get a double result
    // (FITS data is in column order)
//    new RichArrayMatrix(data.getData.asInstanceOf[Array[Array[Float]]]).asMatrix.t * 1.0
    matrixFromArray(data.getData.asInstanceOf[Array[Array[Float]]])
  }


  //func vib_spectra(void)
  // Porting Note: This could also just be read from the resource FITS file that is used for testing
  def vibSpectra(): DenseMatrix[Double] = {
    //  spv = array(0.,[2,4000,6]);
    //  spv(,1) = freq = span(0.,1000.,4000);
    //
    //  for (i=1;i<=numberof(*tipvibfreq);i++) {
    //    spv(,2) += ((*tipvibrms)(i))*                                       \
    //      exp(-((freq-(*tipvibfreq)(i))/((*tipvibwidth)(i)/1.66))^2.)^2.;
    //  }
    //  spv(,2) = spv(,2)/sum(spv(,2));
    //
    //  for (i=1;i<=numberof(*tiltvibfreq);i++) {
    //    spv(,3) += ((*tiltvibrms)(i))*                                       \
    //     exp(-((freq-(*tiltvibfreq)(i))/((*tiltvibwidth)(i)/1.66))^2.)^2.;
    //  }
    //  spv(,3) = spv(,3)/sum(spv(,3));
    //
    //  return spv;


    val spv = DenseMatrix.zeros[Double](4000, 6)
    val freq = span(0.0, 1000.0, 4000)
    spv(::, 0) := freq

    for (i <- 0 until tipvibfreq.size) {
      spv(::, 1) :+= (exp((((freq - tipvibfreq(i)) / (tipvibwidth(i) / 1.66)) :^ 2.0) * -1.0) :^ 2.0) * tipvibrms(i)
    }
    spv(::, 1) := spv(::, 1) :/ spv(::, 1).sum

    for (i <- 0 until tiltvibfreq.size) {
      spv(::, 2) :+= (exp((((freq - tiltvibfreq(i)) / (tiltvibwidth(i) / 1.66)) :^ 2.0) * -1.0) :^ 2.0) * tiltvibrms(i)
    }
    spv(::, 2) := spv(::, 2) :/ spv(::, 2).sum

    spv
  }

  def vibSpectraCached(): DenseMatrix[Double] = {
      fitsRead("vib_spectra.fits")
  }


  //func null_modes_spectra(void)
  // Porting Note: This could also just be read from the resource FITS file used for testing,
  // instead of reading in zernike_spectra.fits and then doing the calculations
  def nullModesSpectra(): DenseMatrix[Double] = {
    //  sp = array(0.,[2,4000,6]);
    //  if (fileExist("zernike_spectra.fits")) {
    //      write,"Reading zernike_spectra.fits";
    //      a = fits_read("zernike_spectra.fits");
    //      sp(1:numberof(a(,1)),) = a;
    //  } else {
    //    zernike_spectra,2,x2,y2;
    //    a=array(0.,[2,numberof(x2),6]);
    //    a(,1)=x2; a(,2)=y2;
    //    zernike_spectra,3,x3,y3;
    //    a(,3)=y3;
    //    zernike_spectra,4,x4,y4;
    //    a(,4)=y4;
    //    zernike_spectra,5,x5,y5;
    //    a(,5)=y5;
    //    zernike_spectra,6,x6,y6;
    //    a(,6)=y6;
    //    fitsWrite,"zernike_spectra.fits",a;
    //    sp(1:numberof(x2),) = fits_read("zernike_spectra.fits");
    //  }

    // Porting note: Assume the file exists (we can port the zernike_spectra function later if needed)
    val sp = DenseMatrix.zeros[Double](4000, 6)
    val a = fitsRead("zernike_spectra.fits")
    sp(0 until a.rows, ::) := a

    // read out zernike spectra
    //  sp(1:,1)      = double(indgen(4000)-1.)*(sp(3,1)-sp(2,1))+sp(1,1);
    //  //fill in frequency vector
    //  freq          = sp(,1);
    //  dfreq         = (sp(3,1)-sp(2,1));
    //
    //  rtel          = tel_diam/2.;
    //  dr0           = tel_diam/(r0vis*(lambdawfs/0.5)^1.2);
    //  cn2           = cn2/sum(cn2);
    //  dr0i          = (cn2*dr0^(5./3.))^(3./5.);
    //  nlayers       = numberof(cn2);
    //
    //  b = array(0.,[2,4000,6]);
    //  b(,1) = freq;


//    sp(::, 0) := Vector.range(0, 4000) * (sp(2, 0) - sp(1, 0)) + sp(0, 0);
    sp(::, 0) := DenseVector.tabulate(4000)(_ * (sp(2, 0) - sp(1, 0))) :+ sp(0, 0)

    //fill in frequency vector
    val freq = sp(::, 0)
    val dfreq = sp(2, 0) - sp(1, 0)

    val rtel = tel_diam / 2.0
    val dr0 = tel_diam / (r0vis * math.pow(lambdawfs / 0.5,  1.2))
    val cn2_2 = cn2 / cn2.sum
    val dr0i = (cn2_2 :* math.pow(dr0, 5.0 / 3.0)) :^ (3.0 / 5.0)
    val nlayers = cn2_2.size

    val b = DenseMatrix.zeros[Double](4000, 6)
    b(::, 0) := freq

    //Tip:
    //  sp2           = sp(,1)*0.;
    //  x2 = a(,1);
    //  dfreqinit = x2(3)-x2(2);
    //  y2 = a(,2)/(sum(a(,2))*dfreqinit);
    //  for (i=1;i<=nlayers;i++) {
    //    tmp         = spline(x2*y2,x2,freq/(wind(i)/rtel))/freq;
    //    tmp         = tmp/(sum(tmp)*dfreq);
    //    sp2         = sp2+tmp*0.45*dr0i(i)^(5./3.);
    //  }
    //  b(,2) = sp2;

    val sp2 = DenseVector.zeros[Double](sp.rows)
    val x2 = a(::, 0)
    val dfreqinit = x2(2) - x2(1)
    val a1 = a(::, 1)
    val y2 = a1 / (a1.sum * dfreqinit)
    for (i <- 0 until nlayers) {
      val tmp = spline(x2 :* y2, x2, freq / (wind(i) / rtel)) :/ freq
      val tmp2 = tmp / (tmp.sum * dfreq)
      sp2 += tmp2 * 0.45 * math.pow(dr0i(i), 5.0 / 3.0)
    }
    b(::, 1) := sp2

    //Tilt:
    //  sp3           = sp(,1)*0.;
    //  y3 = a(,3)/(sum(a(,3))*dfreqinit);
    //  for (i=1;i<=nlayers;i++) {
    //    tmp         = spline(x2*y3,x2,freq/(wind(i)/rtel))/freq;
    //    tmp         = tmp/(sum(tmp)*dfreq);
    //    sp3         = sp3+tmp*0.45*dr0i(i)^(5./3.);
    //  }
    //  b(,3) = sp3;

    val sp3 = DenseVector.zeros[Double](sp.rows)
    val a2 = a(::, 2)
    val y3 = a2 / (a2.sum * dfreqinit)
    for (i <- 0 until nlayers) {
      val tmp = spline(x2 :* y3, x2, freq / (wind(i) / rtel)) :/ freq
      val tmp2 = tmp / (tmp.sum * dfreq)
      sp3 += tmp2 * 0.45 * math.pow(dr0i(i), 5.0 / 3.0)
    }
    b(::, 2) := sp3

    //focus:
    //  sp4           = sp(,1)*0.;
    //  y4 = a(,4)/(sum(a(,4))*dfreqinit);
    //  for (i=2;i<=nlayers;i++) {
    //    tmp         = spline(x2*y4,x2,freq/(wind(i)/rtel))/freq;
    //    tmp         = tmp/(sum(tmp)*dfreq);
    //    sp4         = sp4+tmp*alt(i)^2.*0.02332*dr0i(i)^(5./3.);
    //  }
    //  b(,4) = sp4;

    val sp4 = DenseVector.zeros[Double](sp.rows)
    val a3 = a(::, 3)
    val y4 = a3 / (a3.sum * dfreqinit)
    for (i <- 1 until nlayers) {
      val tmp = spline(x2 :* y4, x2, freq / (wind(i) / rtel)) :/ freq
      val tmp2 = tmp / (tmp.sum * dfreq)
      sp4 += tmp2 * math.pow(alt(i), 2.0) * 0.02332 * math.pow(dr0i(i), 5.0 / 3.0)
    }
    b(::, 3) := sp4

    //astig:
    //  sp5           = sp(,1)*0.;
    //  y5 = a(,5)/(sum(a(,5))*dfreqinit);
    //  for (i=2;i<=nlayers;i++) {
    //    tmp         = spline(x2*y5,x2,freq/(wind(i)/rtel))/freq;
    //    tmp         = tmp/(sum(tmp)*dfreq);
    //    sp5         = sp5+tmp*alt(i)^2.*0.02332*dr0i(i)^(5./3.);
    //  }
    //  b(,5) = sp5;

    val sp5 = DenseVector.zeros[Double](sp.rows)
    val a4 = a(::, 4)
    val y5 = a4 / (a4.sum * dfreqinit)
    for (i <- 1 until nlayers) {
      val tmp = spline(x2 :* y5, x2, freq / (wind(i) / rtel)) :/ freq
      val tmp2 = tmp / (tmp.sum * dfreq)
      sp5 += tmp2 * math.pow(alt(i), 2.0) * 0.02332 * math.pow(dr0i(i), 5.0 / 3.0)
    }
    b(::, 4) := sp5

    //astig:
    //  sp6           = sp(,1)*0.;
    //  y6 = a(,6)/(sum(a(,6))*dfreqinit);
    //  for (i=2;i<=nlayers;i++) {
    //    tmp         = spline(x2*y6,x2,freq/(wind(i)/rtel))/freq;
    //    tmp         = tmp/(sum(tmp)*dfreq);
    //    sp6         = sp6+tmp*alt(i)^2.*0.02332*dr0i(i)^(5./3.);
    //  }
    //  b(,6) = sp6;

    val sp6 = DenseVector.zeros[Double](sp.rows)
    val a5 = a(::, 5)
    val y6 = a5 / (a5.sum * dfreqinit)
    for (i <- 1 until nlayers) {
      val tmp = spline(x2 :* y6, x2, freq / (wind(i) / rtel)) :/ freq
      val tmp2 = tmp / (tmp.sum * dfreq)
      sp6 += tmp2 * math.pow(alt(i), 2.0) * 0.02332 * math.pow(dr0i(i), 5.0 / 3.0)
    }
    b(::, 5) := sp6

    //  sp = b;
    //  b  = [];
    //
    //  ttsp = sp(,2:3)(,avg); ttsp /= sum(ttsp);
    //  tasp = sp(,4:6)(,avg); tasp /= sum(tasp);
    //  sp(,2:3) = ttsp(,-);
    //  sp(,4:6) = tasp(,-);
    //
    //  return sp;

    // Porting note: using b instead of sp to avoid reassign to val sp
    val ttsp = rowAvg(b(::, 1 to 2))
    ttsp :/= ttsp.sum
    val tasp = rowAvg(b(::, 3 to 5))
    tasp :/= tasp.sum
    b(::, 1) := ttsp
    b(::, 2) := ttsp
    b(::, 3) := tasp
    b(::, 4) := tasp
    b(::, 5) := tasp

    b
  }

  // Reads the spectra from a FITS file instead of calculating it
  def nullModesSpectraCached(): DenseMatrix[Double] = {
    fitsRead("null_modes_spectra.fits")
  }


  def magsky(): DenseVector[Double] = {
    //  lambda   = [3.5,4, 5, 6, 7, 8, 9,10,11]*1e-7;     // meters
    //  qe       = [0., 5,30,37,37,32,23,10,0.]*0.01*1.6; // percent
    //  h        = 6.62e-34;
    //  c        = 3e8;
    //
    //  // for band u,b,v,r,i
    //  cw       = [365,440,550,700,900]*1e-9;            // central wavelength
    //  dw       = [68,98,89,22,24]*1e-3;                 // delta_lambda in microns
    //  zp       = [-11.37,-11.18,-11.42,-11.76,-12.08];
    //  // photometric zeropoints in W/cm2/mic
    //  zpv      = 21.8;                                  // mag V per square arcsec at darkest
    //  msky     = array(double,[2,5,4]);
    //  msky(,1) = zpv+[0.  ,0.8 ,0.,-0.9,-1.9];          // darkest
    //  msky(,2) = zpv+[-1.5,0.2 ,0.,-0.8,-1.6]-0.6;      // 50%
    //  msky(,3) = zpv+[-2.2,0.  ,0.,-0.4,-0.8]-1.8;      // 80%
    //  msky(,4) = zpv+[-3. ,-0.5,0.,-0.1,-0.2]-3.3;      // bright
    //
    //  // m = -2.5 (log10(f) +zp)
    //
    //  lvec  = double(indgen(10)-1)/9.*750e-9+350e-9;     // lambda vector
    //  qeapd = spline(qe,lambda,lvec);
    //
    //  res   = 0.;
    //
    //  for (i=1;i<=4;i++) {
    //    f   = 10.^(-0.4*msky(,i)+zp);                   // f in W/cm2/mic
    //    f   = f/(h*c/cw)*pi*(400.^2-50.^2);             // f in N_photon/pup_gemini/s/mic
    //    f   = f*0.1;                                    //f in N_photon/pup_gemini/s/100nm
    //    tab1=f; tab2=cw;
    //    grow,tab1,f(5); grow,tab2,1100e-9;
    //    sp  = tspline(8,tab1,tab2,lvec);
    //    grow,res,sum(sp*qeapd);
    //  }
    //
    //  res *= (pi*ttwfs_aper_radius^2.); // for the TT wfs field stop
    //
    //  return zero_point_fudge*res(2:);


    val lambda = DenseVector(3.5, 4, 5, 6, 7, 8, 9, 10, 11) * 1e-7 // meters
    val qe = DenseVector(0.0, 5, 30, 37, 37, 32, 23, 10, 0.0) * 0.01 * 1.6 // percent
    val h = 6.62e-34
    val c = 3e8

    // for band u,b,v,r,i
    val cw = DenseVector(365.0, 440.0, 550.0, 700.0, 900.0) * 1e-9 // central wavelength
    val dw = DenseVector(68.0, 98.0, 89.0, 22.0, 24.0) * 1e-3 // delta_lambda in microns
    val zp = DenseVector(-11.37, -11.18, -11.42, -11.76, -12.08)

    // photometric zeropoints in W/cm2/mic
    val zpv = 21.8 // mag V per square arcsec at darkest
    val msky = Array(
      DenseVector(0.0, 0.8, 0.0, -0.9, -1.9) + zpv, // darkest
      DenseVector(-1.5, 0.2, 0.0, -0.8, -1.6) + (zpv - 0.6), // 50%
      DenseVector(-2.2, 0.0, 0.0, -0.4, -0.8) + (zpv - 1.8), // 80%
      DenseVector(-3.0, -0.5, 0.0, -0.1, -0.2) + (zpv - 3.3) // bright
    )

    val lvec = DenseVector.tabulate(10)(_ / 9.0) * 750.0e-9 + 350.0e-9 // lambda vector
    val qeapd = spline(qe, lambda, lvec)

    val res = DenseVector.zeros[Double](4)

    for (i <- 0 until 4) {
      val f1 = YUtils.pow(10.0, msky(i) * -0.4 + zp) // f en W/cm2/mic
      val f2 = (f1 :/ divide(h * c, cw)) * math.Pi * (math.pow(400.0, 2) - math.pow(50.0, 2)) // f in N_photon/pup_gemini/s/mic
      val f = f2 * 0.1 //f in N_photon/pup_gemini/s/100nm
      val tab1 = grow(f, f(f.size - 1))
      val tab2 = grow(cw, 1100e-9)
      val sp = tspline(8, tab1, tab2, lvec)
      res(i) = (sp :* qeapd).sum
    }

    (res * (math.Pi * math.pow(ttwfs_aper_radius, 2.0))) * zero_point_fudge // for the TT wfs field stop
  }


  def magstar(vstar: Double): Double = {
    //  lambda = [3.5,4, 5, 6, 7, 8, 9,10,11]*1e-7;      // meters
    //  qe     = [0., 5,30,37,37,32,23,10,0.]*0.01*1.6;  // percent
    //  h      = 6.62e-34;
    //  c      = 3e8;
    val lambda = DenseVector(3.5, 4, 5, 6, 7, 8, 9, 10, 11) * 1e-7
    val qe = DenseVector(0.0, 5, 30, 37, 37, 32, 23, 10, 0.0) * 0.01 * 1.6 // percent
    val h = 6.62e-34
    val c = 3e8

    // for band u,b,v,r,i
    //  cw     = [365,440,550,700,900]*1e-9;             // central wavelength
    //  dw     = [68,98,89,22,24]*1e-3;                  // delta_lambda in microns
    //  zp     = [-11.37,-11.18,-11.42,-11.76,-12.08];
    val cw = DenseVector(365.0, 440.0, 550.0, 700.0, 900.0) :* 1e-9 // central wavelength
    val dw = DenseVector(68.0, 98.0, 89.0, 22.0, 24.0) :* 1e-3 // delta_lambda in microns
    val zp = DenseVector(-11.37, -11.18, -11.42, -11.76, -12.08)

    // photometric zeropoints in W/cm2/mic
    //  zpv    = vstar;                                  // Vmag of star
    //  msky   = zpv+[-0.63,-0.58,0.,-0.52,-0.93];       // G0 star magnitudes
    val zpv = vstar // Vmag of star
    val msky = DenseVector(-0.63, -0.58, 0.0, -0.52, -0.93) + zpv // G0 star magnitudes

    //  lvec   = double(indgen(10)-1)/9.*750.e-9+350.e-9; // lambda vector
    //  qeapd  = spline(qe,lambda,lvec);
    val lvec = DenseVector.tabulate(10)(_ * 1.0) / 9.0 * 750.0e-9 + 350.0e-9 // lambda vector
    val qeapd = spline(qe, lambda, lvec)

    //  f      = 10.^(-0.4*msky+zp);                     // f en W/cm2/mic
    //  f      = f/(h*c/cw)*pi*(400.^2-50.^2);
    //  f      = f*0.1;
    //  tab1   = f; grow,tab1,f(5);
    //  tab2   = cw; grow,tab2,1100e-9;
    //  sp     = tspline(10,tab1,tab2,lvec);
    val f1 = YUtils.pow(10.0, msky * -0.4 + zp) // f en W/cm2/mic
    val f2 = (f1 :/ divide(h * c, cw)) * math.Pi * (math.pow(400.0, 2) - math.pow(50.0, 2))
    val f = f2 * 0.1
    val tab1 = grow(f, f(f.size - 1))
    val tab2 = grow(cw, 1100e-9)
    val sp = tspline(10, tab1, tab2, lvec)

    //  return zero_point_fudge*sum(sp*qeapd);
    zero_point_fudge * (sp :* qeapd).sum
  }


  //func ftcb(te,tcal,tmir,gain,dim,x=)
  ///* DOCUMENT ftcb(te,tcal,tmir,gain,dim,x=)
  //   returns [f,hbo,hcor,hbf]
  //   AUTHOR: F.Rigaut, way back in 1996?
  //   SEE ALSO:
  // */
  def ftcb(te: Double, tcal: Double, tmir: Double, gain: Double, dim: Int, x: DenseVector[Double] = null): DenseMatrix[Double] = {
    //  f = indgen(dim)/te/2./dim;
    //  if (!is_void(x)) { f = x;}
    //  p = 2i*pi*f;
    //
    //  hzoh  = (1.-exp(-te*p))/(te*p);
    //  hmir  = 1./(1.+tmir*p);
    //  hwfs  = (1.-exp(-te*p))/(te*p);
    //  hcal  = gain*exp(-tcal*p);
    //
    //  hbo  = hzoh*hmir*hwfs*hcal/(1-exp(-p*te));
    //
    //  hcor = double((1./(1.+hbo))*conj(1./(1.+hbo)));
    //  hbf  = double((hbo/(1.+hbo))*conj(hbo/(1.+hbo)));
    //  hbo  = double(hbo*conj(hbo));
    //
    //  return ([f,hbo,hcor,hbf]);

//    val f = if (x != null) x else (DenseVector.range(1, dim+1) / (te * 2.0 * dim))
//    // Porting note: 2*i is a complex number (i is defined in scalala.scalar package)
//    val p = f * math.Pi * 2*i
//
//    // Porting note: rearranged expressions to put vectors first, due to limitations in scalala-RC1.
//    // Also added methods (expComplex, divideComplex) to handle some operations that were not available.
//    // Added conjMultiply to get back to real vectors.
//    val pte = p * te
//    val mpte = pte * -1
//    val hzoh = (expComplex(mpte) * -1.0 + 1.0) :/ pte
//    val hmir = divideComplex(1.0, p * tmir + 1.0)
//    val hwfs = (expComplex(mpte) * -1.0 + 1.0) :/ pte
//    val hcal = expComplex(p * -tcal) * gain
//
//    val hbo = (((hzoh :* hmir) :* hwfs) :* hcal) :/ (expComplex(mpte) * -1.0 + 1.0)
//    val hbop1 = hbo + 1.0
//    val hcor = conjMultiply(divideComplex(1.0, hbop1));
//    val hbf = conjMultiply(hbo :/ hbop1)
//    val hbo2 = conjMultiply(hbo)
//
////    val m = Matrix.zeros[Double](f.size, 4)
////    m(::, 0) := f
////    m(::, 1) := hbo2
////    m(::, 2) := hcor
////    m(::, 3) := hbf
////    m
//    Matrix(f.toArray, hbo2.toArray, hcor.toArray, hbf.toArray).t

    // XXX Use java version for performance
    val a = HotSpots.ftcb(te, tcal, tmir, gain, dim)
    DenseMatrix(a(0),a(1),a(2),a(3)).t
  }
}