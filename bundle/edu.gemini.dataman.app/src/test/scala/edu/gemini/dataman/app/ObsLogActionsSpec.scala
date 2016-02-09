package edu.gemini.dataman.app

import edu.gemini.dataman.app.ObsLogActions.FitsFile

import edu.gemini.gsa.query.GsaRecord
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ItemKey
import edu.gemini.spModel.dataset.{DatasetExecRecord, DatasetMd5, DatasetQaState, DatasetGsaState, DatasetLabel}
import edu.gemini.spModel.obslog.ObsLog
import org.scalacheck.Prop

import java.time.Instant

import org.scalacheck.Prop._
import scala.collection.JavaConverters._
import scalaz.{\/-, -\/}


object ObsLogActionsSpec extends TestSupport {

  "FitsFile regex" should {
    "match the prefix of all strings that don't contain vertical whitespace" in
      forAll { (s: String) =>
        s"$s.fits" match {
          case FitsFile(n) => s == n
          case _           => s.exists(_.isWhitespace)
        }
      }
    "match any strings not ending with .fits suffix and not containing vertical whitespace" in
      forAll { (s: String) =>
        s.endsWith(".fits") || (s match {
          case FitsFile(n) => s == n
          case _           => s.exists(_.isWhitespace)
        })
      }

    "ignore intermediate .fits sub-strings in all strings not containing vertical whitespace" in
      forAll { (s: String) =>
        val fn = s".fits$s.fits"
        s"$fn.fits" match {
          case FitsFile(n) => fn == n
          case _           => s.exists(_.isWhitespace)
        }
      }
  }


  val FilenamePrefix   = "GN20160207S"
  val DatasetLabelKey  = new ItemKey("observe:dataLabel")
  val ObservationIdKey = new ItemKey("ocs:observationId")

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

  def test(p: (List[ISPProgram], List[GsaRecord], List[DatasetExecRecord]) => Boolean): Prop =
    forAllPrograms { (odb, progs) =>

      val labs = missingLabels(progs).sorted
      val recs = labs.zipWithIndex.map { case (lab, index) =>
        GsaRecord(Some(lab), f"$FilenamePrefix%s${index+1}%03d.fits", DatasetGsaState(DatasetQaState.UNDEFINED, Instant.now(), DatasetMd5.empty))
      }

      new ObsLogActions(odb).updateSummit(recs).unsafeRun match {
        case -\/(f) =>
          failure(f.explain)
          false
        case \/-((_, exs)) =>
          p(progs, recs, exs)
      }
    }

  "ObsLogActions" should {
    "strip .fits from discovered datasets" ! test { (progs, gsaRecs, execRecs) =>
      execRecs.sortBy(_.dataset.getLabel).zipWithIndex.forall { case (ex, index) =>
        ex.dataset.getDhsFilename == f"$FilenamePrefix%s${index+1}%03d"
      }
    }

    "add a config for missing datasets" ! test { (progs, gsaRecs, execRecs) =>
      gsaRecs.forall { gsaRec =>
        gsaRec.label.forall { label =>
          val oid = label.getObservationId
          val pid = oid.getProgramID

          progs.find(_.getProgramID == pid).exists { p =>
            p.getAllObservations.asScala.find(_.getObservationID == oid).exists { o =>
              Option(ObsLog.getIfExists(o)).exists { log =>
                Option(log.getExecRecord.getConfigForDataset(label)).exists { config =>
                  config.getItemValue(ObservationIdKey) == oid.stringValue
                }
              }
            }
          }
        }
      }
    }
  }
}
