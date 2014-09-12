package edu.gemini.model.p1.dtree

import UIState._

import scalaz._
import Scalaz._

sealed abstract class UIState[C, S] protected (undoStack: PageStack, page: UIPage[C, S], redoStack: PageStack) {

  implicit val boolMonoid = Monoid.instance[Boolean](_ || _,  false)

  val node = page.node
  val default = page.default

  def defaultString: Option[String]

  /**
   * Returns the previous state, with a default choice and redo information to recover current
   * state, or None if the undo stack is empty. Check canUndo if you're unsure.
   */
  def undo = undoStack.headOption.map(_.mkState(undoStack.tail, page :: redoStack))

  /**
   * Returns the next state, with undo information to recover current state, or None if the redo
   * stack is empty. Check canRedo if you're unsure.
   */
  def redo = redoStack.headOption.map(_.mkState(page :: undoStack, redoStack.tail))

  /** Returns true if the undo stack is non-empty. */
  def canUndo = !undoStack.isEmpty

  /** Returns true if the redo stack is non-empty. */
  def canRedo = !redoStack.isEmpty

  def canFinish = ~default.map(apply(_).isRight)
  
  /** Apply a selection, returning a new state or the list of choices and final result. */
  def apply(c: S): Either[UIState[_, _], (UIState[_, _], Any)] = page.default.filter(_ == c).flatMap(c => redo).map(Left(_)).getOrElse {
    page.node.apply(c) match {
      case Left(n) => Left(n.toUIPage.mkState(page.withDefault(Some(c)) :: undoStack, Nil))
      case Right(r) =>
        val ns = (page.withDefault(Some(c)) :: undoStack).reverse
        val s = ns.head.mkState(Nil, ns.tail)
        Right((s, r)) // compiler makes us reconstruct here. odd.
    }
  }

  def recover(a: Any):Option[UIState[_,_]] = for {
    choice <- node.unapply.lift(a)
    result <- apply(choice) match {
      case Left(n)  => n.recover(a)
      case Right((s, _)) => Some(s)
    }
  } yield result

}

// Parent of state based on selection
abstract class SelectUIState[C, S](undoStack: PageStack, page: SelectUIPage[C, S], redoStack: PageStack) extends UIState[C, S](undoStack, page, redoStack) {
  def select(is: List[Int]) = page.select(is)
  def selection = page.selection
}

case class SingleUIState[C](undoStack: PageStack, page: SingleUIPage[C], redoStack: PageStack)
  extends SelectUIState[C, C](undoStack, page, redoStack) {

  def defaultString = page.default.map(page.node.choices.indexOf(_)).map(_ + 1).map("[%s]".format(_))
}

case class MultiUIState[C](undoStack: PageStack, page: MultiUIPage[C], redoStack: PageStack)
  extends SelectUIState[C, List[C]](undoStack, page, redoStack) {

  def defaultString = page.default.map(_.map(page.node.choices.indexOf(_)).map(_ + 1).mkString("[", ",", "]"))
}

// State based on text
case class TextUIState[+C <: String](undoStack: PageStack, page: TextUIPage, redoStack: PageStack)
  extends UIState[String, String](undoStack, page, redoStack) {
  def defaultString = page.default
  def text[S](input: S):Option[S] = some(input)
}

object UIState {
  private[dtree]type PageStack = List[UIPage[_, _]]
}

