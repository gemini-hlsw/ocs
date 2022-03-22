package jsky.app.ot.vcs

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBQueryRunner
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.util.trpc.client.TrpcClient
import jsky.app.ot.shared.progstate.{LeafNodeData, LeafNodeFetchFunctor}
import jsky.app.ot.vcs.vm.{VmUpdateEvent, VmStore}

import java.net.{ConnectException, SocketTimeoutException}
import java.util.logging.{ Logger, Level }

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.Swing
import scala.util.{Success, Failure}
import jsky.app.ot.OT


/**
 * A VcsVersionPublisher with some glorious program-updating side-effects.
 * Namely, it delegates to a simple straight-forward version poll but before
 * forwarding on its version information it first checks to see if any obs exec
 * logs have been updated remotely. If so, then it goes back to the database to
 * get the exec log data object and apply it locally setting the version
 * information to the same values as in the remote database.  At that point,
 * the version updates are forwarded on to any subscribers.  In this way, any
 * subscribers will never see pending updates to the obs exec logs because
 * they'll always just get applied automatically.
 */
case class ProgramUpdater(id: SPProgramID, loc: Peer) extends VmStore.Sub with mutable.Publisher[VmUpdateEvent] {
  val Log: Logger = Logger.getLogger(getClass.getName)

  private def prog: Option[ISPProgram] = Option(SPDB.get().lookupProgramByID(id))

  type ExecLogMap = Map[SPNodeKey, ISPObsExecLog]

  // Gets a Map[SPNodeKey, ISPObsExecLog] for all observation exec logs that
  // have been updated remotely.  (Note the ISPObsExecLog is of course the
  // local version in our database.)
  private def updatedExecLogs(program: ISPProgram, remoteJvm: VersionMap): ExecLogMap = {
    val localJvm = program.getVersions

    def addObs(o: ISPObservation, m: ExecLogMap): ExecLogMap =
      (Option(o.getObsExecLog) map { log =>
        val key = log.getNodeKey
        val remoteVersions = nodeVersions(remoteJvm, key)
        val localVersions  = nodeVersions(localJvm, key)
        remoteVersions.tryCompareTo(localVersions) match {
          case Some(n) if n > 0 => m + (key -> log) // remote is newer
          case _ => m
        }
      }).getOrElse(m)

    @tailrec
    def m0(ns: List[ISPNode], m: ExecLogMap): ExecLogMap =
      ns match {
        case Nil    => m
        case n :: t => n match {
          case o: ISPObservation    => m0(t, addObs(o, m))
          case _: ISPTemplateFolder => m0(t, m) // just a shortcut, no need to descend into templates
          case _: ISPConflictFolder => m0(t, m) // shortcut
          case _                    => m0(n.children ++ t, m)
        }
      }

    m0(program.children, Map.empty[SPNodeKey, ISPObsExecLog])
  }

  private def asyncUpdate(program: ISPProgram, remoteJvm: VersionMap, execLogMap: ExecLogMap, updates: Map[SPNodeKey, LeafNodeData]): Unit = {
    // Update the remote JVM to match the information we just got from the ODB
    val newRemoteJvm = (remoteJvm/:updates.toList) { case (m,(key, data)) =>
      m + (key -> data.versions)
    }

    // Apply an update to a single exec log, making sure that the version
    // information ends up matching the values on the server.
    def applyUpdate(n: ISPObsExecLog, up: LeafNodeData): Unit = {
      n.setDataObjectAndVersion(up.dataObject, up.versions)
    }

    // Apply all the updates with the program write lock held.
    def applyUpdates(): Unit = {
      program.getProgramWriteLock()
      try {
        updates.foreach { case (key, update) =>
          applyUpdate(execLogMap(key), update)
        }
      } finally {
        program.returnProgramWriteLock()
      }
    }

    // Do the actual updating in the event loop and pass along the updated
    // version map to any subscribers.
    Swing.onEDT {
      if (updates.nonEmpty) applyUpdates()
      publish(VmUpdateEvent(id, Some(newRemoteJvm)))
    }
  }

  private def updateExecLogs(program: ISPProgram, remoteJvm: VersionMap): Unit = {
    val execLogMap = updatedExecLogs(program, remoteJvm)
    if (execLogMap.isEmpty) publish(VmUpdateEvent(id, Some(remoteJvm)))
    else TrpcClient(loc).withKeyChain(OT.getKeyChain) future { r =>
           val execLogKeys = execLogMap.keys.asJavaCollection
           val func = new LeafNodeFetchFunctor(id, execLogKeys)
           r[IDBQueryRunner].execute(func, null).getResult.asScala.toMap
         } onComplete {
           case Failure(t) =>
             val msg = s"Could not get exec log updates from ${loc.host}:${loc.port}: ${t.getMessage}"
             t match {
               case _:SocketTimeoutException => Log.log(Level.WARNING, msg)
               case _:ConnectException       => Log.log(Level.WARNING, msg)
               case _                        => Log.log(Level.WARNING, msg, t)
             }
             Swing.onEDT { publish(VmUpdateEvent(id, Some(remoteJvm))) }
           case Success(m) => asyncUpdate(program, remoteJvm, execLogMap, m)
         }
  }

  def notify(pub: VmStore.Pub, event: VmUpdateEvent): Unit =
    (for {
      p  <- prog
      vm <- event.vm
    } yield (p, vm)).fold(publish(event)) { case (p, vm) =>
      updateExecLogs(p, vm)
    }

  def start(): Unit = VmStore.subscribe(this, _.pid == id)

  def stop(): Unit = VmStore.removeSubscription(this)
}
