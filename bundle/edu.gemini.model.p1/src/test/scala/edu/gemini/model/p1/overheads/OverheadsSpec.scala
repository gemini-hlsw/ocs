package edu.gemini.model.p1.overheads

import edu.gemini.model.p1.immutable._

import scala.collection.immutable.HashMap

// None of this is applicable anymore in light of the change from REL-2985 to REL-2926.
class OverheadsSpec extends org.specs2.mutable.Specification {
 "Overheads" should {
   "calculate all times properly in both directions" in {
     forall(OverheadsSpec.instMap) { case ((k, v), (blueprint, expected)) =>
       val overheads = Overheads(blueprint())
       val results = for (o <- Overheads(blueprint())) yield {
         val calculated = o.calculate(OverheadsSpec.intTime)
         calculated
       }
       results.map(r => OverheadsSpec.almostEqual(r, expected)) should beSome(true)
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
      obsTimes(1.7, 0.23))
      ),
    (("gmos", "longns"), (
      () => GmosSBlueprintLongslitNs(GmosSDisperser.B600, GmosSFilter.None, GmosSFpuNs.values.apply(0)),
      obsTimes(1.7, 0.23))
      ),
    (("gmos", "mos"), (
      () => GmosSBlueprintMos(GmosSDisperser.B600, GmosSFilter.None, GmosSMOSFpu.values.apply(0), false, false),
      obsTimes(1.7, 0.23))
      ),
    (("gmos", "long"), (
      () => GmosSBlueprintLongslit(GmosSDisperser.B600, GmosSFilter.None, GmosSFpu.values.apply(0)),
      obsTimes(1.7, 0.23))
      ),
    (("gmos", "ifuns"), (
      () => GmosSBlueprintIfuNs(GmosSDisperser.B600, GmosSFilter.None, GmosSFpuIfuNs.values.apply(0)),
      obsTimes(1.7, 0.23))
      ),
    (("gmos", "imaging"), (
      () => GmosSBlueprintImaging(Nil),
      obsTimes(1.7, 0.00))
      ),
    (("gmos", "mosns"), (
      () => GmosSBlueprintMos(GmosSDisperser.B600, GmosSFilter.None, GmosSMOSFpu.values.apply(0), true, false),
      obsTimes(1.7, 0.23))
      ),
    (("phoenix", "long"), (
      () => PhoenixBlueprint(Site.GS, PhoenixFocalPlaneUnit.values.apply(0), PhoenixFilter.values.apply(0)),
      obsTimes(1.7, 0.3334))
      ),
    (("visitor", "any"), (
      () => VisitorBlueprint(Site.GS, ""),
      obsTimes(1.7, 0.00))
      ),
    (("f2", "mos"), (
      () => Flamingos2BlueprintMos(Flamingos2Disperser.R1200JH, Nil, false),
      obsTimes(1.7, 0.5))
      ),
    (("f2", "imaging"), (
      () => Flamingos2BlueprintImaging(Nil),
      obsTimes(1.7, 0.17))
      ),
    (("f2", "long"), (
      () => Flamingos2BlueprintLongslit(Flamingos2Disperser.R1200JH, Nil, Flamingos2Fpu.values.apply(0)),
      obsTimes(1.7, 0.5))
      ),
    (("gsaoi", "imaging"), (
      () => GsaoiBlueprint(Nil),
      obsTimes(1.7, 0.00))
      ),
    (("nifs", "ifu"), (
      () => NifsBlueprint(NifsDisperser.Z),
      obsTimes(1.7, 0.5))
      ),
    (("texes", "long"), (
      () => TexesBlueprint(Site.GS, TexesDisperser.values.apply(0)),
      obsTimes(1.7, 0.00))
      ),
    (("gpi", "ifu"), (
      () => GpiBlueprint(GpiObservingMode.HDirect, GpiDisperser.values.apply(0)),
      obsTimes(1.7, 0.085))
      ),
    (("dssi", "ifu"), (
      () => DssiBlueprint(Site.GS),
      obsTimes(1.7, 0.00))
      ),
    (("igrins2", "spec"), (
      () => Igrins2Blueprint(Igrins2NoddingOption.NodToSky),
      obsTimes(1.7, 0.5))
      ),
    (("gnirs", "imaging"), (
      () => GnirsBlueprintImaging(AltairNone, GnirsPixelScale.PS_005, GnirsFilter.values.apply(0)),
      obsTimes(1.7, 0.17))
      ),
    (("gnirs", "long"), (
      () => GnirsBlueprintSpectroscopy(AltairNone, GnirsPixelScale.PS_005, GnirsDisperser.D_10, GnirsCrossDisperser.LXD, GnirsFpu.values.apply(0), GnirsCentralWavelength.values.apply(0)),
      obsTimes(1.7, 0.5))
      ),
    (("niri", "imagingngs"), (
      () => NiriBlueprint(AltairNGS(false), NiriCamera.F6, Nil),
      obsTimes(1.7, 0.17))
      ),
    (("niri", "imaging"), (
      () => NiriBlueprint(AltairNone, NiriCamera.F6, Nil),
      obsTimes(1.7, 0.17))
      ),
    (("graces", "spec"), (
      () => GracesBlueprint(GracesFiberMode.values.apply(0), GracesReadMode.values.apply(0)),
      obsTimes(1.7, 0.00))
      ),
    (("keck", "all"), (
      () => KeckBlueprint(KeckInstrument.values.apply(0)),
      obsTimes(1.70, 0.00))
      ),
    (("subaru", "all"), (
      () => SubaruBlueprint(SubaruInstrument.values.apply(0), None),
      obsTimes(1.70, 0.00))
      )
  )
}
