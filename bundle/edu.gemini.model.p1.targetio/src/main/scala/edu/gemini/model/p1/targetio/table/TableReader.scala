package edu.gemini.model.p1.targetio.table

import uk.ac.starlink.fits.FitsTableBuilder
import uk.ac.starlink.votable.VOTableBuilder
import annotation.tailrec
import uk.ac.starlink.table._
import formats.{CsvTableBuilder, TstTableBuilder}
import java.io._
import uk.ac.starlink.util._
import java.util.logging.{Level, Logger}

class TableReader(table: StarTable) {
  // Map of column name to the corresponding table column index
  val colIndex: Map[String, Int] =
    (0 until table.getColumnCount).map(i => table.getColumnInfo(i).getName -> i).toMap

  class Row(val data: Seq[_]) {
    def apply[T](col: Column[_, T]): Either[String, T] =
      col.read(data(colIndex(col.name)))

    def get[T](col: Column[_, Option[T]]): Either[String, Option[T]] =
      colIndex.get(col.name) match {
        case None    => Right(None)
        case Some(i) => col.read(data(i))
      }
  }

  def rows: List[Row] =
    ((0L until table.getRowCount) map { i => new Row(table.getRow(i).toSeq) }).toList

  def has(col: Column[_,_]): Boolean = colIndex.get(col.name).isDefined
}

object TableReader {
  private val LOG = Logger.getLogger(classOf[TableReader].getName)

  def UNRECOGNIZED_MESSAGE =
    "Couldn't read the format of the target table input.  Expecting a FITS table, VO table, a CSV file, or tab-separated text."
  def MISSING_COLUMNS_MESSAGE(cols: Traversable[String]) =
    "Input missing columns: " + cols.mkString(", ")
  def RANDOM_STIL_EXCEPTION_MESSAGE(msg: String) = {
    val noclue = "There was a problem reading targets from the input, check the source."
    Option(msg) match {
      case None                    => noclue
      case Some(s) if s.trim == "" => noclue
      case Some(s) => "Encounted an unexpected problem reading targets from the input: %s.  Check the source.".format(s)
    }
  }

  private val supportedFormats =
    Array[TableBuilder](new FitsTableBuilder, new VOTableBuilder, new CsvTableBuilder, new TstTableBuilder)

  private val factory = new StarTableFactory(true) {
    setDefaultBuilders(Array[TableBuilder]())
    setKnownBuilders(supportedFormats)
  }

  @tailrec
  private def starTables(seq: TableSequence, res: List[StarTable]): List[StarTable] = {
    val tab = seq.nextTable()
    if (tab == null) res else starTables(seq, tab :: res)
  }

  private def starTables(seq: TableSequence): List[StarTable] = starTables(seq, Nil).reverse

  // Try all the formats one by one looking for one that works.  Don't let
  // STIL handle this because it is buggy and throws exceptions.
  private def makeStarTables(src: DataSource): Either[String, TableSequence] = {
    val empty: Option[TableSequence] = None
    val res = (empty/:supportedFormats) {
      (opt, bldr) => opt.orElse(try {
        Some(factory.makeStarTables(src, bldr.getFormatName))
      } catch {
        case ex: TableFormatException =>
          // Expected because only at most one format that we try will be valid
          None
        case ex: Exception =>
          LOG.log(Level.WARNING, "Problem reading target file.: " + ex.getMessage)
          None
      })
    }
    res.toRight(UNRECOGNIZED_MESSAGE)
  }


  private def apply(seq: TableSequence, requiredColumns: Traversable[Column[_,_]]): Either[String, List[TableReader]] =
    try {
      val tables  = starTables(seq) map { tab => new TableReader(tab) }
      val missing = missingColumns(tables, requiredColumns)
      if (missing.isEmpty)
        Right(tables)
      else
        Left(MISSING_COLUMNS_MESSAGE(requiredColumns.filter(missing.contains).map(_.name)))
    } catch {
      case ex: Exception => Left(errorMessage(ex))
    }

  private def apply(src: DataSource, requiredColumns: Traversable[Column[_,_]]): Either[String, List[TableReader]] =
    try {
      for {
        seq  <- makeStarTables(src).right
        rdrs <- apply(seq, requiredColumns).right
      } yield rdrs
    } finally {
      src.close()
    }

  private def errorMessage(ex: Exception): String = {
    LOG.log(Level.WARNING, Option(ex.getMessage).getOrElse(""), ex)
    RANDOM_STIL_EXCEPTION_MESSAGE(ex.getMessage)
  }

  def apply(file: File, requiredColumns: Traversable[Column[_, _]]): Either[String, List[TableReader]] =
    apply(new FileDataSource(file), requiredColumns)

  def apply(data: String, requiredColumns: Traversable[Column[_, _]]): Either[String, List[TableReader]] =
    apply(new ByteArrayDataSource("", data.getBytes("UTF-8")), requiredColumns)

  // Grim Java method for reading an input stream into a byte array
  def toDataSource(is: InputStream): Either[String, DataSource] = {
    val buf = new Array[Byte](1024 * 8)
    val bos = new ByteArrayOutputStream()
    val bis = new BufferedInputStream(is)
    try {
      var r = bis.read(buf)
      while (r != -1) {
        bos.write(buf, 0, r)
        r = bis.read(buf)
      }
      bos.flush()
      Right(new ByteArrayDataSource("", bos.toByteArray))
    } catch {
      case ex: Exception => Left(errorMessage(ex))
    } finally {
      try { bos.close() } catch { case ex: Exception => /* ignore */ }
    }
  }

  def apply(is: InputStream, requiredColumns: Traversable[Column[_, _]]): Either[String, List[TableReader]] =
    for {
      src <- toDataSource(is).right
      res <- apply(src, requiredColumns).right
    } yield res

  private def missingColumns(table: TableReader, requiredColumns: Traversable[Column[_,_]]): Set[Column[_,_]] =
    requiredColumns.filterNot(table.has).toSet

  private def missingColumns(tables: List[TableReader], requiredColumns: Traversable[Column[_,_]]): Set[Column[_,_]] =
    (Set.empty[Column[_,_]]/:tables) {
      (res, table) => res ++ missingColumns(table, requiredColumns)
    }
}
