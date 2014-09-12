package edu.gemini.pit.catalog.votable

object Data {

  def apply(e:Elem):Data = {
    val tableData = TableData(e.elem("TABLEDATA"))
    Data(tableData)
  }
  
}

case class Data(tableData:TableData)