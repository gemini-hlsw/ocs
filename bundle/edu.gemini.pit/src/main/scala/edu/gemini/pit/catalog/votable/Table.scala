package edu.gemini.pit.catalog.votable

object Table {
  
  def apply(e:Elem):Table = {
    val id = e.attr("ID")
    val name = e.attr("name")
    val fields = e.elems("FIELD").map(Field(_))
    val data = Data(e.elem("DATA"))
    Table(id, name, fields, data)
  }
  
}

case class Table(id:String, name:String, fields:List[Field], data:Data) {
  
  override def toString = "Table(%s, %s, %d x %d)".format(id, name, fields.length, data.tableData.rows.length)
  
}