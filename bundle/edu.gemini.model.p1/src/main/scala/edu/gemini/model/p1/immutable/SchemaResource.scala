package edu.gemini.model.p1.immutable

import scala.xml.{ XML, Elem }

object SchemaResource {

  private var map = Map[String, Elem]()

  private def load(schemaFile: String) = synchronized {
    map.getOrElse(schemaFile, {
      val e = XML.load(getClass.getResource("/" + schemaFile))
      map = map + (schemaFile -> e)
      e
    })
  }

}

class SchemaResource(schemaFile: String) {

  import SchemaResource._

  lazy val schema = load(schemaFile)

  // TODO: probably a better way to do this
  protected def patternFor(simpleType: String) = (for {
    t <- schema \ "simpleType"
    n <- t \ "@name"
    if n.text == simpleType
    p <- t \\ "pattern"
    v <- p \ "@value"
  } yield v).toSeq.map(_.text.r.pattern).headOption.get

}
