package edu.gemini.model.p1.overheads

import edu.gemini.model.p1.immutable._

import scalaz._
import Scalaz._
import squants.time._

import scala.annotation.tailrec

// This is not the ideal package for this to live in, but to avoid circular bundle references, we put it here.
// Much of this is rendered obsolete due to the change from REL-2985 to REL-2926, but I am leaving it here in case
// it is needed in the future by ITC and also because of time constraints.
sealed trait Overheads {
  // REL-2985 -> REL-2926: acquisitionOverhead and otherOverheadFraction are no longer used.
  def partnerOverheadFraction: Double // fpart

  // Everything was, per REL-2985, formerly a
//  def calculateFromIntTime(intTime: TimeAmount): ObservationTimes = {
//    val intTimeHrs  = intTime.toHours.value
//    val progTimeHrs = calcProgTime(intTimeHrs)
//    val partTimeHrs = progTimeHrs * partnerOverheadFraction
//    ObservationTimes(TimeAmount(progTimeHrs, TimeUnit.HR), TimeAmount(partTimeHrs, TimeUnit.HR))
//  }

  def calculate(progTime: TimeAmount): ObservationTimes = {
    val progTimeHrs = progTime.toHours.value
    val partTimeHrs = progTimeHrs * partnerOverheadFraction
    ObservationTimes(TimeAmount(progTimeHrs, TimeUnit.HR), TimeAmount(partTimeHrs, TimeUnit.HR))
  }

  // This was needed for 2017A to 2017B migration as per REL-2985, as time in former proposals will be migrated to
  // progTime, and previously, all times were based on intTime which was calculated from progTime.
  // This has been reversed and we are no longer using integration time in the PIT as per REL-2926, making this
  // obsolete and unused.
//  def intTimeFromProgTime(progTime: TimeAmount): TimeAmount = {
//    val progTimeHrs = progTime.toHours.value
//    val numAcqs     = numAcqsFromProgTimeInHrs(progTimeHrs)
//    val intTimeHrs  = (progTimeHrs - numAcqs * acquisitionOverhead.toHours)/(1 + otherOverheadFraction)
//    TimeAmount(intTimeHrs, TimeUnit.HR)
//  }

  // Iterative method for calculating the program time in hours iteratively through the number of acquisitions.
  // No longer used in transition from REL-2985 to REL-2926.
//  private def calcProgTime(intTime: Double): Double = {
//    val overheadTimeHrs = intTime * (1 + otherOverheadFraction)
//
//    @tailrec
//    def calcProgTimeIter(numAcqsOld: Int, numAcqsNew: Int, progTime: Double, iteration: Int): Double = {
//      // If the number of acquisitions is stable or we have
//      if (numAcqsOld == numAcqsNew || iteration > 10)
//        progTime
//      else {
//        val progTimeNew = numAcqsNew * acquisitionOverhead.toHours + overheadTimeHrs
//        calcProgTimeIter(numAcqsNew, numAcqsFromProgTimeInHrs(progTimeNew), progTimeNew, iteration + 1)
//      }
//    }
//
//    calcProgTimeIter(0, numAcqsFromProgTimeInHrs(overheadTimeHrs), 0.0, 0)
//  }
//
//  // Calculate the number of acquisitions from program time in hours.
//  private def numAcqsFromProgTimeInHrs(progTime: Double): Int =
//    (progTime / Overheads.visTimeHrs).toInt + ((progTime % Overheads.visTimeHrs > 0) ? 1 | 0)
}

// Due to the large number of possible blueprint bases and how each one requires different configuration params
// to determine the overheads, it seems infeasible to read this information from a file, so for now it is hard-coded
// and will require changing here if these values change.
object Overheads extends (BlueprintBase => Option[Overheads]) {
  // t_vis as per REL-2985. No longer needed by switch to REL-2926.
//  lazy val visTime    = Hours(2)
//  lazy val visTimeHrs = Overheads.visTime.toHours

  private case class SimpleOverheads(partnerOverheadFraction: Double) extends Overheads

  // Empty overheads for instruments from exchange partners, e.g. Keck and Subaru.
  private lazy val EmptyOverheads          = SimpleOverheads(0.00).some

  // GMOS overheads are the same between sites.
  private lazy val GmosImagingOverheads    = SimpleOverheads(0.00).some
  private lazy val GmosLongslitOverheads   = SimpleOverheads(0.10).some
  private lazy val GmosLongslitNsOverheads = SimpleOverheads(0.10).some
  private lazy val GmosMosOverheads        = SimpleOverheads(0.10).some
  private lazy val GmosMosNsOverheads      = SimpleOverheads(0.10).some
  private lazy val GmosIfuOverheads        = SimpleOverheads(0.10).some
  private lazy val GmosIfuNsOverheads      = SimpleOverheads(0.10).some

  def apply(b: BlueprintBase): Option[Overheads] = b match {
    case _: Flamingos2BlueprintImaging  => SimpleOverheads(0.10).some
    case _: Flamingos2BlueprintLongslit => SimpleOverheads(0.25).some
    case _: Flamingos2BlueprintMos      => SimpleOverheads(0.25).some

    case _: GnirsBlueprintImaging       => SimpleOverheads(0.10).some
    case _: GnirsBlueprintSpectroscopy  => SimpleOverheads(0.25).some

    case _: NifsBlueprintBase           => SimpleOverheads(0.25).some
    case _: GsaoiBlueprint              => SimpleOverheads(0.00).some
    case _: GracesBlueprint             => SimpleOverheads(0.00).some
    case _: GpiBlueprint                => SimpleOverheads(0.05).some
    case _: PhoenixBlueprint            => SimpleOverheads(0.25).some
    case _: TexesBlueprint              => SimpleOverheads(0.00).some

    case _: DssiBlueprint               => SimpleOverheads(0.00).some
    case _: VisitorBlueprint            => SimpleOverheads(0.00).some
    case _: AlopekeBlueprint            => SimpleOverheads(0.00).some
    case _: ZorroBlueprint              => SimpleOverheads(0.00).some
    case _: IgrinsBlueprint             => SimpleOverheads(0.20).some
    case _: MaroonXBlueprint            => SimpleOverheads(0.00).some

    // NIRI relies on whether or not AO is being used.
    case _: NiriBlueprint             => SimpleOverheads(0.10).some


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

    // Keck and Subaru instruments have no overheads associated with them.
    case _: KeckBlueprint   => EmptyOverheads
    case _: SubaruBlueprint => EmptyOverheads

    // Any other configuration is unsupported.
    case _ => None
  }
}
