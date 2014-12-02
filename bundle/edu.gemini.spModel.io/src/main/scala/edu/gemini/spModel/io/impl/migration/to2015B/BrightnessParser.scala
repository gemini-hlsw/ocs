package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.shared.skyobject.Magnitude

import scala.util.matching.Regex
import scalaz._, Scalaz._

object BrightnessParser {

  type Parser = String => Option[NonEmptyList[Magnitude]]

  def parseBrightness: Parser =
    List(

      // Parsing here is very conservative; we try a lot of patterns rather than trying to cram a
      // bunch of stuff into a small number of them. This first group matches the entire string.

      // J=19
      // r=10.24 mag
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)=(-?\d+(\.\d+)?)( MAG)?$""".r, 2, 1),

      // J(Vega)=16.9
      // H(vega) = 16.9
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\(VEGA\) ?= ?(-?\d+(\.\d+)?)( MAG)?$""".r, 2, 1),

      // i(AB)=16.6
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\(AB\)=(-?\d+(\.\d+)?)( MAG)?$""".r, 2, 1, Magnitude.System.AB),

      // B_mag=9.4
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)_MAG=(-?\d+(\.\d+)?)$""".r, 2, 1),

      // H,11.268
      // R, 10.7
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q), ?(-?\d+(\.\d+)?)$""".r, 2, 1),

      // K = 9.0
      // J ~ 16.5
      // R ~ 22 mag
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q) [=~] (-?\d+(\.\d+)?)( MAG)?$""".r, 2, 1),

      // i mag = 18.1
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q) MAG = (-?\d+(\.\d+)?)$""".r, 2, 1),

      // Hband 13.07
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)BAND (-?\d+(\.\d+)?)$""".r, 2, 1),

      // <V> 20.00
      pat("""^<(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)> (-?\d+(\.\d+)?)$""".r, 2, 1),

      // B18.00
      // B 11.1
      // K  19.56
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q) *(-?\d+(\.\d+)?)$""".r, 2, 1),

      // I(AB)18.96
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\(AB\)(-?\d+(\.\d+)?)$""".r, 2, 1, Magnitude.System.AB),

      // I_AB=19.09
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)_AB=(-?\d+(\.\d+)?)$""".r, 2, 1, Magnitude.System.AB),

      // N = 30 mJy
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q) = (-?\d+(\.\d+)?) MJY$""".r, 2, 1, Magnitude.System.Jy),

      // N=2Jy
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)=(-?\d+(\.\d+)?)JY$""".r, 2, 1, Magnitude.System.Jy),

      // Q(18.3) = 17.57
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\(\d+\.\d+\) = (-?\d+(\.\d+)?)$""".r, 3, 1),

      // N(10.4) = 51.26 Jy
      // F(6.7um)=1.78Jy
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\(\d+\.\d+(UM)?\) ?= ?(-?\d+(\.\d+)?) ?JY$""".r, 3, 1, Magnitude.System.Jy),


      // N, 18.2 Jy
      pat("""^(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q), (-?\d+(\.\d+)?) JY$""".r, 2, 1, Magnitude.System.Jy),

      // 107 mJy at N
      // 105 mJy nucleus at N
      // 38 Jy at Q
      pat("""^(-?\d+(\.\d+)?) M?JY( NUCLEUS)? AT (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)$""".r, 1, 4, Magnitude.System.Jy),

      // 6.35/V
      pat("""^(-?\d+(\.\d+)?)/(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)$""".r, 1, 3),

      // 15: H
      pat("""^(-?\d+(\.\d+)?): (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)$""".r, 1, 3),

      // 10.206 Jmag
      pat("""^(-?\d+(\.\d+)?) (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)MAG$""".r, 1, 3),

      // 14.1(R)
      // 24.6 (H)
      pat("""^(-?\d+(\.\d+)?) ?\((U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\)$""".r, 1, 3),

      // 19.05 (I-band)
      pat("""^(-?\d+(\.\d+)?) ?\((U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)-BAND\)$""".r, 1, 3),

      // 19.7(iAB)
      pat("""^(-?\d+(\.\d+)?) ?\((U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)AB\)$""".r, 1, 3, Magnitude.System.AB),

      // 0.18Jy/N
      pat("""^(-?\d+(\.\d+)?)JY/(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)$""".r, 1, 3, Magnitude.System.Jy),

      // 50 Jy @ N
      pat("""^(-?\d+(\.\d+)?) JY @ (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)$""".r, 1, 3, Magnitude.System.Jy),

      // 3 mJy N-band
      pat("""^(-?\d+(\.\d+)?) MJY (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)-BAND$""".r, 1, 3, Magnitude.System.Jy),

      // 14.9mag at K
      // 2.7 mag at N
      pat("""^(-?\d+(\.\d+)?) ?MAG AT (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)$""".r, 1, 3),

      // 17.9 (i mag)
      pat("""^(-?\d+(\.\d+)?) \((U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q) MAG\)$""".r, 1, 3),

      ///
      /// These last ones matches only parts of the string, so they need to come last.
      ///

      // 110 mJy @N, 2300 mJy @Q
      // 5 Jy @N, 20 Jy @Q
      // 1.4Jy @ N
      pat("""\b(-?\d+(\.\d+)?) ?M?JY @ ?(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\b""".r, 1, 3, Magnitude.System.Jy),

      // 12.34U, 10.869J, 10.316K
      // 4.4 I
      // 18.74  K
      pat("""\b(-?\d+(\.\d+)?) *(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)\b""".r, 1, 3),

      // 14.8 Jmag, 14.2 Hmag
      pat("""\b(-?\d+(\.\d+)?) (U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)MAG\b""".r, 1, 3),

      // J=8.52,M=-0.24
      // K~5.7, J~9.6
      // B ~ 8.3, V~ 8.14, K ~8
      pat("""\b(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q) ?[=~] ?(-?\d+(\.\d+)?)\b""".r, 2, 1),

      // Fmag=16.49, Jmag=17.38
      // Kmag 11.5
      pat("""\b(U|B|V|UC|R|I|Y|J|H|K|L|M|N|Q)MAG[= ](-?\d+(\.\d+)?)\b""".r, 2, 1)

    ).foldRight[Parser](_ => None)((a, b) => s => a(s) orElse b(s))

  def pat(r: Regex, magGroup: Int, bandGroup: Int, sys: Magnitude.System = Magnitude.System.Vega): Parser = s =>
    r.findAllIn(s.trim.toUpperCase).matchData.toList.map { m =>
      val mag  = m.group(magGroup).toDouble
      val band = Magnitude.Band.valueOf(m.group(bandGroup))
      new Magnitude(band, mag, sys)
    }.toNel

}
