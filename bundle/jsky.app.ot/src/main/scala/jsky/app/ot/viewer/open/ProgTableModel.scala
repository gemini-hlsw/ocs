package jsky.app.ot.viewer.open

import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.pot.spdb.{IDBDatabaseService, IDBQueryRunner}
import edu.gemini.pot.sp.{ISPProgram, SPNodeKey}
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Conflicting, Newer, Older}
import edu.gemini.shared.util.immutable.MapOp
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.sp.vcs2.VcsFailure
import edu.gemini.sp.vcs2.VcsFailure.VcsException
import edu.gemini.spModel.core.{Peer, ProgramId, SPProgramID, VersionException}
import edu.gemini.spModel.util.DBProgramInfo
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.trpc.client.TrpcClient
import edu.gemini.shared.gui.ErrorBoxWithHyperlink
import jsky.app.ot.OT
import jsky.app.ot.shared.spModel.util.DBProgramListFunctor
import jsky.app.ot.vcs.VcsOtClient
import jsky.app.ot.viewer.DBProgramChooserFilter
import jsky.util.gui.DialogUtil
import java.io.{IOException, InvalidClassException}
import java.net.{ConnectException, SocketTimeoutException}
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import javax.swing.table.AbstractTableModel

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.Swing._
import scala.util.{Failure, Success}
import scalaz.{-\/, \/-}

object ProgTableModel {
  lazy val Log = java.util.logging.Logger.getLogger(getClass.getName)
  type Elem = Either[ISPProgram, DBProgramInfo]
}

// Our table model
class ProgTableModel(filter: DBProgramChooserFilter, db: IDBDatabaseService, auth: KeyChain) extends AbstractTableModel {

  val vcs = VcsOtClient.ref.map(_.reg)

  import ProgTableModel._

  // Members
  lazy val cols = Seq(("Program ID", 100), ("Title", 355), ("Size", 45), ("Changes", 100))

  // Mutable Cells
  private val locals   = new AtomicReference[Vector[ISPProgram]](Vector.empty)
  private val remotes  = new AtomicReference[Map[Peer, Vector[DBProgramInfo]]](Map())
  private val elems    = new AtomicReference[Vector[Elem]](Vector.empty)
  private val statuses = new AtomicReference[Map[SPNodeKey, VersionComparison]](Map())
  private val failures = new AtomicReference[Map[SPNodeKey, VcsFailure]](Map())

  // Initialization
  filter.addActionListener(ActionListener(_ => fireTableDataChanged()))
  refresh(full = true)

  /** Return the element at the given index, if the index is valid. */
  def get(i: Int): Option[Elem] =
    elems.get.lift(i)

  /** Return the index of the program with the specified program Id, if any. */
  def indexOf(id: SPNodeKey): Option[Int] =
    Some(elems.get.map(_.fold(_.getNodeKey, _.nodeKey)).indexOf(id)).filter(_ >= 0)

  /** Return the program info at the given index, if the index is valid. */
  def getInfo(i: Int): Option[DBProgramInfo] =
    get(i).map(_.fold(_.getProgInfo(db), identity))

  /** Return the program at the given index, if the index is valid AND it's local. */
  def getProg(i: Int): Option[ISPProgram] =
    get(i).flatMap(_.left.toOption)

  /** Return the info and remote information at the given index, if the index is valid AND it's remote. */
  def getRemote(i: Int): Option[(DBProgramInfo, Peer)] =
    getInfo(i).flatMap(i => remotes.get.find(_._2.contains(i)).map(p => (i, p._1)))

  def getStatus(i: Int): Option[VersionComparison] =
    get(i).flatMap(_.left.toOption).flatMap { p => statuses.get().get(p.getNodeKey) }

  def getStatus(k: SPNodeKey): Option[VersionComparison] =
    statuses.get().get(k)

  lazy val getColumnCount: Int =
    cols.length

  override def getColumnName(column: Int): String =
    cols(column)._1

  def getRowCount: Int =
    elems.get.length

  def statusMessage(id:SPProgramID): Option[String] =
    locals.get.find(_.getProgramID == id).flatMap(statusMessage)

  def statusMessage(p: ISPProgram): Option[String] = for {
    reg <- vcs
    _   <- reg.registration(p.getProgramID)
  } yield statuses.get.get(p.getNodeKey).map {
      case Newer       => "Outgoing"
      case Conflicting => "Incoming, Outgoing"
      case Older       => "Incoming"
      case _           => "None"
    }.map { s =>
      if (p.hasConflicts) s + ", Conflicts" else s
    }.orElse(failures.get.get(p.getNodeKey).map {
      case VcsException(_: IOException) => "Cannot Connect"
      case _                            => "Error" // for now
    }).getOrElse("Pending...")


  def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef =
    getInfo(rowIndex).map { p =>
      columnIndex match {
        case 0 => p.programID
        case 1 => p.programName
        case 2 => Long.box(p.size)
        case 3 => statusMessage(p.programID).orNull
      }
    }.orNull

  override def getColumnClass(columnIndex: Int): Class[_] =
    columnIndex match {
      case 0 => classOf[SPProgramID]
      case 1 => classOf[String]
      case 2 => classOf[java.lang.Long]
      case 3 => classOf[String]
    }

  // Data changed is the trigger to recalculate the element set.
  override def fireTableDataChanged(): Unit = {
    elems set {

      val locals0 = locals.get
      val ls: Vector[Elem] = locals0.map(Left(_))
      val rs: Vector[Elem] = remotes.get.values.flatten.filterNot { info =>
        locals0.map(_.getProgramID).contains(info.programID)
      }.map(Right(_)).toVector

      val all = (ls ++ rs).sortBy(_.fold(p => Option(p.getProgramID), i => Option(i.programID)))

      // massage to work with filter.filter below
      // zip with Option[ProgramId] to form Vector[(Option[ProgramId], Elem)]
      val pidAll = all.map { elem =>
        (elem.fold(p => Option(p.getProgramID), i => Option(i.programID)).map { pid =>
          ProgramId.parse(pid.stringValue())
        }, elem)
      }

      // filter massaged Vector and then discard the Option[ProgramId]
      filter.filter(db, pidAll.asJava, new MapOp[(Option[ProgramId], Elem), Option[ProgramId]] {
        def apply(p: (Option[ProgramId], Elem)): Option[ProgramId] = p._1
      }).asScala.unzip._2.toVector
    }
    super.fireTableDataChanged()
  }

  /** Recompute table contents. */
  def refresh(full: Boolean): Unit = {
    // The peer associated with the currently selected key, if any.
    val selectedPeer = auth.selection.unsafeRun.toOption.flatten.map(_._1)

    // Refresh local programs from local DB, and from VCS
    def refreshLocal(ignored: Any = null): Unit = {
      val func  = new DBProgramListFunctor(DBProgramListFunctor.EMPTY_PROGRAM_ID_OR_READABLE)
      val func0 = db.getQueryRunner(OT.getUser).queryPrograms(func)

      // We've already filtered out local programs associated with the selected
      // peer's database for which the current key doesn't provide access.
      // Additionally filter out local programs associated with other peers.
      val progs = func0.getList.asScala.toVector.filter { info =>
        val programPeer = for {
          pid  <- Option(info.programID)
          reg  <- vcs
          peer <- reg.registration(pid)
        } yield peer

        programPeer.forall(selectedPeer.contains)
      }

      locals.set(progs.map { info =>
        db.lookupProgram(info.nodeKey)
      })

      fireTableDataChanged()

      // Reset VCS status
      statuses.set(Map())
      failures.set(Map())

      // Update VCS for each program
      locals.get.foreach(updateVCS)
    }

    // Refresh the program list from remote sites
    def refreshRemote(): Unit =
      selectedPeer.foreach(updateRemote)

    // Refresh VCS status for a single program
    def updateVCS(p: ISPProgram): Unit = {
      def fireTDC(): Unit = Swing.onEDT { super.fireTableDataChanged() }

      for {
        pid    <- Option(p.getProgramID)
        reg    <- vcs
        loc    <- reg.registration(pid)
        client <- VcsOtClient.ref
      } client.version(pid).forkAsync {
        case \/-(vm) =>
          statuses.modify(_ + (p.getNodeKey -> VersionMap.compare(p.getVersions, vm)))
          fireTDC()
        case -\/(f)  =>
          failures.modify(_ + (p.getNodeKey -> f))
          fireTDC()
      }
    }


    // Refresh program list from a single site (synchronous)
    def updateRemote(peer: Peer): Unit = {

      def logException(t: Throwable) {
        val msg = "Couldn't update program list from %s:%d: %s".format(peer.host, peer.port, t.getMessage)
        t match {
          case e: VersionException =>
            Log.log(Level.WARNING, msg)
            ErrorBoxWithHyperlink.showErrorBoxWithLink(e.getHtmlMessage(s"${peer.host}:${peer.port}"))

          case _: InvalidClassException =>
            val versionMsg =
              s"""|Your software is incompatible with the service provided by ${peer.host}:${peer.port}.
                |You probably need to upgrade your software to a more recent version.""".stripMargin
            Log.log(Level.WARNING, msg)
            DialogUtil.error(versionMsg)

          case _: SocketTimeoutException =>
            Log.log(Level.WARNING, msg)

          case _: ConnectException =>
            Log.log(Level.WARNING, msg)

          case _ =>
            Log.log(Level.WARNING, msg, t)
        }
      }

      TrpcClient(peer).withKeyChain(OT.getKeyChain) future { r =>
        val func = new DBProgramListFunctor(DBProgramListFunctor.NON_EMPTY_PROGRAM_ID_AND_READABLE)
        val func0 = r[IDBQueryRunner].queryPrograms(func)
        func0.getList.asScala.toVector
      } onComplete {
        case Success(progList) =>
          remotes.modify(_ + (peer -> progList))
          Swing.onEDT { fireTableDataChanged() }
        case Failure(t)        =>
          logException(t)
      }
    }

    // Do it, clearing remotes if requested
    if (full) remotes.set(Map.empty)
    refreshLocal()
    refreshRemote()
  }
}
