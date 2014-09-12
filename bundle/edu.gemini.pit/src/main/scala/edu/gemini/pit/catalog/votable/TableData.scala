package edu.gemini.pit.catalog.votable

object TableData {

  def apply(e:Elem):TableData = {
    val rows = e.elems("TR").map(_.elems("TD").map(_.text))
    TableData(rows)
  }
  
}

case class TableData(rows:List[Row])