// Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ViewerManager.java 47005 2012-07-26 22:35:47Z swalker $
//
package jsky.app.ot.viewer

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{ProgramEvent, ProgramEventListener, IDBDatabaseService}

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._
import edu.gemini.spModel.core.SPProgramID

object ViewerManager {

  /** All viewers are attached to the local database. This is not ideal. */
  private val db: IDBDatabaseService = SPDB.get

  /** Let's go ahead and remove programs from viewers on deletion. */
  db.addProgramEventListener(new ProgramEventListener[ISPProgram] {
    def programRemoved(pme: ProgramEvent[ISPProgram]): Unit =
        close(pme.getOldProgram)

    def programAdded(pme: ProgramEvent[ISPProgram]): Unit = ()
    def programReplaced(pme: ProgramEvent[ISPProgram]): Unit = ()
  })

  /** Constructs and returns a new, empty Viewer associated with the local database. */
  def newViewer: SPViewer = new SPViewerFrame(db).getViewer

  /** Constructs and opens a new program in the specified viewer, if any, otherwise constructs a new viewer. */
  def newProgram(viewer: Option[SPViewer]): SPViewer =
    open(db.getFactory.createProgram(null, null) <| db.put, viewer)

  /** Constructs and opens a new program in new viewer. */
  def newProgram(): SPViewer =
    newProgram(None)

  /** Constructs and opens a new program in the specified viewer, if non-null, otherwise constructs a new viewer. */
  def newProgram(viewerOrNull: SPViewer): SPViewer =
    newProgram(Option(viewerOrNull))

  /**
   * Reveals the specified node in a viewer, preferring (a) any existing viewer
   * with this node's root ancestor in its history, then (b) the supplied
   * viewer, if any, then (c) any open but empty viewer, and then finally (d)
   * a new viewer.
   */
  def open(node: ISPNode, viewer: Option[SPViewer]): SPViewer = {
    val v = find(node) orElse viewer orElse findEmpty getOrElse newViewer
    v.tryNavigate(v.getHistory.go(node))
    v.showParentFrame()
    v
  }

  /**
   * Reveals the specified program in a viewer if it exists in the database.
   * Prefers any existing viewer wth this program its history before creating a
   * new viewer.
   * @return the viewer in which the program is opened, if any
   */
  def open(pid: SPProgramID): Option[SPViewer] = Option(db.lookupProgramByID(pid)).map(open)

  /**
   * Reveals the specified node in a viewer, preferring (a) any existing viewer with this node's root ancestor in its
   * history, then (b) a new viewer.
   */
  def open(node: ISPNode): SPViewer =
    open(node, None)

  /**
   * Reveals the specified node in a viewer, preferring (a) any existing viewer with this node's root ancestor in its
   * history, then (b) the supplied viewer, if non-null, then finally (c) a new viewer.
   */
  def open(node: ISPNode, viewerOrNull: SPViewer): SPViewer =
    open(node, Option(viewerOrNull))

  /**
   * Removes references to this node's root ancestor from any existing viewer, then opens the specified node in
   * a new viewer.
   */
  def openInNewViewer(node: ISPNode): SPViewer = {
    close(node.getProgram)
    open(node)
  }

  def close(node: ISPProgram): Unit = {
    find(node).foreach(_.closeProgram(node))
  }

  /**
   * Finds the viewer associated with the specified node, if any. Note that this association may simply be via
   * history; the node (or indeed its root ancestor) need not be visible.
   */
  def find(node: ISPNode): Option[SPViewer] =
    SPViewer.instances.asScala.find(_.getHistory.find(node).isDefined)

  /**
   * Finds the viewer associated with the specified node, if any. Note that this association may simply be via
   * history; the node (or indeed its root ancestor) need not be visible.
   */
  def findOrNull(node: ISPNode): SPViewer =
    find(node).orNull

  /**
   * Finds an open viewer that isn't displaying any program node, if any.
   */
  def findEmpty: Option[SPViewer] =
    SPViewer.instances.asScala.find(_.getRoot == null)

  /**
   * Finds an open viewer that isn't displaying any program node, if any.
   */
  def findEmptyOrNull: SPViewer =
    findEmpty.orNull
}
