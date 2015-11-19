package edu.gemini.dataman.osgi

import edu.gemini.dataman.app.{DmanActionExec, GsaPollActions}
import edu.gemini.dataman.core._
import edu.gemini.dataman.query.{GsaRecordQuery, GsaQaUpdateQuery, GsaResponse, TimeFormat}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel}

import scalaz._
import Scalaz._

/** Simple OSGi shell commands that expose the GSA queries (mostly for
  * testing and debugging). */
sealed trait GsaCommands {
  def gsa(args: Array[String]): String
}

object GsaCommands {
  def apply(config: DmanConfig, odb: IDBDatabaseService): GsaCommands =
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
          |  gsa sync <list-query>                    Fetches a listing and applies any changes to the program.
          |  gsa set <qa-state> <dataset-label> ...   Sets one or more dataset QA states.
          |
        """.stripMargin

      val exec        = DmanActionExec(config, odb)
      val summitSync  = GsaPollActions(config.summitHost, config.site, odb)
      val archiveSync = GsaPollActions(config.archiveHost, config.site, odb)

      override def gsa(args: Array[String]): String =
        args.toList match {
          case List("list", a)            => list(a)
          case List("sync", a)            => sync(a)
          case "set" :: qaState :: labels => set(qaState, labels)
          case _                          => help
        }

      def list(arg: String): String = {
        def format(lst: List[GsaRecord]): String = {
          val rows = List("Dataset Label", "QA State", "Time", "MD5", "Filename") :: lst.sortBy(_.label).map { f =>
            List(f.label.map(_.toString) | "", f.state.qa.displayValue, TimeFormat.format(f.state.timestamp), f.state.md5.hexString, f.filename)
          }
          rows.transpose.map { col =>
            val w = col.maxBy(_.length).length
            col.map(_.padTo(w, " ").mkString)
          }.transpose.map(_.mkString("  ")).mkString("\n")
        }

        // Figure out which query to run, if any.
        val f: Option[GsaRecordQuery => GsaResponse[List[GsaRecord]]] = arg match {
          case "tonight" => Some(_.tonight)
          case "week"    => Some(_.thisWeek)
          case id0       => DmanId.parse(id0).map {
            case DmanId.Dset(lab) => (q: GsaRecordQuery) => q.dataset(lab).map(_.toList)
            case DmanId.Obs(oid)  => (q: GsaRecordQuery) => q.observation(oid)
            case DmanId.Prog(pid) => (q: GsaRecordQuery) => q.program(pid)
          }
        }

        // Send the request to the server and format the results in a table.
        f.fold(help) { fn =>
          fn(GsaRecordQuery(config.summitHost, config.site)).fold(_.explain, format)
        }
      }

      def sync(arg: String): String = {
        // Figure out which update to run, if any.
        val f: Option[GsaPollActions => DmanAction[DatasetUpdates]] = arg match {
          case "tonight" => Some(_.tonight)
          case "week"    => Some(_.thisWeek)
          case id0       => DmanId.parse(id0).map {
            case DmanId.Dset(lab) => (p: GsaPollActions) => p.dataset(lab)
            case DmanId.Obs(oid)  => (p: GsaPollActions) => p.observation(oid)
            case DmanId.Prog(pid) => (p: GsaPollActions) => p.program(pid)
          }
        }

        f.fold(help) { fn =>
          exec.fork(fn(summitSync))
          s"Sync $arg forked."
        }
      }

      def set(qaString: String, labelStrings: List[String]): String = {
        // Parse the arguments into List[GsaQaUpdate.Request] if possible.
        val requests = for {
          qa <- DatasetQaState.values.find(_.displayValue.toLowerCase == qaString.toLowerCase) \/> s"Could not parse `$qaString` as a QA state."
          ls <- labelStrings.map { s => \/.fromTryCatch(new DatasetLabel(s)).leftMap(_ => s"Could not parse `$s` as a dataset label.") }.sequenceU
        } yield ls.map { lab => QaRequest(lab, qa) }

        // Send the update request to the server.
        val responses = requests.flatMap { rs =>
          GsaQaUpdateQuery(config.summitHost, config.site, config.gsaAuth).setQaStates(rs).leftMap(_.explain)
        }

        // Format the error messages, if any.
        responses.fold(identity, _.sortBy(_.label).collect {
          case QaResponse(label, Some(m)) => s"$label: $m"
        }.mkString("\n"))
      }
    }
}
