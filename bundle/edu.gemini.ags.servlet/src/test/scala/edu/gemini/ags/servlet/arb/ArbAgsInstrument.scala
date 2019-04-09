package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.AgsInstrument
import edu.gemini.ags.servlet.AgsInstrument._
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbAgsInstrument {

  implicit val arbFlamingos2: Arbitrary[Flamingos2] =
    Arbitrary { arbitrary[LyotWheel].map(Flamingos2(_)) }

  implicit val arbGmosNorth: Arbitrary[GmosNorth] =
    Arbitrary { arbitrary[FPUnitNorth].map(GmosNorth(_)) }

  implicit val arbGmosSouth: Arbitrary[GmosSouth] =
    Arbitrary { arbitrary[FPUnitSouth].map(GmosSouth(_)) }

  implicit val arbOther: Arbitrary[Other] =
    Arbitrary { arbitrary[Instrument].map(Other(_)) }

  implicit val arbAgsInstrument: Arbitrary[AgsInstrument] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[Flamingos2],
        arbitrary[GmosNorth],
        arbitrary[GmosSouth],
        arbitrary[Other]
      )
    }
}

object agsinstrument extends ArbAgsInstrument
