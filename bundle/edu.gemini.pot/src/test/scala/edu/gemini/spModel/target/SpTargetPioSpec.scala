package edu.gemini.spModel.target

import edu.gemini.spModel.core.{Wavelength, Arbitraries}
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.EmissionLine.{Continuum, Flux}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

/** Tests Pio input/output operations for SpTargets.
  * Currently this only tests that the source profile and distribution are stored and retrieved.
  * Feel free to expand on this; however, I guess this will all become obsolete once we switch to the new target model.
  */
object SpTargetPioSpec extends Specification with ScalaCheck with Arbitraries {

  {
    implicit val arbDistribution = Arbitrary[SpectralDistribution] {
      Gen.oneOf(
        BlackBody(8000),
        BlackBody(10000),
        PowerLaw(0),
        PowerLaw(1),
        EmissionLine(Wavelength.fromNanometers(450), 150, Flux.fromWatts(13), Continuum.fromWatts(22)),
        EmissionLine(Wavelength.fromNanometers(550), 400, Flux.fromWatts(23), Continuum.fromWatts(42)),
        LibraryStar.A0V,
        LibraryStar.A5III,
        LibraryNonStar.NGC2023,
        LibraryNonStar.GammaDra
      )
    }
    implicit val arbProfile = Arbitrary[SpatialProfile] {
      Gen.oneOf(
        PointSource(),
        GaussianSource(0.5),
        GaussianSource(0.75),
        UniformSource())
    }

    "SPTargetPio" should {
      "store source profile and distribution" !
        prop { (sd: Option[SpectralDistribution], sp: Option[SpatialProfile]) =>

          val factory = new PioXmlFactory()
          val paramSet = factory.createParamSet("test")

          val spt = new SPTarget(10, 10)
          spt.getTarget.setSpatialProfile(sp)
          spt.getTarget.setSpectralDistribution(sd)

          val pset = SPTargetPio.getParamSet(spt, factory)
          val spt2 = SPTargetPio.fromParamSet(pset)

          assert(spt.getTarget.getSpatialProfile == spt2.getTarget.getSpatialProfile)
          assert(spt.getTarget.getSpectralDistribution == spt2.getTarget.getSpectralDistribution)
        }
    }

  }

}
