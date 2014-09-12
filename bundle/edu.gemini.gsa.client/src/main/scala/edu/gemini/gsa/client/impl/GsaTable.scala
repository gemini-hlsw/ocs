package edu.gemini.gsa.client.impl

import edu.gemini.gsa.client.api._
import edu.gemini.model.p1.immutable._

object GsaTable {
  /**
   * Turns the map of scapped GSA table information (column name -> List of
   * cells in the rows of that column) into a GsaTable object, which provides
   * access to the datasets.
   */
  def fromMap(m: GsaMap): Either[String, GsaTable] =
    GsaColumn.ALL.filterNot(col => m.contains(col.name)) match {
      case Nil => Right(new GsaTable(m))
      case lst => Left("GSA data is missing columns: " + lst.map(_.name).mkString(", "))
    }
}

import GsaColumn._

class GsaTable private (table: GsaMap) {
  def datasets: List[Either[String, GsaDataset]] =
    references.zip(targets).zip(instruments).zip(times).zip(details) map {
      case ((((refE, targetE), instE), timeE), detailE) => for {
        ref    <- refE.right
        target <- targetE.right
        inst   <- instE.right
        time   <- timeE.right
        detail <- detailE.right
      } yield GsaDataset(ref, target, inst, time, detail)
    }

  def references: List[Either[String, GsaDatasetReference]] =
    parseCol(DSET_NAME).zip(parseCol(FILE_NAME)).zip(parseCol(SCIENCE_PROG)) map {
      case ((nameE, fileE), progE) => for {
        name <- nameE.right
        file <- fileE.right
        prog <- fileE.right
      } yield GsaDatasetReference(name, file, prog)
    }

  def times: List[Either[String, GsaDatasetTime]] =
    utTimes.zip(releaseTimes) map {
      case (ute, rele) => for {
        ut  <- ute.right
        rel <- rele.right
      } yield GsaDatasetTime(ut, rel)
    }


  def utTimes: List[Either[String, Long]]      = parseCol(UT_DATE)
  def releaseTimes: List[Either[String, Long]] = parseCol(RELEASE_DATE)

  def targets: List[Either[String, GsaDatasetTarget]] =
    rows(TARGET_NAME).zip(coordinates) map {
      case (name, coorde) => coorde.right map { c => GsaDatasetTarget(name, c) }
    }

  def coordinates: List[Either[String, Coordinates]] =
    ras.zip(decs) map {
      case (rae, dece) => for {
        ra  <- rae.right
        dec <- dece.right
      } yield HmsDms(ra, dec)
    }

  def ras:  List[Either[String, HMS]] = parseCol(RA)
  def decs: List[Either[String, DMS]] = parseCol(DEC)

  def instruments: List[Either[String, String]] = parseCol(INSTRUMENT)

  def integrationTimes: List[Either[String, Option[Long]]] = parseCol(INTEGRATION_TIME)
  def filters: List[Either[String, String]]                = parseCol(FILTER)
  def wavelengths: List[Either[String, Option[Double]]]    = parseCol(WAVELENGTH)

  def details: List[Either[String, GsaDatasetDetail]] =
    integrationTimes.zip(filters).zip(wavelengths) map {
      case ((integrationE, filterE), wavelengthE) =>
        for {
          integration <- integrationE.right
          filters     <- filterE.right
          wavelength  <- wavelengthE.right
        } yield GsaDatasetDetail(integration, filters, wavelength)
    }

  private def parseCol[T](col: GsaColumn[T]) = rows(col).map(cell => col.parse(cell))

  private def rows(col: GsaColumn[_]): List[String] = table.getOrElse(col.name, Nil)
}