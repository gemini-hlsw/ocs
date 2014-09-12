package edu.gemini.pit.catalog.votable

object Resource {
  
  def apply(e:Elem):Resource = {
    val cooSys = e.elemOption("COOSYS").map(CooSys(_))
    val tables = e.elems("TABLE").map(Table(_))
    Resource(cooSys, tables)
  }
  
}

case class Resource(cooSys:Option[CooSys], tables:List[Table])