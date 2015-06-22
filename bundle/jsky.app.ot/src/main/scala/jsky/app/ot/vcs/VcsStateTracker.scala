package jsky.app.ot.vcs

import edu.gemini.sp.vcs.reg.VcsRegistrationEvent
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.pot.sp.{SPUtil, ISPObsExecLog, ISPNode, ISPProgram}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs._

import jsky.app.ot.vcs.vm.{VmStore, VmUpdateEvent}

import java.beans.{PropertyChangeEvent, PropertyChangeListener}

import scala.swing.{Swing, Publisher}
import scala.swing.event.Event

case class VcsStateEvent(programId: Option[SPProgramID], peer: Option[Peer], status: ProgramStatus, conflictNodes: List[ISPNode]) extends Event

object VcsStateTracker {
  private def id(po: Option[ISPProgram]): Option[SPProgramID] =
    po.flatMap(p => Option(p.getProgramID))
}

final class VcsStateTracker extends Publisher {

  import VcsStateTracker._

  private val programListener = new PropertyChangeListener {
    def propertyChange(evt: PropertyChangeEvent) {
      val mayChangeConflictList =
        evt.getPropertyName != SPUtil.getDataObjectPropertyName &&
          !SPUtil.isTransientClientDataPropertyName(evt.getPropertyName)

      // ignore the auto-updating bits, these are handled by the ProgramUpdater
      // and will be taken into account when it updates the remote version
      if (!evt.getSource.isInstanceOf[ISPObsExecLog]) updateState(mayChangeConflictList)
    }
  }

  private var progNode: Option[ISPProgram] = None
  private var remoteVersions: Option[VersionMap] = None
  private var vcsState: VcsStateEvent = VcsStateEvent(None, None, ProgramStatus.Unknown, Nil)
  private var updater: Option[ProgramUpdater] = None

  VcsGui.registrar.foreach { reg =>
    reg.subscribe(new scala.collection.mutable.Subscriber[VcsRegistrationEvent, reg.Pub] {
      override def notify(pub: reg.Pub, event: VcsRegistrationEvent): Unit = Swing.onEDT {
        if (progNode.exists(_.getProgramID == event.pid)) {
          resetUpdater()
          updateState(calcConflicts = false)
        }
      }
    })
  }

  def setProgram(n: ISPNode) {
    val oldProgNode = progNode
    val newProgNode = n match {
      case p: ISPProgram => Some(p)
      case _ => None
    }

    if (newProgNode != oldProgNode) {
      oldProgNode.foreach {
        _.removeCompositeChangeListener(programListener)
      }
      newProgNode.foreach {
        _.addCompositeChangeListener(programListener)
      }
      progNode = newProgNode

      // Only if we've changed the program id do we need a new remote version
      if (id(oldProgNode) != id(newProgNode)) resetUpdater()
      updateState()
    }
  }

  def updateRemoteVersions(r: Option[VersionMap]): Unit =
    if (VersionMap.isNewer(r, remoteVersions)) {
      remoteVersions = r
      updateState(calcConflicts = false)
    }

  def conflicts: List[ISPNode] = vcsState.conflictNodes

  private def updateState(calcConflicts: Boolean = true): Unit = {
    val newPeer = id(progNode).flatMap(VcsGui.peer)

    val newStatus = (for {
      rJvm <- remoteVersions
      prog <- progNode
    } yield ProgramStatus(prog.getVersions, rJvm)).getOrElse(ProgramStatus.Unknown)

    val newConflicts = if (calcConflicts) progNode.toList.flatMap(ConflictNavigator.allConflictNodes)
    else vcsState.conflictNodes

    val newState = new VcsStateEvent(id(progNode), newPeer, newStatus, newConflicts)
    if (newState != vcsState) {
      vcsState = newState
      publish(vcsState)
    }
  }

  def currentState: VcsStateEvent = vcsState

  private def resetUpdater(): Unit = {
    remoteVersions = for {
      pid <- id(progNode)
      vm  <- VmStore.get(pid)
    } yield vm

    updater.foreach { up =>
      up.stop()
      up.removeSubscriptions()
    }

    updater = for {
      id <- id(progNode)
      reg <- VcsGui.registrar
      loc <- reg.registration(id)
    } yield ProgramUpdater(id, loc)

    updater.foreach { up =>
      up.subscribe(new up.Sub {
        def notify(pub: up.type#Pub, event: VmUpdateEvent): Unit = {
          updateRemoteVersions(event.vm)
        }
      })
      up.start()
    }
  }
}
