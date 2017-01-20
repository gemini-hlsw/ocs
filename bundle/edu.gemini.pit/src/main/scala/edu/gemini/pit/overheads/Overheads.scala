package edu.gemini.pit.overheads

import java.time.Duration

import edu.gemini.model.p1.immutable._

import scalaz._
import Scalaz._

sealed trait Overheads {
  def partnerOverheadFraction: Double
  def acquisitionOverhead: Duration
  def typicalExpTime: Option[Duration]
  def otherOverheadFraction: Double
}

// Due to the large number of possible blueprint bases and how each one requires different configuration params
// to determine the overheads, it seems infeasible to read this information from a file, so for now it is hard-coded
// and will require changing here if these values change.
object Overheads extends (BlueprintBase => Option[Overheads]) {
  private class SimpleOverheads(override val partnerOverheadFraction: Double,
                                override val acquisitionOverhead: Duration,
                                override val typicalExpTime: Option[Duration],
                                override val otherOverheadFraction: Double) extends Overheads

  // Simplified way to create SimpleOverheads with standard fields.
  // Times must be given in minutes.
  private object SimpleOverheads {
    def apply(partnerOverheadFraction: Double,
              acquisitionOverhead: Long,
              typicalExpTime: Long,
              otherOverheadFraction: Double) =
      new SimpleOverheads(
        partnerOverheadFraction,
        Duration.ofMinutes(acquisitionOverhead),
        Duration.ofMinutes(typicalExpTime).some,
        otherOverheadFraction)
  }

  // GMOS overheads are the same between sites.
  private lazy val GmosImagingOverheads    = SimpleOverheads(0.00,  6,  300, 0.137).some
  private lazy val GmosLongslitOverheads   = SimpleOverheads(0.10, 16, 1200, 0.034).some
  private lazy val GmosLongslitNsOverheads = SimpleOverheads(0.10, 16,  960, 0.271).some
  private lazy val GmosMosOverheads        = SimpleOverheads(0.10, 18, 1200, 0.028).some
  private lazy val GmosMosNsOverheads      = SimpleOverheads(0.10, 18,  960, 0.271).some
  private lazy val GmosIfuOverheads        = SimpleOverheads(0.10, 18, 1200, 0.083).some
  private lazy val GmosIfuNsOverheads      = SimpleOverheads(0.10, 18,  960, 0.311).some

  def apply(b: BlueprintBase): Option[Overheads] = b match {
    case _: Flamingos2BlueprintImaging  => SimpleOverheads(0.10, 15,   60, 0.467).some
    case _: Flamingos2BlueprintLongslit => SimpleOverheads(0.25, 20,  120, 0.283).some
    case _: Flamingos2BlueprintMos      => SimpleOverheads(0.25, 30,  120, 0.283).some

    case _: GnirsBlueprintImaging       => SimpleOverheads(0.10, 10,   60, 0.183).some
    case _: GnirsBlueprintSpectroscopy  => SimpleOverheads(0.25, 15,  300, 0.080).some

    case _: NifsBlueprint               => SimpleOverheads(0.25, 11,  600, 0.175).some
    case _: GsaoiBlueprint              => SimpleOverheads(0.00, 30,   60, 0.833).some
    case _: GracesBlueprint             => SimpleOverheads(0.00, 10, 1200, 0.036).some
    case _: GpiBlueprint                => SimpleOverheads(0.05, 10,   60, 0.333).some
    case _: PhoenixBlueprint            => SimpleOverheads(0.25, 20, 1200, 0.021).some
    case _: TexesBlueprint              => SimpleOverheads(0.00, 20,  900, 0.022).some

    case _: DssiBlueprint               => new SimpleOverheads(0.00, Duration.ofMinutes(10), None, 0.010).some
    case _: VisitorBlueprint            => new SimpleOverheads(0.00, Duration.ofMinutes(10), None, 0.100).some


    // NIRI relies on whether or not AO is being used.
    case nbp: NiriBlueprint             => SimpleOverheads(0.10, nbp.altair.ao.toBoolean ? 10 | 6, 60, 0.183).some


    // GMOS is independent of site unless N&S is being used.
    case _: GmosNBlueprintImaging    | _: GmosSBlueprintImaging    => GmosImagingOverheads
    case _: GmosNBlueprintLongslit   | _: GmosSBlueprintLongslit   => GmosLongslitOverheads
    case _: GmosNBlueprintLongslitNs | _: GmosSBlueprintLongslitNs => GmosLongslitNsOverheads
    case _: GmosNBlueprintIfu        | _: GmosSBlueprintIfu        => GmosIfuOverheads

    // Only GMOS-S offers IFU N&S.
    case _: GmosSBlueprintIfuNs                                    => GmosIfuNsOverheads

    // For MOS, N&S setting is a represented as a boolean flag.
    case gmosbp: GmosNBlueprintMos => gmosbp.nodAndShuffle ? GmosMosNsOverheads | GmosMosOverheads
    case gmosbp: GmosSBlueprintMos => gmosbp.nodAndShuffle ? GmosMosNsOverheads | GmosMosOverheads


    // Any other configuration is unsupported.
    case _ => None
  }
}
