package edu.gemini.catalog.votable

import edu.gemini.spModel.core.Target.SiderealTarget

import scalaz.{\/-, \/}

case class UcdWord(token: String)
case class Ucd(tokens: List[UcdWord]) {
  def includes(ucd: UcdWord): Boolean = tokens.contains(ucd)
}

object Ucd {
  def parseUcd(v: String): Ucd = Ucd(v.split(";").filter(_.nonEmpty).map(_.toLowerCase).map(UcdWord).toList)

  def apply(ucd: String):Ucd = parseUcd(ucd)
}

/** Describes a field */
case class FieldDescriptor(id: String, name: String, ucd: Ucd)

case class TableRowItem(field: FieldDescriptor, data: String)
case class TableRow(items: List[TableRowItem]) {
  def itemsMap:Map[Ucd, String] = items.map(i => i.field.ucd -> i.data)(collection.breakOut)
}

/** ParsedTable and ParsedResources contains a list of problems */
case class ParsedTable(rows: List[CatalogProblem \/ SiderealTarget]) {
  def containsError: Boolean = rows.exists(_.isLeft)
}
case class ParsedVoResource(tables: List[ParsedTable]) {
  def containsError: Boolean = tables.exists(_.containsError)
}

/** The result of parsing a Catalog Query is a list of targets */
case class TargetsTable(rows: List[SiderealTarget])

object TargetsTable {
  def apply(t: ParsedTable): TargetsTable = TargetsTable(t.rows.collect { case \/-(r) => r })
}

case class VoResource(tables: List[TargetsTable])

object VoResource {
  def apply(r: ParsedVoResource):VoResource = VoResource(r.tables.map(TargetsTable.apply))
}

/** Indicates an issue parsing the targets, e.g. missing values, bad format, etc. */
sealed trait CatalogProblem

case class ValidationError(url: String) extends CatalogProblem
case class GenericError(msg: String) extends CatalogProblem
case class MissingValues(fields: List[Ucd]) extends CatalogProblem
case class FieldValueProblem(ucd: Ucd, value: String) extends CatalogProblem
case class UnmatchedField(ucd: Ucd) extends CatalogProblem