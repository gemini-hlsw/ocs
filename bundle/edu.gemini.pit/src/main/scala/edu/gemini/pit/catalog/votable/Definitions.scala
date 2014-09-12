package edu.gemini.pit.catalog.votable

object Definitions {
  
  def apply(e: Elem): Definitions = {
    val cooSys = CooSys(e.elem("COOSYS"))
    Definitions(cooSys)
  }
  
}

case class Definitions(cooSys: CooSys)