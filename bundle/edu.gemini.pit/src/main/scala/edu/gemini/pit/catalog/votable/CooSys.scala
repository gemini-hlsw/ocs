package edu.gemini.pit.catalog.votable

object CooSys {

  def apply(e: Elem): CooSys = {

    val id = e.attr("ID")
    val equinox = e.attr("equinox")
    val epoch = e.attr("epoch")
    val system = e.attr("system")

    CooSys(id, equinox, epoch, system)

  }
}

case class CooSys(id:String, equinox:String, epoch:String, system:String)

