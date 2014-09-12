package jsky.app.ot.vcs

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.data.ISPDataObject

import javax.swing.table.AbstractTableModel

import scalaz._
import Scalaz._

object SyncAllTableModel {
  lazy val Log = java.util.logging.Logger.getLogger(getClass.getName)

  sealed trait Column {
    def name: String
    def width: Int
    def clazz: Class[_]
  }

  case object StateColumn extends Column {
    val name  = ""
    val width = 20
    def clazz = classOf[SyncAllModel.State]
  }

  case object IdColumn extends Column {
    val name  = "Program ID"
    val width = 150
    def clazz = classOf[SPProgramID]
  }

  case object TitleColumn extends Column {
    val name  = "Title"
    val width = 300
    def clazz = classOf[String]
  }

  case object DetailColumn extends Column {
    val name  = "Details"
    val width = 120
    def clazz = classOf[SyncAllModel.State]
  }

  val Columns     = Vector[Column](StateColumn, IdColumn, TitleColumn, DetailColumn)
  val ColumnIndex = Columns.zipWithIndex.toMap
}

class SyncAllTableModel extends AbstractTableModel {
  import SyncAllTableModel._

  private var model = SyncAllModel.empty

  override lazy val getColumnCount: Int = Columns.length

  override def getColumnName(column: Int): String = Columns(column).name

  override def getColumnClass(column: Int): Class[_] = Columns(column).clazz

  override def getRowCount: Int = model.programs.length

  def programs: Vector[SyncAllModel.ProgramSync]  = model.programs
  def program(row: Int): SyncAllModel.ProgramSync = programs(row)

  override def getValueAt(row: Int, column: Int): AnyRef = synchronized {
    val p = programs(row)
    Columns(column) match {
      case StateColumn   => p.state
      case IdColumn      => p.pid
      case TitleColumn   => ~Option(p.program.getDataObject.asInstanceOf[ISPDataObject].getTitle)
      case DetailColumn  => p.state
    }
  }

  def update(m: SyncAllModel): Unit = {
    model = m
    fireTableDataChanged()
  }
}