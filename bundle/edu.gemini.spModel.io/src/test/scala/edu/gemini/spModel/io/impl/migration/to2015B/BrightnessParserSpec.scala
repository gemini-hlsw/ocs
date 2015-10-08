package edu.gemini.spModel.io.impl.migration.to2015B

import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.shared.skyobject.Magnitude.Band
import edu.gemini.spModel.core.MagnitudeSystem
import edu.gemini.spModel.core.MagnitudeSystem.{VEGA, AB, JY}
import org.specs2.mutable.Specification

import scalaz.NonEmptyList

object BrightnessParserSpec extends Specification {

  import Magnitude.Band._

  def mag(n: Double, b: Band, s: MagnitudeSystem): Magnitude =
    new Magnitude(b, n, s)

  def one(n: Double, b: Band, s: MagnitudeSystem): NonEmptyList[Magnitude] =
    NonEmptyList(mag(n, b, s))

  // All taken from historical data
  val cases = Map(
    "0.18Jy/N"                    -> one(.18, N, JY),
    "1.4Jy @ N"                   -> one(1.4, N, JY),
    "10.206 Jmag"                 -> one(10.206, J, VEGA),
    "105 mJy nucleus at N"        -> one(0.105, N, JY),
    "107 mJy at N"                -> one(0.107, N, JY),
    "110 mJy @N, 2300 mJy @Q"     -> NonEmptyList(mag(0.110, N, JY), mag(2.300, Q, JY)),
    "12.34U, 10.869J, 10.316K"    -> NonEmptyList(mag(12.34, U, VEGA), mag(10.869, J, VEGA), mag(10.316, K, VEGA)),
    "14.1(R)"                     -> one(14.1, R, VEGA),
    "14.8 Jmag, 14.2 Hmag"        -> NonEmptyList(mag(14.8, J, VEGA), mag(14.2, H, VEGA)),
    "14.9mag at K"                -> one(14.9, K, VEGA),
    "15: H"                       -> one(15, H, VEGA),
    "17.9 (i mag)"                -> one(17.9, i, AB),
    "18.74  K"                    -> one(18.74, K, VEGA),
    "19.05 (I-band)"              -> one(19.05, I, VEGA),
    "19.7(iAB)"                   -> one(19.7, i, AB),
    "2.7 mag at N"                -> one(2.7, N, VEGA),
    "24.6 (H)"                    -> one(24.6, H, VEGA),
    "3 mJy N-band"                -> one(0.003, N, JY),
    "38 Jy at Q"                  -> one(38, Q, JY),
    "4.4 I"                       -> one(4.4, I, VEGA),
    "5 Jy @N, 20 Jy @Q"           -> NonEmptyList(mag(5, N, JY), mag(20, Q, JY)),
    "50 Jy @ N"                   -> one(50, N, JY),
    "6.35/V"                      -> one(6.35, V, VEGA),
    "<V> 20.00"                   -> one(20, V, VEGA),
    "B 11.1"                      -> one(11.1, B, VEGA),
    "B ~ 8.3, V~ 8.14, K ~8"      -> NonEmptyList(mag(8.3, B, VEGA), mag(8.14, V, VEGA), mag(8, K, VEGA)),
    "B18.00"                      -> one(18, B, VEGA),
    "B_mag=9.4"                   -> one(9.4, B, VEGA),
    "H,11.268"                    -> one(11.268, H, VEGA),
    "Hband 13.07"                 -> one(13.07, H, VEGA),
    "i mag = 18.1"                -> one(18.1, i, AB),
    "I(AB)18.96"                  -> one(18.96, I, AB),
    "i(AB)=16.6"                  -> one(16.6, i, AB),
    "I_AB=19.09"                  -> one(19.09, I, AB),
    "J mag ~ 18.8"                -> one(18.8, J, VEGA),
    "J ~ 16.5"                    -> one(16.5, J, VEGA),
    "J=8.52,M=-0.24"              -> NonEmptyList(mag(8.52, J, VEGA), mag(-0.24, M, VEGA)),
    "K  19.56"                    -> one(19.56, K, VEGA),
    "K  = 19.9"                   -> one(19.9, K, VEGA),
    "K = 9.0"                     -> one(9.0, K, VEGA),
    "Kmag 11.5"                   -> one(11.5, K, VEGA),
    "K~5.7, J~9.6"                -> NonEmptyList(mag(5.7, K, VEGA), mag(9.6, J, VEGA)),
    "N = 30 mJy"                  -> one(0.030, N, JY),
    "N(10.4) = 51.26 Jy"          -> one(51.26, N, JY),
    "N, 18.2 Jy"                  -> one(18.2, N, JY),
    "N=2Jy"                       -> one(2, N, JY),
    "Q(18.3) = 17.57"             -> one(17.57, Q, VEGA),
    "R ~ 22 mag"                  -> one(22, R, VEGA),
    "R, 10.7"                     -> one(10.7, R, VEGA),
    "U 15.45, J 13.053, K 12.542" -> NonEmptyList(mag(15.45, U, VEGA), mag(13.053, J, VEGA), mag(12.542, K, VEGA)),
    "11.6 UC"                     -> one(11.6, UC, VEGA),
    "11.6 UCmag"                  -> one(11.6, UC, VEGA),
    "11.6 UC mag"                 -> one(11.6, UC, VEGA),
    "11.6 (UCmag)"                -> one(11.6, UC, VEGA),
    "11.6 U"                      -> one(11.6, U, VEGA),
    "11.6 Umag"                   -> one(11.6, U, VEGA),
    "11.6 U mag"                  -> one(11.6, U, VEGA),
    "11.6 (Umag)"                 -> one(11.6, U, VEGA),
    "16.9 J (Vega)"               -> one(16.9, J, VEGA),
    "16.9 J Vega"                 -> one(16.9, J, VEGA),
    "16.9 J Jy"                   -> one(16.9, J, JY),
    "7.39 in V"                   -> one(7.39, V, VEGA),
    "13.774 in J-band "           -> one(13.774, J, VEGA),
    "L'=7.055,M=7.04"             -> NonEmptyList(mag(7.055, L, VEGA), mag(7.04, M, VEGA)),
    "22.7 z'"                     -> one(22.7, z, AB)
  )

  "Brightness Parser" should {
      cases.foreach { case (s, ms) =>
        "parse " + s in {
          BrightnessParser.parseBrightness(s) must beSome(ms)
        }
      }
  }

}
