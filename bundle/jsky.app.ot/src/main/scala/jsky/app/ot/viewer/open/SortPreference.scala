package jsky.app.ot.viewer.open

import javax.swing.{SortOrder, RowSorter}
import java.util.prefs.Preferences
import scala.util.Try

/**
 * Keep up with the user's sorting preference.
 */
object SortPreference {
  val DefaultColumn = 0
  val DefaultOrder  = SortOrder.ASCENDING

  private def key(n: String) = s"${getClass.getSimpleName}.$n"
  private val columnKey = key("column")
  private val orderKey  = key("order")

  private lazy val preferences = Preferences.userNodeForPackage(getClass)

  def get: RowSorter.SortKey = {
    val col  = preferences.getInt(columnKey, DefaultColumn)
    val ordS = preferences.get(orderKey, DefaultOrder.name)
    val ord  = Try { SortOrder.valueOf(ordS) }.getOrElse(DefaultOrder)
    new RowSorter.SortKey(col, ord)
  }

  def set(k: RowSorter.SortKey): Unit = {
    preferences.putInt(columnKey, k.getColumn)
    preferences.put(orderKey, k.getSortOrder.name)
  }
}
