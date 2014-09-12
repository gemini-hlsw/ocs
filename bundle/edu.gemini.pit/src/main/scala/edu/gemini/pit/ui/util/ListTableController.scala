package edu.gemini.pit.ui.util

import edu.gemini.ui.gface._

abstract class ListTableController[E, S] extends GTableController[List[E], E, S] {
  private var model: Option[List[E]] = None
  def getElementAt(i: Int): E = model.map(_.apply(i)).get
  def getElementCount: Int = model.map(_.length).getOrElse(0)
  def modelChanged(v: GViewer[List[E], E], old: List[E], m: List[E]) {
    model = Option(m)
  }
}