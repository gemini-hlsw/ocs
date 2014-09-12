package edu.gemini.model.p1.dtree

import UIState.PageStack

// Internal aliases only used for the implementation of UIState.
private[dtree] sealed trait UIPage[C, S] {
  def node: Node[_, C, S, _]
  def default: Option[S]
  def mkState(undoStack: PageStack, redoStack: PageStack): UIState[C, S]
  def withDefault(s: Option[S]): UIPage[C, S]
  def toInitialState = mkState(Nil, Nil)
}

// Parent of UI pages that support selection
private[dtree] sealed trait SelectUIPage[C, S] extends UIPage[C, S]{
  def select(is: List[Int]): Option[S]
  def selection: List[Int]
}

private[dtree] case class SingleUIPage[C](node: SingleSelectNode[_, C, _], default: Option[C]) extends SelectUIPage[C, C]{
  def mkState(undoStack: PageStack, redoStack: PageStack) = SingleUIState(undoStack, this, redoStack)
  def withDefault(s: Option[C]): SingleUIPage[C] = copy(default = s)

  def select(is: List[Int]) = is match {
    case i :: Nil if i >= 0 && i < node.choices.length => Some(node.choices(i))
    case _ => None
  }
  def selection: List[Int] = default.map(node.choices.indexOf(_)).toList
}

private[dtree] case class MultiUIPage[C](node: MultiSelectNode[_, C, _], default: Option[List[C]]) extends SelectUIPage[C, List[C]] {
  def mkState(undoStack: PageStack, redoStack: PageStack) = MultiUIState(undoStack, this, redoStack)
  def withDefault(s: Option[List[C]]): MultiUIPage[C] = copy(default = s)

  def select(is: List[Int]) = if (is.forall(i => i >= 0 && i < node.choices.length)) Some(is.map(node.choices)) else None
  def selection: List[Int] = default.map(_.map(node.choices.indexOf(_))).getOrElse(List.empty)
}

// Page supporting a single text field
private[dtree] case class TextUIPage(node: TextNode[_, _], default: Option[String]) extends UIPage[String, String] {
  def mkState(undoStack: PageStack, redoStack: PageStack) = TextUIState(undoStack, this, redoStack)
  def withDefault(s: Option[String]): TextUIPage = copy(default = s.filter(_.nonEmpty))
}
