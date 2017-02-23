package edu.gemini.model.p1.overheads

import edu.gemini.model.p1.immutable._

import scalaz._
import Scalaz._
import squants.time._

import scala.annotation.tailrec

// This is not the ideal package for this to live in, but to avoid circular bundle references, we put it here.
sealed trait Overheads {
  def partnerOverheadFraction: Double // fpart
  def acquisitionOverhead: Time       // acqover
  def otherOverheadFraction: Double   // fother

  def calculate(intTime: TimeAmount): ObservationTimes = {
    val intTimeHrs  = intTime.toHours.value
    val progTimeHrs = calcProgTime(intTimeHrs)
    val partTimeHrs = progTimeHrs * partnerOverheadFraction
    ObservationTimes(TimeAmount(progTimeHrs, TimeUnit.HR), TimeAmount(partTimeHrs, TimeUnit.HR))
  }

  // This is needed for 2017A to 2017B migration, as time in former proposals will be migrated to progTime, and
  // thus intTime - the value that is user modifiable - must be calculated from this.
  def intTimeFromProgTime(progTime: TimeAmount): TimeAmount = {
    val progTimeHrs = progTime.toHours.value
    val numAcqs     = numAcqsFromProgTimeInHrs(progTimeHrs)
    val intTimeHrs  = (progTimeHrs - numAcqs * acquisitionOverhead.toHours)/(1 + otherOverheadFraction)
    TimeAmount(intTimeHrs, TimeUnit.HR)
  }

  // Iterative method for calculating the program time in hours iteratively through the number of acquisitions.
  private def calcProgTime(intTime: Double): Double = {
    val overheadTimeHrs = intTime * (1 + otherOverheadFraction)

    @tailrec
    def calcProgTimeIter(numAcqsOld: Int, numAcqsNew: Int, progTime: Double, iteration: Int): Double = {
      // If the number of acquisitions is stable or we have
      if (numAcqsOld == numAcqsNew || iteration > 10)
        progTime
      else {
        val progTimeNew = numAcqsNew * acquisitionOverhead.toHours + overheadTimeHrs
        calcProgTimeIter(numAcqsNew, numAcqsFromProgTimeInHrs(progTimeNew), progTimeNew, iteration + 1)
      }
    }

    calcProgTimeIter(0, numAcqsFromProgTimeInHrs(overheadTimeHrs), 0.0, 0)
  }

  // Calculate the number of acquisitions from program time in hours.
  private def numAcqsFromProgTimeInHrs(progTime: Double): Int =
    (progTime / Overheads.visTimeHrs).toInt + ((progTime % Overheads.visTimeHrs > 0) ? 1 | 0)
}

// Due to the large number of possible blueprint bases and how each one requires different configuration params
// to determine the overheads, it seems infeasible to read this information from a file, so for now it is hard-coded
// and will require changing here if these values change.
object Overheads extends (BlueprintBase => Option[Overheads]) {
  // t_vis as per REL-2985.
  // visTime is in hours already: if this changes, this calculation should change to ensure no loss of data.
  lazy val visTime    = Hours(2)
  lazy val visTimeHrs = Overheads.visTime.toHours

  private case class SimpleOverheads(override val partnerOverheadFraction: Double,
                                     acquisitionOverheadMins: Long,
                                     override val otherOverheadFraction: Double) extends Overheads {
    override val acquisitionOverhead = Minutes(acquisitionOverheadMins)
  }

  // GMOS overheads are the same between sites.
  private lazy val GmosImagingOverheads    = SimpleOverheads(0.00,  6, 0.144).some
  private lazy val GmosLongslitOverheads   = SimpleOverheads(0.10, 16, 0.034).some
  private lazy val GmosLongslitNsOverheads = SimpleOverheads(0.10, 16, 0.271).some
  private lazy val GmosMosOverheads        = SimpleOverheads(0.10, 18, 0.028).some
  private lazy val GmosMosNsOverheads      = SimpleOverheads(0.10, 18, 0.271).some
  private lazy val GmosIfuOverheads        = SimpleOverheads(0.10, 18, 0.083).some
  private lazy val GmosIfuNsOverheads      = SimpleOverheads(0.10, 18, 0.311).some

  def apply(b: BlueprintBase): Option[Overheads] = b match {
    case _: Flamingos2BlueprintImaging  => SimpleOverheads(0.10,  6, 0.490).some
    case _: Flamingos2BlueprintLongslit => SimpleOverheads(0.25, 20, 0.283).some
    case _: Flamingos2BlueprintMos      => SimpleOverheads(0.25, 30, 0.283).some

    case _: GnirsBlueprintImaging       => SimpleOverheads(0.10, 10, 0.193).some
    case _: GnirsBlueprintSpectroscopy  => SimpleOverheads(0.25, 15, 0.080).some

    case _: NifsBlueprint               => SimpleOverheads(0.25, 11, 0.175).some
    case _: GsaoiBlueprint              => SimpleOverheads(0.00, 30, 0.875).some
    case _: GracesBlueprint             => SimpleOverheads(0.00, 10, 0.036).some
    case _: GpiBlueprint                => SimpleOverheads(0.05, 10, 0.333).some
    case _: PhoenixBlueprint            => SimpleOverheads(0.25, 20, 0.021).some
    case _: TexesBlueprint              => SimpleOverheads(0.00, 20, 0.022).some

    case _: DssiBlueprint               => new SimpleOverheads(0.00, 10, 0.010).some
    case _: VisitorBlueprint            => new SimpleOverheads(0.00, 10, 0.100).some


    // NIRI relies on whether or not AO is being used.
    case nbp: NiriBlueprint             => SimpleOverheads(0.10, nbp.altair.ao.toBoolean ? 10 | 6, 0.193).some


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
