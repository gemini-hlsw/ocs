package edu.gemini.pit.ui.robot

import javax.swing.SwingUtilities
import java.util.logging.{Level, Logger}
import scalaz.Lens
import edu.gemini.pit.ui.binding.Bound
import edu.gemini.pit.model.Model

/**
 * A trait for classes with a hunk of state and a list of change listeners, both of which can be mutated only on the
 * UI thread.
 */
trait Robot extends Bound[Model, Model] {

  // Our lens
  val lens = Lens.lensId[Model]

  // A logger
  protected val logger = Logger.getLogger(getClass.getName)

  // Some type aliases
  type State
  type Listener = State => Unit

  // Our initial state
  protected def initialState: State

  // Mutable state and listeners
  private var _state: State = initialState
  private var _listeners: List[Listener] = Nil

  // Accessor and mutator for state
  def state = _state
  protected[this] def state_=(newState: State) {
    checkThread()
    _state = newState
    for (f <- _listeners) try {
      f(_state)
    } catch {
      case e:Exception => logger.log(Level.WARNING, "Problem notifying a listener", e)
    }
  }

  // Reset
  def reset() {
    state = initialState
  }

  // Mutator for listeners (add-only for now)
  def addListener(l: Listener) {
    checkThread()
    _listeners = l :: _listeners
    l(state)
  }

  protected def checkThread() {
    assert(SwingUtilities.isEventDispatchThread, "This method can only be called from the event dispatch thread.")
  }

  protected def onEventDispatchThread(f: => Unit) {
    SwingUtilities.invokeLater(new Runnable {
      def run() {
        f
      }
    })
  }

}