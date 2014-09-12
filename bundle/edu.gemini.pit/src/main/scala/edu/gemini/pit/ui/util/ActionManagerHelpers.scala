package edu.gemini.pit.ui.util

import javax.swing.Action
import edu.gemini.ui.workspace.IActionManager
import IActionManager.Relation
import Relation._

trait ActionManagerHelpers {

  case class Menu(name: String, relation: Relation, relative: Option[Menu])

  implicit val pimpMenu = (m: Menu) => m.name

  implicit val pimpActionManager = (mgr: IActionManager) => new {

    def add(ms: Menu*) {
      ms foreach { m => mgr.addContainer(m.relation, m.relative.map(_.name).getOrElse(""), m, m) }
    }

    def add(menu: Menu, actions: Option[Action]*) {
      // TODO: make this more scala-like; it's a direct port of QPT code
      add(menu)
      var path = menu.name
      var rel = FirstChildOf
      for (oa <- actions; a <- oa) {
        val id = Integer.toString(System.identityHashCode(a))
        mgr.addAction(rel, path, id, a)
        path = menu.name + "/" + id
        rel = NextSiblingOf
      }
      path = menu.name
      rel = FirstChildOf
      for (oa <- actions) {
        oa match {
          case Some(a) =>
            val id = Integer.toString(System.identityHashCode(a))
            path = menu.name + "/" + id
            rel = NextSiblingOf
          case None =>
            mgr.addSeparator(rel, path)
        }
      }
    }

  }
}