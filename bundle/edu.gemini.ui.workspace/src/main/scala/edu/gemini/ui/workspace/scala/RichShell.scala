package edu.gemini.ui.workspace.scala

import edu.gemini.ui.workspace.IShell
import java.util.logging.{Level, Logger}
import javax.swing.SwingUtilities
import java.io.File

object RichShell {
  private val log = Logger.getLogger(classOf[RichShell[_]].getName)
}

// N.B. this isn't threadsafe. It expects to be on the UI dispatch thread.
class RichShell[A](shell:IShell) {

  import RichShell._

  // file holds the associated file, if any. This association is largely independent of the model, although if the 
  // model is set to None then so is the file. The idea is that undo should not affect the file association.
  private var myFile:Option[File] = None

  // myModel holds our current unstable state
  private var myModel:Option[A] = None

  // We have a list of listeners who are notified of model/pivot changes
  private var listeners:List[() => Unit] = Nil

  // If this is true, we're in the process of pushing state, which is not re-entrant
  private var updating = false

  // If this is true and old model was rolled into a new version
  // Required to support the special case tha the model was upgraded
  // It may make more sense as a method in the model A
  private var wasRolled = false

  // Undo and redo are stacks of state
  private var undoStack:List[A] = Nil
  private var redoStack:List[A] = Nil

  // Our pivot indicates the last savepoint
  private var pivot:Option[A] = None

  // Some one-liner predicates
  def isModified = model != pivot
  def isClean = !isModified
  def canUndo = undoStack.nonEmpty
  def canRedo = redoStack.nonEmpty
  def isRolled = wasRolled

  // Make a new state current, or clear out everything if the new state is None.
  private def push(a:Option[A], undoable:Boolean) = ui {
    undoStack = (a, model) match {
      case (Some(_), Some(b)) if undoable => b :: undoStack
      case (Some(_), Some(b))             => undoStack
      case _                              => Nil
    }
    redoStack = Nil
    if (a.isEmpty) pivot = None
    commit(a)
  }

  def undo() = ui {
    assert(model.isDefined)
    val (un, m, re) = roll(undoStack, model.get, redoStack)
    undoStack = un
    redoStack = re
    commit(Some(m))
  }

  def redo() = ui {
    assert(model.isDefined)
    val (re, m, un) = roll(redoStack, model.get, undoStack)
    undoStack = un
    redoStack = re
    commit(Some(m))
  }

  def checkpoint() = ui {
    // We saved so we are not rolled anymore
    wasRolled = false
    pivot = model
    commit(model)
  }

  private def commit(a:Option[A]) = ui {
    myModel = a
    notifyListeners()
    shell.setModel(a.orNull)
  }

  private def notifyListeners() = ui {
    synchronized {
      //println("Notifying of state. [%d] %s [%d]".format(undoStack.length, myModel.getClass.getName, redoStack.length))
      listeners.foreach(_())
    }
  }

  def model = myModel
  def model_=(newModel:Option[A], undoable:Boolean = true) = ui {
    if (newModel != model) {
      // This method is not re-entrant
      if (updating)
        log.log(Level.WARNING, "Concurrent model change.", new Exception)
      try {
        push(newModel, undoable)
      } finally {
        updating = false
      }
    } else {
      log.log(Level.WARNING, "Unchanged model discarded.", new Exception)
    }
  }

  def file = myFile
  def file_=(f:Option[File]) = ui {
    (model, f) match {
      case (None,  None) => // nop
      case (Some(_), Some(f)) =>
        // This is the only valid case
        myFile = Some(f)
        notifyListeners()
      case (Some(_), None) =>
        log.log(Level.WARNING, "Discarding file detach.", new Exception)
      case (None,  _) =>
        log.log(Level.WARNING, "Discarding file attach for empty model.", new Exception)
    }
  }

  def init(model:Option[A], f:Option[File], wasRolled: Boolean) = ui {
    this.wasRolled = wasRolled
    undoStack = Nil
    redoStack = Nil
    myFile = f
    pivot = model
    commit(model)
  }

  def close() = shell.close()

  def listen(f: => Unit) = synchronized {
    f
    listeners = (() => f) :: listeners
  }

  private def roll[A](src:List[A], a:A, dst:List[A]):(List[A], A, List[A]) = {
    assert(src.nonEmpty)
    (src.tail, src.head, a :: dst)
  }

  def peer = shell.getPeer
  def context = shell.getContext
  def advisor = shell.getAdvisor

  private def ui[A](f: => A):A = {
    require(SwingUtilities.isEventDispatchThread, "This method can only be called from the event dispatch thread.")
    f
  }

}

