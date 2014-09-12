package jsky.app.ot.gemini.editor

import jsky.util.DateUtil
import javax.swing.table.{TableColumn, DefaultTableModel}
import edu.gemini.sp.vcs.reg.VcsRegistrar
import scala.concurrent.ExecutionContext.Implicits.global
import edu.gemini.util.trpc.client.TrpcClient
import edu.gemini.sp.vcs.VcsServer
import java.util.logging.{Level, Logger}
import edu.gemini.pot.client.SPDB
import edu.gemini.sp.vcs.log._
import edu.gemini.sp.vcs.VcsFailure.VcsException
import scala.util.Success
import edu.gemini.sp.vcs.VersionControlSystem
import scala.util.Failure
import scala.swing.Swing
import jsky.app.ot.OT

object EdProgramHelper {

  val Log = Logger.getLogger(getClass.getName)

  case class ColumnInfo(name: String, prefWidth: Int, minWidth: Int, maxWidth: Int)

  val columnInfos = Array(
    ColumnInfo("Start", 130, 130, 140),
    ColumnInfo("End", 130, 130, 140),
    //    ColumnInfo("F", 50, 50, 100),
    ColumnInfo("Syncs", 60, 60, 60),
    ColumnInfo("Keys Used", 600, 100, 10000)
  )

  // OCSINF-280.  Dropping the F (fetch) and S (store) columns in favor of a
  // single "Syncs" count, which is really just the store count since every
  // store requires you to have incorporated all the changes in the database
  // first.

  implicit class ToRowOps(es: VcsEventSet) {
    def toRow: Array[Object] = Array[Object](
    DateUtil.formatUTC(es.timestamps._1),
    DateUtil.formatUTC(es.timestamps._2),
    //    { val a = es.ops.get(OpFetch).getOrElse(0); a : Integer }, // yes, the tempvar is needed
    {
      val a = es.ops.get(OpStore).getOrElse(0); a: Integer
    },
    es.principals.map(_.getName).mkString(", "))
  }

  def updateHistoryTable(ed: EdProgram, vcsReg: VcsRegistrar): Unit = {

    // Set the widths of the table columns to leave room for the comments
    def setColumnWidths() {
      val model = ed._w.historyTable.getColumnModel
      columnInfos.zipWithIndex.foreach {
        case (info, i) =>
          setColumnWidth(model.getColumn(i), info.prefWidth, info.minWidth, info.maxWidth)
      }
    }

    def setColumnWidth(col: TableColumn, prefWidth: Int, minWidth: Int, maxWidth: Int) {
      col.setPreferredWidth(prefWidth)
      col.setMinWidth(minWidth)
      col.setMaxWidth(maxWidth)
    }

    def model(es: List[VcsEventSet]) =
      new DefaultTableModel(es.map(_.toRow).toArray[Array[Object]], // ugh
        columnInfos.map(_.name).toArray[Object]) {
        override def isCellEditable(row: Int, column: Int) = false
      }

    def update(es: List[VcsEventSet]) {
      ed._w.historyTable.setModel(model(es))
      setColumnWidths()
    }

    // Remove the old values, possibly from some other program.
    update(Nil)

    for {
      pid <- Option(ed.getProgram).map(_.getProgramID)
      peer <- vcsReg.registration(pid)
    } TrpcClient(peer).withKeyChain(OT.getKeyChain) future {
      r =>
        val remote = VersionControlSystem(SPDB.get(), r[VcsServer])
        remote.log(pid, 0, 100).fold({
          case VcsException(e) => throw e
          case f => throw new RuntimeException(f.toString)
        }, identity)
    } onComplete {
      case Failure(e) =>
        Log.log(Level.WARNING, s"Failed to fetch VCS history for $pid", e)

      case Success((es, more)) =>
        Log.info(s"Got ${es.length} history items for $pid, more == $more")

        Swing.onEDT {
          // The program we are viewing could have changed while we were
          // out getting the log.
          for {
            p <- Option(ed.getProgram)
            cp <- Option(p.getProgramID)
            if cp == pid
          } {
            update(es)
          }
        }
    }
  }

}
