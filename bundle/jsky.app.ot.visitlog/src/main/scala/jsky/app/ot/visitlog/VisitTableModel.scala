package jsky.app.ot.visitlog

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.obsrecord.ObsVisit

import javax.swing.table.AbstractTableModel
import java.util.Date

object VisitTableModel {
  sealed abstract class Column[+T <% AnyRef : Manifest](val name: String) {
    def value(visit: ObsVisit): T
    def clazz: Class[_] = manifest.runtimeClass
  }

  case object IdColumn extends Column[SPObservationID]("Observation") {
    def value(visit: ObsVisit): SPObservationID = visit.getObsId
  }

  case object Datasets extends Column[String]("Datasets") {
    def value(visit: ObsVisit): String = {
      val indices = visit.getAllDatasetLabels.toList.map(_.getIndex).sorted
      val groups  = (List.empty[Range]/:indices) { (lst, index) =>
        lst match {
          case Nil    => List(Range(index, index))
          case h :: t => if (h.end + 1 == index) Range(h.start, index) :: t
                         else if (h.end == index) lst
                         else Range(index, index) :: lst
        }
      }
      groups.reverse.map { r =>
        if (r.start == r.end) r.start.toString
        else s"${r.start}-${r.end}"
      }.mkString(",")
    }
  }

  case object StartTime extends Column[Date]("Start") {
    def value(visit: ObsVisit): Date = new Date(visit.getStartTime)
  }

  case object Duration extends Column[String]("Duration (mins)") {
    def value(visit: ObsVisit): String = f"${visit.getTotalTime/60000d}%1.2f"
  }

  val columns = List(IdColumn, Datasets, StartTime, Duration)
}

import VisitTableModel._

class VisitTableModel extends AbstractTableModel {
  private var visitList: List[ObsVisit] = Nil

  def getRowCount: Int = visitList.length
  def getColumnCount: Int = columns.length

  def getValueAt(r: Int, c: Int): Object = columns(c).value(visitList(r))

  override def getColumnName(c: Int): String    = columns(c).name
  override def getColumnClass(c: Int): Class[_] = columns(c).clazz

  def visits: List[ObsVisit] = visitList
  def visits_=(visits: List[ObsVisit]): Unit = {
    visitList = visits
    fireTableDataChanged()
  }
}
