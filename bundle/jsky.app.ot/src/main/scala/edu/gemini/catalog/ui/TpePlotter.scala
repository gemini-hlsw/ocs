package edu.gemini.catalog.ui

import java.awt.geom.AffineTransform
import java.net.URL
import java.util
import javax.swing.event.TableModelListener

import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import jsky.app.ot.tpe.TpeImageWidget
import jsky.catalog._
import jsky.catalog.gui.BasicTablePlotter
import jsky.coords.{CoordinateRadius, Coordinates, WorldCoordinates}
import scalaz._
import Scalaz._

import scala.collection.JavaConverters._

class TpePlotter(display: CatalogImageDisplay) {

  /**
    * This is a horrible class to adapt the new catalog api to the old one to reduce the amount of conversion of the code
    * It has many unimplemented method that will likely blow in the most unexpected places
    */
  case object CatalogAdapter extends PlotableCatalog {
    override def getNumSymbols: Int = ???

    override def setSymbols(symbols: Array[TablePlotSymbol]): Unit = ???

    override def setSymbolsEdited(edited: Boolean): Unit = ???

    override def isSymbolsEdited: Boolean = ???

    override def getSymbolDesc(i: Int): TablePlotSymbol = ???

    override def getSymbols: Array[TablePlotSymbol] = {
      Array(new TablePlotSymbol())
    }

    override def getType: String = ???

    override def setParent(catDir: CatalogDirectory): Unit = ???

    override def getName: String = ???

    override def isLocal: Boolean = ???

    override def getId: String = ???

    override def isImageServer: Boolean = ???

    override def getParent: CatalogDirectory = ???

    override def getDescription: String = ???

    override def getDocURL: URL = ???

    override def setRegionArgs(queryArgs: QueryArgs, region: CoordinateRadius): Unit = ???

    override def setName(name: String): Unit = ???

    override def getPath: Array[Catalog] = ???

    override def getNumParams: Int = ???

    override def getParamDesc(i: Int): FieldDesc = ???

    override def getParamDesc(name: String): FieldDesc = ???

    override def getQueryArgs: QueryArgs = ???

    override def query(queryArgs: QueryArgs): QueryResult = ???

    override def getTitle: String = ???
  }

  /** Query Result Adapter to let the BasicTablePlotter work */
  case class TableQueryResultAdapter(model: TargetsModel) extends TableQueryResult {
    val catalog = CatalogAdapter
    // Table QueryResult methods
    override def getCatalog: Catalog = catalog

    override def getDataVector: util.Vector[util.Vector[AnyRef]] = new util.Vector(model.targets.map { t =>
      new util.Vector[AnyRef](List(t.coordinates.ra.toAngle.formatHMS, t.coordinates.dec.formatDMS).asJavaCollection)
    }.asJavaCollection)

    override def getColumnDesc(i: Int): FieldDesc = ???

    override def getColumnIndex(name: String): Int = ???

    override def getColumnIdentifiers: util.Vector[String] = ???

    override def hasCoordinates: Boolean = ???

    override def getCoordinates(rowIndex: Int): Coordinates = ???

    override def getRowCoordinates: RowCoordinates = new RowCoordinates(0, 1, 2000)

    override def getWCSCenter: WorldCoordinates = ???

    override def getQueryArgs: QueryArgs = ???

    override def setQueryArgs(queryArgs: QueryArgs): Unit = ???

    override def isMore: Boolean = ???

    // Catalog methods
    override def getName: String = catalog.getName

    override def setName(name: String): Unit = catalog.setName(name)

    override def getId: String = catalog.getId

    override def getTitle: String = catalog.getTitle

    override def getDescription: String = catalog.getDescription

    override def getDocURL: URL = catalog.getDocURL

    override def getNumParams: Int = catalog.getNumParams

    override def getParamDesc(i: Int): FieldDesc = catalog.getParamDesc(i)

    override def getParamDesc(name: String): FieldDesc = catalog.getParamDesc(name)

    override def setRegionArgs(queryArgs: QueryArgs, region: CoordinateRadius): Unit = catalog.setRegionArgs(queryArgs, region)

    override def isLocal: Boolean = catalog.isLocal

    override def isImageServer: Boolean = catalog.isImageServer

    override def getType: String = catalog.getType

    override def setParent(catDir: CatalogDirectory): Unit = catalog.setParent(catDir)

    override def getParent: CatalogDirectory = catalog.getParent

    override def getPath: Array[Catalog] = catalog.getPath

    override def query(queryArgs: QueryArgs): QueryResult = catalog.query(queryArgs)

    // TableModel methods, just delegate to model
    override def getRowCount: Int = model.getRowCount

    override def getColumnClass(columnIndex: Int): Class[_] = model.getColumnClass(columnIndex)

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = model.isCellEditable(rowIndex, columnIndex)

    override def getColumnCount: Int = model.getColumnCount

    override def getColumnName(columnIndex: Int): String = model.getColumnName(columnIndex)

    override def removeTableModelListener(l: TableModelListener): Unit = model.removeTableModelListener(l)

    override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = model.getValueAt(rowIndex, columnIndex)

    override def setValueAt(aValue: scala.Any, rowIndex: Int, columnIndex: Int): Unit = model.setValueAt(aValue, rowIndex, columnIndex)

    override def addTableModelListener(l: TableModelListener): Unit = model.addTableModelListener(l)
  }

  /**
   * Plot the given table data.
   */
  def plot(model: TargetsModel): Unit = {
    display.plotter.plot(TableQueryResultAdapter(model))
  }

  /**
    * Called when the view changes, e.g. with zoom in/out
    */
  def transformGraphics(trans: AffineTransform): Unit = {
    display.plotter.transformGraphics(trans)
  }
}
