package edu.gemini.model.p1.targetio.table

import PartialFunction.condOpt
import uk.ac.starlink.table.ColumnInfo
import edu.gemini.model.p1.targetio.api.{FileFormat, Binary, Text}

case class Column[R, T](name: String, parse: PartialFunction[Any, T], extract: R => T, serializer: StilSerializer[T]) {
  def read(obj: Any): Either[String, T] =
    try {
      condOpt(obj)(parse).toRight(parseError(obj))
    } catch {
      case ex: Exception => Left(parseError(obj))
    }

  val writePrimitive = (serializer.asBinary _).compose(extract)
  val writeText      = (serializer.asText _).compose(extract)

  def stilClass(format: FileFormat): Class[_] = format match {
    case Text   => classOf[String]
    case Binary => serializer.primitiveClass
  }

  def info(format: FileFormat): ColumnInfo = new ColumnInfo(name, stilClass(format), name)

  def writer(format: FileFormat): R => Any = format match {
    case Binary => writePrimitive
    case Text   => writeText
  }

  def write(format: FileFormat, value: R): Any = writer(format).apply(value)

  def parseError(o: Any) = 
    "Could not read '%s' value from '%s'".format(name, Option(o).map(_.toString).getOrElse(""))

}