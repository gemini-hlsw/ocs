package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

sealed trait Site {
  def name: String
  def abbreviation: String
  def isExchange: Boolean = false
}

object Site {
  def fromMutable(site: M.Site):Site = site match {
    case M.Site.GEMINI_NORTH => GN
    case M.Site.GEMINI_SOUTH => GS
  }

  def toMutable(site: Site):M.Site = site match {
    case GN => M.Site.GEMINI_NORTH
    case GS => M.Site.GEMINI_SOUTH
    case _  => sys.error("not supported")
  }

  case object GN extends Site {
    def name = "Gemini North"
    def abbreviation = "GN"
  }

  case object GS extends Site {
    def name = "Gemini South"
    def abbreviation = "GS"
  }

  case object Keck extends Site {
    def name = "W.M. Keck Observatory"
    def abbreviation = "Keck"
    override def isExchange = true
  }

  case object Subaru extends Site {
    def name = "Subaru Telescope"
    def abbreviation = "Subaru"
    override def isExchange = true
  }

  case object CFH extends Site {
    def name = "CFH Observatory"
    def abbreviation = "CFH"
    override def isExchange = true
  }
}