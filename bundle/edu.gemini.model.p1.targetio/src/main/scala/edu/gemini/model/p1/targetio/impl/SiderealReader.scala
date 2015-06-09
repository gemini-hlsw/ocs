package edu.gemini.model.p1.targetio.impl

import edu.gemini.model.p1.immutable.{Magnitude, ProperMotion, HmsDms, SiderealTarget}
import edu.gemini.model.p1.mutable.CoordinatesEpoch.J_2000
import edu.gemini.model.p1.mutable.{MagnitudeBand, MagnitudeSystem}
import edu.gemini.model.p1.mutable.MagnitudeSystem.VEGA

import edu.gemini.model.p1.targetio.api._
import edu.gemini.model.p1.targetio.table._

import SiderealColumns._
import java.io.{InputStream, File}
import java.util.UUID

object SiderealReader extends TargetReader[SiderealTarget] {
  def read(file: File): Result      = targets(TableReader(file, REQUIRED))
  def read(is: InputStream): Result = targets(TableReader(is,   REQUIRED))
  def read(data: String): Result    = targets(TableReader(data, REQUIRED))

  private def targets(e: Either[String, List[TableReader]]): Result =
    e match {
      case Left(msg)  => Left(DataSourceError(msg))
      case Right(lst) => Right(lst.flatMap(targets))
    }

  private def name(table: TableReader, row: TableReader#Row) =
    if (table.has(NAME)) row(NAME).right.toOption else None

  private def targets(table: TableReader): List[TargetResult] =
    table.rows map { row =>
      target(row).left map { msg => ParseError(msg, name(table, row), row.data) }
    }

  private def target(row: TableReader#Row): Either[String, SiderealTarget] =
    for {
      name <- row(NAME).right
      ra   <- row(RA).right
      dec  <- row(DEC).right
      pm   <- pm(row).right
      mag  <- magList(row).right
    } yield SiderealTarget(UUID.randomUUID(), name, HmsDms(ra, dec), J_2000, pm, mag)

  private def pm(row: TableReader#Row): Either[String, Option[ProperMotion]] =
    for {
      deltaRA  <- row.get(PM_RA).right
      deltaDec <- row.get(PM_DEC).right
    } yield deltaRA.flatMap(r => deltaDec.map(d => ProperMotion(r, d)))


  private val emptyMags: Either[String, List[Magnitude]] = Right(Nil)

  private def magList(row: TableReader#Row): Either[String, List[Magnitude]] =
    (emptyMags/:MagnitudeBand.values().toList) {
      (res, band) => {
        for {
          lst <- res.right
          mag <- row.get(SiderealColumns.magnitude(band)).right
          sys <- row.get(SiderealColumns.magnitudeSystem(band)).right
        } yield cons(mag, sys, lst)
      }
    }

  private def cons(omag: Option[Magnitude], osys: Option[MagnitudeSystem], lst: List[Magnitude]): List[Magnitude] =
    omag map { _.copy(system = osys.getOrElse(VEGA)) :: lst } getOrElse lst
}