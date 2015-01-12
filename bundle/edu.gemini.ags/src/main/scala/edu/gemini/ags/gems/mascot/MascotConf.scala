package edu.gemini.ags.gems.mascot

import breeze.linalg._
import breeze.util._


/**
 * Constants taken from mascot.conf in the Yorick version.
 * These were added only as needed, so the order is a bit different.
 */
object MascotConf {

  // MASCOT PARAMETERS
  val mag_min_threshold = 6.5   // XXX allan: changed from 10.
  val mag_max_threshold = 21.2  // XXX allan: changed from 17.5
  val nstar_limit = 7
  val get_2mass_image = true

  // 0 to 1. 0=select best avg, 1=select best rms.
  val avg_rms_criteria = 0.0

  // in arcsec. to fetch from Nomad1
  val ttgs_max_fov_radius = 1.2

  val sampfreq = 800.0

  // already taken into account in magstar.
  val detqe = 1.0

  val thrup = 0.10
  // will x magstar() by this. see code.
  val zero_point_fudge = 0.5
  val ttron = 0.0
  val lambdawfs = 0.65
  val lambdaim = 1.65
  val r0vis = 0.166
  val debug = 0
  val tel_diam = 7.9
  // 0=no sky, 1=new moon, 2=50%, 3=80%, 4=full moon.
  val case_sky = 4
  // in e-/s/channel.
  val dark_current = 600.0
  // field stop radius [arcsec]
  val ttwfs_aper_radius = 0.7

  val optim_npt = 5
  val smap_npt = 33

  // arcsecs
  val halffield = 40.0

  val nst = 3
  // max nb of ast to save in result file when processing a batch of object
  val nast2print = 5

  val cn2 = DenseVector(0.646, 0.080, 0.119, 0.035, 0.025, 0.080, 0.015)

  val wind = DenseVector(5.0, 7.5, 12.0, 25.0, 34.0, 21.0, 8.0)

  val alt = DenseVector(0.0, 1800.0, 3300.0, 5800.0, 7400.0, 13100.0, 15800.0)

  // first coef in equation below is estimate with outer scale: in rd^2, from Noll w/ outer scale
  val rmstt1 = 0.30 * (math.pow((7.9 / r0vis), 1.666))
  // in rd rms, difference at edges.
  val rmstt2 = math.sqrt(rmstt1) * 4.0
  // in meters, difference at edges
  val rmstt3 = rmstt2 * 0.5e-6 / (2 * math.Pi)
  // in arcsec rms
  val rmstt = rmstt3 / 7.9 / 4.848e-6
  // approx. typ. 50% corr angle @ 4 arcmin.
  val rmsta = rmstt * 100.0/240.0
  val rmsmodes = DenseVector(rmstt, rmstt, rmsta, rmsta, rmsta)

  // tip vibrations in arcsec rms.
  val tipvibrms = DenseVector(0.08, 0.005, 0.008)

  // corresponding TT vib freqs [Hz]
  val tipvibfreq = DenseVector(0.5, 22.0, 75.0)

  // corresponding TT vib width [Hz]
  val tipvibwidth = DenseVector(0.1, 4.0, 10.0)

  // tilt vibrations in arcsec rms.
  val tiltvibrms = DenseVector(0.08, 0.005, 0.008)

  // corresponding TT vib freqs [Hz]
  val tiltvibfreq = DenseVector(0.5, 22.0, 75.0)

  // corresponding TT vib width [Hz]
  val tiltvibwidth = DenseVector(0.1, 4.0, 10.0)


  val maxthetaas = 60.0
  // crowding radius in arcsec.
  val crowding_radius = 5
  // minimum distance star to edge of aperture
  val edge_margin = 5.0
  // forget about this one.
  val window_pad = 0
  // adhoc correction for gaussian -> poisson in wfs noise expression.
  val correct_for_poisson = true

  // Invalid magnitude value: original source had just the constant
  val invalidMag = -27

}