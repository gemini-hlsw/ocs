package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.AgsInstrument
import edu.gemini.ags.servlet.AgsInstrument._
import edu.gemini.ags.servlet.AgsAo.Altair
import edu.gemini.spModel.gemini.altair.AltairParams
import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.LyotWheel
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth
import edu.gemini.spModel.telescope.PosAngleConstraint

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbAgsInstrument {

  implicit val arbAltair: Arbitrary[Altair] =
    Arbitrary {
      arbitrary[AltairParams.Mode].map(Altair(_))
    }

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
        a <- arbitrary[Option[Altair]]
      } yield GmosNorth(f, c, a)
    }

  implicit val arbGmosSouth: Arbitrary[GmosSouth] =
    Arbitrary {
      for {
        f <- arbitrary[FPUnitSouth]
        c <- arbitrary[PosAngleConstraint]
      } yield GmosSouth(f, c)
    }

  implicit val arbGnirs: Arbitrary[Gnirs] =
    Arbitrary {
      for {
        c <- arbitrary[PosAngleConstraint]
        a <- arbitrary[Option[Altair]]
      } yield Gnirs(c, a)
    }

  implicit val arbGsaoi: Arbitrary[Gsaoi] =
    Arbitrary { arbitrary[PosAngleConstraint].map(Gsaoi(_)) }

  implicit val arbNifs: Arbitrary[Nifs] =
    Arbitrary { arbitrary[Option[Altair]].map(Nifs(_)) }

  implicit val arbNiri: Arbitrary[Niri] =
    Arbitrary { arbitrary[Option[Altair]].map(Niri(_)) }

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
        arbitrary[Nifs],
        arbitrary[Niri],
        arbitrary[Other]
      )
    }
}

object agsinstrument extends ArbAgsInstrument
