package edu.gemini.catalog.ui

import javax.swing.table.{AbstractTableModel, TableModel, DefaultTableModel}

import edu.gemini.catalog.api.{RadiusConstraint, CatalogQuery}
import edu.gemini.catalog.votable.VoTableClient
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{Angle, Coordinates}

import scala.collection.JavaConverters._
import scala.swing._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz._
import Scalaz._

trait PreferredSizeFrame { this: Window =>
  def adjustSize() {
    // Set initial based on the desktop's size, the code below will work with multiple desktops
    val screenSize = java.awt.GraphicsEnvironment
                 .getLocalGraphicsEnvironment
                 .getDefaultScreenDevice
                 .getDefaultConfiguration.getBounds
    preferredSize = new Dimension(screenSize.getWidth.intValue() * 3/4, (2f / 3f * screenSize.getHeight).intValue())
  }
}

object QueryResultsWindow {
  val baseColumnNames = List("Id", "RA", "DE")

  case class TargetsModel(targets: List[SiderealTarget]) extends AbstractTableModel {
    override def getRowCount = targets.length

    override def getColumnCount = baseColumnNames.size

    override def getColumnName(column: Int): String =
      baseColumnNames(column)

    override def getValueAt(rowIndex: Int, columnIndex: Int) = (targets.isDefinedAt(rowIndex) option {
      val target = targets(rowIndex)
      val t = baseColumnNames.zipWithIndex.find(_._2 == columnIndex).map {
        case ("Id", _)  => target.name
        case ("RA", _)  => target.coordinates.ra.toAngle.formatHMS
        case ("DE", _) => target.coordinates.dec.formatDMS
      }
      t
    }).flatten.orNull
  }

  case class QueryResultsFrame(table: Table) extends Frame with PreferredSizeFrame {
    title = "Query Results"
    contents = new BorderPanel() {
      add(new ScrollPane() {
        contents = table
      }, BorderPanel.Position.Center)
    }
    adjustSize()
    pack()
  }

  import java.util.{Vector => JVector}
  val instance = this

  private val table = new Table()
  table.model = new DefaultTableModel(new JVector(baseColumnNames.asJava), 20)
  private val frame = QueryResultsFrame(table)

  def showOn(c: Coordinates):Unit = Swing.onEDT {
    if (frame.visible) {
      frame.peer.toFront()
    } else {
      frame.centerOnScreen()
      frame.visible = true
      frame.peer.toFront()
    }
    reloadSearchData(c)
  }

  def reloadSearchData(c: Coordinates): Unit = {
    val query = CatalogQuery.catalogQuery(c, RadiusConstraint.between(Angle.zero, Angle.fromArcmin(17.9)), None)
    VoTableClient.catalog(query).onSuccess {
      case x =>
        Swing.onEDT {
          table.model = TargetsModel(x.result.targets.rows)
        }
    }
  }
}