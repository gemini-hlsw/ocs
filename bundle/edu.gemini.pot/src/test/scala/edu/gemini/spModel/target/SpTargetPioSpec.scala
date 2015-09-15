package edu.gemini.spModel.target

import edu.gemini.spModel.core.Arbitraries
import edu.gemini.spModel.core.WavelengthConversions._
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.system.{ConicTarget, HmsDegTarget, ITarget}
import org.scalacheck.{Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import squants.motion.VelocityConversions._
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._

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
        EmissionLine(450.nm, 150.kps, 13.wattsPerSquareMeter, 22.wattsPerSquareMeterPerMicron),
        EmissionLine(550.nm, 400.kps, 23.wattsPerSquareMeter, 42.wattsPerSquareMeterPerMicron),
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
          spt.setSpatialProfile(sp)
          spt.setSpectralDistribution(sd)

          val pset = SPTargetPio.getParamSet(spt, factory)
          val spt2 = SPTargetPio.fromParamSet(pset)

          assert(spt.getTarget.getSpatialProfile == spt2.getTarget.getSpatialProfile)
          assert(spt.getTarget.getSpectralDistribution == spt2.getTarget.getSpectralDistribution)
        }
    }

  }

  val ParamRa  = "c1"
  val ParamDec = "c2"

  "SPTargetPio" should {
    def newTargetParamSet(t: ITarget): ParamSet = {
      val fact = new PioXmlFactory
      val spt  = new SPTarget(t)
      SPTargetPio.getParamSet(spt, fact)
    }

    def expect(ps: ParamSet, era: Double, edec: Double): Unit = {
      val spt = SPTargetPio.fromParamSet(ps)
      val ra  = spt.getTarget.getRaDegrees
      val dec = spt.getTarget.getDecDegrees

      ra  must beCloseTo(era,  0.000001)
      dec must beCloseTo(edec, 0.000001)
    }

    def fromDegrees(t: ITarget): Unit = {
      val ps   = newTargetParamSet(t)
      val pRa  = ps.getParam(ParamRa)
      val pDec = ps.getParam(ParamDec)

      pRa.setValue("180.0")
      pDec.setValue("10.0")

      expect(ps, 180.0, 10.0)
    }

    def fromHmsDms(t: ITarget): Unit = {
      val ps   = newTargetParamSet(t)
      val pRa  = ps.getParam(ParamRa)
      val pDec = ps.getParam(ParamDec)

      pRa.setValue("180.0")
      pDec.setValue("10.0")

      expect(ps, 180.0, 10.0)
    }

    "read RA and Dec specified as degrees for sidereal targets" in {
      fromDegrees(new HmsDegTarget)
    }

    "read RA and Dec specified as HMS/DMS for sidereal targets" in {
      fromHmsDms(new HmsDegTarget)
    }

    "read RA and Dec specified as degrees for non-sidereal targets" in {
      fromDegrees(new ConicTarget)
    }

    "read RA and Dec specified as HMS/DMS for non-sidereal targets" in {
      fromHmsDms(new ConicTarget)
    }
  }

}
