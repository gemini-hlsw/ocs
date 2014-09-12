package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.{AuxFileTransferEvent, AuxFileTransferListener, AuxFileException, AuxFile}
import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.spModel.core.SPProgramID

import jsky.app.ot.ui.util.ProgressModel

import java.io.IOException
import java.lang.reflect.UndeclaredThrowableException
import java.util.logging.{Level, Logger}
import javax.swing.{JFrame, SwingUtilities}

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing._
import scala.swing.Dialog.showMessage
import scala.swing.Dialog.Message.{Error, Warning}
import scala.swing.Swing.onEDT
import scala.util.{Success, Failure}

object AuxFileAction {
  def transferListener(pm: ProgressModel) = new AuxFileTransferListener {
    def transferProgressed(evt: AuxFileTransferEvent) = {
      val percent = Math.floor(evt.getPercentageTransferred).toInt
      onEDT(pm.setValue(percent))
      !pm.isCancelled
    }
  }

  def silentUpdate(m: AuxFileModel) {
    for (client <- m.client; pid <- m.currentPid) {
      m.busy(pid)
      future {
        client.listAll(pid).asScala.toList
      } onComplete {
        case Failure(ex) =>
          Logger.getLogger(this.getClass.getName).log(Level.WARNING, ex.getMessage, ex)
          onEDT { m.failure(pid) }
        case Success(lst) =>
          onEDT { m.success(pid, lst) }
      }
    }
  }
}

abstract class AuxFileAction(t: String, c: Component, model: AuxFileModel) extends Action(t) with Reactor {
  protected val Log = Logger.getLogger(this.getClass.getName)

  listenTo(model)
  reactions += {
    case AuxFileStateEvent(_) => enabled = currentEnabledState
  }

  def currentEnabledState: Boolean =
    model.client.isDefined && model.currentState.exists(_.status == AuxFileState.Idle)

  def interpret(ex: AuxFileException): String

  protected def jFrame: Option[javax.swing.JFrame] =
    for {
      sc <- Option(c)
      r  <- Option(SwingUtilities.getRoot(sc.peer))
      if r.isInstanceOf[JFrame]
    } yield r.asInstanceOf[JFrame]

  private def update(client: AuxFileClient, pid: SPProgramID): List[AuxFile] =
    client.listAll(pid).asScala.toList

  protected def exec[I](input: => Option[I])(op: (AuxFileClient, SPProgramID, I) => Unit) {
    for (client <- model.client; pid <- model.currentPid; in <- input) {
      model.busy(pid)
      future {
        op(client, pid, in)
        update(client, pid)
      } onComplete {
        case Failure(ute: UndeclaredThrowableException) if ute.getCause.isInstanceOf[IOException] =>
          Log.log(Level.INFO, "IO Exception working with aux file server", ute)
          onEDT {
            showMessage(c, "There was a problem communicating with the remote server.  Check your connection or try again later.", "Communication Problem", Warning)
            model.failure(pid)
          }

        case Failure(ex: AuxFileException) =>
          Log.log(Level.WARNING, interpret(ex), ex)
          onEDT {
            showMessage(c, interpret(ex), "File Attachment Error", Warning)
            model.failure(pid)
          }

        case Failure(ex)                   =>
          Log.log(Level.WARNING, "Unexpected AuxFile client error", ex)
          onEDT {
            showMessage(c, "Sorry, an error occurred while working with the server.", "Unexpected Error", Error)
            model.failure(pid)
          }

        case Success(lst)                  =>
          onEDT { model.success(pid, lst) }
      }
    }
  }
}
