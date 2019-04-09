package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.AgsInstrument
import edu.gemini.ags.servlet.AgsInstrument._
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.telescope.PosAngleConstraint

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbAgsInstrument {

  implicit val arbFlamingos2: Arbitrary[Flamingos2] =
    Arbitrary {
      for {
        w <- arbitrary[LyotWheel]
        c <- arbitrary[PosAngleConstraint]
      } yield Flamingos2(w, c)
    }

  implicit val arbGmosNorth: Arbitrary[GmosNorth] =
    Arbitrary {
      for {
        f <- arbitrary[FPUnitNorth]
        c <- arbitrary[PosAngleConstraint]
      } yield GmosNorth(f, c)
    }

  implicit val arbGmosSouth: Arbitrary[GmosSouth] =
    Arbitrary {
      for {
        f <- arbitrary[FPUnitSouth]
        c <- arbitrary[PosAngleConstraint]
      } yield GmosSouth(f, c)
    }

  implicit val arbGnirs: Arbitrary[Gnirs] =
    Arbitrary { arbitrary[PosAngleConstraint].map(Gnirs(_)) }

  implicit val arbGsaoi: Arbitrary[Gsaoi] =
    Arbitrary { arbitrary[PosAngleConstraint].map(Gsaoi(_)) }

  implicit val arbOther: Arbitrary[Other] =
    Arbitrary {
      arbitrary[Instrument].map(Other(_))
    }

  implicit val arbAgsInstrument: Arbitrary[AgsInstrument] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[Flamingos2],
        arbitrary[GmosNorth],
        arbitrary[GmosSouth],
        arbitrary[Gnirs],
        arbitrary[Gsaoi],
        arbitrary[Other]
      )
    }
}

object agsinstrument extends ArbAgsInstrument
