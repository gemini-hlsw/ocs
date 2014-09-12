package edu.gemini.pit.catalog.votable

import scala.xml._

object VOTable {

  def apply(is:java.io.InputStream):VOTable = apply(XML.load(is))
  
  def apply(e: Elem):VOTable = {
    val version = e.attr("version")
    val definitions = e.elemOption("DEFINITIONS").map(Definitions(_))
    val cooSys = e.elemOption("COOSYS").map(CooSys(_))
    val resources = e.elems("RESOURCE").map(Resource(_))
    VOTable(version, definitions, cooSys, resources)
  }

}

case class VOTable(version: String, definitions: Option[Definitions], cooSys:Option[CooSys], resources:List[Resource])
