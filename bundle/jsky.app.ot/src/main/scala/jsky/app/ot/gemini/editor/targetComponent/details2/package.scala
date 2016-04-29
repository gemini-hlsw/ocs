package jsky.app.ot.gemini.editor.targetComponent

import java.beans.{PropertyChangeListener, PropertyChangeEvent}

import jsky.app.ot.ui.util.FlatButtonUtil
import jsky.util.gui.{ TextBoxWidget, TextBoxWidgetWatcher }

import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.BorderFactory._
import javax.swing.JButton
import javax.swing.border.Border

import scala.language.implicitConversions
import scalaz._, Scalaz._, scalaz.concurrent.Task

package object details2 {

  /** Create a titled border with inner and outer padding. */
  def titleBorder(title: String): Border =
    createCompoundBorder(
      createEmptyBorder(2,2,2,2),
      createCompoundBorder(
        createTitledBorder(title),
        createEmptyBorder(2,2,2,2)))

  def watcher(f: String => Unit) = new TextBoxWidgetWatcher {
    override def textBoxKeyPress(tbwe: TextBoxWidget): Unit = textBoxAction(tbwe)
    override def textBoxAction(tbwe: TextBoxWidget): Unit = f(tbwe.getValue)
  }

  def searchButton(doSearch: => Unit): JButton =
    button("eclipse/search.gif")(doSearch)

  def refreshButton(doRefresh: => Unit): JButton =
    button("eclipse/refresh.gif")(doRefresh)

  def button(image: String)(action: => Unit): JButton =
    FlatButtonUtil.create(image) <| { b =>
      b.addActionListener(new ActionListener() {
        override def actionPerformed(e: ActionEvent) = action
      })
    }

  def forkSwingWorker[A <: AnyRef](constructImpl: => A)(finishedImpl: Throwable \/ A => Unit): Unit =
    Task(constructImpl).unsafePerformAsync(finishedImpl)

  implicit def F2ActionlListener(f: ActionEvent => Unit): ActionListener =
    new ActionListener { def actionPerformed(e: ActionEvent): Unit = f(e) }

  implicit def F2PropertyChangeListener(f: PropertyChangeEvent => Unit): PropertyChangeListener =
    new PropertyChangeListener { def propertyChange(evt: PropertyChangeEvent): Unit = f(evt) }

  // Turn an A @ Option[B] into an A @> B given a default value for B
  implicit class OptionsLensOps[A, B](lens: A @> Option[B]) {
    def orZero(zero: B): A @> B =
      lens.xmapB(_.getOrElse(zero))(b => (b != zero).option(b))
  }

}
