package edu.gemini.model.p1.dtree

/**
 * A decision tree node is parametrized on its input type, output type, the type of available
 * choices that the user must select from, and the type of this selection. A node is evaluated
 * with a selection, and the result of this evaluation is either the next node or the final answer.
 */
sealed abstract class Node[+I, C, S, +O](state: I) extends (S => Either[Node[O, _, _, _], O]) {

  def title: String
  def description: String
  def toUIPage: UIPage[C, S]

  def default: Option[S] = None

  // Given a final result, return the choice the user must have made
  def unapply: PartialFunction[Any, S]
}

/**
 * A SelectNode lets the user choose the next state based on a set of choices
 */
abstract class SelectNode[+I, C, S, +O](state: I) extends Node[I, C, S, O](state) {
  def choices: List[C]
}

abstract class SingleSelectNode[+I, C, +O](state: I) extends SelectNode[I, C, C, O](state) {
  def toUIPage = SingleUIPage[C](this, default)
}

abstract class MultiSelectNode[+I, C, +O](state: I) extends SelectNode[I, C, List[C], O](state) {
  def toUIPage = MultiUIPage(this, default)
}

/**
 * A text node lets the user enter free text. Note that choices and selection are Strings
 */
abstract class TextNode[+I, +O](state: I) extends Node[I, String, String, O](state) {
  def toUIPage = TextUIPage(this, default)
}
