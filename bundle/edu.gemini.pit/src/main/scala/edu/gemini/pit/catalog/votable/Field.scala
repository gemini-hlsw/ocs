package edu.gemini.pit.catalog.votable

object Field {

  def apply(e: Elem): Field = {
    val id = e.attr("ID")
    val name = e.attr("name")
    val ucd = e.attrOption("ucd")
    val datatype = e.attr("datatype")
    val width = e.attrOption("width")
    val type_ = e.attrOption("type")
    Field(id, name, ucd, datatype, width, type_)
  }

}

case class Field(id: String, name: String, ucd: Option[String], datatype: String, width: Option[String], type_ : Option[String])