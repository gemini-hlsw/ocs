package edu.gemini.dataman.app

import edu.gemini.dataman.app.ObsLogActions.FitsFile

import edu.gemini.dataman.core.GsaRecord
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.dataset.{DatasetMd5, DatasetQaState, DatasetGsaState, DatasetLabel}
import edu.gemini.spModel.obslog.ObsLog

import java.time.Instant

import org.scalacheck.Prop._
import scala.collection.JavaConverters._
import scalaz.{\/-, -\/}


object ObsLogActionsSpec extends TestSupport {

  "FitsFile regex" should {
    "match all strings ending with .fits suffix" in
      forAll { (s: String) =>
        s"$s.fits" match {
          case FitsFile(n) => s == n
          case _           => false
        }
      }

    "match all strings not ending with .fits suffix" in
      forAll { (s: String) =>
        s.endsWith(".fits") || (s match {
          case FitsFile(n) => s == n
          case _           => false
        })
      }

    "ignore intermediate .fits sub-strings" in
      forAll { (s: String) =>
        val fn = s".fits$s.fits"
        s"$fn.fits" match {
          case FitsFile(n) => fn == n
          case _           => false
        }
      }
  }


  val DatasetLabelKey = new ItemKey("observe:dataLabel")

  // All missing dataset labels in the given programs.
  def missingLabels(progs: List[ISPProgram]): List[DatasetLabel] =
    progs.flatMap { p =>
      p.getObservations.asScala.flatMap { o =>
        Option(ObsLog.getIfExists(o)).toList.flatMap { log =>
          val cs  = ConfigBridge.extractSequence(o, null, ConfigValMapInstances.IDENTITY_MAP)
          val all = cs.getDistinctItemValues(DatasetLabelKey).collect {
            case lab: DatasetLabel => lab
            case s: String         => new DatasetLabel(s)
          }
          (all.toSet -- log.getDatasetLabels.asScala).toList
        }
      }
    }

  "ObsLogActions" should {
    "strip .fits from discovered datasets" ! forAllPrograms { (odb, progs) =>

      val prefix = "GN20160207S"

      val labs = missingLabels(progs).sorted
      val recs = labs.zipWithIndex.map { case (lab, index) =>
          GsaRecord(Some(lab), f"$prefix%s${index+1}%03d.fits", DatasetGsaState(DatasetQaState.UNDEFINED, Instant.now(), DatasetMd5.empty))
      }

      val act  = new ObsLogActions(odb).updateSummit(recs)

      act.unsafeRun match {
        case -\/(f) =>
          failure(f.explain)
          false
        case \/-((_, exs)) =>
          exs.sortBy(_.dataset.getLabel).zipWithIndex.forall { case (ex, index) =>
            ex.dataset.getDhsFilename == f"$prefix%s${index+1}%03d"
          }
      }
    }
  }

}
