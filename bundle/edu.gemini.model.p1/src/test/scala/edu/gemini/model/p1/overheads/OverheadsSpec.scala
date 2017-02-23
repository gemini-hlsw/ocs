package edu.gemini.model.p1.overheads

import edu.gemini.model.p1.immutable._
import org.specs2.mutable.Specification

import scala.collection.immutable.HashMap

class OverheadsSpec extends Specification {
  "Overheads" should {
    "calculate all times properly in both directions" in {
      forall(OverheadsSpec.instMap) { case ((_, _), (blueprint, expected)) =>
        val overheads = Overheads(blueprint())
        val results = for (o <- Overheads(blueprint())) yield {
          val calculated = o.calculate(OverheadsSpec.intTime)
          val intTime    = o.intTimeFromProgTime(calculated.progTime)
          (calculated, intTime)
        }
        results shouldNotEqual None and(OverheadsSpec.almostEqual(results.get._1, expected) shouldEqual true) and(OverheadsSpec.almostEqual(results.get._2, OverheadsSpec.intTime) shouldEqual true)
      }
    }
  }
}

object OverheadsSpec {
  private val precision = 0.01
  def almostEqual(t1: TimeAmount, t2: TimeAmount): Boolean =
    (t1.toHours.value - t2.toHours.value).abs < precision
  def almostEqual(times1: ObservationTimes, times2: ObservationTimes): Boolean =
    almostEqual(times1.progTime, times2.progTime) && almostEqual(times1.partTime, times2.partTime)

  // Integration time in hours used in the tests.
  val intTime = TimeAmount(1.70, TimeUnit.HR)

  // Convenience function to create ObservationTimes in hours.
  private def obsTimes(progTime: Double, partTime: Double): ObservationTimes = {
    def hrs(t: Double): TimeAmount =
      TimeAmount(t, TimeUnit.HR)
    ObservationTimes(hrs(progTime), hrs(partTime))
  }

  // Mapping of all instrument modes to blueprint and results.
  // These are taken from the python script in REL-2985.
  // Values in blueprints don't generally matter with exception of GMOS and NIRI.
  // We just need blueprints for the lookups.
  val instMap = HashMap(
    (("gmos", "ifu"), (
      () => GmosSBlueprintIfu(GmosSDisperser.B600, GmosSFilter.None, GmosSFpuIfu.values.apply(0)),
      obsTimes(2.44, 0.24))
      ),
    (("gmos", "longns"), (
      () => GmosSBlueprintLongslitNs(GmosSDisperser.B600, GmosSFilter.None, GmosSFpuNs.values.apply(0)),
      obsTimes(2.69, 0.27))
      ),
    (("gmos", "mos"), (
      () => GmosSBlueprintMos(GmosSDisperser.B600, GmosSFilter.None, GmosSMOSFpu.values.apply(0), false, false),
      obsTimes(2.35, 0.23))
      ),
    (("gmos", "long"), (
      () => GmosSBlueprintLongslit(GmosSDisperser.B600, GmosSFilter.None, GmosSFpu.values.apply(0)),
      obsTimes(2.29, 0.23))
      ),
    (("gmos", "ifuns"), (
      () => GmosSBlueprintIfuNs(GmosSDisperser.B600, GmosSFilter.None, GmosSFpuIfuNs.values.apply(0)),
      obsTimes(2.83, 0.28))
      ),
    (("gmos", "imaging"), (
      () => GmosSBlueprintImaging(Nil),
      obsTimes(2.14, 0.00))
      ),
    (("gmos", "mosns"), (
      () => GmosSBlueprintMos(GmosSDisperser.B600, GmosSFilter.None, GmosSMOSFpu.values.apply(0), true, false),
      obsTimes(2.76, 0.28))
      ),
    (("phoenix", "long"), (
      () => PhoenixBlueprint(Site.GS, PhoenixFocalPlaneUnit.values.apply(0), PhoenixFilter.values.apply(0)),
      obsTimes(2.40, 0.60))
      ),
    (("visitor", "any"), (
      () => VisitorBlueprint(Site.GS, ""),
      obsTimes(2.20, 0.00))
      ),
    (("f2", "mos"), (
      () => Flamingos2BlueprintMos(Flamingos2Disperser.R1200JH, Nil, false),
      obsTimes(3.18, 0.80))
      ),
    (("f2", "imaging"), (
      () => Flamingos2BlueprintImaging(Nil),
      obsTimes(2.73, 0.28))
      ),
    (("f2", "long"), (
      () => Flamingos2BlueprintLongslit(Flamingos2Disperser.R1200JH, Nil, Flamingos2Fpu.values.apply(0)),
      obsTimes(2.85, 0.71))
      ),
    (("gsaoi", "imaging"), (
      () => GsaoiBlueprint(Nil),
      obsTimes(4.69, 0.00))
      ),
    (("nifs", "ifu"), (
      () => NifsBlueprint(NifsDisperser.Z),
      obsTimes(2.36, 0.59))
      ),
    (("texes", "long"), (
      () => TexesBlueprint(Site.GS, TexesDisperser.values.apply(0)),
      obsTimes(2.40, 0.00))
      ),
    (("gpi", "ifu"), (
      () => GpiBlueprint(GpiObservingMode.HDirect, GpiDisperser.values.apply(0)),
      obsTimes(2.60, 0.13))
      ),
    (("dssi", "ifu"), (
      () => DssiBlueprint(Site.GS),
      obsTimes(1.88, 0.00))
      ),
    (("gnirs", "imaging"), (
      () => GnirsBlueprintImaging(AltairNone, GnirsPixelScale.PS_005, GnirsFilter.values.apply(0)),
      obsTimes(2.36, 0.24))
      ),
    (("gnirs", "long"), (
      () => GnirsBlueprintSpectroscopy(AltairNone, GnirsPixelScale.PS_005, GnirsDisperser.D_10, GnirsCrossDisperser.LXD, GnirsFpu.values.apply(0), GnirsCentralWavelength.values.apply(0)),
      obsTimes(2.34, 0.58))
      ),
    (("niri", "imagingngs"), (
      () => NiriBlueprint(AltairNGS(false), NiriCamera.F6, Nil),
      obsTimes(2.36, 0.24))
      ),
    (("niri", "imaging"), (
      () => NiriBlueprint(AltairNone, NiriCamera.F6, Nil),
      obsTimes(2.23, 0.22))
      ),
    (("graces", "spec"), (
      () => GracesBlueprint(GracesFiberMode.values.apply(0), GracesReadMode.values.apply(0)),
      obsTimes(1.93, 0.00))
      )
  )
}