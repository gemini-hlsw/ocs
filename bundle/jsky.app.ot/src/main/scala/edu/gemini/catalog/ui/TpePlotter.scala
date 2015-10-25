package edu.gemini.catalog.ui

import java.awt.Color
import java.net.URL
import javax.swing.event.TableModelListener

import edu.gemini.catalog.ui.tpe.CatalogImageDisplay
import edu.gemini.shared.skyobject.SkyObject
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.shared.util.immutable.{Option => JOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import jsky.catalog._
import jsky.coords._
import jsky.util.gui.StatusLogger
import scalaz._
import Scalaz._

import scala.collection.JavaConverters._

/**
  * Encapsulates adapters to make the display plotter believe it is being used with an old-world catalog
  * The implementation is minimal to get through plotting, it will be expanded if necessary
  *
  * Some day BasicTablePlotter will work using the new API rather than the other way around
  */
case class TpePlotter(display: CatalogImageDisplay) {

  case class QueryArgsAdapter(model: TargetsModel) extends QueryArgs {
    override def getCatalog: Catalog = ???

    override def getStatusLogger: StatusLogger = ???

    override def getParamValueAsInt(label: String, defaultValue: Int): Int = ???

    override def setParamValueRange(label: String, minValue: scala.Any, maxValue: scala.Any): Unit = ???

    override def setParamValueRange(label: String, minValue: Double, maxValue: Double): Unit = ???

    override def getQueryType: String = ???

    override def getId: String = ???

    override def getMaxRows: Int = ???

    override def getParamValue(i: Int): AnyRef = ???

    override def getParamValue(label: String): AnyRef = ???

    override def getConditions: Array[SearchCondition] = ???

    override def setRegion(region: CoordinateRadius): Unit = ???

    override def setId(id: String): Unit = ???

    override def copy(): QueryArgs = ???

    override def setMaxRows(maxRows: Int): Unit = ???

    override def getParamValueAsString(label: String, defaultValue: String): String = ???

    override def setQueryType(queryType: String): Unit = ???

    override def getParamValueAsDouble(label: String, defaultValue: Double): Double = ???

    override def setParamValues(values: Array[AnyRef]): Unit = ???

    override def setParamValue(i: Int, value: scala.Any): Unit = ???

    override def setParamValue(label: String, value: scala.Any): Unit = ???

    override def setParamValue(label: String, value: Int): Unit = ???

    override def setParamValue(label: String, value: Double): Unit = ???

    override def getRegion: CoordinateRadius = {
      val raHMS = model.base.ra.toAngle.toHMS
      val decDMS = model.base.dec.toDegrees
      val c = new WorldCoords(new HMS(raHMS.hours, raHMS.minutes, raHMS.seconds), new DMS(decDMS))
      new CoordinateRadius(c, model.radiusConstraint.maxLimit.toArcmins)
    }
  }

  /**
    * This is a horrible class to adapt the new catalog api to the old one to reduce the amount of conversion of the code
    * It has many unimplemented method that will likely blow in the most unexpected places
    */
  case class CatalogAdapter(model: TargetsModel) extends PlotableCatalog {
    override def getNumSymbols: Int = ???

    override def setSymbols(symbols: Array[TablePlotSymbol]): Unit = ???

    override def setSymbolsEdited(edited: Boolean): Unit = ???

    override def isSymbolsEdited: Boolean = ???

    override def getSymbolDesc(i: Int): TablePlotSymbol = ???

    override def getSymbols: Array[TablePlotSymbol] = {
      Array(new TablePlotSymbol() <| {_.setFg(Color.red)} <| {_.setShape(TablePlotSymbol.CIRCLE)})
    }

    override def getType: String = ???

    override def setParent(catDir: CatalogDirectory): Unit = ???

    override def getName: String = "catalog"

    override def isLocal: Boolean = ???

    override def getId: String = ~model.info.map(_.catalog.id)

    override def isImageServer: Boolean = ???

    override def getParent: CatalogDirectory = ???

    override def getDescription: String = ???

    override def getDocURL: URL = ???

    override def setRegionArgs(queryArgs: QueryArgs, region: CoordinateRadius): Unit = ???

    override def setName(name: String): Unit = ???

    override def getPath: Array[Catalog] = ???

    override def getNumParams: Int = 0

    override def getParamDesc(i: Int): FieldDesc = ???

    override def getParamDesc(name: String): FieldDesc = ???

    override def getQueryArgs: QueryArgs = QueryArgsAdapter(model)

    override def query(queryArgs: QueryArgs): QueryResult = ???

    override def getTitle: String = ???
  }

  /** Query Result Adapter to let the BasicTablePlotter work */
  case class TableQueryResultAdapter(model: TargetsModel) extends TableQueryResult {
    val catalog = CatalogAdapter(model)
    // Table QueryResult methods
    override def getCatalog: Catalog = catalog

    override def getDataVector: java.util.Vector[java.util.Vector[AnyRef]] = new java.util.Vector(model.targets.map { t =>
      val mags = MagnitudeBand.all.map(t.magnitudeIn).collect {
        case Some(v) => Double.box(v.value)
        case None    => null // This is required for the Java side of plotting
      }
      new java.util.Vector[AnyRef]((List(t.name, t.coordinates.ra.toAngle.formatHMS, t.coordinates.dec.formatDMS) ::: mags).asJavaCollection)
    }.asJavaCollection)

    override def getColumnDesc(i: Int): FieldDesc = ???

    override def getColumnIndex(name: String): Int = ???

    override def getColumnIdentifiers: java.util.List[String] = {
      val mags = MagnitudeBand.all.map(_.name + "mag")
      (List("Id", "RAJ2000", "DECJ2000") ::: mags).asJava
    }

    override def hasCoordinates: Boolean = ???

    override def getCoordinates(rowIndex: Int): Coordinates = ???

    override def getRowCoordinates: RowCoordinates = new RowCoordinates(1, 2, 2000) <| {_.setIdCol(0)}

    override def getWCSCenter: WorldCoordinates = ???

    override def getQueryArgs: QueryArgs = catalog.getQueryArgs

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

    override def getSkyObject(i: Int): JOption[SkyObject] = model.targets.lift(i).map(_.toOldModel).asGeminiOpt
  }

  /**
   * Plot the given table data.
   */
  def plot(model: TargetsModel): Unit = {
    display.plotter.plot(TableQueryResultAdapter(model))
  }

}
