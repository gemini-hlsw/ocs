package edu.gemini.spdb.shell.misc

import edu.gemini.pot.sp.{ISPObservation, ISPProgram}
import edu.gemini.spModel.rich.pot.sp.obsWrapper
import edu.gemini.spModel.target.EphemerisPurge

import scala.collection.JavaConverters._
import scalaz._, Scalaz._

/** Shell command for purging ephemeris data. */
object EphemerisPurgeCommand {
  sealed trait PurgeOption {
    def include(o: ISPObservation): Boolean = this match {
      case All          => true
      case ObservedOnly => o.isObserved
    }

    def displayValue: String = this match {
      case All          => "all"
      case ObservedOnly => "observed"
    }
  }

  object PurgeOption {
    val AllValues = List(All, ObservedOnly)

    def fromDisplayValue(s: String): Option[PurgeOption] =
      AllValues.find(_.displayValue === s)

    def usageString: String =
      AllValues.map(_.displayValue).mkString("[", " | ", "]")
  }

  /** Option to purge ephemeris data from all observations in the program. */
  case object All extends PurgeOption

  /** Option to purge ephemeris data from just the observed observations in the program. */
  case object ObservedOnly extends PurgeOption


  /** Purges ephemeris data from observations in the provided program. */
  def apply(p: ISPProgram, purgeOption: PurgeOption): String = {
    val updates = for {
      o  <- p.getAllObservations.asScala.filter(purgeOption.include)
      io <- EphemerisPurge.purge(o)
    } yield io

    updates.foreach(_.unsafePerformIO())

    s"Purged ephemeris data from ${updates.size} ${(purgeOption == ObservedOnly) ? "OBSERVED " | ""}observation(s)."
  }
}
