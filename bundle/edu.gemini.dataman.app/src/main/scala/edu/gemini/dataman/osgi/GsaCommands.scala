package edu.gemini.dataman.osgi

import edu.gemini.dataman.gsa.query.{GsaQaUpdateQuery, GsaQaUpdate, GsaQueryError, GsaFile, GsaResponse, GsaFileQuery, TimeFormat}
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.{Site, SPProgramID}
import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel}

import scalaz._
import Scalaz._

/** Simple OSGi shell commands that expose the GSA queries (mostly for
  * testing and debugging). */
sealed trait GsaCommands {
  def gsa(args: Array[String]): String
}

object GsaCommands {
  def apply(gsaHost: String, site: Site, auth: String): GsaCommands =
    new GsaCommands {

      val help =
        """
          |GsaServer Help
          |--------------
          |
          |Grammar:
          |
          |  <list-query> := <progid> | <obsid> | <dataset-label> | tonight | week
          |  <qa-state>   := Undefined | Pass | Usable | Fail | Check
          |
          |GSA Commands:
          |
          |  gsa list <list-query>                    Fetches a listing of the corresponding datasets.
          |  gsa set <qa-state> <dataset-label> ...   Sets one or more dataset QA states.
          |
        """.stripMargin

      sealed trait Id

      case class ProgId(pid: SPProgramID) extends Id
      case class ObsId(oid: SPObservationID) extends Id
      case class DatasetId(label: DatasetLabel) extends Id

      def parseId(s: String): Option[Id] = {
        def parse[A](c: String => A): Option[A] =
          \/.fromTryCatch { c(s) }.toOption

        parse(s => DatasetId(new DatasetLabel(s)))  orElse
         parse(s => ObsId(new SPObservationID(s)))  orElse
         parse(s => ProgId(SPProgramID.toProgramID(s)))
      }

      override def gsa(args: Array[String]): String =
        args.toList match {
          case List("list", a)            => list(a)
          case "set" :: qaState :: labels => set(qaState, labels)
          case _                          => help
        }

      def list(arg: String): String = {
        def format(lst: List[GsaFile]): String = {
          val rows = List("Dataset Label", "QA State", "Time", "Filename") :: lst.sortBy(_.label).map { f =>
            List(f.label.toString, f.qa.displayValue, TimeFormat.format(f.time), f.filename)
          }
          rows.transpose.map { col =>
            val w = col.maxBy(_.length).length
            col.map(_.padTo(w, " ").mkString)
          }.transpose.map(_.mkString("  ")).mkString("\n")
        }

        // Figure out which query to run, if any.
        val f: Option[GsaFileQuery => GsaResponse[List[GsaFile]]] = arg match {
          case "tonight" => Some(_.tonight)
          case "week"    => Some(_.thisWeek)
          case id0       => parseId(id0).map {
            case DatasetId(lab) => (q: GsaFileQuery) => q.dataset(lab).map(_.toList)
            case ObsId(oid)     => (q: GsaFileQuery) => q.observation(oid)
            case ProgId(pid)    => (q: GsaFileQuery) => q.program(pid)
          }
        }

        // Send the request to the server and format the results in a table.
        f.fold(help) { fn =>
          fn(GsaFileQuery(gsaHost, site)).fold(GsaQueryError.explain, format)
        }
      }

      def set(qaString: String, labelStrings: List[String]): String = {
        // Parse the arguments into List[GsaQaUpdate.Request] if possible.
        val requests = for {
          qa <- DatasetQaState.values.find(_.displayValue.toLowerCase == qaString.toLowerCase) \/> s"Could not parse `$qaString` as a QA state."
          ls <- labelStrings.map { s => \/.fromTryCatch(new DatasetLabel(s)).leftMap(_ => s"Could not parse `$s` as a dataset label.") }.sequenceU
        } yield ls.map { lab => GsaQaUpdate.Request(lab, qa) }

        // Send the update request to the server.
        val responses = requests.flatMap { rs =>
          GsaQaUpdateQuery(gsaHost, site, auth).setQaStates(rs).leftMap(GsaQueryError.explain)
        }

        // Format the error messages, if any.
        responses.fold(identity, _.sortBy(_.label).collect {
          case GsaQaUpdate.Response(label, Some(m)) => s"$label: $m"
        }.mkString("\n"))
      }
    }
}
