package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.mutable.CoordinatesEpoch.J_2000
import edu.gemini.model.p1.targetio.api._
import edu.gemini.model.p1.targetio.table._

import NonSiderealColumns._
import java.io.{InputStream, File}
import java.util.UUID

import edu.gemini.spModel.core.Coordinates

object NonSiderealReader extends TargetReader[NonSiderealTarget] {
  def read(file: File): Result      = targets(TableReader(file, REQUIRED))
  def read(is: InputStream): Result = targets(TableReader(is,   REQUIRED))
  def read(data: String): Result    = targets(TableReader(data, REQUIRED))

  private def targets(e: Either[String, List[TableReader]]): Result =
    e match {
      case Left(msg)  => Left(DataSourceError(msg))
      case Right(lst) => Right(lst.flatMap(targets))
    }

  private def targets(table: TableReader): List[TargetResult] = {
    val (errors, targets) = group(table)
    errors.map(Left(_)) ++ targets.map(Right(_))
  }

  private def group(table: TableReader): (List[ParseError], List[NonSiderealTarget]) = {
    val (errors, successes) = elements(table).partition(_.isLeft)
    val ephrows = successes map { _.right.get }

    val groupedElements = ephrows.groupBy(_.ord)
    val targets = groupedElements mapValues { lst =>
      NonSiderealTarget(UUID.randomUUID(), lst.head.name, lst.map(_.element).sortBy(_.validAt), J_2000)
    }

    case class Order(targetList: List[NonSiderealTarget], viewed: Set[Int]) {
      def +(ord: Int): Order =
        if (viewed.contains(ord)) this else Order(targets(ord) :: targetList, viewed + ord)
    }

    val orderedIds     = ephrows.map(_.ord)
    val orderedTargets = (Order(Nil, Set.empty)/:orderedIds)(_ + _).targetList.reverse
    (errors.map(_.left.get), orderedTargets)
  }

  private def name(table: TableReader, row: TableReader#Row) =
    if (table.has(NAME)) row(NAME).right.toOption else None

  private def elements(table: TableReader): List[Either[ParseError, NamedEphemeris]] =
    table.rows map { row =>
      element(row).left map { msg => ParseError(msg, name(table, row), row.data) }
    }

  private def element(row: TableReader#Row): Either[String, NamedEphemeris] =
    for {
      id   <- row(ID).right
      name <- row(NAME).right
      utc  <- row(UTC).right
      ra   <- row(RA).right
      dec  <- row(DEC).right
      mag  <- row.get(MAG).right
    } yield NamedEphemeris(id, name, EphemerisElement(Coordinates(ra, dec), mag, utc))
}