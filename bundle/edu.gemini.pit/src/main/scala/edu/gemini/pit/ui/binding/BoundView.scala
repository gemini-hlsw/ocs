package edu.gemini.pit.ui.binding

import scalaz._
import Scalaz._
import edu.gemini.pit.model.{AppPreferences, Model}
import swing.{Action, UIElement}
import edu.gemini.ui.workspace.{IViewContext, IViewAdvisor}
import edu.gemini.ui.workspace.scala.RichShell
import java.util.logging.{Logger,Level}
import edu.gemini.pit.ui._
import edu.gemini.pit.ui.view.tac.TacView

/**
 * Trait for top-level UI elements (views in this case).
 */
trait BoundView[A] extends Bound[Model, A] {this:UIElement =>

  private[BoundView] val thePeer = peer

  // This value will be set by the framework prior to model updates, so you can check it at that point. This is a hack.
  // Making the world read-only after submitting was a requirement discovered late in the game, so it was hacked
  // into BoundView just to make it easily available everywhere. It should be revisited.
  var canEdit = true

  // Subclasses should override if the main content component isn't the focus receiver.
  def focus() {
    peer.requestFocus()
  }

  // Subclasses can override with a map of common actions, if desired. Thee will be triggered by the corresponding
  // top-level menu items when this view has focus.
  protected lazy val commonActions:Map[CommonActions.Value, Action] = Map.empty

}

object BoundView {

  val LOGGER = Logger.getLogger(classOf[BoundView[_]].getName)

  /**
   * An IViewAdvisor implementation whose content is a BoundView. This is how all the view advisors are constructed for
   * the PIT and it hides a bit of the GFace ugliness.
   */
  class Advisor[A](title: String, v: BoundView[A]) extends IViewAdvisor {

    def setFocus() {
      v.focus()
    }

    def open(c: IViewContext) {
      c.setTitle(title)
      c.setContent(v.thePeer)
      val shell: RichShell[Model] = c.getShell
      v.commonActions.foreach {
        case (k, v) => c.addRetargetAction(k, v.peer)
      }

      bindToShell(shell, v) {
        m =>
          // TODO: this, more elegantly
          v.canEdit = TacView.isTac || ~m.map(_.proposal.proposalClass.key.isEmpty)
      }
    }

    def close(c: IViewContext) {}
  }

  /**
   * Bind an object to the shell. The object will receive update events and will have the ability to push model changes
   * up to the shell via its supplied lens. The effect is a hack introduced to set the canEdit member; this should be
   * revisited.
   * @param shell the shell to bind to
   * @param bound the bound object
   * @param undoable do updates pushed from the bound object affect the undo state?
   * @param effect allows the binding source to perform an arbitrary action prior to updates
   */
  def bindToShell(shell: RichShell[Model], bound: Bound[Model, _], undoable: Boolean = true)(effect: Option[Model] => Unit) {

    def push(m: Option[Model]) {

      // Ignore updates that don't change the model's value. This prevents feedback loops when setting the values of
      // controls that subsequently issue change notifications. Such loops eventually stabilize and terminate but it
      // can take a long time. So I think this is the correct semantics.
      if (m != shell.model) {

        // If the proposal should be read-only we can still get updates via various sources such as robots, which is ok.
        // However updates from elsewhere in the UI are probably not ok. In any case logging is here for debugging.
        if (~bound.outer.map(_.proposal.isSubmitted) && !TacView.isTac)
          LOGGER.log(Level.WARNING, "Modification to read-only proposal.")

        shell.model_=(m, undoable)

      }
    }

    def doBind() {

      // Ok first execute the effect supplied by the binder, which is typically a no-op but is needed to support the
      // canEdit hack. We should be able to get rid of this.
      effect(shell.model)

      // Every time we perform a binding operation (i.e., each time we get a new model from the shell and tell our
      // bound object about it) we need to be sure the mutable ownership pointers are correct on the proposal's
      // observations. I think this should only happen when copying and pasting observations between proposals, but
      // it was a late entry to the model (a side-effect of making the target list its own thing, as opposed to
      // observations.map(_.target).distinct, which was the original approach). There may be some interactions with
      // undo/redo that also cause issues. In any case it's worth keeping an eye on.
      shell.model.map(_.proposal).foreach { m =>
        if (!m.check) {
          LOGGER.fine("Proposal's observations have incorrect ownership.")
          m.fix()
        }
      }

      // Really this should be the only thing we do here.
      bound.bind(shell.model, push)

    }

    // Bind when the model changes.
    shell.listen(doBind)

    // And when app prefs change (entering/exiting TAC mode, basically)
    AppPreferences.addListener(_ => doBind)

  }

}