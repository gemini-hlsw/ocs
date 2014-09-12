package jsky.app.ot.vcs.vm

import edu.gemini.pot.sp.version.vmChecksum
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.spModel.core.{SPProgramID, Peer}
import edu.gemini.spModel.rich.pot.spdb._
import edu.gemini.util.security.auth.keychain.Action._
import edu.gemini.util.security.auth.keychain.KeyChain

import jsky.app.ot.{OT, OTOptions}
import jsky.app.ot.shared.vcs.VersionMapFunctor.VmUpdate
import jsky.app.ot.shared.vcs.VersionMapFunctor
import jsky.app.ot.viewer.SPViewer

import java.net.ConnectException
import java.util.logging.{Level, Logger}

import scala.actors.{TIMEOUT, DaemonActor}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Swing
import scala.util.{Success, Failure}
import scalaz.effect.IO
import scalaz.{-\/, \/-}

object VmUpdater {
  val Log = Logger.getLogger(this.getClass.getName)

  sealed trait Mode
  case object Staff extends Mode
  case object Pi extends Mode

  private def currentMode: Mode = if (OTOptions.isStaffGlobally) Staff else Pi

  sealed trait ProgramCategory
  case object Active extends ProgramCategory
  case object Inactive extends ProgramCategory

  def pollPeriodSec(mode: Mode, cat: ProgramCategory): Int =
    (mode, cat) match {
      case (Staff, Active)   =>    10  // 10 seconds
      case (Staff, Inactive) =>  3600  //  1 hour
      case (Pi,    Active)   =>   300  //  5 minutes
      case (Pi,    Inactive) => 86400  //  1 day
    }

  def pids(cat: ProgramCategory, db: IDBDatabaseService): Seq[SPProgramID] =
    cat match {
      case Active   =>
        // all programs open in a viewer ...
//        SPViewer.instances().asScala.toVector.flatMap { v =>
//          Option(v.getHistory).map(_.rootEntries.map(re => Option(re.root.getProgramID)).flatten).getOrElse(Vector.empty)
//        }
        // The current visible program
        SPViewer.instances().asScala.toVector.flatMap { v =>
          (for {
            p <- Option(v.getProgram)
            i <- Option(p.getProgramID)
          } yield i).toVector
        }

      case Inactive =>
        // all local programs not open in a viewer
        val allPids  = db.allPrograms(OT.getUser).map(p => Option(p.getProgramID)).flatten
        val openPids = pids(Active, db).toSet
        allPids.filter(p => !openPids.contains(p))
    }

  object STOP
  object UPDATE

  def updateAll(peer: Peer, pids: Seq[SPProgramID]): Future[List[VmUpdate]] = {
    val fut = VersionMapFunctor.future(OT.getKeyChain, peer, pids.map(pid => (pid, VmStore.get(pid).map(vmChecksum))))
    fut.onComplete {
      case Failure(t)  => handleFailure(peer, pids, t)
      case Success(us) => handleSuccess(us)
    }
    fut
  }

  private def handleSuccess(ups: List[VmUpdate]): Unit = Swing.onEDT {
    Log.log(Level.FINE, s"VmUpdate for ${ups.map(_.pid).mkString(", ")}")
    ups.foreach(VmStore.update)
  }

  private def handleFailure(peer: Peer, pids: Seq[SPProgramID], t: Throwable): Unit = Swing.onEDT {
    t match {
      case _: ConnectException =>
        // Spare a stack trace on the console if it is just a connection refused.
        Log.log(Level.INFO, s"VmUpdater ${peer.displayName} connection refused for ${pids.mkString(", ")}")
      case _ =>
        Log.log(Level.WARNING, s"VmUpdater ${peer.displayName} failure for ${pids.mkString(", ")}", t)
    }
    pids.foreach(VmStore.remove)
  }

  private var vmUpdater = Map.empty[Peer, (Mode, List[VmUpdater])]

  def manageUpdates(db: IDBDatabaseService, kc: KeyChain, reg: VcsRegistrar): Unit = {
    def allPeers: Set[Peer] =
      kc.peers.unsafeRun.toOption.getOrElse(Set.empty)

    def start(peer: Peer, mode: Mode): Unit = {
      val ups = List(Active, Inactive).map(pc => VmUpdater(peer, db, reg.registration, mode, pc))
      ups.foreach(_.start())
      vmUpdater = vmUpdater.updated(peer, (mode, ups))
    }

    def stop(peer: Peer): Unit = {
      vmUpdater.get(peer).foreach(_._2.foreach(_.stop()))
      vmUpdater = vmUpdater - peer
    }

    kc.addListener(IO {
      val mode       = currentMode
      val knownPeers = vmUpdater.keySet

      // Update the poll period for any peers we already knew about.
      (allPeers & knownPeers).foreach { existingPeer =>
        if (mode != vmUpdater(existingPeer)._1) {
          stop(existingPeer)
          start(existingPeer, mode)
        }
      }

      // Start up any peers we didn't know about
      (allPeers &~ knownPeers).foreach { newPeer => start(newPeer, mode) }

      // Turn off any that we've forgotten.
      (knownPeers &~ allPeers).foreach(stop)

      true
    }).unsafeRunAndThrow
  }

}

import VmUpdater._

case class VmUpdater(peer: Peer, db: IDBDatabaseService, registrar: SPProgramID => Option[Peer], mode: Mode, category: ProgramCategory) {
  val pollMs = pollPeriodSec(mode, category) * 1000

  class Updater extends DaemonActor {
    def update(): Unit = {
      val zippedPids = pids(category, db).filter(pid => registrar(pid).exists(_ == peer)).map { pid =>
        (pid, VmStore.get(pid).map(vmChecksum))
      }

      if (zippedPids.size > 0) { // no need to bother a peer for which we have no prog
        VersionMapFunctor.exec(OT.getKeyChain, peer, zippedPids) match {
          case \/-(ups) => handleSuccess(ups)
          case -\/(t)   => handleFailure(peer, zippedPids.unzip._1, t)
        }
      }
    }

    def act(): Unit = {
      update()
      loop {
        reactWithin(pollMs) {
          case TIMEOUT => update()
          case STOP    => exit()
        }
      }
    }
  }

  private var updater: Option[Updater] = None

  def start() {
    if (!updater.isDefined) {
      updater = Some(new Updater)
      updater foreach { up =>
        up.start()
        Log.info(s"Polling ${peer.displayName} for ${category.toString.toLowerCase} program VCS version information at $pollMs ms")
      }
    }
  }

  def stop() {
    updater foreach { up =>
      up ! STOP
      Log.info(s"Stopped polling ${peer.displayName} for ${category.toString.toLowerCase} program VCS version information")
    }
    updater = None
  }

  override def toString: String = s"VmUpdater(${peer.displayName}, $category, $pollMs)"
}
