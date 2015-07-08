package edu.gemini.catalog.votable

import edu.gemini.catalog.api.CatalogQuery
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.core.Target.SiderealTarget

import scala.util.matching.Regex

import scalaz._
import Scalaz._

case class UcdWord(token: String)
case class Ucd(tokens: List[UcdWord]) {
  def includes(ucd: UcdWord): Boolean = tokens.contains(ucd)
  def matches(r: Regex): Boolean = tokens.exists(t => r.findFirstIn(t.token).isDefined)
}

object Ucd {
  def parseUcd(v: String): Ucd = Ucd(v.split(";").filter(_.nonEmpty).map(_.toLowerCase).map(UcdWord).toList)

  def apply(ucd: String):Ucd = parseUcd(ucd)
}

/** Describes a field */
case class FieldId(id: String, ucd: Ucd)
case class FieldDescriptor(id: FieldId, name: String)

case class TableRowItem(field: FieldDescriptor, data: String)
case class TableRow(items: List[TableRowItem]) {
  def itemsMap:Map[FieldId, String] = items.map(i => i.field.id -> i.data)(collection.breakOut)
}

/** ParsedTable and ParsedResources contains a list of problems */
case class ParsedTable(rows: List[CatalogProblem \/ SiderealTarget]) {
  def containsError: Boolean = rows.exists(_.isLeft)
}

object ParsedTable {
  implicit val monoid = Monoid.instance[ParsedTable]((a, b) => ParsedTable(a.rows |+| b.rows), ParsedTable(Nil))
}

case class ParsedVoResource(tables: List[ParsedTable]) {
  def containsError: Boolean = tables.exists(_.containsError)
}

/** The result of parsing a Catalog Query is a list of targets */
case class TargetsTable(rows: List[SiderealTarget])

object TargetsTable {
  def apply(t: ParsedTable): TargetsTable = TargetsTable(t.rows.collect { case \/-(r) => r })

  val Zero = TargetsTable(Nil)

  implicit val monoid = Monoid.instance[TargetsTable]((a, b) => TargetsTable(a.rows |+| b.rows), Zero)
}

case class CatalogQueryResult(targets:TargetsTable, problems: List[CatalogProblem]) {
  def containsError: Boolean = problems.nonEmpty

  def filter(query: CatalogQuery): CatalogQueryResult = {
    val t = targets.rows.filter(query.filter)
    copy(targets = TargetsTable(t))
  }
}

object CatalogQueryResult {
  def apply(r: ParsedVoResource):CatalogQueryResult = CatalogQueryResult(TargetsTable(r.tables.foldMap(identity)), r.tables.flatMap(_.rows.collect {case -\/(p) => p}))

  val Zero = CatalogQueryResult(TargetsTable.Zero, Nil)

  implicit val monoid = Monoid.instance[CatalogQueryResult]((a, b) => CatalogQueryResult(a.targets |+| b.targets, a.problems |+| b.problems), Zero)
}

case class QueryResult(query: CatalogQuery, result: CatalogQueryResult)

/** Indicates an issue parsing the targets, e.g. missing values, bad format, etc. */
sealed trait CatalogProblem

case class ValidationError(url: String) extends CatalogProblem
case class GenericError(msg: String) extends CatalogProblem
case class MissingValues(fields: List[Ucd]) extends CatalogProblem
case class FieldValueProblem(ucd: Ucd, value: String) extends CatalogProblem
case class UnmatchedField(ucd: Ucd) extends CatalogProblem
case object UnknownCatalog extends CatalogProblem

case class CatalogException(problems: List[CatalogProblem]) extends RuntimeException(problems.mkString(", ")) {
  def firstMessage:String = ~problems.headOption.map {
    case e: GenericError => e.msg
    case e               => e.toString
  }
}